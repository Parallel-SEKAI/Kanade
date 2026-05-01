# AGENTS.md — Kanade 项目 AI 开发代理指南

> 本文件为 AI 代理（Claude、GPT、Copilot 等）提供完整的项目上下文、架构说明与编码规范，以确保代码修改与项目风格保持一致。

---

## 1. 项目概述

**Kanade (奏)** 是一款面向 Android 平台的高性能、现代化音乐播放器，专注于极致播放体验、沉浸式 UI 设计与高度可扩展性。

- **组织**: Parallel-SEKAI
- **包名**: `org.parallel_sekai.kanade`
- **目标平台**: Android 8.0 (API 26) ~ Android 16 (API 36)
- **开源协议**: Apache License 2.0

### 核心功能
- **统一播放**: 本地 MediaStore 音轨 + 通过 KSS 脚本扩展的外部在线音源
- **高级歌词**: 标准 LRC、增强型 LRC（逐字）、Apple 风格 TTML，支持平滑过渡与分享
- **Kanade 脚本系统 (KSS)**: 基于 QuickJS 的 JavaScript 插件引擎，允许开发者编写脚本接入第三方音乐平台
- **现代 UI/UX**: 纯 Jetpack Compose 构建，动态渐变（Palette API）、沉浸式播放器过渡、延迟解析优化

---

## 2. 技术栈

| 模块 | 技术选型 |
| :--- | :--- |
| 编程语言 | Kotlin (JVM 17), JavaScript (ES2015+ via QuickJS) |
| 界面框架 | Jetpack Compose (Material 3, BOM 2025.12.00) |
| 音频引擎 | Android Media3 (ExoPlayer 1.9.0 + MediaSession) |
| 架构模式 | MVI (Model-View-Intent) + Repository Pattern |
| 脚本引擎 | `app.cash.quickjs:quickjs-android:0.9.2` |
| 图像加载 | Coil 2.7.0 + Palette KTX 1.0.0 |
| 数据持久化 | Jetpack DataStore Preferences 1.1.3 |
| 网络层 | OkHttp 4.12.0 |
| JSON 序列化 | kotlinx.serialization 1.7.3 |
| 导航 | Navigation Compose 2.9.6 |
| 歌词广播 | Lyric-Getter-Api 6.0.0, SuperLyricApi 2.4 |
| 代码格式 | Spotless 7.0.2 + ktlint 1.5.0 |

---

## 3. 构建与运行

```bash
# 构建 Debug APK
./gradlew :app:assembleDebug

# 构建 Release APK
./gradlew :app:assembleRelease

# 代码格式化（提交前必须执行）
./gradlew spotlessApply

# 运行 Lint
./gradlew :app:lint

# CI 脚本 (lint + assembleRelease)
./ci.sh
```

### CI/CD 流程
- **GitHub Actions** (`.github/workflows/android_build.yml`): JDK 17 → `spotlessCheck` → `assembleDebug` → Upload APK
- **pre-commit.ci**: 自动修复格式问题、每周更新 hooks
- **本地 pre-commit**: trailing-whitespace、end-of-file-fixer、check-yaml、check-added-large-files、ktlint --android

### 构建配置
| 配置项 | 值 |
| :--- | :--- |
| `compileSdk` | 36 |
| `minSdk` | 26 |
| `targetSdk` | 36 |
| `jvmTarget` | 17 |
| `versionCode` | 1 |
| `versionName` | 1.0 |
| `isMinifyEnabled` (release) | false |

---

## 4. 项目结构

