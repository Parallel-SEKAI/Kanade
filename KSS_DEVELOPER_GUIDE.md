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
 *     { "key": "high_quality", "label": "HQ Audio", "type": "boolean", "default": "false" }
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
| `configs` | `array?` | List of user-configurable variables (see below). |

### Configuration Items (`configs`)
- `key`: The internal key used in code.
- `label`: Display name in the Settings UI.
- `type`: One of `"string"`, `"number"`, or `"boolean"`.
- `default`: Default value (as a string).

---

## 3. Core Interface

You must implement the following functions. They can be standard functions or `async` functions.

### `init(configJson)`
Called once when the engine starts or when settings change.
- `configJson`: A JSON string containing current user settings.
- **Tip**: Always `JSON.parse(configJson)` at the start.

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

---

## 4. Data Models

### `MusicItem`
```javascript
{
    id: "track_123",      // String
    title: "Song Title",  // String
    artist: "Artist Name",// String (comma separated for multiple)
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

### `console.log(message)`
Prints a message to the Android Logcat (Tag: `KanadeScript`).

### `http.get(url, options)`
Performs a synchronous network request.
- **Returns**: Response body as a string.

### `http.post(url, body, options)`
Performs an HTTP POST request.
- **Returns**: Response body as a string.

---

## 6. Deployment & Testing

1.  Open **Settings** -> **Script Management** in Kanade.
2.  Click the **"+" (Import Script)** button.
3.  Select your `.js` file.
4.  Toggle the **Switch** to activate your script.
5.  If your script has configurations, click the **Gear** icon to edit them.
6.  Check the **Library** or **Search** screen to see your data.

---

## 7. Full Boilerplate

```javascript
/**
 * @kanade_script
 * {
 *   "id": "boilerplate",
 *   "name": "Boilerplate Provider",
 *   "version": "1.0.0",
 *   "author": "Kanade",
 *   "configs": [
 *     { "key": "user", "label": "Username", "type": "string", "default": "Guest" }
 *   ]
 * }
 */

let settings = {};

function init(config) {
    settings = JSON.parse(config);
}

function getHomeList() {
    return [
        { id: "h1", title: "Hello " + settings.user, artist: "System" }
    ];
}

function search(query, page) {
    // const results = http.get("https://api.example.com/search?q=" + query);
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
