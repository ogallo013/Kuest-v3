package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.db.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private lateinit var database: AppDatabase
  private lateinit var dao: EscrowDao

  @Before
  fun setup() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    dao = database.escrowDao()
  }

  @After
  fun teardown() {
    database.close()
  }

  @Test
  fun testAppNameIsCorrect() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Kuest", appName)
  }

  @Test
  fun testCreateAndFetchEscrowDeal() = runBlocking {
    val deal = EscrowDeal(
      id = "deal_123",
      buyerId = "buyer_1",
      sellerId = "seller_1",
      title = "Vintage leather jacket",
      amount = 4500.0,
      currency = "KES",
      currentState = EscrowState.CREATED,
      secureHandshakeHash = "secure_hash_abc",
      marketListingId = "listing_456",
      chatRoomId = "chat_789"
    )

    dao.insertDeal(deal)

    val fetched = dao.getDealById("deal_123")
    assertNotNull(fetched)
    assertEquals("buyer_1", fetched?.buyerId)
    assertEquals("seller_1", fetched?.sellerId)
    assertEquals("Vintage leather jacket", fetched?.title)
    assertEquals(4500.0, fetched?.amount ?: 0.0, 0.0)
    assertEquals("KES", fetched?.currency)
    assertEquals(EscrowState.CREATED, fetched?.currentState)
  }

  @Test
  fun testAllowedTransitions() = runBlocking {
    val deal = EscrowDeal(
      id = "transition_deal",
      buyerId = "buyer_1",
      sellerId = "seller_1",
      title = "Vintage leather jacket",
      amount = 4500.0,
      currency = "KES",
      currentState = EscrowState.CREATED,
      secureHandshakeHash = "secure_hash_abc",
      marketListingId = "listing_456",
      chatRoomId = "chat_789"
    )
    dao.insertDeal(deal)

    // CREATED -> FUNDED (Allowed)
    var success = dao.transitionDealState("transition_deal", EscrowState.FUNDED, "buyer_1")
    assertTrue(success)
    assertEquals(EscrowState.FUNDED, dao.getDealById("transition_deal")?.currentState)

    // FUNDED -> DISPATCHED (Allowed)
    success = dao.transitionDealState("transition_deal", EscrowState.DISPATCHED, "seller_1")
    assertTrue(success)
    assertEquals(EscrowState.DISPATCHED, dao.getDealById("transition_deal")?.currentState)

    // DISPATCHED -> COMPLETED (Allowed)
    success = dao.transitionDealState("transition_deal", EscrowState.COMPLETED, "buyer_1")
    assertTrue(success)
    assertEquals(EscrowState.COMPLETED, dao.getDealById("transition_deal")?.currentState)
  }

  @Test
  fun testDisallowedTransitionBlocked() = runBlocking {
    val deal = EscrowDeal(
      id = "blocked_deal",
      buyerId = "buyer_1",
      sellerId = "seller_1",
      title = "Vintage leather jacket",
      amount = 4500.0,
      currency = "KES",
      currentState = EscrowState.CREATED,
      secureHandshakeHash = "secure_hash_abc",
      marketListingId = "listing_456",
      chatRoomId = "chat_789"
    )
    dao.insertDeal(deal)

    // CREATED -> COMPLETED (Disallowed)
    val success = dao.transitionDealState("blocked_deal", EscrowState.COMPLETED, "buyer_1")
    assertFalse(success)
    // State should remain CREATED
    assertEquals(EscrowState.CREATED, dao.getDealById("blocked_deal")?.currentState)
  }

  @Test
  fun testStateTransitionAuditLogs() = runBlocking {
    val deal = EscrowDeal(
      id = "audit_deal",
      buyerId = "buyer_1",
      sellerId = "seller_1",
      title = "Vintage leather jacket",
      amount = 4500.0,
      currency = "KES",
      currentState = EscrowState.CREATED,
      secureHandshakeHash = "secure_hash_abc",
      marketListingId = "listing_456",
      chatRoomId = "chat_789"
    )
    dao.insertDeal(deal)

    dao.transitionDealState("audit_deal", EscrowState.FUNDED, "buyer_1")
    dao.transitionDealState("audit_deal", EscrowState.DISPATCHED, "seller_1")

    val logs = dao.getLogsForDeal("audit_deal").first()
    assertEquals(2, logs.size)

    assertEquals(EscrowState.CREATED, logs[0].fromState)
    assertEquals(EscrowState.FUNDED, logs[0].toState)
    assertEquals("buyer_1", logs[0].triggeredBy)

    assertEquals(EscrowState.FUNDED, logs[1].fromState)
    assertEquals(EscrowState.DISPATCHED, logs[1].toState)
    assertEquals("seller_1", logs[1].triggeredBy)
  }
}
