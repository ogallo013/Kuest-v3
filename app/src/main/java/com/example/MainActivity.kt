package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.components.*
import kotlin.math.*
import kotlin.random.Random

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.room.Room
import com.example.db.AppDatabase
import com.example.db.EscrowRepository

// Global reactive theme state
var isLightModeGlobal by mutableStateOf(true)

@Composable
fun getAppBg(): Color = if (isLightModeGlobal) Color(0xFFF6F8FC) else Color(0xFF0A0A0C)

@Composable
fun getSurfaceBg(): Color = if (isLightModeGlobal) Color(0xFFFFFFFF) else Color(0xFF121216)

@Composable
fun getCardBg(): Color = if (isLightModeGlobal) Color(0xFFFFFFFF) else Color(0xFF1F1433)

@Composable
fun getCardPurple(): Color = if (isLightModeGlobal) Color(0xFFF1E4FF) else Color(0xFF1F1433)

@Composable
fun getCardSecondaryBg(): Color = if (isLightModeGlobal) Color(0xFFF1F5F9) else Color(0xFF150C24)

@Composable
fun getTextPrimary(): Color = if (isLightModeGlobal) Color(0xFF0F172A) else Color.White

@Composable
fun getTextSecondary(): Color = if (isLightModeGlobal) Color(0xFF475569) else Color.Gray

@Composable
fun getTextTertiary(): Color = if (isLightModeGlobal) Color(0xFF64748B) else Color.LightGray

@Composable
fun getBorderColor(): Color = if (isLightModeGlobal) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.08f)

class MainActivity : ComponentActivity() {
  private lateinit var database: AppDatabase
  private lateinit var repository: EscrowRepository

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Initialize Room Database
    database = Room.databaseBuilder(
      applicationContext,
      AppDatabase::class.java,
      "kuest_escrow_deals.db"
    ).fallbackToDestructiveMigration().build()

    repository = EscrowRepository(database.escrowDao())

    // Seed database if empty in a background coroutine
    lifecycleScope.launch {
      val existing = database.escrowDao().getDealById("1")
      if (existing == null) {
        // Deal 1: Funded
        repository.createDeal(
          id = "1",
          buyerId = "@kuest_explorer",
          sellerId = "@sam_listings",
          title = "MacBook Pro M1 (Refurbished)",
          amount = 85000.0,
          currency = "KES",
          secureHandshakeHash = "hs_85000_macbook",
          marketListingId = "1",
          chatRoomId = "chat_1"
        )
        repository.transitionState("1", com.example.db.EscrowState.FUNDED, "@kuest_explorer")

        // Deal 2: Dispatched (In Transit)
        repository.createDeal(
          id = "2",
          buyerId = "@kuest_explorer",
          sellerId = "@craft_bantu",
          title = "Custom Leather Boots",
          amount = 7500.0,
          currency = "KES",
          secureHandshakeHash = "hs_7500_boots",
          marketListingId = "4",
          chatRoomId = "chat_2"
        )
        repository.transitionState("2", com.example.db.EscrowState.FUNDED, "@kuest_explorer")
        repository.transitionState("2", com.example.db.EscrowState.DISPATCHED, "@craft_bantu")

        // Deal 3: Completed
        repository.createDeal(
          id = "3",
          buyerId = "@kuest_explorer",
          sellerId = "@eldoret_farm",
          title = "Organic Wild Honey (5L)",
          amount = 3200.0,
          currency = "KES",
          secureHandshakeHash = "hs_3200_honey",
          marketListingId = "2",
          chatRoomId = "chat_3"
        )
        repository.transitionState("3", com.example.db.EscrowState.FUNDED, "@kuest_explorer")
        repository.transitionState("3", com.example.db.EscrowState.DISPATCHED, "@eldoret_farm")
        repository.transitionState("3", com.example.db.EscrowState.COMPLETED, "@kuest_explorer")
      }
    }

    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        var hasStartedAdventure by remember { mutableStateOf(false) }
        if (!hasStartedAdventure) {
          KuestLandingScreen(onStartAdventure = { hasStartedAdventure = true })
        } else {
          KuestDashboardScreen(repository = repository, onBack = { hasStartedAdventure = false })
        }
      }
    }
  }
}

// Data class for particle system
data class EmojiParticle(
  val id: Long,
  val emoji: String,
  val targetX: Float,
  val targetY: Float,
  val scale: Float,
  val rotation: Float
)

@Composable
fun KuestLandingScreen(onStartAdventure: () -> Unit = {}) {
  val context = LocalContext.current
  
  // List of micro-adventures/quests
  val questPool = listOf(
    "🎯 Find a piece of street art or a mural nearby and capture its hidden detail.",
    "🌳 Discover the nearest green space, find its oldest tree, and stay there for 3 minutes.",
    "☕ Visit an independent café you've never tried and order their absolute specialty drink.",
    "🚶 Walk exactly 500 steps due North, and note down the first unusual thing you see.",
    "✨ Purchase a tiny treat, gift or snack and give it to a friend or surprise a stranger.",
    "🏛️ Explore a local landmark or historic site and search for a detail from before the 2000s.",
    "📙 Visit a local bookstore or library and open a random book to page 42 to find a life quote."
  )

  // Exploration States
  var isLoading by remember { mutableStateOf(false) }
  val coroutineScope = rememberCoroutineScope()

  // Autoload and enter the app after 6 seconds
  LaunchedEffect(Unit) {
    kotlinx.coroutines.delay(6000)
    if (!isLoading) {
      isLoading = true
      kotlinx.coroutines.delay(1400) // Simulated radar sync delay
      onStartAdventure()
      isLoading = false
    }
  }
  var currentQuestIndex by remember { mutableStateOf(0) }
  val activeQuest = questPool[currentQuestIndex]
  var isQuestCompleted by remember { mutableStateOf(false) }
  var isDrawerOpen by remember { mutableStateOf(false) }
  var streakCount by remember { mutableStateOf(3) } // Starting with a solid 3-day streak
  var totalXp by remember { mutableStateOf(1450) }

  // Particle states for dopamine celebration
  var particles by remember { mutableStateOf<List<EmojiParticle>>(emptyList()) }
  var particleProgress by remember { mutableStateOf(0f) }

  // Set up animation progress for particles
  LaunchedEffect(particles) {
    if (particles.isNotEmpty()) {
      particleProgress = 0f
      animate(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
      ) { value, _ ->
        particleProgress = value
      }
      particles = emptyList() // Reset particles after run
    }
  }

  // Infinite animations for idle states
  val infiniteTransition = rememberInfiniteTransition(label = "Idle Animations")
  
  // Animated lava flow time sequence representing the helical crack transition
  val lavaTime by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = (2 * Math.PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(6000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "LavaTimeAnim"
  )
  
  // Subtle button pulse
  val idleScale by infiniteTransition.animateFloat(
    initialValue = 0.98f,
    targetValue = 1.02f,
    animationSpec = infiniteRepeatable(
      animation = tween(2200, easing = EaseInOutSine),
      repeatMode = RepeatMode.Reverse
    ),
    label = "Pulse"
  )

  // Gentle compass needle rotation
  val idleRotation by infiniteTransition.animateFloat(
    initialValue = -8f,
    targetValue = 8f,
    animationSpec = infiniteRepeatable(
      animation = tween(3000, easing = EaseInOutQuad),
      repeatMode = RepeatMode.Reverse
    ),
    label = "CompassWobble"
  )

  // Unconditional loader pulsing animation state
  val loaderPulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.4f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(800, easing = EaseInOutSine),
      repeatMode = RepeatMode.Reverse
    ),
    label = "LoaderPulse"
  )

  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val isHovered by interactionSource.collectIsHoveredAsState()

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF111111))
  ) {
    // 1. Two-Half Background Layout
    Column(modifier = Modifier.fillMaxSize()) {
      // Upper Half (Vintage Compass Map Image & Kuest Title)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      ) {
        Image(
          painter = painterResource(id = R.drawable.kuest_compass_header_1784046007015),
          contentDescription = "Vintage Compass Map",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
        
        // Moody dark overlay to blend with the yellow half and highlight the title
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                colors = listOf(
                  Color.Black.copy(alpha = 0.6f),
                  Color.Black.copy(alpha = 0.2f),
                  Color(0xFFFFB300).copy(alpha = 0.25f) // soft gold edge blend
                )
              )
            )
        )
        
        // Brand Title Card - Sleek, Premium, High-Contrast
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 40.dp, start = 24.dp, end = 24.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Card(
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.55f)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
            modifier = Modifier
              .shadow(16.dp, shape = RoundedCornerShape(24.dp))
          ) {
            Column(
              modifier = Modifier.padding(horizontal = 28.dp, vertical = 18.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = "K U E S T",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 8.sp,
                fontFamily = FontFamily.SansSerif
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "YOUR CITY, UNLOCKED",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD54F), // premium yellow accent
                letterSpacing = 2.sp,
                fontFamily = FontFamily.SansSerif
              )
            }
          }
        }
      }
      
      // Lower Half (Interactive Yellow 3D Honeycomb Tiles)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      ) {
        // High-fidelity yellow honeycomb tile canvas
        YellowHoneycombBackground(
          lavaTime = lavaTime,
          isHovered = isHovered,
          modifier = Modifier.fillMaxSize()
        )
        
        // Ambient dark bottom vignette
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                colors = listOf(
                  Color.Transparent,
                  Color.Black.copy(alpha = 0.4f)
                )
              )
            )
        )
        
        // Inscription Badge at the bottom
        Box(
          modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(bottom = 36.dp),
          contentAlignment = Alignment.BottomCenter
        ) {
          Card(
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.5.dp, Color(0xFFFFD54F)),
            modifier = Modifier
              .shadow(12.dp, shape = RoundedCornerShape(50))
              .clickable { isDrawerOpen = true }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .background(Color(0xFFFFD54F), CircleShape)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "LIVE IN YOUR CITY 🧭",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.5.sp,
                fontFamily = FontFamily.SansSerif
              )
            }
          }
        }
      }
    }
    
    // 2. Centered Floating Action Button overlapping the split screen seam
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      // Dynamic button scale based on press, hover, or loading state
      val targetScale = if (isLoading) {
        0.96f
      } else if (isPressed) {
        0.93f
      } else if (isHovered) {
        1.08f // premium subtle hover expansion
      } else {
        idleScale
      }
      val buttonScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
          dampingRatio = Spring.DampingRatioMediumBouncy,
          stiffness = Spring.StiffnessLow
        ),
        label = "ScaleFactor"
      )

      // Subtle lift elevation mimicking smooth transitions
      val targetElevation = if (isLoading) {
        4.dp
      } else if (isPressed) {
        8.dp
      } else if (isHovered) {
        32.dp // premium hover lift
      } else {
        16.dp
      }
      val buttonElevation by animateDpAsState(
        targetValue = targetElevation,
        animationSpec = tween(350, easing = EaseOutQuad),
        label = "Elevation"
      )

      // Smooth dynamic shadow colors for golden drop-shadow glow
      val targetAmbientColor = if (isLoading) {
        Color(0x10000000)
      } else if (isPressed) {
        Color(0x35FFB300)
      } else if (isHovered) {
        Color(0x60FFD54F)
      } else {
        Color(0x1C000000)
      }
      val targetSpotColor = if (isLoading) {
        Color(0x15000000)
      } else if (isPressed) {
        Color(0x50FF9100)
      } else if (isHovered) {
        Color(0x85FFB300)
      } else {
        Color(0x2E000000)
      }
      val animatedAmbientColor by animateColorAsState(
        targetValue = targetAmbientColor,
        animationSpec = tween(350, easing = EaseOutQuad),
        label = "AmbientColor"
      )
      val animatedSpotColor by animateColorAsState(
        targetValue = targetSpotColor,
        animationSpec = tween(350, easing = EaseOutQuad),
        label = "SpotColor"
      )

      // Dynamic golden aura behind the button that expands and glows smoothly during hover/press
      val auraScale by animateFloatAsState(
        targetValue = if (isLoading) 0.95f else if (isPressed) 1.02f else if (isHovered) 1.18f else 1.0f,
        animationSpec = tween(400, easing = EaseOutQuad),
        label = "AuraScale"
      )
      val auraAlpha by animateFloatAsState(
        targetValue = if (isLoading) 0.05f else if (isPressed) 0.22f else if (isHovered) 0.55f else 0.10f,
        animationSpec = tween(400, easing = EaseOutQuad),
        label = "AuraAlpha"
      )
      val auraColor = Color(0xFFFFD54F)

      // Glowing aura shadow layer behind the button
      Box(
        modifier = Modifier
          .scale(buttonScale * auraScale)
          .align(Alignment.Center)
          .background(auraColor.copy(alpha = auraAlpha), RoundedCornerShape(32.dp))
          .padding(horizontal = 42.dp, vertical = 34.dp)
      ) {
        Box(modifier = Modifier.size(width = 140.dp, height = 60.dp))
      }

      // Centered Button
      Box(
        modifier = Modifier
          .scale(buttonScale)
          .shadow(
            elevation = buttonElevation,
            shape = RoundedCornerShape(32.dp),
            ambientColor = animatedAmbientColor,
            spotColor = animatedSpotColor,
            clip = false
          )
          .border(
            width = 2.dp,
            color = Color(0xFFFFB300), // premium amber golden border
            shape = RoundedCornerShape(32.dp)
          )
          .background(Color.White, RoundedCornerShape(32.dp))
          .hoverable(interactionSource)
          .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            enabled = !isLoading // completely disable when loading to prevent double actions
          ) {
            if (!isLoading) {
              isLoading = true
              coroutineScope.launch {
                kotlinx.coroutines.delay(1400) // Simulated radar sync delay
                onStartAdventure()
                isLoading = false
              }
            }
          }
          .padding(horizontal = 32.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center
      ) {
        if (isLoading) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            CircularProgressIndicator(
              color = Color(0xFFFFB300),
              strokeWidth = 3.5.dp,
              modifier = Modifier.size(30.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "SYNCING RADAR...",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 2.0.sp,
              color = Color(0xFF111111).copy(alpha = loaderPulseAlpha),
              fontFamily = FontFamily.SansSerif
            )
          }
        } else {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            Text(
              text = "🧭",
              fontSize = 32.sp,
              modifier = Modifier.rotate(idleRotation)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "START ADVENTURE",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 2.5.sp,
              color = Color(0xFF111111),
              fontFamily = FontFamily.SansSerif
            )
          }
        }
      }
    }

    // 3. Slide-Up Interactive Quest Drawer (Pure White, Premium Minimalist Layout)
    AnimatedVisibility(
      visible = isDrawerOpen,
      enter = slideInVertically(
        initialOffsetY = { it },
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium)
      ) + fadeIn(),
      exit = slideOutVertically(
        targetOffsetY = { it },
        animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMedium)
      ) + fadeOut()
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Black.copy(alpha = 0.15f))
          .clickable { isDrawerOpen = false },
        contentAlignment = Alignment.BottomCenter
      ) {
        // Drawer Card Content
        Card(
          colors = CardDefaults.cardColors(containerColor = Color.White),
          shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = false) {}, // Prevent clicks from leaking to background
          elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .navigationBarsPadding()
              .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            // Drag handle decorator
            Box(
              modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .background(Color(0xFFE5E5EA), RoundedCornerShape(2.dp))
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Title & Navigation Top Row
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "DAILY EXPLORATION",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111111),
                letterSpacing = 1.5.sp
              )
              
              IconButton(
                onClick = { isDrawerOpen = false },
                modifier = Modifier
                  .size(36.dp)
                  .background(Color(0xFFF2F2F7), CircleShape)
              ) {
                Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = "Close",
                  tint = Color(0xFF333333),
                  modifier = Modifier.size(16.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Gamified Stats Header
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              // Streak counter
              Row(
                modifier = Modifier
                  .background(Color(0xFFFFF9E6), RoundedCornerShape(12.dp))
                  .border(1.dp, Color(0xFFFFEB3B).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                  .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(text = "🔥", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "$streakCount DAY STREAK",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFFE65100),
                  letterSpacing = 0.5.sp
                )
              }

              // XP Indicator
              Row(
                modifier = Modifier
                  .background(Color(0xFFE8F5E9), RoundedCornerShape(12.dp))
                  .border(1.dp, Color(0xFF81C784).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                  .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(text = "💎", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "$totalXp XP",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF2E7D32),
                  letterSpacing = 0.5.sp
                )
              }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Quest Card
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFEBEBEF), RoundedCornerShape(20.dp))
                .background(Color(0xFFFAFAFC), RoundedCornerShape(20.dp))
                .padding(20.dp)
            ) {
              Column {
                Row(
                  verticalAlignment = Alignment.Top,
                  horizontalArrangement = Arrangement.SpaceBetween,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "ACTIVE KUEST",
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFF9999A1),
                      letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                      text = activeQuest,
                      fontSize = 15.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = Color(0xFF111111),
                      lineHeight = 22.sp
                    )
                  }

                  Spacer(modifier = Modifier.width(12.dp))

                  // Circular Quest Checkbox
                  Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                      .size(32.dp)
                      .clip(CircleShape)
                      .background(if (isQuestCompleted) Color(0xFF00C853) else Color(0xFFEEEEEE))
                      .clickable {
                        if (!isQuestCompleted) {
                          isQuestCompleted = true
                          streakCount++
                          totalXp += 250
                          
                          // Spawn Confetti Particles!
                          val newParticles = List(15) { i ->
                            val angle = Random.nextFloat() * Math.PI.toFloat() - Math.PI.toFloat()
                            val distance = Random.nextFloat() * 250f + 150f
                            EmojiParticle(
                              id = System.nanoTime() + i,
                              emoji = listOf("🎉", "✨", "🔥", "💎", "🚀", "🎯", "🙌", "💫", "🦄").random(),
                              targetX = (cos(angle) * distance).toFloat(),
                              targetY = (sin(angle) * distance).toFloat() - 150f,
                              scale = Random.nextFloat() * 0.5f + 0.8f,
                              rotation = Random.nextFloat() * 360f
                            )
                          }
                          particles = newParticles
                        } else {
                          // Uncheck and decrement for full testing loopability
                          isQuestCompleted = false
                          streakCount--
                          totalXp -= 250
                        }
                      }
                  ) {
                    if (isQuestCompleted) {
                      Text("✓", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                }

                if (isQuestCompleted) {
                  Spacer(modifier = Modifier.height(16.dp))
                  HorizontalDivider(color = Color(0xFFEBEBEF))
                  Spacer(modifier = Modifier.height(12.dp))
                  Text(
                    text = "🎉 Awesome! +250 XP added to your adventure rank.",
                    fontSize = 13.sp,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons Section (Reroll, Share)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              // Reroll Quest Button
              Button(
                onClick = {
                  isQuestCompleted = false
                  var nextIndex = currentQuestIndex
                  while (nextIndex == currentQuestIndex) {
                    nextIndex = Random.nextInt(questPool.size)
                  }
                  currentQuestIndex = nextIndex
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF2F2F7)),
                modifier = Modifier
                  .weight(1f)
                  .height(50.dp)
                  .border(1.dp, Color(0xFFE5E5EA), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
              ) {
                Text(text = "🎲 Roll New", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF33333F))
              }

              // Share Adventure Button
              Button(
                onClick = {
                  val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "I just started a new Kuest: '$activeQuest'! Join the adventure with me! 🧭✨")
                    type = "text/plain"
                  }
                  val shareIntent = Intent.createChooser(sendIntent, null)
                  context.startActivity(shareIntent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                modifier = Modifier
                  .weight(1f)
                  .height(50.dp)
                  .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = Color(0x15000000),
                    spotColor = Color(0x20000000),
                    clip = false
                  ),
                shape = RoundedCornerShape(16.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Share,
                  contentDescription = "Share",
                  tint = Color.White,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Share", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
              }
            }
          }
        }
      }
    }

    // 4. Dopamine Particle Layer (Rendering floats on complete)
    if (particles.isNotEmpty()) {
      particles.forEach { particle ->
        val xAnim = particle.targetX * particleProgress
        val yAnim = particle.targetY * particleProgress
        val scaleAnim = particle.scale * (1f - particleProgress * 0.3f)
        val alphaAnim = 1f - particleProgress

        Box(
          modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center)
            .offset(x = xAnim.dp, y = yAnim.dp)
            .scale(scaleAnim)
            .rotate(particle.rotation * particleProgress)
        ) {
          Text(
            text = particle.emoji,
            fontSize = 24.sp,
            modifier = Modifier.scale(alphaAnim) // fading representation
          )
        }
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun LandingScreenPreview() {
  MyApplicationTheme {
    KuestLandingScreen()
  }
}

@Composable
fun LavaHiveBackground(
  lavaTime: Float,
  isHovered: Boolean,
  modifier: Modifier = Modifier
) {
  Canvas(modifier = modifier) {
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    
    // Hexagon grid parameters
    val R = 28.dp.toPx() // Hexagon radius
    val gap = 3.dp.toPx() // Crack thickness
    val dy = 1.5f * R
    val dx = sqrt(3f) * R
    
    // Limits of drawing
    val maxRadius = 150.dp.toPx()
    
    // Underlay: Draw a subtle warm radial glowing atmosphere underneath the center
    val pulseGlowRadius = if (isHovered) 160.dp.toPx() else 120.dp.toPx()
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(
          Color(0x35FF9E00), // Soft orange glow
          Color(0x10FF5E00), // Fainter outer orange
          Color.Transparent
        ),
        center = Offset(centerX, centerY),
        radius = pulseGlowRadius
      ),
      radius = pulseGlowRadius,
      center = Offset(centerX, centerY)
    )

    // Columns and rows to cover a 320dp area
    val cols = 7
    val rows = 7
    
    // Determine speed of rotation based on hover
    val speedMultiplier = if (isHovered) 1.8f else 1.0f

    // List of tile and grid drawing
    for (row in -rows..rows) {
      for (col in -cols..cols) {
        val rowF = row.toFloat()
        val colF = col.toFloat()
        
        // Flat-topped hexagon center calculation
        val cy = centerY + rowF * dy
        val cx = centerX + colF * dx + (if (row % 2 != 0) dx / 2f else 0f)
        
        // Distance and angle from central anchor
        val dxFromCenter = cx - centerX
        val dyFromCenter = cy - centerY
        val distFromCenter = sqrt(dxFromCenter * dxFromCenter + dyFromCenter * dyFromCenter)
        
        if (distFromCenter < maxRadius) {
          // Fade alpha near the edge of the circular boundary
          val tileAlpha = if (distFromCenter > maxRadius - R) {
            ((maxRadius - distFromCenter) / R).coerceIn(0f, 1f)
          } else {
            1f
          }
          
          val angle = atan2(dyFromCenter, dxFromCenter)
          
          // 3-arm spiral (helix) lava intensity formula
          val helixArg = angle * 3f - distFromCenter * 0.012f + lavaTime * speedMultiplier
          val intensity = (sin(helixArg) * 0.5f + 0.5f)
          
          // Outer lava glow lines
          val borderLavaColor = androidx.compose.ui.graphics.lerp(
            Color(0xFFFF3D00), // Molten Orange-Red
            Color(0xFFFFEA00), // Blinding Lava Yellow
            intensity
          )
          
          // Draw hexagon tile (slightly smaller than full R to leave cracks)
          val drawR = R - gap / 2f
          val path = Path().apply {
            for (i in 0..5) {
              val hexAngle = i.toFloat() * (3.1415927f / 3f)
              val x = cx + drawR * cos(hexAngle)
              val y = cy + drawR * sin(hexAngle)
              if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
          }
          
          // Draw the glow outline inside the cracks FIRST so it radiates from the edges
          drawPath(
            path = path,
            color = borderLavaColor.copy(alpha = tileAlpha * (0.6f + intensity * 0.4f)),
            style = Stroke(width = gap * 1.8f)
          )
          
          // Draw an outer neon core glow for the hot lava cracks
          drawPath(
            path = path,
            color = Color.White.copy(alpha = tileAlpha * intensity * 0.8f),
            style = Stroke(width = gap * 0.6f)
          )

          // Fill the Marble Tile on top of the cracks
          // Base tile colors - rich marble finish using a linear gradient
          val gradientStartColor = Color(0xFFFCFCFD)
          val gradientEndColor = Color(0xFFECECEF)
          
          val tileBrush = Brush.linearGradient(
            colors = listOf(gradientStartColor, gradientEndColor),
            start = Offset(cx - drawR, cy - drawR),
            end = Offset(cx + drawR, cy + drawR)
          )
          
          drawPath(
            path = path,
            brush = tileBrush,
            alpha = tileAlpha
          )
          
          // Draw organic marble veins inside the tile to complete the marble effect
          // We use a deterministic random seed derived from the unique row/col position
          val seed = (row + 10) * 100 + (col + 10)
          val rand = java.util.Random(seed.toLong())
          
          val veinPath = Path()
          val numVeins = rand.nextInt(2) + 1 // 1 or 2 veins per tile
          
          for (v in 0 until numVeins) {
            // Generate a vein starting from one side of the hexagon and ending on another
            val startAngle = rand.nextFloat() * 2f * 3.1415927f
            val endAngle = startAngle + 3.1415927f + (rand.nextFloat() - 0.5f) * 1.5f
            
            val sx = cx + drawR * 0.8f * cos(startAngle)
            val sy = cy + drawR * 0.8f * sin(startAngle)
            val ex = cx + drawR * 0.8f * cos(endAngle)
            val ey = cy + drawR * 0.8f * sin(endAngle)
            
            // Midpoint with random organic wiggle
            val midAngle = (startAngle + endAngle) / 2f
            val midDist = drawR * 0.4f * (rand.nextFloat() - 0.5f)
            val mx = (sx + ex) / 2f + midDist * cos(midAngle)
            val my = (sy + ey) / 2f + midDist * sin(midAngle)
            
            veinPath.reset()
            veinPath.moveTo(sx, sy)
            veinPath.quadraticTo(mx, my, ex, ey)
            
            // Draw the vein using a clipping path so it stays strictly within the tile bounds
            clipPath(path) {
              drawPath(
                path = veinPath,
                color = Color(0xFFC4C4CD).copy(alpha = 0.25f * tileAlpha),
                style = Stroke(width = 1.dp.toPx())
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun YellowHoneycombBackground(
  lavaTime: Float,
  isHovered: Boolean,
  modifier: Modifier = Modifier
) {
  Canvas(modifier = modifier) {
    val R = 36.dp.toPx() // radius
    val gap = 4.dp.toPx()
    val dy = 1.5f * R
    val dx = sqrt(3f) * R
    
    // Warm rich yellow-orange base fill
    drawRect(color = Color(0xFFFFB300))
    
    val cols = (size.width / dx).toInt() + 3
    val rows = (size.height / dy).toInt() + 3
    
    val speedMultiplier = if (isHovered) 1.6f else 1.0f
    
    // Loop through grid to render interlocking tiles
    for (row in -2..rows) {
      for (col in -2..cols) {
        val rowF = row.toFloat()
        val colF = col.toFloat()
        
        // Offset rows for isometric staggered layout
        val cy = rowF * dy - 20.dp.toPx()
        val cx = colF * dx + (if (row % 2 != 0) dx / 2f else 0f) - 20.dp.toPx()
        
        // Animated dynamic breathing
        val waveArg = lavaTime * speedMultiplier + rowF * 0.35f + colF * 0.25f
        val hoverScale = if (isHovered) 1.03f else 1.0f
        val breathingOffset = sin(waveArg) * 2.5f.dp.toPx()
        
        val drawR = (R - gap / 2f) * hoverScale
        
        // 3D cast shadows underneath each tile (gives that beautiful 3D depth!)
        val shadowOffset = 6.dp.toPx() + breathingOffset * 0.5f
        val shadowPath = Path().apply {
          for (i in 0..5) {
            val hexAngle = i.toFloat() * (3.1415927f / 3f)
            val x = cx + drawR * cos(hexAngle)
            val y = cy + shadowOffset + drawR * sin(hexAngle)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
          }
          close()
        }
        
        // Soft dark amber shadow
        drawPath(
          path = shadowPath,
          color = Color(0x3B5D4037)
        )
        
        // Main tile path
        val tilePath = Path().apply {
          for (i in 0..5) {
            val hexAngle = i.toFloat() * (3.1415927f / 3f)
            val x = cx + drawR * cos(hexAngle)
            val y = cy + breathingOffset + drawR * sin(hexAngle)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
          }
          close()
        }
        
        // Gorgeous gradient colors (glowing gold to deep amber)
        val baseYellow = Color(0xFFFFD54F) // light gold yellow
        val goldYellow = Color(0xFFFFB300) // solid gold
        val amberOrange = Color(0xFFFF8F00) // deep warm amber
        
        val colorFactor = (sin(waveArg * 0.6f) * 0.5f + 0.5f)
        val tileColor = androidx.compose.ui.graphics.lerp(
          baseYellow,
          goldYellow,
          colorFactor
        )
        
        val tileBrush = Brush.linearGradient(
          colors = listOf(tileColor, amberOrange),
          start = Offset(cx - drawR, cy + breathingOffset - drawR),
          end = Offset(cx + drawR, cy + breathingOffset + drawR)
        )
        
        drawPath(
          path = tilePath,
          brush = tileBrush
        )
        
        // Delicate highlight line for premium finish
        val highlightPath = Path().apply {
          val hexAngle0 = 3f * (3.1415927f / 3f)
          val hexAngle1 = 4f * (3.1415927f / 3f)
          val hexAngle2 = 5f * (3.1415927f / 3f)
          val hexAngle3 = 6f * (3.1415927f / 3f)
          
          moveTo(cx + drawR * cos(hexAngle0), cy + breathingOffset + drawR * sin(hexAngle0))
          lineTo(cx + drawR * cos(hexAngle1), cy + breathingOffset + drawR * sin(hexAngle1))
          lineTo(cx + drawR * cos(hexAngle2), cy + breathingOffset + drawR * sin(hexAngle2))
          lineTo(cx + drawR * cos(hexAngle3), cy + breathingOffset + drawR * sin(hexAngle3))
        }
        
        drawPath(
          path = highlightPath,
          color = Color.White.copy(alpha = 0.4f),
          style = Stroke(width = 1.dp.toPx())
        )
      }
    }
  }
}

@Composable
fun KuestDashboardScreen(repository: com.example.db.EscrowRepository, onBack: () -> Unit) {
  val coroutineScope = rememberCoroutineScope()
  var currentTab by remember { mutableStateOf(0) } // 0: Pulse, 1: Chats, 2: Wallet, 3: Market
  var selectedLocation by remember { mutableStateOf("Kilimani, Nairobi") }
  var walletBalance by remember { mutableStateOf(45200.0) }
  var walletEscrowLocked by remember { mutableStateOf(15000.0) }
  var isPrivacyMode by remember { mutableStateOf(false) }
  
  // Premium Event Quest States
  var selectedPremiumQuestCategory by remember { mutableStateOf("All") }
  var selectedPremiumQuestDetail by remember { mutableStateOf<PremiumQuest?>(null) }
  var selectedRadarGemQuest by remember { mutableStateOf<PremiumQuest?>(null) }
  var partySizeState by remember { mutableStateOf(4) }
  
  // Dialog controls
  var showLocationDialog by remember { mutableStateOf(false) }
  var showQrDialog by remember { mutableStateOf<EscrowItem?>(null) }
  var showChatDialog by remember { mutableStateOf<MarketplaceItem?>(null) }
  var showSendMoneyDialog by remember { mutableStateOf(false) }
  var showCreateEscrowDialog by remember { mutableStateOf(false) }

  // Active chat listing context
  var activeListingInChat by remember { mutableStateOf<MarketplaceItem?>(null) }
  
  // Active escrow in checkout flow
  var activeEscrowInCheckout by remember { mutableStateOf<EscrowItem?>(null) }
  
  // Seller-side analytics funds
  var sellerPendingPayouts by remember { mutableStateOf(18000.0) }
  var sellerClearedEarnings by remember { mutableStateOf(124000.0) }
  
  // Toggle between Buyer and Seller perspectives in Wallet tab
  var isSellerMode by remember { mutableStateOf(false) }

  // Geofence Threat Simulation Service state
  var showGeofenceSimulation by remember { mutableStateOf(false) }
  val mockDeviceLocations = remember {
    listOf(
      MockDeviceLocation("📍 Kilimani Center (In-Zone Base Point)", -1.2921, 36.8073),
      MockDeviceLocation("📍 Yaya Center (In-Zone Boundary Margin)", -1.2938, 36.7994),
      MockDeviceLocation("📍 Westlands Mall (Out-Zone Suppressed)", -1.2588, 36.8027),
      MockDeviceLocation("📍 Jomo Kenyatta Airport (Far Out-Zone Suppressed)", -1.3323, 36.9211)
    )
  }
  var selectedMockDeviceLocation by remember { mutableStateOf(mockDeviceLocations[0]) }
  var isProcessingGeofencePayload by remember { mutableStateOf(false) }
  var geofenceConsoleLogs by remember {
    mutableStateOf(
      listOf(
        GeofenceLog(
          id = "g_init",
          timestamp = "06:10:00",
          message = "Geofence simulation network initialization engine online."
        )
      )
    )
  }

  // Custom simulation modals
  var showCheckoutDialog by remember { mutableStateOf(false) }
  var showScannerDialog by remember { mutableStateOf<EscrowItem?>(null) }
  var showSuccessAnimation by remember { mutableStateOf<String?>(null) }

  // Notification simulation service state
  var notificationsList by remember {
    mutableStateOf(
      listOf(
        InAppNotification(
          id = "init_1",
          title = "🔒 Escrow Contract Activated",
          body = "Your KES 85,000 for 'MacBook Pro M1' is securely locked in platform escrow custody.",
          timestamp = "10m ago",
          dealId = "1",
          targetState = "FUNDED"
        ),
        InAppNotification(
          id = "init_2",
          title = "🎉 Welcome to KUEST Escrow Ledger",
          body = "Explore direct zero-broker spaces and culinary experiences protected by peer-to-peer escrow.",
          timestamp = "2h ago",
          dealId = "",
          targetState = "WELCOME"
        )
      )
    )
  }
  var activeNotificationBanner by remember { mutableStateOf<InAppNotification?>(null) }
  var showNotificationsCenter by remember { mutableStateOf(false) }

  val triggerNotification: (String, String, String, String) -> Unit = { title, body, dId, targetState ->
    val newNotif = InAppNotification(
      id = java.util.UUID.randomUUID().toString(),
      title = title,
      body = body,
      timestamp = "Just now",
      dealId = dId,
      targetState = targetState
    )
    notificationsList = listOf(newNotif) + notificationsList
    activeNotificationBanner = newNotif
  }

  // Search filter query
  var searchQuery by remember { mutableStateOf("") }

  // Kuest monetization and campaign states
  var isProUser by remember { mutableStateOf(false) }
  var activeSponsoredQuest by remember { mutableStateOf<String?>(null) }
  var claimedPromoIds by remember { mutableStateOf(setOf<String>()) }
  
  // Map perspective style (standard, satellite, hybrid)
  var mapType by remember { mutableStateOf("standard") }

  // Active filter for neighborhood pulse ("All", "Safety", "Trade", "Events")
  var activeFeedFilter by remember { mutableStateOf("All") }

  var showTelemetryHudByDeal by remember { mutableStateOf<EscrowItem?>(null) }

  // Local currency and prefix config
  val currencyPrefix = when {
    selectedLocation.contains("Nairobi") -> "KES"
    selectedLocation.contains("Lagos") -> "₦"
    selectedLocation.contains("Johannesburg") -> "R"
    else -> "KES"
  }

  // Dynamic social posts feed list state
  var socialPosts by remember(selectedLocation) {
    mutableStateOf(
      when {
        selectedLocation.contains("Nairobi") -> listOf(
          SocialPost(
            id = "pulse_ninta",
            author = "Ninta Diranner",
            timeAgo = "7 hours ago",
            tag = "#Safety Alert",
            content = "Welcome to the new Neighbor-hub app ports of Africa and his healths.",
            commentsCount = 12,
            fireCount = 42,
            isHighAlert = true,
            tags = listOf("#Safety Alert", "#LocalMarket", "#Local Alert")
          ),
          SocialPost(
            id = "pulse_mariia",
            author = "Mariia Nonida",
            timeAgo = "2 hours ago",
            tag = "#Safety Alert",
            content = "Weod hearths placed with his mommunity staces on sara/how stads for Africa...",
            commentsCount = 5,
            fireCount = 18,
            isHighAlert = true,
            tags = listOf("#Safety Alert", "#LocalMarket")
          ),
          SocialPost("1", "@kamau_dev", "10m ago", "🚨 Safety", "Heavy traffic buildup near Ring Road. Police checking credentials near the roundabout. Take Lenana Road instead!", 24, 45, true),
          SocialPost("2", "@mama_chapo", "45m ago", "🛍️ Trade", "Fresh, hot chapati and beef stew ready for pickup! Proximity delivery within Kilimani. KES 250 per plate.", 12, 31, false),
          SocialPost("3", "@nai_creative", "2h ago", "🎉 Events", "Acoustic Sunset session starting at 6 PM! No entry fee, just bring good vibes. 🎸📍 Kilimani Rooftops.", 8, 19, false)
        )
        selectedLocation.contains("Lagos") -> listOf(
          SocialPost("1", "@tunde_safe", "5m ago", "🚨 Safety", "Avoid the Third Mainland Bridge for now. Minor bumper-to-bumper incident, but it's backing up fast.", 18, 52, true),
          SocialPost("2", "@suya_master", "25m ago", "🛍️ Trade", "Premium hot Suya and grilled dodo ready for delivery around Ikeja! ₦ 2,500 per platter.", 34, 88, false),
          SocialPost("3", "@eko_art", "1h ago", "🎉 Events", "Eko Tech & Creative Exhibition starts in 1 hour at the Civic Center. Free registration for Kuest adventurers! 🎨💻", 15, 41, false)
        )
        else -> listOf(
          SocialPost("1", "@jozi_patrol", "15m ago", "🚨 Safety", "All quiet around the Rivonia community patrol loop. Safety rating is clear 🟢.", 5, 12, true),
          SocialPost("2", "@gugu_eats", "50m ago", "🛍️ Trade", "Traditional Gauteng platter with chakalaka and pap ready for lunch orders. R 120.", 11, 28, false),
          SocialPost("3", "@sandton_art", "3h ago", "🎉 Events", "Sunset jazz session in Sandton Square tonight. Grab a cup of coffee and enjoy the tunes.", 21, 56, false)
        )
      }
    )
  }

  // Dynamic Escrow trades loaded from local Room database Flow
  val dbDeals by repository.allDeals.collectAsState(initial = emptyList())
  val escrowItems = dbDeals.map { deal ->
    val statusText = when (deal.currentState) {
      com.example.db.EscrowState.CREATED -> "Funds Awaiting"
      com.example.db.EscrowState.FUNDED -> "Funds Held"
      com.example.db.EscrowState.DISPATCHED -> "In Transit"
      com.example.db.EscrowState.COMPLETED -> "Delivered"
      com.example.db.EscrowState.DISPUTED -> "Disputed"
      com.example.db.EscrowState.CANCELLED -> "Cancelled"
    }
    val badge = when (deal.currentState) {
      com.example.db.EscrowState.CREATED -> "⏳ Awaiting Funding"
      com.example.db.EscrowState.FUNDED -> "🔒 Secured Escrow"
      com.example.db.EscrowState.DISPATCHED -> "🚚 In Transit"
      com.example.db.EscrowState.COMPLETED -> "✓ Completed"
      com.example.db.EscrowState.DISPUTED -> "🚨 Disputed"
      com.example.db.EscrowState.CANCELLED -> "❌ Cancelled"
    }
    EscrowItem(
      id = deal.id,
      title = deal.title,
      merchant = deal.sellerId,
      amount = deal.amount,
      status = statusText,
      badgeText = badge
    )
  }

  val onTransitionState: (String, com.example.db.EscrowState, String) -> Unit = { dealId, targetState, actor ->
    coroutineScope.launch {
      val deal = repository.getDealById(dealId)
      if (deal != null) {
        val oldState = deal.currentState
        val success = repository.transitionState(dealId, targetState, actor)
        if (success) {
          val amt = deal.amount
          when (targetState) {
            com.example.db.EscrowState.FUNDED -> {
              if (oldState == com.example.db.EscrowState.CREATED) {
                if (walletBalance >= amt) {
                  walletBalance -= amt
                  walletEscrowLocked += amt
                  sellerPendingPayouts += amt
                  showSuccessAnimation = "🔒 ESCROW FUNDED SUCCESSFULLY\n\nYour payment of $currencyPrefix ${String.format("%,.0f", amt)} has been locked in secure platform escrow holding. The vendor @${deal.sellerId} is notified to dispatch."
                  triggerNotification(
                    "🔒 Escrow Contract Funded",
                    "Funds of $currencyPrefix ${String.format("%,.0f", amt)} for '${deal.title}' are securely locked in platform custody.",
                    dealId,
                    "FUNDED"
                  )
                } else {
                  showSuccessAnimation = "Insufficient wallet balance to fund this escrow deal."
                }
              }
            }
            com.example.db.EscrowState.DISPATCHED -> {
              showSuccessAnimation = "🚚 COURIER DISPATCHED!\n\nEscrow transition logged: FUNDED ➔ DISPATCHED. Courier is on their way with secure Handshake verification QR!"
              triggerNotification(
                "🚚 Courier Dispatched!",
                "Merchant @${deal.sellerId} has dispatched '${deal.title}'. Proximity courier is en route with your Handshake QR!",
                dealId,
                "DISPATCHED"
              )
            }
            com.example.db.EscrowState.DISPUTED -> {
              showSuccessAnimation = "🚨 DISPUTE REGISTERED\n\nEscrow transition logged: ${oldState} ➔ DISPUTED. The funds of $currencyPrefix ${String.format("%,.0f", amt)} are locked in vault security. Platform support team is alerted!"
              triggerNotification(
                "🚨 Dispute Opened",
                "A formal dispute was logged for '${deal.title}'. Secure ledger has frozen the $currencyPrefix ${String.format("%,.0f", amt)} payout.",
                dealId,
                "DISPUTED"
              )
            }
            com.example.db.EscrowState.COMPLETED -> {
              if (oldState == com.example.db.EscrowState.FUNDED || oldState == com.example.db.EscrowState.DISPATCHED || oldState == com.example.db.EscrowState.DISPUTED) {
                walletEscrowLocked = maxOf(0.0, walletEscrowLocked - amt)
                sellerPendingPayouts = maxOf(0.0, sellerPendingPayouts - amt)
                sellerClearedEarnings += amt
              }
              showSuccessAnimation = "✓ ESCROW COMPLETED\n\nHandshake verified! $currencyPrefix ${String.format("%,.0f", amt)} successfully released to @${deal.sellerId}'s cleared earnings!"
              triggerNotification(
                "✓ Escrow Payment Settled",
                "Handshake verified! $currencyPrefix ${String.format("%,.0f", amt)} has been successfully released to @${deal.sellerId}.",
                dealId,
                "COMPLETED"
              )
            }
            com.example.db.EscrowState.CREATED -> {}
            com.example.db.EscrowState.CANCELLED -> {
              if (oldState == com.example.db.EscrowState.DISPUTED || oldState == com.example.db.EscrowState.FUNDED || oldState == com.example.db.EscrowState.DISPATCHED) {
                walletEscrowLocked = maxOf(0.0, walletEscrowLocked - amt)
                sellerPendingPayouts = maxOf(0.0, sellerPendingPayouts - amt)
                walletBalance += amt
                showSuccessAnimation = "❌ DEAL CANCELLED & REFUNDED\n\nFunds of $currencyPrefix ${String.format("%,.0f", amt)} have been fully refunded back to your wallet balance."
                triggerNotification(
                  "❌ Deal Cancelled & Refunded",
                  "Contract cancelled for '${deal.title}'. Your $currencyPrefix ${String.format("%,.0f", amt)} has been refunded.",
                  dealId,
                  "CANCELLED"
                )
              } else {
                showSuccessAnimation = "❌ DEAL CANCELLED\n\nDeal [${deal.title}] cancelled successfully."
                triggerNotification(
                  "❌ Deal Cancelled",
                  "Deal for '${deal.title}' has been cancelled.",
                  dealId,
                  "CANCELLED"
                )
              }
            }
          }
        } else {
          showSuccessAnimation = "⚠️ INVALID STATE TRANSITION!\n\nTransition from $oldState to $targetState is not allowed by the Escrow State Machine Guards."
        }
      }
    }
  }

  // Dynamic Marketplace listings
  val marketplaceItems = remember(selectedLocation) {
    listOf(
      MarketplaceItem("1", "1-BR Premium Studio Loft", "🏠 SPACE", "400m away", 35000.0, "@verified_landlord", true),
      MarketplaceItem("2", "Nigerian Jollof & Suya Bowl", "🍳 CULINARY", "1.2km away", 650.0, "@verified_chef", true),
      MarketplaceItem("3", "Cozy Shared Workspace Desk", "🏠 SPACE", "800m away", 1200.0, "@cowork_hub", true),
      MarketplaceItem("4", "Artisanal Sourdough Loaf", "🍳 CULINARY", "300m away", 450.0, "@baking_bantu", false)
    )
  }

  // Active Map Pin highlight state
  var selectedPinInfo by remember { mutableStateOf<String?>(null) }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = getAppBg(),
    topBar = {
      Surface(
        color = getSurfaceBg(),
        border = BorderStroke(1.dp, getBorderColor()),
        modifier = Modifier.shadow(4.dp)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text("🧭", fontSize = 22.sp)
            Text(
              text = "KUEST",
              fontSize = 18.sp,
              fontWeight = FontWeight.Black,
              color = getTextPrimary(),
              letterSpacing = 1.sp
            )
          }
          
          // Theme Switcher Toggle Button
          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(20.dp))
              .background(if (isLightModeGlobal) Color(0xFFE2E8F0) else Color(0xFF1E293B))
              .clickable { isLightModeGlobal = !isLightModeGlobal }
              .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(if (isLightModeGlobal) "☀️ Light" else "🌙 Dark", color = getTextPrimary(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    },
    bottomBar = {
      Surface(
        color = getSurfaceBg(),
        border = BorderStroke(1.dp, getBorderColor()),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.shadow(16.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(vertical = 12.dp, horizontal = 12.dp),
          horizontalArrangement = Arrangement.SpaceAround,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Bottom Tab 0: Discover (Active by default, gold icon + small dot indicator)
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
              .clickable { currentTab = 0 }
              .padding(6.dp)
              .weight(1f)
          ) {
            Text(
              text = "🧭",
              fontSize = 22.sp,
              color = if (currentTab == 0) Color(0xFFF4B942) else getTextSecondary()
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "Discover",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = if (currentTab == 0) Color(0xFFF4B942) else getTextSecondary()
            )
            if (currentTab == 0) {
              Box(
                modifier = Modifier
                  .padding(top = 2.dp)
                  .size(4.dp)
                  .background(Color(0xFFF4B942), CircleShape)
              )
            }
          }

          // Bottom Tab 1: Inbox (Binds to original Chats tab 1)
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
              .clickable { currentTab = 1 }
              .padding(6.dp)
              .weight(1f)
          ) {
            Text(
              text = "📥",
              fontSize = 22.sp,
              color = if (currentTab == 1) Color(0xFFF4B942) else getTextSecondary()
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "Inbox",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = if (currentTab == 1) Color(0xFFF4B942) else getTextSecondary()
            )
            if (currentTab == 1) {
              Box(
                modifier = Modifier
                  .padding(top = 2.dp)
                  .size(4.dp)
                  .background(Color(0xFFF4B942), CircleShape)
              )
            }
          }

          // Bottom Tab 2: Wallet (Binds to original Wallet tab 2)
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
              .clickable { currentTab = 2 }
              .padding(6.dp)
              .weight(1f)
          ) {
            Text(
              text = "💳",
              fontSize = 22.sp,
              color = if (currentTab == 2) Color(0xFFF4B942) else getTextSecondary()
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "Wallet",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = if (currentTab == 2) Color(0xFFF4B942) else getTextSecondary()
            )
            if (currentTab == 2) {
              Box(
                modifier = Modifier
                  .padding(top = 2.dp)
                  .size(4.dp)
                  .background(Color(0xFFF4B942), CircleShape)
              )
            }
          }

          // Bottom Tab 3: Travel Essentials (Binds to original Kuest Souk tab 3)
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
              .clickable { currentTab = 3 }
              .padding(6.dp)
              .weight(1f)
          ) {
            Text(
              text = "🧳",
              fontSize = 22.sp,
              color = if (currentTab == 3) Color(0xFFF4B942) else getTextSecondary()
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "Travel Essentials",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = if (currentTab == 3) Color(0xFFF4B942) else getTextSecondary()
            )
            if (currentTab == 3) {
              Box(
                modifier = Modifier
                  .padding(top = 2.dp)
                  .size(4.dp)
                  .background(Color(0xFFF4B942), CircleShape)
              )
            }
          }
        }
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      when (currentTab) {
        0 -> {
          KuestPremiumHomeScreen(
            selectedLocation = selectedLocation,
            onShowLocationDialogChange = { showLocationDialog = it },
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            currencyPrefix = currencyPrefix,
            onTriggerCheckout = { item, quest ->
              activeEscrowInCheckout = item
              currentTab = 2 // Move to wallet checkout ledger
              triggerNotification(
                "🔒 Escrow Party Contract Formed",
                "Locked ${currencyPrefix} " + String.format("%,.0f", item.amount) + " in secure escrow for ${quest.title}. Invite link shared!",
                "",
                "FUNDED"
              )
            },
            onCreateErrandDeal = { title, riderHandle, amount ->
              coroutineScope.launch {
                val newId = System.currentTimeMillis().toString()
                repository.createDeal(
                  id = newId,
                  buyerId = "@kuest_explorer",
                  sellerId = riderHandle,
                  title = title,
                  amount = amount,
                  currency = "KES",
                  secureHandshakeHash = "BIKE_ERRAND_${newId}",
                  marketListingId = null,
                  chatRoomId = null
                )
                repository.transitionState(newId, com.example.db.EscrowState.FUNDED, "@kuest_explorer")
                triggerNotification(
                  "🏍️ Errand Courier Booked",
                  "Locked KES " + String.format("%,.0f", amount) + " in secure escrow for: $title with $riderHandle.",
                  "",
                  "FUNDED"
                )
              }
            },
            onTriggerNotification = { title, body, dId, targetState ->
              triggerNotification(title, body, dId, targetState)
            }
          )
        }
        1 -> {
          ChatsScreen(
            currencyPrefix = currencyPrefix,
            activeListingInChat = activeListingInChat,
            onActiveListingChange = { activeListingInChat = it },
            onLockEscrowClick = { listing ->
              val itemToCheckout = EscrowItem(
                id = System.currentTimeMillis().toString(),
                title = listing.title,
                merchant = listing.verifiedOwner,
                amount = listing.price,
                status = "Funds Held",
                badgeText = "🔒 Secured Escrow"
              )
              activeEscrowInCheckout = itemToCheckout
              currentTab = 2
            },
            onBack = onBack
          )
        }
        2 -> {
          WalletScreen(
            currencyPrefix = currencyPrefix,
            selectedLocation = selectedLocation,
            walletBalance = walletBalance,
            onWalletBalanceChange = { walletBalance = it },
            walletEscrowLocked = walletEscrowLocked,
            onWalletEscrowLockedChange = { walletEscrowLocked = it },
            sellerPendingPayouts = sellerPendingPayouts,
            onSellerPendingPayoutsChange = { sellerPendingPayouts = it },
            sellerClearedEarnings = sellerClearedEarnings,
            onSellerClearedEarningsChange = { sellerClearedEarnings = it },
            isSellerMode = isSellerMode,
            onIsSellerModeChange = { isSellerMode = it },
            activeEscrowInCheckout = activeEscrowInCheckout,
            onActiveEscrowInCheckoutChange = { activeEscrowInCheckout = it },
            escrowItems = escrowItems,
            onConfirmCheckout = { checkoutItem ->
              coroutineScope.launch {
                val existing = repository.getDealById(checkoutItem.id)
                if (existing == null) {
                  repository.createDeal(
                    id = checkoutItem.id,
                    buyerId = "@kuest_explorer",
                    sellerId = checkoutItem.merchant,
                    title = checkoutItem.title,
                    amount = checkoutItem.amount,
                    currency = currencyPrefix,
                    secureHandshakeHash = "hs_${checkoutItem.id}",
                    marketListingId = null,
                    chatRoomId = null
                  )
                }
                onTransitionState(checkoutItem.id, com.example.db.EscrowState.FUNDED, "@kuest_explorer")
              }
            },
            onDispatchClick = { saleItem ->
              onTransitionState(saleItem.id, com.example.db.EscrowState.DISPATCHED, "@verified_seller")
            },
            onTransitionState = onTransitionState,
            getLogsForDeal = { dealId -> repository.getLogsForDeal(dealId) },
            onGenerateQrClick = { showQrDialog = it },
            onScanReceiptClick = { showScannerDialog = it },
            onSendMoneyClick = { showSendMoneyDialog = true },
            onCustomEscrowClick = { showCreateEscrowDialog = true },
            onSuccessMessage = { showSuccessAnimation = it },
            onLaunchTelemetryHud = { showTelemetryHudByDeal = it },
            onBack = onBack,
            isPrivacyMode = isPrivacyMode,
            onIsPrivacyModeChange = { isPrivacyMode = it }
          )
        }
        3 -> {
          MarketScreen(
            currencyPrefix = currencyPrefix,
            marketplaceItems = marketplaceItems,
            onMessageClick = { listing ->
              activeListingInChat = listing
              currentTab = 1
            },
            onBack = onBack,
            onLockEscrowClick = { listing ->
              val itemToCheckout = EscrowItem(
                id = System.currentTimeMillis().toString(),
                title = listing.title,
                merchant = listing.verifiedOwner,
                amount = listing.price,
                status = "Funds Held",
                badgeText = "🔒 Secured Escrow"
              )
              activeEscrowInCheckout = itemToCheckout
              currentTab = 2
            }
          )
        }
        1000 -> {
          // SCREEN 2: Wallet & Escrow Ledger (High-Contrast Fintech layout)
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(16.dp)
              .verticalScroll(rememberScrollState())
          ) {
            // App Bar Row
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Escrow Ledger 🔒",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
              )

              IconButton(
                onClick = onBack,
                modifier = Modifier
                  .size(36.dp)
                  .background(Color(0xFF1E1E24), CircleShape)
              ) {
                Text(text = "🏠", fontSize = 16.sp)
              }
            }

            // Top Premium Card Container with Purple/Indigo space aesthetic
            Card(
              colors = CardDefaults.cardColors(containerColor = Color.Transparent),
              shape = RoundedCornerShape(24.dp),
              modifier = Modifier
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(24.dp))
                .background(
                  Brush.linearGradient(
                    colors = listOf(Color(0xFF311B92), Color(0xFF1A237E), Color(0xFF0D1B2A))
                  ),
                  RoundedCornerShape(24.dp)
                )
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(24.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column {
                    Text(
                      text = "PASSPORT BALANCE",
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color.White.copy(alpha = 0.6f),
                      letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                      text = "$currencyPrefix " + String.format("%,.2f", walletBalance),
                      fontSize = 28.sp,
                      fontWeight = FontWeight.Black,
                      color = Color.White
                    )
                  }
                  Text(text = "🪙", fontSize = 36.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column {
                    Text(
                      text = "LOCKED IN ESCROW",
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color.White.copy(alpha = 0.6f),
                      letterSpacing = 0.5.sp
                    )
                    Text(
                      text = "$currencyPrefix " + String.format("%,.2f", walletEscrowLocked),
                      fontSize = 15.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFFFFB300)
                    )
                  }

                  Column(horizontalAlignment = Alignment.End) {
                    Text(
                      text = "LOCALE WALLET",
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                      text = if (currencyPrefix == "KES") "Nairobi, Kenya" else if (currencyPrefix == "₦") "Lagos, Nigeria" else "Sandton, SA",
                      fontSize = 13.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color.White
                    )
                  }
                }

                Spacer(modifier = Modifier.height(20.dp))

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                Spacer(modifier = Modifier.height(16.dp))

                // Fintech Wallet quick action buttons
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Button(
                    onClick = { showSendMoneyDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                  ) {
                    Text("💸 Transfer", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                  }

                  Spacer(modifier = Modifier.width(10.dp))

                  Button(
                    onClick = { showCreateEscrowDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9100)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                  ) {
                    Text("🔒 Lock Escrow", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Escrow Timeline Header
            Text(
              text = "Active Trades & Escrow",
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White,
              letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Vertical list of active escrows with custom progress tracker bars
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
              escrowItems.forEach { item ->
                EscrowRowItem(
                  item = item,
                  currencyPrefix = currencyPrefix,
                  onGenerateQr = { showQrDialog = item }
                )
              }
            }
          }
        }
        2000 -> {
          // SCREEN 3: Zero-Broker Marketplace
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(16.dp)
          ) {
            // Marketplace header row
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = "Zero-Broker Hub 🏠",
                  fontSize = 20.sp,
                  fontWeight = FontWeight.Black,
                  color = Color.White
                )
                Text(
                  text = "Direct community marketplace. Zero fees, zero brokers.",
                  fontSize = 11.sp,
                  color = Color.Gray
                )
              }

              IconButton(
                onClick = onBack,
                modifier = Modifier
                  .size(36.dp)
                  .background(Color(0xFF1E1E24), CircleShape)
              ) {
                Text(text = "🏠", fontSize = 16.sp)
              }
            }

            // Quick Category Filters
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              listOf("All listings", "🏠 Spaces", "🍳 Culinary").forEach { filter ->
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1E24))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                  Text(
                    text = filter,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dual-column grid displaying listings
            LazyVerticalGrid(
              columns = GridCells.Fixed(2),
              modifier = Modifier.fillMaxSize(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              items(marketplaceItems) { listing ->
                MarketplaceCard(
                  item = listing,
                  currencyPrefix = currencyPrefix,
                  onMessageClick = { showChatDialog = listing }
                )
              }
            }
          }
        }
      }

      // -- ALL POPUP INTERACTIVE DIALOGS --

      // -- ALL POPUP INTERACTIVE DIALOGS --

      // Holographic Handshake Receipt QR Scanner Simulator
      if (showScannerDialog != null) {
        val scannerItem = showScannerDialog!!
        Dialog(onDismissRequest = { showScannerDialog = null }) {
          Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D11)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.5.dp, Color(0xFF00C853)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp)
          ) {
            Column(
              modifier = Modifier.padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = "HOLOGRAPHIC SCANNER 🛰️",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
              )
              Text(
                text = "KUEST ESCROW VERIFICATION SHIELD",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00C853),
                letterSpacing = 0.5.sp
              )

              Spacer(modifier = Modifier.height(20.dp))

              // Animated Laser scanning canvas
              Box(
                modifier = Modifier
                  .size(180.dp)
                  .background(Color.Black, RoundedCornerShape(16.dp))
                  .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                  .padding(12.dp),
                contentAlignment = Alignment.Center
              ) {
                // Animated scan line
                val scanTransition = rememberInfiniteTransition(label = "Scan")
                val scanY by scanTransition.animateFloat(
                  initialValue = 0.1f,
                  targetValue = 0.9f,
                  animationSpec = infiniteRepeatable(
                    animation = tween(1800, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                  ),
                  label = "ScanY"
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                  val h = size.height
                  val w = size.width
                  
                  // Draw HUD brackets
                  val bracketLen = 20.dp.toPx()
                  val strokeW = 2.dp.toPx()
                  val hudColor = Color(0xFF00C853)

                  // Top Left bracket
                  drawLine(hudColor, Offset(0f, 0f), Offset(bracketLen, 0f), strokeWidth = strokeW)
                  drawLine(hudColor, Offset(0f, 0f), Offset(0f, bracketLen), strokeWidth = strokeW)

                  // Top Right bracket
                  drawLine(hudColor, Offset(w, 0f), Offset(w - bracketLen, 0f), strokeWidth = strokeW)
                  drawLine(hudColor, Offset(w, 0f), Offset(w, bracketLen), strokeWidth = strokeW)

                  // Bottom Left bracket
                  drawLine(hudColor, Offset(0f, h), Offset(bracketLen, h), strokeWidth = strokeW)
                  drawLine(hudColor, Offset(0f, h), Offset(0f, h - bracketLen), strokeWidth = strokeW)

                  // Bottom Right bracket
                  drawLine(hudColor, Offset(w, h), Offset(w - bracketLen, h), strokeWidth = strokeW)
                  drawLine(hudColor, Offset(w, h), Offset(w, h - bracketLen), strokeWidth = strokeW)

                  // Holographic scanning laser line
                  val laserY = h * scanY
                  drawLine(
                    color = Color(0xFF00C853),
                    start = Offset(0f, laserY),
                    end = Offset(w, laserY),
                    strokeWidth = 3.dp.toPx()
                  )

                  // Draw glowing laser bloom
                  drawRect(
                    brush = Brush.verticalGradient(
                      colors = listOf(Color(0xFF00C853).copy(alpha = 0.15f), Color.Transparent),
                      startY = laserY - 15.dp.toPx(),
                      endY = laserY
                    ),
                    topLeft = Offset(0f, laserY - 15.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(w, 15.dp.toPx())
                  )
                }

                Text("Aligning partner QR...", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
              }

              Spacer(modifier = Modifier.height(16.dp))

              Text(
                text = scannerItem.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
              )
              Text(
                text = "Releasing Escrow: $currencyPrefix " + String.format("%,.0f", scannerItem.amount),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00C853)
              )

              Spacer(modifier = Modifier.height(14.dp))

              Text(
                text = "Scan and align with the dispatch partner's Handshake QR code. By aligning, you legally release custody of the held funds to the vendor.",
                fontSize = 9.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp
              )

              Spacer(modifier = Modifier.height(20.dp))

              Button(
                onClick = {
                  onTransitionState(scannerItem.id, com.example.db.EscrowState.COMPLETED, "@kuest_explorer")
                  showScannerDialog = null
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text("Align & Release Funds", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
              }
            }
          }
        }
      }

      // Full-screen Gamified Cyber HUD Overlay
      if (showTelemetryHudByDeal != null) {
        val dealId = showTelemetryHudByDeal!!.id
        val activeDealItem = escrowItems.find { it.id == dealId } ?: showTelemetryHudByDeal!!
        val transState = when (activeDealItem.status) {
          "Funds Awaiting" -> TransactionState.CREATED
          "Funds Held" -> TransactionState.FUNDED
          "In Transit" -> TransactionState.DISPATCHED
          "Disputed" -> TransactionState.DISPUTED
          "Delivered" -> TransactionState.COMPLETED
          else -> TransactionState.CREATED
        }
        
        Dialog(
          onDismissRequest = { showTelemetryHudByDeal = null },
          properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
          Box(modifier = Modifier.fillMaxSize()) {
            GamifiedTransactionScreen(
              transactionId = activeDealItem.id,
              currentState = transState,
              displayAmount = "$currencyPrefix " + String.format("%,.0f", activeDealItem.amount),
              onStateTransitionRequested = { id, targetState ->
                val roomState = when (targetState) {
                  TransactionState.CREATED -> com.example.db.EscrowState.CREATED
                  TransactionState.FUNDED -> com.example.db.EscrowState.FUNDED
                  TransactionState.DISPATCHED -> com.example.db.EscrowState.DISPATCHED
                  TransactionState.COMPLETED -> com.example.db.EscrowState.COMPLETED
                  TransactionState.DISPUTED -> com.example.db.EscrowState.DISPUTED
                }
                coroutineScope.launch {
                  repository.transitionState(id, roomState, "SYSTEM_WEBSOCKET")
                }
              }
            )
            
            // Floating Close button with futuristic borders
            IconButton(
              onClick = { showTelemetryHudByDeal = null },
              modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                .border(1.dp, NeonCyan, CircleShape)
            ) {
              Text("✕", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
          }
        }
      }

      // Success confirmation dialog
      if (showSuccessAnimation != null) {
        val successMsg = showSuccessAnimation!!
        Dialog(onDismissRequest = { showSuccessAnimation = null }) {
          Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141418)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFFFFB300)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp)
          ) {
            Column(
              modifier = Modifier.padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Box(
                modifier = Modifier
                  .size(56.dp)
                  .background(Color(0xFFFFB300).copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Text("🤝", fontSize = 28.sp)
              }

              Spacer(modifier = Modifier.height(16.dp))

              Text(
                text = "Transaction Settled",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
              )

              Spacer(modifier = Modifier.height(8.dp))

              Text(
                text = successMsg,
                fontSize = 11.sp,
                color = Color.LightGray,
                textAlign = TextAlign.Center,
                lineHeight = 15.sp
              )

              Spacer(modifier = Modifier.height(24.dp))

              Button(
                onClick = { showSuccessAnimation = null },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text("Return to Vault", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
              }
            }
          }
        }
      }

      // 0. Geofence Threat Simulation Dashboard Dialog
      if (showGeofenceSimulation) {
        Dialog(onDismissRequest = { showGeofenceSimulation = false }) {
          Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131316)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 620.dp)
              .shadow(24.dp, RoundedCornerShape(24.dp))
          ) {
            Column(modifier = Modifier.padding(20.dp)) {
              // Header
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(text = "📡", fontSize = 20.sp)
                  Spacer(modifier = Modifier.width(8.dp))
                  Column {
                    Text(
                      text = "Low-Latency Geofence Tester",
                      fontSize = 15.sp,
                      fontWeight = FontWeight.Black,
                      color = Color.White
                    )
                    Text(
                      text = "Threat Alerting Simulation Engine",
                      fontSize = 10.sp,
                      color = Color.Gray
                    )
                  }
                }

                Text(
                  text = "✕",
                  fontSize = 14.sp,
                  color = Color.Gray,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier
                    .clickable { showGeofenceSimulation = false }
                    .padding(4.dp)
                )
              }

              Spacer(modifier = Modifier.height(16.dp))

              // Simulation Parameters Panel
              Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C24)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(14.dp)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🛡️", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "Threat Geofence Parameters",
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color.White
                    )
                  }
                  Spacer(modifier = Modifier.height(8.dp))
                  
                  // Incident center vector details
                  Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                      .padding(10.dp)
                  ) {
                    Text(
                      text = "1. Active Threat Center (Fixed Target)",
                      fontSize = 9.sp,
                      color = Color(0xFFFF9100),
                      fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = "Lenana Roundabout Traffic Blockade",
                      fontSize = 11.sp,
                      color = Color.White,
                      fontWeight = FontWeight.Bold
                    )
                    Text(
                      text = "Target Vector: Lat: -1.2915, Lng: 36.8060 | Radius: 1500m",
                      fontSize = 9.sp,
                      color = Color.Gray
                    )
                  }

                  Spacer(modifier = Modifier.height(12.dp))

                  // Select Mock Device Physical Location Dropdown Mock/Card list
                  Text(
                    text = "2. Select Mock Device Location Spoof",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                  )
                  Spacer(modifier = Modifier.height(6.dp))

                  // Custom visual list selection of mock positions
                  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    mockDeviceLocations.forEach { loc ->
                      val isSel = selectedMockDeviceLocation.name == loc.name
                      Card(
                        colors = CardDefaults.cardColors(
                          containerColor = if (isSel) Color(0xFFFF9100).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f)
                        ),
                        border = BorderStroke(
                          1.dp,
                          if (isSel) Color(0xFFFF9100) else Color.White.copy(alpha = 0.08f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                          .fillMaxWidth()
                          .clickable {
                            selectedMockDeviceLocation = loc
                            val now = java.time.LocalTime.now()
                            val timeStr = String.format("%02d:%02d:%02d", now.hour, now.minute, now.second)
                            geofenceConsoleLogs = listOf(
                              GeofenceLog(
                                id = "g_${System.currentTimeMillis()}",
                                timestamp = timeStr,
                                message = "Device location spoof updated to: ${loc.name}"
                              )
                            ) + geofenceConsoleLogs
                          }
                      ) {
                        Row(
                          modifier = Modifier.padding(10.dp),
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                          Column {
                            Text(
                              text = loc.name,
                              fontSize = 11.sp,
                              fontWeight = FontWeight.Bold,
                              color = if (isSel) Color(0xFFFF9100) else Color.White
                            )
                            Text(
                              text = "Lat: ${loc.lat}, Lng: ${loc.lng}",
                              fontSize = 9.sp,
                              color = Color.Gray
                            )
                          }
                          if (isSel) {
                            Text(text = "🛰️ Active", fontSize = 10.sp, color = Color(0xFFFF9100), fontWeight = FontWeight.Bold)
                          }
                        }
                      }
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              // Trigger Button
              Button(
                onClick = {
                  if (!isProcessingGeofencePayload) {
                    isProcessingGeofencePayload = true
                    val now = java.time.LocalTime.now()
                    val timeStr1 = String.format("%02d:%02d:%02d", now.hour, now.minute, now.second)
                    geofenceConsoleLogs = listOf(
                      GeofenceLog(
                        id = "g_${System.currentTimeMillis()}",
                        timestamp = timeStr1,
                        message = "📥 Inbound FCM Data Payload: [SAFETY_ALERT] - Lat: -1.2915, Lng: 36.8060, MaxRadius: 1500.0m"
                      )
                    ) + geofenceConsoleLogs

                    coroutineScope.launch {
                      kotlinx.coroutines.delay(900)
                      val dist = calculateGeofenceDistance(
                        selectedMockDeviceLocation.lat,
                        selectedMockDeviceLocation.lng,
                        -1.2915,
                        36.8060
                      )

                      val nowFinish = java.time.LocalTime.now()
                      val timeStr2 = String.format("%02d:%02d:%02d", nowFinish.hour, nowFinish.minute, nowFinish.second)

                      if (dist <= 1500.0) {
                        val msg = "💥 TRIGGER ALERT! Device inside geofence perimeter (${String.format("%.0f", dist)}m away). Native OS alarm window rendered."
                        geofenceConsoleLogs = listOf(
                          GeofenceLog(
                            id = "g_${System.currentTimeMillis()}",
                            timestamp = timeStr2,
                            message = msg,
                            isTriggered = true
                          )
                        ) + geofenceConsoleLogs

                        triggerNotification(
                          "💥 GEOFENCE CRITICAL SECURITY WARNING",
                          "You are inside the threat perimeter (${String.format("%.0f", dist)}m from blockade). Evacuate Lenana Rd immediately.",
                          "",
                          "GEOFENCE_TRIGGER"
                        )
                      } else {
                        val km = dist / 1000.0
                        val msg = "🛡️ SILENT SUPPRESSION: Device outside boundaries (${String.format("%.1f", km)}km away). Packet discarded safely."
                        geofenceConsoleLogs = listOf(
                          GeofenceLog(
                            id = "g_${System.currentTimeMillis()}",
                            timestamp = timeStr2,
                            message = msg,
                            isSuppressed = true
                          )
                        ) + geofenceConsoleLogs
                      }
                      isProcessingGeofencePayload = false
                    }
                  }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9100)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                if (isProcessingGeofencePayload) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text("PROCESSING INBOUND PACKET...", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                } else {
                  Text("FIRE INBOUND NOTIFICATION TEST PAYLOAD", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              // Live Logs Terminal Header
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "LIVE BACKGROUND SERVICE CONSOLE LOGS",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.Gray,
                  letterSpacing = 0.5.sp
                )
                Text(
                  text = "Clear Terminal 🧹",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.Red.copy(alpha = 0.8f),
                  modifier = Modifier.clickable {
                    geofenceConsoleLogs = emptyList()
                  }
                )
              }

              Spacer(modifier = Modifier.height(6.dp))

              // Terminal Screen Block
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .weight(1f)
                  .background(Color(0xFF07080B), RoundedCornerShape(12.dp))
                  .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                  .padding(10.dp)
              ) {
                if (geofenceConsoleLogs.isEmpty()) {
                  Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                  ) {
                    Text("No logs recorded.", fontSize = 10.sp, color = Color.DarkGray)
                  }
                } else {
                  androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    items(geofenceConsoleLogs) { log ->
                      val textColor = when {
                        log.isTriggered -> Color(0xFFFF5252) // Neon red
                        log.isSuppressed -> Color(0xFF40C4FF) // Neon blue
                        else -> Color.LightGray
                      }
                      Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                          text = "[${log.timestamp}]",
                          color = Color.Gray,
                          fontSize = 9.sp,
                          fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                          text = log.message,
                          color = textColor,
                          fontSize = 9.sp,
                          fontFamily = FontFamily.Monospace
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }

      // 1. Location Selection Dialog
      if (showLocationDialog) {
        Dialog(onDismissRequest = { showLocationDialog = false }) {
          Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp)
              .shadow(24.dp, RoundedCornerShape(24.dp))
          ) {
            Column(modifier = Modifier.padding(20.dp)) {
              Text(
                text = "Select Neighborhood 🧭",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1A1A1A)
              )
              Spacer(modifier = Modifier.height(16.dp))

              val locations = listOf("Kilimani, Nairobi", "Karen, Nairobi", "Westlands, Nairobi", "Gigiri, Nairobi")
              locations.forEach { loc ->
                val isSelected = selectedLocation == loc
                Card(
                  colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFFFFF8E1) else Color(0xFFF8FAFC)
                  ),
                  shape = RoundedCornerShape(12.dp),
                  border = BorderStroke(
                    1.dp,
                    if (isSelected) Color(0xFFFFD54F) else Color(0xFFE2E8F0)
                  ),
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                      selectedLocation = loc
                      showLocationDialog = false
                    }
                    .padding(vertical = 6.dp)
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = loc,
                      fontSize = 13.sp,
                      fontWeight = FontWeight.Bold,
                      color = if (isSelected) Color(0xFFCFB53B) else Color(0xFF4A5568)
                    )
                    if (isSelected) {
                      Text("⭐", fontSize = 12.sp)
                    }
                  }
                }
              }
            }
          }
        }
      }

      // 2. Generate Delivery QR Dialog (Escrow Handshake Release)
      if (showQrDialog != null) {
        val escrowItem = showQrDialog!!
        Dialog(onDismissRequest = { showQrDialog = null }) {
          Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111115)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.5.dp, Color(0xFFFFD54F)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp)
          ) {
            Column(
              modifier = Modifier.padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = "Handshake QR Code 🤝",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "Secure Escrow Transaction Release Code",
                fontSize = 10.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
              )

              Spacer(modifier = Modifier.height(20.dp))

              // Beautiful generated vector QR box container
              Box(
                modifier = Modifier
                  .size(180.dp)
                  .background(Color.White, RoundedCornerShape(16.dp))
                  .padding(14.dp),
                contentAlignment = Alignment.Center
              ) {
                // Let's render a custom styled QR grid mockup in Canvas!
                Canvas(modifier = Modifier.fillMaxSize()) {
                  val cellSize = size.width / 9f
                  // Corner squares
                  drawRect(Color.Black, size = androidx.compose.ui.geometry.Size(cellSize * 3f, cellSize * 3f))
                  drawRect(Color.White, topLeft = Offset(cellSize, cellSize), size = androidx.compose.ui.geometry.Size(cellSize, cellSize))

                  drawRect(Color.Black, topLeft = Offset(size.width - cellSize * 3f, 0f), size = androidx.compose.ui.geometry.Size(cellSize * 3f, cellSize * 3f))
                  drawRect(Color.White, topLeft = Offset(size.width - cellSize * 2f, cellSize), size = androidx.compose.ui.geometry.Size(cellSize, cellSize))

                  drawRect(Color.Black, topLeft = Offset(0f, size.height - cellSize * 3f), size = androidx.compose.ui.geometry.Size(cellSize * 3f, cellSize * 3f))
                  drawRect(Color.White, topLeft = Offset(cellSize, size.height - cellSize * 2f), size = androidx.compose.ui.geometry.Size(cellSize, cellSize))

                  // Some random pixels inside QR
                  val r = java.util.Random(123)
                  for (x in 3..5) {
                    for (y in 0..8) {
                      if (r.nextBoolean()) {
                        drawRect(Color.Black, topLeft = Offset(x * cellSize, y * cellSize), size = androidx.compose.ui.geometry.Size(cellSize, cellSize))
                      }
                    }
                  }
                  for (x in 0..8) {
                    for (y in 3..5) {
                      if (r.nextBoolean()) {
                        drawRect(Color.Black, topLeft = Offset(x * cellSize, y * cellSize), size = androidx.compose.ui.geometry.Size(cellSize, cellSize))
                      }
                    }
                  }
                }
                Box(
                  modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFFFAFAFC), CircleShape)
                    .border(1.dp, Color(0xFFE5E5EA), CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Text("🔒", fontSize = 16.sp)
                }
              }

              Spacer(modifier = Modifier.height(16.dp))

              Text(
                text = escrowItem.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
              )
              Text(
                text = "Value: $currencyPrefix " + String.format("%,.0f", escrowItem.amount),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD54F)
              )

              Spacer(modifier = Modifier.height(20.dp))

              Text(
                text = "Show this QR code to the dispatch driver / trade partner on hand-off. Scanning releases the escrow funds to them instantly.",
                fontSize = 10.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
              )

              Spacer(modifier = Modifier.height(20.dp))

              Button(
                onClick = {
                  onTransitionState(escrowItem.id, com.example.db.EscrowState.COMPLETED, "@kuest_explorer")
                  showQrDialog = null
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text("Complete & Release Escrow", color = Color.Black, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      // 3. Marketplace Direct Message/Chat Dialog
      if (showChatDialog != null) {
        val listing = showChatDialog!!
        var chatInput by remember { mutableStateOf("") }
        var chatMessages by remember(listing) {
          mutableStateOf(
            listOf(
              ChatMessage("Hello there! Is this listing still available? 🧭", true, "10:30"),
              ChatMessage("Yes it is! Proximity is verified. Are you currently in the metropolis?", false, "10:31")
            )
          )
        }

        Dialog(onDismissRequest = { showChatDialog = null }) {
          Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141418)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
            modifier = Modifier
              .fillMaxWidth()
              .height(440.dp)
              .padding(16.dp)
          ) {
            Column(modifier = Modifier.fillMaxSize()) {
              // Chat Header
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(Color(0xFF1D1D22))
                  .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier
                      .size(24.dp)
                      .background(Color(0xFFFFB300), CircleShape),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(text = "✓", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                  }
                  Spacer(modifier = Modifier.width(8.dp))
                  Column {
                    Text(
                      text = listing.verifiedOwner,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color.White
                    )
                    Text(
                      text = listing.title,
                      fontSize = 9.sp,
                      color = Color.LightGray,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }

                IconButton(
                  onClick = { showChatDialog = null },
                  modifier = Modifier.size(28.dp)
                ) {
                  Text(text = "✕", color = Color.Gray, fontSize = 14.sp)
                }
              }

              // Chat Messages Body
              LazyColumn(
                modifier = Modifier
                  .weight(1f)
                  .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                items(chatMessages) { msg ->
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (msg.isFromMe) Arrangement.End else Arrangement.Start
                  ) {
                    Card(
                      colors = CardDefaults.cardColors(
                        containerColor = if (msg.isFromMe) Color(0xFFFFB300) else Color(0xFF2E2E36)
                      ),
                      shape = RoundedCornerShape(12.dp),
                      modifier = Modifier.widthIn(max = 180.dp)
                    ) {
                      Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                          text = msg.text,
                          color = if (msg.isFromMe) Color.Black else Color.White,
                          fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                          text = msg.time,
                          color = if (msg.isFromMe) Color.Black.copy(alpha = 0.5f) else Color.Gray,
                          fontSize = 8.sp,
                          textAlign = TextAlign.End,
                          modifier = Modifier.fillMaxWidth()
                        )
                      }
                    }
                  }
                }
              }

              // Chat Input row
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(Color(0xFF1D1D22))
                  .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF2A2A32), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                  if (chatInput.isEmpty()) {
                    Text(
                      text = "Type message directly...",
                      color = Color.Gray,
                      fontSize = 11.sp
                    )
                  }
                  BasicTextFieldMock(
                    value = chatInput,
                    onValueChange = { chatInput = it },
                    textColor = Color.White,
                    fontSize = 11.sp
                  )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                  modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFFFFB300), RoundedCornerShape(12.dp))
                    .clickable {
                      if (chatInput.isNotBlank()) {
                        val userMsg = ChatMessage(chatInput, true, "10:32")
                        chatMessages = chatMessages + userMsg
                        val typed = chatInput
                        chatInput = ""

                        // Delayed automated handshake reply
                        val reply = ChatMessage(
                          "Awesome! Let's lock this deal in Escrow on the Wallet page so we are both protected. 🔒 No broker fees!",
                          false,
                          "10:32"
                        )
                        chatMessages = chatMessages + reply
                      }
                    },
                  contentAlignment = Alignment.Center
                ) {
                  Text("📤", fontSize = 14.sp)
                }
              }
            }
          }
        }
      }

      // 4. Send Money Dialog
      if (showSendMoneyDialog) {
        var transferAmount by remember { mutableStateOf("") }
        var recipientTag by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showSendMoneyDialog = false }) {
          Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16161A)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp)
          ) {
            Column(modifier = Modifier.padding(20.dp)) {
              Text(
                text = "Transfer Funds 💸",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
              )
              Spacer(modifier = Modifier.height(14.dp))

              Text("Recipient @tag", color = Color.Gray, fontSize = 11.sp)
              Spacer(modifier = Modifier.height(4.dp))
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(Color(0xFF222228), RoundedCornerShape(12.dp))
                  .padding(12.dp)
              ) {
                if (recipientTag.isEmpty()) {
                  Text("@username...", color = Color.Gray, fontSize = 12.sp)
                }
                BasicTextFieldMock(recipientTag, { recipientTag = it }, Color.White, 12.sp)
              }

              Spacer(modifier = Modifier.height(12.dp))

              Text("Amount in $currencyPrefix", color = Color.Gray, fontSize = 11.sp)
              Spacer(modifier = Modifier.height(4.dp))
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(Color(0xFF222228), RoundedCornerShape(12.dp))
                  .padding(12.dp)
              ) {
                if (transferAmount.isEmpty()) {
                  Text("Enter amount...", color = Color.Gray, fontSize = 12.sp)
                }
                BasicTextFieldMock(transferAmount, { transferAmount = it }, Color.White, 12.sp)
              }

              Spacer(modifier = Modifier.height(20.dp))

              Button(
                onClick = {
                  val amt = transferAmount.toDoubleOrNull() ?: 0.0
                  if (amt > 0.0 && amt <= walletBalance) {
                    walletBalance -= amt
                    showSendMoneyDialog = false
                  }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text("Confirm Transfer", color = Color.White, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      // 5. Create Escrow Dialog
      if (showCreateEscrowDialog) {
        var escrowTitle by remember { mutableStateOf("") }
        var escrowRecipient by remember { mutableStateOf("") }
        var escrowAmount by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showCreateEscrowDialog = false }) {
          Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16161A)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp)
          ) {
            Column(modifier = Modifier.padding(20.dp)) {
              Text(
                text = "Lock Escrow Contract 🔒",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
              )
              Spacer(modifier = Modifier.height(14.dp))

              Text("Deal Item/Description", color = Color.Gray, fontSize = 11.sp)
              Spacer(modifier = Modifier.height(4.dp))
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(Color(0xFF222228), RoundedCornerShape(12.dp))
                  .padding(12.dp)
              ) {
                if (escrowTitle.isEmpty()) {
                  Text("e.g., iPhone 13, Delivery order...", color = Color.Gray, fontSize = 12.sp)
                }
                BasicTextFieldMock(escrowTitle, { escrowTitle = it }, Color.White, 12.sp)
              }

              Spacer(modifier = Modifier.height(10.dp))

              Text("Merchant/Partner @tag", color = Color.Gray, fontSize = 11.sp)
              Spacer(modifier = Modifier.height(4.dp))
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(Color(0xFF222228), RoundedCornerShape(12.dp))
                  .padding(12.dp)
              ) {
                if (escrowRecipient.isEmpty()) {
                  Text("@partner...", color = Color.Gray, fontSize = 12.sp)
                }
                BasicTextFieldMock(escrowRecipient, { escrowRecipient = it }, Color.White, 12.sp)
              }

              Spacer(modifier = Modifier.height(10.dp))

              Text("Amount in $currencyPrefix", color = Color.Gray, fontSize = 11.sp)
              Spacer(modifier = Modifier.height(4.dp))
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(Color(0xFF222228), RoundedCornerShape(12.dp))
                  .padding(12.dp)
              ) {
                if (escrowAmount.isEmpty()) {
                  Text("Enter amount...", color = Color.Gray, fontSize = 12.sp)
                }
                BasicTextFieldMock(escrowAmount, { escrowAmount = it }, Color.White, 12.sp)
              }

              Spacer(modifier = Modifier.height(20.dp))

              Button(
                onClick = {
                  val amt = escrowAmount.toDoubleOrNull() ?: 0.0
                  if (amt > 0.0 && amt <= walletBalance && escrowTitle.isNotBlank()) {
                    val newId = System.currentTimeMillis().toString()
                    val merchantTag = if (escrowRecipient.startsWith("@")) escrowRecipient else "@$escrowRecipient"

                    coroutineScope.launch {
                      repository.createDeal(
                        id = newId,
                        buyerId = "@kuest_explorer",
                        sellerId = merchantTag,
                        title = escrowTitle,
                        amount = amt,
                        currency = currencyPrefix,
                        secureHandshakeHash = "hs_$newId",
                        marketListingId = null,
                        chatRoomId = null
                      )
                      onTransitionState(newId, com.example.db.EscrowState.FUNDED, "@kuest_explorer")
                    }
                    showCreateEscrowDialog = false
                  }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9100)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text("Confirm Escrow lock", color = Color.White, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      // 6. Heads-up Notification Banner Overlay
      activeNotificationBanner?.let { banner ->
        LaunchedEffect(banner.id) {
          kotlinx.coroutines.delay(4500)
          if (activeNotificationBanner?.id == banner.id) {
            activeNotificationBanner = null
          }
        }

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter)
            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
            .zIndex(99f)
        ) {
          Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16161A).copy(alpha = 0.95f)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFFF9100).copy(alpha = 0.6f)),
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                currentTab = 2
                activeNotificationBanner = null
                notificationsList = notificationsList.map {
                  if (it.id == banner.id) it.copy(isRead = true) else it
                }
              }
              .shadow(12.dp, RoundedCornerShape(16.dp))
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .background(Color(0xFFFF9100).copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Text(text = "🔔", fontSize = 18.sp)
              }

              Spacer(modifier = Modifier.width(12.dp))

              Column(modifier = Modifier.weight(1f)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = banner.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                  )
                  Text(
                    text = banner.timestamp,
                    fontSize = 9.sp,
                    color = Color.Gray
                  )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = banner.body,
                  fontSize = 11.sp,
                  color = Color.LightGray,
                  maxLines = 2
                )
              }

              Spacer(modifier = Modifier.width(8.dp))

              IconButton(
                onClick = { activeNotificationBanner = null },
                modifier = Modifier.size(24.dp)
              ) {
                Text(text = "✕", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      // 7. Activity Ledger Alerts Drawer (Notification Center)
      if (showNotificationsCenter) {
        Dialog(onDismissRequest = { showNotificationsCenter = false }) {
          Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131316)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 24.dp)
              .shadow(24.dp, RoundedCornerShape(24.dp))
          ) {
            Column(modifier = Modifier.padding(20.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(text = "🔔", fontSize = 20.sp)
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Activity Ledger Alerts",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                  )
                }

                Text(
                  text = "✕",
                  fontSize = 14.sp,
                  color = Color.Gray,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier
                    .clickable { showNotificationsCenter = false }
                    .padding(4.dp)
                )
              }

              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "Real-time updates of local transactions and peer-to-peer escrow operations.",
                fontSize = 10.sp,
                color = Color.Gray
              )

              Spacer(modifier = Modifier.height(14.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                val unreadCount = notificationsList.count { !it.isRead }
                Text(
                  text = "$unreadCount unread notification(s)",
                  fontSize = 11.sp,
                  color = Color(0xFFFFB300),
                  fontWeight = FontWeight.Bold
                )

                if (notificationsList.isNotEmpty()) {
                  Text(
                    text = "Clear All",
                    fontSize = 11.sp,
                    color = Color.Red.copy(alpha = 0.8f),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                      .clickable { notificationsList = emptyList() }
                      .padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              if (notificationsList.isEmpty()) {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(16.dp)),
                  contentAlignment = Alignment.Center
                ) {
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "📭", fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                      text = "Inbox fully settled!",
                      fontSize = 12.sp,
                      color = Color.White,
                      fontWeight = FontWeight.Bold
                    )
                    Text(
                      text = "No pending platform activity logged.",
                      fontSize = 10.sp,
                      color = Color.Gray
                    )
                  }
                }
              } else {
                androidx.compose.foundation.lazy.LazyColumn(
                  modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = 280.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  items(notificationsList) { notif ->
                    val borderHighlightColor = if (!notif.isRead) {
                      Color(0xFFFF9100).copy(alpha = 0.3f)
                    } else {
                      Color.White.copy(alpha = 0.08f)
                    }
                    val itemBg = if (!notif.isRead) {
                      Color(0xFFFF9100).copy(alpha = 0.05f)
                    } else {
                      Color.White.copy(alpha = 0.02f)
                    }

                    Card(
                      colors = CardDefaults.cardColors(containerColor = itemBg),
                      shape = RoundedCornerShape(12.dp),
                      border = BorderStroke(1.dp, borderHighlightColor),
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!notif.isRead) {
                              Box(
                                modifier = Modifier
                                  .size(6.dp)
                                  .background(Color(0xFFFF9100), CircleShape)
                              )
                              Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                              text = notif.title,
                              fontSize = 11.sp,
                              fontWeight = FontWeight.Bold,
                              color = Color.White
                            )
                          }
                          Text(
                            text = notif.timestamp,
                            fontSize = 9.sp,
                            color = Color.Gray
                          )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                          text = notif.body,
                          fontSize = 10.sp,
                          color = Color.LightGray
                        )

                        if (notif.dealId.isNotEmpty()) {
                          Spacer(modifier = Modifier.height(8.dp))
                          Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                          ) {
                            Text(
                              text = "Inspect Secure Ledger 💳",
                              fontSize = 9.sp,
                              fontWeight = FontWeight.Bold,
                              color = Color(0xFFFFD54F),
                              modifier = Modifier
                                .background(Color(0xFFFFD54F).copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                .clickable {
                                  currentTab = 2
                                  showNotificationsCenter = false
                                  notificationsList = notificationsList.map {
                                    if (it.id == notif.id) it.copy(isRead = true) else it
                                  }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                          }
                        } else if (!notif.isRead) {
                          Spacer(modifier = Modifier.height(6.dp))
                          Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                          ) {
                            Text(
                              text = "Mark as Read",
                              fontSize = 9.sp,
                              color = Color.Gray,
                              modifier = Modifier
                                .clickable {
                                  notificationsList = notificationsList.map {
                                    if (it.id == notif.id) it.copy(isRead = true) else it
                                  }
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                          }
                        }
                      }
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(14.dp))

              Button(
                onClick = {
                  notificationsList = notificationsList.map { it.copy(isRead = true) }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222228)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text("Mark All as Read", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }
}

// Interactive custom vector Map overlay showing streets, grids, boundaries, and interactive pins
@Composable
fun InteractiveVectorMap(
  selectedLocation: String,
  posts: List<SocialPost>,
  mapType: String,
  selectedPinInfo: String?,
  onPinClick: (String) -> Unit
) {
  var hoveredPostId by remember { mutableStateOf<String?>(null) }
  var hoveredGeofenceId by remember { mutableStateOf<String?>(null) }

  val pulseAnim = rememberInfiniteTransition(label = "Pulse")
  val pulseScale by pulseAnim.animateFloat(
    initialValue = 0.8f,
    targetValue = 1.3f,
    animationSpec = infiniteRepeatable(
      animation = tween(1500, easing = EaseInOutSine),
      repeatMode = RepeatMode.Reverse
    ),
    label = "PulseScale"
  )

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val widthDp = maxWidth
    val heightDp = maxHeight

    Canvas(modifier = Modifier.fillMaxSize()) {
      when (mapType) {
        "satellite" -> {
          // Deep forest earthy/terrain background
          drawRect(color = Color(0xFF0F140F))

          // Draw some agricultural/urban blocks to simulate sat views
          // Farmland parcels
          drawRect(
            color = Color(0xFF1E2818),
            topLeft = Offset(size.width * 0.1f, size.height * 0.15f),
            size = Size(size.width * 0.35f, size.height * 0.25f)
          )
          drawRect(
            color = Color(0xFF1B2418),
            topLeft = Offset(size.width * 0.55f, size.height * 0.5f),
            size = Size(size.width * 0.4f, size.height * 0.3f)
          )

          // Soft dark green circles representing tree/forest canopies
          drawCircle(
            color = Color(0xFF0C190D),
            radius = 60.dp.toPx(),
            center = Offset(size.width * 0.15f, size.height * 0.75f)
          )
          drawCircle(
            color = Color(0xFF0A160A),
            radius = 100.dp.toPx(),
            center = Offset(size.width * 0.8f, size.height * 0.2f)
          )

          // Custom curved blue river
          drawCircle(
            color = Color(0xFF0D1B2A),
            radius = 120.dp.toPx(),
            center = Offset(size.width * 0.5f, size.height * -0.1f)
          )

          // Dense urban building rooftop clusters
          val roofColor = Color(0xFF42474E)
          val secondRoofColor = Color(0xFF533F3B)
          // Cluster 1
          for (i in 0..6) {
            val rx = size.width * 0.45f + (i * 24f)
            val ry = size.height * 0.4f + (i % 2 * 14f)
            drawRect(
              color = roofColor,
              topLeft = Offset(rx, ry),
              size = Size(16f, 12f)
            )
          }
          // Cluster 2
          for (i in 0..5) {
            val rx = size.width * 0.2f + (i * 20f)
            val ry = size.height * 0.55f + (i % 3 * 10f)
            drawRect(
              color = secondRoofColor,
              topLeft = Offset(rx, ry),
              size = Size(14f, 10f)
            )
          }
        }
        "hybrid" -> {
          // Base Satellite view
          drawRect(color = Color(0xFF0F140F))

          // Farmland parcels
          drawRect(
            color = Color(0xFF1E2818),
            topLeft = Offset(size.width * 0.1f, size.height * 0.15f),
            size = Size(size.width * 0.35f, size.height * 0.25f)
          )
          drawRect(
            color = Color(0xFF1B2418),
            topLeft = Offset(size.width * 0.55f, size.height * 0.5f),
            size = Size(size.width * 0.4f, size.height * 0.3f)
          )

          // Soft dark green circles representing tree/forest canopies
          drawCircle(
            color = Color(0xFF0C190D),
            radius = 60.dp.toPx(),
            center = Offset(size.width * 0.15f, size.height * 0.75f)
          )
          drawCircle(
            color = Color(0xFF0A160A),
            radius = 100.dp.toPx(),
            center = Offset(size.width * 0.8f, size.height * 0.2f)
          )

          // Urban clusters rooftops
          val roofColor = Color(0xFF42474E)
          for (i in 0..6) {
            val rx = size.width * 0.45f + (i * 24f)
            val ry = size.height * 0.4f + (i % 2 * 14f)
            drawRect(
              color = roofColor,
              topLeft = Offset(rx, ry),
              size = Size(16f, 12f)
            )
          }

          // PLUS: Bright glowing hybrid vector streets on top!
          val highwayColor = Color(0xFFFFD54F).copy(alpha = 0.55f)
          val streetColor = Color(0xFF00E5FF).copy(alpha = 0.4f)
          val gridSpacing = 40.dp.toPx()

          // Horizontal streets
          for (y in 0..size.height.toInt() step gridSpacing.toInt()) {
            drawLine(
              color = streetColor,
              start = Offset(0f, y.toFloat()),
              end = Offset(size.width, y.toFloat()),
              strokeWidth = 2.dp.toPx()
            )
          }

          // Vertical streets
          for (x in 0..size.width.toInt() step gridSpacing.toInt()) {
            drawLine(
              color = streetColor,
              start = Offset(x.toFloat(), 0f),
              end = Offset(x.toFloat(), size.height),
              strokeWidth = 2.dp.toPx()
            )
          }

          // Diagonals representing freeways/rail lines in gold highway color
          drawLine(
            color = highwayColor,
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height),
            strokeWidth = 5.dp.toPx()
          )
        }
        else -> {
          // Standard style: Classic modern Dark theme layout
          drawRect(color = Color(0xFF131317))

          // Draw some stylized streets lines representing city map grids
          val streetColor = Color(0xFF1D1D24)
          val gridSpacing = 40.dp.toPx()

          // Horizontal streets
          for (y in 0..size.height.toInt() step gridSpacing.toInt()) {
            drawLine(
              color = streetColor,
              start = Offset(0f, y.toFloat()),
              end = Offset(size.width, y.toFloat()),
              strokeWidth = 4.dp.toPx()
            )
          }

          // Vertical streets
          for (x in 0..size.width.toInt() step gridSpacing.toInt()) {
            drawLine(
              color = streetColor,
              start = Offset(x.toFloat(), 0f),
              end = Offset(x.toFloat(), size.height),
              strokeWidth = 4.dp.toPx()
            )
          }

          // Diagonals representing freeways/rail lines
          drawLine(
            color = Color(0xFF282834),
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height),
            strokeWidth = 6.dp.toPx()
          )

          // Highlight area or central hotspot overlay
          drawCircle(
            color = Color(0xFFFFD54F).copy(alpha = 0.03f),
            radius = 120.dp.toPx(),
            center = Offset(size.width / 2f, size.height / 2f)
          )
        }
      }

      // Draw dynamic pulsing backgrounds for each post pin
      posts.forEach { post ->
        val (rx, ry) = when (post.id) {
          "pulse_ninta" -> 0.30f to 0.40f
          "pulse_mariia" -> 0.60f to 0.30f
          "1" -> 0.35f to 0.45f
          "2" -> 0.65f to 0.32f
          "3" -> 0.50f to 0.70f
          else -> {
            val hashCode = Math.abs(post.id.hashCode())
            val px = 0.2f + ((hashCode % 60) / 100f)
            val py = 0.2f + (((hashCode / 7) % 50) / 100f)
            px to py
          }
        }

        val color = when {
          post.isHighAlert || post.tag.contains("Safety", ignoreCase = true) || post.content.contains("Safety", ignoreCase = true) -> Color(0xFFD32F2F)
          post.tag.contains("Trade", ignoreCase = true) || post.tag.contains("Market", ignoreCase = true) || post.tag.contains("🛍️") || post.content.contains("Trade", ignoreCase = true) -> Color(0xFF388E3C)
          post.tag.contains("Event", ignoreCase = true) || post.tag.contains("🎉") || post.content.contains("Event", ignoreCase = true) -> Color(0xFF1976D2)
          else -> Color(0xFFFFB300)
        }

        drawCircle(
          color = color.copy(alpha = 0.15f * (2f - pulseScale)),
          radius = 24.dp.toPx() * pulseScale,
          center = Offset(size.width * rx, size.height * ry)
        )

        drawCircle(
          color = color,
          radius = 6.dp.toPx(),
          center = Offset(size.width * rx, size.height * ry)
        )
      }

      // Draw neon geofence boundary rings on map
      val isLagos = selectedLocation.contains("Lagos")
      val isJozi = selectedLocation.contains("Johannesburg")
      val geofenceNodes = when {
        isLagos -> listOf(
          Triple("g1", "Ikeja Safe Zone Hub", Offset(0.25f, 0.58f)),
          Triple("g2", "Third Mainland Bridge Guard", Offset(0.52f, 0.42f)),
          Triple("g3", "Eko Exhibition Ground", Offset(0.45f, 0.28f)),
          Triple("g4", "Lekki Toll Gate Guard", Offset(0.78f, 0.72f))
        )
        isJozi -> listOf(
          Triple("g1", "Sandton Central Core", Offset(0.25f, 0.58f)),
          Triple("g2", "Rivonia Safety Loop", Offset(0.52f, 0.42f)),
          Triple("g3", "Rosebank Creator District", Offset(0.45f, 0.28f)),
          Triple("g4", "Gauteng Community Perimeter", Offset(0.78f, 0.72f))
        )
        else -> listOf(
          Triple("g1", "Kilimani Center Hub", Offset(0.25f, 0.58f)),
          Triple("g2", "Yaya Center Margin", Offset(0.52f, 0.42f)),
          Triple("g3", "Westlands Mall Perimeter", Offset(0.45f, 0.28f)),
          Triple("g4", "Jomo Kenyatta Airport Far Guard", Offset(0.78f, 0.72f))
        )
      }

      fun getGeofenceDetails(id: String): Pair<Int, Color> {
        return when (id) {
          "g1" -> 150 to Color(0xFF00E5FF)
          "g2" -> 250 to Color(0xFFFF3D00)
          "g3" -> 300 to Color(0xFFFFD54F)
          else -> 800 to Color(0xFF00E676)
        }
      }

      geofenceNodes.forEach { node ->
        val (id, name, offset) = node
        val (range, gColor) = getGeofenceDetails(id)
        val centerOffset = Offset(size.width * offset.x, size.height * offset.y)
        val radiusPx = (range / 10f).dp.toPx()

        // Outer neon glow ring
        drawCircle(
          color = gColor.copy(alpha = 0.06f * pulseScale),
          radius = radiusPx,
          center = centerOffset
        )
        
        // Dashed geofence perimeter ring
        drawCircle(
          color = gColor.copy(alpha = 0.35f),
          radius = radiusPx,
          center = centerOffset,
          style = Stroke(
            width = 1.5.dp.toPx(),
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
              floatArrayOf(15f, 10f),
              0f
            )
          )
        )

        // Core neon node point
        drawCircle(
          color = gColor,
          radius = 5.dp.toPx(),
          center = centerOffset
        )
        // Pulsing center halo
        drawCircle(
          color = gColor.copy(alpha = 0.25f),
          radius = 12.dp.toPx() * pulseScale,
          center = centerOffset
        )
      }
    }

    val isLagos = selectedLocation.contains("Lagos")
    val isJozi = selectedLocation.contains("Johannesburg")
    val geofenceNodes = when {
      isLagos -> listOf(
        Triple("g1", "Ikeja Safe Zone Hub", Offset(0.25f, 0.58f)),
        Triple("g2", "Third Mainland Bridge Guard", Offset(0.52f, 0.42f)),
        Triple("g3", "Eko Exhibition Ground", Offset(0.45f, 0.28f)),
        Triple("g4", "Lekki Toll Gate Guard", Offset(0.78f, 0.72f))
      )
      isJozi -> listOf(
        Triple("g1", "Sandton Central Core", Offset(0.25f, 0.58f)),
        Triple("g2", "Rivonia Safety Loop", Offset(0.52f, 0.42f)),
        Triple("g3", "Rosebank Creator District", Offset(0.45f, 0.28f)),
        Triple("g4", "Gauteng Community Perimeter", Offset(0.78f, 0.72f))
      )
      else -> listOf(
        Triple("g1", "Kilimani Center Hub", Offset(0.25f, 0.58f)),
        Triple("g2", "Yaya Center Margin", Offset(0.52f, 0.42f)),
        Triple("g3", "Westlands Mall Perimeter", Offset(0.45f, 0.28f)),
        Triple("g4", "Jomo Kenyatta Airport Far Guard", Offset(0.78f, 0.72f))
      )
    }

    fun getGeofenceDetails(id: String): Pair<Int, Color> {
      return when (id) {
        "g1" -> 150 to Color(0xFF00E5FF)
        "g2" -> 250 to Color(0xFFFF3D00)
        "g3" -> 300 to Color(0xFFFFD54F)
        else -> 800 to Color(0xFF00E676)
      }
    }

    fun getGeofenceStatus(id: String): String {
      return when (id) {
        "g2" -> "Triggered"
        "g4" -> "Suppressed"
        else -> "Active"
      }
    }

    // Draw the 3 high-fidelity screenshot overlays for the Pulse Map!
    val showMockOverlays = selectedLocation.contains("Nairobi") || selectedLocation.contains("Kilimani")

    if (showMockOverlays) {
      // 1. Safety Alert Overlay
      val safetyPinX = widthDp * 0.25f
      val safetyPinY = heightDp * 0.32f

      // Draw safety pin (Yellow glowing pin)
      Box(
        modifier = Modifier
          .align(Alignment.TopStart)
          .offset(x = safetyPinX - 18.dp, y = safetyPinY - 18.dp)
          .size(36.dp)
          .shadow(6.dp, CircleShape)
          .clip(CircleShape)
          .background(Color(0xFFFF9100).copy(alpha = 0.2f * pulseScale))
          .border(BorderStroke(1.5.dp, Color(0xFFFF9100)), CircleShape)
          .clickable {
            onPinClick("Ninta Diranner: Welcome to the new Neighbor-hub app of Africa and his healths.")
          },
        contentAlignment = Alignment.Center
      ) {
        Text("🚨", fontSize = 16.sp)
      }

      // Safety card above it
      Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFA111216)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        modifier = Modifier
          .align(Alignment.TopStart)
          .offset(x = (safetyPinX - 70.dp).coerceAtLeast(12.dp), y = safetyPinY - 80.dp)
          .shadow(8.dp, RoundedCornerShape(16.dp))
          .clickable {
            onPinClick("Ninta Diranner: Welcome to the new Neighbor-hub app of Africa and his healths.")
          }
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Yellow glow badge
          Box(
            modifier = Modifier
              .size(28.dp)
              .background(Color(0xFFFF9100).copy(alpha = 0.15f), CircleShape)
              .border(BorderStroke(1.dp, Color(0xFFFF9100).copy(alpha = 0.5f)), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Text("⚠️", fontSize = 12.sp)
          }
          Column {
            Text(
              text = "Safety Alert",
              fontSize = 11.sp,
              fontWeight = FontWeight.Black,
              color = Color.White
            )
            Text(
              text = "Verified by 14 Neighbors",
              fontSize = 9.sp,
              fontWeight = FontWeight.Medium,
              color = Color.LightGray.copy(alpha = 0.7f)
            )
          }
        }
      }

      // 2. Trade Overlay
      val tradePinX = widthDp * 0.68f
      val tradePinY = heightDp * 0.28f

      // Draw trade pin (Mini produce thumbnail)
      Box(
        modifier = Modifier
          .align(Alignment.TopStart)
          .offset(x = tradePinX - 16.dp, y = tradePinY - 16.dp)
          .size(32.dp)
          .shadow(4.dp, CircleShape)
          .clip(CircleShape)
          .background(Color(0xFF388E3C).copy(alpha = 0.2f))
          .border(BorderStroke(1.5.dp, Color(0xFF388E3C)), CircleShape)
          .clickable {
            onPinClick("Fresh Produce Market - Kilimani: Fresh fruits and vegetables available near Yaya Center.")
          },
        contentAlignment = Alignment.Center
      ) {
        Text("🥦", fontSize = 14.sp)
      }

      // Trade Card above it
      Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFA111216)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        modifier = Modifier
          .align(Alignment.TopStart)
          .offset(x = (tradePinX - 110.dp).coerceAtMost(widthDp - 210.dp), y = tradePinY - 76.dp)
          .shadow(8.dp, RoundedCornerShape(16.dp))
          .clickable {
            onPinClick("Fresh Produce Market - Kilimani: Fresh fruits and vegetables available near Yaya Center.")
          }
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Fresh veggies miniature thumbnail
          Box(
            modifier = Modifier
              .size(28.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(Color(0xFF1E222B)),
            contentAlignment = Alignment.Center
          ) {
            Text("🍅🥦", fontSize = 11.sp)
          }
          Column {
            Text(
              text = "Trade",
              fontSize = 11.sp,
              fontWeight = FontWeight.Black,
              color = Color.White
            )
            Text(
              text = "Fresh Produce Market -\nKilimani (200m away)",
              fontSize = 9.sp,
              fontWeight = FontWeight.Medium,
              color = Color.LightGray.copy(alpha = 0.7f),
              lineHeight = 11.sp
            )
          }
        }
      }

      // 3. Live Music Event (Pulsing concentric rings)
      val eventX = widthDp * 0.65f
      val eventY = heightDp * 0.48f

      Box(
        modifier = Modifier
          .align(Alignment.TopStart)
          .offset(x = eventX - 110.dp, y = eventY - 110.dp)
          .size(220.dp),
        contentAlignment = Alignment.Center
      ) {
        // Concentric ring 1
        Box(
          modifier = Modifier
            .size(110.dp * pulseScale)
            .background(Color(0xFF1976D2).copy(alpha = 0.05f * (1.5f - pulseScale)), CircleShape)
            .border(BorderStroke(1.dp, Color(0xFF1976D2).copy(alpha = 0.2f * (1.5f - pulseScale))), CircleShape)
        )
        // Concentric ring 2
        Box(
          modifier = Modifier
            .size(160.dp * pulseScale)
            .background(Color(0xFF1976D2).copy(alpha = 0.03f * (1.5f - pulseScale)), CircleShape)
            .border(BorderStroke(1.dp, Color(0xFF1976D2).copy(alpha = 0.15f * (1.5f - pulseScale))), CircleShape)
        )
        // Concentric ring 3
        Box(
          modifier = Modifier
            .size(220.dp * pulseScale)
            .background(Color(0xFF1976D2).copy(alpha = 0.015f * (1.5f - pulseScale)), CircleShape)
            .border(BorderStroke(1.dp, Color(0xFF1976D2).copy(alpha = 0.1f * (1.5f - pulseScale))), CircleShape)
        )

        // Pulsing core node
        Box(
          modifier = Modifier
            .size(10.dp)
            .background(Color(0xFF1976D2), CircleShape)
        )
      }

      // Event callout pill on top
      Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFA111216)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.15f)),
        modifier = Modifier
          .align(Alignment.TopStart)
          .offset(x = eventX - 85.dp, y = eventY - 24.dp)
          .shadow(8.dp, RoundedCornerShape(20.dp))
          .clickable {
            onPinClick("Acoustic Sunset: Live Music Event starting at 6 PM! RSVP now!")
          }
      ) {
        Column(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Text(text = "((•))", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE040FB))
            Text(
              text = "Live Music Event",
              fontSize = 11.sp,
              fontWeight = FontWeight.Black,
              color = Color.White
            )
          }
          Text(
            text = "( RSVP Now )",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color.LightGray.copy(alpha = 0.8f)
          )
        }
      }
    } else {
      // Fallback: standard post markers
      posts.forEach { post ->
        val (rx, ry) = when (post.id) {
          "pulse_ninta" -> 0.30f to 0.40f
          "pulse_mariia" -> 0.60f to 0.30f
          "1" -> 0.35f to 0.45f
          "2" -> 0.65f to 0.32f
          "3" -> 0.50f to 0.70f
          else -> {
            val hashCode = Math.abs(post.id.hashCode())
            val px = 0.2f + ((hashCode % 60) / 100f)
            val py = 0.2f + (((hashCode / 7) % 50) / 100f)
            px to py
          }
        }

        val color = when {
          post.isHighAlert || post.tag.contains("Safety", ignoreCase = true) || post.content.contains("Safety", ignoreCase = true) -> Color(0xFFD32F2F)
          post.tag.contains("Trade", ignoreCase = true) || post.tag.contains("Market", ignoreCase = true) || post.tag.contains("🛍️") || post.content.contains("Trade", ignoreCase = true) -> Color(0xFF388E3C)
          post.tag.contains("Event", ignoreCase = true) || post.tag.contains("🎉") || post.content.contains("Event", ignoreCase = true) -> Color(0xFF1976D2)
          else -> Color(0xFFFFB300)
        }

        val tagText = when {
          post.isHighAlert || post.tag.contains("Safety", ignoreCase = true) || post.content.contains("Safety", ignoreCase = true) -> "🚨 Safety Alert"
          post.tag.contains("Trade", ignoreCase = true) || post.tag.contains("Market", ignoreCase = true) || post.tag.contains("🛍️") || post.content.contains("Trade", ignoreCase = true) -> "🛍️ Trade Active"
          post.tag.contains("Event", ignoreCase = true) || post.tag.contains("🎉") || post.content.contains("Event", ignoreCase = true) -> "🎉 Event Live"
          else -> "📍 Info"
        }

        Box(
          modifier = Modifier
            .align(Alignment.TopStart)
            .offset(
              x = widthDp * rx - 48.dp,
              y = heightDp * ry - 22.dp
            )
            .pointerInput(post.id) {
              awaitPointerEventScope {
                while (true) {
                  val event = awaitPointerEvent()
                  when (event.type) {
                    PointerEventType.Enter -> {
                      hoveredPostId = post.id
                    }
                    PointerEventType.Exit -> {
                      if (hoveredPostId == post.id) {
                        hoveredPostId = null
                      }
                    }
                  }
                }
              }
            }
            .clickable {
              hoveredPostId = if (hoveredPostId == post.id) null else post.id
              onPinClick("${post.author}: ${post.content}")
            }
            .clip(RoundedCornerShape(10.dp))
            .background(color)
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(tagText, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
      }
    }

    // Interactive invisible hover regions for the neon geofence nodes
    geofenceNodes.forEach { node ->
      val (id, name, offset) = node
      Box(
        modifier = Modifier
          .align(Alignment.TopStart)
          .offset(
            x = widthDp * offset.x - 20.dp,
            y = heightDp * offset.y - 20.dp
          )
          .size(40.dp)
          .pointerInput(id) {
            awaitPointerEventScope {
              while (true) {
                val event = awaitPointerEvent()
                when (event.type) {
                  PointerEventType.Enter -> {
                    hoveredGeofenceId = id
                  }
                  PointerEventType.Exit -> {
                    if (hoveredGeofenceId == id) {
                      hoveredGeofenceId = null
                    }
                  }
                }
              }
            }
          }
          .clickable {
            hoveredGeofenceId = if (hoveredGeofenceId == id) null else id
          }
      )
    }

    // Render hover card for active post pins
    posts.forEach { post ->
      if (hoveredPostId == post.id) {
        val (rx, ry) = when (post.id) {
          "pulse_ninta" -> 0.30f to 0.40f
          "pulse_mariia" -> 0.60f to 0.30f
          "1" -> 0.35f to 0.45f
          "2" -> 0.65f to 0.32f
          "3" -> 0.50f to 0.70f
          else -> {
            val hashCode = Math.abs(post.id.hashCode())
            val px = 0.2f + ((hashCode % 60) / 100f)
            val py = 0.2f + (((hashCode / 7) % 50) / 100f)
            px to py
          }
        }

        val color = when {
          post.isHighAlert || post.tag.contains("Safety", ignoreCase = true) || post.content.contains("Safety", ignoreCase = true) -> Color(0xFFD32F2F)
          post.tag.contains("Trade", ignoreCase = true) || post.tag.contains("Market", ignoreCase = true) || post.tag.contains("🛍️") || post.content.contains("Trade", ignoreCase = true) -> Color(0xFF388E3C)
          post.tag.contains("Event", ignoreCase = true) || post.tag.contains("🎉") || post.content.contains("Event", ignoreCase = true) -> Color(0xFF1976D2)
          else -> Color(0xFFFFB300)
        }

        val tagText = when {
          post.isHighAlert || post.tag.contains("Safety", ignoreCase = true) || post.content.contains("Safety", ignoreCase = true) -> "🚨 Safety Alert"
          post.tag.contains("Trade", ignoreCase = true) || post.tag.contains("Market", ignoreCase = true) || post.tag.contains("🛍️") || post.content.contains("Trade", ignoreCase = true) -> "🛍️ Trade Active"
          post.tag.contains("Event", ignoreCase = true) || post.tag.contains("🎉") || post.content.contains("Event", ignoreCase = true) -> "🎉 Event Live"
          else -> "📍 Info"
        }

        val locationName = when (post.id) {
          "pulse_ninta" -> "Kilimani Sector Alpha"
          "pulse_mariia" -> "Yaya Central Boundary"
          "1" -> "Ring Road Roundabout"
          "2" -> "Kilimani Residential Zone"
          "3" -> "Sunset Rooftop Zone"
          else -> "Kuest District Node"
        }

        val distanceInKm = Math.sqrt(((rx - 0.5f) * (rx - 0.5f) + (ry - 0.5f) * (ry - 0.5f)).toDouble()) * 4.2
        val formattedDistance = String.format("%.1f km away", distanceInKm)

        val cardX = ((widthDp * rx) - 100.dp).coerceIn(10.dp, widthDp - 210.dp)
        val cardY = ((heightDp * ry) - 145.dp).coerceIn(10.dp, heightDp - 140.dp)

        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFA141419)),
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.5.dp, color.copy(alpha = 0.9f)),
          modifier = Modifier
            .align(Alignment.TopStart)
            .offset(x = cardX, y = cardY)
            .width(200.dp)
            .shadow(16.dp, RoundedCornerShape(12.dp))
            .zIndex(20f)
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = tagText,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                color = color,
                modifier = Modifier
                  .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                  .padding(horizontal = 4.dp, vertical = 2.dp)
              )
              Text(
                text = formattedDistance,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = Color.LightGray
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = locationName,
              fontSize = 11.sp,
              fontWeight = FontWeight.Black,
              color = Color.White
            )
            Text(
              text = "By ${post.author}",
              fontSize = 9.sp,
              fontWeight = FontWeight.Medium,
              color = Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = post.content,
              fontSize = 9.sp,
              color = Color.LightGray.copy(alpha = 0.9f),
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
              lineHeight = 11.sp
            )
          }
        }
      }
    }

    // Render hover card for geofence nodes
    geofenceNodes.forEach { node ->
      val (id, name, offset) = node
      if (hoveredGeofenceId == id) {
        val (range, gColor) = getGeofenceDetails(id)
        val status = getGeofenceStatus(id)
        val nodeDistance = when (id) {
          "g1" -> "0.4 km"
          "g2" -> "0.9 km"
          "g3" -> "2.1 km"
          "g4" -> "14.5 km"
          else -> "1.2 km"
        }

        val cardX = ((widthDp * offset.x) - 100.dp).coerceIn(10.dp, widthDp - 210.dp)
        val cardY = ((heightDp * offset.y) - 145.dp).coerceIn(10.dp, heightDp - 140.dp)

        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFA0B0E14)),
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.5.dp, gColor), // Glowing neon border
          modifier = Modifier
            .align(Alignment.TopStart)
            .offset(x = cardX, y = cardY)
            .width(200.dp)
            .shadow(16.dp, RoundedCornerShape(12.dp))
            .zIndex(30f)
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "🛡️ GEOFENCE NODE",
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                color = gColor,
                modifier = Modifier
                  .background(gColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                  .padding(horizontal = 4.dp, vertical = 2.dp)
              )
              Text(
                text = status.uppercase(),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = when (status) {
                  "Triggered" -> Color(0xFFFF3D00)
                  "Suppressed" -> Color.Gray
                  else -> Color(0xFF00E676)
                }
              )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
              text = name,
              fontSize = 11.sp,
              fontWeight = FontWeight.Black,
              color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text("RADIUS", fontSize = 7.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Text("${range}m perimeter", fontSize = 9.sp, color = Color.LightGray)
              }
              Column(horizontalAlignment = Alignment.End) {
                Text("DISTANCE", fontSize = 7.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Text(nodeDistance, fontSize = 9.sp, color = Color.LightGray, fontWeight = FontWeight.Bold)
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
              text = when (status) {
                "Triggered" -> "⚠️ Alert active: entering zone initiates automated local OS ledger security check."
                "Suppressed" -> "💤 Bypassed: geofence reporting inactive to optimize device performance."
                else -> "🟢 Protected: low-latency local telemetry tracking fully operational."
              },
              fontSize = 8.sp,
              color = Color.LightGray.copy(alpha = 0.8f),
              lineHeight = 10.sp
            )
          }
        }
      }
    }
  }
}

// Social feed post card
@Composable
fun SocialPostCard(post: SocialPost, onFireClick: (String) -> Unit = {}) {
  val borderStroke = if (post.isHighAlert) {
    BorderStroke(1.5.dp, Color(0xFFD32F2F).copy(alpha = 0.5f))
  } else {
    BorderStroke(1.dp, Color(0xFFE5E5EA))
  }

  Card(
    colors = CardDefaults.cardColors(containerColor = Color.White),
    shape = RoundedCornerShape(16.dp),
    border = borderStroke,
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(28.dp)
              .background(Color(0xFFF2F2F7), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Text(text = "👤", fontSize = 14.sp)
          }
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = post.author,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = Color.Black
            )
            Text(
              text = post.timeAgo,
              fontSize = 10.sp,
              color = Color.Gray
            )
          }
        }

        // Tag Badge
        val badgeColor = when {
          post.tag.contains("Safety") -> Color(0xFFFFEBEE)
          post.tag.contains("Trade") -> Color(0xFFE8F5E9)
          else -> Color(0xFFE3F2FD)
        }
        val badgeTextColor = when {
          post.tag.contains("Safety") -> Color(0xFFC62828)
          post.tag.contains("Trade") -> Color(0xFF2E7D32)
          else -> Color(0xFF1565C0)
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(badgeColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = post.tag,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = badgeTextColor
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = post.content,
        fontSize = 12.sp,
        color = Color(0xFF2A2A2F),
        lineHeight = 16.sp
      )

      if (post.tags.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          post.tags.forEach { tagLabel ->
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFFBEBEB))
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(
                text = tagLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFD32F2F)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      HorizontalDivider(color = Color(0xFFF2F2F7))

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
          // Interactive Upvote Fire Button
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xFFFFF8E1))
              .clickable { onFireClick(post.id) }
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text(text = "🔥", fontSize = 13.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = post.fireCount.toString(),
              fontSize = 11.sp,
              color = Color(0xFFE65100),
              fontWeight = FontWeight.ExtraBold
            )
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
          ) {
            Text(text = "💬", fontSize = 13.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = post.commentsCount.toString(), fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
          }
        }

        Text(
          text = "Share ↗",
          fontSize = 10.sp,
          color = Color.Gray,
          fontWeight = FontWeight.Bold,
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable { /* Share mock trigger */ }
            .padding(4.dp)
        )
      }
    }
  }
}

// Fintech active escrow row item with progress indicator track
@Composable
fun EscrowRowItem(
  item: EscrowItem,
  currencyPrefix: String,
  onGenerateQr: () -> Unit
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = Color(0xFF16161C)),
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = item.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Seller: ${item.merchant}",
            fontSize = 10.sp,
            color = Color.Gray
          )
        }

        Text(
          text = "$currencyPrefix " + String.format("%,.0f", item.amount),
          fontSize = 15.sp,
          fontWeight = FontWeight.Black,
          color = Color(0xFFFFD54F)
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Custom 3-Step Progress Indicator track
      val progress = when (item.status) {
        "Funds Held" -> 1
        "In Transit" -> 2
        "Delivered" -> 3
        else -> 1
      }

      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Funds Locked 🔒",
            fontSize = 9.sp,
            color = if (progress >= 1) Color(0xFFFFB300) else Color.Gray,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "In Transit 🚚",
            fontSize = 9.sp,
            color = if (progress >= 2) Color(0xFFFFB300) else Color.Gray,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Completed ✓",
            fontSize = 9.sp,
            color = if (progress >= 3) Color(0xFF00C853) else Color.Gray,
            fontWeight = FontWeight.Bold
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Draw track layout
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(Color(0xFF22222A), RoundedCornerShape(3.dp)),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Progress segment 1
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .background(
                if (progress >= 1) Color(0xFFFFB300) else Color.Transparent,
                RoundedCornerShape(3.dp)
              )
          )
          // Divider
          Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color(0xFF16161C)))
          // Progress segment 2
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .background(
                if (progress >= 2) Color(0xFFFFB300) else Color.Transparent
              )
          )
          // Divider
          Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color(0xFF16161C)))
          // Progress segment 3
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .background(
                if (progress >= 3) Color(0xFF00C853) else Color.Transparent,
                RoundedCornerShape(3.dp)
              )
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Status Badge text
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF22222B))
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = item.badgeText,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (progress == 3) Color(0xFF00C853) else Color(0xFFFFB300)
          )
        }

        // Generate Delivery QR
        Button(
          onClick = onGenerateQr,
          colors = ButtonDefaults.buttonColors(
            containerColor = if (progress == 3) Color(0xFF1E1E24) else Color(0xFFFFB300)
          ),
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
          modifier = Modifier.height(28.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "📱 ", fontSize = 10.sp)
            Text(
              text = if (progress == 3) "View Ledger" else "Handshake QR",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = if (progress == 3) Color.White else Color.Black
            )
          }
        }
      }
    }
  }
}

// Zero-Broker Marketplace Card Component
@Composable
fun MarketplaceCard(
  item: MarketplaceItem,
  currencyPrefix: String,
  onMessageClick: () -> Unit
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = Color(0xFF111216)), // Surface Container
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
    modifier = Modifier
      .fillMaxWidth()
      .wrapContentHeight()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)
    ) {
      // Image aspect-ratio container: 120x120px with visual placeholder
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(120.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(
            Brush.linearGradient(
              colors = if (item.tag.contains("SPACE")) {
                listOf(Color(0xFF002233), Color(0xFF005577)) // Cool space layout gradient
              } else {
                listOf(Color(0xFF221100), Color(0xFF553311)) // Local culinary layout gradient
              }
            )
          )
      ) {
        // Locator badge overlay on image (e.g. "400m away")
        Box(
          modifier = Modifier
            .padding(6.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Black.copy(alpha = 0.75f))
            .padding(horizontal = 6.dp, vertical = 3.dp)
            .align(Alignment.TopStart)
        ) {
          Text(
            text = item.distance,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontFamily = FontFamily.SansSerif
          )
        }

        // Tag indicator top-right overlay
        Box(
          modifier = Modifier
            .padding(6.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Black.copy(alpha = 0.75f))
            .padding(horizontal = 6.dp, vertical = 3.dp)
            .align(Alignment.TopEnd)
        ) {
          Text(
            text = item.tag,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00E5FF),
            fontFamily = FontFamily.Monospace
          )
        }

        // Center visual placeholder icon representation
        Text(
          text = if (item.tag.contains("SPACE")) "🏠" else "🍳",
          fontSize = 42.sp,
          modifier = Modifier.align(Alignment.Center)
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Texts: Title and Verified Seller
      Text(
        text = item.title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        fontFamily = FontFamily.SansSerif,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      
      Spacer(modifier = Modifier.height(4.dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(6.dp)
            .background(Color(0xFF00E676), CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "✓ Verified Seller",
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFF00E676), // Mint green
          fontFamily = FontFamily.SansSerif
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Footer Flex Row: price and Message button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "$currencyPrefix " + String.format("%,.0f", item.price),
          fontSize = 13.sp,
          fontWeight = FontWeight.Black,
          color = Color(0xFFFFB800), // Terminal Amber
          fontFamily = FontFamily.Monospace
        )

        Button(
          onClick = onMessageClick,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB800)), // Yellow text action
          shape = RoundedCornerShape(6.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
          modifier = Modifier.height(26.dp)
        ) {
          Text(
            text = "Message",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF08080A),
            fontFamily = FontFamily.SansSerif
          )
        }
      }
    }
  }
}

// ==================== STATE-MACHINE CHATS & NEGOTIATIONS ====================
@Composable
fun ChatsScreen(
  currencyPrefix: String,
  activeListingInChat: MarketplaceItem?,
  onActiveListingChange: (MarketplaceItem?) -> Unit,
  onLockEscrowClick: (MarketplaceItem) -> Unit,
  onBack: () -> Unit
) {
  var activeInboxTab by remember { mutableStateOf("negotiations") }
  var rsvpFlashSwarm by remember { mutableStateOf(false) }
  var claimedCampusPass by remember { mutableStateOf(false) }
  var claimedAvatar by remember { mutableStateOf(false) }
  var localToastMessage by remember { mutableStateOf<String?>(null) }

  // Auto-dismiss local toast after 3 seconds
  LaunchedEffect(localToastMessage) {
    if (localToastMessage != null) {
      kotlinx.coroutines.delay(3000)
      localToastMessage = null
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp)
  ) {
    // Chat Room header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = if (activeListingInChat != null) "Negotiations 💬" else if (activeInboxTab == "negotiations") "Inbox & Chats 📥" else "Promotions Hub 🎁",
          fontSize = 20.sp,
          fontWeight = FontWeight.Black,
          color = Color.White
        )
        Text(
          text = if (activeListingInChat != null) "Secure local transactions via verified escrow accounts." else if (activeInboxTab == "negotiations") "Discuss trade with vendors in real-time." else "Track your active city/campus rewards and time-locked promotions.",
          fontSize = 11.sp,
          color = Color.Gray
        )
      }

      IconButton(
        onClick = onBack,
        modifier = Modifier
          .size(36.dp)
          .background(Color(0xFF1E1E24), CircleShape)
      ) {
        Text(text = "🏠", fontSize = 16.sp)
      }
    }

    // Local Toast Message feedback HUD
    if (localToastMessage != null) {
      Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 12.dp)
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text("⚡", fontSize = 16.sp)
          Text(
            text = localToastMessage ?: "",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
          )
          Text(
            text = "✕",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp,
            modifier = Modifier.clickable { localToastMessage = null }
          )
        }
      }
    }

    if (activeListingInChat == null) {
      // Sub-tabs: Conversations vs Promotions
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 16.dp)
          .background(Color(0xFF121216), RoundedCornerShape(10.dp))
          .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
          .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        listOf("negotiations" to "💬 Conversations", "promotions" to "🎁 Promo Hub").forEach { (tabId, label) ->
          val isActive = activeInboxTab == tabId
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(8.dp))
              .background(if (isActive) Color(0xFFFFB300) else Color.Transparent)
              .clickable { activeInboxTab = tabId }
              .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Text(
                text = label,
                color = if (isActive) Color.Black else Color.LightGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
              if (tabId == "promotions" && !rsvpFlashSwarm) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                  modifier = Modifier
                    .size(6.dp)
                    .background(Color(0xFFE53935), CircleShape)
                )
              }
            }
          }
        }
      }

      if (activeInboxTab == "negotiations") {
        // No listing selected: Show active chat channels
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF16161A), RoundedCornerShape(20.dp))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .padding(24.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🧭", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "No Active Negotiations Yet",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Go to the Market tab and message a vendor to start a verified zero-broker escrow deal safely.",
              fontSize = 11.sp,
              color = Color.Gray,
              textAlign = TextAlign.Center,
              lineHeight = 16.sp
            )
          }
        }
      } else {
        // Render promotions hub
        Column(
          modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          // Promo Card 1: Flash Swarm Bonus (Transferred from Discover Screen)
          Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C1014)),
            border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Text("🎟️", fontSize = 18.sp)
                  Text(
                    text = "FLASH SWARM BONUS ACTIVE",
                    color = Color(0xFFE53935),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                  )
                }
                Box(
                  modifier = Modifier
                    .background(Color(0xFFE53935), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                  Text(
                    text = if (rsvpFlashSwarm) "CLAIMED" else "14:52 MINS LEFT",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              Text(
                text = "RSVP for 'Sauti Sol Tribute' or 'Chefs Table' in the next 15 minutes to secure a complimentary premium drink voucher & +500 XP bonus!",
                color = Color.LightGray,
                fontSize = 11.sp,
                lineHeight = 16.sp
              )

              Spacer(modifier = Modifier.height(12.dp))

              if (!rsvpFlashSwarm) {
                Button(
                  onClick = {
                    rsvpFlashSwarm = true
                    localToastMessage = "✓ RSVP Successful! Complimentary premium drink voucher unlocked in Wallet."
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.fillMaxWidth().height(36.dp)
                ) {
                  Text("RSVP Now & Unlock Voucher", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
              } else {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E24), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF00C853).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✓ RSVP & VIP Voucher Confirmed", color = Color(0xFF00C853), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("COUPON: SWARM_SAUTI_DRINK_77", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                  }
                }
              }
            }
          }

          // Promo Card 2: Campus Active Streak Pass
          Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1A14)),
            border = BorderStroke(1.dp, Color(0xFFF4B942).copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Text("🎓", fontSize = 18.sp)
                  Text(
                    text = "CAMPUS CLUB EXCLUSIVE",
                    color = Color(0xFFF4B942),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                  )
                }
                Box(
                  modifier = Modifier
                    .background(Color(0xFFF4B942).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .border(1.dp, Color(0xFFF4B942), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                  Text(
                    text = "ACTIVE STREAK REWARD",
                    color = Color(0xFFF4B942),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              Text(
                text = "Unbelievable value! You have hit an 8-day streak. Claim your free all-access entry pass to the upcoming Uni Tech & Startup Summit in Westlands.",
                color = Color.LightGray,
                fontSize = 11.sp,
                lineHeight = 16.sp
              )

              Spacer(modifier = Modifier.height(12.dp))

              if (!claimedCampusPass) {
                Button(
                  onClick = {
                    claimedCampusPass = true
                    localToastMessage = "✓ Tech Summit Pass claimed successfully! Access key generated."
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF4B942)),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.fillMaxWidth().height(36.dp)
                ) {
                  Text("Claim Summit Pass", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
              } else {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E24), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF00C853).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✓ Summit Ticket Secured", color = Color(0xFF00C853), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("TICKET ID: TECH-SUMMIT-552A-3D", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                  }
                }
              }
            }
          }

          // Promo Card 3: Chronos Avatar Frame
          Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF120C1F)),
            border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Text("📸", fontSize = 18.sp)
                  Text(
                    text = "CHRONOS AVATAR DECORATION",
                    color = Color(0xFFC4B5FD),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                  )
                }
                Box(
                  modifier = Modifier
                    .background(Color(0xFF8B5CF6).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                  Text(
                    text = "CREATOR DROP",
                    color = Color(0xFFC4B5FD),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              Text(
                text = "Unveiling the official KUEST Chronos Digital Avatar Frame. Connect your active selfie cryptographic hash to verify your ticket and unlock custom avatar rings.",
                color = Color.LightGray,
                fontSize = 11.sp,
                lineHeight = 16.sp
              )

              Spacer(modifier = Modifier.height(12.dp))

              if (!claimedAvatar) {
                Button(
                  onClick = {
                    claimedAvatar = true
                    localToastMessage = "✓ Avatar Frame claimed! Applied to @kuest_explorer profile."
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.fillMaxWidth().height(36.dp)
                ) {
                  Text("Unlock Avatar Frame", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
              } else {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E24), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF00C853).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✓ Avatar Frame Verified & Activated", color = Color(0xFF00C853), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("FRAME HASH: hs_77d8f99a382e", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                  }
                }
              }
            }
          }
        }
      }
    } else {
      // Selected active negotiation channel
      val listing = activeListingInChat
      var chatInput by remember { mutableStateOf("") }
      var chatMessages by remember(listing) {
        mutableStateOf(
          listOf(
            ChatMessage("Hi! I found your listing: **${listing.title}** for ${currencyPrefix} ${String.format("%,.0f", listing.price)}. Is it still available? 🧭", true, "10:30"),
            ChatMessage("Habari! Yes, it is 100% available and proximity verified. I'm currently near ${listing.distance}. Let's lock this in Escrow so we are both secured! 🔒", false, "10:31"),
            ChatMessage("Perfect! How does physical delivery/handshake work?", true, "10:32"),
            ChatMessage("Once you click '🔒 Lock Escrow' below and confirm payment, the funds will be locked in platform custody. I'll dispatch my rider. When they arrive, scan my QR code on delivery to release the funds! Simple & 100% scam-free.", false, "10:32")
          )
        )
      }

      Column(modifier = Modifier.fillMaxSize()) {
        // Product Context Card Pinned at the top
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .background(Color(0xFFFFB300), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
              ) {
                Text(text = if (listing.tag.contains("SPACE")) "🏠" else "🍳", fontSize = 18.sp)
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = listing.title,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier
                      .size(6.dp)
                      .background(Color(0xFF00C853), CircleShape)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "Verified Seller ${listing.verifiedOwner} • ${listing.distance}",
                    fontSize = 9.sp,
                    color = Color.LightGray
                  )
                }
              }
            }

            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = "${currencyPrefix} " + String.format("%,.0f", listing.price),
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFFD54F)
              )
              Text(
                text = "0% Broker Fees",
                fontSize = 8.sp,
                color = Color.Gray
              )
            }
          }
        }

        // Chat Messages Bubble List
        LazyColumn(
          modifier = Modifier
            .weight(1f)
            .background(Color(0xFF141418), RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(chatMessages) { msg ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = if (msg.isFromMe) Arrangement.End else Arrangement.Start
            ) {
              Card(
                colors = CardDefaults.cardColors(
                  containerColor = if (msg.isFromMe) Color(0xFFFFB300) else Color(0xFF2E2E36)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.widthIn(max = 240.dp)
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Text(
                    text = msg.text,
                    color = if (msg.isFromMe) Color.Black else Color.White,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = msg.time,
                    color = if (msg.isFromMe) Color.Black.copy(alpha = 0.5f) else Color.Gray,
                    fontSize = 8.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Chat action bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .weight(1f)
              .background(Color(0xFF1E1E24), RoundedCornerShape(12.dp))
              .padding(horizontal = 12.dp, vertical = 10.dp)
          ) {
            if (chatInput.isEmpty()) {
              Text(
                text = "Reply to vendor...",
                color = Color.Gray,
                fontSize = 11.sp
              )
            }
            BasicTextFieldMock(
              value = chatInput,
              onValueChange = { chatInput = it },
              textColor = Color.White,
              fontSize = 11.sp
            )
          }

          Spacer(modifier = Modifier.width(8.dp))

          Box(
            modifier = Modifier
              .size(38.dp)
              .background(Color(0xFFFFB300), RoundedCornerShape(12.dp))
              .clickable {
                if (chatInput.isNotBlank()) {
                  val userMsg = ChatMessage(chatInput, true, "10:33")
                  chatMessages = chatMessages + userMsg
                  chatInput = ""
                }
              },
            contentAlignment = Alignment.Center
          ) {
            Text("📤", fontSize = 14.sp)
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Giant interactive floating action button to Lock Escrow
        Button(
          onClick = { onLockEscrowClick(listing) },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9100)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🔒 ", fontSize = 14.sp)
            Text(
              text = "Lock Escrow Contract • ${currencyPrefix} ${String.format("%,.0f", listing.price)}",
              fontSize = 12.sp,
              fontWeight = FontWeight.Black,
              color = Color.White
            )
          }
        }
      }
    }
  }
}

// ==================== KUEST COFFER & ADVENTURE TREASURY ====================
@Composable
fun WalletScreen(
  currencyPrefix: String,
  selectedLocation: String,
  walletBalance: Double,
  onWalletBalanceChange: (Double) -> Unit,
  walletEscrowLocked: Double,
  onWalletEscrowLockedChange: (Double) -> Unit,
  sellerPendingPayouts: Double,
  onSellerPendingPayoutsChange: (Double) -> Unit,
  sellerClearedEarnings: Double,
  onSellerClearedEarningsChange: (Double) -> Unit,
  isSellerMode: Boolean,
  onIsSellerModeChange: (Boolean) -> Unit,
  activeEscrowInCheckout: EscrowItem?,
  onActiveEscrowInCheckoutChange: (EscrowItem?) -> Unit,
  escrowItems: List<EscrowItem>,
  onConfirmCheckout: (EscrowItem) -> Unit,
  onDispatchClick: (EscrowItem) -> Unit,
  onTransitionState: (String, com.example.db.EscrowState, String) -> Unit,
  getLogsForDeal: (String) -> kotlinx.coroutines.flow.Flow<List<com.example.db.EscrowStateLog>>,
  onGenerateQrClick: (EscrowItem) -> Unit,
  onScanReceiptClick: (EscrowItem) -> Unit,
  onSendMoneyClick: () -> Unit,
  onCustomEscrowClick: () -> Unit,
  onSuccessMessage: (String) -> Unit,
  onLaunchTelemetryHud: (EscrowItem) -> Unit,
  onBack: () -> Unit,
  isPrivacyMode: Boolean = false,
  onIsPrivacyModeChange: (Boolean) -> Unit = {}
) {
  KuestMpesaRegistryWalletScreen(
    currencyPrefix = currencyPrefix,
    selectedLocation = selectedLocation,
    isSellerMode = isSellerMode,
    onIsSellerModeChange = onIsSellerModeChange,
    onSuccessMessage = onSuccessMessage,
    onBack = onBack
  )
}

@Composable
fun LegacyWalletScreen(
  currencyPrefix: String,
  selectedLocation: String,
  walletBalance: Double,
  onWalletBalanceChange: (Double) -> Unit,
  walletEscrowLocked: Double,
  onWalletEscrowLockedChange: (Double) -> Unit,
  sellerPendingPayouts: Double,
  onSellerPendingPayoutsChange: (Double) -> Unit,
  sellerClearedEarnings: Double,
  onSellerClearedEarningsChange: (Double) -> Unit,
  isSellerMode: Boolean,
  onIsSellerModeChange: (Boolean) -> Unit,
  activeEscrowInCheckout: EscrowItem?,
  onActiveEscrowInCheckoutChange: (EscrowItem?) -> Unit,
  escrowItems: List<EscrowItem>,
  onConfirmCheckout: (EscrowItem) -> Unit,
  onDispatchClick: (EscrowItem) -> Unit,
  onTransitionState: (String, com.example.db.EscrowState, String) -> Unit,
  getLogsForDeal: (String) -> kotlinx.coroutines.flow.Flow<List<com.example.db.EscrowStateLog>>,
  onGenerateQrClick: (EscrowItem) -> Unit,
  onScanReceiptClick: (EscrowItem) -> Unit,
  onSendMoneyClick: () -> Unit,
  onCustomEscrowClick: () -> Unit,
  onSuccessMessage: (String) -> Unit,
  onLaunchTelemetryHud: (EscrowItem) -> Unit,
  onBack: () -> Unit,
  isPrivacyMode: Boolean = false,
  onIsPrivacyModeChange: (Boolean) -> Unit = {}
) {
  var expandedDealId by remember { mutableStateOf<String?>(null) }

  // Interactive Live Campfire Pool state for Naivasha Caravan Shuttle
  var campfirePoolProgress by remember { mutableStateOf(7200.0) }
  var myPoolContribution by remember { mutableStateOf(1500.0) }
  val targetCampfirePool = 10000.0

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF020617)) // Slate 950
      .padding(16.dp)
      .verticalScroll(rememberScrollState())
  ) {
    // Header Row
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = if (isSellerMode) "Partner Studio 💼" else "My Wallet 💳",
          fontSize = 22.sp,
          fontWeight = FontWeight.Black,
          color = Color.White,
          letterSpacing = (-0.5).sp
        )
        Text(
          text = if (isSellerMode) 
            "Track bookings, manage payments, and view your seller earnings." 
          else 
            "Manage your balance, lock escrow payments, and track rewards.",
          fontSize = 11.sp,
          color = Color(0xFF94A3B8),
          lineHeight = 14.sp
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Vault Privacy Shield pill
        Box(
          modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (isPrivacyMode) Color(0xFF064E3B) else Color(0xFF0F172A))
            .border(
              width = 1.dp,
              color = if (isPrivacyMode) Color(0xFF10B981).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f),
              shape = RoundedCornerShape(18.dp)
            )
            .clickable { onIsPrivacyModeChange(!isPrivacyMode) }
            .padding(horizontal = 12.dp),
          contentAlignment = Alignment.Center
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = if (isPrivacyMode) "🛡️" else "👁️",
              fontSize = 12.sp
            )
            Text(
              text = if (isPrivacyMode) "SHIELDED" else "UNMASKED",
              fontSize = 8.sp,
              fontWeight = FontWeight.Black,
              color = if (isPrivacyMode) Color(0xFF10B981) else Color(0xFF64748B),
              letterSpacing = 0.5.sp
            )
          }
        }

        IconButton(
          onClick = onBack,
          modifier = Modifier
            .size(36.dp)
            .background(Color(0xFF0F172A), CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
        ) {
          Text(text = "🏠", fontSize = 16.sp)
        }
      }
    }

    // Explorer / Host View Toggle Tabs
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(44.dp)
        .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
        .padding(4.dp),
      horizontalArrangement = Arrangement.SpaceAround,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight()
          .clip(RoundedCornerShape(8.dp))
          .background(if (!isSellerMode) Color(0xFF1E293B) else Color.Transparent)
          .clickable { onIsSellerModeChange(false) }
          .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "🎒 Explorer Wallet",
          color = if (!isSellerMode) Color(0xFFFFB300) else Color(0xFF64748B),
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold
        )
      }

      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight()
          .clip(RoundedCornerShape(8.dp))
          .background(if (isSellerMode) Color(0xFF1E293B) else Color.Transparent)
          .clickable { onIsSellerModeChange(true) }
          .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "💼 Partner Studio",
          color = if (isSellerMode) Color(0xFFFFB300) else Color(0xFF64748B),
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    if (!isSellerMode) {
      // ==================== EXPLORER PERSPECTIVE ====================

      // Secured Caravan checkout card
      if (activeEscrowInCheckout != null) {
        val checkoutItem = activeEscrowInCheckout
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
          shape = RoundedCornerShape(20.dp),
          border = BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.4f)),
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text("🔒", fontSize = 20.sp)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Secure Funds in Escrow",
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFFB300)
              )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Item/Listing: ${checkoutItem.title}",
              fontSize = 12.sp,
              color = Color.White,
              fontWeight = FontWeight.Bold
            )
            
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(vertical = 4.dp)
            ) {
              Text(
                text = "Seller: ${checkoutItem.merchant}",
                fontSize = 10.sp,
                color = Color(0xFF94A3B8)
              )
              if (checkoutItem.isVendorVerified) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "✓ Verified Seller",
                  fontSize = 8.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF10B981),
                  modifier = Modifier
                    .background(Color(0xFF10B981).copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "📍 ${checkoutItem.distance}",
                fontSize = 10.sp,
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.SemiBold
              )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Fees Breakdown Summary Box
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                .padding(10.dp)
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text("Item Price", fontSize = 10.sp, color = Color.Gray)
                  Text("KES ${String.format("%,.0f", checkoutItem.amount)}", fontSize = 10.sp, color = Color.White)
                }
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text("Secure Escrow Fee (2%)", fontSize = 10.sp, color = Color.Gray)
                  Text("KES ${String.format("%,.0f", checkoutItem.serviceFee)}", fontSize = 10.sp, color = Color.White)
                }
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text("Mobile Payment Fee", fontSize = 10.sp, color = Color.Gray)
                  Text("KES ${String.format("%,.0f", checkoutItem.networkFee)}", fontSize = 10.sp, color = Color.White)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.1f))
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text("Total Amount to Hold", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB300))
                  val grandTotal = checkoutItem.amount + checkoutItem.serviceFee + checkoutItem.networkFee
                  Text(
                    text = "KES ${String.format("%,.0f", grandTotal)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFB300)
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Adaptive Carrier Gateways
            when {
              selectedLocation.contains("Nairobi") -> {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF047857).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF10B981), RoundedCornerShape(12.dp))
                    .padding(12.dp)
                ) {
                  Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text("🟢", fontSize = 12.sp)
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("M-Pesa STK Push Express", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("A secure prompt will request your PIN to authorize the escrow transaction.", fontSize = 9.sp, color = Color(0xFF94A3B8))
                  }
                }
              }
              selectedLocation.contains("Lagos") -> {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF039BE5).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF039BE5), RoundedCornerShape(12.dp))
                    .padding(12.dp)
                ) {
                  Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text("🔵", fontSize = 12.sp)
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("Paystack Direct Secure EFT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Secure OTP bank transfer authorization. Holds funds in escrow.", fontSize = 9.sp, color = Color(0xFF94A3B8))
                  }
                }
              }
              else -> {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
                ) {
                  Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text("🔴", fontSize = 12.sp)
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("Ozow Capitec Pay / EFT Gateway", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Approve safely inside your banking app. Funds will be held in secure escrow.", fontSize = 9.sp, color = Color(0xFF94A3B8))
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
              Button(
                onClick = { onActiveEscrowInCheckoutChange(null) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .weight(1f)
                  .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
              ) {
                Text("Cancel", color = Color.White, fontSize = 11.sp)
              }

              Spacer(modifier = Modifier.width(10.dp))

              Button(
                onClick = {
                  onConfirmCheckout(checkoutItem)
                  onActiveEscrowInCheckoutChange(null)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(2f)
              ) {
                Text(
                  text = "Confirm & Pay to Escrow 🔒",
                  color = Color.Black,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }
      }

      // Kuest Adventure Passport & Vault Card (Gamified central card)
      Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (isPrivacyMode) Color(0xFF10B981).copy(alpha = 0.25f) else Color(0xFFFFB300).copy(alpha = 0.15f)),
        modifier = Modifier
          .fillMaxWidth()
          .shadow(12.dp, RoundedCornerShape(20.dp))
          .background(
            Brush.linearGradient(
              colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
            ),
            shape = RoundedCornerShape(20.dp)
          )
          .clickable { onIsPrivacyModeChange(!isPrivacyMode) }
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
          // Card Header
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = "KUEST WALLET BALANCES",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF64748B),
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
              )
              if (isPrivacyMode) {
                Text(
                  text = "🔒 SHIELDED",
                  fontSize = 8.sp,
                  fontWeight = FontWeight.Black,
                  color = Color(0xFF10B981),
                  modifier = Modifier
                    .background(Color(0xFF064E3B), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                )
              }
            }

            Box(
              modifier = Modifier
                .background(Color(0xFFFFB300).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                .border(0.5.dp, Color(0xFFFFB300), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
              Text(
                text = "GOLD COINS",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFB300)
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Balance Display
          if (isPrivacyMode) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.padding(vertical = 4.dp)
            ) {
              Text(
                text = "$currencyPrefix ",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.6f),
                fontFamily = FontFamily.Monospace
              )
              Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(5) {
                  Box(
                    modifier = Modifier
                      .size(10.dp)
                      .clip(CircleShape)
                      .background(Color(0xFF10B981))
                  )
                }
              }
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "TAP TO REVEAL",
                fontSize = 8.sp,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                  .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(4.dp))
                  .padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          } else {
            Text(
              text = "$currencyPrefix " + String.format("%,.2f", walletBalance),
              fontSize = 30.sp,
              fontWeight = FontWeight.Black,
              color = Color.White
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Gamified Progression Bar (XP Level & Streak Count)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("🎒", fontSize = 11.sp)
                Text("EXPLORER LEVEL 12", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("•", fontSize = 9.sp, color = Color.Gray)
                Text("8,400 / 10,000 XP", fontSize = 9.sp, color = Color(0xFFFFB300), fontWeight = FontWeight.Bold)
              }
              Spacer(modifier = Modifier.height(4.dp))
              // Custom XP slider bar
              Box(
                modifier = Modifier
                  .fillMaxWidth(0.9f)
                  .height(4.dp)
                  .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(2.dp))
              ) {
                Box(
                  modifier = Modifier
                    .fillMaxWidth(0.84f)
                    .fillMaxHeight()
                    .background(Color(0xFFFFB300), RoundedCornerShape(2.dp))
                )
              }
            }

            Box(
              modifier = Modifier
                .background(Color(0xFFE11D48).copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                .border(0.5.dp, Color(0xFFE11D48), RoundedCornerShape(20.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("🔥", fontSize = 11.sp)
                Text("14D STREAK", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFFE11D48))
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Locked Escrow status banner
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .height(30.dp)
              .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
              .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
              .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(if (isPrivacyMode) "🛡️" else "🔒", fontSize = 10.sp)
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Secure Escrow Balance (Held)",
                fontSize = 10.sp,
                color = Color(0xFF94A3B8)
              )
            }
            Text(
              text = if (isPrivacyMode) "••••" else "$currencyPrefix " + String.format("%,.0f", walletEscrowLocked),
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFFFB300),
              fontFamily = FontFamily.Monospace
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          // ---------------- GAMIFIED LOOT BAG INVENTORY ----------------
          Text(
            text = "🎁 ACTIVE REWARDS & COLLECTIBLES",
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFFFFB300),
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(bottom = 6.dp)
          )
          
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            val lootItems = listOf(
              Pair("🧥 Hoodie", "7-Peak"),
              Pair("🕶️ Specs", "UV400"),
              Pair("🔋 Solar", "Rugged"),
              Pair("🎟️ Pass", "Naivasha")
            )
            lootItems.forEach { loot ->
              Box(
                modifier = Modifier
                  .weight(1f)
                  .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                  .border(0.5.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                  .padding(6.dp),
                contentAlignment = Alignment.Center
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(loot.first, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                  Text(loot.second, fontSize = 8.sp, color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Action Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Button(
          onClick = onSendMoneyClick,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
          modifier = Modifier
            .weight(1f)
            .height(44.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text("📥", fontSize = 14.sp)
            Text(
              text = "Deposit Funds",
              fontSize = 11.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFF10B981)
            )
          }
        }

        Button(
          onClick = onCustomEscrowClick,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.4f)),
          modifier = Modifier
            .weight(1f)
            .height(44.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text("🤝", fontSize = 14.sp)
            Text(
              text = "New Escrow Contract",
              fontSize = 11.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFFFFB300)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // ==================== INTERACTIVE CAMPFIRE GROUP POOL (New Feature) ====================
      Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
              Text("🔥", fontSize = 18.sp)
              Column {
                Text(
                  text = "GROUP BILL-SPLIT / SHARED POOL",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Black,
                  color = Color.White
                )
                Text(
                  text = "Naivasha Sunset Ridge Event Split",
                  fontSize = 9.sp,
                  color = Color(0xFF94A3B8)
                )
              }
            }
            
            Box(
              modifier = Modifier
                .background(Color(0xFFE11D48), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = "GROUP GOAL",
                color = Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Group contributors row with avatar mockups
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Group Members (Active contributions):", fontSize = 10.sp, color = Color(0xFF64748B))
            Text(
              text = "Target Pool: KES ${String.format("%,.0f", targetCampfirePool)}",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFFFB300)
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          // Beautiful Row of Avatar/Contribution tags
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            val swarm = listOf(
              Triple("Me", myPoolContribution, "👤"),
              Triple("David", 2500.0, "🧭"),
              Triple("Sarah", 1500.0, "🛸"),
              Triple("Peter", 1700.0, "📸")
            )
            swarm.forEach { member ->
              Box(
                modifier = Modifier
                  .weight(1f)
                  .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
                  .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                  .padding(5.dp),
                contentAlignment = Alignment.Center
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(member.third, fontSize = 14.sp)
                  Text(member.first, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                  Text("KES " + String.format("%,.0f", member.second), fontSize = 8.sp, color = Color(0xFFFFB300), fontFamily = FontFamily.Monospace)
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Progress Bar
          val ratio = (campfirePoolProgress / targetCampfirePool).toFloat().coerceIn(0f, 1f)
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(8.dp)
              .clip(RoundedCornerShape(4.dp))
              .background(Color.White.copy(alpha = 0.05f))
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth(ratio)
                .fillMaxHeight()
                .background(Color(0xFFFFB300), RoundedCornerShape(4.dp))
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Total Pooled: KES ${String.format("%,.0f", campfirePoolProgress)} (${(ratio * 100).toInt()}%)",
              fontSize = 10.sp,
              color = Color(0xFF94A3B8)
            )

            Text(
              text = if (campfirePoolProgress >= targetCampfirePool) "✓ Pool Fully Funded!" else "Awaiting KES ${String.format("%,.0f", targetCampfirePool - campfirePoolProgress)}",
              fontSize = 10.sp,
              color = if (campfirePoolProgress >= targetCampfirePool) Color(0xFF10B981) else Color(0xFFFFB300),
              fontWeight = FontWeight.Bold
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Pool contribution CTA
          Button(
            onClick = {
              if (walletBalance >= 1000.0) {
                onWalletBalanceChange(walletBalance - 1000.0)
                onWalletEscrowLockedChange(walletEscrowLocked + 1000.0)
                campfirePoolProgress += 1000.0
                myPoolContribution += 1000.0
                onSuccessMessage("🔥 Contributed KES 1,000 to the Naivasha sunset event pool! Funds secured in escrow.")
              } else {
                onSuccessMessage("⚠️ Insufficient available wallet balance.")
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
            enabled = campfirePoolProgress < targetCampfirePool
          ) {
            Text(
              text = if (campfirePoolProgress >= targetCampfirePool) "✓ POOL FULLY SECURED" else "CONTRIBUTE KES 1,000 (Secure in Escrow Pool)",
              color = Color.Black,
              fontSize = 11.sp,
              fontWeight = FontWeight.Black
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // ---------------- ACTIVE ADVENTURE ESCROWS LIST ----------------
      Text(
        text = "🔒 ACTIVE SECURE ESCROW AGREEMENTS",
        fontSize = 12.sp,
        fontWeight = FontWeight.Black,
        color = Color(0xFF64748B),
        letterSpacing = 0.8.sp
      )

      Spacer(modifier = Modifier.height(8.dp))

      if (escrowItems.isEmpty()) {
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text("🗺️", fontSize = 24.sp)
              Spacer(modifier = Modifier.height(6.dp))
              Text("No active escrow agreements found.", fontSize = 11.sp, color = Color.Gray)
              Text("Select a service, ticket, or transaction from the map/chat to hold funds securely.", fontSize = 10.sp, color = Color.Gray)
            }
          }
        }
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          escrowItems.forEach { item ->
            Card(
              colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
              shape = RoundedCornerShape(16.dp),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  expandedDealId = if (expandedDealId == item.id) null else item.id
                }
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                // Header
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column {
                    Text(
                      text = item.title,
                      fontSize = 13.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color.White
                    )
                    Text(
                      text = "Seller / Partner: ${item.merchant}",
                      fontSize = 10.sp,
                      color = Color(0xFF94A3B8)
                    )
                  }
                  Text(
                    text = if (isPrivacyMode) "••••" else "$currencyPrefix " + String.format("%,.0f", item.amount),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFB300),
                    fontFamily = FontFamily.Monospace
                  )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val step = when (item.status) {
                  "Funds Held" -> 1
                  "In Transit" -> 2
                  "Delivered" -> 3
                  else -> 1
                }

                // High-fidelity Pipeline Progress Bar (Kuest Escrow Vault style)
                Column {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(6.dp)
                      .background(Color(0xFF020617), RoundedCornerShape(3.dp))
                  ) {
                    Box(
                      modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (step >= 1) Color(0xFFFFB300) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(3.dp))
                    )
                    Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color(0xFF0F172A)))
                    Box(
                      modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (step >= 2) Color(0xFFFFB300) else Color.White.copy(alpha = 0.05f))
                    )
                    Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color(0xFF0F172A)))
                    Box(
                      modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (step >= 3) Color(0xFF10B981) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(3.dp))
                    )
                  }

                  Spacer(modifier = Modifier.height(6.dp))

                  // Labels Row
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text(
                      text = "Funds Held 🔒",
                      fontSize = 9.sp,
                      color = if (step >= 1) Color(0xFFFFB300) else Color(0xFF64748B),
                      fontWeight = FontWeight.Bold,
                      fontFamily = FontFamily.Monospace
                    )
                    Text(
                      text = "In Transit 🚚",
                      fontSize = 9.sp,
                      color = if (step >= 2) Color(0xFFFFB300) else Color(0xFF64748B),
                      fontWeight = FontWeight.Bold,
                      fontFamily = FontFamily.Monospace
                    )
                    Text(
                      text = "Delivered ✓",
                      fontSize = 9.sp,
                      color = if (step >= 3) Color(0xFF10B981) else Color(0xFF64748B),
                      fontWeight = FontWeight.Bold,
                      fontFamily = FontFamily.Monospace
                    )
                  }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(6.dp))
                      .background(Color(0xFF1E293B))
                      .padding(horizontal = 8.dp, vertical = 4.dp)
                  ) {
                    val badgeColor = when (item.status) {
                      "Delivered" -> Color(0xFF10B981)
                      "Cancelled" -> Color(0xFFEF4444)
                      "Disputed" -> Color(0xFFEF4444)
                      else -> Color(0xFFFFB300)
                    }
                    Text(
                      text = when (item.status) {
                        "Funds Held" -> "🔒 FUNDS SECURED"
                        "In Transit" -> "🚚 DISPATCHED / IN TRANSIT"
                        "Delivered" -> "✓ TRANSACTION COMPLETED"
                        "Cancelled" -> "❌ CANCELLED / REFUNDED"
                        else -> item.badgeText.uppercase()
                      },
                      fontSize = 8.sp,
                      fontWeight = FontWeight.Black,
                      color = badgeColor,
                      fontFamily = FontFamily.Monospace
                    )
                  }

                  Row(verticalAlignment = Alignment.CenterVertically) {
                    when (item.status) {
                      "Funds Awaiting" -> {
                        Button(
                          onClick = { onConfirmCheckout(item) },
                          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                          shape = RoundedCornerShape(8.dp),
                          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                          modifier = Modifier.height(24.dp)
                        ) {
                          Text("Pay & Fund 🔒", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                          onClick = { onTransitionState(item.id, com.example.db.EscrowState.CANCELLED, "@kuest_explorer") },
                          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                          shape = RoundedCornerShape(8.dp),
                          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                          modifier = Modifier.height(24.dp)
                        ) {
                          Text("Cancel ❌", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                      }
                      "Funds Held" -> {
                        Button(
                          onClick = { onTransitionState(item.id, com.example.db.EscrowState.DISPUTED, "@kuest_explorer") },
                          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                          shape = RoundedCornerShape(8.dp),
                          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                          modifier = Modifier.height(24.dp)
                        ) {
                          Text("File Dispute 🚨", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                      }
                      "In Transit" -> {
                        Button(
                          onClick = { onScanReceiptClick(item) },
                          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                          shape = RoundedCornerShape(8.dp),
                          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                          modifier = Modifier.height(24.dp)
                        ) {
                          Text("Verify Receipt 🤝", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                          onClick = { onTransitionState(item.id, com.example.db.EscrowState.DISPUTED, "@kuest_explorer") },
                          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                          shape = RoundedCornerShape(8.dp),
                          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                          modifier = Modifier.height(24.dp)
                        ) {
                          Text("Dispute 🚨", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                      }
                      "Disputed" -> {
                        Button(
                          onClick = { onTransitionState(item.id, com.example.db.EscrowState.COMPLETED, "@kuest_admin") },
                          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                          shape = RoundedCornerShape(8.dp),
                          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                          modifier = Modifier.height(24.dp)
                        ) {
                          Text("Release ✓", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                          onClick = { onTransitionState(item.id, com.example.db.EscrowState.CANCELLED, "@kuest_admin") },
                          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                          shape = RoundedCornerShape(8.dp),
                          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                          modifier = Modifier.height(24.dp)
                        ) {
                          Text("Refund ❌", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                      }
                      "Delivered" -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                          Box(
                            modifier = Modifier
                              .border(0.5.dp, Color(0xFF10B981), RoundedCornerShape(4.dp))
                              .padding(horizontal = 6.dp, vertical = 2.dp)
                          ) {
                            Text("✓ Completed", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                          }
                          Box(
                            modifier = Modifier
                              .border(0.5.dp, Color(0xFF10B981), RoundedCornerShape(4.dp))
                              .padding(horizontal = 6.dp, vertical = 2.dp)
                          ) {
                            Text("✓ Vault Released", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                          }
                        }
                      }
                      "Cancelled" -> {
                        Box(
                          modifier = Modifier
                            .background(Color(0xFFEF4444).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                          Text("❌ CANCELLED/REFUNDED", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                        }
                      }
                    }
                  }
                }

                if (expandedDealId == item.id) {
                  EscrowGraphicsCard(
                    transactionId = item.id,
                    state = item.status,
                    formattedAmount = "$currencyPrefix " + String.format("%,.0f", item.amount),
                    onLaunchTelemetryHud = { onLaunchTelemetryHud(item) }
                  )
                  EscrowAuditLogView(dealId = item.id, getLogsForDeal = getLogsForDeal)
                }
              }
            }
          }
        }
      }
    } else {
      // ==================== CAMPFIRE CREATOR & HOST PORTAL ====================

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
          border = BorderStroke(1.dp, if (isPrivacyMode) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFFFB300).copy(alpha = 0.15f)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .weight(1f)
            .height(84.dp)
            .clickable { onIsPrivacyModeChange(!isPrivacyMode) }
        ) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "⏳ Pending Escrow",
              fontSize = 11.sp,
              color = Color(0xFF94A3B8),
              fontWeight = FontWeight.Medium
            )
            Text(
              text = if (isPrivacyMode) "••••" else "$currencyPrefix " + String.format("%,.0f", sellerPendingPayouts),
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = if (isPrivacyMode) Color(0xFF10B981) else Color(0xFFFFB300),
              fontFamily = FontFamily.Monospace
            )
          }
        }

        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
          border = BorderStroke(1.dp, if (isPrivacyMode) Color(0xFF10B981).copy(alpha = 0.25f) else Color(0xFF10B981).copy(alpha = 0.15f)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .weight(1f)
            .height(84.dp)
            .clickable { onIsPrivacyModeChange(!isPrivacyMode) }
        ) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "💎 Cleared Earnings",
              fontSize = 11.sp,
              color = Color(0xFF94A3B8),
              fontWeight = FontWeight.Medium
            )
            Text(
              text = if (isPrivacyMode) "••••" else "$currencyPrefix " + String.format("%,.0f", sellerClearedEarnings),
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF10B981),
              fontFamily = FontFamily.Monospace
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Expedition Bookings Activity Chart
      Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier
          .fillMaxWidth()
          .height(200.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Monthly Sales Activity",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )

            Text(
              text = "+24.5% Growth",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF10B981)
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f)
          ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
              val path = Path()
              val fillPath = Path()
              
              val points = listOf(
                Offset(0f, size.height * 0.85f),
                Offset(size.width * 0.2f, size.height * 0.7f),
                Offset(size.width * 0.4f, size.height * 0.75f),
                Offset(size.width * 0.6f, size.height * 0.4f),
                Offset(size.width * 0.8f, size.height * 0.3f),
                Offset(size.width, size.height * 0.15f)
              )

              for (i in 1..3) {
                val y = size.height * (i / 4f)
                drawLine(
                  color = Color.White.copy(alpha = 0.05f),
                  start = Offset(0f, y),
                  end = Offset(size.width, y),
                  strokeWidth = 1.dp.toPx()
                )
              }

              path.moveTo(points[0].x, points[0].y)
              fillPath.moveTo(points[0].x, size.height)
              fillPath.lineTo(points[0].x, points[0].y)

              for (i in 1 until points.size) {
                val prev = points[i - 1]
                val current = points[i]
                val controlPoint1 = Offset(prev.x + (current.x - prev.x) / 2f, prev.y)
                val controlPoint2 = Offset(prev.x + (current.x - prev.x) / 2f, current.y)
                
                path.cubicTo(
                  controlPoint1.x, controlPoint1.y,
                  controlPoint2.x, controlPoint2.y,
                  current.x, current.y
                )

                fillPath.cubicTo(
                  controlPoint1.x, controlPoint1.y,
                  controlPoint2.x, controlPoint2.y,
                  current.x, current.y
                )
              }

              fillPath.lineTo(size.width, size.height)
              fillPath.lineTo(0f, size.height)
              fillPath.close()

              drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                  colors = listOf(Color(0xFFFFB300).copy(alpha = 0.12f), Color.Transparent)
                )
              )

              drawPath(
                path = path,
                color = Color(0xFFFFB300),
                style = Stroke(width = 2.5.dp.toPx())
              )

              points.forEach { pt ->
                drawCircle(Color.Black, radius = 4.dp.toPx(), center = pt)
                drawCircle(Color(0xFFFFB300), radius = 2.5.dp.toPx(), center = pt)
              }
            }

            if (isPrivacyMode) {
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .background(Color(0xFA020617))
                  .clickable { onIsPrivacyModeChange(false) },
                contentAlignment = Alignment.Center
              ) {
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Text("🛡️", fontSize = 22.sp)
                  Text(
                    text = "ANALYTICS SHIELDED",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF10B981),
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                  )
                  Text(
                    text = "Tap inside the graph area to reveal statistics",
                    fontSize = 8.sp,
                    color = Color.Gray
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            listOf("May", "Jun", "Jul", "Aug", "Sep", "Today").forEach { m ->
              Text(
                text = m,
                fontSize = 9.sp,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // ---------------- HOST QUEST MANAGER (New Feature) ----------------
      Text(
        text = "⛰️ MY ACTIVE EVENTS & LISTINGS",
        fontSize = 12.sp,
        fontWeight = FontWeight.Black,
        color = Color(0xFF64748B),
        letterSpacing = 0.8.sp
      )
      
      Spacer(modifier = Modifier.height(8.dp))

      Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          val hostedQuests = listOf(
            Triple("Naivasha Volcanic Ridge & Hot Springs Hike", "Tomorrow • 14 Booked", "🧭"),
            Triple("7-Peak Sunset Wind-Farm Acoustic Jam", "Tomorrow • 22 Booked", "⛰️"),
            Triple("Deep Sea Snorkeling & Reef Restoration", "Next Friday • 8 Booked", "🛥️")
          )
          
          hostedQuests.forEachIndexed { idx, quest ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .background(Color.White.copy(alpha = 0.02f), CircleShape)
                  .border(0.5.dp, Color.White.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Text(quest.third, fontSize = 16.sp)
              }
              
              Column(modifier = Modifier.weight(1f)) {
                Text(quest.first, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                Text(quest.second, fontSize = 9.sp, color = Color(0xFFFFB300), fontWeight = FontWeight.Bold)
              }
              
              Box(
                modifier = Modifier
                  .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                  .padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Text("LIVE", color = Color(0xFF10B981), fontSize = 8.sp, fontWeight = FontWeight.Black)
              }
            }
            if (idx < hostedQuests.size - 1) {
              Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color.White.copy(alpha = 0.05f)))
            }
          }
          
          Spacer(modifier = Modifier.height(10.dp))
          
          Button(
            onClick = { onSuccessMessage("✓ Listing Creation Panel Launched!") },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("+ Create New Event / Listing", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // ---------------- HOST FULFILLMENT CENTER (My Bookings) ----------------
      Text(
        text = "🤝 PARTNER FULFILLMENT CENTER (ACTIVE SALES)",
        fontSize = 12.sp,
        fontWeight = FontWeight.Black,
        color = Color(0xFF64748B),
        letterSpacing = 0.8.sp
      )

      Spacer(modifier = Modifier.height(8.dp))

      if (escrowItems.isEmpty()) {
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
            Text("No active customer orders found.", fontSize = 11.sp, color = Color.Gray)
          }
        }
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          escrowItems.forEach { sale ->
            Card(
              colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
              shape = RoundedCornerShape(16.dp),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  expandedDealId = if (expandedDealId == sale.id) null else sale.id
                }
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column {
                    Text(sale.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Customer: @kuest_explorer", fontSize = 10.sp, color = Color.Gray)
                  }
                  Text(
                    "$currencyPrefix " + String.format("%,.0f", sale.amount),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFB300),
                    fontFamily = FontFamily.Monospace
                  )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(6.dp))
                      .background(Color(0xFF1E293B))
                      .padding(horizontal = 8.dp, vertical = 4.dp)
                  ) {
                    val statusLabel = when (sale.status) {
                      "Funds Awaiting" -> "⏳ Awaiting Customer Payment"
                      "Funds Held" -> "🔒 Funds Secured in Escrow"
                      "In Transit" -> "🚚 Service in Progress / Dispatched"
                      "Disputed" -> "🚨 Disputed Transaction"
                      "Delivered" -> "✓ Settle Complete"
                      "Cancelled" -> "❌ Cancelled"
                      else -> sale.status
                    }
                    val badgeColor = when (sale.status) {
                      "Delivered" -> Color(0xFF10B981)
                      "Cancelled" -> Color(0xFFEF4444)
                      "Disputed" -> Color(0xFFEF4444)
                      else -> Color(0xFFFFB300)
                    }
                    Text(
                      text = statusLabel.uppercase(),
                      fontSize = 8.sp,
                      fontWeight = FontWeight.Black,
                      color = badgeColor,
                      fontFamily = FontFamily.Monospace
                    )
                  }

                  Row(verticalAlignment = Alignment.CenterVertically) {
                    when (sale.status) {
                      "Funds Awaiting" -> {
                        Box(
                          modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFFB300).copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                          Text("Awaiting Customer ⏳", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB300))
                        }
                      }
                      "Funds Held" -> {
                        Button(
                          onClick = { onDispatchClick(sale) },
                          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                          shape = RoundedCornerShape(8.dp),
                          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                          modifier = Modifier.height(24.dp)
                        ) {
                          Text("Dispatch / Start Service 🚐", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                      }
                      "In Transit" -> {
                        Button(
                          onClick = { onGenerateQrClick(sale) },
                          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                          shape = RoundedCornerShape(8.dp),
                          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                          modifier = Modifier.height(24.dp)
                        ) {
                          Text("Fulfillment QR Handshake 🤝", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                      }
                      "Disputed" -> {
                        Box(
                          modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                          Text("🚨 Disputed (Locked)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                        }
                      }
                      "Delivered" -> {
                        Box(
                          modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                          Text("Rewards Cleared ✓", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }
                      }
                      "Cancelled" -> {
                        Box(
                          modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                          Text("❌ Refunded", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                        }
                      }
                    }
                  }
                }

                if (expandedDealId == sale.id) {
                  EscrowGraphicsCard(
                    transactionId = sale.id,
                    state = sale.status,
                    formattedAmount = "$currencyPrefix " + String.format("%,.0f", sale.amount),
                    onLaunchTelemetryHud = { onLaunchTelemetryHud(sale) }
                  )
                  EscrowAuditLogView(dealId = sale.id, getLogsForDeal = getLogsForDeal)
                }
              }
            }
          }
        }
      }
    }
  }
}

// ==================== ESCROW AUDIT LOG TIMELINE VIEW ====================
@Composable
fun EscrowAuditLogView(
  dealId: String,
  getLogsForDeal: (String) -> kotlinx.coroutines.flow.Flow<List<com.example.db.EscrowStateLog>>
) {
  val logsFlow = remember(dealId) { getLogsForDeal(dealId) }
  val logs by logsFlow.collectAsState(initial = emptyList())

  Spacer(modifier = Modifier.height(10.dp))
  Row(
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      painter = painterResource(id = R.drawable.ic_escrow_dialogue_wireframe),
      contentDescription = "Audit log wireframe telemetry header icon",
      tint = NeonCyan,
      modifier = Modifier.size(16.dp)
    )
    Text(
      text = "SECURE LEDGER STATE TRANSITIONS",
      fontSize = 10.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
      color = NeonCyan
    )
  }
  Spacer(modifier = Modifier.height(6.dp))

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(CyberSurface, shape = AngularCyberShape)
      .border(1.dp, NeonCyan.copy(alpha = 0.3f), shape = AngularCyberShape)
      .padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    if (logs.isEmpty()) {
      Text(
        text = "NO REGISTERED DATABASE TRANSITIONS YET.",
        color = Color.Gray,
        fontSize = 9.sp,
        fontFamily = FontFamily.Monospace
      )
    } else {
      logs.forEach { log ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              painter = painterResource(id = R.drawable.ic_escrow_dialogue_wireframe),
              contentDescription = "Transaction Log Step",
              tint = NeonGreen,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            val transitionText = if (log.fromState == null) {
              "CREATED (${log.toState})"
            } else {
              "${log.fromState} ➔ ${log.toState}"
            }
            Text(
              text = transitionText,
              fontSize = 10.sp,
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
          }
          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = "BY: ${log.triggeredBy?.uppercase() ?: "SYSTEM"}",
              fontSize = 8.sp,
              color = Color.LightGray,
              fontFamily = FontFamily.Monospace
            )
            val df = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            Text(
              text = df.format(java.util.Date(log.changedAt)),
              fontSize = 8.sp,
              color = Color.Gray,
              fontFamily = FontFamily.Monospace
            )
          }
        }
      }
    }
  }
}

// ==================== KUEST CARAVAN ADVENTURE LOGISTICS & SOUK ====================
data class CaravanPhotographer(
  val id: String,
  val name: String,
  val handle: String,
  val rating: Double,
  val reviews: Int,
  val specialty: String,
  val gear: String,
  val price: Double,
  val emoji: String,
  val description: String
)

data class CaravanTourGuide(
  val id: String,
  val companyName: String,
  val rating: Double,
  val totalTours: Int,
  val mainGuide: String,
  val plannedEventTitle: String,
  val plannedEventDate: String,
  val plannedEventPrice: Double,
  val emoji: String,
  val description: String
)

data class CaravanTravelerGood(
  val id: String,
  val name: String,
  val seller: String,
  val price: Double,
  val rating: Double,
  val emoji: String,
  val description: String,
  val stock: Int
)

data class CaravanCourierShop(
  val id: String,
  val name: String,
  val duration: String,
  val basePricePerKg: Double,
  val rating: Double,
  val emoji: String,
  val description: String,
  val serviceType: String
)

@Composable
fun MarketScreen(
  currencyPrefix: String,
  marketplaceItems: List<MarketplaceItem>,
  onMessageClick: (MarketplaceItem) -> Unit,
  onBack: () -> Unit,
  onLockEscrowClick: ((MarketplaceItem) -> Unit)? = null
) {
  var searchQuery by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf("All") }
  
  // Caravan State
  var caravanJoined by remember { mutableStateOf(false) }
  var caravanProgress by remember { mutableStateOf(0.80f) }
  var caravanCount by remember { mutableStateOf(4) }

  // Traveler's Indemnity State
  var isTravelerIndemnityActive by remember { mutableStateOf(true) }

  // Dialog / Details States
  var showDriverContactDialog by remember { mutableStateOf(false) }
  var showEscrowRulesDialog by remember { mutableStateOf(false) }
  
  // New Interactive Dialog / Details States
  var selectedPhotographer by remember { mutableStateOf<CaravanPhotographer?>(null) }
  var selectedTourGuide by remember { mutableStateOf<CaravanTourGuide?>(null) }
  var selectedTravelerGood by remember { mutableStateOf<CaravanTravelerGood?>(null) }
  var selectedCourierShop by remember { mutableStateOf<CaravanCourierShop?>(null) }

  // New interactive form inputs inside dialogs
  var selectedShootType by remember { mutableStateOf("Sunset Campfire Portrait") }
  var selectedGuideTicketQty by remember { mutableStateOf(1) }
  var selectedGoodQty by remember { mutableStateOf(1) }
  var courierWeightInput by remember { mutableStateOf("2") } // Default 2 kg
  var courierDestinationInput by remember { mutableStateOf("Nairobi Westlands Hub") }

  var activeHudMessage by remember { mutableStateOf<String?>(null) }
  val scope = rememberCoroutineScope()

  fun triggerHud(msg: String) {
    activeHudMessage = msg
    scope.launch {
      kotlinx.coroutines.delay(3500)
      if (activeHudMessage == msg) {
        activeHudMessage = null
      }
    }
  }

  // Categories list
  val categories = listOf(
    Pair("All", "🗺️"),
    Pair("Shuttles", "👥"),
    Pair("Media", "📸"),
    Pair("Gear", "🎒"),
    Pair("Guides", "🧭"),
    Pair("Courier", "📦")
  )

  // High-fidelity adventure logistics data
  val photographers = remember {
    listOf(
      CaravanPhotographer(
        id = "photo_peter",
        name = "Peter Kajiado",
        handle = "@kajiado_lens",
        rating = 4.9,
        reviews = 142,
        specialty = "Mountain Action & Campfire Portraits",
        gear = "Sony A7R V + 70-200mm f2.8 GM II",
        price = 3500.0,
        emoji = "📸",
        description = "Expert altitude photographer based in Ngong Hills. Specializes in sunset campfire action portraits and dramatic lighting peaks trek photography. Delivers premium digital album within 12 hours!"
      ),
      CaravanPhotographer(
        id = "photo_sarah",
        name = "Sarah Drones",
        handle = "@sunset_drones_ke",
        rating = 5.0,
        reviews = 98,
        specialty = "Aerial 4K Cinematography",
        gear = "DJI Mavic 3 Pro Cine + Hasselblad",
        price = 5000.0,
        emoji = "🛸",
        description = "Licensed commercial drone operator. Offers 4K cinematic follow-me sequences, epic aerial panoramas, and Naivasha hot air balloon launch video shoots. 100% legal & insured airspace flights."
      ),
      CaravanPhotographer(
        id = "photo_alex",
        name = "Alex Surf",
        handle = "@malindi_surf_media",
        rating = 4.8,
        reviews = 110,
        specialty = "Ocean Surf & Festival Vibe Shoot",
        gear = "Sony A7 IV + AquaTech Water Housing",
        price = 4000.0,
        emoji = "🏄",
        description = "Malindi-native action photographer. Perfect for underwater snorkeling captures, sand dune surfs, and Summer Tides beachfront festival crowd vibes. Includes waterproof drone highlights."
      )
    )
  }

  val tourGuides = remember {
    listOf(
      CaravanTourGuide(
        id = "guide_rift",
        companyName = "Rift Valley Explorers Co.",
        rating = 4.9,
        totalTours = 340,
        mainGuide = "David Rift",
        plannedEventTitle = "Naivasha Volcanic Ridge & Hot Air Balloon Hike",
        plannedEventDate = "July 18 (Tomorrow)",
        plannedEventPrice = 6000.0,
        emoji = "🧭",
        description = "Leading eco-tourism guides in Naivasha. Certified wildlife spotters and balloon safety inspectors guiding you through Hell's Gate volcanic paths and obsidian-rich hot springs."
      ),
      CaravanTourGuide(
        id = "guide_swaleh",
        companyName = "Malindi Marine Safaris Ltd",
        rating = 4.95,
        totalTours = 480,
        mainGuide = "Captain Swaleh",
        plannedEventTitle = "Deep Sea Coral Reef Snorkeling Expedition",
        plannedEventDate = "July 24 (Next Friday)",
        plannedEventPrice = 5000.0,
        emoji = "🛥️",
        description = "Vetted marine guides specializing in coastal exploration, dolphin encounters, coral reef restoration educational treks, and premium deep-sea sandbar cookouts."
      ),
      CaravanTourGuide(
        id = "guide_grace",
        companyName = "Ngong Wind-Farm Trailblazers",
        rating = 4.8,
        totalTours = 190,
        mainGuide = "Grace Hills",
        plannedEventTitle = "7-Peak Sunset Wind-Farm Ridge Trek",
        plannedEventDate = "July 18 (Tomorrow)",
        plannedEventPrice = 2000.0,
        emoji = "⛰️",
        description = "Adventure guides focusing on the legendary 7 peaks of Ngong. High-energy crew with campfire setup kits, marshmallows, and summit acoustic music jam sessions."
      )
    )
  }

  val travelerGoods = remember {
    listOf(
      CaravanTravelerGood(
        id = "goods_hoodie",
        name = "KUEST 7-Peak Campfire Hoodie",
        seller = "@kuest_apparel",
        price = 3500.0,
        rating = 4.9,
        emoji = "🧥",
        description = "Heavyweight premium cotton fleece hoodie designed for freezing wind-farm summit winds in Ngong Hills. Features reflective safety telemetry stripes.",
        stock = 15
      ),
      CaravanTravelerGood(
        id = "goods_combo",
        name = "Desert Specs & Vibe Sunblock Combo",
        seller = "@safari_glow",
        price = 1200.0,
        rating = 4.7,
        emoji = "🕶️",
        description = "UV400 desert-proof tinted sunglasses paired with custom SPF 50+ aloe vera sunblock cream. Essential protection for Naivasha and Malindi shores.",
        stock = 45
      ),
      CaravanTravelerGood(
        id = "goods_powerbank",
        name = "20K mAh Solar Rugged Power Bank",
        seller = "@power_nomad",
        price = 4500.0,
        rating = 4.95,
        emoji = "🔋",
        description = "Shockproof backup battery recharging via integrated solar panel. Features 15W wireless charging pad and integrated 400-lumen LED trail flashlight.",
        stock = 8
      ),
      CaravanTravelerGood(
        id = "goods_drybag",
        name = "Waterproof Adventure Dry-Bag (20L)",
        seller = "@wild_gear_ke",
        price = 2500.0,
        rating = 4.8,
        emoji = "🎒",
        description = "Keep your high-end cameras and electronics bone-dry during snorkeling excursions, coastal boat transfers, or sudden beachfront rainstorms.",
        stock = 22
      )
    )
  }

  val courierShops = remember {
    listOf(
      CaravanCourierShop(
        id = "courier_caravan",
        name = "Secured Caravan Courier & Luggage Drop",
        duration = "Same-Day Delivery",
        basePricePerKg = 600.0,
        rating = 4.9,
        emoji = "📦",
        description = "Fully insured secure luggage courier. Drop off heavy gear or massive travel souvenirs in Malindi/Naivasha, and collect them at Nairobi Westlands Hub. Release escrow funds instantly upon arrival check.",
        serviceType = "Priority Event Shuttle Logistics"
      ),
      CaravanCourierShop(
        id = "courier_g4s",
        name = "G4S Premium Artifact Secure Courier",
        duration = "Next-Day Insured Priority",
        basePricePerKg = 1200.0,
        rating = 4.95,
        emoji = "🚛",
        description = "Gold standard high-security logistics for highly precious artifacts like obsidian carvings, customized Maasai beadcrafts, and high-value gear.",
        serviceType = "Insured Valuables Transport"
      )
    )
  }

  // Helper filters based on Search and Selected Category
  val filteredPhotographers = photographers.filter {
    (selectedCategory == "All" || selectedCategory == "Media") &&
    (searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true) || it.specialty.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true))
  }

  val filteredTourGuides = tourGuides.filter {
    (selectedCategory == "All" || selectedCategory == "Guides") &&
    (searchQuery.isEmpty() || it.companyName.contains(searchQuery, ignoreCase = true) || it.plannedEventTitle.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true))
  }

  val filteredTravelerGoods = travelerGoods.filter {
    (selectedCategory == "All" || selectedCategory == "Gear") &&
    (searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true))
  }

  val filteredCouriers = courierShops.filter {
    (selectedCategory == "All" || selectedCategory == "Courier") &&
    (searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true))
  }

  val showShuttlesSection = selectedCategory == "All" || selectedCategory == "Shuttles"

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF020617)) // Slate-950 background
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(bottom = 80.dp) // Avoid navigation bar overlap
    ) {
      // 1. Header Row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "KUEST CARAVAN™",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = (-0.5).sp
          )
          Text(
            text = "Adventure Logistics & Souk 🎒",
            fontSize = 12.sp,
            color = Color(0xFFFFB300), // Elegant Amber Gold
            fontWeight = FontWeight.Bold
          )
        }

        // Mini Profile badge
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
          Text("👑", fontSize = 14.sp)
          Text(
            text = "@kuest_explorer",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
        }
      }

      // 2. Search Bar
      Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text("🔍", fontSize = 16.sp)
          Box(modifier = Modifier.weight(1f)) {
            if (searchQuery.isEmpty()) {
              Text(
                text = "Search photographers, guides, gear, couriers...",
                color = Color(0xFF64748B),
                fontSize = 11.sp
              )
            }
            BasicTextFieldMock(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              textColor = Color.White,
              fontSize = 12.sp
            )
          }
          if (searchQuery.isNotEmpty()) {
            Text(
              text = "✕",
              color = Color(0xFF64748B),
              fontSize = 14.sp,
              modifier = Modifier.clickable { searchQuery = "" }
            )
          }
        }
      }

      // 3. Quick Categories Horizontal Row
      LazyRow(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(categories) { cat ->
          val isSelected = selectedCategory == cat.first
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(20.dp))
              .background(if (isSelected) Color(0xFFFFB300) else Color(0xFF0F172A))
              .border(
                1.dp,
                if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(20.dp)
              )
              .clickable { selectedCategory = cat.first }
              .padding(horizontal = 14.dp, vertical = 8.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(cat.second, fontSize = 12.sp)
              Text(
                text = cat.first,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.Black else Color.White
              )
            }
          }
        }
      }

      // --- INDEMNITY & ESCROW OVERVIEW ---
      if (selectedCategory == "All") {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Traveler's Indemnity Safe-Pass Toggle
          Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (isTravelerIndemnityActive) Color(0xFF10B981).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f)),
            modifier = Modifier
              .weight(1f)
              .clickable { 
                isTravelerIndemnityActive = !isTravelerIndemnityActive
                triggerHud(if (isTravelerIndemnityActive) "🛡️ Traveler's Indemnity Protection Activated!" else "⚠️ Indemnity Deactivated.")
              }
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
              ) {
                Text("🛡️ Safe-Pass", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Box(
                  modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isTravelerIndemnityActive) Color(0xFF065F46) else Color(0xFF1E293B))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                  Text(
                    text = if (isTravelerIndemnityActive) "ON" else "OFF",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black
                  )
                }
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text("Protects logistics booking dispute & cancellation refunds.", fontSize = 9.sp, color = Color(0xFF94A3B8), lineHeight = 12.sp)
            }
          }

          // Escrow Rules Guidelines link
          Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
            modifier = Modifier
              .weight(1f)
              .clickable { showEscrowRulesDialog = true }
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
              ) {
                Text("📜 Escrow Vault", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("ℹ️", fontSize = 10.sp, color = Color(0xFFFFB300))
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text("Funds held securely, released only on delivery verification.", fontSize = 9.sp, color = Color(0xFF94A3B8), lineHeight = 12.sp)
            }
          }
        }
      }

      // --- SECTION 1: SHUTTLE CARAVANS & POOLS ---
      if (showShuttlesSection) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text("👥", fontSize = 14.sp)
            Text(
              text = "EVENT TRANSIT SHUTTLES & CARPOOLS",
              fontSize = 11.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFF64748B),
              letterSpacing = 0.8.sp
            )
          }
          Spacer(modifier = Modifier.height(6.dp))

          Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text("🚐", fontSize = 28.sp)
                  Column {
                    Text(
                      text = "\"The Naivasha Sunset Flotilla Caravan\" • Tonight",
                      fontSize = 13.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color.White
                    )
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      Text(
                        text = "CARAVAN STATUS: $caravanCount/5 LOCKED IN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFB300)
                      )
                      Box(
                        modifier = Modifier
                          .background(Color(0xFFE11D48), RoundedCornerShape(4.dp))
                          .padding(horizontal = 5.dp, vertical = 1.dp)
                      ) {
                        Text(
                          text = "SAVE 25%",
                          color = Color.White,
                          fontSize = 8.sp,
                          fontWeight = FontWeight.Black
                        )
                      }
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              // Custom Progress Bar
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(8.dp)
                  .clip(RoundedCornerShape(4.dp))
                  .background(Color.White.copy(alpha = 0.08f))
              ) {
                Box(
                  modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(caravanProgress)
                    .background(Color(0xFFFFB300))
                )
              }

              Spacer(modifier = Modifier.height(6.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Goal Progress: ${(caravanProgress * 100).toInt()}%",
                  fontSize = 10.sp,
                  color = Color(0xFF64748B)
                )
                if (!caravanJoined) {
                  Text(
                    text = "Need 1 more member to unlock 25% discount!",
                    fontSize = 10.sp,
                    color = Color(0xFFF59E0B),
                    fontWeight = FontWeight.Bold
                  )
                } else {
                  Text(
                    text = "✓ Caravan Filled & Secured!",
                    fontSize = 10.sp,
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Bold
                  )
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              // Pass Action Button
              Button(
                onClick = {
                  if (!caravanJoined) {
                    caravanJoined = true
                    caravanProgress = 1.0f
                    caravanCount = 5
                    triggerHud("✓ Joined Caravan! Booking Escrow Formed.")
                    
                    onLockEscrowClick?.invoke(
                      MarketplaceItem(
                        id = "caravan_pass_sunset",
                        title = "Caravan Pass: Naivasha Sunset Flotilla",
                        tag = "👥 CARAVAN",
                        distance = "Active Deal",
                        price = 1500.0,
                        verifiedOwner = "@naivasha_flotilla",
                        isVerified = true
                      )
                    )
                  } else {
                    triggerHud("You are already locked into this caravan shuttle!")
                  }
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (caravanJoined) Color(0xFF064E3B) else Color(0xFFFFB300)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Text(if (caravanJoined) "✓ RESERVED" else "🎟️", fontSize = 14.sp, color = if (caravanJoined) Color(0xFF10B981) else Color.Black)
                  Text(
                    text = if (caravanJoined) "Caravan Pass Active (KES 1,500 held)" else "CARAVAN PASS: Lock KES 1,500 (Secure wholesale rate)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (caravanJoined) Color.White else Color.Black
                  )
                }
              }
              
              if (caravanJoined) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .clickable { showDriverContactDialog = true }
                    .padding(8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🚐", fontSize = 16.sp)
                    Column {
                      Text("Caravan Shuttle Driver Assigned", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                      Text("Mwangi J. • +254 722 000 111", fontSize = 9.sp, color = Color(0xFF94A3B8))
                    }
                  }
                  Box(
                    modifier = Modifier
                      .background(Color(0xFFFFB300).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                      .padding(horizontal = 6.dp, vertical = 2.dp)
                  ) {
                    Text("CONTACT INFO", color = Color(0xFFFFB300), fontSize = 8.sp, fontWeight = FontWeight.Black)
                  }
                }
              }
            }
          }
        }
      }

      // --- SECTION 2: VETTED TOURIST GUIDE COMPANIES ---
      if (filteredTourGuides.isNotEmpty()) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text("🧭", fontSize = 14.sp)
            Text(
              text = "VETTED LOCAL TOUR COMPANIES & PLANNED EVENTS",
              fontSize = 11.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFF64748B),
              letterSpacing = 0.8.sp
            )
          }
          Spacer(modifier = Modifier.height(6.dp))

          filteredTourGuides.forEach { guide ->
            Card(
              colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
              shape = RoundedCornerShape(16.dp),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
                .clickable { selectedTourGuide = guide }
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.Top
                ) {
                  Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(guide.emoji, fontSize = 28.sp)
                    Column {
                      Text(guide.companyName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("⭐ ${guide.rating}", fontSize = 10.sp, color = Color(0xFFFFB300), fontWeight = FontWeight.Bold)
                        Text("•", fontSize = 10.sp, color = Color.Gray)
                        Text("${guide.totalTours} Guided Expeditions", fontSize = 10.sp, color = Color.Gray)
                      }
                    }
                  }
                  
                  Box(
                    modifier = Modifier
                      .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                      .padding(horizontal = 6.dp, vertical = 2.dp)
                  ) {
                    Text("✓ PLATINUM", color = Color(0xFF10B981), fontSize = 8.sp, fontWeight = FontWeight.Black)
                  }
                }

                Spacer(modifier = Modifier.height(10.dp))
                
                // Planned Event highlight
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.03f))
                    .padding(10.dp)
                ) {
                  Column {
                    Text("📅 NEXT PLANNED EXPEDITION:", fontSize = 8.sp, color = Color(0xFFFFB300), fontWeight = FontWeight.Bold)
                    Text(guide.plannedEventTitle, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Text("Date: ${guide.plannedEventDate}", fontSize = 9.sp, color = Color(0xFF94A3B8))
                      Text(
                        text = "$currencyPrefix " + String.format("%,.0f", guide.plannedEventPrice),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFB300)
                      )
                    }
                  }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Button(
                    onClick = { selectedTourGuide = guide },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                  ) {
                    Text("Details & Itinerary 🧭", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                  }

                  Button(
                    onClick = {
                      onLockEscrowClick?.invoke(
                        MarketplaceItem(
                          id = guide.id,
                          title = "Guided Expedition: ${guide.plannedEventTitle}",
                          tag = "🧭 GUIDE",
                          distance = guide.companyName,
                          price = guide.plannedEventPrice,
                          verifiedOwner = "@" + guide.mainGuide.lowercase().replace(" ", "_"),
                          isVerified = true
                        )
                      )
                      triggerHud("✓ Transferred to Wallet to fund Tour Guide Escrow Contract.")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                  ) {
                    Text("Lock Ticket Escrow 🔒", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Black)
                  }
                }
              }
            }
          }
        }
      }

      // --- SECTION 3: ELITE ADVENTURE PHOTOGRAPHERS & MEDIA ---
      if (filteredPhotographers.isNotEmpty()) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text("📸", fontSize = 14.sp)
            Text(
              text = "ELITE ADVENTURE PHOTOGRAPHERS & DUST CREATORS",
              fontSize = 11.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFF64748B),
              letterSpacing = 0.8.sp
            )
          }
          Spacer(modifier = Modifier.height(6.dp))

          filteredPhotographers.forEach { photo ->
            Card(
              colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
              shape = RoundedCornerShape(16.dp),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
                .clickable { selectedPhotographer = photo }
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(photo.emoji, fontSize = 28.sp)
                    Column {
                      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(photo.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(photo.handle, fontSize = 10.sp, color = Color(0xFFFFB300), fontFamily = FontFamily.Monospace)
                      }
                      Text("Specialty: ${photo.specialty}", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    }
                  }
                  
                  Column(horizontalAlignment = Alignment.End) {
                    Text("⭐ ${photo.rating}", fontSize = 11.sp, color = Color(0xFFFFB300), fontWeight = FontWeight.Black)
                    Text("(${photo.reviews} reviews)", fontSize = 8.sp, color = Color.Gray)
                  }
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                  text = "Gear: ⚙️ ${photo.gear}",
                  fontSize = 10.sp,
                  color = Color(0xFFE2E8F0),
                  fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column {
                    Text("PACKAGE DISPATCH RATE", fontSize = 8.sp, color = Color.Gray)
                    Text("$currencyPrefix " + String.format("%,.0f", photo.price), fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFB300))
                  }

                  Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                      onClick = { selectedPhotographer = photo },
                      colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                      shape = RoundedCornerShape(8.dp),
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                      Text("View Portfolio 📁", fontSize = 9.sp, color = Color.White)
                    }

                    Button(
                      onClick = {
                        onLockEscrowClick?.invoke(
                          MarketplaceItem(
                            id = photo.id,
                            title = "Adventure Media Session: ${photo.name}",
                            tag = "📸 MEDIA",
                            distance = photo.specialty,
                            price = photo.price,
                            verifiedOwner = photo.handle,
                            isVerified = true
                          )
                        )
                        triggerHud("✓ Media session contract created! Locked in escrow.")
                      },
                      colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                      shape = RoundedCornerShape(8.dp),
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                      Text("Hire Creator 🔒", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }
          }
        }
      }

      // --- SECTION 4: TRAVELER GOODS, GEAR & ESSENTIALS ---
      if (filteredTravelerGoods.isNotEmpty()) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text("🎒", fontSize = 14.sp)
            Text(
              text = "TRAVELER STALLS: GEAR, APPAREL & ESSENTIALS",
              fontSize = 11.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFF64748B),
              letterSpacing = 0.8.sp
            )
          }
          Spacer(modifier = Modifier.height(6.dp))

          // 2x2 grid style for goods
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            filteredTravelerGoods.chunked(2).forEach { rowItems ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                rowItems.forEach { good ->
                  Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                    modifier = Modifier
                      .weight(1f)
                      .clickable { selectedTravelerGood = good }
                  ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Text(good.emoji, fontSize = 26.sp)
                        Box(
                          modifier = Modifier
                            .background(Color(0xFFFFB300).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                          Text("${good.stock} LEFT", color = Color(0xFFFFB300), fontSize = 7.sp, fontWeight = FontWeight.Black)
                        }
                      }

                      Spacer(modifier = Modifier.height(8.dp))
                      Text(good.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                      Text("By ${good.seller}", fontSize = 8.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                      
                      Spacer(modifier = Modifier.height(6.dp))
                      Text(
                        text = good.description,
                        fontSize = 9.sp,
                        color = Color(0xFF94A3B8),
                        lineHeight = 12.sp,
                        maxLines = 2
                      )

                      Spacer(modifier = Modifier.height(10.dp))

                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Text(
                          text = "$currencyPrefix " + String.format("%,.0f", good.price),
                          fontSize = 11.sp,
                          fontWeight = FontWeight.Black,
                          color = Color(0xFFFFB300)
                        )

                        Box(
                          modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFB300))
                            .clickable {
                              onLockEscrowClick?.invoke(
                                MarketplaceItem(
                                  id = good.id,
                                  title = "Gear Purchase: ${good.name}",
                                  tag = "🎒 GEAR",
                                  distance = "Stall Pickup",
                                  price = good.price,
                                  verifiedOwner = good.seller,
                                  isVerified = true
                                )
                              )
                              triggerHud("✓ Merch item secure contract added to wallet checkout!")
                            },
                          contentAlignment = Alignment.Center
                        ) {
                          Text("🛒", fontSize = 11.sp)
                        }
                      }
                    }
                  }
                }
                // Placeholder if uneven count
                if (rowItems.size < 2) {
                  Spacer(modifier = Modifier.weight(1f))
                }
              }
            }
          }
        }
      }

      // --- SECTION 5: COURIER & DISPATCH CORNER ---
      if (filteredCouriers.isNotEmpty()) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text("📦", fontSize = 14.sp)
            Text(
              text = "SECURED TRAVEL COURIERS & SOUVENIR DISPATCH SHOPS",
              fontSize = 11.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFF64748B),
              letterSpacing = 0.8.sp
            )
          }
          Spacer(modifier = Modifier.height(6.dp))

          filteredCouriers.forEach { courier ->
            Card(
              colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
              shape = RoundedCornerShape(16.dp),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
                .clickable { selectedCourierShop = courier }
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(courier.emoji, fontSize = 28.sp)
                    Column {
                      Text(courier.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                      Text("⚡ ${courier.duration}", fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                    }
                  }
                  
                  Box(
                    modifier = Modifier
                      .background(Color(0xFFFFB300).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                      .padding(horizontal = 6.dp, vertical = 2.dp)
                  ) {
                    Text("⭐ ${courier.rating}", color = Color(0xFFFFB300), fontSize = 8.sp, fontWeight = FontWeight.Black)
                  }
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                  text = courier.description,
                  fontSize = 10.sp,
                  color = Color(0xFF94A3B8),
                  lineHeight = 14.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column {
                    Text("BASE LOGISTICS PRICE / KG", fontSize = 8.sp, color = Color.Gray)
                    Text("$currencyPrefix " + String.format("%,.0f", courier.basePricePerKg), fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFB300))
                  }

                  Button(
                    onClick = { selectedCourierShop = courier },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                  ) {
                    Text("Book Dispatch Vault →", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                  }
                }
              }
            }
          }
        }
      }
    }

    // Floating transient overlay for HUD Messages / Toasts
    activeHudMessage?.let { msg ->
      Box(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = 90.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xFF0F172A))
          .border(1.dp, Color(0xFFFFB300), RoundedCornerShape(12.dp))
          .padding(horizontal = 16.dp, vertical = 10.dp)
      ) {
        Text(
          text = msg,
          color = Color.White,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }

    // --- DIALOGS / FLOATING DETAILS SHEETS ---

    // 1. Driver Contact Info Dialog (No complex mapping needed as requested)
    if (showDriverContactDialog) {
      Dialog(onDismissRequest = { showDriverContactDialog = false }) {
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
          border = BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.3f)),
          shape = RoundedCornerShape(20.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Text("👨‍✈️ Caravan Shuttle Driver Profile", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            
            Box(
              modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFB300).copy(alpha = 0.1f)),
              contentAlignment = Alignment.Center
            ) {
              Text("🚐", fontSize = 28.sp)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text("Juma Mwangi", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
              Text("Verified Kuest Safari Partner • ⭐ 4.95", fontSize = 11.sp, color = Color.Gray)
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))

            Column(
              modifier = Modifier.fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Vehicle:", fontSize = 11.sp, color = Color.Gray)
                Text("Toyota Landcruiser 4x4 (White)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
              }
              Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Plate Number:", fontSize = 11.sp, color = Color.Gray)
                Text("KDJ 120H", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
              }
              Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Phone Number:", fontSize = 11.sp, color = Color.Gray)
                Text("+254 722 000 111", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB300))
              }
            }

            Text(
              text = "The driver has already received your exact GPS route coordinates on their console. You only need to verify their Handshake QR upon arrival.",
              fontSize = 10.sp,
              color = Color.LightGray,
              textAlign = TextAlign.Center
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Button(
                onClick = { showDriverContactDialog = false },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.weight(1f)
              ) {
                Text("Dismiss", fontSize = 11.sp, color = Color.White)
              }
              Button(
                onClick = {
                  showDriverContactDialog = false
                  triggerHud("📞 Dialing Driver Juma Mwangi...")
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                modifier = Modifier.weight(1f)
              ) {
                Text("Call Now", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }

    // 2. Escrow Rules Dialog
    if (showEscrowRulesDialog) {
      Dialog(onDismissRequest = { showEscrowRulesDialog = false }) {
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
          border = BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.3f)),
          shape = RoundedCornerShape(20.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
          Column(
            modifier = Modifier
              .padding(18.dp)
              .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Text("📜 Kuest Caravan Escrow Guidelines", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            
            Column(
              verticalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("1. DEPOSIT COVENANT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB300))
              Text("The traveler locks the agreed funds securely in the platform's multi-party smart escrow vault. The service partner is immediately notified to dispatch.", fontSize = 10.sp, color = Color.LightGray)

              Text("2. MUTUAL VERIFICATION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB300))
              Text("Once the photographer, tour guide, or courier completes their delivery, you scan their Handshake QR or barcode. Funds are instantly released.", fontSize = 10.sp, color = Color.LightGray)

              Text("3. CANCELLATION PROTECT COVER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB300))
              Text("Our optional Traveler's Indemnity safe-pass cover reimburses you instantly in the event of extreme weather changes, guide cancels, or road delays.", fontSize = 10.sp, color = Color.LightGray)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
              onClick = { showEscrowRulesDialog = false },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("I Understand", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // 3. Interactive Photographer Hire Dialog
    selectedPhotographer?.let { photo ->
      Dialog(onDismissRequest = { selectedPhotographer = null }) {
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
          border = BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.4f)),
          shape = RoundedCornerShape(24.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
          Column(
            modifier = Modifier
              .padding(20.dp)
              .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("📸 Hire Travel Creator", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
              IconButton(onClick = { selectedPhotographer = null }) {
                Text("✕", color = Color(0xFF64748B), fontSize = 14.sp, fontWeight = FontWeight.Bold)
              }
            }

            Text(photo.name, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text(photo.handle, fontSize = 12.sp, color = Color(0xFFFFB300), fontFamily = FontFamily.Monospace)

            Text(photo.description, fontSize = 11.sp, color = Color(0xFF94A3B8), lineHeight = 15.sp)

            Box(
              modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                .padding(10.dp)
            ) {
              Column {
                Text("🛠️ CAMERA GEAR & OPTICS:", fontSize = 8.sp, color = Color(0xFFFFB300), fontWeight = FontWeight.Bold)
                Text(photo.gear, fontSize = 10.sp, color = Color.White, fontFamily = FontFamily.Monospace)
              }
            }

            Text("Select Shoot Focus Package:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            
            // Custom radio selector
            listOf("Sunset Campfire Portrait", "Trail Trek Action Reel", "High Altitude Drone Shoot", "Mainstage Festival Vibe").forEach { shoot ->
              val isSel = selectedShootType == shoot
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (isSel) Color(0xFFFFB300).copy(alpha = 0.08f) else Color.Transparent)
                  .clickable { selectedShootType = shoot }
                  .border(0.5.dp, if (isSel) Color(0xFFFFB300) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                  .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(shoot, fontSize = 11.sp, color = if (isSel) Color.White else Color(0xFF94A3B8), fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                Text(if (isSel) "●" else "○", color = if (isSel) Color(0xFFFFB300) else Color.Gray, fontSize = 14.sp)
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Secure Session Hold:", fontSize = 11.sp, color = Color(0xFF94A3B8))
              Text("$currencyPrefix " + String.format("%,.0f", photo.price), fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFB300))
            }

            Button(
              onClick = {
                selectedPhotographer = null
                onLockEscrowClick?.invoke(
                  MarketplaceItem(
                    id = photo.id,
                    title = "Media Session: ${photo.name} ($selectedShootType)",
                    tag = "📸 MEDIA",
                    distance = photo.handle,
                    price = photo.price,
                    verifiedOwner = photo.handle,
                    isVerified = true
                  )
                )
              },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Secure Creator Escrow (🔒 Hold $currencyPrefix ${String.format("%,.0f", photo.price)})", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
          }
        }
      }
    }

    // 4. Interactive Tour Guide Dialog
    selectedTourGuide?.let { guide ->
      Dialog(onDismissRequest = { selectedTourGuide = null }) {
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
          border = BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.4f)),
          shape = RoundedCornerShape(24.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
          Column(
            modifier = Modifier
              .padding(20.dp)
              .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("🧭 Certified Local Tour Company", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
              IconButton(onClick = { selectedTourGuide = null }) {
                Text("✕", color = Color(0xFF64748B), fontSize = 14.sp, fontWeight = FontWeight.Bold)
              }
            }

            Text(guide.companyName, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Text("⭐ ${guide.rating}", fontSize = 12.sp, color = Color(0xFFFFB300), fontWeight = FontWeight.Bold)
              Text("•", fontSize = 12.sp, color = Color.Gray)
              Text("Lead Guide: ${guide.mainGuide}", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }

            Text(guide.description, fontSize = 11.sp, color = Color(0xFF94A3B8), lineHeight = 15.sp)

            Box(
              modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.02f))
                .padding(12.dp)
            ) {
              Column {
                Text("📅 NEXT PLANNED EVENT ITINERARY:", fontSize = 8.sp, color = Color(0xFFFFB300), fontWeight = FontWeight.Bold)
                Text(guide.plannedEventTitle, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Schedule: ${guide.plannedEventDate} • Includes group transport, peak navigation, and safety ranger fees.", fontSize = 10.sp, color = Color.LightGray)
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Select Ticket Quantity:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                  modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
                    .clickable { if (selectedGuideTicketQty > 1) selectedGuideTicketQty-- },
                  contentAlignment = Alignment.Center
                ) {
                  Text("-", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Text("$selectedGuideTicketQty", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Box(
                  modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
                    .clickable { if (selectedGuideTicketQty < 10) selectedGuideTicketQty++ },
                  contentAlignment = Alignment.Center
                ) {
                  Text("+", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
              }
            }

            Box(
              modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF020617), RoundedCornerShape(8.dp))
                .padding(10.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text("Total Secured Escrow Hold:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                val totalCost = guide.plannedEventPrice * selectedGuideTicketQty
                Text("$currencyPrefix " + String.format("%,.0f", totalCost), color = Color(0xFFFFB300), fontSize = 14.sp, fontWeight = FontWeight.Black)
              }
            }

            Button(
              onClick = {
                selectedTourGuide = null
                val totalCost = guide.plannedEventPrice * selectedGuideTicketQty
                onLockEscrowClick?.invoke(
                  MarketplaceItem(
                    id = guide.id,
                    title = "Tour: ${guide.plannedEventTitle} ($selectedGuideTicketQty Pax)",
                    tag = "🧭 GUIDE",
                    distance = guide.companyName,
                    price = totalCost,
                    verifiedOwner = "@" + guide.mainGuide.lowercase().replace(" ", "_"),
                    isVerified = true
                  )
                )
              },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Secure Travel Tickets 🔒", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
          }
        }
      }
    }

    // 5. Interactive Courier Shop Booking Dialog
    selectedCourierShop?.let { courier ->
      Dialog(onDismissRequest = { selectedCourierShop = null }) {
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
          border = BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.4f)),
          shape = RoundedCornerShape(24.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
          Column(
            modifier = Modifier
              .padding(20.dp)
              .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("📦 Travel Souvenir Dispatch", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
              IconButton(onClick = { selectedCourierShop = null }) {
                Text("✕", color = Color(0xFF64748B), fontSize = 14.sp, fontWeight = FontWeight.Bold)
              }
            }

            Text(courier.name, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text(courier.serviceType, fontSize = 11.sp, color = Color(0xFFFFB300), fontFamily = FontFamily.Monospace)

            Text(courier.description, fontSize = 11.sp, color = Color(0xFF94A3B8), lineHeight = 15.sp)

            Text("Set Shipment Weight (kg):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Card(
              colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Text("⚖️", fontSize = 14.sp)
                Box(modifier = Modifier.weight(1f)) {
                  BasicTextFieldMock(
                    value = courierWeightInput,
                    onValueChange = { courierWeightInput = it },
                    textColor = Color.White,
                    fontSize = 12.sp
                  )
                }
              }
            }

            Text("Destination Dispatch Address:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Card(
              colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Text("📍", fontSize = 14.sp)
                Box(modifier = Modifier.weight(1f)) {
                  BasicTextFieldMock(
                    value = courierDestinationInput,
                    onValueChange = { courierDestinationInput = it },
                    textColor = Color.White,
                    fontSize = 12.sp
                  )
                }
              }
            }

            // Estimate price
            val weight = courierWeightInput.toDoubleOrNull() ?: 2.0
            val totalPrice = courier.basePricePerKg * weight

            Box(
              modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF020617), RoundedCornerShape(8.dp))
                .padding(10.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text("Calculated Courier Escrow Hold:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Text("$currencyPrefix " + String.format("%,.0f", totalPrice), color = Color(0xFFFFB300), fontSize = 14.sp, fontWeight = FontWeight.Black)
              }
            }

            Button(
              onClick = {
                selectedCourierShop = null
                val weightVal = courierWeightInput.toDoubleOrNull() ?: 2.0
                val totalPriceVal = courier.basePricePerKg * weightVal
                onLockEscrowClick?.invoke(
                  MarketplaceItem(
                    id = courier.id,
                    title = "Courier: ${courier.name} (${courierWeightInput}kg to $courierDestinationInput)",
                    tag = "📦 COURIER",
                    distance = courierDestinationInput,
                    price = totalPriceVal,
                    verifiedOwner = "@courier_dispatch",
                    isVerified = true
                  )
                )
              },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Initiate Secure Dispatch Escrow 🔒", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
          }
        }
      }
    }

    // 6. Interactive Traveler Good Buy Dialog
    selectedTravelerGood?.let { good ->
      Dialog(onDismissRequest = { selectedTravelerGood = null }) {
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
          border = BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.4f)),
          shape = RoundedCornerShape(24.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
          Column(
            modifier = Modifier
              .padding(20.dp)
              .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("🎒 Traveler Shop & Gear Stall", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
              IconButton(onClick = { selectedTravelerGood = null }) {
                Text("✕", color = Color(0xFF64748B), fontSize = 14.sp, fontWeight = FontWeight.Bold)
              }
            }

            Text(good.name, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("Stall Merchant: " + good.seller, fontSize = 11.sp, color = Color(0xFFFFB300), fontFamily = FontFamily.Monospace)

            Text(good.description, fontSize = 11.sp, color = Color(0xFF94A3B8), lineHeight = 15.sp)

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Select Quantity:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                  modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
                    .clickable { if (selectedGoodQty > 1) selectedGoodQty-- },
                  contentAlignment = Alignment.Center
                ) {
                  Text("-", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Text("$selectedGoodQty", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Box(
                  modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
                    .clickable { if (selectedGoodQty < good.stock) selectedGoodQty++ },
                  contentAlignment = Alignment.Center
                ) {
                  Text("+", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
              }
            }

            Box(
              modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF020617), RoundedCornerShape(8.dp))
                .padding(10.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text("Total Secured Goods Hold:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                val totalCost = good.price * selectedGoodQty
                Text("$currencyPrefix " + String.format("%,.0f", totalCost), color = Color(0xFFFFB300), fontSize = 14.sp, fontWeight = FontWeight.Black)
              }
            }

            Button(
              onClick = {
                selectedTravelerGood = null
                val totalCost = good.price * selectedGoodQty
                onLockEscrowClick?.invoke(
                  MarketplaceItem(
                    id = good.id,
                    title = "Gear Stall: ${good.name} ($selectedGoodQty Pcs)",
                    tag = "🎒 GEAR",
                    distance = "Pickup Station",
                    price = totalCost,
                    verifiedOwner = good.seller,
                    isVerified = true
                  )
                )
              },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Buy & Hold Escrow 🔒", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
          }
        }
      }
    }
  }
}

// Basic Text Field mock to safely allow keyboard text input
@Composable
fun BasicTextFieldMock(
  value: String,
  onValueChange: (String) -> Unit,
  textColor: Color,
  fontSize: TextUnit,
  modifier: Modifier = Modifier
) {
  // Use Compose standard BasicTextField to support active user keyboard input
  androidx.compose.foundation.text.BasicTextField(
    value = value,
    onValueChange = onValueChange,
    textStyle = androidx.compose.ui.text.TextStyle(
      color = textColor,
      fontSize = fontSize,
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Bold
    ),
    modifier = modifier.fillMaxWidth(),
    cursorBrush = Brush.verticalGradient(colors = listOf(textColor, textColor))
  )
}

// Reusable localized grid of marketplace items
@Composable
fun LocalizedMarketplaceGrid(
  items: List<MarketplaceItem>,
  currencyPrefix: String,
  onViewDetails: (MarketplaceItem) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    val chunked = items.chunked(2)
    chunked.forEach { rowItems ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        rowItems.forEach { item ->
          Box(
            modifier = Modifier
              .weight(1f)
          ) {
            MarketplaceCardComponent(
              item = item,
              currencyPrefix = currencyPrefix,
              onViewDetails = { onViewDetails(item) }
            )
          }
        }
        // Balance layout for odd item sizes
        if (rowItems.size < 2) {
          Spacer(modifier = Modifier.weight(1f))
        }
      }
    }
  }
}

// Single marketplace grid card item matching KUEST dark-theme aesthetics
@Composable
fun MarketplaceCardComponent(
  item: MarketplaceItem,
  currencyPrefix: String,
  onViewDetails: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = Color(0xFF111216)),
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
    modifier = modifier
      .fillMaxWidth()
      .clickable { onViewDetails() }
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      // Image space gradient backing
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(90.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(
            Brush.linearGradient(
              colors = if (item.tag.contains("SPACE")) {
                listOf(Color(0xFF002233), Color(0xFF005577))
              } else if (item.tag.contains("CULINARY")) {
                listOf(Color(0xFF221100), Color(0xFF553311))
              } else {
                listOf(Color(0xFF112211), Color(0xFF224422))
              }
            )
          ),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = if (item.tag.contains("SPACE")) "🏠" else if (item.tag.contains("CULINARY")) "🍳" else "🌾",
          fontSize = 32.sp
        )
        
        // Dynamic localized proximity overlay
        Box(
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(4.dp)
            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
          Text(text = item.distance, fontSize = 8.sp, color = Color.White)
        }

        // Verification Badge
        Box(
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(4.dp)
            .background(Color(0xFF00E676), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
          Text("🛡️ BROKER VERIFIED", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = item.title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )

      Spacer(modifier = Modifier.height(4.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "$currencyPrefix " + String.format("%,.0f", item.price),
          fontSize = 11.sp,
          fontWeight = FontWeight.Black,
          color = Color(0xFFFFB800)
        )

        Text(
          text = "View Details →",
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFF00E5FF)
        )
      }
    }
  }
}

// Data models
data class GeofenceLog(
  val id: String,
  val timestamp: String,
  val message: String,
  val isSuppressed: Boolean = false,
  val isTriggered: Boolean = false
)

data class MockDeviceLocation(
  val name: String,
  val lat: Double,
  val lng: Double
)

data class InAppNotification(
  val id: String,
  val title: String,
  val body: String,
  val timestamp: String,
  val dealId: String,
  val targetState: String, // e.g. "FUNDED", "DISPATCHED", "COMPLETED", "DISPUTED", "CANCELLED", "WELCOME"
  val isRead: Boolean = false
)

data class SocialPost(
  val id: String,
  val author: String,
  val timeAgo: String,
  val tag: String,
  val content: String,
  val commentsCount: Int,
  val fireCount: Int,
  val isHighAlert: Boolean,
  val tags: List<String> = listOf(tag)
)

data class EscrowItem(
  val id: String,
  val title: String,
  val merchant: String,
  val amount: Double,
  val status: String,
  val badgeText: String,
  val distance: String = "0.8 km",
  val isVendorVerified: Boolean = true,
  val serviceFee: Double = 100.0,
  val networkFee: Double = 50.0
)

data class MarketplaceItem(
  val id: String,
  val title: String,
  val tag: String,
  val distance: String,
  val price: Double,
  val verifiedOwner: String,
  val isVerified: Boolean
)

data class ChatMessage(
  val text: String,
  val isFromMe: Boolean,
  val time: String
)

fun calculateGeofenceDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
  val r = 6371000.0 // Earth radius in meters
  val dLat = Math.toRadians(lat2 - lat1)
  val dLon = Math.toRadians(lon2 - lon1)
  val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
          Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
          Math.sin(dLon / 2) * Math.sin(dLon / 2)
  val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
  return r * c
}

// Featured Host Data Model
data class FeaturedHost(
  val name: String,
  val handle: String,
  val rating: Double,
  val reviewsCount: Int,
  val specialty: String,
  val avatar: String,
  val badge: String
)

// Premium Quest Data Model
data class PremiumQuest(
  val id: String,
  val title: String,
  val category: String, // "Fine Dining", "Seasonal Festivals", "Expeditions", "Art & Soul"
  val subtitle: String,
  val date: String,
  val location: String,
  val price: Double,
  val rating: Double,
  val reviewsCount: Int,
  val emoji: String,
  val description: String,
  val capacity: String,
  val groupOffer: String,
  val duration: String,
  val imageRes: Int? = null
)

// Errand Rider Data Model
data class ErrandRider(
  val name: String,
  val handle: String,
  val distance: String,
  val rating: Double,
  val specialization: String,
  val commissionToday: Double,
  val status: String,
  val avatar: String
)

// Broadcast Bid Data Model
data class BroadcastBid(
  val riderName: String,
  val handle: String,
  val avatar: String,
  val rating: Double,
  val distance: String,
  val itemCost: Double,
  val deliveryFee: Double,
  val deliveryTime: String,
  val tag: String
)

// Host Event Data Model
data class HostEvent(
  val id: String,
  val title: String,
  val category: String,
  val price: Double,
  val tier: String,
  val ticketsSold: Int,
  val capacity: Int,
  val revenue: Double,
  val emoji: String,
  val status: String
)

// Gallery Memory Data Model
data class GalleryMemory(
  val id: String,
  val title: String,
  val caption: String,
  val date: String,
  val mood: String,
  val tag: String,
  val filterApplied: String,
  val likesCount: Int,
  val comments: List<String>
)


data class ChronosMapMarker(
  val id: String,
  val title: String,
  val category: String,
  val x: Float,
  val y: Float,
  val status: String,
  val totalCaptured: Int,
  val photographerActive: Boolean
)

data class ChronosPhotoItem(
  val id: String,
  val title: String,
  val location: String,
  val timestamp: String,
  val faceTags: List<String>,
  val filterApplied: String,
  val gradientColors: List<Color>,
  val emoji: String
)

data class DiscoverReel(
  val id: String,
  val title: String,
  val category: String,
  val creator: String,
  val duration: String,
  val initialViews: Int,
  val initialLikes: Int,
  val gradientColors: List<Color>,
  val emoji: String,
  val activityDescription: String,
  val nextEventDate: String
)

data class HobbyCircle(
  val id: String,
  val name: String,
  val activeMembers: Int,
  val activeOnline: Int,
  val currentQuest: String,
  val tier: String,
  val emoji: String,
  val isJoined: Boolean = false
)

data class SwarmMission(
  val id: String,
  val title: String,
  val requiredCount: Int,
  val currentCount: Int,
  val rewardXp: Int,
  val emoji: String,
  val isCheckedIn: Boolean = false
)

data class ScheduledEvent(
  val id: String,
  val title: String,
  val groupName: String,
  val datetime: String,
  val venue: String,
  val emoji: String,
  val isBooked: Boolean,
  val isEarmarked: Boolean,
  val trackingChannel: String
)

data class MpesaVerifiedTx(
  val id: String,
  val date: String,
  val amount: Double,
  val recipient: String,
  val phone: String,
  val referenceCode: String,
  val status: String
)

data class P2PInstallmentPlan(
  val id: String,
  val title: String,
  val merchant: String,
  val merchantPhone: String,
  val totalAmount: Double,
  val paidAmount: Double,
  val installmentAmount: Double,
  val frequency: String,
  val durationDays: Int,
  val nextDueDate: String
)

data class HostingKit(
  val id: String,
  val title: String,
  val price: Double,
  val description: String,
  val emoji: String,
  val itemHighlights: List<String>,
  val imageRes: Int? = null
)

data class CulinaryCuration(
  val id: String,
  val partnerName: String,
  val itemTitle: String,
  val price: Double,
  val durationMin: Int,
  val emoji: String,
  val rating: Double,
  val description: String,
  val imageRes: Int? = null,
  val sourceType: String = "🏡 KITCHEN"
)

@Composable
fun KuestPremiumHomeScreen(
  selectedLocation: String,
  onShowLocationDialogChange: (Boolean) -> Unit,
  searchQuery: String,
  onSearchQueryChange: (String) -> Unit,
  currencyPrefix: String,
  onTriggerCheckout: (EscrowItem, PremiumQuest) -> Unit,
  onCreateErrandDeal: ((String, String, Double) -> Unit)? = null,
  onTriggerNotification: ((String, String, String, String) -> Unit)? = null
) {
  var activeSubTab by remember { mutableStateOf("home") }
  
  // State for temporary pop-up notification when Discover sub-tab is clicked
  var showDiscoverNicheNotification by remember { mutableStateOf(false) }

  LaunchedEffect(activeSubTab) {
    if (activeSubTab == "discover") {
      showDiscoverNicheNotification = true
    }
  }

  LaunchedEffect(showDiscoverNicheNotification) {
    if (showDiscoverNicheNotification) {
      kotlinx.coroutines.delay(2500)
      showDiscoverNicheNotification = false
    }
  }
  
  // Automated Clique Calendar & Event Organizer State
  var scheduledEvents by remember {
    mutableStateOf(
      listOf(
        ScheduledEvent("e1", "USIU Campus Esports League", "USIU Gamers Guild", "Today, 3:00 PM", "USIU Esports Arena", "🎮", isBooked = false, isEarmarked = true, "Gaming Hub #3"),
        ScheduledEvent("e2", "Sunset Vinyasa Yoga", "Kilimani Wellness Squad", "Tomorrow, 5:30 PM", "Kilimani Gardens", "🧘", isBooked = false, isEarmarked = true, "Wellness Club"),
        ScheduledEvent("e3", "Naivasha Sunset Ridge Caravan", "Kilimani Squad", "July 20, 1:00 PM", "Naivasha Lakeside", "🌅", isBooked = true, isEarmarked = false, "Main Stage Event")
      )
    )
  }

  // State for the 3 brand-new Clique Hub features
  var cliqueVibeStatus by remember { mutableStateOf("🔥 HIGH ENERGY") }
  var cliqueVibeRating by remember { mutableStateOf(4.8f) }
  var droppedBeaconState by remember { mutableStateOf<String?>(null) } // "📍 Kilimani Gardens Rendezvous"
  var whisperConfessions by remember {
    mutableStateOf(
      listOf(
        "To the girl in the red hoodie at the USIU esports tournament: you have insane APM! 😍",
        "Mama's Secret Kitchen samosas are legendary. I ate 5 before the meeting...",
        "Who else is sneaking out of the study group for the Sunset Vinyasa Yoga tomorrow? 🤫",
        "USIU Esports tournament was absolute fire, Kilimani squad represented well!"
      )
    )
  }
  var newConfessionText by remember { mutableStateOf("") }

  var activeHudMessage by remember { mutableStateOf<String?>(null) }
  val scope = rememberCoroutineScope()

  fun triggerHud(msg: String) {
    activeHudMessage = msg
    scope.launch {
      kotlinx.coroutines.delay(1800)
      if (activeHudMessage == msg) activeHudMessage = null
    }
  }

  // Premium State Variables
  var selectedPremiumCategory by remember { mutableStateOf("All") }
  var selectedQuestForDetail by remember { mutableStateOf<PremiumQuest?>(null) }
  var selectedRiderForBooking by remember { mutableStateOf<ErrandRider?>(null) }
  var customErrandTitle by remember { mutableStateOf("") }
  var customErrandAmount by remember { mutableStateOf("") }

  // Featured Host structures and forms
  var showHostBroadcastDialog by remember { mutableStateOf(false) }
  var hostTitle by remember { mutableStateOf("") }
  var hostCategory by remember { mutableStateOf("Culinary Class") }
  var hostPrice by remember { mutableStateOf("1500") }
  var hostDate by remember { mutableStateOf("Tomorrow, 5:00 PM") }
  var hostCapacity by remember { mutableStateOf("15 slots left") }
  var hostDescription by remember { mutableStateOf("") }
  var hostDuration by remember { mutableStateOf("2 Hours") }
  var hostLocation by remember { mutableStateOf("Kilimani Social Club") }

  val featuredHostsList = remember {
    listOf(
      FeaturedHost("Chef Kamau", "@chef_kamau", 4.9, 142, "🍳 Culinary & Afro-Fusion Masterclass", "👨‍🍳", "Master Chef"),
      FeaturedHost("Zola Mindfulness", "@zola_yoga", 4.8, 88, "🧘 Vinyasa Yoga & Breathwork", "🧘‍♀️", "Guru"),
      FeaturedHost("MC Sharaf", "@mc_sharaf", 4.9, 215, "🎤 Club Hype, MC & Host Extraordinaire", "🎤", "Super Host"),
      FeaturedHost("Coach Kiprop", "@coach_kip", 4.7, 56, "⚽ Gym Sessions & Sports Coaching", "🏃‍♂️", "Pro Coach")
    )
  }

  var isHostingModeActive by remember { mutableStateOf(false) }
  var eventErrandInput by remember { mutableStateOf("") }
  var activeConciergeErrands by remember { mutableStateOf(listOf<String>()) }
  var selectedHostingKitForDetail by remember { mutableStateOf<HostingKit?>(null) }
  var preOrderEventName by remember { mutableStateOf<String?>(null) }
  var selectedRiderNickname by remember { mutableStateOf<String?>(null) }
  var riderCallbackRequestedMap by remember { mutableStateOf(mapOf<String, Boolean>()) }

  val hostingKits = remember {
    listOf(
      HostingKit(
        id = "hk1",
        title = "Don Julio 1942 + Citrus Premium Kit",
        price = 22000.0,
        description = "A chilled bottle of Don Julio 1942 Reposado, fresh organic limes, key lime syrup, premium sparkling mixers, dehydrated citrus garnishes, and 4 designer highball glasses.",
        emoji = "🍹",
        itemHighlights = listOf("Chilled 45-min Delivery", "Artisanal Garnishes", "Premium Glassware"),
        imageRes = R.drawable.img_hosting_kit_1784384033472
      ),
      HostingKit(
        id = "hk2",
        title = "Artisanal Charcuterie & Cheese Platter",
        price = 4500.0,
        description = "Fine selected dry-aged salami, prosciutto, imported brie and aged cheddar, local organic honeycomb, premium nuts, and artisanal rosemary crackers arranged elegantly.",
        emoji = "🧀",
        itemHighlights = listOf("Serves 4-6", "Temperature-Controlled Box", "Fresh Today"),
        imageRes = R.drawable.img_chef_table_1784383989247
      ),
      HostingKit(
        id = "hk3",
        title = "Belvedere Chilled Vodka + Tonic Kit",
        price = 9500.0,
        description = "A standard 750ml bottle of premium Belvedere Vodka, 4 bottles of double-filtered tonic water, fresh sliced cucumber, mint sprigs, and a 2kg bag of gourmet clear-sphere ice.",
        emoji = "🍸",
        itemHighlights = listOf("Sphere Ice Included", "45-min Flash Delivery", "Fresh Herb Garnishes"),
        imageRes = R.drawable.img_hosting_kit_1784384033472
      ),
      HostingKit(
        id = "hk4",
        title = "Elite Single Malt & Cigar Pairing Kit",
        price = 18500.0,
        description = "Glenmorangie 12-Year Single Malt Scotch Whisky paired with two hand-rolled Cuban robusto cigars, cedar wood matches, and a portable travel ashtray.",
        emoji = "🥃",
        itemHighlights = listOf("Dual Robustos", "Premium Scotch", "Matches & Ashtray"),
        imageRes = R.drawable.img_hosting_kit_1784384033472
      )
    )
  }

  val culinaryCurations = remember {
    listOf(
      CulinaryCuration(
        id = "cc1",
        partnerName = "The Chef's Atrium",
        itemTitle = "Modernist Flamed Salmon Tataki",
        price = 3200.0,
        durationMin = 35,
        emoji = "🍣",
        rating = 4.9,
        description = "Sashimi-grade salmon lightly seared with a blowtorch, served with yuzu glaze, microgreens, and a side of house-made black truffle soy sauce.",
        imageRes = R.drawable.img_chef_table_1784383989247,
        sourceType = "🏨 HOTEL AD"
      ),
      CulinaryCuration(
        id = "cc2",
        partnerName = "La Taverna Boutiques",
        itemTitle = "Handmade Truffle Cappelletti Pasta",
        price = 2800.0,
        durationMin = 40,
        emoji = "🍝",
        rating = 4.8,
        description = "Freshly rolled pasta stuffed with wild porcini mushrooms, bathed in a rich white truffle butter emulsion and topped with 24-month aged parmigiano-reggiano.",
        imageRes = R.drawable.img_chef_table_1784383989247,
        sourceType = "🏡 INDEPENDENT KITCHEN"
      ),
      CulinaryCuration(
        id = "cc3",
        partnerName = "Smoked Woodhouse",
        itemTitle = "24-Hour Cherrywood Brisket Slider Set",
        price = 2400.0,
        durationMin = 30,
        emoji = "🍔",
        rating = 4.9,
        description = "Three mini brioche buns stuffed with ultra-tender cherrywood-smoked beef brisket, homemade sweet-bourbon BBQ reduction, and crispy onion straws.",
        imageRes = R.drawable.img_chef_table_1784383989247,
        sourceType = "⚡ DELIVERIES PLATFORM"
      ),
      CulinaryCuration(
        id = "cc4",
        partnerName = "Mama's Secret Kitchen",
        itemTitle = "Nairobi Homemade Samosa Feast",
        price = 1200.0,
        durationMin = 20,
        emoji = "🍳",
        rating = 4.9,
        description = "Handcrafted crispy beef and lentil samosas made with heirloom family spices, served with fresh cooling mint chutney.",
        imageRes = R.drawable.img_chef_table_1784383989247,
        sourceType = "🍳 HOMEMADE"
      ),
      CulinaryCuration(
        id = "cc5",
        partnerName = "Prime Grocers Dock",
        itemTitle = "Aged Bluefin Tuna Steak Pack",
        price = 4200.0,
        durationMin = 15,
        emoji = "🍣",
        rating = 4.7,
        description = "Thick-cut fresh bluefin tuna steak ready-to-grill or sear. Sourced directly from local high-end grocery docks.",
        imageRes = R.drawable.img_chef_table_1784383989247,
        sourceType = "🛒 SUPERMARKET"
      )
    )
  }

  var premiumQuests by remember {
    mutableStateOf(
      listOf(
        PremiumQuest(
          id = "pq1",
          title = "Amboseli Wilderness Expedition",
          category = "Expeditions",
          subtitle = "5-Day Luxury Guided Safari",
          date = "Jul 22 - Jul 27",
          location = "Amboseli National Park",
          price = 45000.0,
          rating = 4.9,
          reviewsCount = 120,
          emoji = "🐘",
          description = "Includes high-clearance private transport, gourmet catered luxury canvas tents, real-time geofenced security protocols, and expert-guided night safaris.",
          capacity = "12 slots left",
          groupOffer = "15% off for groups of 4 or more",
          duration = "5 Days",
          imageRes = R.drawable.img_safari_1784383978239
        ),
        PremiumQuest(
          id = "pq2",
          title = "Tasting Menu: Chef's Table",
          category = "Fine Dining",
          subtitle = "9-Course Afro-Fusion Experience",
          date = "Tonight, 7:30 PM",
          location = "Cultured Kilimani, Nairobi",
          price = 6500.0,
          rating = 4.8,
          reviewsCount = 84,
          emoji = "🍳",
          description = "An immersive nine-course culinary storytelling showcase pairing classic African ingredients with modernist culinary techniques.",
          capacity = "4 seats left",
          groupOffer = "Complimentary select wine flight for pairs",
          duration = "3 Hours",
          imageRes = R.drawable.img_chef_table_1784383989247
        ),
        PremiumQuest(
          id = "pq3",
          title = "Sauti Sol Tribute Concert",
          category = "Seasonal Festivals",
          subtitle = "VIP Stage Access & Lounge Pass",
          date = "Tomorrow, 6:00 PM",
          location = "Ngong Racecourse, Nairobi",
          price = 3500.0,
          rating = 4.7,
          reviewsCount = 210,
          emoji = "🎪",
          description = "The ultimate celebration of East African sound. Features direct-to-artist ticket escrow, private lounge amenities, and fast-track bars.",
          capacity = "50 tickets left",
          groupOffer = "10% group rebate automatically applied on purchase",
          duration = "6 Hours",
          imageRes = R.drawable.img_concert_1784384001360
        ),
        PremiumQuest(
          id = "pq4",
          title = "Vector Art & NFT Exhibition",
          category = "Art & Soul",
          subtitle = "Digital Canvas Vernissage",
          date = "Jul 25, 4:00 PM",
          location = "Contemporary Arts Center",
          price = 1200.0,
          rating = 4.6,
          reviewsCount = 45,
          emoji = "🎨",
          description = "Interactive gallery showcasing leading African vector and digital artists with secure blockchain ownership handshakes.",
          capacity = "15 slots left",
          groupOffer = "Buy 3 tickets and receive 1 free coupon",
          duration = "4 Hours",
          imageRes = R.drawable.img_nft_exhibit_1784384012885
        )
      )
    )
  }

  val errandRiders = remember {
    listOf(
      ErrandRider(
        name = "Njoroge Kamau",
        handle = "@njox_delivery",
        distance = "1.2 km away",
        rating = 4.9,
        specialization = "Urgent Deliveries",
        commissionToday = 2400.0,
        status = "Online • Idle",
        avatar = "🏍️"
      ),
      ErrandRider(
        name = "Aisha Mohammed",
        handle = "@aisha_express",
        distance = "2.5 km away",
        rating = 4.8,
        specialization = "Grocery & Fragile",
        commissionToday = 3100.0,
        status = "On Errand",
        avatar = "🚲"
      ),
      ErrandRider(
        name = "David Kiprop",
        handle = "@kip_speed",
        distance = "0.8 km away",
        rating = 4.9,
        specialization = "High-Value Items",
        commissionToday = 4500.0,
        status = "Online • Active",
        avatar = "⚡"
      )
    )
  }

  val hostEvents = remember {
    listOf(
      HostEvent(
        id = "h1",
        title = "Secret Rooftop DJ Set",
        category = "Music",
        price = 2500.0,
        tier = "Premium VIP",
        ticketsSold = 84,
        capacity = 100,
        revenue = 210000.0,
        emoji = "🎧",
        status = "Live"
      ),
      HostEvent(
        id = "h2",
        title = "Decentralized Supper Club",
        category = "Culinary",
        price = 4500.0,
        tier = "Elite Access",
        ticketsSold = 22,
        capacity = 25,
        revenue = 99000.0,
        emoji = "🍽️",
        status = "Upcoming"
      )
    )
  }

  var tokenInput by remember { mutableStateOf("") }
  var isTokenVerified by remember { mutableStateOf(false) }
  var isFaceKeyActive by remember { mutableStateOf(false) }
  var userSelfieHash by remember { mutableStateOf<String?>(null) }
  var isCirclePrivacyEnabled by remember { mutableStateOf(true) }
  var showInviteModal by remember { mutableStateOf(false) }
  var selectedPhotoIds by remember { mutableStateOf(setOf<String>()) }
  var activeLocationFilter by remember { mutableStateOf<String?>(null) }
  var selectedMarker by remember { mutableStateOf<ChronosMapMarker?>(null) }
  var showCheckoutModal by remember { mutableStateOf(false) }
  var selectedMapCategory by remember { mutableStateOf("All Layers") }

  var selectedDiscoverCategory by remember { mutableStateOf("All") }
  var selectedReelForDetail by remember { mutableStateOf<DiscoverReel?>(null) }
  
  var discoverReels by remember {
    mutableStateOf(
      listOf(
        DiscoverReel(
          id = "dr1",
          title = "Sunset Rooftop Silent Disco Blast",
          category = "Secret Events",
          creator = "@vibe_architect",
          duration = "0:45",
          initialViews = 18400,
          initialLikes = 3200,
          gradientColors = listOf(Color(0xFF8B5CF6), Color(0xFFD946EF)),
          emoji = "🎧",
          activityDescription = "Highlights of the 3-channel visual neon showdown on the Westlands Skyline. Dynamic dance circles, interactive glow stations, and custom sound design.",
          nextEventDate = "Tonight, 9:00 PM @ Westlands Rooftop"
        ),
        DiscoverReel(
          id = "dr2",
          title = "Nairobi Skate Jam: Mid-Year Bowl Clash",
          category = "Niche Hobbies",
          creator = "@skate_nrb",
          duration = "1:12",
          initialViews = 9500,
          initialLikes = 1450,
          gradientColors = listOf(Color(0xFFF43F5E), Color(0xFFFB7185)),
          emoji = "🛹",
          activityDescription = "Watch local street shredders battle it out in the custom-built concrete crater. Showcasing intense trick runs, best-trick competitions, and group rideouts.",
          nextEventDate = "Jul 25, 2:00 PM @ Alchemist Arena"
        ),
        DiscoverReel(
          id = "dr3",
          title = "Mombasa Go-Kart Grand Prix Highlights",
          category = "Adrenaline Sports",
          creator = "@karting_gurus",
          duration = "1:30",
          initialViews = 24200,
          initialLikes = 5310,
          gradientColors = listOf(Color(0xFFF59E0B), Color(0xFFEF4444)),
          emoji = "🏎️",
          activityDescription = "Breathtaking multi-lap drone capture of the high-speed midnight drift final. Speed tracking overlay and crowd reaction cams included.",
          nextEventDate = "Jul 29, 10:00 PM @ Nyali Go-Kart Track"
        ),
        DiscoverReel(
          id = "dr4",
          title = "Rongai Forest Offroad Bike Climb",
          category = "Group Quests",
          creator = "@forest_trailblazers",
          duration = "0:58",
          initialViews = 11200,
          initialLikes = 2190,
          gradientColors = listOf(Color(0xFF10B981), Color(0xFF047857)),
          emoji = "🚲",
          activityDescription = "First-person helmet cam tackling the muddy technical drops and steep climbs of the deep Rongai woods. Join the weekly forest trailblazer rides!",
          nextEventDate = "Sunday, 8:00 AM @ Rongai Forest Entrance"
        ),
        DiscoverReel(
          id = "dr5",
          title = "Modernist Afro-Fusion Cooking Battle",
          category = "Culinary Arts",
          creator = "@chef_nairobi_modern",
          duration = "2:15",
          initialViews = 14800,
          initialLikes = 3900,
          gradientColors = listOf(Color(0xFFEC4899), Color(0xFFF43F5E)),
          emoji = "🍳",
          activityDescription = "Dynamic recap of the ultimate kitchen shootout styling traditional cassava and flame-grilled brisket. Real-time ingredient mystery boxes and tasting panel verdicts.",
          nextEventDate = "Jul 28, 7:00 PM @ Kilimani Food Yard"
        )
      )
    )
  }

  var hobbyCircles by remember {
    mutableStateOf(
      listOf(
        HobbyCircle(
          id = "hc1",
          name = "Nairobi Boardgames & RPG Guild",
          activeMembers = 680,
          activeOnline = 24,
          currentQuest = "Defeat the Red Dragon & Draft a Craft Beer",
          tier = "🏆 Diamond Tier • 12D Streak",
          emoji = "🎲",
          isJoined = false
        ),
        HobbyCircle(
          id = "hc2",
          name = "Sunset Peak Hiking & Peak Chasers",
          activeMembers = 1250,
          activeOnline = 45,
          currentQuest = "Conquer the Ngong Hills 5-Peak Speed Run",
          tier = "⛰️ Gold Tier • Active Daily",
          emoji = "🥾",
          isJoined = false
        ),
        HobbyCircle(
          id = "hc3",
          name = "Vector Art & Cyberpunk Creative Lab",
          activeMembers = 340,
          activeOnline = 18,
          currentQuest = "Co-create Nairobi 2099 Vector Mural",
          tier = "🎨 Creative Tier • Weekly Workshop",
          emoji = "👾",
          isJoined = false
        ),
        HobbyCircle(
          id = "hc4",
          name = "Acoustic Fire pit Sessions Nairobi",
          activeMembers = 520,
          activeOnline = 14,
          currentQuest = "Compose Collective Campfire Jam Songbook",
          tier = "🎸 Soul Tier • 5D Streak",
          emoji = "🔥",
          isJoined = false
        )
      )
    )
  }

  var swarmMissions by remember {
    mutableStateOf(
      listOf(
        SwarmMission(
          id = "sm1",
          title = "Westlands Mall Geo-Checkin Stampede",
          requiredCount = 30,
          currentCount = 24,
          rewardXp = 400,
          emoji = "🛍️",
          isCheckedIn = false
        ),
        SwarmMission(
          id = "sm2",
          title = "Silent Disco Midnight Flash Mob",
          requiredCount = 50,
          currentCount = 42,
          rewardXp = 600,
          emoji = "🎪",
          isCheckedIn = false
        ),
        SwarmMission(
          id = "sm3",
          title = "Morning Park Run Swarm - Uhuru Park",
          requiredCount = 20,
          currentCount = 11,
          rewardXp = 300,
          emoji = "🏃",
          isCheckedIn = false
        )
      )
    )
  }

  val mapMarkers = remember {
    listOf(
      ChronosMapMarker("m1", "Main Stage - Sunset Set", "Live Stages", 0.25f, 0.45f, "Live Photo Capture: Active", 142, true),
      ChronosMapMarker("m2", "Culinary Pavilion", "Culinary Hotspots", 0.75f, 0.3f, "Live Photo Capture: Active", 84, true),
      ChronosMapMarker("m3", "Neon Gallery", "Art Vectors", 0.45f, 0.75f, "Live Photo Capture: Idle", 65, false)
    )
  }

  val galleryPhotos = remember {
    listOf(
      ChronosPhotoItem("p1", "Ethereal Sunrise", "Main Stage - Sunset Set", "06:14 AM", listOf("Me", "Alex"), "Golden Hour 🌅", listOf(Color(0xFF1E293B), Color(0xFF1F2937)), "🕺"),
      ChronosPhotoItem("p2", "Techno Pulse", "Main Stage - Sunset Set", "11:42 PM", listOf("Alex", "Sarah"), "Neon Synthwave 🌆", listOf(Color(0xFF0F172A), Color(0xFF1E1B4B)), "🎧"),
      ChronosPhotoItem("p3", "Whiskey Glow", "Culinary Pavilion", "08:15 PM", listOf("Me"), "Retro VHS 📼", listOf(Color(0xFF111827), Color(0xFF311005)), "🥃"),
      ChronosPhotoItem("p4", "Neon Installation", "Neon Gallery", "09:30 PM", listOf("Sarah"), "Cyberpunk 👾", listOf(Color(0xFF020617), Color(0xFF0B132B)), "🎨"),
      ChronosPhotoItem("p5", "Late Night Session", "Main Stage - Sunset Set", "02:10 AM", listOf("Unknown"), "Retro VHS 📼", listOf(Color(0xFF090D16), Color(0xFF1F1235)), "🎹")
    )
  }

  val basePrice = 1500.0
  val r = 0.15
  val N = selectedPhotoIds.size
  val rawTotal = N * basePrice
  val discountFactor = if (N > 1) Math.pow(1.0 - r, (N - 1).toDouble()) else 1.0
  val finalTotal = (rawTotal * discountFactor).coerceAtLeast(rawTotal * 0.55)
  val discountPercent = if (N > 1) ((rawTotal - finalTotal) / rawTotal * 100).toInt() else 0
  val splitCost = finalTotal / 3.0

  Box(modifier = Modifier.fillMaxSize().background(getAppBg())) {
    Column(modifier = Modifier.fillMaxSize()) {
      Row(
        modifier = Modifier.fillMaxWidth().background(getSurfaceBg()).statusBarsPadding().padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        listOf("home" to "🧭 Home Feed", "discover" to "🗺️ Discover", "gallery" to "🎭 Squad Vibes").forEach { (tabId, label) ->
          val isActive = activeSubTab == tabId
          Box(
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
              .background(if (isActive) Color(0xFFF4B942).copy(alpha = 0.15f) else Color.Transparent)
              .border(1.dp, if (isActive) Color(0xFFF4B942) else getBorderColor(), RoundedCornerShape(8.dp))
              .clickable { activeSubTab = tabId; triggerHud("Switched to ${tabId.uppercase()}") }
              .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(label, color = if (isActive) Color(0xFFF4B942) else getTextSecondary(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }

      when (activeSubTab) {
        "home" -> {
          Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Location Bar Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .background(getSurfaceBg(), RoundedCornerShape(8.dp))
                  .border(1.dp, getBorderColor(), RoundedCornerShape(8.dp))
                  .clickable { onShowLocationDialogChange(true) }
                  .padding(horizontal = 12.dp, vertical = 10.dp)
              ) {
                Text("📍 $selectedLocation", color = getTextPrimary(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }

              Box(
                modifier = Modifier
                  .weight(1f)
                  .background(getSurfaceBg(), RoundedCornerShape(8.dp))
                  .border(1.dp, getBorderColor(), RoundedCornerShape(8.dp))
                  .padding(10.dp)
              ) {
                if (searchQuery.isEmpty()) Text("🔍 Search events, drinks, or kits...", color = getTextSecondary(), fontSize = 12.sp)
                BasicTextFieldMock(searchQuery, onSearchQueryChange, getTextPrimary(), 12.sp)
              }
            }

            // 🌟 EXCLUSIVE CATEGORY SHELF SELECTOR (SUPERMARKET SHELF ACCENT)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text("🏪 DIGITAL SUPERMARKET SHELFS", color = getTextSecondary(), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
              LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                val homeCategories = listOf("All", "🍽️ Fine Dining", "🎪 Festivals", "🐆 Expeditions", "🎨 Art & Soul", "🍹 Liquid Kits", "🛎️ Hosted Classes", "🎈 Kids Celebrations")
                items(homeCategories) { cat ->
                  val cleanCatName = cat.substringAfter(" ")
                  val isSelected = selectedPremiumCategory == cat || selectedPremiumCategory == cleanCatName || (selectedPremiumCategory == "All" && cat == "All")
                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(16.dp))
                      .background(if (isSelected) Color(0xFFF4B942) else getSurfaceBg())
                      .border(1.dp, if (isSelected) Color(0xFFF4B942) else getBorderColor(), RoundedCornerShape(16.dp))
                      .clickable {
                        selectedPremiumCategory = cat
                        triggerHud("Browsing $cat shelf...")
                      }
                      .padding(horizontal = 14.dp, vertical = 8.dp)
                  ) {
                    Text(cat, color = if (isSelected) Color(0xFF150C24) else getTextSecondary(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }

            val cleanSelectedCategory = selectedPremiumCategory.substringAfter(" ")
            val showAll = selectedPremiumCategory == "All" || selectedPremiumCategory == ""

            // AISLE 0.5: KUEST JUNIOR: WHOLE-DAY KIDS BIRTHDAYS & CELEBRATIONS
            val showKidsCelebrations = showAll || cleanSelectedCategory.contains("Kids", ignoreCase = true) || cleanSelectedCategory.contains("Celebrations", ignoreCase = true) || cleanSelectedCategory.contains("Party", ignoreCase = true) || cleanSelectedCategory.contains("Parties", ignoreCase = true)
            if (showKidsCelebrations) {
              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🎈", fontSize = 15.sp)
                    Text(
                      text = "KUEST JUNIOR • VIP BIRTHDAYS",
                      color = getTextPrimary(),
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Black,
                      letterSpacing = 1.sp
                    )
                  }
                  Box(
                    modifier = Modifier
                      .background(Color(0xFFF4B942).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                      .padding(horizontal = 6.dp, vertical = 2.dp)
                  ) {
                    Text("🎂 PARTY VIBEZ", color = Color(0xFFF4B942), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                  }
                }
                Text(
                  text = "Full-day premium lawn parties with vetted host crews. Secured & locked safely in vault escrow! 🔒🎈",
                  color = getTextSecondary(),
                  fontSize = 11.sp
                )

                // 2-Column YouTube Shorts Inspired Layout Grid! (using Row + weight)
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                  // Card 1: VIP Kids Birthday Carnival (Lawn Package)
                  Card(
                    colors = CardDefaults.cardColors(containerColor = getSurfaceBg()),
                    border = BorderStroke(1.dp, getBorderColor()),
                    modifier = Modifier
                      .weight(1f)
                      .height(240.dp)
                      .clickable {
                        val escrowItem = EscrowItem(
                          id = "junior_carnival_${System.currentTimeMillis()}",
                          title = "VIP Kids Birthday Carnival Escrow Deposit",
                          merchant = "@kuest_junior_events",
                          amount = 75000.0,
                          status = "Funds Held",
                          badgeText = "🎈 Kids Party Escrow"
                        )
                        onTriggerCheckout(escrowItem, premiumQuests[0])
                        triggerHud("Locking 50% VIP Carnival booking deposit in escrow...")
                      }
                  ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                      Image(
                        painter = painterResource(id = R.drawable.img_kids_party_1784391110613),
                        contentDescription = "Premium VIP Lawn Carnival",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                      )
                      // Top Overlay elements
                      Row(
                        modifier = Modifier
                          .fillMaxWidth()
                          .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Box(
                          modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                          Text("👑 VIP LAWN", color = Color(0xFFF4B942), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                        // Options menu icon mimicking Shorts interface
                        Box(
                          modifier = Modifier
                            .size(20.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .clickable {
                              triggerHud("ℹ️ VIP Lawn includes 3-tier custom cake, live bubble artist, themed bounce castle, and safety hosts.")
                            },
                          contentAlignment = Alignment.Center
                        ) {
                          Text("⋮", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                      }

                      // Bottom Overlay scrim + info
                      Box(
                        modifier = Modifier
                          .fillMaxWidth()
                          .align(Alignment.BottomStart)
                          .background(
                            Brush.verticalGradient(
                              colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                            )
                          )
                          .padding(10.dp)
                      ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                          Text(
                            text = "VIP Kids Carnival",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                          )
                          Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                          ) {
                            Text(
                              text = "From KES 75k",
                              color = Color(0xFFF4B942),
                              fontSize = 10.sp,
                              fontWeight = FontWeight.Bold
                            )
                            Box(
                              modifier = Modifier
                                .background(Color(0xFFF4B942), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                              Text("BOOK", color = Color(0xFF150C24), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                          }
                        }
                      }
                    }
                  }

                  // Card 2: Backyard Glamping Adventure (At-Home Package)
                  Card(
                    colors = CardDefaults.cardColors(containerColor = getSurfaceBg()),
                    border = BorderStroke(1.dp, getBorderColor()),
                    modifier = Modifier
                      .weight(1f)
                      .height(240.dp)
                      .clickable {
                        val escrowItem = EscrowItem(
                          id = "junior_glamping_${System.currentTimeMillis()}",
                          title = "Backyard Glamping Adventure Escrow Deposit",
                          merchant = "@kuest_junior_events",
                          amount = 45000.0,
                          status = "Funds Held",
                          badgeText = "🎈 Kids Party Escrow"
                        )
                        onTriggerCheckout(escrowItem, premiumQuests[0])
                        triggerHud("Locking 50% Backyard Glamping booking deposit in escrow...")
                      }
                  ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                      Image(
                        painter = painterResource(id = R.drawable.img_kids_home_party_1784399790523),
                        contentDescription = "Backyard Glamping Adventure",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                      )
                      // Top Overlay elements
                      Row(
                        modifier = Modifier
                          .fillMaxWidth()
                          .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Box(
                          modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                          Text("🏡 GLAMPING", color = Color(0xFFF4B942), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                        // Options menu icon mimicking Shorts interface
                        Box(
                          modifier = Modifier
                            .size(20.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .clickable {
                              triggerHud("ℹ️ Backyard Glamping includes custom teepee tents, fairy light canopy, movie projector, & kids favor boxes.")
                            },
                          contentAlignment = Alignment.Center
                        ) {
                          Text("⋮", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                      }

                      // Bottom Overlay scrim + info
                      Box(
                        modifier = Modifier
                          .fillMaxWidth()
                          .align(Alignment.BottomStart)
                          .background(
                            Brush.verticalGradient(
                              colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                            )
                          )
                          .padding(10.dp)
                      ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                          Text(
                            text = "Backyard Adventure",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                          )
                          Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                          ) {
                            Text(
                              text = "From KES 45k",
                              color = Color(0xFFF4B942),
                              fontSize = 10.sp,
                              fontWeight = FontWeight.Bold
                            )
                            Box(
                              modifier = Modifier
                                .background(Color(0xFFF4B942), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                              Text("BOOK", color = Color(0xFF150C24), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }

            // AISLE 1: EVENT PROMOTIONS & LIVE EXPERIENCES (WHAT YOU SEE IS WHAT YOU GET)
            val filteredQuests = premiumQuests.filter { quest ->
              val isHosted = quest.category.equals("Hosted Classes", ignoreCase = true) || quest.id.startsWith("host_q_")
              if (showAll) {
                !isHosted
              } else {
                quest.category.equals(cleanSelectedCategory, ignoreCase = true) || quest.category.contains(cleanSelectedCategory, ignoreCase = true)
              }
            }

            if (filteredQuests.isNotEmpty()) {
              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                  Text("🎪 FEATURED TICKETS & EXPERIENCE PASSES", color = getTextPrimary(), fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                  if (!showAll) {
                    Box(modifier = Modifier.background(Color(0xFFF4B942).copy(alpha = 0.15f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                      Text("FILTERED", color = Color(0xFFF4B942), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                }
                Text("Lock tickets securely in escrow. Scan QR or present custom handshake code at checkout to release funds instantly to organizers.", color = getTextSecondary(), fontSize = 11.sp)

                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                  items(filteredQuests) { quest ->
                    Card(
                      colors = CardDefaults.cardColors(containerColor = getSurfaceBg()),
                      border = BorderStroke(1.dp, getBorderColor()),
                      modifier = Modifier.width(280.dp)
                    ) {
                      Column {
                        // Image Thumbnail Header (Visual supermarket-style layout)
                        Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                          if (quest.imageRes != null) {
                            Image(
                              painter = painterResource(id = quest.imageRes),
                              contentDescription = quest.title,
                              modifier = Modifier.fillMaxSize(),
                              contentScale = ContentScale.Crop
                            )
                          } else {
                            Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.05f)))
                          }
                          // Overlay category
                          Box(
                            modifier = Modifier
                              .align(Alignment.TopStart)
                              .padding(8.dp)
                              .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                              .padding(horizontal = 6.dp, vertical = 2.dp)
                          ) {
                            Text(quest.category.uppercase(), color = Color(0xFFF4B942), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                          }
                          // Overlay rating
                          Box(
                            modifier = Modifier
                              .align(Alignment.TopEnd)
                              .padding(8.dp)
                              .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                              .padding(horizontal = 6.dp, vertical = 2.dp)
                          ) {
                            Text("⭐ ${quest.rating}", color = Color(0xFFF4B942), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                          }
                        }

                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                          Text(quest.title, color = getTextPrimary(), fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1)
                          Text(quest.subtitle, color = getTextSecondary(), fontSize = 10.sp, maxLines = 1)
                          Text("📍 ${quest.location}", color = getTextSecondary(), fontSize = 10.sp, maxLines = 1)
                          Text("📅 ${quest.date} • ${quest.capacity}", color = Color(0xFFF4B942), fontSize = 9.sp, fontWeight = FontWeight.Bold)

                          Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(getBorderColor()))
                          
                          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                              Text("TICKET PRICE", color = getTextSecondary(), fontSize = 8.sp)
                              Text("KES ${String.format("%,.0f", quest.price)}", color = Color(0xFFF4B942), fontSize = 12.sp, fontWeight = FontWeight.Black)
                            }

                            Button(
                              onClick = { selectedQuestForDetail = quest },
                              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF4B942)),
                              contentPadding = PaddingValues(horizontal = 10.dp),
                              modifier = Modifier.height(28.dp)
                            ) {
                              Text("RSVP / BUY", color = Color(0xFF150C24), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                              onClick = {
                                preOrderEventName = quest.title
                                triggerHud("Premium Liquid Pre-order channel initialized!")
                              },
                              colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                              border = BorderStroke(1.dp, getBorderColor()),
                              contentPadding = PaddingValues(horizontal = 4.dp),
                              modifier = Modifier.height(28.dp)
                            ) {
                              Text("🍹 PRE", color = getTextPrimary(), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }

            // AISLE 1.5: 🔥 WEEKLY HOTLIST: THIS WEEK'S EXCLUSIVES (Niche Events, Club DJ Sets & Festivals)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                  Text("🔥", fontSize = 15.sp)
                  Text(
                    text = "WEEKLY HOTLIST • PLANNED FOR THIS WEEK",
                    color = getTextPrimary(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                  )
                }
                Box(
                  modifier = Modifier
                    .background(Color(0xFFF4B942).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                  Text("NICHE & HIGHLIGHTS", color = Color(0xFFF4B942), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
              }
              Text(
                text = "Boutique music festivals, high-energy club DJ bookings, and curated niche events happening strictly this week. Guaranteed escrow security.",
                color = getTextSecondary(),
                fontSize = 11.sp
              )

              LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Item 1: Club Booking (DJ Set)
                item {
                  Card(
                    colors = CardDefaults.cardColors(containerColor = getSurfaceBg()),
                    border = BorderStroke(1.dp, getBorderColor()),
                    modifier = Modifier
                      .width(260.dp)
                      .clickable {
                        val escrowItem = EscrowItem(
                          id = "week_dj_${System.currentTimeMillis()}",
                          title = "Alchemist Boiler Room Ticket Deposit",
                          merchant = "@alchemist_lounge",
                          amount = 1500.0,
                          status = "Funds Held",
                          badgeText = "🎫 Club DJ Set"
                        )
                        onTriggerCheckout(escrowItem, premiumQuests[0])
                        triggerHud("Securing Boiler Room entry ticket with Escrow protection...")
                      }
                  ) {
                    Column {
                      Box(modifier = Modifier.fillMaxWidth().height(125.dp)) {
                        Image(
                          painter = painterResource(id = R.drawable.img_concert_1784384001360),
                          contentDescription = "Alchemist Boiler Room Set",
                          modifier = Modifier.fillMaxSize(),
                          contentScale = ContentScale.Crop
                        )
                        Box(
                          modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                          Text("📅 WEDNESDAY (TONIGHT)", color = Color(0xFFF4B942), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                          modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                          Text("CLUB BOOKING", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                      }

                      Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                          text = "Boiler Room: Afro-House Set",
                          color = getTextPrimary(),
                          fontSize = 12.sp,
                          fontWeight = FontWeight.Black,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                        )
                        Text(
                          text = "Booked by: @the_alchemist_bar",
                          color = getTextSecondary(),
                          fontSize = 10.sp,
                          fontWeight = FontWeight.Medium
                        )
                        Text(
                          text = "360° dancefloor with live sets by Nairobi's leading electronic music crate-diggers. Immersive audio experience.",
                          color = getTextSecondary(),
                          fontSize = 10.sp,
                          maxLines = 2,
                          overflow = TextOverflow.Ellipsis
                        )

                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Column {
                            Text("TICKET PRICE", color = getTextSecondary(), fontSize = 8.sp)
                            Text("KES 1,500", color = Color(0xFFF4B942), fontSize = 11.sp, fontWeight = FontWeight.Black)
                          }
                          Box(
                            modifier = Modifier
                              .background(Color(0xFFF4B942), RoundedCornerShape(4.dp))
                              .padding(horizontal = 8.dp, vertical = 4.dp)
                          ) {
                            Text("BUY TICKET", color = Color(0xFF150C24), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                          }
                        }
                      }
                    }
                  }
                }

                // Item 2: Music Festival
                item {
                  Card(
                    colors = CardDefaults.cardColors(containerColor = getSurfaceBg()),
                    border = BorderStroke(1.dp, getBorderColor()),
                    modifier = Modifier
                      .width(260.dp)
                      .clickable {
                        val escrowItem = EscrowItem(
                          id = "week_fest_${System.currentTimeMillis()}",
                          title = "Rift Neon Balloon Fest Pass",
                          merchant = "@rift_canopy_network",
                          amount = 4500.0,
                          status = "Funds Held",
                          badgeText = "🎫 Music Festival"
                        )
                        onTriggerCheckout(escrowItem, premiumQuests[0])
                        triggerHud("Securing Rift Festival VIP Pass with Escrow protection...")
                      }
                  ) {
                    Column {
                      Box(modifier = Modifier.fillMaxWidth().height(125.dp)) {
                        Image(
                          painter = painterResource(id = R.drawable.img_rift_balloon_quest_1784241619424),
                          contentDescription = "Rift Neon Balloon Fest",
                          modifier = Modifier.fillMaxSize(),
                          contentScale = ContentScale.Crop
                        )
                        Box(
                          modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                          Text("📅 SATURDAY", color = Color(0xFFF4B942), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                          modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                          Text("FESTIVAL", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                      }

                      Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                          text = "Rift Neon Balloon Music Fest",
                          color = getTextPrimary(),
                          fontSize = 12.sp,
                          fontWeight = FontWeight.Black,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                        )
                        Text(
                          text = "Booked by: @rift_canopy_network",
                          color = getTextSecondary(),
                          fontSize = 10.sp,
                          fontWeight = FontWeight.Medium
                        )
                        Text(
                          text = "Glow-in-the-dark hot air balloons, multi-genre stage lineups, craft food trucks, and overnight campsite integration.",
                          color = getTextSecondary(),
                          fontSize = 10.sp,
                          maxLines = 2,
                          overflow = TextOverflow.Ellipsis
                        )

                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Column {
                            Text("TICKET PRICE", color = getTextSecondary(), fontSize = 8.sp)
                            Text("KES 4,500", color = Color(0xFFF4B942), fontSize = 11.sp, fontWeight = FontWeight.Black)
                          }
                          Box(
                            modifier = Modifier
                              .background(Color(0xFFF4B942), RoundedCornerShape(4.dp))
                              .padding(horizontal = 8.dp, vertical = 4.dp)
                          ) {
                            Text("BUY TICKET", color = Color(0xFF150C24), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                          }
                        }
                      }
                    }
                  }
                }

                // Item 3: Niche Event (Ladies Only)
                item {
                  Card(
                    colors = CardDefaults.cardColors(containerColor = getSurfaceBg()),
                    border = BorderStroke(1.dp, getBorderColor()),
                    modifier = Modifier
                      .width(260.dp)
                      .clickable {
                        val escrowItem = EscrowItem(
                          id = "week_ladies_${System.currentTimeMillis()}",
                          title = "Heels & Cocktails Rooftop Pass",
                          merchant = "@skylounge_kilimani",
                          amount = 2500.0,
                          status = "Funds Held",
                          badgeText = "🎫 Niche Event"
                        )
                        onTriggerCheckout(escrowItem, premiumQuests[0])
                        triggerHud("Securing Ladies-Only Rooftop Social Pass with Escrow...")
                      }
                  ) {
                    Column {
                      Box(modifier = Modifier.fillMaxWidth().height(125.dp)) {
                        Image(
                          painter = painterResource(id = R.drawable.img_ladies_niche_1784404313061),
                          contentDescription = "Heels & Cocktails Rooftop",
                          modifier = Modifier.fillMaxSize(),
                          contentScale = ContentScale.Crop
                        )
                        Box(
                          modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                          Text("📅 FRIDAY", color = Color(0xFFF4B942), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                          modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                          Text("LADIES ONLY", color = Color(0xFFF4B942), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                      }

                      Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                          text = "Heels & Cocktails Rooftop Social",
                          color = getTextPrimary(),
                          fontSize = 12.sp,
                          fontWeight = FontWeight.Black,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                        )
                        Text(
                          text = "Booked by: @skylounge_kilimani",
                          color = getTextSecondary(),
                          fontSize = 10.sp,
                          fontWeight = FontWeight.Medium
                        )
                        Text(
                          text = "Niche networking with female startup founders, creators, and professionals. Free-flow select dynamic cocktails.",
                          color = getTextSecondary(),
                          fontSize = 10.sp,
                          maxLines = 2,
                          overflow = TextOverflow.Ellipsis
                        )

                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Column {
                            Text("TICKET PRICE", color = getTextSecondary(), fontSize = 8.sp)
                            Text("KES 2,500", color = Color(0xFFF4B942), fontSize = 11.sp, fontWeight = FontWeight.Black)
                          }
                          Box(
                            modifier = Modifier
                              .background(Color(0xFFF4B942), RoundedCornerShape(4.dp))
                              .padding(horizontal = 8.dp, vertical = 4.dp)
                          ) {
                            Text("BUY TICKET", color = Color(0xFF150C24), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                          }
                        }
                      }
                    }
                  }
                }
              }
            }

            // AISLE 2: LIQUID EXCLUSIVES & PARTY KITS (SUPERMARKET SHELF STYLE)
            val showLiquidKits = showAll || cleanSelectedCategory.contains("Liquid", ignoreCase = true) || cleanSelectedCategory.contains("Kit", ignoreCase = true)
            if (showLiquidKits) {
              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                  Text("🍹 LIQUID EXCLUSIVES & PRE-GAME DRINK KITS", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                  Box(modifier = Modifier.background(Color(0xFFF4B942).copy(alpha = 0.12f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text("⚡ 45m Delivery", color = Color(0xFFF4B942), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                  }
                }
                Text("Curated spirits, premium mixers, clear-sphere ice, and designer glasses delivered pre-chilled direct to your party coordinates.", color = Color.Gray, fontSize = 11.sp)

                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                  items(hostingKits) { kit ->
                    Card(
                      colors = CardDefaults.cardColors(containerColor = Color(0xFF121216)),
                      border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                      modifier = Modifier.width(260.dp).clickable { selectedHostingKitForDetail = kit }
                    ) {
                      Column {
                        // Product image
                        Box(modifier = Modifier.fillMaxWidth().height(130.dp)) {
                          if (kit.imageRes != null) {
                            Image(
                              painter = painterResource(id = kit.imageRes),
                              contentDescription = kit.title,
                              modifier = Modifier.fillMaxSize(),
                              contentScale = ContentScale.Crop
                            )
                          } else {
                            Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.05f)))
                          }
                          // Price badge overlaid
                          Box(
                            modifier = Modifier
                              .align(Alignment.BottomEnd)
                              .padding(8.dp)
                              .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                              .padding(horizontal = 6.dp, vertical = 2.dp)
                          ) {
                            Text("KES ${String.format("%,.0f", kit.price)}", color = Color(0xFFF4B942), fontSize = 11.sp, fontWeight = FontWeight.Black)
                          }
                        }

                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                          Text(kit.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1)
                          Text(kit.description, color = Color.Gray, fontSize = 10.sp, maxLines = 2)

                          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                              Text("❄️", fontSize = 11.sp)
                              Text("Chilled Box", color = Color.LightGray, fontSize = 9.sp)
                            }
                            
                            Button(
                              onClick = {
                                val escrowItem = EscrowItem(
                                  id = System.currentTimeMillis().toString(),
                                  title = "Hosting Kit: ${kit.title}",
                                  merchant = "@kuest_liquid_lounge",
                                  amount = kit.price,
                                  status = "Funds Held",
                                  badgeText = "🍹 Premium Liquids"
                                )
                                onTriggerCheckout(escrowItem, premiumQuests[0])
                                triggerHud("Locking Escrow for ${kit.title}...")
                              },
                              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF4B942)),
                              contentPadding = PaddingValues(horizontal = 10.dp),
                              modifier = Modifier.height(28.dp)
                            ) {
                              Text("ADD TO CART", color = Color(0xFF150C24), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }

            // AISLE 3: CURATED CULINARY NODES (Space-Saving YouTube Shorts Video Shelf style)
            val showCulinary = showAll || cleanSelectedCategory.contains("Dining", ignoreCase = true) || cleanSelectedCategory.contains("Culinary", ignoreCase = true)
            if (showCulinary) {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("📺", fontSize = 14.sp)
                    Text("CULINARY VIDEO ADS • SWIPE HOT DROPS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                  }
                  Box(
                    modifier = Modifier
                      .background(Color.Red.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                      .padding(horizontal = 6.dp, vertical = 2.dp)
                  ) {
                    Text("LIVE FEED", color = Color.Red, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                  }
                }
                Text(
                  text = "Addictive culinary mini-vlogs by independent kitchens, hotels, premium supermarkets, and home cooks. Watch video previews and tap to order instantly securely locked in escrow.",
                  color = Color.Gray,
                  fontSize = 11.sp
                )

                LazyRow(
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  items(culinaryCurations.size) { index ->
                    val curation = culinaryCurations[index]
                    Card(
                      colors = CardDefaults.cardColors(containerColor = Color(0xFF150C24)),
                      border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                      modifier = Modifier
                        .width(155.dp)
                        .height(245.dp)
                        .clickable {
                          val escrowItem = EscrowItem(
                            id = "food_${curation.id}_${System.currentTimeMillis()}",
                            title = "Culinary Node: ${curation.itemTitle}",
                            merchant = curation.partnerName,
                            amount = curation.price,
                            status = "Funds Held",
                            badgeText = "🍽️ Curated Culinary"
                          )
                          onTriggerCheckout(escrowItem, premiumQuests[0])
                          triggerHud("Securing ${curation.itemTitle} order via escrow protection! 🔒")
                        }
                    ) {
                      Box(modifier = Modifier.fillMaxSize()) {
                        // 1. Image as video background
                        if (curation.imageRes != null) {
                          Image(
                            painter = painterResource(id = curation.imageRes),
                            contentDescription = curation.itemTitle,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                          )
                        } else {
                          Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.05f)))
                        }

                        // 2. Translucent dark overlay with gradient
                        Box(
                          modifier = Modifier
                            .fillMaxSize()
                            .background(
                              androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                  Color.Black.copy(alpha = 0.3f),
                                  Color.Transparent,
                                  Color.Black.copy(alpha = 0.85f)
                                )
                              )
                            )
                        )

                        // 3. Top category label & option dots
                        Row(
                          modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Box(
                            modifier = Modifier
                              .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                              .border(1.dp, Color(0xFFF4B942).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                              .padding(horizontal = 4.dp, vertical = 2.dp)
                          ) {
                            Text(
                              text = curation.sourceType,
                              color = Color(0xFFF4B942),
                              fontSize = 7.sp,
                              fontWeight = FontWeight.Bold
                            )
                          }
                          Text("⋮", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // 4. Play Button Overlay (YouTube Shorts style)
                        Box(
                          modifier = Modifier
                            .align(Alignment.Center)
                            .size(34.dp)
                            .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                          contentAlignment = Alignment.Center
                        ) {
                          Text("▶", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(start = 2.dp))
                        }

                        // 5. Bottom Overlay with Info and BUY button
                        Column(
                          modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp),
                          verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                          Text(
                            text = curation.itemTitle,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                          )
                          
                          Text(
                            text = curation.partnerName,
                            color = Color(0xFFF4B942),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                          )

                          Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                          ) {
                            Column {
                              Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("⭐", fontSize = 8.sp)
                                Text("${curation.rating}", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                              }
                              Text("KES ${String.format("%,.0f", curation.price)}", color = Color.LightGray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }

                            Box(
                              modifier = Modifier
                                .background(Color(0xFFF4B942), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                              Text("ORDER", color = Color(0xFF150C24), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }



            // AISLE 4: BODA EXPRESS RUNNERS (Highly Simplified Premium Rider Grid)
            Card(
              colors = CardDefaults.cardColors(containerColor = Color(0xFF150C24)),
              border = BorderStroke(1.dp, Color(0xFFF4B942).copy(alpha = 0.3f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                  Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🏍️", fontSize = 14.sp)
                    Text("BODA EXPRESS 🏍️💨", color = Color(0xFFF4B942), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                  }
                  Box(modifier = Modifier.background(Color(0xFFF4B942).copy(alpha = 0.12f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text("⚡ FASTA RUNS", color = Color(0xFFF4B942), fontSize = 7.sp, fontWeight = FontWeight.Bold)
                  }
                }

                Text(
                  text = "Need a quick run? Select a certified Rider below to dispatch instant secure errands. ⚡💨",
                  color = Color.LightGray,
                  fontSize = 11.sp
                )

                // 1. Horizontal Rider Grid (Selfies as Pickable Avatars)
                val ridersList = remember {
                  listOf(
                    Triple("Turbo", R.drawable.img_rider_selfie_1_1784405445693, "Active in Kilimani • 2 mins away • ⭐ 4.9"),
                    Triple("Apex", R.drawable.img_rider_selfie_2_1784405457461, "Active in Westlands • 5 mins away • ⭐ 5.0"),
                    Triple("Viper", R.drawable.img_rider_selfie_3_1784405496905, "Active in Kileleshwa • 8 mins away • ⭐ 4.8")
                  )
                }

                // If no rider is selected, default to Turbo so it pops immediately
                val currentRider = selectedRiderNickname ?: "Turbo"
                val activeRiderTriple = ridersList.find { it.first == currentRider } ?: ridersList[0]

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceEvenly,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  ridersList.forEach { rider ->
                    val isSelected = currentRider == rider.first
                    Column(
                      horizontalAlignment = Alignment.CenterHorizontally,
                      verticalArrangement = Arrangement.spacedBy(4.dp),
                      modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedRiderNickname = rider.first }
                        .padding(4.dp)
                    ) {
                      Box(
                        modifier = Modifier
                          .size(54.dp)
                          .background(Color.Black, CircleShape)
                          .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Color(0xFFF4B942) else Color.White.copy(alpha = 0.15f),
                            shape = CircleShape
                          )
                          .padding(2.dp)
                      ) {
                        Image(
                          painter = painterResource(id = rider.second),
                          contentDescription = "Rider ${rider.first}",
                          modifier = Modifier.fillMaxSize().clip(CircleShape),
                          contentScale = ContentScale.Crop
                        )
                        // Live green dot
                        Box(
                          modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(10.dp)
                            .background(Color(0xFF2ECC71), CircleShape)
                            .border(1.5.dp, Color.Black, CircleShape)
                        )
                      }
                      
                      Text(
                        text = rider.first,
                        color = if (isSelected) Color(0xFFF4B942) else Color.White,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                      )
                    }
                  }
                }

                // Selected Rider Interactive Zone
                Card(
                  colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                  border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Column {
                        Text(
                          text = "RIDER: @${activeRiderTriple.first.lowercase()}",
                          color = Color.White,
                          fontSize = 11.sp,
                          fontWeight = FontWeight.Bold
                        )
                        Text(
                          text = activeRiderTriple.third,
                          color = Color(0xFFF4B942),
                          fontSize = 9.sp,
                          fontFamily = FontFamily.Monospace
                        )
                      }
                      Box(
                        modifier = Modifier
                          .background(Color(0xFF2ECC71).copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                          .padding(horizontal = 6.dp, vertical = 2.dp)
                      ) {
                        Text("⚡ CAN WAIT & CALL", color = Color(0xFF2ECC71), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                      }
                    }

                    Text(
                      text = "🔒 PRIVACY SECURED: Nicknames protect rider identities. Full legal documents are vetted & securely locked in KUEST Escrow.",
                      color = Color.Gray,
                      fontSize = 8.sp,
                      lineHeight = 10.sp
                    )

                    // Secure Call Back Quest Trigger
                    val callbackSent = riderCallbackRequestedMap[activeRiderTriple.first] ?: false
                    
                    if (callbackSent) {
                      Box(
                        modifier = Modifier
                          .fillMaxWidth()
                          .background(Color(0xFFF4B942).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                          .border(1.dp, Color(0xFFF4B942).copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                          .padding(8.dp),
                        contentAlignment = Alignment.Center
                      ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                          Text("📞", fontSize = 11.sp)
                          Text(
                            text = "Call-back Quest Sent to @${activeRiderTriple.first.lowercase()}! Expect a secure voice call in ~60s.",
                            color = Color(0xFFF4B942),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                          )
                        }
                      }
                    } else {
                      Button(
                        onClick = {
                          riderCallbackRequestedMap = riderCallbackRequestedMap + (activeRiderTriple.first to true)
                          triggerHud("Secure Call-back Quest sent! @${activeRiderTriple.first.lowercase()} will call you now 📞🔒")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF4B942)),
                        modifier = Modifier.fillMaxWidth().height(32.dp),
                        contentPadding = PaddingValues(0.dp)
                      ) {
                        Text(
                          text = "📞 REQUEST SECURE CALL FROM @${activeRiderTriple.first.uppercase()}",
                          color = Color(0xFF150C24),
                          fontSize = 9.sp,
                          fontWeight = FontWeight.Black
                        )
                      }
                    }

                    // Errand Text field Dispatch Zone
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                        .padding(6.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      Box(modifier = Modifier.weight(1f)) {
                        if (eventErrandInput.isEmpty()) {
                          Text("Custom errand (e.g. Bring charger, buy keys)...", color = Color.Gray, fontSize = 10.sp)
                        }
                        BasicTextFieldMock(eventErrandInput, { eventErrandInput = it }, Color.White, 10.sp)
                      }
                      
                      if (eventErrandInput.isNotBlank()) {
                        Button(
                          onClick = {
                            val newRun = "Errand with @${activeRiderTriple.first}: $eventErrandInput"
                            activeConciergeErrands = activeConciergeErrands + newRun
                            eventErrandInput = ""
                            triggerHud("Custom Quest dispatched to @${activeRiderTriple.first.lowercase()}! Escrow locked 🏍️🔒")
                          },
                          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF4B942)),
                          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                          modifier = Modifier.height(24.dp)
                        ) {
                          Text("DISPATCH", color = Color(0xFF150C24), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                      }
                    }
                  }
                }

                if (activeConciergeErrands.isNotEmpty()) {
                  Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))
                  Text("ACTIVE BODA RUNS IN PROGRESS:", color = Color(0xFFF4B942), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                  
                  activeConciergeErrands.forEach { run ->
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🏍️", fontSize = 12.sp)
                        Text(run, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                      }
                      Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(Color(0xFF2ECC71), CircleShape))
                        Text("Active Escrow 🔒", color = Color(0xFF2ECC71), fontSize = 9.sp)
                        Text("✕", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.clickable {
                          activeConciergeErrands = activeConciergeErrands - run
                          triggerHud("Concierge contract refunded back to explorer wallet 💰")
                        })
                      }
                    }
                  }
                }
              }
            }

            // AISLE 5: HOST & SPONSOR ANALYTICS LEDGER (Shown for Hosts or when expanding)
            if (isHostingModeActive) {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🎪 HOST & SPONSOR ANALYTICS LEDGER", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                hostEvents.forEach { event ->
                  Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF121216)), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                          Text(event.emoji, fontSize = 18.sp)
                          Column {
                            Text(event.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(event.tier, color = Color.Gray, fontSize = 10.sp)
                          }
                        }
                        Box(modifier = Modifier.background(if (event.status == "Live") Color.Red.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                          Text(event.status.uppercase(), color = if (event.status == "Live") Color.Red else Color.LightGray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                      }
                      Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.05f)))
                      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                          Text("TICKETS SOLD", color = Color.Gray, fontSize = 8.sp)
                          Text("${event.ticketsSold} / ${event.capacity}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                          Text("GROSS REVENUE", color = Color.Gray, fontSize = 8.sp)
                          Text("KES ${String.format("%,.0f", event.revenue)}", color = Color(0xFFF4B942), fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                      }
                    }
                  }
                }
              }
            }

            // Zero-Broker Errand Match summary banner (integrated premium look)
            Card(
              colors = CardDefaults.cardColors(containerColor = Color(0xFF150C24)),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
              modifier = Modifier.fillMaxWidth().clickable { activeSubTab = "discover" }
            ) {
              Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🤝", fontSize = 28.sp)
                Column(modifier = Modifier.weight(1f)) {
                  Text("Looking for standard courier contracts? 🏍️", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                  Text("Bypass third-party brokers. Dispatch custom couriers directly from the Discover tab above with zero commission leaks.", color = Color.Gray, fontSize = 11.sp)
                }
              }
            }

            // 📸 CHRONOS PHOTO COMPANION HINT
            Card(
              colors = CardDefaults.cardColors(containerColor = Color(0xFF150C24)),
              border = BorderStroke(1.dp, Color(0xFFF4B942).copy(alpha = 0.2f)),
              modifier = Modifier.fillMaxWidth().clickable { activeSubTab = "discover" }
            ) {
              Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("📸", fontSize = 28.sp)
                Column(modifier = Modifier.weight(1f)) {
                  Text("Looking for your photos? 🔍", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                  Text("You are currently in an active Chronos Camera Sync Zone! Head to the Discover and Gallery tabs above to capture your selfie hash and download your event photos.", color = Color.Gray, fontSize = 11.sp)
                }
              }
            }
          }
        }
        "discover" -> {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .verticalScroll(rememberScrollState())
              .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
          ) {
              // Spacing of categories is reconsidered for a premium feel with custom emojis
              val categoriesList = listOf(
                "All" to "🌟 All",
                "Secret Events" to "🤫 Secret Events",
                "Niche Hobbies" to "🛹 Niche Hobbies",
                "Adrenaline Sports" to "🧗 Adrenaline Sports",
                "Group Quests" to "⚔️ Group Quests",
                "Culinary Arts" to "🍳 Culinary Arts"
              )

              LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 4.dp)
              ) {
                items(categoriesList) { (id, label) ->
                  val isSelected = selectedDiscoverCategory == id
                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(20.dp))
                      .background(
                        if (isSelected) Color(0xFFF4B942) 
                        else Color(0xFF1F1433).copy(alpha = 0.6f)
                      )
                      .border(
                        width = 1.2.dp,
                        color = if (isSelected) Color(0xFFF4B942) else Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(20.dp)
                      )
                      .clickable { selectedDiscoverCategory = id }
                      .padding(horizontal = 16.dp, vertical = 10.dp)
                  ) {
                    Text(
                      text = label,
                      color = if (isSelected) Color(0xFF150C24) else Color.White.copy(alpha = 0.85f),
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      letterSpacing = 0.5.sp
                    )
                  }
                }
              }

              // 🛰️ KUEST DISCOVERY PULSE STATUS MOCKUP (Showing Active Live Sync)
              Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF120C1F)),
                border = BorderStroke(1.dp, Color(0xFFF4B942).copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                      // Pulsing red live indicator dot
                      Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Red))
                      Text(
                        text = "KUEST PULSE ENGINE • LIVE ACTIVE SYNC",
                        color = Color(0xFFF4B942),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                      )
                    }
                    Box(
                      modifier = Modifier
                        .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                      Text("ACTIVE ⚡", color = Color(0xFF10B981), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                  
                  Text(
                    text = "Your real-time campus operating system is live. Visual camera streams, active Geopins, and local meetup channels are synchronizing automatically.",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                  )
                  
                  // Active components status row
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    listOf(
                      "📸 Chronos Cam" to "SYNCED 🟢",
                      "🎥 Live Streams" to "4 REELS ACTIVE 🔴",
                      "🗺️ Squad Pins" to "8 LIVE 📍"
                    ).forEach { (label, statusText) ->
                      Box(
                        modifier = Modifier
                          .weight(1f)
                          .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                          .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                          .padding(8.dp)
                      ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                          Text(label, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                          Text(statusText, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                      }
                    }
                  }
                }
              }

            // Video / Highlight Reels Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text("🎬 DYNAMIC EVENT HIGHLIGHT REELS", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
              val filteredReels = discoverReels.filter { selectedDiscoverCategory == "All" || it.category == selectedDiscoverCategory }
              if (filteredReels.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(160.dp).background(Color(0xFF121216), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                  Text("No reels matching the '$selectedDiscoverCategory' category yet.", color = Color.Gray, fontSize = 11.sp)
                }
              } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                  items(filteredReels) { reel ->
                    Card(
                      colors = CardDefaults.cardColors(containerColor = Color(0xFF121216)),
                      border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                      modifier = Modifier.width(260.dp).clickable { selectedReelForDetail = reel }
                    ) {
                      Column(modifier = Modifier.fillMaxWidth()) {
                        // Reel Mock Thumbnail Card with vertical color gradient
                        Box(modifier = Modifier.fillMaxWidth().height(140.dp).background(Brush.verticalGradient(reel.gradientColors))) {
                          Text(reel.emoji, fontSize = 48.sp, modifier = Modifier.align(Alignment.Center))
                          
                          // Play overlay button
                          Box(
                            modifier = Modifier
                              .align(Alignment.Center)
                              .size(44.dp)
                              .clip(CircleShape)
                              .background(Color.Black.copy(alpha = 0.5f))
                              .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                              .clickable { triggerHud("Playing cinematic highlight for ${reel.title}!") }
                          ) {
                            Text("▶", color = Color.White, fontSize = 16.sp, modifier = Modifier.align(Alignment.Center).offset(x = 1.dp))
                          }

                          // Views count badge
                          Box(
                            modifier = Modifier
                              .align(Alignment.TopEnd)
                              .padding(8.dp)
                              .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                              .padding(horizontal = 6.dp, vertical = 2.dp)
                          ) {
                            Text("👁️ ${String.format("%,.0f", reel.initialViews.toDouble())} views", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                          }

                          // Duration badge
                          Box(
                            modifier = Modifier
                              .align(Alignment.BottomEnd)
                              .padding(8.dp)
                              .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                              .padding(horizontal = 4.dp, vertical = 2.dp)
                          ) {
                            Text(reel.duration, color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                          }

                          // Category tag
                          Box(
                            modifier = Modifier
                              .align(Alignment.BottomStart)
                              .padding(8.dp)
                              .background(Color(0xFFF4B942), RoundedCornerShape(4.dp))
                              .padding(horizontal = 6.dp, vertical = 2.dp)
                          ) {
                            Text(reel.category.uppercase(), color = Color(0xFF150C24), fontSize = 8.sp, fontWeight = FontWeight.Black)
                          }
                        }

                        // Text content and reaction buttons
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                          Text(reel.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1)
                          Text("Created by ${reel.creator}", color = Color(0xFFF4B942), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                          
                          Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.05f)))
                          
                          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                              // Like Button
                              Row(
                                modifier = Modifier
                                  .clickable {
                                    val updatedList = discoverReels.map { r ->
                                      if (r.id == reel.id) {
                                        val liked = r.initialLikes > reel.initialLikes
                                        r.copy(initialLikes = if (liked) reel.initialLikes else reel.initialLikes + 1)
                                      } else r
                                    }
                                    discoverReels = updatedList
                                    triggerHud("Reel Liked! +15 XP added to streak 🔥")
                                  },
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                              ) {
                                Text("🔥", fontSize = 13.sp)
                                Text("${reel.initialLikes}", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                              }
                              Text("💬 12", color = Color.Gray, fontSize = 11.sp)
                            }
                            
                            Box(
                              modifier = Modifier
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .clickable { selectedReelForDetail = reel }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                              Text("EXPLORE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }

            // Active Niche Hobby Circles (Group Activities)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Text("👥 OFFICIAL CAMPUS & CITY HOBBY CIRCLES", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
              hobbyCircles.forEach { circle ->
                Card(
                  colors = CardDefaults.cardColors(containerColor = Color(0xFF121216)),
                  border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(42.dp).background(Color.White.copy(alpha = 0.05f), CircleShape)) {
                      Text(circle.emoji, modifier = Modifier.align(Alignment.Center), fontSize = 20.sp)
                    }
                    
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                      Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(circle.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Box(modifier = Modifier.background(Color(0xFFF4B942).copy(alpha = 0.15f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                          Text(circle.tier, color = Color(0xFFF4B942), fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        }
                      }
                      Text("👤 ${circle.activeMembers} members • ${circle.activeOnline} active now", color = Color.Gray, fontSize = 10.sp)
                      Text("🎯 Quest: ${circle.currentQuest}", color = Color.LightGray, fontSize = 10.sp)
                    }

                    Button(
                      onClick = {
                        val updated = hobbyCircles.map { c ->
                          if (c.id == circle.id) {
                            val nextJoined = !c.isJoined
                            if (nextJoined) {
                              triggerHud("Joined ${circle.name}! Group chat unlocked in active channels! 💬")
                            } else {
                              triggerHud("Left ${circle.name}.")
                            }
                            c.copy(isJoined = nextJoined)
                          } else c
                        }
                        hobbyCircles = updated
                      },
                      colors = ButtonDefaults.buttonColors(
                        containerColor = if (circle.isJoined) Color(0xFF150C24) else Color(0xFFF4B942)
                      ),
                      border = if (circle.isJoined) BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)) else null,
                      contentPadding = PaddingValues(horizontal = 10.dp),
                      modifier = Modifier.height(32.dp)
                    ) {
                      Text(
                        if (circle.isJoined) "MEMBER ✅" else "JOIN CIRCLE",
                        color = if (circle.isJoined) Color.LightGray else Color(0xFF150C24),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                      )
                    }
                  }
                }
              }
            }

            // Swarm Missions / Co-op Group Quests (Encourages Real-world crowd check-ins)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Text("⚡ LIVE CO-OP SWARM CHALLENGES", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
              swarmMissions.forEach { mission ->
                Card(
                  colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1433)),
                  border = BorderStroke(1.dp, Color(0xFFF4B942).copy(alpha = 0.1f)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(mission.emoji, fontSize = 20.sp)
                        Column {
                          Text(mission.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                          Text("Reward: 🎁 +${mission.rewardXp} XP Boost", color = Color(0xFFF4B942), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                      }
                      
                      Button(
                        onClick = {
                          val updated = swarmMissions.map { m ->
                            if (m.id == mission.id) {
                              val nextChecked = !m.isCheckedIn
                              val nextCount = if (nextChecked) m.currentCount + 1 else m.currentCount - 1
                              if (nextChecked) {
                                triggerHud("GPS Handshake Confirmed! 📍 Checked in. +${mission.rewardXp} XP!")
                              } else {
                                triggerHud("Checked out of Swarm.")
                              }
                              m.copy(isCheckedIn = nextChecked, currentCount = nextCount)
                            } else m
                          }
                          swarmMissions = updated
                        },
                        colors = ButtonDefaults.buttonColors(
                          containerColor = if (mission.isCheckedIn) Color.Transparent else Color(0xFFF4B942).copy(alpha = 0.15f)
                        ),
                        border = BorderStroke(1.dp, if (mission.isCheckedIn) Color.Green else Color(0xFFF4B942)),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.height(28.dp)
                      ) {
                        Text(
                          if (mission.isCheckedIn) "CHECKED IN 📍" else "CHECK IN",
                          color = if (mission.isCheckedIn) Color.Green else Color(0xFFF4B942),
                          fontSize = 9.sp,
                          fontWeight = FontWeight.Bold
                        )
                      }
                    }

                    // Progress bar for the group swarm goal
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Swarm Progress", color = Color.Gray, fontSize = 9.sp)
                        Text("${mission.currentCount} / ${mission.requiredCount} Checked In", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                      }
                      Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.05f))) {
                        val fraction = (mission.currentCount.toFloat() / mission.requiredCount.toFloat()).coerceIn(0f, 1f)
                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(fraction).background(Color(0xFFF4B942)))
                      }
                    }
                  }
                }
              }
            }
          }
        }
        "gallery" -> {
          Box(modifier = Modifier.fillMaxSize()) {
            Column(
              modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = if (N > 0) 90.dp else 16.dp),
              verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
              
              // 📅 AUTOMATED CLIQUE CALENDAR & GROUP SYNC
              Card(
                colors = CardDefaults.cardColors(containerColor = getSurfaceBg()),
                border = BorderStroke(1.dp, Color(0xFFF4B942).copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
              ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                      Text("📅", fontSize = 20.sp)
                      Column {
                        Text(
                          text = "📅 UPCOMING CHILL PLOTS",
                          color = getTextPrimary(),
                          fontSize = 12.sp,
                          fontWeight = FontWeight.Black,
                          letterSpacing = 1.sp
                        )
                        Text(
                          text = "Live Plot Tracker 🗓️",
                          color = Color(0xFFF4B942),
                          fontSize = 9.sp,
                          fontWeight = FontWeight.Bold
                        )
                      }
                    }
                    Box(
                      modifier = Modifier
                        .background(Color(0xFFF4B942).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                      Text("🟢 SYNC'D", color = Color(0xFFF4B942), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                  }

                  Text(
                    text = "Your upcoming plots and group trips mapped out automatically based on active tickets and meetups! 🗓️⚡",
                    color = getTextSecondary(),
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                  )

                  // Active Smart Alarm Status Banner
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .background(Color(0xFFE11D48).copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                      .border(0.5.dp, Color(0xFFE11D48).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                      .padding(10.dp)
                  ) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("🔔", fontSize = 14.sp)
                        Column {
                          Text("SMART REMINDER ALARM", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                          Text("Silent Disco starting in 2 hours • Tickets Locked", fontSize = 10.sp, color = Color(0xFF94A3B8))
                        }
                      }
                      Box(
                        modifier = Modifier
                          .background(Color(0xFFE11D48), RoundedCornerShape(4.dp))
                          .clickable { triggerHud("⏰ Flash alert reminder broadcasted to your group squad!") }
                          .padding(horizontal = 6.dp, vertical = 3.dp)
                      ) {
                        Text("PING SQUAD", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                      }
                    }
                  }

                  Spacer(modifier = Modifier.height(4.dp))

                  // Event list
                  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    scheduledEvents.forEach { event ->
                      Box(
                        modifier = Modifier
                          .fillMaxWidth()
                          .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                          .border(1.dp, getBorderColor(), RoundedCornerShape(12.dp))
                          .padding(12.dp)
                      ) {
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.Top
                        ) {
                          Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                            Text(event.emoji, fontSize = 24.sp, modifier = Modifier.padding(top = 2.dp))
                            Column {
                              Text(event.title, color = getTextPrimary(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                              Text("👥 Channel: ${event.groupName}", color = Color(0xFFF4B942), fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                              Text("📍 Venue: ${event.venue}", color = getTextSecondary(), fontSize = 9.sp)
                              Text("⏰ Time: ${event.datetime}", color = getTextTertiary(), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                          }

                          Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                              modifier = Modifier
                                .background(
                                  if (event.isBooked) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFFFB300).copy(alpha = 0.15f),
                                  RoundedCornerShape(4.dp)
                                )
                                .border(0.5.dp, if (event.isBooked) Color(0xFF10B981) else Color(0xFFFFB300), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                              Text(
                                text = if (event.isBooked) "✓ BOOKED" else "👥 EARMARKED",
                                color = if (event.isBooked) Color(0xFF10B981) else Color(0xFFFFB300),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black
                              )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                              IconButton(
                                onClick = { triggerHud("⏰ KUEST Smart Alarm configured for ${event.title}") },
                                modifier = Modifier
                                  .size(24.dp)
                                  .background(Color.White.copy(alpha = 0.05f), CircleShape)
                              ) {
                                Text("⏰", fontSize = 10.sp)
                              }
                              IconButton(
                                onClick = { triggerHud("📢 Broadcasted group sync check to ${event.groupName} members") },
                                modifier = Modifier
                                  .size(24.dp)
                                  .background(Color.White.copy(alpha = 0.05f), CircleShape)
                              ) {
                                Text("💬", fontSize = 10.sp)
                              }
                            }
                          }
                        }
                      }
                    }
                  }

                  Spacer(modifier = Modifier.height(4.dp))

                  // Simulation Button to show off the Automated scheduling
                  Button(
                    onClick = {
                      val isAlreadyAdded = scheduledEvents.any { it.id == "sim_1" }
                      if (!isAlreadyAdded) {
                        val simEvent = ScheduledEvent(
                          id = "sim_1",
                          title = "🔥 DJ Set & Beer Fest by Club Alchemist",
                          groupName = "Nairobi Nightlife Guild",
                          datetime = "Tonight, 9:00 PM",
                          venue = "Club Alchemist Lounge",
                          emoji = "🎧",
                          isBooked = true,
                          isEarmarked = false,
                          trackingChannel = "Nightlife"
                        )
                        scheduledEvents = scheduledEvents + simEvent
                        triggerHud("🔮 New group-earmarked event auto-synced to your calendar!")
                        
                        // Add an M-Pesa direct recipient for the DJ Set
                        onTriggerNotification?.invoke(
                          "📅 Automated Calendar Sync",
                          "Club Alchemist DJ Set was earmarked by your group. Auto-synced ticket and agenda details!",
                          "",
                          "BOOKED"
                        )
                      } else {
                        triggerHud("Club Alchemist DJ Set is already synced in your calendar!")
                      }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF4B942).copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFF4B942)),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                      Text("🔮", fontSize = 12.sp)
                      Text("SIMULATE GROUP BOOKING AUTO-SYNC", color = Color(0xFFF4B942), fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                  }
                }
              }

              // 📢 HOST BROADCAST HUB & CLASS SCHEDULER CARD (Now in Gallery)
              Card(
                colors = CardDefaults.cardColors(containerColor = getCardPurple()),
                border = BorderStroke(1.dp, Color(0xFFF4B942).copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
              ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                      Text("📢", fontSize = 20.sp)
                      Text(
                        text = "HOST BROADCAST & SCHEDULER",
                        color = getTextPrimary(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                      )
                    }
                    Box(
                      modifier = Modifier
                        .background(Color(0xFFF4B942), RoundedCornerShape(4.dp))
                        .clickable { showHostBroadcastDialog = !showHostBroadcastDialog }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                      Text(
                        text = if (showHostBroadcastDialog) "Hide Form ✕" else "Schedule Class +",
                        color = Color(0xFF150C24),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                      )
                    }
                  }
                  
                  Text(
                    text = "Host culinary classes, gym sessions, yoga meets, or sports events. Live broadcasts get pushed directly into local feeds to attract clients.",
                    color = getTextSecondary(),
                    fontSize = 11.sp
                  )
                  
                  if (showHostBroadcastDialog) {
                    Box(
                      modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(getBorderColor())
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                      Text("Class / Event Category", color = getTextSecondary(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                      ) {
                        listOf("Culinary Class", "Gym Group", "Yoga Session", "Club Host", "Sports").forEach { catName ->
                          val isSel = hostCategory == catName
                          Box(
                            modifier = Modifier
                              .weight(1f)
                              .clip(RoundedCornerShape(6.dp))
                              .background(if (isSel) Color(0xFFF4B942) else getSurfaceBg())
                              .border(1.dp, if (isSel) Color(0xFFF4B942) else getBorderColor(), RoundedCornerShape(6.dp))
                              .clickable { hostCategory = catName }
                              .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                          ) {
                            Text(catName, color = if (isSel) Color(0xFF150C24) else getTextPrimary(), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                          }
                        }
                      }
                      
                      Text("Title", color = getTextSecondary(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                      Box(
                        modifier = Modifier
                          .fillMaxWidth()
                          .background(getSurfaceBg(), RoundedCornerShape(6.dp))
                          .border(1.dp, getBorderColor(), RoundedCornerShape(6.dp))
                          .padding(horizontal = 10.dp, vertical = 8.dp)
                      ) {
                        if (hostTitle.isEmpty()) {
                          Text("e.g. Sushi Making Masterclass, Sunset Vinyasa", color = getTextSecondary(), fontSize = 11.sp)
                        }
                        BasicTextFieldMock(hostTitle, { hostTitle = it }, getTextPrimary(), 11.sp)
                      }

                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                      ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                          Text("Ticket Price (KES)", color = getTextSecondary(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                          Box(
                            modifier = Modifier
                              .fillMaxWidth()
                              .background(getSurfaceBg(), RoundedCornerShape(6.dp))
                              .border(1.dp, getBorderColor(), RoundedCornerShape(6.dp))
                              .padding(horizontal = 10.dp, vertical = 8.dp)
                          ) {
                            BasicTextFieldMock(hostPrice, { hostPrice = it }, getTextPrimary(), 11.sp)
                          }
                        }
                        
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                          Text("Capacity Limit", color = getTextSecondary(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                          Box(
                            modifier = Modifier
                              .fillMaxWidth()
                              .background(getSurfaceBg(), RoundedCornerShape(6.dp))
                              .border(1.dp, getBorderColor(), RoundedCornerShape(6.dp))
                              .padding(horizontal = 10.dp, vertical = 8.dp)
                          ) {
                            BasicTextFieldMock(hostCapacity, { hostCapacity = it }, getTextPrimary(), 11.sp)
                          }
                        }
                      }
                      
                      Text("Date & Time", color = getTextSecondary(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                      Box(
                        modifier = Modifier
                          .fillMaxWidth()
                          .background(getSurfaceBg(), RoundedCornerShape(6.dp))
                          .border(1.dp, getBorderColor(), RoundedCornerShape(6.dp))
                          .padding(horizontal = 10.dp, vertical = 8.dp)
                      ) {
                        BasicTextFieldMock(hostDate, { hostDate = it }, getTextPrimary(), 11.sp)
                      }

                      Text("Class Description & Specialty Focus", color = getTextSecondary(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                      Box(
                        modifier = Modifier
                          .fillMaxWidth()
                          .background(getSurfaceBg(), RoundedCornerShape(6.dp))
                          .border(1.dp, getBorderColor(), RoundedCornerShape(6.dp))
                          .padding(horizontal = 10.dp, vertical = 8.dp)
                      ) {
                        if (hostDescription.isEmpty()) {
                          Text("Include prerequisites, materials supplied, or special guests.", color = getTextSecondary(), fontSize = 11.sp)
                        }
                        BasicTextFieldMock(hostDescription, { hostDescription = it }, getTextPrimary(), 11.sp)
                      }
                      
                      Spacer(modifier = Modifier.height(4.dp))
                      
                      Button(
                        onClick = {
                          if (hostTitle.isBlank()) {
                            triggerHud("⚠️ Please input a class title!")
                            return@Button
                          }
                          val cleanPrice = hostPrice.toDoubleOrNull() ?: 0.0
                          val matchEmoji = when (hostCategory) {
                            "Culinary Class" -> "🍳"
                            "Gym Group" -> "💪"
                            "Yoga Session" -> "🧘"
                            "Club Host" -> "🎤"
                            else -> "⚽"
                          }
                          val newQuest = PremiumQuest(
                            id = "host_q_${System.currentTimeMillis()}",
                            title = hostTitle,
                            category = "Hosted Classes", // Configured category directly
                            subtitle = "${hostCategory} led by @kuest_explorer",
                            date = hostDate,
                            location = hostLocation,
                            price = cleanPrice,
                            rating = 5.0, // Brand new host event starts at perfect 5!
                            reviewsCount = 1,
                            emoji = matchEmoji,
                            description = hostDescription.ifBlank { "A premium hosted class focusing on community, interactive guidance, and hands-on professional coaching." },
                            capacity = hostCapacity,
                            groupOffer = "Book with a friend for a 10% rebate!",
                            duration = hostDuration
                          )
                          
                          // Add to current list!
                          premiumQuests = listOf(newQuest) + premiumQuests
                          triggerHud("🚀 Broadcasted ${hostTitle} successfully!")
                          
                          // Clear fields
                          hostTitle = ""
                          hostDescription = ""
                          showHostBroadcastDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF4B942)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                      ) {
                        Text("🚀 BROADCAST LIVE & SCHEDULE CLASS", color = Color(0xFF150C24), fontSize = 11.sp, fontWeight = FontWeight.Black)
                      }
                    }
                  }
                }
              }

              Card(colors = CardDefaults.cardColors(containerColor = getSurfaceBg()), border = BorderStroke(1.dp, getBorderColor()), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                  Column {
                    Text("SECURE PRIVACY VAULT", color = getTextPrimary(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Circle: Syncing (2 Friends Online)", color = getTextSecondary(), fontSize = 10.sp)
                  }
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Strict Mode", color = getTextPrimary(), fontSize = 10.sp)
                    Switch(checked = isCirclePrivacyEnabled, onCheckedChange = { isCirclePrivacyEnabled = it })
                  }
                }
              }

              // ==================== CLIQUE HUB: THREE NEW EXCLUSIVE FEATURES ====================

              // FEATURE 1: 📊 Live Crowd Vibe Check & Hype Gauge
              Card(
                colors = CardDefaults.cardColors(containerColor = getSurfaceBg()),
                border = BorderStroke(1.dp, Color(0xFFF4B942).copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
              ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                      Text("📊", fontSize = 18.sp)
                      Text(
                        text = "LIVE CROWD VIBE CHECK 🌡️",
                        color = getTextPrimary(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                      )
                    }
                    Box(
                      modifier = Modifier
                        .background(Color(0xFFF4B942), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                      Text("LIVE HYPE", color = Color(0xFF150C24), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                  }

                  Text(
                    text = "Check real-time hype meters and party vibes at USIU and Nairobi joints before heading out! 🔥🌡️",
                    color = getTextSecondary(),
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                  )

                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                      .padding(10.dp)
                  ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Text(
                          text = "Vibe Rating: $cliqueVibeStatus",
                          fontSize = 11.sp,
                          fontWeight = FontWeight.Bold,
                          color = Color.White
                        )
                        Text(
                          text = "Score: ${String.format("%.1f", cliqueVibeRating)} / 5.0",
                          fontSize = 11.sp,
                          fontWeight = FontWeight.Black,
                          color = Color(0xFFF4B942)
                        )
                      }

                      // A visual volume slider/meter
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                      ) {
                        repeat(10) { index ->
                          val isActive = index < (cliqueVibeRating * 2).toInt()
                          Box(
                            modifier = Modifier
                              .weight(1f)
                              .height(8.dp)
                              .clip(RoundedCornerShape(2.dp))
                              .background(
                                if (isActive) {
                                  if (index > 7) Color(0xFFE11D48) else if (index > 4) Color(0xFFFFB300) else Color(0xFF10B981)
                                } else {
                                  Color.White.copy(alpha = 0.05f)
                                }
                              )
                          )
                        }
                      }
                    }
                  }

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    listOf(
                      Triple("⚡ Decibels", "⚡ INSANE DECIBELS", 4.9f),
                      Triple("🕺 Packed", "🕺 PACKED OUT", 4.7f),
                      Triple("🍃 Chill", "🍃 CHILL SESH", 4.2f)
                    ).forEach { (label, status, rating) ->
                      Button(
                        onClick = {
                          cliqueVibeStatus = status
                          cliqueVibeRating = rating
                          triggerHud("Voted: $status! Vibe metrics updated.")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier
                          .weight(1f)
                          .height(28.dp)
                          .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp)
                      ) {
                        Text(label, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                      }
                    }
                  }
                }
              }

              // FEATURE 2: 📍 Clique Rendezvous GPS Beacon
              Card(
                colors = CardDefaults.cardColors(containerColor = getSurfaceBg()),
                border = BorderStroke(1.dp, Color(0xFFF4B942).copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
              ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                      Text("📍", fontSize = 18.sp)
                      Text(
                        text = "SQUAD BEACON PIN 📍",
                        color = getTextPrimary(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                      )
                    }
                    Box(
                      modifier = Modifier
                        .background(if (droppedBeaconState != null) Color(0xFF10B981) else Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                      Text(
                        text = if (droppedBeaconState != null) "BROADCASTING" else "OFFLINE",
                        color = if (droppedBeaconState != null) Color.Black else Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                      )
                    }
                  }

                  Text(
                    text = "Drop a live temporary pin visible ONLY to your trusted squad. 📍🗺️",
                    color = getTextSecondary(),
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                  )

                  if (droppedBeaconState != null) {
                    Box(
                      modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF064E3B).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .border(0.5.dp, Color(0xFF10B981).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                    ) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Column {
                          Text("ACTIVE BEACON COORDINATES", fontSize = 9.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                          Text(droppedBeaconState!!, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Button(
                          onClick = {
                            droppedBeaconState = null
                            triggerHud("Beacon revoked. Coordinates deleted.")
                          },
                          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                          contentPadding = PaddingValues(horizontal = 8.dp),
                          modifier = Modifier.height(24.dp),
                          shape = RoundedCornerShape(4.dp)
                        ) {
                          Text("REVOKE", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                      }
                    }
                  } else {
                    Button(
                      onClick = {
                        droppedBeaconState = "📍 USIU Campus Quad - East Wing [Ref: 1.292, 36.807]"
                        triggerHud("Squad Beacon dropped!")
                        onTriggerNotification?.invoke(
                          "📍 Squad Beacon dropped",
                          "A squad beacon was dropped at USIU Campus Quad. Your squad members have been notified!",
                          "",
                          "MAP_PIN"
                        )
                      },
                      colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF4B942).copy(alpha = 0.15f)),
                      shape = RoundedCornerShape(10.dp),
                      border = BorderStroke(1.dp, Color(0xFFF4B942)),
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      Text("DROP SQUAD PIN 📍", color = Color(0xFFF4B942), fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                  }
                }
              }

              // FEATURE 3: 🤫 USIU Campus Confessions Hype Board
              Card(
                colors = CardDefaults.cardColors(containerColor = getSurfaceBg()),
                border = BorderStroke(1.dp, Color(0xFFF4B942).copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
              ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                      Text("🤫", fontSize = 18.sp)
                      Text(
                        text = "CAMPUS CONFESSIONS 🤫",
                        color = getTextPrimary(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                      )
                    }
                    Box(
                      modifier = Modifier
                        .background(Color(0xFFE11D48).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                      Text("ANONYMOUS", color = Color(0xFFE11D48), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                  }

                  Text(
                    text = "Spit the secret juice or drop anonymous university gossip anonymously! 🤫💬",
                    color = getTextSecondary(),
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                  )

                  // Scrollable confessions list
                  Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .heightIn(max = 140.dp)
                      .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                      .padding(8.dp)
                      .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    whisperConfessions.forEach { confession ->
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Top
                      ) {
                        Text("💬", fontSize = 11.sp, modifier = Modifier.padding(top = 1.dp))
                        Text(
                          text = confession,
                          fontSize = 10.sp,
                          color = Color.LightGray,
                          lineHeight = 13.sp
                        )
                      }
                      Box(
                        modifier = Modifier
                          .fillMaxWidth()
                          .height(0.5.dp)
                          .background(Color.White.copy(alpha = 0.05f))
                      )
                    }
                  }

                  // Confession input row
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Box(
                      modifier = Modifier
                        .weight(1f)
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .border(1.dp, getBorderColor(), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                      BasicTextFieldMock(
                        newConfessionText,
                        { newConfessionText = it },
                        getTextPrimary(),
                        11.sp
                      )
                      if (newConfessionText.isEmpty()) {
                        Text("Whisper anonymously...", color = Color.Gray, fontSize = 11.sp)
                      }
                    }

                    Button(
                      onClick = {
                        if (newConfessionText.isNotBlank()) {
                          whisperConfessions = whisperConfessions + newConfessionText
                          newConfessionText = ""
                          triggerHud("Whisper broadcasted anonymously!")
                        } else {
                          triggerHud("Please type a whisper first!")
                        }
                      },
                      colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF4B942)),
                      contentPadding = PaddingValues(horizontal = 10.dp),
                      modifier = Modifier.height(28.dp),
                      shape = RoundedCornerShape(6.dp)
                    ) {
                      Text("Whisper", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                  }
                }
              }
              if (activeLocationFilter != null) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text("Filtered: $activeLocationFilter", color = Color(0xFF1D5C64), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  Text("Clear ✕", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.clickable { activeLocationFilter = null })
                }
              }
              Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  listOf("Me", "Alex", "Sarah").forEach { Text("👤 $it", color = Color.LightGray, fontSize = 10.sp, modifier = Modifier.background(Color.White.copy(alpha = 0.05f)).padding(4.dp)) }
                }
                Button(onClick = { showInviteModal = true }, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent), border = BorderStroke(1.dp, Color(0xFF1D5C64)), contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.height(28.dp)) {
                  Text("+ Invite", color = Color.White, fontSize = 10.sp)
                }
              }
              val filtered = galleryPhotos.filter {
                (activeLocationFilter == null || it.location == activeLocationFilter) && (!isCirclePrivacyEnabled || it.faceTags.any { tag -> tag in listOf("Me", "Alex", "Sarah") })
              }
              if (filtered.isEmpty()) {
                Text("No matching photos in stream.", color = Color.Gray, modifier = Modifier.padding(32.dp).align(Alignment.CenterHorizontally), fontSize = 12.sp)
              } else {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                  filtered.chunked(2).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                      row.forEach { p ->
                        val isSel = selectedPhotoIds.contains(p.id)
                        Box(modifier = Modifier.weight(1f)) {
                          ChronosPhotoCard(photo = p, isSelected = isSel, onSelectionChange = {
                            selectedPhotoIds = if (it) selectedPhotoIds + p.id else selectedPhotoIds - p.id
                          })
                        }
                      }
                      if (row.size < 2) Box(modifier = Modifier.weight(1f))
                    }
                  }
                }
              }
            }
            if (N > 0) {
              Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF121216)), border = BorderStroke(1.dp, Color(0xFF1D5C64)), modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                  Column {
                    Text("$N selected • KES ${String.format("%,.0f", finalTotal)}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Split: KES ${String.format("%,.0f", splitCost)} each", color = Color(0xFF1D5C64), fontSize = 10.sp)
                  }
                  Button(onClick = { showCheckoutModal = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D5C64))) {
                    Text("BUNDLE & SAVE ${discountPercent}%", fontSize = 10.sp, color = Color.White)
                  }
                }
              }
            }
          }
        }
      }
    }

    if (showInviteModal) {
      Dialog(onDismissRequest = { showInviteModal = false }) {
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.padding(16.dp)) {
          Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("SECURE TOKEN LINK", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text("https://chronos.private/join/u47f8s", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Button(onClick = { showInviteModal = false; triggerHud("Copied secure circle link!") }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D5C64))) {
              Text("Copy & Dismiss", color = Color.White)
            }
          }
        }
      }
    }

    if (showCheckoutModal) {
      var step by remember { mutableStateOf(0) }
      LaunchedEffect(Unit) { kotlinx.coroutines.delay(1000); step = 1; kotlinx.coroutines.delay(1000); step = 2 }
      Dialog(onDismissRequest = { if(step==2) { selectedPhotoIds=emptySet(); showCheckoutModal=false } }) {
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.padding(16.dp)) {
          Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (step < 2) {
              CircularProgressIndicator(color = Color(0xFF1D5C64))
              Text(if(step==0) "Bargaining secure group deal..." else "Confirming split payment...", color = Color.Black, fontSize = 12.sp)
            } else {
              Text("🎉", fontSize = 32.sp)
              Text("Bundle Unlocked!", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
              Text("KES ${String.format("%,.0f", finalTotal)} split equally across 3 members.", color = Color.Gray, fontSize = 11.sp, textAlign = TextAlign.Center)
              Button(onClick = { selectedPhotoIds=emptySet(); showCheckoutModal=false; triggerHud("Decrypted media downloaded!") }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D5C64))) {
                Text("ACCESS FILES", color = Color.White)
              }
            }
          }
        }
      }
    }

    if (selectedQuestForDetail != null) {
      val quest = selectedQuestForDetail!!
      Dialog(onDismissRequest = { selectedQuestForDetail = null }) {
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1433)),
          border = BorderStroke(1.dp, Color(0xFFF4B942).copy(alpha = 0.3f)),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.padding(16.dp)
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
              Text("${quest.emoji} ${quest.category.uppercase()}", color = Color(0xFFF4B942), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
              IconButton(onClick = { selectedQuestForDetail = null }, modifier = Modifier.size(24.dp)) {
                Text("✕", color = Color.White, fontSize = 14.sp)
              }
            }
            Text(quest.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text(quest.subtitle, color = Color.Gray, fontSize = 12.sp)
            
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
              Column {
                Text("Escrow Price", color = Color.Gray, fontSize = 9.sp)
                Text("${currencyPrefix} ${String.format("%,.0f", quest.price)}", color = Color(0xFFF4B942), fontSize = 13.sp, fontWeight = FontWeight.Bold)
              }
              Column {
                Text("Duration", color = Color.Gray, fontSize = 9.sp)
                Text(quest.duration, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
              }
              Column {
                Text("Rating", color = Color.Gray, fontSize = 9.sp)
                Text("⭐ ${quest.rating}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
              }
            }
            
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))
            Text("Quest Details", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(quest.description, color = Color.LightGray, fontSize = 11.sp)
            
            Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text("👥 Group Promotion:", color = Color(0xFFF4B942), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(quest.groupOffer, color = Color.White, fontSize = 10.sp)
              }
            }
            
            Button(
              onClick = {
                val escrowItem = EscrowItem(
                  id = System.currentTimeMillis().toString(),
                  title = quest.title,
                  merchant = "@kuest_merchant_corp",
                  amount = quest.price,
                  status = "Funds Held",
                  badgeText = "🔒 Secured Escrow"
                )
                onTriggerCheckout(escrowItem, quest)
                selectedQuestForDetail = null
                triggerHud("Secure Escrow Channel established!")
              },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF4B942)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("LOCK SECURE ESCROW", color = Color(0xFF150C24), fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
          }
        }
      }
    }

    if (selectedRiderForBooking != null) {
      val rider = selectedRiderForBooking!!
      Dialog(onDismissRequest = { selectedRiderForBooking = null }) {
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1433)),
          border = BorderStroke(1.dp, Color(0xFFF4B942).copy(alpha = 0.3f)),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.padding(16.dp)
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
              Text("🏍️ HIRE ${rider.name.uppercase()}", color = Color(0xFFF4B942), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
              IconButton(onClick = { selectedRiderForBooking = null }, modifier = Modifier.size(24.dp)) {
                Text("✕", color = Color.White, fontSize = 14.sp)
              }
            }
            Text("Secure Handshake Courier Contract", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Text("The deposit is locked securely in escrow. Released to courier ${rider.handle} ONLY when your device performs a dynamic code swap.", color = Color.Gray, fontSize = 11.sp)
            
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))
            
            Text("What package/errand is this for?", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF121216), RoundedCornerShape(8.dp)).border(1.dp, Color.White.copy(alpha = 0.1f)).padding(10.dp)) {
              if (customErrandTitle.isEmpty()) Text("e.g. Pickup laundry from Kilimani Mall", color = Color.Gray, fontSize = 11.sp)
              BasicTextFieldMock(customErrandTitle, { customErrandTitle = it }, Color.White, 11.sp)
            }
            
            Text("Escrow Value (KES)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF121216), RoundedCornerShape(8.dp)).border(1.dp, Color.White.copy(alpha = 0.1f)).padding(10.dp)) {
              if (customErrandAmount.isEmpty()) Text("e.g. 1500", color = Color.Gray, fontSize = 11.sp)
              BasicTextFieldMock(customErrandAmount, { customErrandAmount = it }, Color.White, 11.sp)
            }
            
            Button(
              onClick = {
                val amountVal = customErrandAmount.toDoubleOrNull() ?: 1200.0
                val titleVal = if (customErrandTitle.isNotBlank()) customErrandTitle else "Secured Handshake Courier Errand"
                onCreateErrandDeal?.invoke(titleVal, rider.handle, amountVal)
                selectedRiderForBooking = null
                customErrandTitle = ""
                customErrandAmount = ""
                triggerHud("Escrow booked for courier!")
              },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF4B942)),
              modifier = Modifier.fillMaxWidth(),
              enabled = customErrandTitle.isNotBlank() && customErrandAmount.isNotBlank()
            ) {
              Text("INITIATE COURIER ESCROW", color = Color(0xFF150C24), fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
          }
        }
      }
    }

    if (selectedReelForDetail != null) {
      val reel = selectedReelForDetail!!
      Dialog(onDismissRequest = { selectedReelForDetail = null }) {
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1433)),
          border = BorderStroke(1.dp, Color(0xFFF4B942).copy(alpha = 0.3f)),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.padding(16.dp)
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
              Text("${reel.emoji} ${reel.category.uppercase()}", color = Color(0xFFF4B942), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
              IconButton(onClick = { selectedReelForDetail = null }, modifier = Modifier.size(24.dp)) {
                Text("✕", color = Color.White, fontSize = 14.sp)
              }
            }
            Text(reel.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text("Published by ${reel.creator} • ${reel.duration}", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))
            
            Text("Activity Overview", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(reel.activityDescription, color = Color.LightGray, fontSize = 11.sp)
            
            Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))) {
              Row(modifier = Modifier.padding(10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🗓️", fontSize = 20.sp)
                Column {
                  Text("NEXT SCHEDULED SESSION:", color = Color(0xFFF4B942), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                  Text(reel.nextEventDate, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
              Button(
                onClick = {
                  selectedReelForDetail = null
                  triggerHud("RSVP confirmed! Added next session to KUEST Calendar 🗓️")
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF4B942)),
                modifier = Modifier.weight(1f)
              ) {
                Text("FREE RSVP", color = Color(0xFF150C24), fontWeight = FontWeight.Bold, fontSize = 11.sp)
              }
              
              Button(
                onClick = {
                  selectedReelForDetail = null
                  triggerHud("Community Lobby Joined! Open Chat channels to meet creators 💬")
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                modifier = Modifier.weight(1f)
              ) {
                Text("JOIN LOBBY 💬", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
              }
            }
          }
        }
      }
    }

    if (selectedHostingKitForDetail != null) {
      val kit = selectedHostingKitForDetail!!
      Dialog(onDismissRequest = { selectedHostingKitForDetail = null }) {
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1433)),
          border = BorderStroke(1.dp, Color(0xFFF4B942).copy(alpha = 0.3f)),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.padding(16.dp)
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
              Text("🍹 PREMIUM LIQUID ESCROW", color = Color(0xFFF4B942), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
              IconButton(onClick = { selectedHostingKitForDetail = null }, modifier = Modifier.size(24.dp)) {
                Text("✕", color = Color.White, fontSize = 14.sp)
              }
            }
            
            Text("${kit.emoji} ${kit.title}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text("Chilled concierge courier delivery in under 45 minutes directly to your designated coordinates.", color = Color.Gray, fontSize = 11.sp)

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))

            Text(kit.description, color = Color.LightGray, fontSize = 11.sp)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text("HIGHLIGHTS:", color = Color(0xFFF4B942), fontSize = 9.sp, fontWeight = FontWeight.Bold)
              kit.itemHighlights.forEach { highlight ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                  Text("🔹", fontSize = 10.sp)
                  Text(highlight, color = Color.White, fontSize = 10.sp)
                }
              }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Column {
                Text("Escrow Total", color = Color.Gray, fontSize = 9.sp)
                Text("${currencyPrefix} ${String.format("%,.0f", kit.price)}", color = Color(0xFFF4B942), fontSize = 15.sp, fontWeight = FontWeight.Black)
              }

              Button(
                onClick = {
                  val escrowItem = EscrowItem(
                    id = System.currentTimeMillis().toString(),
                    title = "Hosting Kit: ${kit.title}",
                    merchant = "@kuest_liquid_lounge",
                    amount = kit.price,
                    status = "Funds Held",
                    badgeText = "🍹 Premium Liquids"
                  )
                  onTriggerCheckout(escrowItem, premiumQuests[0])
                  selectedHostingKitForDetail = null
                  triggerHud("Secure Escrow created for ${kit.title}! Driver dispatched. 🏍️❄️")
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF4B942)),
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier.height(36.dp)
              ) {
                Text("CONFIRM ORDER", color = Color(0xFF150C24), fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }

    if (preOrderEventName != null) {
      val eventName = preOrderEventName!!
      Dialog(onDismissRequest = { preOrderEventName = null }) {
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1433)),
          border = BorderStroke(1.dp, Color(0xFFF4B942).copy(alpha = 0.3f)),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.padding(16.dp)
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
              Text("🍹 PRE-ORDER DRINKS", color = Color(0xFFF4B942), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
              IconButton(onClick = { preOrderEventName = null }, modifier = Modifier.size(24.dp)) {
                Text("✕", color = Color.White, fontSize = 14.sp)
              }
            }

            Text("Pre-game delivery for ${eventName}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Text("Select a kit to have delivered direct to your home before heading out, or directly to your VIP table/booth at the venue.", color = Color.Gray, fontSize = 11.sp)

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(180.dp)) {
              items(hostingKits) { kit ->
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .clickable {
                      val escrowItem = EscrowItem(
                        id = System.currentTimeMillis().toString(),
                        title = "Event Pre-Order: ${kit.title} for ${eventName}",
                        merchant = "@kuest_liquid_lounge",
                        amount = kit.price,
                        status = "Funds Held",
                        badgeText = "🍹 Event Pre-Order"
                      )
                      onTriggerCheckout(escrowItem, premiumQuests[0])
                      preOrderEventName = null
                      triggerHud("Pre-order booked! Handshake escrow active for table service/delivery 🍾")
                    }
                    .padding(10.dp),
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(kit.emoji, fontSize = 20.sp)
                  Column(modifier = Modifier.weight(1f)) {
                    Text(kit.title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("KES ${String.format("%,.0f", kit.price)} • Chilled delivery", color = Color.Gray, fontSize = 9.sp)
                  }
                  Text("SELECT", color = Color(0xFFF4B942), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
              }
            }

            Button(
              onClick = { preOrderEventName = null },
              colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Cancel", color = Color.White, fontSize = 11.sp)
            }
          }
        }
      }
    }

    activeHudMessage?.let { m ->
      Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp).background(Color(0xFF121216), RoundedCornerShape(8.dp)).border(1.dp, Color(0xFF1D5C64), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(m, color = Color.White, fontSize = 11.sp)
      }
    }

    // Floating Pop-up notification for Discover tab
    AnimatedVisibility(
      visible = showDiscoverNicheNotification,
      enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
      exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
      modifier = Modifier
        .align(Alignment.TopCenter)
        .padding(16.dp)
        .statusBarsPadding()
        .zIndex(100f)
    ) {
      Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1433)),
        border = BorderStroke(1.5.dp, Color(0xFFF4B942)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("🔥", fontSize = 18.sp)
              Text(
                text = "DISCOVER NICHE ACTIVITIES",
                color = Color(0xFFF4B942),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
              )
            }
            IconButton(
              onClick = { showDiscoverNicheNotification = false },
              modifier = Modifier.size(24.dp)
            ) {
              Text("✕", color = Color.Gray, fontSize = 12.sp)
            }
          }
          Text(
            text = "Vibe-matching group adventures, custom community loops, and stream highlights.",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Watch past highlight reels, join active local hobby circles, and co-sign ongoing swarm challenges in real time to secure exclusive rewards and XP multipliers.",
            color = Color.LightGray,
            fontSize = 10.sp,
            lineHeight = 14.sp
          )
          
          // Progress bar countdown effect
          var progressWidth by remember { mutableStateOf(1f) }
          LaunchedEffect(showDiscoverNicheNotification) {
            if (showDiscoverNicheNotification) {
              progressWidth = 1f
              val steps = 25
              for (i in 1..steps) {
                kotlinx.coroutines.delay(100)
                progressWidth = 1f - (i.toFloat() / steps)
              }
            }
          }
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(3.dp)
              .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(1.5.dp))
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth(progressWidth)
                .height(3.dp)
                .background(Color(0xFFF4B942), RoundedCornerShape(1.5.dp))
            )
          }
        }
      }
    }
  }
}

@Composable
fun ChronosPhotoCard(photo: ChronosPhotoItem, isSelected: Boolean, onSelectionChange: (Boolean) -> Unit) {
  Box(modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(8.dp)).background(Brush.verticalGradient(photo.gradientColors)).border(1.dp, if (isSelected) Color(0xFF1D5C64) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)).clickable { onSelectionChange(!isSelected) }) {
    Text("CHRONOS Private", color = Color.White.copy(alpha = 0.05f), modifier = Modifier.align(Alignment.Center).rotate(-15f), fontSize = 10.sp)
    Box(modifier = Modifier.align(Alignment.Center).size(36.dp).background(Color.White.copy(alpha = 0.05f), CircleShape)) { Text(photo.emoji, modifier = Modifier.align(Alignment.Center)) }
    Box(modifier = Modifier.align(Alignment.TopStart).padding(6.dp).background(Color.Black.copy(alpha = 0.4f)).padding(2.dp)) { Text(photo.filterApplied, color = Color.LightGray, fontSize = 8.sp) }
    Box(modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp).size(16.dp).clip(CircleShape).background(if (isSelected) Color(0xFF1D5C64) else Color.Black.copy(alpha = 0.5f))) { if(isSelected) Text("✓", color = Color.White, fontSize = 9.sp, modifier = Modifier.align(Alignment.Center)) }
    Column(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Color.Black.copy(alpha = 0.6f)).padding(6.dp)) {
      Text(photo.title, color = Color.White, fontSize = 10.sp, maxLines = 1)
      Text("📍 ${photo.location}", color = Color.LightGray, fontSize = 8.sp, maxLines = 1)
    }
  }
}

@Composable
fun KuestMpesaRegistryWalletScreen(
  currencyPrefix: String,
  selectedLocation: String,
  isSellerMode: Boolean,
  onIsSellerModeChange: (Boolean) -> Unit,
  onSuccessMessage: (String) -> Unit,
  onBack: () -> Unit
) {
  // Local states
  var verifiedTxs by remember {
    mutableStateOf(
      listOf(
        MpesaVerifiedTx("tx_1", "Today, 11:24 AM", 1500.0, "Mama's Secret Kitchen", "+254 712 345 678", "SGB381FK82", "🟢 Direct Pay Verified"),
        MpesaVerifiedTx("tx_2", "Yesterday, 4:10 PM", 4500.0, "USIU Esports Arena", "+254 722 999 888", "SFA941JN10", "🟢 Direct Pay Verified")
      )
    )
  }

  var installmentPlans by remember {
    mutableStateOf(
      listOf(
        P2PInstallmentPlan("plan_1", "Naivasha Ridge Shuttle Bus & Event Ticket", "Kilimani Sound Partners", "+254 701 444 555", 12000.0, 4000.0, 2000.0, "Weekly", 60, "July 25, 2026")
      )
    )
  }

  var cliqueTrustScore by remember { mutableStateOf(780) }
  var mpesaCodeInput by remember { mutableStateOf("") }
  
  // Installment Creator fields
  val presetMerchants = listOf(
    Pair("Mama's Secret Kitchen", "+254 712 345 678"),
    Pair("Club Alchemist DJ Crew", "+254 722 555 111"),
    Pair("Kilimani Sound Partners", "+254 701 444 555")
  )
  var selectedMerchantIdx by remember { mutableStateOf(0) }
  var totalContractAmount by remember { mutableStateOf("12000") }
  var selectedFrequency by remember { mutableStateOf("Weekly") }
  var selectedDurationDays by remember { mutableStateOf(60) }
  var customContractTitle by remember { mutableStateOf("Acoustic Jam Event Sponsor") }

  // Community Campfire pooling states
  var campfirePoolProgress by remember { mutableStateOf(7200.0) }
  val targetCampfirePool = 10000.0

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF020617)) // Slate 950
      .padding(16.dp)
      .verticalScroll(rememberScrollState())
  ) {
    // Header Row
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = if (isSellerMode) "Partner Studio Registry 💼" else "Direct M-Pesa Ledger 💳",
          fontSize = 20.sp,
          fontWeight = FontWeight.Black,
          color = Color.White,
          letterSpacing = (-0.5).sp
        )
        Text(
          text = if (isSellerMode) 
            "Track direct customer payments, installment contracts & direct settlements." 
          else 
            "Verify direct phone transfers & split larger payments into easy chunks! ⚡📱",
          fontSize = 11.sp,
          color = Color(0xFF94A3B8),
          lineHeight = 14.sp
        )
      }

      IconButton(
        onClick = onBack,
        modifier = Modifier
          .size(36.dp)
          .background(Color(0xFF0F172A), CircleShape)
          .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
      ) {
        Text(text = "🏠", fontSize = 16.sp)
      }
    }

    // Toggle Tabs
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(44.dp)
        .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
        .padding(4.dp),
      horizontalArrangement = Arrangement.SpaceAround,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight()
          .clip(RoundedCornerShape(8.dp))
          .background(if (!isSellerMode) Color(0xFF1E293B) else Color.Transparent)
          .clickable { onIsSellerModeChange(false) }
          .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "🎒 Explorer Pay",
          color = if (!isSellerMode) Color(0xFFFFB300) else Color(0xFF64748B),
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold
        )
      }

      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight()
          .clip(RoundedCornerShape(8.dp))
          .background(if (isSellerMode) Color(0xFF1E293B) else Color.Transparent)
          .clickable { onIsSellerModeChange(true) }
          .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "💼 Partner Studio",
          color = if (isSellerMode) Color(0xFFFFB300) else Color(0xFF64748B),
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    if (!isSellerMode) {
      // ==================== EXPLORER VIEW ====================

      // Trust & Credit Score Card
      Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.25f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
              Text("🛡️", fontSize = 20.sp)
              Column {
                Text("SQUAD TRUST SCORE 🛡️", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Verified Direct M-Pesa History", color = Color(0xFF64748B), fontSize = 9.sp)
              }
            }
            Box(
              modifier = Modifier
                .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                .border(0.5.dp, Color(0xFF10B981), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text("A+ ELITE CLIENT", color = Color(0xFF10B981), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "$cliqueTrustScore pts",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
              )
              Text("Explorer Trust Rating", fontSize = 9.sp, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = "KES 15,000",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFB300)
              )
              Text("Available Spend Limit", fontSize = 9.sp, color = Color.Gray)
            }
          }

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(4.dp)
              .clip(RoundedCornerShape(2.dp))
              .background(Color.White.copy(alpha = 0.05f))
          ) {
            Box(
              modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(cliqueTrustScore.toFloat() / 900f)
                .background(Color(0xFF10B981))
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // External transaction manual registry
      Card(
        colors = CardDefaults.cardColors(containerColor = getSurfaceBg()),
        border = BorderStroke(1.dp, getBorderColor()),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "📝 SYNC M-PESA PAYMENT",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFFFFB300),
            letterSpacing = 1.sp
          )
          Text(
            text = "Paste your Safaricom SMS code here to instantly log your direct payment on the KUEST registry. ⚡📱",
            color = getTextSecondary(),
            fontSize = 10.sp,
            lineHeight = 13.sp
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .weight(1.5f)
                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                .border(1.dp, getBorderColor(), RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
              BasicTextFieldMock(mpesaCodeInput, { mpesaCodeInput = it }, Color.White, 12.sp)
              if (mpesaCodeInput.isEmpty()) {
                Text("E.g. SGB381FK82...", color = Color.Gray, fontSize = 12.sp)
              }
            }

            Button(
              onClick = {
                if (mpesaCodeInput.length >= 6) {
                  val newTx = MpesaVerifiedTx(
                    id = "manual_" + System.currentTimeMillis().toString(),
                    date = "Just now",
                    amount = 2500.0,
                    recipient = "Mama's Secret Kitchen",
                    phone = "+254 712 345 678",
                    referenceCode = mpesaCodeInput.uppercase(),
                    status = "🟢 Direct Pay Verified"
                  )
                  verifiedTxs = listOf(newTx) + verifiedTxs
                  cliqueTrustScore = (cliqueTrustScore + 15).coerceAtMost(900)
                  mpesaCodeInput = ""
                  onSuccessMessage("✓ M-Pesa synced! KUEST Trust rating boosted! ⚡")
                } else {
                  onSuccessMessage("⚠️ Please enter a valid Safaricom M-Pesa reference code!")
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier.weight(1f)
            ) {
              Text("Sync Registry", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Verified M-Pesa transaction list
      Text(
        text = "📊 DIRECT PAY TRANSACTIONS",
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        color = Color(0xFF64748B),
        letterSpacing = 0.5.sp
      )

      Column(
        modifier = Modifier.padding(top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        verifiedTxs.forEach { tx ->
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
              .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
              .padding(10.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("💸", fontSize = 18.sp)
                Column {
                  Text(tx.recipient, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  Text("M-Pesa: ${tx.phone} • Code: ${tx.referenceCode}", color = Color.Gray, fontSize = 9.sp)
                  Text(tx.date, color = Color(0xFFFFB300), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
              }

              Column(horizontalAlignment = Alignment.End) {
                Text("KES " + String.format("%,.0f", tx.amount), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(tx.status, color = Color(0xFF10B981), fontSize = 8.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Installment Creator form (abroad styled)
      Card(
        colors = CardDefaults.cardColors(containerColor = getSurfaceBg()),
        border = BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.15f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
              Text("💳", fontSize = 20.sp)
              Column {
                Text(
                  text = "SPLIT A HIGH-VALUE TICKET 💳",
                  color = Color.White,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Black
                )
                Text(
                  text = "Easy payment splits directly to partners",
                  color = Color(0xFFFFB300),
                  fontSize = 8.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
            Box(
              modifier = Modifier
                .background(Color(0xFFFFB300).copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text("SPLIT BILL", color = Color(0xFFFFB300), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
          }

          Text(
            text = "Divide expensive tickets or bookings into simple payment dates. Send directly to partner M-Pesa. 📱💸",
            color = getTextSecondary(),
            fontSize = 10.sp,
            lineHeight = 13.sp
          )

          // Dropdown Mock selector
          Text("Target Partner / Merchant", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            presetMerchants.forEachIndexed { idx, merchant ->
              val isSelected = selectedMerchantIdx == idx
              Box(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(6.dp))
                  .background(if (isSelected) Color(0xFFFFB300).copy(alpha = 0.15f) else Color.Transparent)
                  .border(1.dp, if (isSelected) Color(0xFFFFB300) else getBorderColor(), RoundedCornerShape(6.dp))
                  .clickable { selectedMerchantIdx = idx }
                  .padding(8.dp),
                contentAlignment = Alignment.Center
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(merchant.first.split(" ")[0], color = if (isSelected) Color(0xFFFFB300) else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                  Text(merchant.second, color = Color.Gray, fontSize = 8.sp)
                }
              }
            }
          }

          // Contract Title and Total Amount inputs
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Column(modifier = Modifier.weight(1.5f)) {
              Text("Contract Description", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                  .border(1.dp, getBorderColor(), RoundedCornerShape(6.dp))
                  .padding(10.dp)
              ) {
                BasicTextFieldMock(customContractTitle, { customContractTitle = it }, Color.White, 11.sp)
              }
            }

            Column(modifier = Modifier.weight(1f)) {
              Text("Total Price (KES)", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                  .border(1.dp, getBorderColor(), RoundedCornerShape(6.dp))
                  .padding(10.dp)
              ) {
                BasicTextFieldMock(totalContractAmount, { totalContractAmount = it }, Color.White, 11.sp)
              }
            }
          }

          // Installment configuration Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Split Frequency", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
              Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                listOf("Weekly", "Monthly").forEach { freq ->
                  val isSel = selectedFrequency == freq
                  Box(
                    modifier = Modifier
                      .weight(1f)
                      .background(if (isSel) Color(0xFFFFB300) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                      .clickable { selectedFrequency = freq }
                      .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(freq, color = if (isSel) Color.Black else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }

            Column(modifier = Modifier.weight(1f)) {
              Text("Split Duration", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
              Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                listOf(30, 60).forEach { days ->
                  val isSel = selectedDurationDays == days
                  Box(
                    modifier = Modifier
                      .weight(1f)
                      .background(if (isSel) Color(0xFFFFB300) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                      .clickable { selectedDurationDays = days }
                      .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                  ) {
                    Text("$days Days", color = if (isSel) Color.Black else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }

          // Dynamic calculation box
          val parsedTotal = totalContractAmount.toDoubleOrNull() ?: 12000.0
          val paymentsCount = if (selectedFrequency == "Weekly") (selectedDurationDays / 7) else (selectedDurationDays / 30)
          val installmentVal = parsedTotal / paymentsCount.coerceAtLeast(1)

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
              .padding(10.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text("SPLIT PLAN PREVIEW", fontSize = 8.sp, color = Color(0xFFFFB300), fontWeight = FontWeight.Black)
                Text("Divided into $paymentsCount direct installment transfers", fontSize = 10.sp, color = Color.LightGray)
              }
              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text = "KES ${String.format("%,.0f", installmentVal)} / $selectedFrequency",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Black,
                  color = Color.White
                )
                Text("Direct Split Installment", fontSize = 8.sp, color = Color.Gray)
              }
            }
          }

          // Contract Creation Trigger
          Button(
            onClick = {
              val merchant = presetMerchants[selectedMerchantIdx]
              val newPlan = P2PInstallmentPlan(
                id = "plan_" + System.currentTimeMillis().toString(),
                title = customContractTitle,
                merchant = merchant.first,
                merchantPhone = merchant.second,
                totalAmount = parsedTotal,
                paidAmount = 0.0,
                installmentAmount = installmentVal,
                frequency = selectedFrequency,
                durationDays = selectedDurationDays,
                nextDueDate = "In 7 Days"
              )
              installmentPlans = installmentPlans + newPlan
              onSuccessMessage("🎉 Split Plan created for ${merchant.first}! Pay direct on M-Pesa.")
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("CREATE PAYMENT SPLIT PLAN 🚀", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Black)
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Active direct installment plans list
      Text(
        text = "💳 MY ACTIVE SPLIT PLANS",
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        color = Color(0xFF64748B),
        letterSpacing = 0.5.sp
      )

      Column(
        modifier = Modifier.padding(top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        installmentPlans.forEach { plan ->
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
              .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
              .padding(12.dp)
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(plan.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                  Text("Settle Direct M-Pesa to: ${plan.merchant} (${plan.merchantPhone})", color = Color.Gray, fontSize = 9.sp)
                }
                Box(
                  modifier = Modifier
                    .background(Color(0xFFFFB300).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                  Text("${plan.frequency} Split", color = Color(0xFFFFB300), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
              }

              val ratio = (plan.paidAmount / plan.totalAmount).toFloat().coerceIn(0f, 1f)
              Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text("Paid: KES ${String.format("%,.0f", plan.paidAmount)} / KES ${String.format("%,.0f", plan.totalAmount)}", color = Color.LightGray, fontSize = 9.sp)
                  Text("Next Due: ${plan.nextDueDate}", color = Color(0xFFE11D48), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.05f))) {
                  Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(ratio).background(Color(0xFFFFB300)))
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Installment: KES ${String.format("%,.0f", plan.installmentAmount)}",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )

                Button(
                  onClick = {
                    val nextPaid = (plan.paidAmount + plan.installmentAmount).coerceAtMost(plan.totalAmount)
                    installmentPlans = installmentPlans.map {
                      if (it.id == plan.id) it.copy(paidAmount = nextPaid, nextDueDate = "In 14 Days") else it
                    }
                    onSuccessMessage("✓ Direct installment of KES ${String.format("%,.0f", plan.installmentAmount)} registered! Pay direct on Safaricom!")
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                  modifier = Modifier
                    .height(26.dp)
                    .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp)),
                  shape = RoundedCornerShape(4.dp)
                ) {
                  Text("Pay Direct M-Pesa 📱", color = Color(0xFFFFB300), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Bill split Naivasha Caravan Campfire shared pool
      Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
              Text("🔥", fontSize = 18.sp)
              Column {
                Text(
                  text = "🔥 SQUAD COFFER POOL",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Black,
                  color = Color.White
                )
                Text(
                  text = "Naivasha Caravan Sunset Trip Fund",
                  fontSize = 9.sp,
                  color = Color(0xFF94A3B8)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Squad contributions:", fontSize = 10.sp, color = Color(0xFF64748B))
            Text(
              text = "Target: KES ${String.format("%,.0f", targetCampfirePool)}",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFFFB300)
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            val cofferMembers = listOf(
              Triple("Me", 2500.0, "👤"),
              Triple("David", 2500.0, "🧭"),
              Triple("Sarah", 1500.0, "🛸"),
              Triple("Peter", 1700.0, "📸")
            )
            cofferMembers.forEach { member ->
              Box(
                modifier = Modifier
                  .weight(1f)
                  .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
                  .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                  .padding(5.dp),
                contentAlignment = Alignment.Center
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(member.third, fontSize = 14.sp)
                  Text(member.first, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                  Text("KES " + String.format("%,.0f", member.second), fontSize = 8.sp, color = Color(0xFFFFB300), fontFamily = FontFamily.Monospace)
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          val ratio = (campfirePoolProgress / targetCampfirePool).toFloat().coerceIn(0f, 1f)
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(8.dp)
              .clip(RoundedCornerShape(4.dp))
              .background(Color.White.copy(alpha = 0.05f))
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth(ratio)
                .fillMaxHeight()
                .background(Color(0xFFFFB300), RoundedCornerShape(4.dp))
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Total Pooled: KES ${String.format("%,.0f", campfirePoolProgress)} (${(ratio * 100).toInt()}%)",
              fontSize = 10.sp,
              color = Color(0xFF94A3B8)
            )

            Button(
              onClick = {
                campfirePoolProgress = (campfirePoolProgress + 1000.0).coerceAtMost(targetCampfirePool)
                onSuccessMessage("🔥 Registered KES 1,000 coffer pool transfer direct to organizer's M-Pesa!")
              },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300).copy(alpha = 0.15f)),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
              modifier = Modifier
                .height(26.dp)
                .border(0.5.dp, Color(0xFFFFB300), RoundedCornerShape(4.dp)),
              shape = RoundedCornerShape(4.dp),
              enabled = campfirePoolProgress < targetCampfirePool
            ) {
              Text("Contribute KES 1k 📱", color = Color(0xFFFFB300), fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
          }
        }
      }

    } else {
      // ==================== PARTNER STUDIO MODE ====================
      
      // Verified collection metrics
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
          border = BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.15f)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f).height(80.dp)
        ) {
          Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
          ) {
            Text("📊 Active Collections", fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
            Text("KES 45,000", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB300), fontFamily = FontFamily.Monospace)
          }
        }

        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
          border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.15f)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f).height(80.dp)
        ) {
          Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
          ) {
            Text("✓ Cleared Settlements", fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
            Text("KES 184,000", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981), fontFamily = FontFamily.Monospace)
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Direct payment confirmation registry
      Card(
        colors = CardDefaults.cardColors(containerColor = getSurfaceBg()),
        border = BorderStroke(1.dp, getBorderColor()),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "🤝 PARTNER DIRECT MPESA VERIFICATION",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF10B981),
            letterSpacing = 0.5.sp
          )
          Text(
            text = "Verify customer transfers directly on your phone. Approve below to sync tickets instantly! 📱💸",
            color = getTextSecondary(),
            fontSize = 10.sp,
            lineHeight = 13.sp
          )

          // Active list of awaiting payments
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val awaitingPayments = listOf(
              Triple("@kuest_explorer", "Naivasha Ridge Shuttle Bus Ticket", 2000.0),
              Triple("@gaming_guild", "Gaming Hub Rental - USIU Campus", 6000.0)
            )

            awaitingPayments.forEach { pay ->
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                  .padding(10.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column {
                    Text(pay.first, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(pay.second, fontSize = 9.sp, color = Color.Gray)
                  }

                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("KES ${String.format("%,.0f", pay.third)}", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFB300))
                    Button(
                      onClick = { onSuccessMessage("✓ Confirmed payment from ${pay.first}! Ticket synchronized in KUEST registry!") },
                      colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                      contentPadding = PaddingValues(horizontal = 8.dp),
                      modifier = Modifier.height(24.dp),
                      shape = RoundedCornerShape(4.dp)
                    ) {
                      Text("Approve 📱", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                  }
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Live Monthly graph (Beautified sales activity)
      Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth().height(150.dp)
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Monthly Sales Activity", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("+24.5% Growth", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
          }

          Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
              val points = listOf(
                Offset(0f, size.height * 0.8f),
                Offset(size.width * 0.2f, size.height * 0.65f),
                Offset(size.width * 0.4f, size.height * 0.7f),
                Offset(size.width * 0.6f, size.height * 0.35f),
                Offset(size.width * 0.8f, size.height * 0.25f),
                Offset(size.width, size.height * 0.1f)
              )
              val path = Path()
              path.moveTo(points[0].x, points[0].y)
              for (i in 1 until points.size) {
                path.lineTo(points[i].x, points[i].y)
              }
              drawPath(path = path, color = Color(0xFF10B981), style = Stroke(width = 2.dp.toPx()))
              points.forEach { pt ->
                drawCircle(Color(0xFF10B981), radius = 3.dp.toPx(), center = pt)
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(30.dp))
  }
}

