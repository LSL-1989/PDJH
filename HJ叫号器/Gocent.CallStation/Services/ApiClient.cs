using System;
using System.Configuration;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;
using Gocent.CallStation.Models;
using Newtonsoft.Json;

namespace Gocent.CallStation.Services
{
    /// <summary>
    /// pdjh-server HTTP 客户端；统一 ResultData 解析。
    /// </summary>
    public static class ApiClient
    {
        private static readonly HttpClient Http = new HttpClient { Timeout = TimeSpan.FromSeconds(15) };

        public static string ApiBaseUrl =>
            (ConfigurationManager.AppSettings["ApiBaseUrl"] ?? "http://127.0.0.1:8081/api").TrimEnd('/');

        public static string WsUrl =>
            ConfigurationManager.AppSettings["WsUrl"] ?? "ws://127.0.0.1:8081/api/pdjh-ws-native";

        public static async Task<ApiResult<T>> GetAsync<T>(string path)
        {
            var url = ApiBaseUrl + (path.StartsWith("/") ? path : "/" + path);
            var resp = await Http.GetAsync(url).ConfigureAwait(false);
            var json = await resp.Content.ReadAsStringAsync().ConfigureAwait(false);
            return Parse<T>(json);
        }

        public static async Task<ApiResult<T>> PostAsync<T>(string path, object? body)
        {
            var url = ApiBaseUrl + (path.StartsWith("/") ? path : "/" + path);
            var content = new StringContent(
                body == null ? "{}" : JsonConvert.SerializeObject(body),
                Encoding.UTF8,
                "application/json");
            var resp = await Http.PostAsync(url, content).ConfigureAwait(false);
            var json = await resp.Content.ReadAsStringAsync().ConfigureAwait(false);
            return Parse<T>(json);
        }

        private static readonly JsonSerializerSettings JsonSettings = new JsonSerializerSettings
        {
            // 后端字段多为 PascalCase（@JsonProperty）；忽略未知字段
            MissingMemberHandling = MissingMemberHandling.Ignore
        };

        private static ApiResult<T> Parse<T>(string json)
        {
            if (string.IsNullOrWhiteSpace(json))
            {
                return new ApiResult<T> { Success = false, Code = 500, Msg = "空响应" };
            }
            try
            {
                return JsonConvert.DeserializeObject<ApiResult<T>>(json, JsonSettings)
                       ?? new ApiResult<T> { Success = false, Code = 500, Msg = "解析失败" };
            }
            catch (Exception ex)
            {
                return new ApiResult<T> { Success = false, Code = 500, Msg = "响应解析失败: " + ex.Message };
            }
        }
    }
}
