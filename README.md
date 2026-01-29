# Kanade (奏) - 高性能 Android 音乐播放器

**Kanade** 是一款专注于极致播放体验、现代 UI 设计与高度可扩展性的 Android 音乐播放器。它基于 Clean Architecture 与 MVI 架构开发，结合了强大的本地播放能力与创新的脚本系统 (KSS)，旨在为用户提供纯粹且强大的音乐享受。

---

## ✨ 核心特性

- **🎶 卓越的播放引擎**：基于 **Android Media3 (ExoPlayer)**，支持无缝切换、背景播放以及系统级媒体控制中心集成。
- **📜 极致歌词体验**：
    - 支持标准 LRC、增强型 LRC 及 TTML (Apple Style) 格式。
    - **逐字同步**：支持 Karaoke 风格的横向平滑填充效果。
    - **系统联动**：通过 `Lyric-Getter-API` 与 `SuperLyricApi` 将歌词实时共享至系统状态栏或桌面悬浮窗。
- **🚀 Kanade Scripting System (KSS)**：
    - 内置 **QuickJS** 引擎，支持通过 JavaScript 编写外部音源插件。
- **🎨 现代沉浸式 UI**：
    - 全面采用 **Jetpack Compose** 与 **Material 3** 设计规范。
    - **动态色彩**：基于专辑封面自动提取主题色 (Palette)，生成流光背景。
    - **灵动过渡**：播放器容器支持手势跟随，MiniPlayer 与全屏模式之间通过线性插值 (lerp) 实现丝滑形变。
- **🔍 智能库管理**：
    - 快速扫描本地媒体库，支持按艺术家、专辑、文件夹分类。
    - **智能艺术家识别**：自动计算合辑 (Compilations) 中的 "Various Artists"，优化乱序的元数据展示。
- **🌍 多语言支持**：原生支持简体中文、繁体中文及英文。

---

## 🛠️ 技术栈

| 模块 | 技术选型 |
| :--- | :--- |
| **编程语言** | Kotlin (JVM 17), JavaScript (QuickJS) |
| **界面框架** | Jetpack Compose (Material 3) |
| **音频引擎** | Android Media3 (ExoPlayer + MediaSession) |
| **架构模式** | Clean Architecture + MVI (Model-View-Intent) |
| **脚本引擎** | app.cash.quickjs:quickjs-android |
| **图像加载** | Coil + Palette (颜色提取) |
| **数据持久化** | Jetpack DataStore |
| **网络层** | OkHttp 4 |

---

## 📦 项目结构

```text
app/src/main/java/org/parallel_sekai/kanade/
├── data/
│   ├── parser/         # 歌词解析器 (LRC/TTML)
│   ├── repository/     # 数据仓库 (播放控制、设置、脚本管理)
│   ├── script/         # KSS 核心实现 (QuickJS 桥接、指令分发)
│   └── source/         # 音源实现 (本地 MediaStore / 脚本音源)
├── service/            # KanadePlaybackService (Media3 后台服务)
└── ui/
    ├── screens/        # Compose 页面 (播放器、搜索、音乐库、设置)
    └── theme/          # Material 3 主题与原子化设计变量
```

---

## 🚀 快速开始

### 编译要求
- Android Studio Ladybug 或更高版本
- JDK 17
- Android SDK 26+ (建议在 API 36/Android 16 环境下运行)

### 构建步骤
1. 克隆仓库：
   ```bash
   git clone https://github.com/your-username/kanade.git
   ```
2. 使用 Gradle 编译：
   ```bash
   ./gradlew assembleDebug
   ```

---

## 📜 脚本系统 (KSS)

Kanade 的核心竞争力在于其插件化。你可以通过编写简单的 JavaScript 脚本来扩展音源。

- **脚本位置**：导入的脚本存储在应用私有目录，默认内置 `mock_provider.js`。
- **能力**：脚本可以访问网络 (HTTP GET/POST)、加密库 (MD5/AES) 以及日志系统。
- **延迟加载**：采用 `ResolvingDataSource` 技术，仅在即将播放时解析脚本 URL，极大节省流量与内存。

详情请参考 [KSS 开发者指南](./KSS_DEVELOPER_GUIDE.md)。

---

## 🤝 贡献与反馈

我们欢迎任何形式的 Contribution！
- 如果你发现了 Bug，请提交 [Issue](https://github.com/your-username/kanade/issues)。
- 如果你有新的脚本或功能想法，欢迎提交 Pull Request。

---

## 📄 开源协议
本项目采用 [Apache License 2.0](LICENSE) 协议开源。
