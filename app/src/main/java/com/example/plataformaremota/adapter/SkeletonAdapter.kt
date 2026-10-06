package com.example.plataformaremota.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.plataformaremota.ShimmerHelper

class SkeletonAdapter(
    private val layoutRes: Int,
    private val quantidade: Int = 5
) : RecyclerView.Adapter<SkeletonAdapter.SkeletonViewHolder>() {

    class SkeletonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SkeletonViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        ShimmerHelper.animarRecursivo(view)
        return SkeletonViewHolder(view)
    }

    override fun onBindViewHolder(holder: SkeletonViewHolder, position: Int) {
        // Nada a fazer
    }

    override fun getItemCount(): Int = quantidade

    override fun onViewRecycled(holder: SkeletonViewHolder) {
        super.onViewRecycled(holder)
        ShimmerHelper.pararRecursivo(holder.itemView)
    }
}