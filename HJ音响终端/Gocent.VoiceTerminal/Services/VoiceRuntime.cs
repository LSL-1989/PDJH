using Newtonsoft.Json.Linq;

namespace Gocent.VoiceTerminal.Services;

/// <summary>收号 + TTS 运行时，供托盘与设置窗共享。</summary>
public sealed class VoiceRuntime : IDisposable
{
    private readonly object _gate = new();
    private StompVoiceClient? _client;
    private readonly TtsQueue _tts = new();

    public AppConfig Config { get; }
    public string StatusText { get; private set; } = "未启动";
    public bool IsOnline { get; private set; }

    public event Action? StatusChanged;

    public VoiceRuntime(AppConfig config)
    {
        Config = config;
        _tts.ApplySettings(config.Volume, config.VoiceName);
        AutoStart.SetEnabled(config.OpenAtLogin);
    }

    public async Task StartAsync()
    {
        lock (_gate)
        {
            _client?.Dispose();
            _client = null;
        }

        var code = (Config.DeviceCode ?? "").Trim().ToUpperInvariant();
        if (string.IsNullOrEmpty(code))
        {
            SetStatus(false, "请先配置设备编号");
            return;
        }

        string wsUrl;
        try
        {
            wsUrl = AppConfig.ToWsUrl(Config.ServerBaseUrl);
        }
        catch (Exception ex)
        {
            SetStatus(false, ex.Message);
            return;
        }

        SetStatus(false, "连接中…");

        var client = new StompVoiceClient();
        client.Connected += () => SetStatus(true, "已连接，等待叫号");
        client.Disconnected += detail => SetStatus(false, string.IsNullOrEmpty(detail) ? "已断开，重连中…" : detail);
        client.MessageReceived += OnMessage;

        lock (_gate)
        {
            _client = client;
        }
        client.Connect(wsUrl, code);
        await Task.CompletedTask;
    }

    private void OnMessage(string body)
    {
        try
        {
            var json = JObject.Parse(body);
            var text = json.Value<string>("ttsCallText")
                       ?? json.Value<string>("callText")
                       ?? "";
            text = text.Trim();
            if (text.Length == 0) return;
            var times = json.Value<int?>("callNum") ?? json.Value<int?>("CallNum") ?? 1;
            var rate = json.Value<int?>("rate") ?? json.Value<int?>("Rate") ?? 0;
            Config.LastCallText = text;
            Config.Save();
            _tts.Enqueue(text, times, rate);
            StatusChanged?.Invoke();
        }
        catch
        {
            /* ignore bad payload */
        }
    }

    public void TestSpeak() => _tts.Enqueue("测试语音，独立音响终端运行正常");

    public void ApplyConfig(
        string serverBaseUrl,
        string deviceCode,
        bool openAtLogin,
        int volume,
        string? voiceName)
    {
        Config.ServerBaseUrl = (serverBaseUrl ?? "").Trim();
        Config.DeviceCode = (deviceCode ?? "").Trim().ToUpperInvariant();
        Config.OpenAtLogin = openAtLogin;
        Config.Volume = Math.Clamp(volume, 0, 100);
        Config.VoiceName = (voiceName ?? "").Trim();
        Config.Save();
        _tts.ApplySettings(Config.Volume, Config.VoiceName);
        AutoStart.SetEnabled(openAtLogin);
    }

    /// <summary>仅更新音量/音色并立即生效（测试播报前可先套用）。</summary>
    public void ApplyVoiceSettings(int volume, string? voiceName)
    {
        Config.Volume = Math.Clamp(volume, 0, 100);
        Config.VoiceName = (voiceName ?? "").Trim();
        Config.Save();
        _tts.ApplySettings(Config.Volume, Config.VoiceName);
    }

    private void SetStatus(bool online, string text)
    {
        IsOnline = online;
        StatusText = text;
        StatusChanged?.Invoke();
    }

    public void Dispose()
    {
        lock (_gate)
        {
            _client?.Dispose();
            _client = null;
        }
        _tts.Dispose();
    }
}
