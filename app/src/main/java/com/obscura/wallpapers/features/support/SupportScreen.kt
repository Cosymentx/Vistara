package com.obscura.wallpapers.features.support

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.annotation.StringRes
import com.obscura.wallpapers.ui.theme.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.obscura.wallpapers.R
import com.obscura.wallpapers.ui.theme.ObscuraTheme
import com.obscura.wallpapers.ui.components.GlassTopAppBar

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
        title = stringResource(R.string.rate_feedback),
        onBackPressed = onBackPressed
    )
    Scaffold(topBar = { topBar() }, snackbarHost = { SnackbarHost(snackbarHostState) }) { _ ->
        Column(
            modifier = contentModifier
                .verticalScroll(rememberScrollState())
                .padding(top = 64.dp, bottom = 80.dp)
                .padding(16.dp)
        ) {
            FeedbackActionList(
                items = listOf(
                    FeedbackActionEntry(
                        icon = Icons.Default.Star,
                        iconTint = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleRes = R.string.rate_in_app_store,
                        subtitleRes = R.string.like_our_app_rate_us,
                        onClick = { viewModel.openAppRating() }
                    ),
                    FeedbackActionEntry(
                        icon = Icons.Default.Email,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        titleRes = R.string.send_email_feedback,
                        subtitleRes = R.string.have_questions_email_us,
                        onClick = { viewModel.sendEmailFeedback() }
                    )
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.direct_feedback),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = feedbackText,
                onValueChange = { viewModel.updateFeedbackText(it) },
                label = { Text(stringResource(R.string.your_feedback_or_suggestion)) },
                placeholder = { Text(stringResource(R.string.enter_feedback_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = contactInfo,
                onValueChange = { viewModel.updateContactInfo(it) },
                label = { Text(stringResource(R.string.contact_info_optional)) },
                placeholder = { Text(stringResource(R.string.email_or_other_contact)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            SubmitButton(
                label = stringResource(R.string.submit_feedback),
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
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
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
