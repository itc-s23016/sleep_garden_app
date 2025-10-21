package com.example.sleep_garden.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ktx.firestoreSettings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

data class AlarmItem(
    val id: String = "",         // Firestore docId
    val hour: Int = 7,
    val minute: Int = 0,
    val enabled: Boolean = false
)

class FirestoreAlarmRepository {

    private val auth = Firebase.auth
    private val db = Firebase.firestore.apply {
        firestoreSettings { isPersistenceEnabled = true } // オフライン有効
    }

    /** 匿名ログインを保証して uid を返す */
    private suspend fun ensureSignIn(): String {
        auth.currentUser?.let { return it.uid }
        return Firebase.auth.signInAnonymously().await().user!!.uid
    }

    private suspend fun alarmsCol() =
        db.collection("users").document(ensureSignIn()).collection("alarms")

    /** 複数アラーム購読 */
    fun observeAlarms(): Flow<List<AlarmItem>> = callbackFlow {
        var reg: ListenerRegistration? = null
        try {
            val uid = ensureSignIn()
            reg = db.collection("users")
                .document(uid)
                .collection("alarms")
                .addSnapshotListener { snap, e ->
                    if (e != null) { /* 省略 */ return@addSnapshotListener }
                    val list = snap?.documents?.map { d ->
                        AlarmItem(
                            id = d.id,
                            hour = (d.get("hour") as? Number)?.toInt() ?: 7,
                            minute = (d.get("minute") as? Number)?.toInt() ?: 0,
                            enabled = (d.get("enabled") as? Boolean) ?: false
                        )
                    }.orEmpty()
                        .sortedWith(compareBy<AlarmItem> { it.hour }.thenBy { it.minute }) // 👈 ここで並び替え
                    trySend(list)
                }
        } catch (ex: Exception) {
            Log.e("AlarmRepo", "observeAlarms signIn/setup failed", ex)
            trySend(emptyList()) // フォールバック
        }
        awaitClose { reg?.remove() }
    }

    /** 追加（自動ID）。デフォルトで有効にして作成 */
    suspend fun addAlarm(hour: Int, minute: Int, enabled: Boolean = true): String {
        val col = alarmsCol()
        val doc = col.document()
        doc.set(
            mapOf(
                "hour" to hour,
                "minute" to minute,
                "enabled" to enabled,
                "updatedAt" to com.google.firebase.Timestamp(Date())
            )
        ).await()
        return doc.id
    }

    /** 時刻更新 */
    suspend fun updateTime(id: String, hour: Int, minute: Int) {
        alarmsCol().document(id).update(
            mapOf(
                "hour" to hour,
                "minute" to minute,
                "updatedAt" to com.google.firebase.Timestamp(Date())
            )
        ).await()
    }

    /** ON/OFF 切替 */
    suspend fun setEnabled(id: String, enabled: Boolean) {
        alarmsCol().document(id).update(
            mapOf(
                "enabled" to enabled,
                "updatedAt" to com.google.firebase.Timestamp(Date())
            )
        ).await()
    }

    /** 削除 */
    suspend fun delete(id: String) {
        alarmsCol().document(id).delete().await()
    }
}
