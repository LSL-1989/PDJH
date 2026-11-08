using System.Windows;

namespace Gocent.CallStation.Services
{
    /// <summary>
    /// 退出叫号器确认：有等候或办理中时给出警告。
    /// </summary>
    public static class ExitConfirm
    {
        public static bool Ask(Window owner, CallDeskService desk) =>
            Ask(owner, desk.Waiting?.Count ?? 0, desk.CurrentPatient != null);

        public static bool Ask(Window owner, int waiting, bool inService)
        {
            var room = StationCopy.RoomNoun();
            string msg;
            MessageBoxImage icon;
            if (waiting > 0 || inService)
            {
                var parts = new System.Collections.Generic.List<string>();
                if (inService) parts.Add("当前有正在办理的患者");
                if (waiting > 0) parts.Add($"还有 {waiting} 位等候患者");
                msg = string.Join("，", parts) + "，未完成办理。确定要退出叫号器吗？";
                icon = MessageBoxImage.Warning;
            }
            else
            {
                msg = $"当前{room}队列中已无等候患者，是否退出叫号器？";
                icon = MessageBoxImage.Question;
            }

            return MessageBox.Show(owner, msg, "退出确认", MessageBoxButton.YesNo, icon) == MessageBoxResult.Yes;
        }
    }
}
