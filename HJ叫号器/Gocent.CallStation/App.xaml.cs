using System.Windows;
using Gocent.CallStation.Services;

namespace Gocent.CallStation
{
    /// <summary>
    /// HJ叫号器入口：无边框 WPF。启动前套用上次保存的深色或浅色。
    /// </summary>
    public partial class App : Application
    {
        protected override void OnStartup(StartupEventArgs e)
        {
            ThemeService.Initialize();
            base.OnStartup(e);
        }
    }
}
