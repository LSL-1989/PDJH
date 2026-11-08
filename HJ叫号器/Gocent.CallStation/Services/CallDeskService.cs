using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Gocent.CallStation.Models;

namespace Gocent.CallStation.Services
{
    /// <summary>
    /// 叫号器统一业务：门诊/检验/体检走等候池；药房额外配药中 + 扫码叫号。
    /// 完成/过号只结束当前，不自动下一位；呼叫须先点完成。
    /// </summary>
    public class CallDeskService
    {
        public CallListItem? CurrentPatient { get; private set; }
        public List<CallListItem> Preparing { get; private set; } = new List<CallListItem>();
        public List<CallListItem> Waiting { get; private set; } = new List<CallListItem>();
        public List<CallListItem> Completed { get; private set; } = new List<CallListItem>();
        public List<CallListItem> Passed { get; private set; } = new List<CallListItem>();

        public event Action? QueueChanged;

        public async Task RefreshAsync()
        {
            var s = SessionManager.Current;
            if (s == null) return;

            if (StationCopy.IsPharmacy())
            {
                var deskTask = CallApi.PharmacyDeskQueueAsync(s.OfficeRoomId);
                var callingTask = CallApi.GetDocCallListAsync(s.DeptId, s.OfficeRoomId, 1);
                var doneTask = CallApi.GetDocCallListAsync(s.DeptId, s.OfficeRoomId, 4);
                var passTask = CallApi.GetDocCallListAsync(s.DeptId, s.OfficeRoomId, 6);
                await Task.WhenAll(deskTask, callingTask, doneTask, passTask).ConfigureAwait(false);

                var desk = deskTask.Result.Success && deskTask.Result.Data != null
                    ? deskTask.Result.Data
                    : new List<CallListItem>();
                Preparing = desk
                    .Where(c => string.Equals(c.TriageState, "PREPARING", StringComparison.OrdinalIgnoreCase))
                    .OrderBy(c => c.Xh ?? int.MaxValue).ThenBy(c => c.Id)
                    .ToList();
                Waiting = desk
                    .Where(c => string.Equals(c.TriageState, "WAITING", StringComparison.OrdinalIgnoreCase)
                                || string.IsNullOrWhiteSpace(c.TriageState))
                    .OrderBy(c => c.Xh ?? int.MaxValue).ThenBy(c => c.Id)
                    .ToList();
                Completed = SortQueue(doneTask.Result.Success ? doneTask.Result.Data : null);
                Passed = SortQueue(passTask.Result.Success ? passTask.Result.Data : null);
                var calling = SortQueue(callingTask.Result.Success ? callingTask.Result.Data : null);
                CurrentPatient = calling.Count > 0 ? calling[0] : null;
            }
            else
            {
                Preparing = new List<CallListItem>();
                var waitingTask = CallApi.GetDocCallListAsync(s.DeptId, s.OfficeRoomId, 0);
                var callingTask = CallApi.GetDocCallListAsync(s.DeptId, s.OfficeRoomId, 1);
                var doneTask = CallApi.GetDocCallListAsync(s.DeptId, s.OfficeRoomId, 4);
                var passTask = CallApi.GetDocCallListAsync(s.DeptId, s.OfficeRoomId, 6);
                await Task.WhenAll(waitingTask, callingTask, doneTask, passTask).ConfigureAwait(false);
                Waiting = SortQueue(waitingTask.Result.Success ? waitingTask.Result.Data : null);
                Completed = SortQueue(doneTask.Result.Success ? doneTask.Result.Data : null);
                Passed = SortQueue(passTask.Result.Success ? passTask.Result.Data : null);
                var calling = SortQueue(callingTask.Result.Success ? callingTask.Result.Data : null);
                CurrentPatient = calling.Count > 0 ? calling[0] : null;
            }

            QueueChanged?.Invoke();
        }

        public async Task<(bool ok, string msg)> CompleteAsync()
        {
            if (CurrentPatient == null) return (false, StationCopy.NoCurrent);
            var examItem = CurrentPatient.ExamItemName;
            var nextHint = CurrentPatient.ExamNextHint;
            var ok = await CallApi.EditCallMsgStateAsync(CurrentPatient.Id, 4).ConfigureAwait(false);
            if (!ok.Success) return (false, ok.Msg ?? "完成失败");
            CurrentPatient = null;
            await RefreshAsync().ConfigureAwait(false);
            if (!string.IsNullOrWhiteSpace(nextHint))
                return (true, StationCopy.Done + "。" + nextHint);
            if (!string.IsNullOrWhiteSpace(examItem))
                return (true, "已完成 " + examItem);
            return (true, StationCopy.Done);
        }

