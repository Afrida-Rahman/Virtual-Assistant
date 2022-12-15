package org.mmh.virtual_assistant.core

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import org.mmh.virtual_assistant.R

class ExerciseInfoAdapter(
    private val imageUrls: List<String>
) : RecyclerView.Adapter<ExerciseInfoAdapter.ExerciseInfoViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseInfoViewHolder {
        return ExerciseInfoViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_exercise_guideline_image_list, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ExerciseInfoViewHolder, position: Int) {
        val image = imageUrls[position]
        val context = holder.imageView.context
        Glide.with(context)
            .load(image)
            .diskCacheStrategy(DiskCacheStrategy.DATA)
            .thumbnail(Glide.with(context).load(R.drawable.gif_loading).centerCrop())
            .transition(DrawableTransitionOptions.withCrossFade(500))
            .override(600)
            .into(holder.imageView)
    }

    override fun getItemCount(): Int = imageUrls.size

    class ExerciseInfoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView =
            view.findViewById(R.id.item_exercise_guideline_image_list_container)
    }
}
