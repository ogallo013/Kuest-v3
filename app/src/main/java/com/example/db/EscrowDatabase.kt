package com.example.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// 1. Define the valid states for our Escrow Transaction
enum class EscrowState {
    CREATED,      // Deal initiated by buyer/seller, awaiting payment
    FUNDED,       // Buyer paid, money safely held in platform bank/M-Pesa wallet
    DISPATCHED,   // Seller handed package to rider/courier
    COMPLETED,    // QR code scanned successfully, seller paid out
    DISPUTED,     // Buyer reported issue or rider failed delivery
    CANCELLED     // Cancelled before funding (or refunded via system)
}

// 2. Escrow Transactions Core Entity
@Entity(tableName = "escrow_deals")
data class EscrowDeal(
    @PrimaryKey val id: String, // UUID as String
    @ColumnInfo(name = "buyer_id") val buyerId: String,
    @ColumnInfo(name = "seller_id") val sellerId: String,
    @ColumnInfo(name = "title") val title: String, // Added to preserve item names in Compose UI
    @ColumnInfo(name = "amount") val amount: Double,
    @ColumnInfo(name = "currency") val currency: String, // KES, NGN, ZAR
    @ColumnInfo(name = "current_state") val currentState: EscrowState = EscrowState.CREATED,
    
    // Verification Security Tokens
    @ColumnInfo(name = "secure_handshake_hash") val secureHandshakeHash: String?,
    
    // Meta References
    @ColumnInfo(name = "market_listing_id") val marketListingId: String?,
    @ColumnInfo(name = "chat_room_id") val chatRoomId: String?,
    
    // Timestamps for auditing
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

// 3. Audit Log Entity for every single state transition
@Entity(
    tableName = "escrow_state_logs",
    foreignKeys = [
        ForeignKey(
            entity = EscrowDeal::class,
            parentColumns = ["id"],
            childColumns = ["deal_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["deal_id"])]
)
data class EscrowStateLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "deal_id") val dealId: String,
    @ColumnInfo(name = "from_state") val fromState: EscrowState?,
    @ColumnInfo(name = "to_state") val toState: EscrowState,
    @ColumnInfo(name = "triggered_by") val triggeredBy: String?, // User ID or 'SYSTEM_CRON'
    @ColumnInfo(name = "changed_at") val changedAt: Long = System.currentTimeMillis()
)

// Room Type Converters for Enum EscrowState
class EscrowConverters {
    @TypeConverter
    fun fromEscrowState(value: EscrowState?): String? = value?.name

    @TypeConverter
    fun toEscrowState(value: String?): EscrowState? = value?.let { enumValueOf<EscrowState>(it) }
}

// 4. Room DAO (Data Access Object)
@Dao
interface EscrowDao {
    @Query("SELECT * FROM escrow_deals ORDER BY created_at DESC")
    fun getAllDeals(): Flow<List<EscrowDeal>>

    @Query("SELECT * FROM escrow_deals WHERE id = :id")
    suspend fun getDealById(id: String): EscrowDeal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeal(deal: EscrowDeal)

    @Update
    suspend fun updateDeal(deal: EscrowDeal)

    @Query("SELECT * FROM escrow_state_logs WHERE deal_id = :dealId ORDER BY changed_at ASC")
    fun getLogsForDeal(dealId: String): Flow<List<EscrowStateLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStateLog(log: EscrowStateLog)

    @Transaction
    suspend fun transitionDealState(
        dealId: String,
        newState: EscrowState,
        triggeredBy: String
    ): Boolean {
        val deal = getDealById(dealId) ?: return false
        val oldState = deal.currentState
        if (oldState == newState) return false

        // Validate allowed state transitions:
        // • CREATED     ──► FUNDED or CANCELLED
        // • FUNDED      ──► DISPATCHED or DISPUTED
        // • DISPATCHED  ──► COMPLETED or DISPUTED
        // • DISPUTED    ──► COMPLETED or CANCELLED
        val isTransitionAllowed = when (oldState) {
            EscrowState.CREATED -> newState == EscrowState.FUNDED || newState == EscrowState.CANCELLED
            EscrowState.FUNDED -> newState == EscrowState.DISPATCHED || newState == EscrowState.DISPUTED
            EscrowState.DISPATCHED -> newState == EscrowState.COMPLETED || newState == EscrowState.DISPUTED
            EscrowState.DISPUTED -> newState == EscrowState.COMPLETED || newState == EscrowState.CANCELLED
            else -> false
        }
        if (!isTransitionAllowed) return false

        // Update the deal state
        val updatedDeal = deal.copy(
            currentState = newState,
            updatedAt = System.currentTimeMillis()
        )
        updateDeal(updatedDeal)

        // Add to audit logs
        val auditLog = EscrowStateLog(
            dealId = dealId,
            fromState = oldState,
            toState = newState,
            triggeredBy = triggeredBy,
            changedAt = System.currentTimeMillis()
        )
        insertStateLog(auditLog)
        return true
    }

    @Query("DELETE FROM escrow_deals")
    suspend fun clearAllDeals()

    @Query("DELETE FROM escrow_state_logs")
    suspend fun clearAllLogs()
}

// 5. Room Database
@Database(entities = [EscrowDeal::class, EscrowStateLog::class], version = 1, exportSchema = false)
@TypeConverters(EscrowConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun escrowDao(): EscrowDao
}
