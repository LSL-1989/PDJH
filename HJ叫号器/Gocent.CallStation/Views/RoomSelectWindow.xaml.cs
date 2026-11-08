using System.Collections.Generic;
using System.Linq;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using Gocent.CallStation.Models;
using Gocent.CallStation.Services;

namespace Gocent.CallStation.Views
{
    /// <summary>
    /// 登录后确认上岗窗口。回车等同确认上岗；文案按叫号器场景（诊室/窗口/检查室）切换。
    /// </summary>
    public partial class RoomSelectWindow : Window
    {
        public OfficeRoomModel? SelectedRoom { get; private set; }
        private bool _confirming;

        public RoomSelectWindow(OfficeRoomModel? defaultRoom, List<OfficeRoomModel> rooms)
        {
            InitializeComponent();
            Title = StationCopy.ConfirmTitle();
            TxtTitle.Text = StationCopy.ConfirmTitle();
            TxtHint.Text = StationCopy.ConfirmHint();
            TxtRoomLabel.Text = StationCopy.ConfirmRoomLabel();
            BtnOk.Content = StationCopy.ConfirmOk;
            CmbRoom.ItemsSource = rooms ?? new List<OfficeRoomModel>();
            if (defaultRoom != null)
            {
                var match = rooms?.FirstOrDefault(r => r.Id == defaultRoom.Id);
                CmbRoom.SelectedItem = match ?? defaultRoom;
            }
            else if (rooms != null && rooms.Count > 0)
            {
                CmbRoom.SelectedIndex = 0;
            }
        }

        /** 无边框透明窗上 IsDefault 偶发无效，回车统一走确认上岗。下拉展开时先采纳高亮项。 */
        private void Window_PreviewKeyDown(object sender, KeyEventArgs e)
        {
            if (e.Key != Key.Enter) return;
            if (CmbRoom.IsDropDownOpen)
            {
                var picked = HighlightedRoom();
                if (picked != null) CmbRoom.SelectedItem = picked;
                CmbRoom.IsDropDownOpen = false;
            }
            e.Handled = true;
            Ok_Click(BtnOk, new RoutedEventArgs());
        }

        /** 下拉列表里当前高亮的诊室；没有高亮项时返回 null */
        private OfficeRoomModel? HighlightedRoom()
        {
            foreach (var item in CmbRoom.Items)
            {
                if (CmbRoom.ItemContainerGenerator.ContainerFromItem(item) is ComboBoxItem box
                    && box.IsHighlighted
                    && item is OfficeRoomModel room)
                    return room;
            }
            return null;
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

        private void Ok_Click(object sender, RoutedEventArgs e)
        {
            if (_confirming) return;
            if (CmbRoom.SelectedItem is not OfficeRoomModel room || string.IsNullOrWhiteSpace(room.Id))
            {
                TxtError.Text = "请选择" + StationCopy.RoomNoun();
                return;
            }
            _confirming = true;
            SelectedRoom = room;
            DialogResult = true;
            Close();
        }
    }
}
