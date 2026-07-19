package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

// 1. Precise Cyber/Athletic Graphic Color Palette
val CyberBlack = Color(0xFF0A0C10)
val CyberSurface = Color(0xFF141822)
val NeonGreen = Color(0xFF39FF14)
val NeonCyan = Color(0xFF00F0FF)

// 2. Custom Graphic Modifier: High-Contrast Radial/Drop Glow
fun Modifier.neonGlow(color: Color, radius: Dp = 8.dp) = this.drawBehind {
    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            style = PaintingStyle.Fill
            isAntiAlias = true
            this.color = color
        }
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.setShadowLayer(radius.toPx(), 0f, 0f, color.hashCode())
        canvas.drawRect(0f, 0f, size.width, size.height, paint)
    }
}

// 3. Custom Graphic Shape: Angular Clipped Corners (EA Sports FC Menu Style)
val AngularCyberShape = GenericShape { size, _ ->
    moveTo(0f, 0f)
    lineTo(size.width - 24f, 0f)
    lineTo(size.width, 24f)
    lineTo(size.width, size.height)
    lineTo(24f, size.height)
    lineTo(0f, size.height - 24f)
    close()
}

// Segmented Progress Loading Graphic built of separate light blocks simulating an energy bar metric
@Composable
fun SegmentedProgressBar(
    modifier: Modifier = Modifier,
    segments: Int = 10,
    filledSegments: Int = 5,
    activeColor: Color = NeonCyan,
    inactiveColor: Color = Color.Gray.copy(alpha = 0.2f)
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (i in 0 until segments) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .background(
                        if (i < filledSegments) activeColor else inactiveColor
                    )
            )
        }
    }
}

// 4. Component View: High-Fidelity Tactical State Card
@Composable
fun EscrowGraphicsCard(
    transactionId: String,
    state: String,
    formattedAmount: String,
    onLaunchTelemetryHud: (() -> Unit)? = null
) {
    // Determine progress based on the state string
    val filledSegments = when (state.uppercase()) {
        "CREATED", "PENDING" -> 3
        "FUNDED" -> 5
        "DISPATCHED", "SHIPPED" -> 8
        "COMPLETED", "SUCCESSFUL" -> 10
        else -> 4
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(CyberSurface, shape = AngularCyberShape)
            .border(1.5.dp, NeonCyan.copy(alpha = 0.6f), shape = AngularCyberShape)
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            
            // Header Info Block
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_escrow_hud_node),
                        contentDescription = "HUD Cyber Node Network connection status",
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "NODE ID: #$transactionId",
                        color = Color.LightGray.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Pulsing High-Visibility State Badge
                Box(
                    modifier = Modifier
                        .background(NeonGreen.copy(alpha = 0.15f))
                        .border(1.dp, NeonGreen)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = state.uppercase(),
                        color = NeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Financial Balance Data Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "TOTAL ESCROW VALUATION",
                        color = Color.Gray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = formattedAmount,
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.neonGlow(NeonCyan.copy(alpha = 0.15f))
                    )
                }
            }

            // Visual Mechanics: Segmented Energy Bar Metric
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ENERGY/ESCROW PIPELINE METRIC",
                        color = Color.Gray,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${filledSegments * 10}% SECURED",
                        color = NeonCyan,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black
                    )
                }
                SegmentedProgressBar(
                    segments = 10,
                    filledSegments = filledSegments,
                    activeColor = if (state.uppercase() == "COMPLETED" || state.uppercase() == "SUCCESSFUL") NeonGreen else NeonCyan
                )
            }

            if (onLaunchTelemetryHud != null) {
                Spacer(modifier = Modifier.height(4.dp))
                androidx.compose.material3.Button(
                    onClick = onLaunchTelemetryHud,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = NeonCyan.copy(alpha = 0.12f),
                        contentColor = NeonCyan
                    ),
                    shape = AngularCyberShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_escrow_dialogue_wireframe),
                            contentDescription = "HUD Hologram Launcher",
                            tint = NeonCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "LAUNCH TACTICAL TELEMETRY HUD 🛰️",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

