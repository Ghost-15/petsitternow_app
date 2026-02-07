package www.com.petsitternow_app.domain.model

/**
 * Walk status enum matching web implementation.
 * @see lib/walks/constants.ts
 */
enum class WalkStatus(val value: String) {
    PENDING("pending"),
    MATCHING("matching"),
    ASSIGNED("assigned"),
    GOING_TO_OWNER("going_to_owner"),
    IN_PROGRESS("in_progress"),
    WALKING("walking"),
    RETURNING("returning"),
    COMPLETED("completed"),
    CANCELLED("cancelled"),
    FAILED("failed"),
    EXPIRED("expired"),
    DISMISSED("dismissed");

    companion object {
        fun fromValue(value: String?): WalkStatus {
            return entries.find { it.value == value } ?: PENDING
        }

        val FINAL_STATUSES = listOf(COMPLETED, CANCELLED, FAILED, EXPIRED)
        val WALKING_PHASE_STATUSES = listOf(WALKING, RETURNING)
        val ROUTE_PHASE_STATUSES = listOf(GOING_TO_OWNER, ASSIGNED, RETURNING)
        val ASSIGNED_PHASE_STATUSES = listOf(ASSIGNED, GOING_TO_OWNER)
    }

    fun isFinal(): Boolean = this in FINAL_STATUSES
    fun isWalkingPhase(): Boolean = this in WALKING_PHASE_STATUSES
    fun isRoutePhase(): Boolean = this in ROUTE_PHASE_STATUSES
    fun isAssignedPhase(): Boolean = this in ASSIGNED_PHASE_STATUSES
}

/**
 * Walk duration options.
 */
enum class WalkDuration(val minutes: Int, val value: String) {
    DURATION_30(30, "30"),
    DURATION_45(45, "45"),
    DURATION_60(60, "60");

    companion object {
        fun fromValue(value: String?): WalkDuration {
            return entries.find { it.value == value } ?: DURATION_30
        }
    }
}

/**
 * Location data for walk pickup point.
 */
data class WalkLocation(
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val address: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "lat" to lat,
        "lng" to lng,
        "address" to address
    )

    companion object {
        fun fromMap(map: Map<String, Any?>?): WalkLocation {
            if (map == null) return WalkLocation()
            return WalkLocation(
                lat = (map["lat"] as? Number)?.toDouble() ?: 0.0,
                lng = (map["lng"] as? Number)?.toDouble() ?: 0.0,
                address = map["address"] as? String ?: ""
            )
        }
    }
}

/**
 * Petsitter info embedded in walk request.
 */
data class PetsitterInfo(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val name: String = "",
    val photoUrl: String? = null
) {
    companion object {
        fun fromMap(map: Map<String, Any?>?): PetsitterInfo? {
            if (map == null) return null
            return PetsitterInfo(
                id = map["id"] as? String ?: "",
                firstName = map["firstName"] as? String ?: "",
                lastName = map["lastName"] as? String ?: "",
                name = map["name"] as? String ?: "",
                photoUrl = map["photoUrl"] as? String
            )
        }
    }
}

/**
 * Pet info embedded in owner info within walk request.
 */
data class PetInfo(
    val id: String = "",
    val name: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "name" to name
    )

    companion object {
        fun fromMap(map: Map<String, Any?>?): PetInfo {
            if (map == null) return PetInfo()
            return PetInfo(
                id = map["id"] as? String ?: "",
                name = map["name"] as? String ?: ""
            )
        }
    }
}

/**
 * Owner info embedded in walk request.
 */
data class OwnerInfo(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val name: String = "",
    val pets: List<PetInfo> = emptyList()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "firstName" to firstName,
        "lastName" to lastName,
        "name" to name,
        "pets" to pets.map { it.toMap() }
    )

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>?): OwnerInfo {
            if (map == null) return OwnerInfo()
            val petsList = (map["pets"] as? List<Map<String, Any?>>)?.map { PetInfo.fromMap(it) } ?: emptyList()
            return OwnerInfo(
                id = map["id"] as? String ?: "",
                firstName = map["firstName"] as? String ?: "",
                lastName = map["lastName"] as? String ?: "",
                name = map["name"] as? String ?: "",
                pets = petsList
            )
        }
    }
}

/**
 * Main walk request document from Firestore.
 */
data class WalkRequest(
    val id: String = "",
    val location: WalkLocation = WalkLocation(),
    val duration: String = "30",
    val status: WalkStatus = WalkStatus.PENDING,
    val owner: OwnerInfo = OwnerInfo(),
    val petsitter: PetsitterInfo? = null,
    val createdAt: Long? = null,
    val matchingStartedAt: Long? = null,
    val assignedAt: Long? = null,
    val completedAt: Long? = null,
    val updatedAt: Long? = null,
    val cancelledBy: String? = null
) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(id: String, map: Map<String, Any?>): WalkRequest {
            return WalkRequest(
                id = id,
                location = WalkLocation.fromMap(map["location"] as? Map<String, Any?>),
                duration = map["duration"] as? String ?: "30",
                status = WalkStatus.fromValue(map["status"] as? String),
                owner = OwnerInfo.fromMap(map["owner"] as? Map<String, Any?>),
                petsitter = PetsitterInfo.fromMap(map["petsitter"] as? Map<String, Any?>),
                createdAt = (map["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.time,
                matchingStartedAt = (map["matchingStartedAt"] as? com.google.firebase.Timestamp)?.toDate()?.time,
                assignedAt = (map["assignedAt"] as? com.google.firebase.Timestamp)?.toDate()?.time,
                completedAt = (map["completedAt"] as? com.google.firebase.Timestamp)?.toDate()?.time,
                updatedAt = (map["updatedAt"] as? com.google.firebase.Timestamp)?.toDate()?.time,
                cancelledBy = map["cancelledBy"] as? String
            )
        }
    }
}

