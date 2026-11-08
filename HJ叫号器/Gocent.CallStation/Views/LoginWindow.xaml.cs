using System;
using System.Collections.Generic;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Shapes;
using Gocent.CallStation.Models;
using Gocent.CallStation.Services;

namespace Gocent.CallStation.Views
{
    /// <summary>
    /// 登录窗：医院名、工号/手机号、密码眼睛显隐、忘记密码、深浅色切换。
    /// 登录成功后确认上岗窗口，进入统一列表版叫号器（药房/体检按场景显示附加区）。
    /// </summary>
    public partial class LoginWindow : Window
    {
        private static readonly Geometry EyeOpen = Geometry.Parse(
            "M8,1 C3.5,1 0.5,5.5 0,6 C0.5,6.5 3.5,11 8,11 C12.5,11 15.5,6.5 16,6 C15.5,5.5 12.5,1 8,1 Z M8,3.5 A2.5,2.5 0 1 1 8,8.5 A2.5,2.5 0 1 1 8,3.5 Z");
        private static readonly Geometry EyeOff = Geometry.Parse(
            "M1,1 L15,11 M2,4.5 C1,5.2 0.3,5.8 0,6 C0.5,6.5 3.5,11 8,11 C9.2,11 10.3,10.7 11.2,10.2 M14,8.2 C15.2,7.2 15.8,6.3 16,6 C15.5,5.5 12.5,1 8,1 C6.5,1 5.2,1.5 4.1,2.2");

        public LoginWindow()
        {
            InitializeComponent();
            ThemeService.Changed += OnThemeChanged;
            Closed += (_, __) => ThemeService.Changed -= OnThemeChanged;
            SyncThemeButton();
            SyncPlaceholders();
            Loaded += async (_, __) =>
            {
                // 仅成功时写入；失败保留 XAML 默认文案，避免断网刷成占位名
                try
                {
                    var name = await DoctorApi.GetHospitalNameAsync();
                    if (!string.IsNullOrWhiteSpace(name))
                        TxtHospital.Text = name;
                }
                catch { /* 保留界面已有医院名 */ }
            };
        }

        /** 登录卡片裁成圆角，窗口四角不再留下方角 */
        private void WindowShell_SizeChanged(object sender, SizeChangedEventArgs e)
        {
            if (sender is not Border shell || shell.ActualWidth <= 0 || shell.ActualHeight <= 0) return;
            shell.Clip = new RectangleGeometry(new Rect(0, 0, shell.ActualWidth, shell.ActualHeight), 8, 8);
        }

        private void Window_PreviewKeyDown(object sender, KeyEventArgs e)
        {
            if (e.Key != Key.Enter) return;
            // 无边框透明窗上 IsDefault 偶发无效，回车统一走登录
            e.Handled = true;
            Login_Click(BtnLogin, new RoutedEventArgs());
        }

        private void Title_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
        {
            if (e.ChangedButton == MouseButton.Left) DragMove();
        }

        private void Close_Click(object sender, RoutedEventArgs e) => Close();

        private void Theme_Click(object sender, RoutedEventArgs e) => ThemeService.Toggle();

        private void OnThemeChanged()
        {
            if (!Dispatcher.CheckAccess())
            {
                Dispatcher.BeginInvoke(new Action(OnThemeChanged));
                return;
            }
            SyncThemeButton();
            if (EyeIcon == null) return;
            EyeIcon.Fill = System.Windows.Media.Brushes.Transparent;
            EyeIcon.Stroke = (Brush)FindResource(
                BtnTogglePwd.IsChecked == true ? "PrimaryBrush" : "PlaceholderBrush");
        }

        private void SyncThemeButton()
        {
            if (TxtThemeLabel != null) TxtThemeLabel.Text = ThemeService.NextLabel;
        }

        private void Account_TextChanged(object sender, System.Windows.Controls.TextChangedEventArgs e) =>
            SyncPlaceholders();

        private void Password_Changed(object sender, RoutedEventArgs e) => SyncPlaceholders();

        private void Field_FocusChanged(object sender, RoutedEventArgs e) => SyncPlaceholders();

        /** 空且未聚焦时显示框内提示，避免和光标叠在一起 */
        private void SyncPlaceholders()
        {
            if (PhAccount != null)
            {
                PhAccount.Visibility = string.IsNullOrEmpty(TxtAccount.Text) && !TxtAccount.IsFocused
                    ? Visibility.Visible
                    : Visibility.Collapsed;
            }
            if (PhPassword == null) return;
            var pwdBoxFocused = TxtPassword.Visibility == Visibility.Visible
                ? TxtPassword.IsFocused
                : TxtPasswordVisible.IsFocused;
            PhPassword.Visibility = string.IsNullOrEmpty(GetPassword()) && !pwdBoxFocused
                ? Visibility.Visible
                : Visibility.Collapsed;
        }