        public async Task<(bool ok, string msg)> CallNextAsync()
        {
            var s = SessionManager.Current;
            if (s == null) return (false, "未登录");

            await RefreshAsync().ConfigureAwait(false);
            if (CurrentPatient != null)
                return (false, StationCopy.MustFinishBeforeNext);
            if (Waiting.Count == 0)
                return (false, StationCopy.IsPharmacy() ? "暂无配药完成可叫号的患者" : StationCopy.NoWaiting);

            var next = Waiting[0];
            var ok = await CallApi.EditCallMsgStateAsync(next.Id, 1, s.OfficeRoomId, s.DoctorId).ConfigureAwait(false);
            if (!ok.Success) return (false, ok.Msg ?? "呼叫失败");

            CurrentPatient = next;
            await EmitCallAsync(next).ConfigureAwait(false);
            await RefreshAsync().ConfigureAwait(false);
            return (true, "正在呼叫 " + next.PatientName);
        }

        public async Task<(bool ok, string msg)> RecallAsync()
        {
            if (CurrentPatient == null) return (false, "当前没有正在呼叫的患者");
            await EmitCallAsync(CurrentPatient).ConfigureAwait(false);
            return (true, "正在重呼 " + CurrentPatient.PatientName);
        }

        public async Task<(bool ok, string msg)> PassAsync()
        {
            if (CurrentPatient == null) return (false, StationCopy.NoCurrent);
            var ok = await CallApi.EditCallMsgStateAsync(CurrentPatient.Id, 6).ConfigureAwait(false);
            if (!ok.Success) return (false, ok.Msg ?? "过号失败");
            CurrentPatient = null;
            await RefreshAsync().ConfigureAwait(false);
            return (true, "已过号");
        }

        public async Task<(bool ok, string msg)> CallSpecificAsync(CallListItem item)
        {
            var s = SessionManager.Current;
            if (s == null) return (false, "未登录");
            if (item == null) return (false, "请选择患者");
            if (string.Equals(item.TriageState, "PREPARING", StringComparison.OrdinalIgnoreCase))
                return (false, "该患者仍在配药中，请先标记配药完成或等待 HIS 回写");

            await RefreshAsync().ConfigureAwait(false);
            if (CurrentPatient != null)
                return (false, StationCopy.MustFinishBeforeNamed);

            var ok = await CallApi.EditCallMsgStateAsync(item.Id, 1, s.OfficeRoomId, s.DoctorId).ConfigureAwait(false);
            if (!ok.Success) return (false, ok.Msg ?? "点名失败");

            CurrentPatient = item;
            await EmitCallAsync(item).ConfigureAwait(false);
            await RefreshAsync().ConfigureAwait(false);
            return (true, "正在呼叫 " + item.PatientName);
        }

        public async Task<(bool ok, string msg)> CallByPatientCodeAsync(string patientCode)
        {
            if (string.IsNullOrWhiteSpace(patientCode)) return (false, "请扫码或输入患者编号");
            await RefreshAsync().ConfigureAwait(false);
            var code = patientCode.Trim();
            if (Preparing.Any(c => MatchCode(c, code)))
                return (false, "该患者仍在配药中，请先「配完」或等待系统回写");
            var target = Waiting.FirstOrDefault(c => MatchCode(c, code));
            if (target == null)
                return (false, "未在本窗口找到可叫号记录：" + code);
            return await CallSpecificAsync(target).ConfigureAwait(false);
        }

        public async Task<(bool ok, string msg)> MarkReadyAsync(CallListItem item)
        {
            if (item == null) return (false, "请选择患者");
            var ok = await CallApi.PharmacyMarkReadyAsync(item.Id).ConfigureAwait(false);
            if (!ok.Success) return (false, ok.Msg ?? "标记失败");
            await RefreshAsync().ConfigureAwait(false);
            return (true, "已进入等候队列");
        }

        private static bool MatchCode(CallListItem c, string code) =>
            string.Equals(c.ShowNo, code, StringComparison.OrdinalIgnoreCase)
            || string.Equals(c.PatientCode, code, StringComparison.OrdinalIgnoreCase);

        private async Task EmitCallAsync(CallListItem record)
        {
            var s = SessionManager.Current;
            if (s == null) return;
            await CallApi.DocCallMsgAsync($"{record.Id}|{s.OfficeName}", s.ScreenCode).ConfigureAwait(false);
        }

        private static List<CallListItem> SortQueue(List<CallListItem>? raw)
        {
            if (raw == null || raw.Count == 0) return new List<CallListItem>();
            return raw.OrderBy(c => c.Xh ?? int.MaxValue).ThenBy(c => c.Id).ToList();
        }
    }
}
