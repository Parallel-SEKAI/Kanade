# Implementation Plan - External Scripts Support (KSS)

## Phase A: Planning & Specification (Completed)
- [x] Requirement Analysis: Define core goals for the scripting system.
- [x] Technical Specification: Draft Logic Flow, Interface Contract, Directory Structure, and Security/Sandboxing. (Created `SCRIPTS.md`)
- [ ] User Review: Obtain approval for the specification.

## Phase B: Execution
- [x] Infrastructure Setup:
    - [x] Add `quickjs-android` and `kotlinx-serialization-json` dependencies.
    - [x] Create `org.parallel_sekai.kanade.data.script` package.
- [x] Script Engine Core:
    - [x] Implement `ScriptEngine` wrapper for QuickJS with Promise support.
    - [x] Implement `ScriptManager` for file scanning, manifest parsing, and lifecycle management.
    - [x] Create `ScriptMusicSource` implementing `IMusicSource`.
- [x] Bridge Layer:
    - [x] Implement `HostBridge` with `kanade.http` (OkHttp), `kanade.cache`, and `kanade.log`.
    - [x] Map JS `MusicItem` and `StreamInfo` to Kotlin data classes.
- [x] UI Integration:
    - [x] Add "External Sources" section to Library screen.
    - [x] Add "Scripts" management page in Settings (List scripts, toggle, reload).
    - [x] Add "Import Script" functionality to handle permission issues.
- [ ] Source Selection in Search:
    - [ ] Implement Source Selection in Search screen (Filter search by specific scripts).
- [x] Robustness & Bug Fixes:
    - [x] Implement `importScript(Uri)` in `ScriptManager`.
    - [x] Fix Netease script home page stuck loading issue (JS bridge wrapper, timeout, loading state).
- [ ] Testing & Samples:
    - [ ] Create a sample `.js` script (e.g., `mock_provider.js`).
    - [ ] Verify search, media URL retrieval, and playback.

## Phase C: Finalization
- [ ] Testing: Verify script loading and data parsing with sample providers.
- [ ] Documentation: Provide a "Script Developer Guide" for end-users.
- [ ] Summary: Finalize changes and prepare for commit.
