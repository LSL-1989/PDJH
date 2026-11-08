using System.Drawing;
using System.IO;
using System.Windows.Forms;
using Gocent.VoiceTerminal.Services;

namespace Gocent.VoiceTerminal.Services;

public sealed class TrayHost : IDisposable
{
    private readonly VoiceRuntime _runtime;
    private readonly Action _openSettings;
    private readonly Action _exit;
    private readonly NotifyIcon _icon;
    private readonly ToolStripMenuItem _statusItem;
    private readonly ToolStripMenuItem _deviceItem;
    private readonly ToolStripMenuItem _autoStartItem;

    public TrayHost(VoiceRuntime runtime, Action openSettings, Action exit)
    {
        _runtime = runtime;
        _openSettings = openSettings;
        _exit = exit;

        _statusItem = new ToolStripMenuItem("状态：…") { Enabled = false };
        _deviceItem = new ToolStripMenuItem("设备：…") { Enabled = false };
        _autoStartItem = new ToolStripMenuItem("开机自启") { CheckOnClick = true };
        _autoStartItem.Checked = runtime.Config.OpenAtLogin;
        _autoStartItem.CheckedChanged += (_, _) =>
        {
            runtime.Config.OpenAtLogin = _autoStartItem.Checked;
            runtime.Config.Save();
            AutoStart.SetEnabled(_autoStartItem.Checked);
        };

        var menu = new ContextMenuStrip();
        menu.Items.Add(_deviceItem);
        menu.Items.Add(_statusItem);
        menu.Items.Add(new ToolStripSeparator());
        menu.Items.Add("打开设置", null, (_, _) => _openSettings());
        menu.Items.Add("测试播报", null, (_, _) => _runtime.TestSpeak());
        menu.Items.Add(_autoStartItem);
        menu.Items.Add(new ToolStripSeparator());
        menu.Items.Add("重新连接", null, async (_, _) => await _runtime.StartAsync());
        menu.Items.Add("退出", null, (_, _) => _exit());

        _icon = new NotifyIcon
        {
            Text = "HJ独立音响终端",
            Icon = LoadAppIcon(),
            Visible = true,
            ContextMenuStrip = menu
        };
        _icon.DoubleClick += (_, _) => _openSettings();

        _runtime.StatusChanged += Refresh;
        Refresh();
    }

    public void Show() => _icon.Visible = true;

    private void Refresh()
    {
        void apply()
        {
            var code = string.IsNullOrWhiteSpace(_runtime.Config.DeviceCode)
                ? "未配置"
                : _runtime.Config.DeviceCode;
            _deviceItem.Text = "设备：" + code;
            _statusItem.Text = "状态：" + _runtime.StatusText;
            _icon.Text = "HJ音响 · " + _runtime.StatusText;
            if (_icon.Text.Length > 63)
                _icon.Text = _icon.Text[..63];
            _autoStartItem.Checked = _runtime.Config.OpenAtLogin;
        }

        if (_icon.ContextMenuStrip?.InvokeRequired == true)
            _icon.ContextMenuStrip.BeginInvoke(apply);
        else
            apply();
    }

    private static Icon LoadAppIcon()
    {
        try
        {
            var exe = Environment.ProcessPath;
            if (!string.IsNullOrEmpty(exe) && File.Exists(exe))
            {
                var ico = Icon.ExtractAssociatedIcon(exe);
                if (ico != null) return ico;
            }
        }
        catch
        {
            /* ignore */
        }
        return SystemIcons.Application;
    }

    public void Dispose()
    {
        _runtime.StatusChanged -= Refresh;
        _icon.Visible = false;
        _icon.Dispose();
    }
}
