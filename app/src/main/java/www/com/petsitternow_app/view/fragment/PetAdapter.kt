package www.com.petsitternow_app.view.fragment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import www.com.petsitternow_app.R
import www.com.petsitternow_app.domain.repository.Pet
import java.util.Calendar

class PetAdapter(
    private var pets: List<Pet>,
    private val onPetClick: (Pet) -> Unit
) : RecyclerView.Adapter<PetAdapter.PetViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pet_card, parent, false)
        return PetViewHolder(view)
    }

    override fun onBindViewHolder(holder: PetViewHolder, position: Int) {
        holder.bind(pets[position])
    }

    override fun getItemCount() = pets.size

    fun updatePets(newPets: List<Pet>) {
        pets = newPets
        notifyDataSetChanged()
    }

    inner class PetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPhoto: ImageView = itemView.findViewById(R.id.ivPhoto)
        private val tvEmoji: TextView = itemView.findViewById(R.id.tvEmoji)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvBreed: TextView = itemView.findViewById(R.id.tvBreed)
        private val tvAge: TextView = itemView.findViewById(R.id.tvAge)

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
            
            val age = calculateAge(pet.birthDate)
            tvAge.text = if (age != null) {
                "$age ${if (age <= 1) "an" else "ans"}"
            } else {
                ""
            }

            itemView.setOnClickListener {
                onPetClick(pet)
            }
        }

        private fun calculateAge(birthDate: String): Int? {
            return try {
                val parts = birthDate.split("-")
                if (parts.size != 3) return null
                val birthYear = parts[0].toInt()
                val birthMonth = parts[1].toInt()
                val birthDay = parts[2].toInt()
                
                val today = Calendar.getInstance()
                var age = today.get(Calendar.YEAR) - birthYear
                
                if (today.get(Calendar.MONTH) + 1 < birthMonth ||
                    (today.get(Calendar.MONTH) + 1 == birthMonth && today.get(Calendar.DAY_OF_MONTH) < birthDay)) {
                    age--
                }
                if (age < 0) null else age
            } catch (e: Exception) {
                null
            }
        }
    }
}
