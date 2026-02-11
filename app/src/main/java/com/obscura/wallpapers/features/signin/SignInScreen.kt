package com.obscura.wallpapers.features.signin

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.obscura.wallpapers.R
import com.obscura.wallpapers.ui.theme.stringResource

@Composable
fun SignInScreen(
    onLoginSuccess: () -> Unit,
    onSkipLogin: () -> Unit,
    viewModel: SignInViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val loginResult by viewModel.loginResult.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            onLoginSuccess()
        }
    }

    LaunchedEffect(loginResult) {
        loginResult?.let {
            when (it) {
                is SignInViewModel.LoginResult.Success -> {
                    snackbarHostState.showSnackbar(it.message)
                    viewModel.clearLoginResult()
                }
                is SignInViewModel.LoginResult.Error -> {
                    snackbarHostState.showSnackbar(it.message)
                    viewModel.clearLoginResult()
                }
            }
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.handleSignInResult(result.data)
        }
    }

    fun launchGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(context, gso)
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent, // Make Scaffold transparent to show full-screen background
        contentWindowInsets = WindowInsets(0, 0, 0, 0) // Disable default insets consumption
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Premium Dynamic Background (Fills the entire screen)
            PremiumBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 2. Logo Section
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(1000)) + slideInVertically(tween(1000)) { -60 }
                ) {
                    LogoSection()
                }

                Spacer(modifier = Modifier.height(0.dp))

                // 3. Content Card
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(1000, 300)) + slideInVertically(tween(1000, 300)) { 60 }
                ) {
                    GlassContentCard()
                }

                Spacer(modifier = Modifier.height(64.dp))

                // 4. Action Buttons
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(1000, 600))
                ) {
                    ActionButtons(
                        onGoogleSignIn = { launchGoogleSignIn() },
                        onSkip = onSkipLogin
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    
    val animX1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 150f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "x1"
    )

    val animY1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 200f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "y1"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Base gradient
        Box(modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                    )
                )
            ))

        // Dynamic Blobs
        Box(
            modifier = Modifier
                .size(450.dp)
                .offset(animX1.dp - 100.dp, animY1.dp - 100.dp)
                .blur(120.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(350.dp)
                .offset(x = 50.dp, y = 50.dp)
                .blur(100.dp)
                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f), CircleShape)
        )
    }
}

@Composable
private fun LogoSection() {
    val infiniteTransition = rememberInfiniteTransition(label = "logo")
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    Box(contentAlignment = Alignment.Center) {
        // Outer glow
        Box(
            modifier = Modifier
                .size(150.dp)
                .graphicsLayer { translationY = floatAnim }
                .background(
                    Brush.radialGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )
        
        // Glass container for logo - removed solid white background
        Surface(
            modifier = Modifier
                .size(110.dp)
                .graphicsLayer { translationY = floatAnim },
            shape = CircleShape,
            color = Color.Transparent, // Completely transparent container
            shadowElevation = 0.dp
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_round),
                contentDescription = "App Logo",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }
    }
}

@Composable
private fun GlassContentCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
//        shape = RoundedCornerShape(32.dp),
//        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
//        border = BorderStroke(
//            1.dp,
//            Brush.linearGradient(
//                listOf(Color.White.copy(alpha = 0.4f), Color.Transparent)
//            )
//        ),
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.app_title),
                style = TextStyle(
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.15f),
                        offset = Offset(0f, 4f),
                        blurRadius = 10f
                    )
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            
//            Text(
//                text = stringResource(R.string.app_tagline).uppercase(),
//                style = MaterialTheme.typography.labelLarge.copy(
//                    letterSpacing = 6.sp,
//                    fontWeight = FontWeight.Bold
//                ),
//                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
//            )
//
//            Spacer(modifier = Modifier.height(28.dp))
//
//            Text(
//                text = stringResource(R.string.auth_description),
//                style = MaterialTheme.typography.bodyMedium.copy(
//                    lineHeight = 24.sp
//                ),
//                textAlign = TextAlign.Center,
//                color = MaterialTheme.colorScheme.onSurfaceVariant
//            )
        }
    }
}

@Composable
private fun ActionButtons(onGoogleSignIn: () -> Unit, onSkip: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Button(
            onClick = onGoogleSignIn,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .shadow(
                    elevation = 16.dp, 
                    shape = RoundedCornerShape(30.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                ),
            shape = RoundedCornerShape(30.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.mipmap.ic_google_login),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = stringResource(R.string.auth_google_sign_in),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                )
            }
        }

        TextButton(
            onClick = onSkip,
            modifier = Modifier.alpha(0.8f)
        ) {
            Text(
                text = stringResource(R.string.auth_skip),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
