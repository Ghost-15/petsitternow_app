package www.com.petsitternow_app.ui.pet

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import www.com.petsitternow_app.R

sealed class PhotoItem {
    data class ExistingPhoto(val url: String) : PhotoItem()
    data class NewPhoto(val uri: Uri) : PhotoItem()
    object AddButton : PhotoItem()
}

class PhotoAdapter(
    private val onAddClick: () -> Unit,
    private val onRemoveClick: (PhotoItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<PhotoItem>()

    companion object {
        private const val TYPE_PHOTO = 0
        private const val TYPE_ADD = 1
    }

    fun updatePhotos(existingPhotos: List<String>, newPhotoUris: List<Uri>) {
        items.clear()
        existingPhotos.forEach { items.add(PhotoItem.ExistingPhoto(it)) }
        newPhotoUris.forEach { items.add(PhotoItem.NewPhoto(it)) }
        items.add(PhotoItem.AddButton)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is PhotoItem.AddButton -> TYPE_ADD
            else -> TYPE_PHOTO
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_ADD -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_photo_add, parent, false)
                AddViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_photo_edit, parent, false)
                PhotoViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is PhotoItem.ExistingPhoto -> (holder as PhotoViewHolder).bind(item)
            is PhotoItem.NewPhoto -> (holder as PhotoViewHolder).bindUri(item)
            is PhotoItem.AddButton -> (holder as AddViewHolder).bind()
        }
    }

    override fun getItemCount() = items.size

    inner class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPhoto: ImageView = itemView.findViewById(R.id.ivPhoto)
        private val btnRemove: ImageView = itemView.findViewById(R.id.btnRemove)

        fun bind(photo: PhotoItem.ExistingPhoto) {
            ivPhoto.load(photo.url) {
                crossfade(true)
            }
            btnRemove.setOnClickListener {
                onRemoveClick(photo)
            }
        }

        fun bindUri(photo: PhotoItem.NewPhoto) {
            ivPhoto.setImageURI(photo.uri)
            btnRemove.setOnClickListener {
                onRemoveClick(photo)
            }
        }
    }

    inner class AddViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardAddPhoto: CardView = itemView.findViewById(R.id.cardAddPhoto)

        fun bind() {
            cardAddPhoto.setOnClickListener {
                onAddClick()
            }
        }
    }
}
