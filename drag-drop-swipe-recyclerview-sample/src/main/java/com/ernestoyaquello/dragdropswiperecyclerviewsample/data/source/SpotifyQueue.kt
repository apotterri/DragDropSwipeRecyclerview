package com.ernestoyaquello.dragdropswiperecyclerviewsample.data.source

import kaaes.spotify.webapi.android.models.TrackSimple
import com.ernestoyaquello.dragdropswiperecyclerviewsample.data.source.base.BaseRepository
import java.util.*


/**
 * A repository for the Queue
 */

class SpotifyQueue : BaseRepository<TrackSimple>() {

    companion object {
        private var instance: SpotifyQueue? = null

        fun getInstance(): SpotifyQueue {
            if (instance == null)
                instance = SpotifyQueue()

            return instance as SpotifyQueue
        }
    }
}