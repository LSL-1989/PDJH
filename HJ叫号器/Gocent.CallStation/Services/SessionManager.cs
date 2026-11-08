using System.Threading.Tasks;
using Gocent.CallStation.Models;

namespace Gocent.CallStation.Services
{
    /// <summary>当前登录会话（进程内单例）</summary>
    public static class SessionManager
    {
        public static DoctorSession? Current { get; set; }

        public static void Clear() => Current = null;

        public static void ApplyRoom(OfficeRoomModel room)
        {
            if (Current == null || room == null) return;
            Current.OfficeRoomId = room.Id;
            Current.OfficeName = string.IsNullOrWhiteSpace(room.Name) ? StationCopy.RoomNoun() : room.Name;
            Current.ScreenCode = room.ScreenId ?? "";
            if (!string.IsNullOrWhiteSpace(room.BusinessType))
            {
                Current.BusinessType = room.BusinessType;
                Current.StationMode = StationCopy.ModeFromBusinessType(room.BusinessType);
            }
        }

        /// <summary>
        /// 通知服务端离诊（doctorId + officeRoomId）并清除本地会话。
        /// 不使用 ConfigureAwait(false)，确保 UI 线程 await 能等完 HTTP 再关窗。
        /// </summary>
        public static async Task LeaveAndClearAsync()
        {
            var session = Current;
            if (session != null && !string.IsNullOrWhiteSpace(session.DoctorId))
            {
                try
                {
                    await DoctorApi.LeaveSittingRoomAsync(session.DoctorId, session.OfficeRoomId);
                }
                catch
                {
                    /* 离诊失败不阻塞退出本地界面 */
                }
            }
            Clear();
        }
    }
}