```
kanade/
├── app/                                    # 唯一模块
│   ├── build.gradle.kts
│   └── src/main/java/org/parallel_sekai/kanade/
│       ├── MainActivity.kt                 # 入口、导航定义、手动依赖注入
│       ├── data/
│       │   ├── model/                      # 不可变数据类 (MusicModel, LyricData, ArtistModel...)
│       │   ├── parser/                     # 歌词解析器 (LRC/TTML)
│       │   ├── repository/                 # 领域逻辑 & 状态管理
│       │   │   ├── PlaybackRepository.kt   # Media3 控制桥接，播放状态持久化
│       │   │   └── SettingsRepository.kt   # DataStore 用户偏好
│       │   ├── script/                     # KSS 脚本系统
│       │   │   ├── ScriptEngine.kt         # QuickJS 运行时封装
│       │   │   ├── HostBridge.kt           # Kotlin→JS 桥接 (HTTP, Crypto, Logging)
│       │   │   ├── ScriptManager.kt        # 脚本生命周期管理
│       │   │   ├── ScriptModels.kt         # 脚本相关数据模型
│       │   │   └── ScriptMusicSource.kt    # 脚本驱动的音源适配器
│       │   ├── source/                     # 数据源抽象
│       │   │   ├── IMusicSource.kt         # 核心音源接口
│       │   │   ├── SourceManager.kt        # 聚合管理器 (单例)
│       │   │   └── local/
│       │   │       └── LocalMusicSource.kt # MediaStore 本地音源
│       │   └── utils/                      # 工具类
│       │       ├── CacheManager.kt
│       │       ├── UrlCacheManager.kt
│       │       ├── LyricGetterManager.kt
│       │       ├── LyricSplitter.kt
│       │       └── LyricImageUtils.kt
│       ├── service/
│       │   └── KanadePlaybackService.kt    # Media3 前台播放服务
│       └── ui/
│           ├── adaptive/
│           │   └── AdaptiveLayout.kt       # 自适应布局 (手机/平板/桌面)
│           ├── preview/                    # Compose Preview 辅助
│           ├── screens/
│           │   ├── player/                 # 播放器 (Contract/ViewModel/Components/SongInfo)
│           │   ├── library/                # 本地库 + 首页浏览
│           │   ├── search/                 # 多源防抖搜索
│           │   ├── artist/                 # 艺术家页面
│           │   ├── settings/               # 设置 (KSS/UI/歌词/缓存)
│           │   └── more/                   # "更多"页面
│           └── theme/
│               ├── Theme.kt               # Material 3 主题 (支持动态颜色)
│               ├── Color.kt
│               ├── Type.kt
│               └── Dimens.kt              # 原子化间距变量
├── build.gradle.kts                        # 根构建文件 (Spotless 配置)
├── settings.gradle.kts
├── gradle/libs.versions.toml               # 版本目录
├── ci.sh                                   # CI 脚本
├── .editorconfig                           # 代码风格规则
├── .pre-commit-config.yaml                 # Pre-commit hooks 配置
└── .github/workflows/android_build.yml     # GitHub Actions
```

---

## 5. 架构模式

### 5.1 MVI (Model-View-Intent)

每个页面严格遵循 MVI 契约，定义在同一包下的 `*Contract.kt` 文件中：

```
State     → 不可变的 UI 快照 (data class)
Intent    → 用户操作 (sealed interface)
Effect    → 一次性事件如 Toast/Snackbar (sealed interface)
```

**数据流**:
```
[Compose UI] --Intent--> [ViewModel] --更新--> [MutableStateFlow<State>]
                                                     ↓
                                               [Compose UI 重组]

[ViewModel] --Effect--> [MutableSharedFlow] --> [Snackbar/Toast]
```

**文件命名约定**:
- `XxxContract.kt` — 包含 `XxxState`, `XxxIntent`, `XxxEffect`
- `XxxViewModel.kt` — 处理 Intent，更新 State，发出 Effect
- `XxxScreen.kt` — Compose UI 实现

### 5.2 Repository Pattern
- `PlaybackRepository` — 播放控制的唯一真相源，桥接 ViewModel ↔ MediaController
- `SettingsRepository` — 用户偏好的 DataStore 管理器

### 5.3 音源抽象 (Strategy Pattern)
- `IMusicSource` — 统一音源接口
- `LocalMusicSource` — 本地 MediaStore 实现
- `ScriptMusicSource` — 脚本驱动的远程源实现
- `SourceManager` — 单例管理器，聚合本地和脚本音源

### 5.4 依赖注入
**无 DI 框架**。所有依赖在 `MainActivity.onCreate()` 中手动构造并传递。

---

## 6. 编码规范

### 6.1 命名约定
| 类型 | 风格 | 示例 |
| :--- | :--- | :--- |
| 类/接口 | PascalCase | `PlayerViewModel`, `IMusicSource` |
| 函数/变量 | camelCase | `handleIntent()`, `currentSong` |
| 常量 | SCREAMING_SNAKE_CASE | `EXTRA_EXPAND_PLAYER` |
| 包名 | 全小写 | `org.parallel_sekai.kanade` |

