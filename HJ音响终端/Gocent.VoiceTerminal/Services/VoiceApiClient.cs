using System.Net.Http;
using Newtonsoft.Json.Linq;

namespace Gocent.VoiceTerminal.Services;

public static class VoiceApiClient
{
    private static readonly HttpClient Http = new() { Timeout = TimeSpan.FromSeconds(8) };

    public static async Task<(bool Ok, string Message, string? BoundSummary)> ValidateDeviceAsync(
        string serverBaseUrl, string deviceCode, CancellationToken ct = default)
    {
        var code = (deviceCode ?? "").Trim().ToUpperInvariant();
        if (string.IsNullOrEmpty(code))
            return (false, "请填写设备编号", null);

        var api = AppConfig.NormalizeApiBase(serverBaseUrl);
        var url = $"{api}/VoiceDevice/GetDeviceConfig?deviceCode={Uri.EscapeDataString(code)}";
        try
        {
            using var res = await Http.GetAsync(url, ct).ConfigureAwait(false);
            var text = await res.Content.ReadAsStringAsync(ct).ConfigureAwait(false);
            var json = JObject.Parse(text);
            var success = json.Value<bool?>("success") ?? json.Value<bool?>("Success") ?? false;
            var msg = json.Value<string>("msg") ?? json.Value<string>("message") ?? "";
            if (!success)
                return (false, string.IsNullOrEmpty(msg) ? "设备不存在或已停用" : msg, null);

            var data = json["data"] as JObject;
            var summary = data?.Value<string>("boundSummary") ?? data?.Value<string>("BoundSummary");
            return (true, "校验通过", summary);
        }
        catch (Exception ex)
        {
            return (false, "无法连接服务器：" + ex.Message, null);
        }
    }
}
