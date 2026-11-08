using System;
using System.IO;
using System.Windows;

namespace Gocent.CallStation.Services
{
    /// <summary>
    /// 叫号器深色/浅色切换。消费者：登录窗、列表叫号窗齿轮菜单。
    /// 精简悬浮窗不再单独切换，沿用当前主题。
    /// 替换 Application 里的 Palette.*.xaml；控件样式用 DynamicResource 才会跟着变。
    /// 选择写入 %LocalAppData%\HJ叫号器\theme.txt，下次启动沿用。
    /// </summary>
    public static class ThemeService
    {
        public const string Dark = "Dark";
        public const string Light = "Light";

        /** 按钮文案是点一下将切到的主题，方便老年医生辨认 */
        public static string NextLabel => Current == Light ? "深色主题" : "浅色主题";

        public static string Current { get; private set; } = Dark;

        public static event Action? Changed;

        public static void Initialize()
        {
            Apply(Load(), persist: false);
        }

        public static void Toggle()
        {
            Apply(Current == Light ? Dark : Light, persist: true);
        }

        private static void Apply(string theme, bool persist)
        {
            if (theme != Light) theme = Dark;
            var dict = new ResourceDictionary
            {
                Source = new Uri("pack://application:,,,/Themes/Palette." + theme + ".xaml", UriKind.Absolute)
            };
            var merged = Application.Current.Resources.MergedDictionaries;
            for (var i = merged.Count - 1; i >= 0; i--)
            {
                var src = merged[i].Source?.OriginalString ?? "";
                if (src.IndexOf("Palette.", StringComparison.OrdinalIgnoreCase) >= 0)
                    merged.RemoveAt(i);
            }
            merged.Insert(0, dict);
            Current = theme;
            if (persist) Save(theme);
            Changed?.Invoke();
        }

        private static string ThemeFile =>
            Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                "HJ叫号器",
                "theme.txt");

        private static string Load()
        {
            try
            {
                var text = File.ReadAllText(ThemeFile).Trim();
                return text == Light ? Light : Dark;
            }
            catch
            {
                return Dark;
            }
        }

        private static void Save(string theme)
        {
            try
            {
                var path = ThemeFile;
                var dir = Path.GetDirectoryName(path);
                if (string.IsNullOrEmpty(dir)) return;
                Directory.CreateDirectory(dir);
                File.WriteAllText(path, theme);
            }
            catch
            {
                /* 记不住主题不影响当次切换 */
            }
        }
    }
}
