using System.Windows;
using Gocent.VoiceTerminal.Services;

namespace Gocent.VoiceTerminal.Views;

public partial class SettingsWindow : Window
{
    private readonly VoiceRuntime _runtime;

    public SettingsWindow(VoiceRuntime runtime)
    {
        InitializeComponent();
        _runtime = runtime;
        TxtServer.Text = runtime.Config.ServerBaseUrl;
        TxtDevice.Text = runtime.Config.DeviceCode;
        ChkAutoStart.IsChecked = runtime.Config.OpenAtLogin;
        SldVolume.Value = runtime.Config.Volume;
        TxtVolume.Text = ((int)SldVolume.Value).ToString();
        LoadVoices();
        RefreshStatus();
        _runtime.StatusChanged += OnStatusChanged;
        Closed += (_, _) => _runtime.StatusChanged -= OnStatusChanged;
    }

    private void LoadVoices()
    {
        var voices = TtsQueue.ListInstalledVoices().ToList();
        // 首项：自动（优先中文）
        var auto = new TtsQueue.VoiceOption("", "自动（优先中文）");
        voices.Insert(0, auto);
        CmbVoice.ItemsSource = voices;

        var saved = (_runtime.Config.VoiceName ?? "").Trim();
        if (!string.IsNullOrEmpty(saved) && voices.Any(v => v.Name == saved))
            CmbVoice.SelectedValue = saved;
        else
            CmbVoice.SelectedIndex = 0;
    }

    private void OnStatusChanged() => Dispatcher.Invoke(RefreshStatus);

    private void RefreshStatus()
    {
        var last = string.IsNullOrWhiteSpace(_runtime.Config.LastCallText)
            ? ""
            : "\n最近叫号：" + _runtime.Config.LastCallText;
        var selected = (CmbVoice.SelectedValue as string) ?? _runtime.Config.VoiceName;
        var effective = string.IsNullOrWhiteSpace(selected)
            ? TtsQueue.ResolveChineseVoiceName()
            : selected;
        var voiceHint = effective != null
            ? "\n当前音色：" + effective + " · 音量 " + (int)SldVolume.Value
            : "\n⚠ 未检测到中文语音包。请在 Windows「设置 → 时间和语言 → 语音」安装中文（简体）语音。";
        TxtStatus.Text = "连接状态：" + _runtime.StatusText + last + voiceHint;
    }

    private void SldVolume_ValueChanged(object sender, RoutedPropertyChangedEventArgs<double> e)
    {
        if (TxtVolume == null) return;
        TxtVolume.Text = ((int)e.NewValue).ToString();
    }

    private string? SelectedVoiceName()
    {
        if (CmbVoice.SelectedItem is TtsQueue.VoiceOption opt)
            return opt.Name;
        return CmbVoice.SelectedValue as string ?? "";
    }

    private async void Save_Click(object sender, RoutedEventArgs e)
    {
        TxtStatus.Text = "校验设备并连接中…";
        var server = TxtServer.Text?.Trim() ?? "";
        var code = TxtDevice.Text?.Trim() ?? "";
        var auto = ChkAutoStart.IsChecked == true;
        var volume = (int)SldVolume.Value;
        var voice = SelectedVoiceName();

        var (ok, message, summary) = await VoiceApiClient.ValidateDeviceAsync(server, code);
        if (!ok)
        {
            TxtStatus.Text = message;
            return;
        }

        _runtime.ApplyConfig(server, code, auto, volume, voice);
        await _runtime.StartAsync();
        TxtStatus.Text = "已保存并连接。" + (string.IsNullOrEmpty(summary) ? "" : " 覆盖：" + summary);
        RefreshStatus();
    }

    private void Test_Click(object sender, RoutedEventArgs e)
    {
        var voice = SelectedVoiceName();
        var volume = (int)SldVolume.Value;
        _runtime.ApplyVoiceSettings(volume, voice);

        if (string.IsNullOrWhiteSpace(voice) && !TtsQueue.HasChineseVoice())
        {
            System.Windows.MessageBox.Show(
                "本机未安装中文 TTS 语音包。\n\n请打开：Windows 设置 → 时间和语言 → 语音 → 添加「中文（简体）」语音。\n安装后重启本程序再测。",
                "缺少中文语音",
                MessageBoxButton.OK,
                MessageBoxImage.Warning);
            return;
        }

        _runtime.TestSpeak();
        RefreshStatus();
    }
}
