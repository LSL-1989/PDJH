using System.Net.WebSockets;
using System.Text;

namespace Gocent.VoiceTerminal.Services;

/// <summary>
/// STOMP over 原生 WebSocket，连接 /api/pdjh-ws-native，订阅 /topic/voice/{deviceCode}。
/// </summary>
public sealed class StompVoiceClient : IDisposable
{
    private ClientWebSocket? _ws;
    private CancellationTokenSource? _cts;
    private Task? _loop;
    private string _destination = "";

    public event Action? Connected;
    public event Action<string>? Disconnected;
    public event Action<string>? MessageReceived;
    public bool IsConnected => _ws != null && _ws.State == WebSocketState.Open;

    public void Connect(string wsUrl, string deviceCode)
    {
        Disconnect();
        _destination = "/topic/voice/" + (deviceCode ?? "").Trim().ToUpperInvariant();
        _cts = new CancellationTokenSource();
        _loop = Task.Run(() => RunAsync(wsUrl, _cts.Token));
    }

    public void Disconnect()
    {
        try { _cts?.Cancel(); } catch { /* ignore */ }
        try { _ws?.Abort(); } catch { /* ignore */ }
        _ws = null;
    }

    private async Task RunAsync(string wsUrl, CancellationToken ct)
    {
        while (!ct.IsCancellationRequested)
        {
            try
            {
                _ws = new ClientWebSocket();
                await _ws.ConnectAsync(new Uri(wsUrl), ct).ConfigureAwait(false);
                await SendFrameAsync("CONNECT\naccept-version:1.1,1.2\nhost:localhost\n\n\0", ct)
                    .ConfigureAwait(false);

                Connected?.Invoke();
                await SendFrameAsync($"SUBSCRIBE\nid:sub-voice\ndestination:{_destination}\n\n\0", ct)
                    .ConfigureAwait(false);

                var buffer = new byte[8192];
                var sb = new StringBuilder();
                while (_ws.State == WebSocketState.Open && !ct.IsCancellationRequested)
                {
                    var result = await _ws.ReceiveAsync(new ArraySegment<byte>(buffer), ct)
                        .ConfigureAwait(false);
                    if (result.MessageType == WebSocketMessageType.Close) break;
                    sb.Append(Encoding.UTF8.GetString(buffer, 0, result.Count));
                    if (!result.EndOfMessage) continue;
                    var text = sb.ToString();
                    sb.Clear();
                    HandleFrame(text);
                }
            }
            catch (OperationCanceledException)
            {
                break;
            }
            catch (Exception ex)
            {
                Disconnected?.Invoke(ex.Message);
                try { await Task.Delay(5000, ct).ConfigureAwait(false); } catch { break; }
            }
            finally
            {
                try { _ws?.Dispose(); } catch { /* ignore */ }
                _ws = null;
            }
        }
        Disconnected?.Invoke("已断开");
    }

    private void HandleFrame(string text)
    {
        if (string.IsNullOrEmpty(text)) return;
        var idx = text.IndexOf("\n\n", StringComparison.Ordinal);
        if (text.StartsWith("MESSAGE", StringComparison.OrdinalIgnoreCase) && idx >= 0)
        {
            var body = text[(idx + 2)..].TrimEnd('\0', '\r', '\n');
            MessageReceived?.Invoke(body);
        }
    }

    private async Task SendFrameAsync(string frame, CancellationToken ct)
    {
        if (_ws == null || _ws.State != WebSocketState.Open) return;
        var bytes = Encoding.UTF8.GetBytes(frame);
        await _ws.SendAsync(new ArraySegment<byte>(bytes), WebSocketMessageType.Text, true, ct)
            .ConfigureAwait(false);
    }

    public void Dispose() => Disconnect();
}
