package org.mmh.virtual_assistant.core

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import org.mmh.virtual_assistant.R
import org.mmh.virtual_assistant.api.response.PhaseInfo1

class ImageSliderAdapter(
    private val context: Context,
    private val phases: List<PhaseInfo1>
) : RecyclerView.Adapter<ImageSliderAdapter.ImageSliderViewHolder>() {

    class ImageSliderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val phaseNumber: TextView = view.findViewById(R.id.phase_number)
        val phaseDescription: TextView = view.findViewById(R.id.phase_description)
        val phaseImage: ImageView = view.findViewById(R.id.phase_image)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ImageSliderViewHolder {
        return ImageSliderViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_image_slider, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ImageSliderViewHolder, position: Int) {
        val phase = phases[position]
        holder.phaseNumber.text = phase.PhaseNumber.toString()
        holder.phaseDescription.text = phase.PhaseDialogue
        Glide.with(context)
            .load(phase.CapturedImage)
            .diskCacheStrategy(DiskCacheStrategy.DATA)
            .thumbnail(Glide.with(context).load(R.drawable.gif_loading).centerCrop())
            .transition(DrawableTransitionOptions.withCrossFade(300))
            .override(600)
            .into(holder.phaseImage)
    }

    override fun getItemCount(): Int = phases.size
}