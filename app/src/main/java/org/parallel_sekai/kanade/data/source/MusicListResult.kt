package org.parallel_sekai.kanade.data.source

import org.parallel_sekai.kanade.data.model.MusicModel

data class MusicListResult(
    val items: List<MusicModel>,
    val totalCount: Int? = null,
)
