package www.com.petsitternow_app.view.adapter

import android.widget.ImageView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import www.com.petsitternow_app.R
import www.com.petsitternow_app.domain.model.WalkRequest
import www.com.petsitternow_app.domain.model.WalkStatus
import www.com.petsitternow_app.util.TimeFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WalkHistoryAdapter(
    private var walks: List<WalkRequest>,
    private val onClick: (WalkRequest) -> Unit,
    private val onRatePetsitter: ((WalkRequest) -> Unit)? = null
) : RecyclerView.Adapter<WalkHistoryAdapter.WalkHistoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalkHistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_walk_history, parent, false)
        return WalkHistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: WalkHistoryViewHolder, position: Int) {
        holder.bind(walks[position])
    }

    override fun getItemCount() = walks.size

    fun updateWalks(newWalks: List<WalkRequest>) {
        walks = newWalks
        notifyDataSetChanged()
    }

    inner class WalkHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvPetsitterInitial: TextView = itemView.findViewById(R.id.tvPetsitterInitial)
        private val tvPetsitterName: TextView = itemView.findViewById(R.id.tvPetsitterName)
        private val tvPetNames: TextView = itemView.findViewById(R.id.tvPetNames)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        private val tvActualDuration: TextView = itemView.findViewById(R.id.tvActualDuration)
        private val layoutRating: View = itemView.findViewById(R.id.layoutRating)
        private val layoutRatingStars: View = itemView.findViewById(R.id.layoutRatingStars)
        private val ivStar1: ImageView = itemView.findViewById(R.id.ivStar1)
        private val ivStar2: ImageView = itemView.findViewById(R.id.ivStar2)
        private val ivStar3: ImageView = itemView.findViewById(R.id.ivStar3)
        private val ivStar4: ImageView = itemView.findViewById(R.id.ivStar4)
        private val ivStar5: ImageView = itemView.findViewById(R.id.ivStar5)
        private val btnRatePetsitter: MaterialButton = itemView.findViewById(R.id.btnRatePetsitter)

        fun bind(walk: WalkRequest) {
            // Date
            val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.FRANCE)
            walk.createdAt?.let { createdAt ->
                tvDate.text = dateFormat.format(Date(createdAt))
            } ?: run {
                tvDate.text = "-"
            }

            // Status
            when (walk.status) {
                WalkStatus.COMPLETED -> {
                    tvStatus.text = "Terminee"
                    tvStatus.setTextColor(itemView.context.getColor(R.color.success))
                    tvStatus.setBackgroundResource(R.drawable.bg_badge_success)
                }
                WalkStatus.CANCELLED -> {
                    tvStatus.text = "Annulee"
                    tvStatus.setTextColor(itemView.context.getColor(R.color.text_muted))
                    tvStatus.setBackgroundResource(R.drawable.bg_badge_age)
                }
                WalkStatus.FAILED, WalkStatus.EXPIRED -> {
                    tvStatus.text = "Echouee"
                    tvStatus.setTextColor(0xFFEF4444.toInt())
                    tvStatus.setBackgroundResource(R.drawable.bg_walk_status_failed)
                }
                else -> {
                    tvStatus.text = "En cours"
                    tvStatus.setTextColor(itemView.context.getColor(R.color.primary))
                    tvStatus.setBackgroundResource(R.drawable.bg_badge_primary)
                }
            }

            // Petsitter info
            walk.petsitter?.let { petsitter ->
                val fullName = "${petsitter.firstName} ${petsitter.lastName}".trim()
                tvPetsitterName.text = if (fullName.isNotEmpty()) fullName else petsitter.name
                tvPetsitterInitial.text = (petsitter.firstName.firstOrNull() ?: petsitter.name.firstOrNull() ?: '?').uppercase()
            } ?: run {
                tvPetsitterName.text = "Non assigne"
                tvPetsitterInitial.text = "?"
            }

            // Pet names from owner info
            val petNames = walk.owner.pets.map { it.name }
            tvPetNames.text = if (petNames.isNotEmpty()) {
                petNames.joinToString(", ")
            } else {
                "${walk.owner.pets.size} chien${if (walk.owner.pets.size > 1) "s" else ""}"
            }

            // Duration
            tvDuration.text = "${walk.duration} min"

            // Actual duration (if completed)
            val actualDuration = if (walk.completedAt != null && walk.assignedAt != null) {
                val durationMs = walk.completedAt - walk.assignedAt
                TimeFormatter.formatDurationMs(durationMs)
            } else {
                "-"
            }
            tvActualDuration.text = actualDuration

            // Rating row: only for completed walks with a petsitter (owner history)
            val isCompletedWithPetsitter = walk.status == WalkStatus.COMPLETED && walk.petsitter != null
            if (isCompletedWithPetsitter) {
                layoutRating.visibility = View.VISIBLE
                val rating = walk.petsitter?.rating
                if (rating != null) {
                    layoutRatingStars.visibility = View.VISIBLE
                    btnRatePetsitter.visibility = View.GONE
                    val starFilled = ContextCompat.getColor(itemView.context, R.color.star_filled)
                    val starOutline = ContextCompat.getColor(itemView.context, R.color.star_outline)
                    val score = rating.score.coerceIn(1, 5)
                    listOf(ivStar1, ivStar2, ivStar3, ivStar4, ivStar5).forEachIndexed { index, iv ->
                        iv.setImageResource(if (index < score) R.drawable.ic_star else R.drawable.ic_star_outline)
                        iv.setColorFilter(if (index < score) starFilled else starOutline)
                    }
                } else {
                    layoutRatingStars.visibility = View.GONE
                    btnRatePetsitter.visibility = View.VISIBLE
                    btnRatePetsitter.setOnClickListener { onRatePetsitter?.invoke(walk) }
                }
            } else {
                layoutRating.visibility = View.GONE
            }

            itemView.setOnClickListener {
                onClick(walk)
            }
        }
    }
}
