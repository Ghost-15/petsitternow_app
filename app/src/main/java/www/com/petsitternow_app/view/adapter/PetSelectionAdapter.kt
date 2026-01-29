package www.com.petsitternow_app.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import www.com.petsitternow_app.R
import www.com.petsitternow_app.domain.repository.Pet

class PetSelectionAdapter(
    private var pets: List<Pet>,
    private val onSelectionChanged: (List<String>) -> Unit
) : RecyclerView.Adapter<PetSelectionAdapter.PetSelectionViewHolder>() {

    private val selectedPetIds = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetSelectionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pet_selection, parent, false)
        return PetSelectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: PetSelectionViewHolder, position: Int) {
        holder.bind(pets[position])
    }

    override fun getItemCount() = pets.size

    fun updatePets(newPets: List<Pet>) {
        pets = newPets
        selectedPetIds.clear()
        notifyDataSetChanged()
        onSelectionChanged(emptyList())
    }

    fun getSelectedPetIds(): List<String> = selectedPetIds.toList()

    inner class PetSelectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPhoto: ImageView = itemView.findViewById(R.id.ivPhoto)
        private val tvEmoji: TextView = itemView.findViewById(R.id.tvEmoji)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvBreed: TextView = itemView.findViewById(R.id.tvBreed)
        private val checkbox: CheckBox = itemView.findViewById(R.id.checkbox)

        fun bind(pet: Pet) {
            tvName.text = pet.name
            tvBreed.text = pet.breed

            if (pet.photos.isNotEmpty()) {
                ivPhoto.visibility = View.VISIBLE
                tvEmoji.visibility = View.GONE
                ivPhoto.load(pet.photos.first()) {
                    crossfade(true)
                    transformations(CircleCropTransformation())
                }
            } else {
                ivPhoto.visibility = View.GONE
                tvEmoji.visibility = View.VISIBLE
                tvEmoji.text = "🐕"
            }

            checkbox.isChecked = selectedPetIds.contains(pet.id)

            val clickListener = View.OnClickListener {
                if (selectedPetIds.contains(pet.id)) {
                    selectedPetIds.remove(pet.id)
                } else {
                    selectedPetIds.add(pet.id)
                }
                checkbox.isChecked = selectedPetIds.contains(pet.id)
                onSelectionChanged(selectedPetIds.toList())
            }

            itemView.setOnClickListener(clickListener)
            checkbox.setOnClickListener(clickListener)
        }
    }
}
