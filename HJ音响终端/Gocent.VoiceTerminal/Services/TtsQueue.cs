using System.Collections.Concurrent;
using System.Globalization;
using System.Speech.Synthesis;

namespace Gocent.VoiceTerminal.Services;

/// <summary>系统 TTS 顺序播报队列，避免叠音；支持音量、音色、语速与重复次数。</summary>
public sealed class TtsQueue : IDisposable
{
    private readonly record struct SpeakItem(string Text, int Rate);

    private readonly ConcurrentQueue<SpeakItem> _queue = new();
    private readonly SemaphoreSlim _signal = new(0);
    private readonly CancellationTokenSource _cts = new();
    private readonly Task _worker;
    private readonly object _speakLock = new();
    private static readonly CultureInfo ZhCn = CultureInfo.GetCultureInfo("zh-CN");

    private volatile int _volume = 100;
    private volatile string _voiceName = "";

    public TtsQueue()
    {
        _worker = Task.Run(LoopAsync);
    }

    public void ApplySettings(int volume, string? voiceName)
    {
        _volume = Math.Clamp(volume, 0, 100);
        _voiceName = (voiceName ?? "").Trim();
    }

    public sealed record VoiceOption(string Name, string Display);

    /// <summary>本机已安装语音列表（优先中文排前）。</summary>
    public static IReadOnlyList<VoiceOption> ListInstalledVoices()
    {
        try
        {
            using var synth = new SpeechSynthesizer();
            return synth.GetInstalledVoices()
                .Where(v => v.Enabled)
                .Select(v => v.VoiceInfo)
                .OrderByDescending(v => IsChineseVoice(v))
                .ThenBy(v => v.Name, StringComparer.OrdinalIgnoreCase)
                .Select(v => new VoiceOption(
                    v.Name,
                    $"{v.Name}（{v.Culture.DisplayName}）"))
                .ToList();
        }
        catch
        {
            return Array.Empty<VoiceOption>();
        }
    }

    public static bool HasChineseVoice() => ResolveChineseVoiceName() != null;

    public static string? ResolveChineseVoiceName()
    {
        try
        {
            using var synth = new SpeechSynthesizer();
            var voices = synth.GetInstalledVoices()
                .Where(v => v.Enabled)
                .Select(v => v.VoiceInfo)
                .ToList();

            var zhCn = voices.FirstOrDefault(v =>
                v.Culture.Name.Equals("zh-CN", StringComparison.OrdinalIgnoreCase));
            if (zhCn != null) return zhCn.Name;

            var zh = voices.FirstOrDefault(IsChineseVoice);
            return zh?.Name;
        }
        catch
        {
            return null;
        }
    }

    private static bool IsChineseVoice(VoiceInfo v) =>
        v.Culture.TwoLetterISOLanguageName.Equals("zh", StringComparison.OrdinalIgnoreCase)
        || v.Name.Contains("Huihui", StringComparison.OrdinalIgnoreCase)
        || v.Name.Contains("Yaoyao", StringComparison.OrdinalIgnoreCase)
        || v.Name.Contains("Kangkang", StringComparison.OrdinalIgnoreCase)
        || v.Name.Contains("Xiaoxiao", StringComparison.OrdinalIgnoreCase)
        || v.Name.Contains("Chinese", StringComparison.OrdinalIgnoreCase);

    /// <param name="times">呼叫次数，1~5</param>
    /// <param name="rate">SAPI 语速 -10~10，0 为正常</param>
    public void Enqueue(string? text, int times = 1, int rate = 0)
    {
        var t = (text ?? "").Trim();
        if (t.Length == 0) return;
        times = Math.Clamp(times, 1, 5);
        rate = Math.Clamp(rate, -10, 10);
        for (var i = 0; i < times; i++)
        {
            _queue.Enqueue(new SpeakItem(t, rate));
            _signal.Release();
        }
    }

    private async Task LoopAsync()
    {
        while (!_cts.IsCancellationRequested)
        {
            try
            {
                await _signal.WaitAsync(_cts.Token).ConfigureAwait(false);
            }
            catch (OperationCanceledException)
            {
                break;
            }

            while (_queue.TryDequeue(out var item))
            {
                try
                {
                    Speak(item.Text, item.Rate);
                }
                catch
                {
                    /* ignore single utterance failure */
                }
            }
        }
    }

    private void Speak(string text, int rate)
    {
        lock (_speakLock)
        {
            using var synth = new SpeechSynthesizer();
            synth.Rate = Math.Clamp(rate, -10, 10);
            synth.Volume = _volume;
            synth.SetOutputToDefaultAudioDevice();

            var preferred = _voiceName;
            var voiceName = !string.IsNullOrWhiteSpace(preferred) && VoiceInstalled(synth, preferred)
                ? preferred
                : ResolveChineseVoiceName();
            if (voiceName != null)
            {
                try { synth.SelectVoice(voiceName); }
                catch { /* keep default if select fails */ }
            }

            var builder = new PromptBuilder(ZhCn);
            builder.AppendText(text);
            synth.Speak(builder);
        }
    }

    private static bool VoiceInstalled(SpeechSynthesizer synth, string name) =>
        synth.GetInstalledVoices().Any(v =>
            v.Enabled && v.VoiceInfo.Name.Equals(name, StringComparison.OrdinalIgnoreCase));

    public void Dispose()
    {
        _cts.Cancel();
        try { _signal.Release(); } catch { /* ignore */ }
        try { _worker.Wait(2000); } catch { /* ignore */ }
        _cts.Dispose();
        _signal.Dispose();
    }
}
