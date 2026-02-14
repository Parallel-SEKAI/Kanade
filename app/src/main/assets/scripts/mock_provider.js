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
    if (typeof config === "string") {
        try {
            currentConfig = JSON.parse(config);
        } catch (e) {
            currentConfig = {};
        }
    } else {
        currentConfig = config || {};
    }
}

function getHomeList(page = 1) {
    if (currentConfig.show_recommendations === "false") return [];
    
    const pageSize = 20;
    const start = (page - 1) * pageSize;
    
    // Simulate end of list
    if (page > 3) return [];

    return [
        { id: `mock_home_${start + 1}`, title: `Recommended Song ${start + 1} (Page ${page})`, artist: "Mock Artist", album: "Mock Album", duration: 210 },
        { id: `mock_home_${start + 2}`, title: `Recommended Song ${start + 2} (Page ${page})`, artist: "Mock Artist", album: "Mock Album", duration: 210 },
        { id: `mock_home_${start + 3}`, title: `Recommended Song ${start + 3} (Page ${page})`, artist: "Mock Artist", album: "Mock Album", duration: 210 },
        { id: `mock_home_${start + 4}`, title: `Recommended Song ${start + 4} (Page ${page})`, artist: "Mock Artist", album: "Mock Album", duration: 210 },
        { id: `mock_home_${start + 5}`, title: `Recommended Song ${start + 5} (Page ${page})`, artist: "Mock Artist", album: "Mock Album", duration: 210 }
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
