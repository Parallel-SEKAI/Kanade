# Kanade 脚本系统 (KSS) 技术规范

## 1. 概述
Kanade 脚本系统 (KSS) 是一个基于 **QuickJS** 引擎的扩展框架，允许开发者通过 JavaScript 编写插件来扩展媒体资源（如在线音乐平台、歌词库等）。KSS 的核心设计理念是逻辑解耦：应用负责 UI 与播放控制，脚本负责数据的抓取与解析。

---

## 2. 脚本生命周期与发现

### 2.1 脚本加载
1.  **扫描**：应用启动或刷新时，`ScriptManager` 会扫描指定目录下的 `.js` 文件。
2.  **元数据提取**：读取文件头部的 JSDoc 块，解析 `ScriptManifest` JSON 块。
3.  **初始化**：脚本加载后会立即调用 `init(config)` 函数，并注入当前用户的偏好设置。

### 2.2 延迟解析 (Last-Minute Resolution)
为了优化性能和网络开销，KSS 采用延迟解析策略。脚本提供的 `id` 会被包装在 `kanade://` 协议中，只有当歌曲即将进入播放队列时，才会调用 `getMediaUrl` 获取真实的流媒体地址。

---

## 3. 接口规范 (Interface)

脚本必须在全局作用域实现以下函数（支持 `async` 异步函数）：

### 3.1 核心方法
- **`init(config)`**: 初始化脚本。`config` 为一个包含用户配置的 JS 对象。
- **`search(query, page)`**: 搜索音乐。返回 `MusicItem[]`。
- **`getHomeList()`**: 获取首页推荐列表。返回 `MusicItem[]`。
- **`getMediaUrl(id)`**: 解析可播放的流媒体 URL。返回 `StreamInfo`。
- **`getLyrics(id)`** *(可选)*: 获取歌词。返回原始歌词字符串（LRC/TTML）。

### 3.2 详情抓取 (v1.1+ 新增)
- **`getMusicListByIds(ids)`**: 批量获取音乐详情。返回 `MusicItem[]`。
- **`getMusicDetail(id)`**: 获取单个音乐详情。返回 `MusicItem`。

---

## 4. 数据模型 (Data Models)

### `MusicItem` (音乐条目)
```javascript
{
    id: "string",        // 平台唯一标识
    title: "string",     // 歌曲标题
    artist: "string",    // 艺术家名（多个用逗号分隔）
    album: "string?",    // 专辑名（可选）
    cover: "string?",    // 封面图 URL（可选）
    duration: number?    // 时长（秒，可选）
}
```

### `StreamInfo` (流媒体信息)
```javascript
{
    url: "string",       // 播放地址
    format: "string",    // 格式 (如 "mp3", "flac")
    headers: {           // 播放请求所需的 HTTP 请求头
        "User-Agent": "...",
        "Referer": "..."
    }
}
```

---

## 5. 原生桥接 API (Host Bridge)

脚本可以通过全局对象访问 Android 原生能力。

### 5.1 `http` (网络请求)
- **`http.get(url, options)`**
- **`http.post(url, body, options)`**

**`options` 字段说明：**
- `headers`: `Object` (请求头)
- `responseType`: `"text"` (默认) | `"hex"` (用于处理二进制数据)
- `contentType`: `String` (仅限 POST，如 `"application/json"`)

### 5.2 `crypto` (加密库)
- **`crypto.md5(text)`**: 返回 MD5 哈希的十六进制字符串。
- **`crypto.aesEncrypt(text, key, mode, padding)`**: AES 加密。
- **`crypto.aesDecrypt(hex, key, mode, padding)`**: AES 解密。
  - `mode`: `"CBC"`, `"ECB"` 等。
  - `padding`: `"PKCS5Padding"`, `"NoPadding"` 等。

### 5.3 `console` (日志)
- **`console.log/info/debug/warn/error(...args)`**: 将日志输出至 Android Logcat (Tag: `KanadeScript`)。

---

## 6. 安全性与限制

1.  **沙箱环境**：无 `process`, `require`, `eval` 访问权限。
2.  **资源限制**：
    - **超时**：单次函数执行不得超过 15 秒。
    - **内存**：每个脚本实例分配 64MB 堆内存。
3.  **网络隔离**：默认仅允许 HTTPS 请求。