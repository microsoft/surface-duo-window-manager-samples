package com.example.travelplanner.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.travelplanner.R
import com.google.android.material.card.MaterialCardView

class RecyclerBaseAdapter(val activity: AppCompatActivity, var onRecyclerEventListener: OnRecyclerEventListener? = null) : RecyclerView.Adapter<RecyclerBaseAdapter.BaseViewHolder>() {
    private var layoutManager: RecyclerView.LayoutManager? = null

    var items: List<Any> = arrayListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var selectedPosition: Int = 0
        set(value) {
            if (value != field) {
                onRecyclerEventListener?.onItemDeselected(field)
                notifyItemChanged(field)
                field = value
                notifyItemChanged(field)
                onRecyclerEventListener?.onItemSelected(field)

                layoutManager?.scrollToPosition(field)
            }
        }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        layoutManager = recyclerView.layoutManager
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_item, parent, false)
        return BaseViewHolder(view, onRecyclerEventListener)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        holder.bind(items[position], position == selectedPosition)
    }

    inner class BaseViewHolder(itemView: View, val onRecyclerEventListener: OnRecyclerEventListener?) : RecyclerView.ViewHolder(itemView), CardAdapter.OnCardEventListener {
        val cardAdapter = CardAdapter(itemView.findViewById(R.id.recycler_item_card), activity, this)
        val rootView: MaterialCardView = itemView.findViewById(R.id.recycler_item_root)

        fun bind(item: Any, selected: Boolean) {
            cardAdapter.attach(item)

            rootView.isChecked = selected
            cardAdapter.expanded = selected
        }

        override fun onCardExpanded() {
            selectedPosition = adapterPosition
        }

        override fun onCardOpenClicked() {
            onRecyclerEventListener?.onItemOpened(adapterPosition)
        }

        override fun onCardClipClicked() {
            onRecyclerEventListener?.onItemClipped(adapterPosition)
        }

        override fun onCardLaunchClicked() {
            onRecyclerEventListener?.onItemLaunched(adapterPosition)
        }

        override fun onCardDeleteClicked() {
            onRecyclerEventListener?.onItemDeleted(adapterPosition)
        }

        override fun onCardTravelClicked() {
            onRecyclerEventListener?.onItemTravelClicked(adapterPosition)
        }

        override fun onCardLongClicked(view: View) {
            onRecyclerEventListener?.onItemLongClicked(adapterPosition, rootView)
        }
    }

    interface OnRecyclerEventListener {
        fun onItemSelected(position: Int) {}
        fun onItemDeselected(position: Int) {}
        fun onItemOpened(position: Int) {}
        fun onItemDeleted(position: Int) {}
        fun onItemClipped(position: Int) {}
        fun onItemLaunched(position: Int) {}
        fun onItemTravelClicked(position: Int) {}
        fun onItemLongClicked(position: Int, view: View) {}
    }
}
