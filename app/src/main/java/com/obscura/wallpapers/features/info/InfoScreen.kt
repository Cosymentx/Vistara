package com.obscura.wallpapers.features.info

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.obscura.wallpapers.R
import com.obscura.wallpapers.ui.components.GlassScaffold
import com.obscura.wallpapers.ui.components.applyVerticalInnerPadding
import com.obscura.wallpapers.ui.components.safeVerticalContentPadding
import com.obscura.wallpapers.ui.icons.ObscuraIcons
import com.obscura.wallpapers.ui.theme.ObscuraTheme
import com.obscura.wallpapers.ui.theme.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoScreen(
    onBackPressed: () -> Unit,
    navController: NavController = rememberNavController(),
    viewModel: InfoViewModel = hiltViewModel()
) {
    val appVersion by viewModel.appVersion.collectAsState()
    val openSourceLibraries by viewModel.openSourceLibraries.collectAsState()

    viewModel.setNavController(navController)

    GlassScaffold(
        title = stringResource(R.string.info_title), onBackPressed = onBackPressed
    ) { paddingValues ->
        val safe = paddingValues.safeVerticalContentPadding(64.dp, 80.dp)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .applyVerticalInnerPadding(paddingValues)
                .padding(
                    top = safe.calculateTopPadding(), bottom = safe.calculateBottomPadding()
                )
                .padding(vertical = 16.dp)
        ) {
            AppInfoSection(
                appVersion = appVersion,
                onPrivacyPolicyClick = { viewModel.openPrivacyPolicy() },
                onTermsOfServiceClick = { viewModel.openTermsOfService() },
                onUserAgreementClick = { viewModel.openUserAgreement() })

            Spacer(modifier = Modifier.height(24.dp))

//            OpenSourceSection(
//                libraries = openSourceLibraries, onLibraryClick = { viewModel.openLibraryUrl(it) })
        }
    }
}

@Composable
private fun AppInfoSection(
    appVersion: String,
    onPrivacyPolicyClick: () -> Unit,
    onTermsOfServiceClick: () -> Unit,
    onUserAgreementClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = appVersion,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.info_app_description),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(horizontal = 20.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        LinkList(
            items = listOf(
                LinkEntry(
                    title = stringResource(R.string.info_user_agreement),
                    onClick = onUserAgreementClick
                ), LinkEntry(
                    title = stringResource(R.string.info_privacy_policy),
                    onClick = onPrivacyPolicyClick
                ), LinkEntry(
                    title = stringResource(R.string.info_terms_of_service),
                    onClick = onTermsOfServiceClick
                )
            )
        )
    }
}

@Composable
private fun OpenSourceSection(
    libraries: List<Library>, onLibraryClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.info_open_source_libraries),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                libraries.forEachIndexed { index, library ->
                    LibraryItem(
                        library = library, onClick = { onLibraryClick(library.url) })

                    if (index < libraries.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = DividerDefaults.Thickness,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkItem(
    title: String, onClick: () -> Unit
) {
    Surface(
        onClick = onClick, modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
//            Icon(
//                imageVector = ObscuraIcons.Info,
//                contentDescription = null,
//                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
//            )

//            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

data class LinkEntry(
    val title: String, val onClick: () -> Unit
)

@Composable
private fun LinkList(
    items: List<LinkEntry>
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        items.forEach { entry ->
            LinkItem(title = entry.title, onClick = entry.onClick)
        }
    }
}

@Composable
private fun LibraryItem(
    library: Library, onClick: () -> Unit
) {
    Surface(
        onClick = onClick, modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp)) {
            Text(
                text = library.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = library.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun InfoScreenPreview() {
    ObscuraTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            InfoScreen(
                onBackPressed = {}, navController = rememberNavController()
            )
        }
    }
}
