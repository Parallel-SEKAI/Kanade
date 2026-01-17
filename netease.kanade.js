/**
 * @kanade_script
 * {
 *   "id": "netease",
 *   "name": "Netease Music",
 *   "version": "1.1.2",
 *   "author": "xiaocaoooo",
 *   "description": "Netease Music provider with robust JSON parsing",
 *   "configs": [
 *     { "key": "cookie", "label": "Cookie (MUSIC_U)", "type": "string", "default": "" },
 *     { "key": "playlist_id", "label": "Home Playlist ID", "type": "string", "default": "3778678" }
 *   ]
 * }
 */

let settings = {};

/**
 * Helper to safely parse JSON with cleanup
 */
function safeParse(str) {
    if (!str) return null;
    try {
        // Remove potential whitespace or non-JSON characters at start/end
        const cleaned = str.trim().replace(/^\uFEFF/, "");
        return JSON.parse(cleaned);
    } catch (e) {
        console.error("[netease] JSON Parse Error:", e.message);
        console.error("[netease] Raw data start:", str.substring(0, 200));
        return null;
    }
}

function init(configJson) {
    settings = safeParse(configJson) || {};
}

function getHomeList() {
    const playlistId = settings.playlist_id || "3778678";
    const url = "https://music.163.com/api/v6/playlist/detail";
    
    try {
        const res = http.post(url, "id=" + playlistId, {
            headers: {
                "Content-Type": "application/x-www-form-urlencoded",
                "Referer": "https://music.163.com/",
                "User-Agent": "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Safari/537.36 Chrome/91.0.4472.164 NeteaseMusicDesktop/2.10.2.200154",
                "Cookie": settings.cookie
            }
        });
        
        const data = safeParse(res);
        if (!data || data.code !== 200 || !data.playlist) return [];

        const trackIds = (data.playlist.trackIds || []).map(t => t.id);
        if (trackIds.length === 0) return [];

        const songs = [];
        const maxSongs = 100;
        const chunkSize = 50;
        const detailUrl = "https://interface3.music.163.com/api/v3/song/detail";

        for (let i = 0; i < Math.min(trackIds.length, maxSongs); i += chunkSize) {
            const chunk = trackIds.slice(i, i + chunkSize);
            const cParam = JSON.stringify(chunk.map(id => ({ id: id, v: 0 })));
            
            const detailRes = http.post(detailUrl, "c=" + encodeURIComponent(cParam), {
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded",
                    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Safari/537.36 Chrome/91.0.4472.164 NeteaseMusicDesktop/2.10.2.200154",
                    "Cookie": settings.cookie
                }
            });
            
            const detailData = safeParse(detailRes);
            if (detailData && detailData.code === 200 && detailData.songs) {
                detailData.songs.forEach(song => {
                    songs.push({
                        id: song.id.toString(),
                        title: song.name,
                        artist: song.ar.map(a => a.name).join(", "),
                        album: song.al.name,
                        cover: song.al.picUrl,
                        duration: Math.floor(song.dt / 1000)
                    });
                });
            }
        }
        return songs;
    } catch (e) {
        console.error("[netease] Error in getHomeList:", e.message);
        return [];
    }
}

function search(query, page) {
    const url = "https://interface3.music.163.com/api/cloudsearch/pc";
    const limit = 20;
    const offset = (page - 1) * limit;
    const body = "s=" + encodeURIComponent(query) + "&type=1&limit=" + limit + "&offset=" + offset;

    try {
        const res = http.post(url, body, {
            headers: {
                "Content-Type": "application/x-www-form-urlencoded",
                "Referer": "https://music.163.com/",
                "User-Agent": "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Safari/537.36 Chrome/91.0.4472.164 NeteaseMusicDesktop/2.10.2.200154",
                "Cookie": settings.cookie
            }
        });
        const data = safeParse(res);
        if (!data || data.code !== 200 || !data.result || !data.result.songs) return [];

        return data.result.songs.map(song => ({
            id: song.id.toString(),
            title: song.name,
            artist: song.ar.map(a => a.name).join(", "),
            album: song.al.name,
            cover: song.al.picUrl,
            duration: Math.floor(song.dt / 1000)
        }));
    } catch (e) {
        console.error("[netease] Error in search:", e.message);
        return [];
    }
}

function getMediaUrl(id) {
    const url = "https://interface3.music.163.com/api/song/enhance/player/url?id=" + id + "&ids=[" + id + "]&br=320000";
    
    try {
        const res = http.get(url, {
            headers: {
                "Referer": "https://music.163.com/",
                "User-Agent": "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Safari/537.36 Chrome/91.0.4472.164 NeteaseMusicDesktop/2.10.2.200154",
                "Cookie": settings.cookie
            }
        });
        const data = safeParse(res);
        if (data && data.code === 200 && data.data && data.data[0] && data.data[0].url) {
            return {
                url: data.data[0].url,
                format: "mp3"
            };
        }
    } catch (e) {
        console.error("[netease] Error in getMediaUrl:", e.message);
    }
    return null;
}

function getLyrics(id) {
    const url = "https://interface3.music.163.com/api/song/lyric";
    const body = "id=" + id + "&cp=false&tv=0&lv=0&rv=0&kv=0&yv=0&ytv=0&yrv=0";
    
    try {
        const res = http.post(url, body, {
            headers: {
                "Content-Type": "application/x-www-form-urlencoded",
                "Referer": "https://music.163.com/",
                "User-Agent": "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Safari/537.36 Chrome/91.0.4472.164 NeteaseMusicDesktop/2.10.2.200154",
                "Cookie": settings.cookie
            }
        });
        const data = safeParse(res);
        if (!data) return "";
        
        let lyric = "";
        if (data.lrc && data.lrc.lyric) {
            lyric += data.lrc.lyric;
        }
        if (data.tlyric && data.tlyric.lyric) {
            lyric += "\n" + data.tlyric.lyric;
        }
        return lyric;
    } catch (e) {
        console.error("[netease] Error in getLyrics:", e.message);
        return "";
    }
}