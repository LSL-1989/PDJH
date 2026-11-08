using System;
using System.Windows;
using System.Windows.Input;
using Gocent.CallStation.Services;

namespace Gocent.CallStation.Views
{
    /// <summary>修改密码窗；含确认密码；force=true 时原密码可空</summary>
    public partial class ChangePasswordWindow : Window
    {
        private readonly string _doctorId;
        private readonly bool _force;

        public ChangePasswordWindow(string doctorId, bool force = false)
        {
            InitializeComponent();
            ThemeService.Changed += OnThemeChanged;
            Closed += (_, __) => ThemeService.Changed -= OnThemeChanged;
            SyncThemeButton();
            _doctorId = doctorId;
            _force = force;
            if (_force)
            {
                TxtHint.Text = "管理员已重置或首次登录，请设置新密码（原密码可留空或填当前默认密码）";
            }
        }

        private void Theme_Click(object sender, RoutedEventArgs e) => ThemeService.Toggle();

        private void OnThemeChanged()
        {
            if (!Dispatcher.CheckAccess())
            {
                Dispatcher.BeginInvoke(new Action(OnThemeChanged));
                return;
            }
            SyncThemeButton();
        }

        private void SyncThemeButton()
        {
            if (BtnTheme != null) BtnTheme.Content = ThemeService.NextLabel;
        }

        private void Title_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
        {
            if (e.ChangedButton == MouseButton.Left) DragMove();
        }

        private void Cancel_Click(object sender, RoutedEventArgs e)
        {
            DialogResult = false;
            Close();
        }

        private async void Ok_Click(object sender, RoutedEventArgs e)
        {
            TxtError.Text = "";
            var neu = TxtNew.Password ?? "";
            var confirm = TxtConfirm.Password ?? "";
            if (neu.Length < 4)
            {
                TxtError.Text = "新密码至少 4 位";
                return;
            }
            if (neu != confirm)
            {
                TxtError.Text = "两次输入的新密码不一致";
                return;
            }
            try
            {
                var res = await DoctorApi.ChangePasswordAsync(_doctorId, TxtOld.Password, neu);
                if (!res.Success)
                {
                    TxtError.Text = string.IsNullOrWhiteSpace(res.Msg) ? "修改失败" : res.Msg;
                    return;
                }
                DialogResult = true;
                Close();
            }
            catch (Exception ex)
            {
                TxtError.Text = ex.Message;
            }
        }
    }
}
