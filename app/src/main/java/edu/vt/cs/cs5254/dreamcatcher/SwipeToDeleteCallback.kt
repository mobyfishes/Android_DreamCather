package edu.vt.cs.cs5254.dreamcatcher

import android.util.Log
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class SwipeToDeleteCallback(val adapter: DreamDetailFragment.DreamEntryAdapter) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT)  {


    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        Log.d("SWIP", "pos : ${viewHolder.absoluteAdapterPosition}")
        adapter.deleteItem(viewHolder.absoluteAdapterPosition)
    }


}