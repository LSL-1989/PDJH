# HJ独立音响终端（.NET 8 + WPF）

在一台**独立电脑**上安装运行，电脑连接功放/音箱。进程**托盘常驻**、支持**开机自启**，通过 Windows `System.Speech` TTS 外放叫号。

**不是网页、不依赖浏览器。** 与**候诊大屏**绑定：播报文案使用大屏「语音播报」模板，叫号范围跟随大屏已关联诊室。

## 工程

```
HJ音响终端/
  Gocent.VoiceTerminal.sln
  Gocent.VoiceTerminal/     # .NET 8 WPF
```

## 编译与运行

```bash
cd HJ音响终端
dotnet restore Gocent.VoiceTerminal.sln
dotnet build Gocent.VoiceTerminal.sln -c Release
dotnet run --project Gocent.VoiceTerminal -c Release
```

发布单文件（可拷贝到音响主机）：

```bash
dotnet publish Gocent.VoiceTerminal -c Release -r win-x64 --self-contained true ^
  -p:PublishSingleFile=true -p:IncludeNativeLibrariesForSelfExtract=true ^
  -o publish
```

将 `publish/` 拷到现场电脑，首次运行填写后端地址与设备编号，勾选开机自启。

## 能力

| 项 | 说明 |
|----|------|
| 收号 | 原生 STOMP `ws://服务器/api/pdjh-ws-native` → `/topic/voice/{设备编号}` |
| 播报 | 文案 = 触发叫号的候诊大屏 CallContent（`{0}`号序 `{1}`姓名 `{2}`诊室） |
| 常驻 | NotifyIcon 托盘；关闭设置窗不退出 |
| 开机自启 | 当前用户 `Run` 注册表项 |
| 后台绑定 | **屏幕管理** → 候诊大屏「绑定音响」（多对多；音响台账在「独立音响管理」） |

## 现场步骤

1. 管理后台 → 独立音响管理 → 新增设备（编号与终端一致）  
2. 管理后台 → 候诊大屏管理 → 配置「语音播报」模板、关联诊室 → **绑定音响**；播报通道选「独立音响」  
3. 音响主机运行本程序 → 填 `http://服务器:8081` 与设备编号 → 保存并连接  
4. 托盘「测试播报」验声；勾选开机自启  
5. 音响故障时：同一大屏将播报通道改为「大屏播报」（大屏主机需外接音箱）  

## 说明

- 技术栈对齐叫号器（WPF），目标框架为 **.NET 8**。  
- 旧 Electron 实现已移除，请使用本 WPF 工程。  
- 一诊区多屏：各屏分别绑定音响；同一台音响可被多块大屏勾选共用。  
