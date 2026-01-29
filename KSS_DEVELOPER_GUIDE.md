# Kanade Scripting System (KSS) Developer Guide

Kanade Scripting System (KSS) allows you to extend the app's functionality by writing JavaScript plugins to fetch music from external sources. These scripts run in a sandboxed QuickJS environment on Android.

---

## 1. Script Structure

A KSS script is a single `.js` file consisting of two parts:
1.  **Manifest**: A JSON block inside a JSDoc-style comment at the top of the file.
2.  **Logic**: The JavaScript functions that handle data fetching.

### Example Header
```javascript
/**
 * @kanade_script
 * {
 *   "id": "my_provider",
 *   "name": "My Custom Provider",
 *   "version": "1.0.0",
 *   "author": "YourName",
 *   "description": "Fetches music from my private API",
 *   "configs": [
 *     { "key": "api_key", "label": "API Key", "type": "string", "default": "" },
 *     { "key": "quality", "label": "Audio Quality", "type": "select", "default": "128k", "options": ["128k", "320k", "flac"] }
 *   ]
 * }
 */
```

---

## 2. Manifest Schema

| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | `string` | Unique identifier for the provider. |
| `name` | `string` | Display name in the UI. |
| `version` | `string` | Semantic versioning (e.g., `1.2.3`). |
| `author` | `string` | Your name or handle. |
| `description` | `string?` | Optional short description. |
| `configs` | `array?` | List of user-configurable variables. |

### Configuration Items (`configs`)
- `key`: The internal key used in code.
- `label`: Display name in the Settings UI.
- `type`: One of `"string"`, `"number"`, `"boolean"`, or `"select"`.
- `default`: Default value (as a string).
- `options`: (For `select` type) List of possible string values.

---

## 3. Core Interface

You must implement the following functions. They can be standard functions or `async` functions.

### `init(config)`
Called once when the engine starts or when settings change.
- `config`: A JavaScript object containing current user settings.
- **Note**: Values are provided based on the `type` defined in the manifest.

### `getHomeList()`
Called to populate the "Discover" section on the Library screen.
- **Returns**: `MusicItem[]`

### `search(query, page)`
Called when the user performs a search.
- `query`: The search string.
- `page`: Page number (starting from 1).
- **Returns**: `MusicItem[]`

### `getMediaUrl(id)`
Called when a track is about to be played.
- `id`: The internal ID of the music item.
- **Returns**: `StreamInfo` object.

### `getLyrics(id)` (Optional)
Called to fetch lyrics for a specific track.
- **Returns**: A raw string (`LRC` or `TTML` format).

### `getMusicListByIds(ids)` (Optional)
Batch fetch music details for a list of IDs.
- **Returns**: `MusicItem[]`

---

## 4. Data Models

### `MusicItem`
```javascript
{
    id: "track_123",      // String
    title: "Song Title",  // String
    artist: "Artist A, B",// String (comma separated for multiple)
    album: "Album Name",  // String (optional)
    cover: "https://...", // String (URL, optional)
    duration: 180         // Number (Seconds, optional)
}
```

### `StreamInfo`
```javascript
{
    url: "https://...",   // Playable media URL
    format: "mp3",        // "mp3", "flac", "m4a", etc.
    headers: {            // Optional HTTP headers for the player
        "User-Agent": "Kanade/1.0",
        "Referer": "https://mysite.com"
    }
}
```

---

## 5. Host Bridge API

The following global objects are provided by the Kanade host:

### `console`
- `console.log/info/debug/warn/error(...args)`: Standard logging.

### `http`
- `http.get(url, options)`: Performs a network request.
- `http.post(url, body, options)`: Performs an HTTP POST request.

**Options**:
- `headers`: `{ "Key": "Value" }`
- `responseType`: `"text"` or `"hex"` (for binary data).
- `contentType`: (POST only) e.g., `"application/json"`.

### `crypto`
- `crypto.md5(text)`: Returns MD5 hex string.
- `crypto.aesEncrypt(text, key, mode, padding)`: AES encryption.
- `crypto.aesDecrypt(hex, key, mode, padding)`: AES decryption.
  - *Modes*: `"CBC"`, `"ECB"`, etc.
  - *Padding*: `"PKCS5Padding"`, `"NoPadding"`, etc.

---

## 6. Full Boilerplate

```javascript
/**
 * @kanade_script
 * {
 *   "id": "boilerplate",
 *   "name": "Boilerplate Provider",
 *   "version": "1.1.0",
 *   "author": "Kanade",
 *   "configs": [
 *     { "key": "user", "label": "Username", "type": "string", "default": "Guest" }
 *   ]
 * }
 */

let settings = {};

function init(config) {
    settings = config;
}

function getHomeList() {
    return [
        { id: "h1", title: "Hello " + settings.user, artist: "System" }
    ];
}

async function search(query, page) {
    // const results = await http.get("https://api.example.com/search?q=" + query);
    // return JSON.parse(results).items;
    return [];
}

function getMediaUrl(id) {
    return {
        url: "https://example.com/stream/" + id + ".mp3",
        format: "mp3"
    };
}
```