package com.example.ui.components

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

interface RealtimeEngineListener {
    fun onStateChanged(status: String, message: String)
}

class RealtimeUiEngine(
    private val userId: String,
    private val wsUrl: String,
    private val listener: RealtimeEngineListener
) : WebSocketListener() {

    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var client: OkHttpClient? = null
    
    // UI components collect from this stream to repaint instantly on incoming messages
    private val _uiStateUpdates = MutableSharedFlow<String>(replay = 0)
    val uiStateUpdates: SharedFlow<String> = _uiStateUpdates

    private var isConnected = false
    private var reconnectDelayMs = 1000L
    private val maxReconnectDelayMs = 30000L

    fun establishConnection() {
        Log.d("RealtimeUiEngine", "Establishing connection to: $wsUrl for user: $userId")
        listener.onStateChanged("CONNECTING", "Establishing connection to $wsUrl...")
        
        client = OkHttpClient.Builder()
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url(wsUrl)
            .build()
        
        webSocket = client?.newWebSocket(request, this)
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        isConnected = true
        reconnectDelayMs = 1000L // Reset backoff timer on successful connection
        listener.onStateChanged("CONNECTED", "Channel active for user $userId. Monitoring Flow stream.")
        startHeartbeatLoop()
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        scope.launch {
            try {
                Log.d("RealtimeUiEngine", "Received websocket text: $text")
                val json = JSONObject(text)
                if (json.optString("type") == "PONG") {
                    listener.onStateChanged("CONNECTED", "Heartbeat PONG received on flow stream.")
                    return@launch
                }
                
                // Emits raw status states (e.g., "FUNDED") down to Jetpack Compose components
                _uiStateUpdates.emit(text)
            } catch (e: Exception) {
                Log.e("RealtimeUiEngine", "Failed to decode frame payload", e)
                // Fail silently or log schema inconsistencies
            }
        }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        isConnected = false
        listener.onStateChanged("CONNECTING", "Channel closing. Reason: $reason")
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        isConnected = false
        listener.onStateChanged("OFFLINE", "Channel closed. Code: $code")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        isConnected = false
        listener.onStateChanged("OFFLINE", "Disruption: ${t.message}. Reconnecting in ${reconnectDelayMs}ms...")
        triggerExponentialReconnect()
    }

    private fun startHeartbeatLoop() {
        scope.launch {
            while (isConnected) {
                delay(15000) // Pulse a PING every 15 seconds to keep the socket alive
                try {
                    Log.d("RealtimeUiEngine", "Pulse heartbeat PING")
                    webSocket?.send("{\"type\":\"PING\"}")
                } catch (e: Exception) {
                    Log.e("RealtimeUiEngine", "Failed to send heartbeat", e)
                    cancel() // Break out if socket is broken
                }
            }
        }
    }

    private fun triggerExponentialReconnect() {
        scope.launch {
            delay(reconnectDelayMs)
            // Double the delay time for the next attempt, capped at 30 seconds
            reconnectDelayMs = (reconnectDelayMs * 2).coerceAtMost(maxReconnectDelayMs)
            establishConnection()
        }
    }

    fun shutdown() {
        scope.cancel()
        webSocket?.close(1000, "App terminated session")
        webSocket = null
        isConnected = false
        listener.onStateChanged("OFFLINE", "Engine shutdown successfully.")
    }

    suspend fun emitTestPayload(payload: String) {
        _uiStateUpdates.emit(payload)
    }

    fun sendPing() {
        webSocket?.send("{\"type\":\"PING\"}")
    }
}
