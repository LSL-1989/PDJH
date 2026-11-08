using System.IO;
using System.Text.Json;

namespace Gocent.VoiceTerminal.Services;

public sealed class AppConfig
{
    public string ServerBaseUrl { get; set; } = "http://127.0.0.1:8081";
    public string DeviceCode { get; set; } = "";
    public bool OpenAtLogin { get; set; } = true;
    public string LastCallText { get; set; } = "";

    /// <summary>TTS 音量 0–100。</summary>
    public int Volume { get; set; } = 100;

    /// <summary>选用的系统语音名称；空则自动选中文语音。</summary>
    public string VoiceName { get; set; } = "";

    private static string ConfigPath =>
        Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "Gocent", "VoiceTerminal", "config.json");

    public static AppConfig Load()
    {
        try
        {
            var path = ConfigPath;
            if (File.Exists(path))
            {
                var json = File.ReadAllText(path);
                var cfg = JsonSerializer.Deserialize<AppConfig>(json) ?? new AppConfig();
                // 旧配置无 Volume 字段时反序列化为 0，回退默认 100
                if (!json.Contains("\"Volume\"", StringComparison.Ordinal))
                    cfg.Volume = 100;
                else
                    cfg.Volume = Math.Clamp(cfg.Volume, 0, 100);
                cfg.VoiceName ??= "";
                return cfg;
            }
        }
        catch
        {
            /* ignore */
        }
        return new AppConfig();
    }

    public void Save()
    {
        var dir = Path.GetDirectoryName(ConfigPath)!;
        Directory.CreateDirectory(dir);
        Volume = Math.Clamp(Volume, 0, 100);
        VoiceName ??= "";
        var json = JsonSerializer.Serialize(this, new JsonSerializerOptions { WriteIndented = true });
        File.WriteAllText(ConfigPath, json);
    }

    public static string NormalizeApiBase(string? serverBaseUrl)
    {
        var raw = (serverBaseUrl ?? "").Trim().TrimEnd('/');
        if (string.IsNullOrEmpty(raw)) return "http://127.0.0.1:8081/api";
        if (raw.EndsWith("/api", StringComparison.OrdinalIgnoreCase)) return raw;
        return raw + "/api";
    }

    public static string ToWsUrl(string? serverBaseUrl)
    {
        var api = NormalizeApiBase(serverBaseUrl);
        if (!Uri.TryCreate(api, UriKind.Absolute, out var uri))
            throw new ArgumentException("服务地址无效");
        var proto = uri.Scheme.Equals("https", StringComparison.OrdinalIgnoreCase) ? "wss" : "ws";
        return $"{proto}://{uri.Authority}{uri.AbsolutePath.TrimEnd('/')}/pdjh-ws-native";
    }
}
