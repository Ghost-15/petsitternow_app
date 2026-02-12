package www.com.petsitternow_app.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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

class MissionHistoryAdapter(
    private var missions: List<WalkRequest>,
    private val userRole: String?,
    private val onClick: (WalkRequest) -> Unit,
    private val onRatePetsitter: ((WalkRequest) -> Unit)? = null,
    private val onRateOwner: ((WalkRequest) -> Unit)? = null
) : RecyclerView.Adapter<MissionHistoryAdapter.MissionHistoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MissionHistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mission_history, parent, false)
        return MissionHistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: MissionHistoryViewHolder, position: Int) {
        holder.bind(missions[position])
    }

    override fun getItemCount() = missions.size

    fun updateMissions(newMissions: List<WalkRequest>) {
        missions = newMissions
        notifyDataSetChanged()
    }

    inner class MissionHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvPersonInitial: TextView = itemView.findViewById(R.id.tvPersonInitial)
        private val tvPersonName: TextView = itemView.findViewById(R.id.tvPersonName)
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
        private val btnRate: MaterialButton = itemView.findViewById(R.id.btnRate)

        fun bind(mission: WalkRequest) {
            // Date
            val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.FRANCE)
            mission.createdAt?.let { createdAt ->
                tvDate.text = dateFormat.format(Date(createdAt))
            } ?: run {
                tvDate.text = "-"
            }

            // Status
            when (mission.status) {
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

            // Person info: petsitter name for owner, owner name for petsitter
            when (userRole) {
                "owner" -> mission.petsitter?.let { petsitter ->
                    val fullName = "${petsitter.firstName} ${petsitter.lastName}".trim()
                    tvPersonName.text = if (fullName.isNotEmpty()) fullName else petsitter.name
                    tvPersonInitial.text = (petsitter.firstName.firstOrNull() ?: petsitter.name.firstOrNull() ?: '?').uppercase()
                } ?: run {
                    tvPersonName.text = "Petsitter"
                    tvPersonInitial.text = "?"
                }
                else -> {
                    val owner = mission.owner
                    val fullName = "${owner.firstName} ${owner.lastName}".trim()
                    tvPersonName.text = if (fullName.isNotEmpty()) fullName else owner.name
                    tvPersonInitial.text = (owner.firstName.firstOrNull() ?: owner.name.firstOrNull() ?: '?').uppercase()
                }
            }

            // Pet names from owner info
            val petNames = mission.owner.pets.map { it.name }
            tvPetNames.text = if (petNames.isNotEmpty()) {
                petNames.joinToString(", ")
            } else {
                "${mission.owner.pets.size} chien${if (mission.owner.pets.size > 1) "s" else ""}"
            }

            // Duration
            tvDuration.text = "${mission.duration} min"

            // Actual duration
            val actualDuration = if (mission.completedAt != null && mission.assignedAt != null) {
                val durationMs = mission.completedAt - mission.assignedAt
                TimeFormatter.formatDurationMs(durationMs)
            } else {
                "-"
            }
            tvActualDuration.text = actualDuration

            // Rating row: only for completed missions
            val isCompleted = mission.status == WalkStatus.COMPLETED
            if (isCompleted && userRole != null) {
                layoutRating.visibility = View.VISIBLE
                val isOwner = userRole == "owner"
                val rating = if (isOwner) mission.petsitter?.rating else mission.owner.rating
                val hasRateButton = if (isOwner) mission.petsitter != null && mission.petsitter?.rating == null
                else mission.owner.rating == null
                if (rating != null) {
                    layoutRatingStars.visibility = View.VISIBLE
                    btnRate.visibility = View.GONE
                    val starFilled = ContextCompat.getColor(itemView.context, R.color.star_filled)
                    val starOutline = ContextCompat.getColor(itemView.context, R.color.star_outline)
                    val score = rating.score.coerceIn(1, 5)
                    listOf(ivStar1, ivStar2, ivStar3, ivStar4, ivStar5).forEachIndexed { index, iv ->
                        iv.setImageResource(if (index < score) R.drawable.ic_star else R.drawable.ic_star_outline)
                        iv.setColorFilter(if (index < score) starFilled else starOutline)
                    }
                } else if (hasRateButton) {
                    layoutRatingStars.visibility = View.GONE
                    btnRate.visibility = View.VISIBLE
                    btnRate.text = itemView.context.getString(if (isOwner) R.string.btn_rate_petsitter else R.string.btn_rate_owner)
                    btnRate.setOnClickListener {
                        if (isOwner) onRatePetsitter?.invoke(mission) else onRateOwner?.invoke(mission)
                    }
                } else {
                    layoutRating.visibility = View.GONE
                }
            } else {
                layoutRating.visibility = View.GONE
            }

            itemView.setOnClickListener {
                onClick(mission)
            }
        }
    }
}
