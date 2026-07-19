package com.example.ui.components

import android.util.Log
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

interface KuestWebSocketListener {
    fun onStateChanged(status: String, message: String)
    fun onPayloadReceived(payloadJson: String, transactionId: String, status: String, amount: String?, district: String?)
}

class KuestWebSocketManager(
    val listener: KuestWebSocketListener
) {
    private var client: OkHttpClient? = null
    private var webSocket: WebSocket? = null
    private var isConnecting = false

    fun connect(url: String) {
        if (webSocket != null || isConnecting) return
        isConnecting = true
        listener.onStateChanged("CONNECTING", "Establishing secure signal to $url...")

        client = OkHttpClient.Builder()
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder().url(url).build()

        webSocket = client?.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnecting = false
                listener.onStateChanged("CONNECTED", "Active pipeline established. Listening for state events.")
                // Send an initial handshake ping
                webSocket.send("{\"type\": \"PING\"}")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("KuestWS", "Received text payload: $text")
                try {
                    val json = JSONObject(text)
                    if (json.optString("type") == "PONG") {
                        listener.onStateChanged("CONNECTED", "Heartbeat PONG received.")
                        return
                    }

                    // Extract properties dynamically based on different packet structures
                    val transactionId = json.optString("transaction_id", json.optString("id", ""))
                    val status = json.optString("status", "")
                    val amount = if (json.has("amount")) json.getString("amount") else null
                    val district = if (json.has("district")) json.getString("district") else null

                    if (transactionId.isNotEmpty() && status.isNotEmpty()) {
                        listener.onPayloadReceived(
                            payloadJson = text,
                            transactionId = transactionId,
                            status = status,
                            amount = amount,
                            district = district
                        )
                    } else {
                        listener.onStateChanged("CONNECTED", "Received system notification frame: $text")
                    }
                } catch (e: Exception) {
                    Log.e("KuestWS", "Error parsing WebSocket payload", e)
                    listener.onStateChanged("CONNECTED", "Non-structured string received: $text")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnecting = false
                this@KuestWebSocketManager.webSocket = null
                listener.onStateChanged("OFFLINE", "Signal disrupted: ${t.message}")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                listener.onStateChanged("CONNECTING", "Closing pipeline...")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnecting = false
                this@KuestWebSocketManager.webSocket = null
                listener.onStateChanged("OFFLINE", "Pipeline closed. (Code: $code)")
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "User requested disconnect")
        webSocket = null
        isConnecting = false
        listener.onStateChanged("OFFLINE", "Disconnected from command console.")
    }

    fun sendPing() {
        webSocket?.send("{\"type\": \"PING\"}")
    }
}
