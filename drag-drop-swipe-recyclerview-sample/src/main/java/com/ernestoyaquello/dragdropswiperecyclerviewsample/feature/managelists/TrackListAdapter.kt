package com.ernestoyaquello.dragdropswiperecyclerviewsample.feature.managelists

import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import androidx.core.widget.ImageViewCompat
import androidx.appcompat.widget.AppCompatImageView
import android.view.View
import android.widget.TextView
import com.ernestoyaquello.dragdropswiperecyclerview.DragDropSwipeAdapter
import com.ernestoyaquello.dragdropswiperecyclerviewsample.R
import com.ernestoyaquello.dragdropswiperecyclerviewsample.data.model.IceCream
import com.ernestoyaquello.dragdropswiperecyclerviewsample.util.Logger
import com.spotify.android.appremote.api.SpotifyAppRemote


import kaaes.spotify.webapi.android.models.TrackSimple
/**
 * Adapter for a list of Spotify Tracks.
 */
class TrackListAdapter(dataSet: List<TrackSimple> = emptyList())
    : DragDropSwipeAdapter<TrackSimple, TrackListAdapter.ViewHolder>(dataSet) {

    private var spotifyRemote: SpotifyAppRemote? = null

    class ViewHolder(trackLayout: View) : DragDropSwipeAdapter.ViewHolder(trackLayout) {
        val trackNameView: TextView = itemView.findViewById(R.id.ice_cream_name)
        val trackPriceView: TextView = itemView.findViewById(R.id.ice_cream_price)
        val dragIcon: AppCompatImageView = itemView.findViewById(R.id.drag_icon)
        val trackIcon: AppCompatImageView? = itemView.findViewById(R.id.ice_cream_icon)
        val trackPhotoFilter: View? = itemView.findViewById(R.id.ice_cream_photo_filter)
    }

    fun setSpotifyAppRemote(remote: SpotifyAppRemote) {
        spotifyRemote = remote
    }

    override fun getViewHolder(itemView: View): ViewHolder {
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(item: TrackSimple, viewHolder: ViewHolder, position: Int) {
        val context = viewHolder.itemView.context

        // Set ice cream name and price
        viewHolder.trackNameView.text = item.name
        // viewHolder.trackPriceView.text = context.getString(R.string.priceFormat, item.price)

/*
        // Set ice cream icon color
        val red = (item.colorRed * 255).toInt()
        val green = (item.colorGreen * 255).toInt()
        val blue = (item.colorBlue * 255).toInt()

        // Set the icon/image color
        if (viewHolder.trackIcon != null) {
            val trackIconColor = Color.rgb(red, green, blue)
            ImageViewCompat.setImageTintList(viewHolder.trackIcon, ColorStateList.valueOf(trackIconColor))
        } else if (viewHolder.trackPhotoFilter != null) {
            val trackPhotoFilter = Color.argb(128, red, green, blue)
            viewHolder.trackPhotoFilter.setBackgroundColor(trackPhotoFilter)
        }
*/

    }

    override fun getViewToTouchToStartDraggingItem(item: TrackSimple, viewHolder: ViewHolder, position: Int) = viewHolder.dragIcon

    override fun onDragStarted(item: TrackSimple, viewHolder: TrackListAdapter.ViewHolder) {
        Logger.log("Dragging started on ${item.name}")
    }

    override fun onSwipeStarted(item: TrackSimple, viewHolder: TrackListAdapter.ViewHolder) {
        Logger.log("Swiping started on ${item.name}")
    }

    override fun onIsDragging(
            item: TrackSimple,
            viewHolder: TrackListAdapter.ViewHolder,
            offsetX: Int,
            offsetY: Int,
            canvasUnder: Canvas?,
            canvasOver: Canvas?,
            isUserControlled: Boolean) {
        // Call commented out to avoid saturating the log
        //Logger.log("The ${if (isUserControlled) "User" else "System"} is dragging ${item.name} (offset X: $offsetX, offset Y: $offsetY)")
    }

    override fun onIsSwiping(
            item: TrackSimple?,
            viewHolder: TrackListAdapter.ViewHolder,
            offsetX: Int,
            offsetY: Int,
            canvasUnder: Canvas?,
            canvasOver: Canvas?,
            isUserControlled: Boolean) {
        // Call commented out to avoid saturating the log
        //Logger.log("The ${if (isUserControlled) "User" else "System"} is swiping ${item?.name} (offset X: $offsetX, offset Y: $offsetY)")
    }

    override fun onDragFinished(item: TrackSimple, viewHolder: TrackListAdapter.ViewHolder) {
        Logger.log("Dragging finished on ${item.name} (the item was dropped)")
    }
    override fun onSwipeAnimationFinished(viewHolder: TrackListAdapter.ViewHolder) {
        Logger.log("Swiping animation finished")
    }

    override fun onClick(item: TrackSimple) {
        Logger.log("Playing ${item.name}")
        spotifyRemote?.getPlayerApi()?.play(item.uri)
    }
}