namespace Gocent.CallStation.Services
{
    /// <summary>
    /// 叫号器场景文案。门诊/药房/检验/医技体检共用同一客户端，
    /// 仅替换点位名词（诊室/窗口/检查室），操作按钮保持「呼叫下一位 / 重呼 / 过号 / 完成」。
    /// </summary>
    public static class StationCopy
    {
        public const string ProductName = "HJ叫号器";

        public static string Mode(string? stationMode = null)
        {
            var raw = !string.IsNullOrWhiteSpace(stationMode)
                ? stationMode
                : SessionManager.Current?.StationMode;
            if (string.IsNullOrWhiteSpace(raw)) return "CLINIC";
            return raw!.Trim().ToUpperInvariant();
        }

        /// <summary>按诊室/科室 businessType 得到 stationMode（与后端 StationUserSyncService 一致）</summary>
        public static string ModeFromBusinessType(string? businessType)
        {
            if (string.IsNullOrWhiteSpace(businessType)) return "CLINIC";
            var t = businessType.Trim().ToUpperInvariant();
            if (t == "PHARMACY") return "PHARMACY";
            if (t == "LAB") return "LAB";
            if (t is "EXAM" or "MEDTECH" or "RADIOLOGY" or "ULTRASOUND" or "ECG") return "EXAM";
            return "CLINIC";
        }

        public static bool IsPharmacy(string? stationMode = null) => Mode(stationMode) == "PHARMACY";
        public static bool IsLab(string? stationMode = null) => Mode(stationMode) == "LAB";
        public static bool IsExam(string? stationMode = null) => Mode(stationMode) == "EXAM";

        /** 物理点位：诊室 / 窗口 / 检查室 */
        public static string RoomNoun(string? stationMode = null)
        {
            var m = Mode(stationMode);
            if (m == "PHARMACY" || m == "LAB") return "窗口";
            if (m == "EXAM") return "检查室";
            return "诊室";
        }

        public static string WaitingTab => "等候";
        public static string OperatorFallback => "值班员";
        public static string ConfirmOk => "确认上岗";
        public static string CompleteAction => "完成";

        public static string ConfirmTitle(string? stationMode = null) =>
            "确认" + RoomNoun(stationMode);

        public static string ConfirmHint(string? stationMode = null) =>
            "确认后将在对应" + RoomNoun(stationMode) + "门口展示值班信息";

        public static string ConfirmRoomLabel(string? stationMode = null) =>
            "当前" + RoomNoun(stationMode);

        public static string NoRooms(string? stationMode = null) =>
            "本科室暂无开放" + RoomNoun(stationMode) + "，请联系管理员";

        public static string NeedRoom(string? stationMode = null) =>
            "请确认" + RoomNoun(stationMode) + "后再进入";

        public static string BindFailed => "窗口绑定失败";

        public static string NoCurrent => "当前没有正在办理的患者";
        public static string MustFinishBeforeNext =>
            "当前仍有正在办理的患者，请先点击「完成」后再呼叫下一位";
        public static string MustFinishBeforeNamed =>
            "当前仍有正在办理的患者，请先点击「完成」后再点名呼叫";
        public static string Done => "已完成";
        public static string NoWaiting => "当前暂无等候患者";
    }
}