### 6.2 格式规则
| 规则 | 值 |
| :--- | :--- |
| 缩进 | 4 空格 |
| 最大行长 | 120 字符 |
| 字符集 | UTF-8 |
| 行尾 | LF |
| 末尾换行 | 必须 |

### 6.3 ktlint 禁用规则 (`.editorconfig`)
- `package-name` — 允许非标准包名
- `function-naming` — 允许 Composable 函数使用大写开头
- `no-wildcard-imports` — 允许通配符导入
- `property-naming` — 允许非标准属性名
- `value-parameter-comment` — 允许行内注释
- `backing-property-naming` — 允许非标准 backing property 名

### 6.4 代码风格
- **提交前必须**运行 `./gradlew spotlessApply`
- 使用 `MaterialTheme.colorScheme` 获取颜色，禁止硬编码色值
- 使用 `Dimens` 对象管理间距，禁止硬编码像素值
- UI 必须无状态，仅响应 StateFlow
- 使用 `sealed interface`（非 `sealed class`）定义 Intent/Effect

---

## 7. 开发指令（Agent/AI 必须遵守）

### 7.1 状态管理
- ViewModel 中使用 `MutableStateFlow`，对外暴露为 `StateFlow`
- UI 层只订阅状态，不直接操作
- Effect 使用 `MutableSharedFlow`，UI 通过 `LaunchedEffect` 收集

### 7.2 媒体操作
- **禁止** UI 代码直接操作 `MediaController` 或 `ExoPlayer`
- 所有播放操作**必须**通过 `PlaybackRepository`

### 7.3 KSS 线程安全
- 所有宿主桥接调用 (HTTP, Crypto) **必须**在 `ScriptEngine` 的专用 `executor` 线程执行
- QuickJS 不是线程安全的，严禁跨线程共享 JS 运行时

### 7.4 错误处理
- UI 使用 `PlayerEffect.ShowError` / `ShowMessage` 反馈
- Repository 捕获异常并返回安全默认值 (如 `emptyList()`)
- 不要让异常传播到 UI 层导致崩溃

### 7.5 异步编程
- 使用 Kotlin Coroutines + Flow
- ViewModel 使用 `viewModelScope`
- Repository 使用 `suspend` 函数
- 播放服务使用 `ProcessLifecycleOwner.get().lifecycleScope`

### 7.6 导航
- 路由定义在 `sealed class Screen` 中
- 参数路由使用 `createRoute()` 方法
- 顶级导航使用 `popUpTo(findStartDestination)` + `saveState` + `restoreState`
- 自适应布局: 宽屏 (≥840dp) 使用 `NavigationRail`，窄屏使用 `NavigationBar`

### 7.7 Compose 最佳实践
- 使用 `remember` 缓存计算结果
- 使用 `LaunchedEffect` 处理副作用
- 使用 `collectAsState()` 收集 StateFlow
- 预览函数放在 `ui/preview/` 目录

---

## 8. 脚本系统 (KSS) 规范

KSS 脚本是 `.js` 文件，由 Manifest（JSDoc 注释）+ 逻辑（全局函数）组成。

### Manifest
```javascript
/**
 * @kanade_script
 * {
 *   "id": "unique_id",
 *   "name": "Display Name",
 *   "version": "1.0.0",
 *   "author": "Author",
 *   "description": "Optional",
 *   "configs": [
 *     { "key": "api_key", "label": "API Key", "type": "string", "default": "" }
 *   ]
 * }
 */
```

### 必须实现的接口
| 函数 | 说明 | 返回值 |
| :--- | :--- | :--- |
| `init(config)` | 初始化，接收用户配置 JS 对象 | void |
| `getHomeList()` | 首页/发现区 | `MusicItem[]` |
| `search(query, page)` | 搜索 | `MusicItem[]` |
| `getMediaUrl(id)` | 解析流媒体 URL | `StreamInfo` |
| `getLyrics(id)` | (可选) 获取歌词 | LRC/TTML 字符串 |
| `getMusicListByIds(ids)` | (可选) 批量获取详情 | `MusicItem[]` |

