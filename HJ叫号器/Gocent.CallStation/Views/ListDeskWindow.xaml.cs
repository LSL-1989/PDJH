using System;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Threading;
using Gocent.CallStation.Models;
using Gocent.CallStation.Services;
using Newtonsoft.Json.Linq;

namespace Gocent.CallStation.Views
{
    /// <summary>
    /// 列表版叫号窗：诊室名在顶栏中间完整换行，关闭键固定右侧。
    /// </summary>
    public partial class ListDeskWindow : Window
    {
        private readonly CallDeskService _desk = new CallDeskService();
        private readonly StompWebSocketClient _ws = new StompWebSocketClient();
        /** null 表示还没收到连接结果，离线文案保持样式里的次要色 */
        private bool? _linkUp;
        private DispatcherTimer? _listErrorTimer;

        public ListDeskWindow()
        {
            InitializeComponent();
            ThemeService.Changed += OnThemeChanged;
            SyncThemeButton();
            Loaded += OnLoaded;
            Closed += (_, __) =>
            {
                ThemeService.Changed -= OnThemeChanged;
                _ws.Disconnect();
                _listErrorTimer?.Stop();
                _desk.QueueChanged -= OnQueueChanged;
            };
            _desk.QueueChanged += OnQueueChanged;
        }

        private async void OnLoaded(object sender, RoutedEventArgs e)
        {
            var s = SessionManager.Current;
            if (s == null)
            {
                Close();
                return;
            }
            RefreshHeader();

            _ws.Connected += () => Dispatcher.Invoke(SetOnline);
            _ws.Disconnected += () => Dispatcher.Invoke(SetOffline);
            _ws.MessageReceived += OnWsMessage;
            _ws.Connect(ApiClient.WsUrl);

            await _desk.RefreshAsync();
        }

        private void RefreshHeader()
        {
            var s = SessionManager.Current;
            if (s == null) return;
            TxtRoom.Text = string.IsNullOrWhiteSpace(s.OfficeName) ? StationCopy.RoomNoun() : s.OfficeName;
            TxtDoctorName.Text = string.IsNullOrWhiteSpace(s.DoctorName) ? StationCopy.OperatorFallback : s.DoctorName;
            TxtDoctorTitle.Text = string.IsNullOrWhiteSpace(s.Title) ? "—" : s.Title;
            if (TxtTabWaiting != null)
                TxtTabWaiting.Text = StationCopy.WaitingTab;
            var pharmacy = StationCopy.IsPharmacy();
            if (PharmacyScanBar != null)
                PharmacyScanBar.Visibility = pharmacy ? Visibility.Visible : Visibility.Collapsed;
            if (BtnTabPreparing != null)
                BtnTabPreparing.Visibility = pharmacy ? Visibility.Visible : Visibility.Collapsed;
            if (TabHeaderGrid != null)
                TabHeaderGrid.Columns = pharmacy ? 4 : 3;
            if (!pharmacy && PanelPreparing != null && PanelPreparing.Visibility == Visibility.Visible)
                ShowQueueTab("wait");
        }

        private void TabWait_Click(object sender, RoutedEventArgs e) => ShowQueueTab("wait");
        private void TabPrep_Click(object sender, RoutedEventArgs e) => ShowQueueTab("prep");
        private void TabDone_Click(object sender, RoutedEventArgs e) => ShowQueueTab("done");
        private void TabPass_Click(object sender, RoutedEventArgs e) => ShowQueueTab("pass");

        /** 页签按稿面铺开：等候青下划线，已完成灰，过号红 */
        private void ShowQueueTab(string tab)
        {
            if (PanelWaiting == null) return;
            PanelWaiting.Visibility = tab == "wait" ? Visibility.Visible : Visibility.Collapsed;
            PanelPreparing.Visibility = tab == "prep" ? Visibility.Visible : Visibility.Collapsed;
            PanelDone.Visibility = tab == "done" ? Visibility.Visible : Visibility.Collapsed;
            PanelPassed.Visibility = tab == "pass" ? Visibility.Visible : Visibility.Collapsed;
            MarkWaiting.Visibility = tab == "wait" ? Visibility.Visible : Visibility.Collapsed;
            MarkPreparing.Visibility = tab == "prep" ? Visibility.Visible : Visibility.Collapsed;
            MarkDone.Visibility = tab == "done" ? Visibility.Visible : Visibility.Collapsed;
            MarkPassed.Visibility = tab == "pass" ? Visibility.Visible : Visibility.Collapsed;
            TxtTabWaiting.Foreground = (Brush)FindResource(tab == "wait" ? "PrimaryBrush" : "TextSecondaryBrush");
            if (TxtTabPreparing != null)
                TxtTabPreparing.Foreground = (Brush)FindResource(tab == "prep" ? "PrimaryBrush" : "TextSecondaryBrush");
            TxtTabDone.Foreground = (Brush)FindResource(tab == "done" ? "PrimaryBrush" : "TextSecondaryBrush");
            TxtTabPassed.Foreground = (Brush)FindResource("DangerBrush");
        }

