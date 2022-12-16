package org.mmh.virtual_assistant.core

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import org.mmh.virtual_assistant.R

class ImageSliderAdapter(
    private val context: Context,
//    private var phaseNumber: List<String>,
//    private var phaseDescription: List<String>,
    private var phaseImages: List<String>
) : RecyclerView.Adapter<ImageSliderAdapter.ImageSliderViewHolder>() {

    class ImageSliderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val phaseNumber: TextView = view.findViewById(R.id.phase_number)
        val phaseDescription: TextView = view.findViewById(R.id.phase_description)
        val phaseImage: ImageView = view.findViewById(R.id.phase_image)

        init {
            phaseImage.setOnClickListener { v: View ->
                val position = absoluteAdapterPosition
                Toast.makeText(
                    phaseImage.context,
                    "clicked on phase ${position + 1}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
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
//        holder.phaseNumber.text = phaseNumber[position]
//        holder.phaseDescription.text = phaseDescription[position]
        val image = phaseImages[position]
        Glide.with(context)
            .load(image)
            .diskCacheStrategy(DiskCacheStrategy.DATA)
            .thumbnail(Glide.with(context).load(R.drawable.gif_loading).centerCrop())
            .transition(DrawableTransitionOptions.withCrossFade(300))
            .override(600)
            .into(holder.phaseImage)
    }

    override fun getItemCount(): Int {
        return phaseImages.size
    }
}