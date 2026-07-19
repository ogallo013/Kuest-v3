package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import com.example.R

// 1. Structural States mapping clean database architecture values
enum class TransactionState {
    CREATED, FUNDED, DISPATCHED, COMPLETED, DISPUTED
}

// 2. State resolver converting API payload raw strings into strict local UI model maps
object UIStateMapper {
    fun resolve(status: String): TransactionState {
        return when (status.uppercase().trim()) {
            "FUNDED" -> TransactionState.FUNDED
            "DISPATCHED" -> TransactionState.DISPATCHED
            "COMPLETED", "SUCCESSFUL", "DELIVERED" -> TransactionState.COMPLETED
            "DISPUTED" -> TransactionState.DISPUTED
            "CREATED", "AWAITING", "FUNDS AWAITING", "FUNDS HELD" -> TransactionState.CREATED
            else -> TransactionState.CREATED
        }
    }
}

// 3. Data blueprint tracking isolated backdrop matrix particles
data class TelemetryParticle(
    val initialX: Float,
    val initialY: Float,
    val speed: Float,
    val size: Float,
    val maxAlpha: Float
)

// 4. Dynamic Ambient Canvas Layer: Renders background details based on state with speed acceleration
@Composable
fun MatrixParticleOverlay(
    state: TransactionState, 
    speedMultiplier: Float,
    modifier: Modifier = Modifier
) {
    val baseParticleColor = when (state) {
        TransactionState.DISPUTED -> Color(0xFFFF3333)
        TransactionState.COMPLETED -> Color(0xFF39FF14)
        TransactionState.FUNDED -> Color(0xFF39FF14)
        else -> Color(0xFF00F0FF)
    }

    val particleColor by animateColorAsState(
        targetValue = baseParticleColor,
        animationSpec = tween(1000),
        label = "particle_color_transition"
    )

    // Infinite loop driving the background canvas frame updates
    val infiniteTransition = rememberInfiniteTransition(label = "hud_matrix_loop")
    val translationFactor by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle_drift"
    )

    // Generate static particle starting positions to avoid redraw performance lag
    val particles = remember {
        List(30) {
            TelemetryParticle(
                initialX = Random.nextFloat(),
                initialY = Random.nextFloat(),
                speed = Random.nextFloat() * 0.4f + 0.15f,
                size = Random.nextFloat() * 5f + 2f,
                maxAlpha = Random.nextFloat() * 0.5f + 0.15f
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { particle ->
            // Shift coordinates upward over time, factored by real-time speedMultiplier acceleration
            val currentY = (size.height * particle.initialY - (translationFactor * particle.speed * speedMultiplier)) % size.height
            val resolvedY = if (currentY < 0) currentY + size.height else currentY
            val resolvedX = size.width * particle.initialX

            drawCircle(
                color = particleColor.copy(alpha = particle.maxAlpha),
                radius = particle.size,
                center = Offset(resolvedX, resolvedY),
                blendMode = BlendMode.Screen
            )
        }
    }
}

// 5. Console log record data class
data class ConsoleLog(
    val timestamp: String,
    val message: String,
    val level: LogLevel = LogLevel.INFO
)

enum class LogLevel {
    INFO, SUCCESS, WARNING, PACKET
}

// 6. Core User Interface Shell View: Merges particles and live telemetry HUD controls seamlessly
@Composable
fun GamifiedTransactionScreen(
    transactionId: String,
    currentState: TransactionState,
    displayAmount: String,
    onStateTransitionRequested: (String, TransactionState) -> Unit = { _, _ -> }
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // WebSockets Server Connection Configuration
    var wsUrl by remember { mutableStateOf("ws://10.0.2.2:8000/ws/v1/realtime/kuest_explorer") }
    var connectionState by remember { mutableStateOf("OFFLINE") } // OFFLINE, CONNECTING, CONNECTED
    var consoleLogs by remember {
        mutableStateOf(
            listOf(
                ConsoleLog("12:36:45", "KUEST Command console online. Signal interceptor initialized.", LogLevel.INFO)
            )
        )
    }

    // Dynamic Matrix Speed multiplier driving particle acceleration
    var matrixSpeedMultiplier by remember { mutableStateOf(1.0f) }

    // Smoothly animate speed multiplier back to 1.0f base rate when accelerated
    LaunchedEffect(matrixSpeedMultiplier) {
        if (matrixSpeedMultiplier > 1.0f) {
            delay(100)
            animate(
                initialValue = matrixSpeedMultiplier,
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 2800, easing = FastOutLinearInEasing)
            ) { value, _ ->
                matrixSpeedMultiplier = value
            }
        }
    }

    var isFlowMode by remember { mutableStateOf(false) }

    // WebSocket Connection Manager reference
    val webSocketManager = remember {
        KuestWebSocketManager(object : KuestWebSocketListener {
            override fun onStateChanged(status: String, message: String) {
                if (!isFlowMode) {
                    connectionState = status
                    val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                    val level = when (status) {
                        "CONNECTED" -> LogLevel.SUCCESS
                        "CONNECTING" -> LogLevel.INFO
                        else -> LogLevel.WARNING
                    }
                    consoleLogs = consoleLogs + ConsoleLog(time, "[WS Callback] $message", level)
                }
            }

            override fun onPayloadReceived(
                payloadJson: String,
                transactionId: String,
                status: String,
                amount: String?,
                district: String?
            ) {
                if (!isFlowMode) {
                    val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                    consoleLogs = consoleLogs + ConsoleLog(time, "[Callback Packet] Secure payload: $payloadJson", LogLevel.PACKET)

                    // Process payload status via our strict UIStateMapper
                    val resolvedState = UIStateMapper.resolve(status)
                    consoleLogs = consoleLogs + ConsoleLog(time, "[Mapper] Resolved status state: $resolvedState", LogLevel.SUCCESS)

                    // Accelerate Matrix Particles instantly to simulate high speed data packet processing
                    matrixSpeedMultiplier = 12.0f

                    // Request state transition back up to the App controller layer & Room DB
                    onStateTransitionRequested(transactionId, resolvedState)
                }
            }
        })
    }

    // RealtimeUiEngine instance
    val realtimeUiEngine = remember(wsUrl) {
        RealtimeUiEngine(
            userId = "kuest_explorer",
            wsUrl = wsUrl,
            listener = object : RealtimeEngineListener {
                override fun onStateChanged(status: String, message: String) {
                    if (isFlowMode) {
                        connectionState = status
                        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                        val level = when (status) {
                            "CONNECTED" -> LogLevel.SUCCESS
                            "CONNECTING" -> LogLevel.INFO
                            else -> LogLevel.WARNING
                        }
                        consoleLogs = consoleLogs + ConsoleLog(time, "[FlowEngine] $message", level)
                    }
                }
            }
        )
    }

    // Collect flow-based events when active
    LaunchedEffect(isFlowMode, realtimeUiEngine) {
        if (isFlowMode) {
            realtimeUiEngine.uiStateUpdates.collect { text ->
                val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                consoleLogs = consoleLogs + ConsoleLog(time, "[FlowStream] Received JSON flow packet: $text", LogLevel.PACKET)
                try {
                    val json = org.json.JSONObject(text)
                    val transactionIdFromPacket = json.optString("transaction_id", json.optString("id", ""))
                    val statusFromPacket = json.optString("status", "")
                    val resolvedState = UIStateMapper.resolve(statusFromPacket)
                    consoleLogs = consoleLogs + ConsoleLog(time, "[Mapper] Resolved status: $resolvedState", LogLevel.SUCCESS)
                    matrixSpeedMultiplier = 12.0f
                    if (transactionIdFromPacket.isNotEmpty()) {
                        onStateTransitionRequested(transactionIdFromPacket, resolvedState)
                    }
                } catch (e: Exception) {
                    consoleLogs = consoleLogs + ConsoleLog(time, "[FlowError] Non-decodable frame: $text", LogLevel.WARNING)
                }
            }
        }
    }

    // Ensure we disconnect when this screen leaves Compose tree
    DisposableEffect(Unit) {
        onDispose {
            webSocketManager.disconnect()
            realtimeUiEngine.shutdown()
        }
    }

    val themeAccentColor = when (currentState) {
        TransactionState.FUNDED -> Color(0xFF39FF14)    // Electric Green
        TransactionState.DISPUTED -> Color(0xFFFF3333)   // Warning Red
        TransactionState.COMPLETED -> Color(0xFF39FF14)  // Success Green
        else -> Color(0xFF00F0FF)                        // Tactical Cyan
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080A0F)) // Base deep dark color palette matte layer
    ) {
        // Step A: Draw the live background matrix particle layer
        MatrixParticleOverlay(
            state = currentState, 
            speedMultiplier = matrixSpeedMultiplier, 
            modifier = Modifier.fillMaxSize()
        )

        // Step B: Build out operational graphic panels layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .statusBarsPadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            
            // Layout Element 1: Top Status Header Panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF10141F).copy(alpha = 0.85f), shape = AngularCyberShape)
                    .border(1.dp, themeAccentColor.copy(alpha = 0.35f), shape = AngularCyberShape)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TACTICAL TELEMETRY FEED",
                        color = Color.LightGray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "SIGNAL REF: #$transactionId",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Status Container: Keeps language professional while utilizing gaming colors
                Box(
                    modifier = Modifier
                        .background(themeAccentColor.copy(alpha = 0.12f))
                        .border(1.dp, themeAccentColor)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = currentState.name, // Renders professional system statuses (e.g., "FUNDED")
                        color = themeAccentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Layout Element 2: Center Transaction Metric Display Component
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0C0F17).copy(alpha = 0.7f), shape = RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF1E2538).copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SECURED ESCROW CONTRACT VOLUME",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = displayAmount,
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // Segmented Verification tracker bar graphics
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("VERIFICATION STAGE", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        Text(
                            text = "SPEED: ${String.format("%.1f", matrixSpeedMultiplier)}x", 
                            color = if (matrixSpeedMultiplier > 1f) NeonGreen else Color.Gray, 
                            fontSize = 8.sp, 
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val activeSegmentsCount = when (currentState) {
                            TransactionState.CREATED -> 1
                            TransactionState.FUNDED -> 2
                            TransactionState.DISPATCHED -> 3
                            TransactionState.COMPLETED -> 4
                            TransactionState.DISPUTED -> 2
                        }
                        
                        for (i in 1..4) {
                            val segmentColor = if (i <= activeSegmentsCount) themeAccentColor else Color(0xFF1E2538)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .background(segmentColor)
                            )
                        }
                    }
                }
            }

            // Layout Element 3: Holographic WebSockets control desk
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF0B0E17).copy(alpha = 0.9f), shape = RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF1E2538), shape = RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    when (connectionState) {
                                        "CONNECTED" -> NeonGreen
                                        "CONNECTING" -> Color(0xFFFFD300)
                                        else -> Color.Gray
                                    },
                                    shape = RoundedCornerShape(50)
                                )
                        )
                        Text(
                            text = "HOLOGRAPHIC WS PANEL ($connectionState)",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    
                    Text(
                        text = "PING",
                        color = if (connectionState == "CONNECTED") NeonCyan else Color.DarkGray,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .border(1.dp, if (connectionState == "CONNECTED") NeonCyan else Color.DarkGray, RoundedCornerShape(3.dp))
                            .clickable(enabled = connectionState == "CONNECTED") {
                                if (isFlowMode) {
                                    realtimeUiEngine.sendPing()
                                } else {
                                    webSocketManager.sendPing()
                                }
                                val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                                consoleLogs = consoleLogs + ConsoleLog(time, "[WS] Dispatched PING heartbeat packet.", LogLevel.INFO)
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Engine Selector Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (isFlowMode) {
                                realtimeUiEngine.shutdown()
                                webSocketManager.disconnect()
                                isFlowMode = false
                                connectionState = "OFFLINE"
                                val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                                consoleLogs = consoleLogs + ConsoleLog(time, "Switched network pipeline to [Callback-Listener Mode]", LogLevel.INFO)
                            }
                        },
                        modifier = Modifier.weight(1f).height(30.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isFlowMode) NeonCyan.copy(alpha = 0.2f) else Color(0xFF141A29),
                            contentColor = if (!isFlowMode) NeonCyan else Color.Gray
                        ),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (!isFlowMode) NeonCyan else Color(0xFF232D42)),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("CALLBACK LISTENER", fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (!isFlowMode) {
                                webSocketManager.disconnect()
                                realtimeUiEngine.shutdown()
                                isFlowMode = true
                                connectionState = "OFFLINE"
                                val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                                consoleLogs = consoleLogs + ConsoleLog(time, "Switched network pipeline to [Reactive FlowEngine Mode]", LogLevel.INFO)
                            }
                        },
                        modifier = Modifier.weight(1f).height(30.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFlowMode) NeonCyan.copy(alpha = 0.2f) else Color(0xFF141A29),
                            contentColor = if (isFlowMode) NeonCyan else Color.Gray
                        ),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isFlowMode) NeonCyan else Color(0xFF232D42)),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("REACTIVE FLOWENGINE", fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }

                // WebSocket Connection Address configuration inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = wsUrl,
                        onValueChange = { wsUrl = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color(0xFF232D42),
                            focusedContainerColor = Color(0xFF07090E),
                            unfocusedContainerColor = Color(0xFF07090E)
                        )
                    )

                    Button(
                        onClick = {
                            if (connectionState == "OFFLINE") {
                                if (isFlowMode) {
                                    realtimeUiEngine.establishConnection()
                                } else {
                                    webSocketManager.connect(wsUrl)
                                }
                            } else {
                                if (isFlowMode) {
                                    realtimeUiEngine.shutdown()
                                } else {
                                    webSocketManager.disconnect()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (connectionState == "OFFLINE") NeonCyan.copy(alpha = 0.15f) else Color(0xFFFF3333).copy(alpha = 0.15f),
                            contentColor = if (connectionState == "OFFLINE") NeonCyan else Color(0xFFFF3333)
                        ),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (connectionState == "OFFLINE") NeonCyan else Color(0xFFFF3333)),
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            text = if (connectionState == "OFFLINE") "CONNECT" else "KILL",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Live log terminal feed
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF05070B), shape = RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFF1E2538), shape = RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    val listState = rememberLazyListState()
                    LaunchedEffect(consoleLogs.size) {
                        if (consoleLogs.isNotEmpty()) {
                            listState.animateScrollToItem(consoleLogs.size - 1)
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(consoleLogs) { log ->
                            val color = when (log.level) {
                                LogLevel.SUCCESS -> NeonGreen
                                LogLevel.WARNING -> Color(0xFFFF3333)
                                LogLevel.PACKET -> Color(0xFFE040FB)
                                LogLevel.INFO -> NeonCyan
                            }
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "[${log.timestamp}] ",
                                    color = Color.Gray,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = log.message,
                                    color = color,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                // Layout Element 4: Manual Signal Broadcast Injector (Mock server payloads as requested)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "MANUAL SIGNAL BROADCAST INJECTOR (MOCK FASTAPI PACKETS)",
                        color = Color.Gray,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Option A: FUNDED standard payload matching the requested frame
                        Button(
                            onClick = {
                                val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                                val payload = "{\"transaction_id\": \"$transactionId\", \"status\": \"FUNDED\", \"amount\": \"$displayAmount\"}"
                                consoleLogs = consoleLogs + ConsoleLog(time, "[Simulator] Injecting payload frame...", LogLevel.INFO)
                                
                                coroutineScope.launch {
                                    delay(400)
                                    if (isFlowMode) {
                                        realtimeUiEngine.emitTestPayload(payload)
                                    } else {
                                        webSocketManager.listener.onPayloadReceived(
                                            payloadJson = payload,
                                            transactionId = transactionId,
                                            status = "FUNDED",
                                            amount = displayAmount,
                                            district = null
                                        )
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF141A29),
                                contentColor = NeonGreen
                            ),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("⚡ FUNDED (₦)", fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }

                        // Option B: DISPATCHED payload matching the enterprise DB district frame
                        Button(
                            onClick = {
                                val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                                val payload = "{\"id\": \"$transactionId\", \"status\": \"DISPATCHED\", \"district\": \"Kilimani\"}"
                                consoleLogs = consoleLogs + ConsoleLog(time, "[Simulator] Injecting payload frame...", LogLevel.INFO)
                                
                                coroutineScope.launch {
                                    delay(400)
                                    if (isFlowMode) {
                                        realtimeUiEngine.emitTestPayload(payload)
                                    } else {
                                        webSocketManager.listener.onPayloadReceived(
                                            payloadJson = payload,
                                            transactionId = transactionId,
                                            status = "DISPATCHED",
                                            amount = null,
                                            district = "Kilimani"
                                        )
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF141A29),
                                contentColor = NeonCyan
                            ),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("🚚 DISPATCHED", fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }

                        // Option C: COMPLETED successful payload transition
                        Button(
                            onClick = {
                                val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                                val payload = "{\"id\": \"$transactionId\", \"status\": \"COMPLETED\", \"district\": \"Kilimani\"}"
                                consoleLogs = consoleLogs + ConsoleLog(time, "[Simulator] Injecting payload frame...", LogLevel.INFO)
                                
                                coroutineScope.launch {
                                    delay(400)
                                    if (isFlowMode) {
                                        realtimeUiEngine.emitTestPayload(payload)
                                    } else {
                                        webSocketManager.listener.onPayloadReceived(
                                            payloadJson = payload,
                                            transactionId = transactionId,
                                            status = "COMPLETED",
                                            amount = null,
                                            district = "Kilimani"
                                        )
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF141A29),
                                contentColor = Color(0xFFE040FB)
                            ),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE040FB).copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("🏆 COMPLETE", fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