### 数据模型
```javascript
// MusicItem
{ id: "string", title: "string", artist: "string", album: "string?", cover: "string?", duration: number? }

// StreamInfo
{ url: "string", format: "mp3"|"flac"|"m4a", headers: { "Key": "Value" } }
```

### 宿主桥接 API
- `http.get(url, options)`, `http.post(url, body, options)` — headers, responseType ("text"/"hex"), contentType
- `crypto.md5(text)`, `crypto.aesEncrypt(text, key, mode, padding)`, `crypto.aesDecrypt(hex, key, mode, padding)`
- `console.log/info/debug/warn/error(...)` → Android Logcat (Tag: `KanadeScript`)

### 安全限制
- 沙箱: 无 `process`, `require`, `eval`
- 超时: 单次函数执行 ≤ 15 秒
- 内存: 每个脚本实例 64MB
- 网络: 默认仅 HTTPS

---

## 9. 关键实现细节

### 9.1 kanade:// 协议与延迟解析
外部歌曲使用 `kanade://resolve?source_id=...&original_id=...` URI 包装脚本 ID。`KanadePlaybackService` 中的 `ResolvingDataSource` 在歌曲即将播放时才调用 `getMediaUrl()` 获取真实流地址，避免提前请求。

### 9.2 歌词系统
- 格式检测: `LyricParserFactory` 自动识别 LRC/TTML
- 同步: `PlayerViewModel` 观察 `progressFlow`，200ms 节流更新当前行
- 广播: 通过 `LyricGetterManager` 推送至系统级歌词 API
- 分享: `LyricImageUtils` 生成歌词卡片，通过 `FileProvider` + `Intent.ACTION_SEND` 分享

### 9.3 动态主题
歌曲切换时 `PlayerViewModel` 使用 `Palette` 提取封面主色调，存入 `PlayerState.gradientColors`，播放器背景平滑过渡。

### 9.4 媒体缓存
- `CacheManager`: 管理音频缓存，支持清理和大小限制 (Settings UI 可配)
- `UrlCacheManager`: 永久缓存脚本音源的已解析 URL，避免重复请求
- Coil: 内存缓存 25%，磁盘缓存 2%，忽略服务器缓存头

---

## 10. 代码质量

| 工具 | 命令 | 说明 |
| :--- | :--- | :--- |
| Spotless + ktlint | `./gradlew spotlessApply` | 格式化 `.kt` 和 `.gradle.kts` |
| Spotless Check | `./gradlew spotlessCheck` | CI 中检查格式 |
| Android Lint | `./gradlew :app:lint` | 静态分析 |
| Pre-commit | 自动触发 | trailing-whitespace, end-of-file, check-yaml, ktlint |

### 测试
- 单元测试: `app/src/test/` (JUnit 4)
- 仪器测试: `app/src/androidTest/` (AndroidX JUnit + Espresso + Compose UI Test)
- 当前仅有模板测试文件，尚无自定义测试

---

## 11. 国际化

| 语言 | 资源目录 |
| :--- | :--- |
| 英文 (默认) | `res/values/` |
| 简体中文 | `res/values-zh/` |
| 繁体中文 | `res/values-zh-rTW/` |

---

## 12. 权限

| 权限 | 用途 |
| :--- | :--- |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | 后台播放 |
| `READ_MEDIA_AUDIO` (API 33+) / `READ_EXTERNAL_STORAGE` (API <33) | 读取本地音频 |
| `POST_NOTIFICATIONS` (API 33+) | 播放通知 |
| `WAKE_LOCK` | 防止播放时休眠 |
| `INTERNET` | 脚本音源网络请求 |

---

## 13. 参考文档

| 文件 | 说明 |
| :--- | :--- |
| `GEMINI.md` | 项目完整文档（含 AI 上下文指令） |
| `KSS_DEVELOPER_GUIDE.md` | KSS 脚本开发指南 (英文) |
| `SCRIPTS.md` | KSS 脚本系统技术规范 (中文) |
| `NETEASE.md` | 网易云 EAPI 对接文档 |
| `home.md` | NetEaseCloudMusicApiEnhanced 完整接口文档 |
| `PLAN.md` | 开发计划 |
| `UI.md` | UI 架构说明 |
