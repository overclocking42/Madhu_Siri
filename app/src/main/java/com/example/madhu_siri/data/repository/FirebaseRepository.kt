package com.example.madhu_siri.data.repository

import com.example.madhu_siri.data.model.HealthLog
import com.example.madhu_siri.data.model.Hive
import com.example.madhu_siri.data.model.NotificationAlert
import com.example.madhu_siri.data.model.AppThemePreference
import com.example.madhu_siri.data.model.SprayAlertDraft
import com.example.madhu_siri.data.model.SprayEvent
import com.example.madhu_siri.data.model.SpraySubmissionResult
import com.example.madhu_siri.data.model.User
import com.example.madhu_siri.utils.BeeTimingUtil
import com.example.madhu_siri.utils.HaversineUtil
import com.google.firebase.Timestamp
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class FirebaseRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val messaging: FirebaseMessaging = FirebaseMessaging.getInstance()
) {
    companion object {
        const val USERS = "users"
        const val HIVES = "hives"
        const val SPRAY_EVENTS = "spray_events"
        const val NOTIFICATIONS = "notifications"
        const val HEALTH_LOGS = "health_logs"
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun signInWithGoogle(credential: AuthCredential) {
        auth.signInWithCredential(credential).await()
    }

    suspend fun upsertGoogleUserProfile(
        selectedRole: String,
        preferredLanguage: String,
        fullNameOverride: String = "",
        phoneNumberOverride: String = ""
    ): User {
        val firebaseUser = auth.currentUser ?: error("No Google account is signed in.")
        val document = db.collection(USERS).document(firebaseUser.uid)
        val existing = document.get().await().toObject(User::class.java)
        val merged = mergeGoogleUser(
            firebaseUser = firebaseUser,
            existing = existing,
            selectedRole = selectedRole,
            preferredLanguage = preferredLanguage,
            fullNameOverride = fullNameOverride,
            phoneNumberOverride = phoneNumberOverride
        )
        document.set(merged).await()
        return merged
    }

    suspend fun register(
        fullName: String,
        email: String,
        password: String,
        role: String,
        phoneNumber: String,
        preferredLanguage: String
    ) {
        auth.createUserWithEmailAndPassword(email, password).await()
        val uid = auth.currentUser?.uid ?: return
        db.collection(USERS).document(uid).set(
            User(
                uid = uid,
                fullName = fullName,
                email = email,
                phoneNumber = phoneNumber,
                role = role,
                preferredLanguage = preferredLanguage,
                themePreference = AppThemePreference.SYSTEM.name,
                createdAt = Timestamp.now()
            )
        ).await()
    }

    suspend fun saveUserRole(role: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection(USERS).document(uid).update(mapOf("role" to role)).await()
    }

    suspend fun syncCurrentUserFcmToken() {
        val uid = auth.currentUser?.uid ?: return
        val token = messaging.token.await()
        db.collection(USERS).document(uid).update(mapOf("fcmToken" to token)).await()
    }

    suspend fun getUserRole(): String? {
        val uid = auth.currentUser?.uid ?: return null
        return db.collection(USERS).document(uid).get().await().getString("role")
    }

    suspend fun getCurrentUserProfile(): User? {
        val uid = auth.currentUser?.uid ?: return null
        return db.collection(USERS).document(uid).get().await().toObject(User::class.java)
    }

    suspend fun findUserByEmail(email: String): User? {
        if (email.isBlank()) return null
        return db.collection(USERS)
            .whereEqualTo("email", email.trim())
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.toObject(User::class.java)
    }

    fun observeUser(userId: String?): Flow<User?> {
        if (userId == null) return flowOf(null)
        return callbackFlow {
            val subscription = db.collection(USERS).document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    trySend(snapshot?.toObject(User::class.java))
                }
            awaitClose { subscription.remove() }
        }
    }

    fun observeHives(): Flow<List<Hive>> = callbackFlow {
        val subscription = db.collection(HIVES)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(Hive::class.java).orEmpty())
            }
        awaitClose { subscription.remove() }
    }

    suspend fun addHive(name: String, lat: Double, lng: Double, notes: String) {
        addHive(name, lat, lng, notes, "08:00", "18:00")
    }

    suspend fun addHive(
        name: String,
        lat: Double,
        lng: Double,
        notes: String,
        activeStartTime: String,
        activeEndTime: String
    ) {
        val uid = auth.currentUser?.uid ?: return
        val user = getCurrentUserProfile()
        val safeStart = BeeTimingUtil.sanitizeTime(activeStartTime, "08:00")
        val safeEnd = BeeTimingUtil.sanitizeTime(activeEndTime, "18:00")
        val docRef = db.collection(HIVES).document()
        docRef.set(
            Hive(
                id = docRef.id,
                name = name,
                lat = lat,
                lng = lng,
                ownerId = uid,
                notes = notes,
                contactNumber = user?.phoneNumber.orEmpty(),
                activeStartTime = safeStart,
                activeEndTime = safeEnd,
                createdAt = Timestamp.now()
            )
        ).await()
    }

    suspend fun updateHive(
        hiveId: String,
        name: String,
        lat: Double,
        lng: Double,
        notes: String,
        activeStartTime: String,
        activeEndTime: String
    ) {
        val user = getCurrentUserProfile()
        val safeStart = BeeTimingUtil.sanitizeTime(activeStartTime, "08:00")
        val safeEnd = BeeTimingUtil.sanitizeTime(activeEndTime, "18:00")
        db.collection(HIVES).document(hiveId).update(
            mapOf(
                "name" to name,
                "lat" to lat,
                "lng" to lng,
                "notes" to notes,
                "contactNumber" to user?.phoneNumber.orEmpty(),
                "activeStartTime" to safeStart,
                "activeEndTime" to safeEnd
            )
        ).await()
    }

    suspend fun deleteHive(hiveId: String) {
        db.collection(HIVES).document(hiveId).delete().await()
    }

    suspend fun createSprayAlert(draft: SprayAlertDraft): SpraySubmissionResult {
        val farmerId = auth.currentUser?.uid ?: return SpraySubmissionResult()
        val lat = draft.latitude ?: return SpraySubmissionResult()
        val lng = draft.longitude ?: return SpraySubmissionResult()

        val duplicateCutoffMillis = System.currentTimeMillis() - 5 * 60 * 1000L
        val recentAlerts = db.collection(SPRAY_EVENTS)
            .whereEqualTo("farmerId", farmerId)
            .get()
            .await()
            .toObjects(SprayEvent::class.java)
            .filter { it.createdAt.toDate().time >= duplicateCutoffMillis }
        if (recentAlerts.isNotEmpty()) {
            return SpraySubmissionResult(
                error = "You already created a spray alert in the last 5 minutes. Please wait before sending another one.",
                preventedDuplicate = true
            )
        }

        val hives = db.collection(HIVES).get().await().toObjects(Hive::class.java)
        val nearbyHives = hives.filter { hive ->
            HaversineUtil.calculateDistance(lat, lng, hive.lat, hive.lng) <= 2.0
        }
        val conflictingHives = nearbyHives.filter { hive ->
            BeeTimingUtil.isSprayBlockedByBeeWindow(hive, draft.scheduledAtMillis)
        }
        if (conflictingHives.isNotEmpty()) {
            val suggestedTimes = BeeTimingUtil.suggestedSafeSprayTimes(conflictingHives, draft.scheduledAtMillis)
            return SpraySubmissionResult(
                nearbyHiveCount = nearbyHives.size,
                notifiedBeekeeperCount = nearbyHives.map { it.ownerId }.distinct().size,
                error = "That spray time overlaps with nearby bee-out hours. Please choose another safe time below.",
                conflictingHiveTimings = conflictingHives.map {
                    "${it.name}: bees out ${it.activeStartTime} - ${it.activeEndTime}"
                },
                nextSafeStartMillis = suggestedTimes.firstOrNull(),
                suggestedSafeTimes = suggestedTimes,
                blockedByActiveBees = true
            )
        }
        val docRef = db.collection(SPRAY_EVENTS).document()
        val event = SprayEvent(
            id = docRef.id,
            farmerId = farmerId,
            sprayType = draft.sprayType.name,
            chemicalName = draft.chemicalName,
            durationHours = draft.durationHours,
            scheduledAt = Timestamp(Date(draft.scheduledAtMillis)),
            lat = lat,
            lng = lng,
            notes = draft.notes,
            createdAt = Timestamp.now()
        )
        val nearbyBeekeeperIds = nearbyHives.map { it.ownerId }
            .filter { it != farmerId }
            .distinct()

        db.runBatch { batch ->
            batch.set(docRef, event)
            nearbyBeekeeperIds.forEach { recipientId ->
                val notificationRef = db.collection(NOTIFICATIONS).document()
                batch.set(
                    notificationRef,
                    NotificationAlert(
                        id = notificationRef.id,
                        recipientUserId = recipientId,
                        sprayEventId = event.id,
                        title = "Spray alert nearby",
                        body = "${draft.chemicalName} spraying is scheduled within ${event.radiusKm.toInt()} km of your hives.",
                        timestamp = Timestamp.now()
                    )
                )
            }
        }.await()

        return SpraySubmissionResult(
            eventId = event.id,
            nearbyHiveCount = nearbyHives.size,
            notifiedBeekeeperCount = nearbyBeekeeperIds.size,
            conflictingHiveTimings = emptyList(),
            nextSafeStartMillis = null,
            suggestedSafeTimes = emptyList(),
            blockedByActiveBees = false
        )
    }

    fun observeSprayEvents(): Flow<List<SprayEvent>> = callbackFlow {
        val subscription = db.collection(SPRAY_EVENTS)
            .orderBy("scheduledAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(SprayEvent::class.java).orEmpty())
            }
        awaitClose { subscription.remove() }
    }

    fun observeHealthLogs(userId: String?): Flow<List<HealthLog>> {
        if (userId == null) return flowOf(emptyList())
        return callbackFlow {
            val subscription = db.collection(HEALTH_LOGS)
                .whereEqualTo("ownerId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val logs = snapshot?.toObjects(HealthLog::class.java)
                        .orEmpty()
                        .sortedByDescending { it.createdAt.toDate().time }
                    trySend(logs)
                }
            awaitClose { subscription.remove() }
        }
    }

    suspend fun addHealthLog(log: HealthLog) {
        val uid = auth.currentUser?.uid ?: return
        val docRef = db.collection(HEALTH_LOGS).document()
        db.collection(HEALTH_LOGS).document(docRef.id).set(
            log.copy(
                id = docRef.id,
                ownerId = uid,
                createdAt = Timestamp.now()
            )
        ).await()
    }

    fun observeNotifications(userId: String?): Flow<List<NotificationAlert>> {
        if (userId == null) return flowOf(emptyList())
        return callbackFlow {
            val subscription = db.collection(NOTIFICATIONS)
                .whereEqualTo("recipientUserId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val notifications = snapshot?.toObjects(NotificationAlert::class.java)
                        .orEmpty()
                        .sortedByDescending { it.timestamp.toDate().time }
                    trySend(notifications)
                }
            awaitClose { subscription.remove() }
        }
    }

    suspend fun updateUserProfile(
        fullName: String,
        phoneNumber: String,
        preferredLanguage: String,
        themePreference: String
    ) {
        val uid = auth.currentUser?.uid ?: return
        db.collection(USERS).document(uid).update(
            mapOf(
                "fullName" to fullName,
                "phoneNumber" to phoneNumber,
                "preferredLanguage" to preferredLanguage,
                "themePreference" to themePreference
            )
        ).await()

        val ownedHives = db.collection(HIVES)
            .whereEqualTo("ownerId", uid)
            .get()
            .await()
            .documents

        if (ownedHives.isNotEmpty()) {
            db.runBatch { batch ->
                ownedHives.forEach { doc ->
                    batch.update(doc.reference, "contactNumber", phoneNumber)
                }
            }.await()
        }
    }

    suspend fun clearOldNotifications() {
        val uid = auth.currentUser?.uid ?: return
        val startOfToday = LocalDate.now(ZoneId.systemDefault())
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val oldDocs = db.collection(NOTIFICATIONS)
            .whereEqualTo("recipientUserId", uid)
            .get()
            .await()
            .documents
            .filter { it.getTimestamp("timestamp")?.toDate()?.time?.let { time -> time < startOfToday } == true }
        if (oldDocs.isNotEmpty()) {
            db.runBatch { batch -> oldDocs.forEach { batch.delete(it.reference) } }.await()
        }
    }

    suspend fun clearOldSprayHistory() {
        val uid = auth.currentUser?.uid ?: return
        val startOfToday = LocalDate.now(ZoneId.systemDefault())
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val oldDocs = db.collection(SPRAY_EVENTS)
            .whereEqualTo("farmerId", uid)
            .get()
            .await()
            .documents
            .filter { it.getTimestamp("createdAt")?.toDate()?.time?.let { time -> time < startOfToday } == true }
        if (oldDocs.isNotEmpty()) {
            db.runBatch { batch -> oldDocs.forEach { batch.delete(it.reference) } }.await()
        }
    }

    private fun mergeGoogleUser(
        firebaseUser: FirebaseUser,
        existing: User?,
        selectedRole: String,
        preferredLanguage: String,
        fullNameOverride: String,
        phoneNumberOverride: String
    ): User {
        val resolvedRole = existing?.role?.takeIf { it.isNotBlank() } ?: selectedRole
        val resolvedLanguage = existing?.preferredLanguage?.takeIf { it.isNotBlank() } ?: preferredLanguage
        val resolvedTheme = existing?.themePreference?.takeIf { it.isNotBlank() } ?: AppThemePreference.SYSTEM.name
        return User(
            uid = firebaseUser.uid,
            fullName = fullNameOverride.takeIf { it.isNotBlank() }
                ?: existing?.fullName?.takeIf { it.isNotBlank() }
                ?: firebaseUser.displayName.orEmpty(),
            email = existing?.email?.takeIf { it.isNotBlank() }
                ?: firebaseUser.email.orEmpty(),
            phoneNumber = phoneNumberOverride.takeIf { it.isNotBlank() }
                ?: existing?.phoneNumber.orEmpty(),
            role = resolvedRole,
            preferredLanguage = resolvedLanguage,
            themePreference = resolvedTheme,
            fcmToken = existing?.fcmToken.orEmpty(),
            createdAt = existing?.createdAt ?: Timestamp.now()
        )
    }
}
