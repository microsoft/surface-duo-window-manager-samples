package com.example.video_trivia_sample

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.video_trivia_sample.model.ActorInfo

class CastListAdapter(private val dataSet: List<ActorInfo>) : RecyclerView.Adapter<CastListAdapter.ViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val actorPic: ImageView = view.findViewById(R.id.actor_pic)
        private val actorText1: TextView = view.findViewById(R.id.actor_text_1)
        private val actorText2: TextView = view.findViewById(R.id.actor_text_2)

        fun bind(actorInfo: ActorInfo) {
            actorPic.setImageResource(actorInfo.picId)
            actorText1.setText(actorInfo.text1Id)
            actorText2.setText(actorInfo.text2Id)
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.cast_member_info_layout, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.bind(dataSet[position])
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size
}
