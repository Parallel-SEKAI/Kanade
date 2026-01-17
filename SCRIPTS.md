# Technical Specification: Kanade Scripting System (KSS)

## 1. Introduction
Kanade Scripting System (KSS) is an extensibility framework designed to allow users to add new media sources (streaming platforms, local metadata providers) via external scripts. By decoupling the content fetching logic from the core application, Kanade can support a rapidly evolving ecosystem of media providers without requiring application updates.

---

## 2. Logic Flow Design

### 2.1 Initialization & Discovery
1.  **Scanner**: On startup, the `ScriptManager` scans the defined script directories for files ending in `.js`.
2.  **Manifest Parsing**: The manager reads the first 1KB of each file to extract a JSON metadata block (wrapped in a comment) containing the script name, unique ID, version, and author.
3.  **Registry**: Valid scripts are added to a `ProviderRegistry`, which the UI uses to populate the "External Sources" list.

### 2.2 Execution Pipeline (Method Call)
When a search is initiated:
1.  **Context Creation**: A `QuickJS` (or similar) VM context is created or retrieved from a pool.
2.  **Bridge Injection**: Host objects (`http`, `cache`, `console`) are injected into the script's global scope.
3.  **Invocation**: The script's `search(query, page)` function is called.
4.  **Serialization**: Resulting objects are serialized to JSON, passed back across the JNI bridge, and mapped to Kotlin data classes (`MusicItem`).

### 2.3 Hot-Reloading
- A `FileObserver` monitors the script directory.
- Changes trigger a "Dirty" flag for the specific script ID.
- The next time the script is accessed, the VM context is destroyed and re-initialized with the new code.

---

## 3. Interface Specification (Contract)

Scripts must export an object that implements the `IKanadeProvider` interface (expressed here in TypeScript syntax for clarity).

```typescript
interface IKanadeProvider {
    /**
     * Called once when the script is loaded.
     * @param config User-specific settings for this provider.
     */
    init(config: Record<string, any>): void;

    /**
     * Search for music.
     * @returns A list of MusicItem objects.
     */
    search(query: string, page: number): Promise<MusicItem[]>;

    /**
     * Retrieve the playable stream URL.
     */
    getMediaUrl(id: string): Promise<StreamInfo>;

    /**
     * (Optional) Fetch lyrics for a track.
     */
    getLyrics?(id: string): Promise<string>;
}

// Data Models
interface MusicItem {
    id: string;      // Internal provider ID
    title: string;
    artist: string;
    album?: string;
    cover?: string;
    duration?: number; // in seconds
}

interface StreamInfo {
    url: string;
    headers?: Record<string, string>;
    format: 'mp3' | 'flac' | 'm4a';
}
```

---

## 4. Host Bridge API (The `kanade` object)

Scripts cannot access the network or filesystem directly. They must use the provided `kanade` global object:

- **`kanade.http.get(url, options)`**: Performs a network request via the host's OkHttp client.
- **`kanade.http.post(url, body, options)`**: Standard POST request.
- **`kanade.cache.get(key)` / `kanade.cache.set(key, val, ttl)`**: Access to a restricted KV store.
- **`kanade.log(message)`**: Forwards logs to the Android Logcat.

---

## 5. Directory Structure

### 5.1 Android (Primary)
- **Primary Path**: `/storage/emulated/0/Android/data/org.parallel_sekai.kanade/files/scripts/`
- **Internal (Protected)**: `/data/user/0/org.parallel_sekai.kanade/files/bundled_scripts/`

---

## 6. Security & Sandboxing

1.  **JavaScript Engine**: Scripts run in a non-Node.js environment (e.g., QuickJS). No access to `process`, `require('fs')`, or `eval`.
2.  **Network Isolation**: Only HTTPS requests are allowed through the `kanade.http` bridge. Private IP ranges (e.g., 192.168.x.x) are blocked by default unless the user enables "Developer Mode."
3.  **Execution Limits**:
    - **Timeout**: Methods are killed after 15 seconds.
    - **Heap Limit**: 64MB per script instance.
4.  **Permissions**: Scripts can never request Android system permissions. They inherit the host's "Internet" permission but are restricted by the Bridge API.
