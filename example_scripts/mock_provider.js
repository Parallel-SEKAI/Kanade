/**
 * @kanade_script
 * {
 *   "id": "mock",
 *   "name": "Mock Provider",
 *   "version": "1.1.0",
 *   "author": "Kanade",
 *   "description": "A mock provider with config support",
 *   "configs": [
 *     { "key": "user_name", "label": "User Name", "type": "string", "default": "Guest" },
 *     { "key": "show_recommendations", "label": "Show Recommendations", "type": "boolean", "default": "true" }
 *   ]
 * }
 */

let currentConfig = {};

const mockData = [
    { id: "m1", title: "Mock Song 1", artist: "Artist A", album: "Album X", duration: 180 },
    { id: "m2", title: "Mock Song 2", artist: "Artist B", album: "Album Y", duration: 210 }
];

function init(config) {
    // console.log("Initializing with config: " + config);
    currentConfig = JSON.parse(config);
}

function getHomeList() {
    if (currentConfig.show_recommendations === "false") return [];
    
    return [
        { id: "h1", title: "Welcome, " + (currentConfig.user_name || "User"), artist: "System", album: "Home", duration: 0 },
        { id: "h2", title: "Trending Mock 1", artist: "Popular Artist", album: "Charts", duration: 200 }
    ];
}

function search(query, page) {
    return mockData.filter(item => 
        item.title.toLowerCase().includes(query.toLowerCase()) || 
        item.artist.toLowerCase().includes(query.toLowerCase())
    );
}

function getMediaUrl(id) {
    return {
        url: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
        format: "mp3"
    };
}

function getLyrics(id) {
    return "[00:00.00]This is a mock lyric\n[00:05.00]Enjoy the mock music!";
}