        private void SetOnline()
        {
            _linkUp = true;
            OnlineDot.Fill = (Brush)FindResource("SuccessBrush");
            TxtOnline.Text = "在线";
            TxtOnline.Foreground = (Brush)FindResource("SuccessBrush");
        }

        private void SetOffline()
        {
            _linkUp = false;
            OnlineDot.Fill = (Brush)FindResource("DangerBrush");
            TxtOnline.Text = "离线";
            TxtOnline.Foreground = (Brush)FindResource("DangerBrush");
        }

        private void OnWsMessage(string body)
        {
            try
            {
                var jo = JObject.Parse(body);
                var type = (string?)jo["type"];
                if (type == "STATE_UPDATE" || type == "CALL_MSG")
                    Dispatcher.InvokeAsync(async () => await _desk.RefreshAsync());
            }
            catch
            {
                Dispatcher.InvokeAsync(async () => await _desk.RefreshAsync());
            }
        }

        private void OnQueueChanged()
        {
            Dispatcher.Invoke(() =>
            {
                var cur = _desk.CurrentPatient;
                if (cur != null)
                {
                    TxtCurrentNo.Text = cur.SeqDisplay;
                    var room = SessionManager.Current?.OfficeName;
                    TxtCurrentMeta.Text = string.IsNullOrWhiteSpace(room)
                        ? "请 " + cur.PatientName + " 就诊"
                        : "请 " + cur.PatientName + " 到" + room + "就诊";
                    var hint = cur.ExamHintDisplay;
                    TxtExamHint.Text = hint;
                    TxtExamHint.Visibility = string.IsNullOrWhiteSpace(hint) ? Visibility.Collapsed : Visibility.Visible;
                }
                else
                {
                    TxtCurrentNo.Text = "—";
                    TxtCurrentMeta.Text = "暂无正在呼叫的患者";
                    TxtExamHint.Text = "";
                    TxtExamHint.Visibility = Visibility.Collapsed;
                }
                ListWaiting.ItemsSource = null;
                ListWaiting.ItemsSource = _desk.Waiting;
                if (ListPreparing != null)
                {
                    ListPreparing.ItemsSource = null;
                    ListPreparing.ItemsSource = _desk.Preparing;
                }
                ListDone.ItemsSource = null;
                ListDone.ItemsSource = _desk.Completed;
                ListPassed.ItemsSource = null;
                ListPassed.ItemsSource = _desk.Passed;
            });
        }

        /** 顶栏齿轮：弹出当前可切换的主题名，以及简易模式 */
        private void Gear_Click(object sender, RoutedEventArgs e)
        {
            if (GearPopup != null) GearPopup.IsOpen = !GearPopup.IsOpen;
        }

        private void Theme_Click(object sender, RoutedEventArgs e)
        {
            if (GearPopup != null) GearPopup.IsOpen = false;
            ThemeService.Toggle();
        }

        /** 外框圆角裁切，避免子元素把四个方角画出来 */
        private void WindowShell_SizeChanged(object sender, SizeChangedEventArgs e)
        {
            if (sender is not Border shell || shell.ActualWidth <= 0 || shell.ActualHeight <= 0) return;
            shell.Clip = new RectangleGeometry(new Rect(0, 0, shell.ActualWidth, shell.ActualHeight), 8, 8);
        }

        private void OnThemeChanged()
        {
            if (!Dispatcher.CheckAccess())
            {
                Dispatcher.BeginInvoke(new Action(OnThemeChanged));
                return;
            }
            SyncThemeButton();
            if (_linkUp == true) SetOnline();
            else if (_linkUp == false) SetOffline();
        }

