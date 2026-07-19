package com.example.db

import kotlinx.coroutines.flow.Flow

class EscrowRepository(private val escrowDao: EscrowDao) {

    val allDeals: Flow<List<EscrowDeal>> = escrowDao.getAllDeals()

    fun getLogsForDeal(dealId: String): Flow<List<EscrowStateLog>> {
        return escrowDao.getLogsForDeal(dealId)
    }

    suspend fun getDealById(id: String): EscrowDeal? {
        return escrowDao.getDealById(id)
    }

    suspend fun createDeal(
        id: String,
        buyerId: String,
        sellerId: String,
        title: String,
        amount: Double,
        currency: String,
        secureHandshakeHash: String?,
        marketListingId: String?,
        chatRoomId: String?
    ) {
        val deal = EscrowDeal(
            id = id,
            buyerId = buyerId,
            sellerId = sellerId,
            title = title,
            amount = amount,
            currency = currency,
            currentState = EscrowState.CREATED,
            secureHandshakeHash = secureHandshakeHash,
            marketListingId = marketListingId,
            chatRoomId = chatRoomId
        )
        escrowDao.insertDeal(deal)

        // Insert initial creation log
        val log = EscrowStateLog(
            dealId = id,
            fromState = null,
            toState = EscrowState.CREATED,
            triggeredBy = buyerId,
            changedAt = System.currentTimeMillis()
        )
        escrowDao.insertStateLog(log)
    }

    suspend fun transitionState(
        dealId: String,
        newState: EscrowState,
        triggeredBy: String
    ): Boolean {
        return escrowDao.transitionDealState(dealId, newState, triggeredBy)
    }

    suspend fun clearAllData() {
        escrowDao.clearAllLogs()
        escrowDao.clearAllDeals()
    }
}
