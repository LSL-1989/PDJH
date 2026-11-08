using System.Collections.Generic;
using System.Threading.Tasks;
using Gocent.CallStation.Models;

namespace Gocent.CallStation.Services
{
    /// <summary>
    /// 叫号器认证与档案 API（/Doctor/*）
    /// </summary>
    public static class DoctorApi
    {
        public static Task<ApiResult<DoctorLoginResult>> LoginAsync(string account, string password) =>
            ApiClient.PostAsync<DoctorLoginResult>("/Doctor/Login", new { account, password });

        public static Task<ApiResult<bool>> ChangePasswordAsync(string doctorId, string? oldPassword, string newPassword) =>
            ApiClient.PostAsync<bool>("/Doctor/ChangePassword", new
            {
                doctorId,
                oldPassword = oldPassword ?? "",
                newPassword
            });

        public static Task<ApiResult<bool>> RequestPasswordResetAsync(string account) =>
            ApiClient.PostAsync<bool>("/Doctor/RequestPasswordReset", new { account });

        /// <summary>确认坐诊诊室：写 office_room.doctor_id，门边屏展示该医生</summary>
        public static Task<ApiResult<OfficeRoomModel>> BindSittingRoomAsync(string doctorId, string officeRoomId) =>
            ApiClient.PostAsync<OfficeRoomModel>("/Doctor/BindSittingRoom", new { doctorId, officeRoomId });

        /// <summary>退出叫号器离岗：清除上岗绑定，门边屏显示暂无值班</summary>
        public static Task<ApiResult<bool>> LeaveSittingRoomAsync(string doctorId, string? officeRoomId = null) =>
            ApiClient.PostAsync<bool>("/Doctor/LeaveSittingRoom", new
            {
                doctorId,
                officeRoomId = officeRoomId ?? ""
            });

        /// <summary>
        /// 读取 sys_config.HospitalName；失败返回 null（调用方勿用默认名覆盖已展示的名称）
        /// </summary>
        public static async Task<string?> GetHospitalNameAsync()
        {
            var res = await ApiClient.GetAsync<SysConfigModel>("/SystemConfig/GetConfigByKey?key=HospitalName")
                .ConfigureAwait(false);
            if (res.Success && res.Data != null && !string.IsNullOrWhiteSpace(res.Data.ConfigValue))
            {
                return res.Data.ConfigValue.Trim();
            }
            return null;
        }
    }

    /// <summary>
    /// 叫号 API（/DoctorCall/*），与原 Vue 工作站共用后端
    /// </summary>
    public static class CallApi
    {
        public static Task<ApiResult<List<CallListItem>>> GetDocCallListAsync(string deptId, string officeRoomId, int state) =>
            ApiClient.GetAsync<List<CallListItem>>(
                $"/DoctorCall/GetDocCallList?deptid={UriEncode(deptId)}&state={state}&officeRoomId={UriEncode(officeRoomId)}");

        public static Task<ApiResult<CallListItem>> AutoCallNextAsync(string deptId, string officeRoomId, string doctorId) =>
            ApiClient.PostAsync<CallListItem>("/DoctorCall/AutoCallNext", new
            {
                deptId,
                officeRoomId,
                doctorId
            });

        public static Task<ApiResult<bool>> DocCallMsgAsync(string msg, string? code) =>
            ApiClient.PostAsync<bool>("/DoctorCall/DocCallMsg", new { msg, code = code ?? "" });

        public static Task<ApiResult<bool>> EditCallMsgStateAsync(
            int callListId, int state, string? officeRoomId = null, string? doctorId = null)
        {
            var body = new Dictionary<string, object?>
            {
                ["calllistid"] = callListId,
                ["state"] = state
            };
            if (!string.IsNullOrEmpty(officeRoomId)) body["officeRoomId"] = officeRoomId;
            if (!string.IsNullOrEmpty(doctorId)) body["doctorId"] = doctorId;
            return ApiClient.PostAsync<bool>("/DoctorCall/EditCallMsgState", body);
        }

        /** 药房窗口队列（配药中 + 请取药） */
        public static Task<ApiResult<List<CallListItem>>> PharmacyDeskQueueAsync(string officeRoomId) =>
            ApiClient.PostAsync<List<CallListItem>>("/queue/pharmacyDeskQueue", new { officeRoomId });

        /** 配药完成 PREPARING → WAITING */
        public static Task<ApiResult<CallListItem>> PharmacyMarkReadyAsync(int callListId) =>
            ApiClient.PostAsync<CallListItem>("/queue/pharmacyMarkReady", new { callListId });

        private static string UriEncode(string s) => System.Uri.EscapeDataString(s ?? "");
    }
}
