package com.ernestoyaquello.dragdropswiperecyclerview.listener

/**
 * Listener for the scroll events on the list.
 */
interface OnItemClickListener<T> {
    /**
     * Callback for whenever a list item is clicked
     *
     */
    fun onItemClicked(item: T)
}