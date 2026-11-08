using System;
using System.Collections.Generic;
using Newtonsoft.Json;

namespace Gocent.CallStation.Models
{
    public class ApiResult<T>
    {
        [JsonProperty("success")]
        public bool Success { get; set; }

        [JsonProperty("code")]
        public int Code { get; set; }

        [JsonProperty("msg")]
        public string Msg { get; set; } = "";

        [JsonProperty("data")]
        public T? Data { get; set; }
    }

    public class DoctorLoginResult
    {
        [JsonProperty("doctorId")]
        public string DoctorId { get; set; } = "";

        [JsonProperty("workNo")]
        public string WorkNo { get; set; } = "";

        [JsonProperty("name")]
        public string Name { get; set; } = "";

        [JsonProperty("phone")]
        public string Phone { get; set; } = "";

        [JsonProperty("deptId")]
        public string DeptId { get; set; } = "";

        [JsonProperty("title")]
        public string Title { get; set; } = "";

        [JsonProperty("mustChangePassword")]
        public int MustChangePassword { get; set; }

        /** CLINIC | PHARMACY | LAB | EXAM，由后端按上岗窗口 businessType 推断 */
        [JsonProperty("stationMode")]
        public string StationMode { get; set; } = "CLINIC";

        [JsonProperty("businessType")]
        public string BusinessType { get; set; } = "";

        [JsonProperty("defaultRoom")]
        public OfficeRoomModel? DefaultRoom { get; set; }

        [JsonProperty("rooms")]
        public List<OfficeRoomModel> Rooms { get; set; } = new List<OfficeRoomModel>();
    }

    public class OfficeRoomModel
    {
        [JsonProperty("id")]
        public string Id { get; set; } = "";

        [JsonProperty("name")]
        public string Name { get; set; } = "";

        [JsonProperty("deptId")]
        public string DeptId { get; set; } = "";

        [JsonProperty("doctorId")]
        public string DoctorId { get; set; } = "";

        [JsonProperty("voiceDeviceId")]
        public string VoiceDeviceId { get; set; } = "";

        [JsonProperty("screenId")]
        public string ScreenId { get; set; } = "";

        [JsonProperty("state")]
        public int? State { get; set; }

        [JsonProperty("businessType")]
        public string? BusinessType { get; set; }
    }

    public class SysConfigModel
    {
        [JsonProperty("configKey")]
        public string ConfigKey { get; set; } = "";

        [JsonProperty("configValue")]
        public string ConfigValue { get; set; } = "";
    }

    /// <summary>
    /// 排队记录。字段名与后端 CallList @JsonProperty 对齐（如 PatientName）。
    /// 展示用属性加 JsonIgnore，避免与反序列化合同冲突。
    /// </summary>
    public class CallListItem
    {
        [JsonProperty("id")]
        public int Id { get; set; }

        [JsonProperty("PatientCode")]
        public string? PatientCode { get; set; }

        [JsonProperty("PatientName")]
        public string? PatientNameRaw { get; set; }

        [JsonProperty("ShowNo")]
        public string? ShowNoRaw { get; set; }

        [JsonProperty("xh")]
        public int? Xh { get; set; }

        [JsonProperty("sex")]
        public string? Sex { get; set; }

        [JsonProperty("ageStr")]
        public string? AgeStr { get; set; }

        [JsonProperty("state")]
        public int? State { get; set; }

        [JsonProperty("TriageState")]
        public string? TriageState { get; set; }

        [JsonProperty("SourceType")]
        public string? SourceType { get; set; }

        [JsonProperty("examItemName")]
        public string? ExamItemName { get; set; }

        [JsonProperty("examNextHint")]
        public string? ExamNextHint { get; set; }

        [JsonProperty("remark")]
        public string? Remark { get; set; }

        [JsonIgnore]
        public string PatientName =>
            string.IsNullOrWhiteSpace(PatientNameRaw) ? "-" : PatientNameRaw!;

        [JsonIgnore]
        public string ShowNo =>
            string.IsNullOrWhiteSpace(ShowNoRaw) ? "-" : ShowNoRaw!;

        [JsonIgnore]
        public string VisitLabel =>
            string.Equals(TriageState, "REVISIT", StringComparison.OrdinalIgnoreCase) ? "复" : "初";

        [JsonIgnore]
        public string StatusLabel =>
            string.Equals(TriageState, "PREPARING", StringComparison.OrdinalIgnoreCase) ? "配"
            : string.Equals(SourceType, "PHARMACY", StringComparison.OrdinalIgnoreCase)
                && string.Equals(TriageState, "WAITING", StringComparison.OrdinalIgnoreCase) ? "取"
            : VisitLabel;

        [JsonIgnore]
        public string ExamHintDisplay
        {
            get
            {
                var item = string.IsNullOrWhiteSpace(ExamItemName) ? Remark : ExamItemName;
                if (string.IsNullOrWhiteSpace(item) && string.IsNullOrWhiteSpace(ExamNextHint)) return "";
                if (string.IsNullOrWhiteSpace(ExamNextHint)) return item ?? "";
                if (string.IsNullOrWhiteSpace(item)) return ExamNextHint!;
                return item + " · " + ExamNextHint;
            }
        }

        [JsonIgnore]
        public string SexDisplay => string.IsNullOrWhiteSpace(Sex) ? "-" : Sex!;

        [JsonIgnore]
        public string AgeDisplay => string.IsNullOrWhiteSpace(AgeStr) ? "-" : AgeStr!;

        [JsonIgnore]
        public string SeqDisplay
        {
            get
            {
                if (!string.IsNullOrWhiteSpace(ShowNoRaw)) return ShowNoRaw!;
                return Xh.HasValue ? Xh.Value.ToString() : "-";
            }
        }
    }

    public class DoctorSession
    {
        public string DoctorId { get; set; } = "";
        public string WorkNo { get; set; } = "";
        public string DoctorName { get; set; } = "";
        /** 职称（列表顶栏展示，替代工号） */
        public string Title { get; set; } = "";
        public string DeptId { get; set; } = "";
        public string OfficeRoomId { get; set; } = "";
        public string OfficeName { get; set; } = "";
        public string ScreenCode { get; set; } = "";
        /** CLINIC | PHARMACY | LAB | EXAM */
        public string StationMode { get; set; } = "CLINIC";
        public string BusinessType { get; set; } = "";
        public List<OfficeRoomModel> Rooms { get; set; } = new List<OfficeRoomModel>();
    }
}
