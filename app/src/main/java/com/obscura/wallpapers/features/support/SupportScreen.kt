package com.obscura.wallpapers.features.support

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.obscura.wallpapers.R
import com.obscura.wallpapers.ui.components.GlassTopAppBar
import com.obscura.wallpapers.ui.components.applyVerticalInnerPadding
import com.obscura.wallpapers.ui.components.safeVerticalContentPadding
import com.obscura.wallpapers.ui.theme.ObscuraTheme
import com.obscura.wallpapers.ui.theme.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    onBackPressed: () -> Unit, viewModel: SupportViewModel = hiltViewModel()
) {
    val feedbackText by viewModel.feedbackText.collectAsState()
    val contactInfo by viewModel.contactInfo.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val submitResult by viewModel.submitResult.collectAsState()
    val shouldNavigateBack by viewModel.shouldNavigateBack.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(submitResult) {
        submitResult?.let {
            when (it) {
                is SubmitResult.Success -> {
                    snackbarHostState.showSnackbar(it.message)
                    viewModel.clearSubmitResult()
                }

                is SubmitResult.Error -> {
                    snackbarHostState.showSnackbar(it.message)
                    viewModel.clearSubmitResult()
                }
            }
        }
    }

    LaunchedEffect(shouldNavigateBack) {
        if (shouldNavigateBack) {
            viewModel.resetNavigationState()
            onBackPressed()
        }
    }

    val (contentModifier, topBar) = GlassTopAppBar(
        title = stringResource(R.string.feedback_title),
        onBackPressed = onBackPressed
    )
    Scaffold(
        topBar = { topBar() },
        snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        val safe = innerPadding.safeVerticalContentPadding(64.dp, 80.dp)
        Column(
            modifier = contentModifier
                .applyVerticalInnerPadding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(
                    top = safe.calculateTopPadding(),
                    bottom = safe.calculateBottomPadding()
                )
                .padding(16.dp)
        ) {
            FeedbackActionList(
                items = listOf(
                    FeedbackActionEntry(
                        icon = Icons.Default.Email,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        titleRes = R.string.feedback_send_email_feedback,
                        subtitleRes = R.string.feedback_have_questions_email_us,
                        onClick = { viewModel.sendEmailFeedback() }
                    )
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.feedback_direct_feedback),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = feedbackText,
                onValueChange = { viewModel.updateFeedbackText(it) },
                label = { Text(stringResource(R.string.feedback_your_feedback_or_suggestion)) },
                placeholder = { Text(stringResource(R.string.feedback_enter_feedback_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                supportingText = {
//                    Text(
//                        text = "${feedbackText.length}",
//                        style = MaterialTheme.typography.labelSmall,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
                },
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = contactInfo,
                onValueChange = { viewModel.updateContactInfo(it) },
                label = { Text(stringResource(R.string.feedback_contact_info_optional)) },
                placeholder = { Text(stringResource(R.string.feedback_email_or_other_contact)) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            SubmitButton(
                label = stringResource(R.string.feedback_submit_feedback),
                isSubmitting = isSubmitting,
                enabled = !isSubmitting && feedbackText.isNotBlank(),
                onClick = { viewModel.submitFeedback() },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private data class FeedbackActionEntry(
    val icon: ImageVector,
    val iconTint: Color,
    val containerColor: Color,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val onClick: () -> Unit
)

@Composable
private fun FeedbackActionList(items: List<FeedbackActionEntry>) {
    items.forEachIndexed { index, item ->
        FeedbackActionCard(
            icon = item.icon,
            iconTint = item.iconTint,
            containerColor = item.containerColor,
            title = stringResource(item.titleRes),
            subtitle = stringResource(item.subtitleRes),
            onClick = item.onClick
        )
        if (index != items.lastIndex) {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FeedbackActionCard(
    icon: ImageVector,
    iconTint: Color,
    containerColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                    Color.Transparent
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                iconTint.copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SubmitButton(
    label: String,
    isSubmitting: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled
    ) {
        if (isSubmitting) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(label)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SupportScreenPreview() {
    ObscuraTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            SupportScreen(onBackPressed = {})
        }
    }
}