/**
 * Active walk data from Realtime Database.
 * Path: active_walks/{requestId}
 */
data class ActiveWalk(
    val status: WalkStatus = WalkStatus.PENDING,
    val petsitterLocation: WalkLocation? = null,
    val ownerLocation: WalkLocation? = null,
    val walkStartedAt: Long? = null,
    val walkEndedAt: Long? = null
) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>?): ActiveWalk? {
            if (map == null) return null
            return ActiveWalk(
                status = WalkStatus.fromValue(map["status"] as? String),
                petsitterLocation = (map["petsitterLocation"] as? Map<String, Any?>)?.let {
                    WalkLocation(
                        lat = (it["lat"] as? Number)?.toDouble() ?: 0.0,
                        lng = (it["lng"] as? Number)?.toDouble() ?: 0.0,
                        address = ""
                    )
                },
                ownerLocation = (map["ownerLocation"] as? Map<String, Any?>)?.let {
                    WalkLocation(
                        lat = (it["lat"] as? Number)?.toDouble() ?: 0.0,
                        lng = (it["lng"] as? Number)?.toDouble() ?: 0.0,
                        address = ""
                    )
                },
                walkStartedAt = (map["walkStartedAt"] as? Number)?.toLong(),
                walkEndedAt = (map["walkEndedAt"] as? Number)?.toLong()
            )
        }
    }
}

/**
 * Pending mission notification for petsitter.
 * Path: petsitter_missions/{userId}
 */
data class PetsitterMission(
    val requestId: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val petNames: List<String> = emptyList(),
    val duration: String = "30",
    val distance: Double = 0.0,
    val location: WalkLocation = WalkLocation(),
    val expiresAt: Long = 0L
) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>?): PetsitterMission? {
            if (map == null) return null
            return PetsitterMission(
                requestId = map["requestId"] as? String ?: "",
                ownerId = map["ownerId"] as? String ?: "",
                ownerName = map["ownerName"] as? String ?: "",
                petNames = (map["petNames"] as? List<String>) ?: emptyList(),
                duration = map["duration"] as? String ?: "30",
                distance = (map["distance"] as? Number)?.toDouble() ?: 0.0,
                location = WalkLocation.fromMap(map["location"] as? Map<String, Any?>),
                expiresAt = (map["expiresAt"] as? Number)?.toLong() ?: 0L
            )
        }
    }

    fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt
    fun remainingSeconds(): Int = ((expiresAt - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
}

/**
 * Petsitter profile from Firestore.
 */
data class PetsitterProfile(
    val userId: String = "",
    val rating: Double = 0.0,
    val totalWalks: Int = 0,
    val acceptanceRate: Double = 100.0,
    val isOnline: Boolean = false,
    val fcmToken: String? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null
) {
    companion object {
        fun fromMap(map: Map<String, Any?>?): PetsitterProfile? {
            if (map == null) return null
            return PetsitterProfile(
                userId = map["userId"] as? String ?: "",
                rating = (map["rating"] as? Number)?.toDouble() ?: 0.0,
                totalWalks = (map["totalWalks"] as? Number)?.toInt() ?: 0,
                acceptanceRate = (map["acceptanceRate"] as? Number)?.toDouble() ?: 100.0,
                isOnline = map["isOnline"] as? Boolean ?: false,
                fcmToken = map["fcmToken"] as? String,
                createdAt = (map["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.time,
                updatedAt = (map["updatedAt"] as? com.google.firebase.Timestamp)?.toDate()?.time
            )
        }
    }
}

/**
 * Petsitter availability from RTDB.
 * Path: petsitters_available/{userId}
 */
data class PetsitterAvailability(
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val lastUpdate: Long = 0L,
    val isOnline: Boolean = false
) {
    fun toMap(): Map<String, Any> = mapOf(
        "lat" to lat,
        "lng" to lng,
        "lastUpdate" to lastUpdate,
        "isOnline" to isOnline
    )

    companion object {
        fun fromMap(map: Map<String, Any?>?): PetsitterAvailability? {
            if (map == null) return null
            return PetsitterAvailability(
                lat = (map["lat"] as? Number)?.toDouble() ?: 0.0,
                lng = (map["lng"] as? Number)?.toDouble() ?: 0.0,
                lastUpdate = (map["lastUpdate"] as? Number)?.toLong() ?: 0L,
                isOnline = map["isOnline"] as? Boolean ?: false
            )
        }
    }
}