        private void SyncThemeButton()
        {
            if (BtnGearTheme != null)
                BtnGearTheme.Content = ThemeService.NextLabel;
        }

        private void Header_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
        {
            if (e.ChangedButton == MouseButton.Left) DragMove();
        }

        private async void CallNext_Click(object sender, RoutedEventArgs e) => await RunAction(() => _desk.CallNextAsync(), maskError: true);
        private async void Recall_Click(object sender, RoutedEventArgs e) => await RunAction(() => _desk.RecallAsync(), maskError: true);
        private async void Pass_Click(object sender, RoutedEventArgs e) => await RunAction(() => _desk.PassAsync(), maskError: true);
        private async void Complete_Click(object sender, RoutedEventArgs e) =>
            await RunAction(() => _desk.CompleteAsync(), showSuccessHint: true, maskError: true);

        private async void CallSpecific_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.Tag is CallListItem item)
                await RunAction(() => _desk.CallSpecificAsync(item));
        }

        private async void MarkReady_Click(object sender, RoutedEventArgs e)
        {
            if (sender is Button btn && btn.Tag is CallListItem item)
                await RunAction(() => _desk.MarkReadyAsync(item));
        }

        private async void ScanCall_Click(object sender, RoutedEventArgs e)
        {
            await RunAction(() => _desk.CallByPatientCodeAsync(TxtScanCode.Text ?? ""));
            TxtScanCode.Clear();
            TxtScanCode.Focus();
        }

        private async void ScanCode_KeyDown(object sender, KeyEventArgs e)
        {
            if (e.Key != Key.Enter) return;
            e.Handled = true;
            await RunAction(() => _desk.CallByPatientCodeAsync(TxtScanCode.Text ?? ""));
            TxtScanCode.Clear();
        }

        private async System.Threading.Tasks.Task RunAction(
            Func<System.Threading.Tasks.Task<(bool ok, string msg)>> action,
            bool showSuccessHint = false,
            bool maskError = false)
        {
            try
            {
                var (ok, msg) = await action();
                if (string.IsNullOrEmpty(msg)) return;
                if (!ok)
                {
                    if (maskError) ShowListError(msg);
                    else MessageBox.Show(this, msg, "提示", MessageBoxButton.OK, MessageBoxImage.Information);
                    return;
                }
                if (showSuccessHint && msg != StationCopy.Done)
                    MessageBox.Show(this, msg, "提示", MessageBoxButton.OK, MessageBoxImage.Information);
            }
            catch (Exception ex)
            {
                if (maskError) ShowListError(ex.Message);
                else MessageBox.Show(this, ex.Message, "错误", MessageBoxButton.OK, MessageBoxImage.Error);
            }
        }

        /** 患者列表上的红色错误遮罩；再次失败会刷新文案并重新计 3 秒 */
        private void ShowListError(string msg)
        {
            if (TxtListError == null || ListErrorMask == null) return;
            TxtListError.Text = msg;
            ListErrorMask.Visibility = Visibility.Visible;
            if (_listErrorTimer == null)
            {
                _listErrorTimer = new DispatcherTimer { Interval = TimeSpan.FromSeconds(3) };
                _listErrorTimer.Tick += (_, __) =>
                {
                    _listErrorTimer.Stop();
                    ListErrorMask.Visibility = Visibility.Collapsed;
                };
            }
            _listErrorTimer.Stop();
            _listErrorTimer.Start();
        }

        private void SwitchCompact_Click(object sender, RoutedEventArgs e)
        {
            if (GearPopup != null) GearPopup.IsOpen = false;
            var compact = new CompactFloatWindow(_desk, _ws, this)
            {
                Left = Left + Width + 8,
                Top = Top + 80
            };
            compact.Show();
            Hide();
        }

        private async void Close_Click(object sender, RoutedEventArgs e)
        {
            try { await _desk.RefreshAsync(); } catch { /* ignore */ }
            if (!ExitConfirm.Ask(this, _desk)) return;

            await SessionManager.LeaveAndClearAsync();
            _ws.Disconnect();
            new LoginWindow().Show();
            Close();
        }
    }
}
