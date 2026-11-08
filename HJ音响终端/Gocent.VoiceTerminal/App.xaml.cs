using System.Threading;
using System.Windows;
using Gocent.VoiceTerminal.Services;
using Gocent.VoiceTerminal.Views;

namespace Gocent.VoiceTerminal;

public partial class App : System.Windows.Application
{
    private Mutex? _mutex;
    private TrayHost? _tray;
    private VoiceRuntime? _runtime;

    protected override void OnStartup(StartupEventArgs e)
    {
        base.OnStartup(e);

        _mutex = new Mutex(true, "Global\\Gocent.VoiceTerminal.SingleInstance", out var created);
        if (!created)
        {
            System.Windows.MessageBox.Show("HJ独立音响终端已在运行（请查看托盘图标）。", "提示",
                MessageBoxButton.OK, MessageBoxImage.Information);
            Shutdown();
            return;
        }

        var config = AppConfig.Load();
        _runtime = new VoiceRuntime(config);
        _tray = new TrayHost(_runtime, OpenSettings, ExitApp);
        _tray.Show();

        if (string.IsNullOrWhiteSpace(config.DeviceCode))
        {
            OpenSettings();
        }
        else
        {
            _ = _runtime.StartAsync();
        }
    }

    private void OpenSettings()
    {
        foreach (Window w in Windows)
        {
            if (w is SettingsWindow existing)
            {
                existing.Show();
                existing.Activate();
                return;
            }
        }

        var win = new SettingsWindow(_runtime!);
        win.Show();
    }

    private void ExitApp()
    {
        _runtime?.Dispose();
        _tray?.Dispose();
        try { _mutex?.ReleaseMutex(); } catch { /* ignore */ }
        _mutex?.Dispose();
        Shutdown();
    }

    protected override void OnExit(ExitEventArgs e)
    {
        _runtime?.Dispose();
        _tray?.Dispose();
        base.OnExit(e);
    }
}
