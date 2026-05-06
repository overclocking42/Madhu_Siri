package com.example.madhu_siri.data.repository

import com.example.madhu_siri.data.model.Hive
import com.example.madhu_siri.data.model.SprayEvent
import com.example.madhu_siri.data.model.User
import com.example.madhu_siri.utils.HaversineUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    suspend fun saveUserRole(role: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).set(User(uid, role)).await()
    }

    suspend fun getUserRole(): String? {
        val uid = auth.currentUser?.uid ?: return null
        val doc = db.collection("users").document(uid).get().await()
        return doc.getString("role")
    }

    fun getHives(): Flow<List<Hive>> = callbackFlow {
        val subscription = db.collection("hives")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val hives = snapshot.toObjects(Hive::class.java)
                    trySend(hives)
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun addHive(lat: Double, lng: Double) {
        val uid = auth.currentUser?.uid ?: return
        val docRef = db.collection("hives").document()
        val hive = Hive(id = docRef.id, lat = lat, lng = lng, ownerId = uid)
        docRef.set(hive).await()
    }

    suspend fun deleteHive(hiveId: String) {
        db.collection("hives").document(hiveId).delete().await()
    }

    suspend fun sendSprayAlert(lat: Double, lng: Double) {
        val docRef = db.collection("sprayEvents").document()
        val event = SprayEvent(id = docRef.id, lat = lat, lng = lng)
        docRef.set(event).await()

        // Geofencing logic: Find nearby hives and would typically trigger FCM via Cloud Functions
        // For this local implementation, we'll simulate the "finding" part
        val hives = db.collection("hives").get().await().toObjects(Hive::class.java)
        hives.forEach { hive ->
            val distance = HaversineUtil.calculateDistance(lat, lng, hive.lat, hive.lng)
            if (distance < 2.0) {
                // In a real app, you'd send a notification to hive.ownerId
                // via a Firebase Cloud Function or a backend service.
            }
        }
    }

    fun getSprayEvents(): Flow<List<SprayEvent>> = callbackFlow {
        val subscription = db.collection("sprayEvents")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    trySend(snapshot.toObjects(SprayEvent::class.java))
                }
            }
        awaitClose { subscription.remove() }
    }
}