        private void TogglePwd_Checked(object sender, RoutedEventArgs e)
        {
            TxtPasswordVisible.Text = TxtPassword.Password;
            TxtPassword.Visibility = Visibility.Collapsed;
            TxtPasswordVisible.Visibility = Visibility.Visible;
            EyeIcon.Data = EyeOff;
            EyeIcon.Fill = System.Windows.Media.Brushes.Transparent;
            EyeIcon.Stroke = (Brush)FindResource("PrimaryBrush");
            SyncPlaceholders();
        }

        private void TogglePwd_Unchecked(object sender, RoutedEventArgs e)
        {
            TxtPassword.Password = TxtPasswordVisible.Text;
            TxtPasswordVisible.Visibility = Visibility.Collapsed;
            TxtPassword.Visibility = Visibility.Visible;
            EyeIcon.Data = EyeOpen;
            EyeIcon.Fill = System.Windows.Media.Brushes.Transparent;
            EyeIcon.Stroke = (Brush)FindResource("PlaceholderBrush");
            SyncPlaceholders();
        }

        private string GetPassword() =>
            TxtPasswordVisible.Visibility == Visibility.Visible
                ? TxtPasswordVisible.Text
                : TxtPassword.Password;

        private async void Login_Click(object sender, RoutedEventArgs e)
        {
            TxtError.Text = "";
            var account = (TxtAccount.Text ?? "").Trim();
            var pwd = GetPassword() ?? "";
            if (string.IsNullOrEmpty(account) || string.IsNullOrEmpty(pwd))
            {
                TxtError.Text = "请输入用户名和密码";
                return;
            }

            try
            {
                var res = await DoctorApi.LoginAsync(account, pwd);
                if (!res.Success || res.Data == null)
                {
                    TxtError.Text = string.IsNullOrWhiteSpace(res.Msg) ? "登录失败" : res.Msg;
                    return;
                }

                var data = res.Data;
                var stationMode = string.IsNullOrWhiteSpace(data.StationMode) ? "CLINIC" : data.StationMode.Trim().ToUpperInvariant();

                if (data.MustChangePassword == 1)
                {
                    var ask = MessageBox.Show(this,
                        "首次登录或密码已重置，是否立即修改密码？",
                        "修改密码",
                        MessageBoxButton.YesNo,
                        MessageBoxImage.Question);
                    if (ask == MessageBoxResult.Yes)
                    {
                        var dlg = new ChangePasswordWindow(data.DoctorId, force: true) { Owner = this };
                        if (dlg.ShowDialog() != true)
                        {
                            TxtError.Text = "请先修改密码后再进入叫号器";
                            return;
                        }
                    }
                }

                if (data.Rooms == null || data.Rooms.Count == 0)
                {
                    TxtError.Text = StationCopy.NoRooms(stationMode);
                    return;
                }

                SessionManager.Current = new DoctorSession
                {
                    DoctorId = data.DoctorId,
                    WorkNo = data.WorkNo ?? "",
                    DoctorName = data.Name ?? StationCopy.OperatorFallback,
                    Title = data.Title ?? "",
                    DeptId = data.DeptId ?? "",
                    StationMode = stationMode,
                    BusinessType = data.BusinessType ?? "",
                    Rooms = data.Rooms ?? new List<OfficeRoomModel>()
                };

                var roomDlg = new RoomSelectWindow(
                    data.DefaultRoom,
                    data.Rooms ?? new List<OfficeRoomModel>())
                { Owner = this };
                if (roomDlg.ShowDialog() != true || roomDlg.SelectedRoom == null)
                {
                    SessionManager.Clear();
                    TxtError.Text = StationCopy.NeedRoom(stationMode);
                    return;
                }

                var bind = await DoctorApi.BindSittingRoomAsync(data.DoctorId, roomDlg.SelectedRoom.Id);
                if (!bind.Success)
                {
                    TxtError.Text = string.IsNullOrWhiteSpace(bind.Msg) ? StationCopy.BindFailed : bind.Msg;
                    SessionManager.Clear();
                    return;
                }

                SessionManager.ApplyRoom(bind.Data ?? roomDlg.SelectedRoom);

                var desk = new ListDeskWindow();
                desk.Show();
                Close();
            }
            catch (Exception ex)
            {
                TxtError.Text = "无法连接服务器：" + ex.Message;
            }
        }

        private async void Forgot_Click(object sender, MouseButtonEventArgs e)
        {
            TxtError.Text = "";
            var account = (TxtAccount.Text ?? "").Trim();
            if (string.IsNullOrEmpty(account))
            {
                TxtError.Text = "请先输入工号或手机号";
                return;
            }
            try
            {
                var res = await DoctorApi.RequestPasswordResetAsync(account);
                if (res.Success)
                {
                    MessageBox.Show(this,
                        "已提交密码重置申请，后台管理员可在「用户管理」中重置密码。",
                        "忘记密码",
                        MessageBoxButton.OK,
                        MessageBoxImage.Information);
                }
                else
                {
                    TxtError.Text = string.IsNullOrWhiteSpace(res.Msg) ? "申请失败" : res.Msg;
                }
            }
            catch (Exception ex)
            {
                TxtError.Text = "申请失败：" + ex.Message;
            }
        }
    }
}
