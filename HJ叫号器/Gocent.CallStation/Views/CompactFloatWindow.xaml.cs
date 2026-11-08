using System;
using System.Windows;
using System.Windows.Input;
using System.Windows.Threading;
using Gocent.CallStation.Services;

namespace Gocent.CallStation.Views
{
    /// <summary>
    /// 精简版 3D 悬浮圆；外圈留白完整显示阴影；退出按待诊业务确认。
    /// </summary>
    public partial class CompactFloatWindow : Window
    {
        /** 168 圆 + 上下左右各 18 阴影留白 */
        private const double CollapsedWidth = 210;
        private const double CollapsedHeight = 210;
        /** 留白*2 + 圆 + 间距 + 按钮列（字号加大后列更宽更高） */
        private const double ExpandedWidth = 390;
        /** 少一颗主题按钮后，展开高度跟着缩短 */
        private const double ExpandedHeight = 390;

        private readonly CallDeskService _desk;
        private readonly StompWebSocketClient _ws;
        private readonly ListDeskWindow _listWindow;
        private bool _exiting;
        private bool _expanded;
        private DispatcherTimer? _ballErrorTimer;

        public CompactFloatWindow(CallDeskService desk, StompWebSocketClient ws, ListDeskWindow listWindow)
        {
            InitializeComponent();
            _desk = desk;
            _ws = ws;
            _listWindow = listWindow;
            Loaded += (_, __) =>
            {
                BtnComplete.Content = StationCopy.CompleteAction;
                CollapsePanel();
                RefreshUi();
                _desk.QueueChanged += OnQueueChanged;
            };
            Closed += (_, __) =>
            {
                _ballErrorTimer?.Stop();
                _desk.QueueChanged -= OnQueueChanged;
            };
        }

        private void OnQueueChanged() => Dispatcher.Invoke(RefreshUi);

        private void RefreshUi()
        {
            var cur = _desk.CurrentPatient;
            if (cur != null)
            {
                TxtName.Text = cur.PatientName;
                TxtMeta.Text = string.IsNullOrWhiteSpace(cur.ExamHintDisplay)
                    ? $"{cur.AgeDisplay} · {cur.SexDisplay} · {cur.SeqDisplay}"
                    : cur.ExamHintDisplay;
            }
            else
            {
                TxtName.Text = "暂无患者";
                TxtMeta.Text = "- · - · -";
            }
        }

        private void HoverHost_MouseEnter(object sender, MouseEventArgs e) => ExpandPanel();

        private void HoverHost_MouseLeave(object sender, MouseEventArgs e)
        {
            if (HoverHost.IsMouseOver) return;
            CollapsePanel();
        }

        private void ExpandPanel()
        {
            if (_expanded) return;
            _expanded = true;
            Width = ExpandedWidth;
            Height = ExpandedHeight;
            BtnPanel.Visibility = Visibility.Visible;
        }

        private void CollapsePanel()
        {
            _expanded = false;
            BtnPanel.Visibility = Visibility.Collapsed;
            Width = CollapsedWidth;
            Height = CollapsedHeight;
        }

        private void Circle_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
        {
            if (e.ChangedButton == MouseButton.Left) DragMove();
        }

        private async void CallNext_Click(object sender, RoutedEventArgs e) => await Run(() => _desk.CallNextAsync());
        private async void Recall_Click(object sender, RoutedEventArgs e) => await Run(() => _desk.RecallAsync());
        private async void Pass_Click(object sender, RoutedEventArgs e) => await Run(() => _desk.PassAsync());
        private async void Complete_Click(object sender, RoutedEventArgs e) =>
            await Run(() => _desk.CompleteAsync(), showSuccessHint: true);

        private async System.Threading.Tasks.Task Run(
            Func<System.Threading.Tasks.Task<(bool ok, string msg)>> action,
            bool showSuccessHint = false)
        {
            try
            {
                var (ok, msg) = await action();
                if (string.IsNullOrEmpty(msg)) return;
                if (!ok)
                {
                    ShowBallError(msg);
                    return;
                }
                if (showSuccessHint && msg != StationCopy.Done)
                    MessageBox.Show(this, msg, "提示", MessageBoxButton.OK, MessageBoxImage.Information);
            }
            catch (Exception ex)
            {
                ShowBallError(ex.Message);
            }
        }

        /** 悬浮球上的红色错误遮罩；再次失败会刷新文案并重新计 3 秒 */
        private void ShowBallError(string msg)
        {
            if (TxtBallError == null || BallErrorMask == null) return;
            TxtBallError.Text = msg;
            BallErrorMask.Visibility = Visibility.Visible;
            if (_ballErrorTimer == null)
            {
                _ballErrorTimer = new DispatcherTimer { Interval = TimeSpan.FromSeconds(3) };
                _ballErrorTimer.Tick += (_, __) =>
                {
                    _ballErrorTimer.Stop();
                    BallErrorMask.Visibility = Visibility.Collapsed;
                };
            }
            _ballErrorTimer.Stop();
            _ballErrorTimer.Start();
        }

        private void Switch_Click(object sender, RoutedEventArgs e)
        {
            _exiting = true;
            _listWindow.Show();
            _listWindow.Activate();
            Close();
        }

        private async void Exit_Click(object sender, RoutedEventArgs e)
        {
            try { await _desk.RefreshAsync(); } catch { /* 用本地缓存继续确认 */ }
            if (!ExitConfirm.Ask(this, _desk)) return;

            _exiting = true;
            await SessionManager.LeaveAndClearAsync();
            _ws.Disconnect();
            new LoginWindow().Show();
            _listWindow.Close();
            Close();
        }

        protected override void OnClosing(System.ComponentModel.CancelEventArgs e)
        {
            if (!_exiting) _listWindow.Show();
            base.OnClosing(e);
        }
    }
}
