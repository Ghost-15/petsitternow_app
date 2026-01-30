package www.com.petsitternow_app.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import www.com.petsitternow_app.R
import www.com.petsitternow_app.domain.model.WalkRequest
import www.com.petsitternow_app.domain.model.WalkStatus
import www.com.petsitternow_app.util.TimeFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MissionHistoryAdapter(
    private var missions: List<WalkRequest>,
    private val onClick: (WalkRequest) -> Unit
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
        private val tvOwnerInitial: TextView = itemView.findViewById(R.id.tvOwnerInitial)
        private val tvOwnerName: TextView = itemView.findViewById(R.id.tvOwnerName)
        private val tvPetNames: TextView = itemView.findViewById(R.id.tvPetNames)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        private val tvActualDuration: TextView = itemView.findViewById(R.id.tvActualDuration)

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
                WalkStatus.FAILED -> {
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

            // Owner info
            mission.owner?.let { owner ->
                val fullName = "${owner.firstName} ${owner.lastName}".trim()
                tvOwnerName.text = if (fullName.isNotEmpty()) fullName else owner.name
                tvOwnerInitial.text = (owner.firstName.firstOrNull() ?: owner.name.firstOrNull() ?: '?').uppercase()
            } ?: run {
                tvOwnerName.text = "Proprietaire"
                tvOwnerInitial.text = "?"
            }

            // Pet names (placeholder)
            tvPetNames.text = "${mission.petIds.size} chien${if (mission.petIds.size > 1) "s" else ""}"

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

            itemView.setOnClickListener {
                onClick(mission)
            }
        }
    }
}
