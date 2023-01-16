package org.mmh.virtual_assistant.core

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import org.mmh.virtual_assistant.R

class ExerciseGuidelineImageListAdapter(
    private val context: Context,
    private val exerciseImageUrls: List<String>
) : RecyclerView.Adapter<ExerciseGuidelineImageListAdapter.ExerciseImageItemViewHolder>() {

    class ExerciseImageItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val exerciseImageUrlsView: ImageView =
            view.findViewById(R.id.item_exercise_guideline_image_list_container)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseImageItemViewHolder {
        return ExerciseImageItemViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_exercise_guideline_image_list, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ExerciseImageItemViewHolder, position: Int) {
        val imageUrl = exerciseImageUrls[position]
        Glide.with(context)
            .load(imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.DATA)
            .thumbnail(Glide.with(context).load(R.drawable.gif_loading).centerCrop())
            .transition(DrawableTransitionOptions.withCrossFade(300))
            .override(600)
            .into(holder.exerciseImageUrlsView)
    }

    override fun getItemCount(): Int = exerciseImageUrls.size
}