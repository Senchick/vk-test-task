package com.example.vktesttask.ui.adapter

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.vktesttask.MainActivity
import com.example.vktesttask.R
import com.example.vktesttask.databinding.LayoutFolderItemBinding
import com.example.vktesttask.model.FileType
import com.example.vktesttask.model.Folder
import com.example.vktesttask.util.formatBytes
import java.text.SimpleDateFormat
import java.util.*

class FolderAdapter(
    private val activity: MainActivity,
    private val onFileSelected: (Folder) -> Unit
) : Adapter<FolderAdapter.FolderHolder>() {
    private var data: List<Folder> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderHolder {
        val binding = LayoutFolderItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        val holder = FolderHolder(binding)

        with(holder.itemView) {
            setOnLongClickListener {
                val item = data[holder.adapterPosition]
                if (item.fileType != FileType.DIR) {
                    val uri = item.file.toUri()
                    val shareIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, uri)

                        type = activity.contentResolver.getType(uri)
                    }

                    activity.startActivity(Intent.createChooser(shareIntent, null))
                }

                false
            }
            setOnClickListener {
                val item = data[holder.adapterPosition]

                onFileSelected(item)
            }
        }

        return holder
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: FolderHolder, position: Int) {
        val binding = holder.binding
        val item = data[position]

        binding.apply {
            title.text = item.name
            if (item.fileType == FileType.DIR) {
                val count = item.subFiles ?: 0
                subtitle.text = "$count " + activity.resources.getQuantityText(R.plurals.element, count)
                    .toString() + "  |  " + convertLongToFormattedDate(item.creationDate)
            } else {
                subtitle.text = formatBytes(item.size ?: 0) + "  |  " + convertLongToFormattedDate(item.creationDate)
            }

            status.visibility = if (item.isChanged) View.VISIBLE else View.GONE

            imageView.setImageResource(
                when (item.fileType) {
                    FileType.FILE -> R.drawable.baseline_file_24
                    FileType.VIDEO -> R.drawable.baseline_video_24
                    FileType.IMAGE -> R.drawable.baseline_image_24
                    FileType.AUDIO -> R.drawable.baseline_audiotrack_24
                    FileType.TEXT -> R.drawable.baseline_text_24
                    FileType.DIR -> R.drawable.baseline_folder_24
                }
            )
        }
    }

    private fun convertLongToFormattedDate(timeInMillis: Long): String {
        val formatter = SimpleDateFormat("HH:mm dd.MM.yyyy", Locale.getDefault())
        val date = Date(timeInMillis)

        return formatter.format(date)
    }

    override fun getItemCount() = data.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newData: List<Folder>) {
        data = newData
        notifyDataSetChanged()
    }

    class FolderHolder(val binding: LayoutFolderItemBinding) : ViewHolder(binding.root)
}
