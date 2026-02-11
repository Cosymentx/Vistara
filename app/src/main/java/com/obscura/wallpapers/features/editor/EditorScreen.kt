package com.obscura.wallpapers.features.editor

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.obscura.wallpapers.R
import com.obscura.wallpapers.core.data.model.UiState
import com.obscura.wallpapers.ui.components.ErrorState
import com.obscura.wallpapers.ui.components.GlassTopAppBar
import com.obscura.wallpapers.ui.components.LoadingState
import com.obscura.wallpapers.ui.icons.ObscuraIcons
import com.obscura.wallpapers.ui.theme.stringResource
import com.obscura.wallpapers.ui.components.applyVerticalInnerPadding
import com.obscura.wallpapers.ui.components.safeVerticalContentPadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperEditScreen(
    onBackPressed: () -> Unit,
    onSaveComplete: () -> Unit,
    viewModel: WallpaperEditViewModel = hiltViewModel()
) {
    val wallpaperState by viewModel.wallpaperState.collectAsState()
    val editState by viewModel.editState.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val context = LocalContext.current

    var selectedTool by remember { mutableStateOf(EditTool.BRIGHTNESS) }

    val (_, topBar) = GlassTopAppBar(
        title = stringResource(R.string.editor_edit_wallpaper),
        onBackPressed = {
            if (!isSaving) onBackPressed()
        },
        actions = {
            IconButton(
                onClick = {
                    viewModel.saveEditedWallpaper(onComplete = {
                        onSaveComplete()
                    })
                }, enabled = !isSaving
            ) {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = stringResource(R.string.save),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        })

    Scaffold(
        topBar = { topBar() },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (wallpaperState) {
                is UiState.Loading -> LoadingState()
                is UiState.Error -> ErrorState(
                    message = (wallpaperState as UiState.Error).message, 
                    onRetry = onBackPressed
                )
                is UiState.Success -> {
                    val wallpaper = (wallpaperState as UiState.Success).data

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Immersive Preview Area
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(16.dp)
                                .shadow(24.dp, RoundedCornerShape(24.dp))
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            if (editState.croppedBitmap != null) {
                                Image(
                                    bitmap = editState.croppedBitmap!!.asImageBitmap(),
                                    contentDescription = wallpaper.title,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize(),
                                    colorFilter = ColorFilter.colorMatrix(
                                        createPreviewColorMatrix(
                                            editState.brightness,
                                            editState.contrast,
                                            editState.saturation,
                                            editState.filter
                                        )
                                    )
                                )
                            } else {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(wallpaper.url)
                                        .crossfade(true)
                                        .memoryCacheKey("edit_${wallpaper.id}")
                                        .diskCacheKey("edit_${wallpaper.id}")
                                        .build(),
                                    contentDescription = wallpaper.title,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize(),
                                    colorFilter = ColorFilter.colorMatrix(
                                        createPreviewColorMatrix(
                                            editState.brightness,
                                            editState.contrast,
                                            editState.saturation,
                                            editState.filter
                                        )
                                    )
                                )
                            }

                            if (isSaving) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.6f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        // Premium Control Panel
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding(),
                            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp,
                            shadowElevation = 16.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(vertical = 24.dp)
                            ) {
                                // Tool Content Area
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp)
                                        .height(100.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when (selectedTool) {
                                        EditTool.BRIGHTNESS -> {
                                            SliderControl(
                                                label = stringResource(R.string.editor_brightness),
                                                value = editState.brightness,
                                                onValueChange = { viewModel.updateBrightness(it) },
                                                valueRange = 0.5f..1.5f,
                                                icon = ObscuraIcons.Brightness
                                            )
                                        }
                                        EditTool.CONTRAST -> {
                                            SliderControl(
                                                label = stringResource(R.string.editor_contrast),
                                                value = editState.contrast,
                                                onValueChange = { viewModel.updateContrast(it) },
                                                valueRange = 0.5f..1.5f,
                                                icon = ObscuraIcons.Contrast
                                            )
                                        }
                                        EditTool.SATURATION -> {
                                            SliderControl(
                                                label = stringResource(R.string.editor_saturation),
                                                value = editState.saturation,
                                                onValueChange = { viewModel.updateSaturation(it) },
                                                valueRange = 0f..2f,
                                                icon = ObscuraIcons.Saturation
                                            )
                                        }
                                        EditTool.FILTER -> {
                                            FilterOptions(
                                                selectedFilter = editState.filter,
                                                onFilterSelected = { viewModel.applyFilter(it) }
                                            )
                                        }
                                        EditTool.CROP -> {
                                            CropOptionsPlaceholder()
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // Tool Selector Row
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 24.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    item {
                                        EditToolButton(
                                            tool = EditTool.BRIGHTNESS,
                                            icon = ObscuraIcons.Brightness,
                                            isSelected = selectedTool == EditTool.BRIGHTNESS,
                                            onClick = { selectedTool = EditTool.BRIGHTNESS }
                                        )
                                    }
                                    item {
                                        EditToolButton(
                                            tool = EditTool.CONTRAST,
                                            icon = ObscuraIcons.Contrast,
                                            isSelected = selectedTool == EditTool.CONTRAST,
                                            onClick = { selectedTool = EditTool.CONTRAST }
                                        )
                                    }
                                    item {
                                        EditToolButton(
                                            tool = EditTool.SATURATION,
                                            icon = ObscuraIcons.Saturation,
                                            isSelected = selectedTool == EditTool.SATURATION,
                                            onClick = { selectedTool = EditTool.SATURATION }
                                        )
                                    }
                                    item {
                                        EditToolButton(
                                            tool = EditTool.FILTER,
                                            icon = ObscuraIcons.Filter,
                                            isSelected = selectedTool == EditTool.FILTER,
                                            onClick = { selectedTool = EditTool.FILTER }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // Footer Actions
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    TextButton(
                                        onClick = { viewModel.resetEdits() },
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = MaterialTheme.colorScheme.outline
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.editor_reset_edits),
                                            style = MaterialTheme.typography.labelLarge
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
}

@Composable
fun EditToolButton(
    tool: EditTool, 
    icon: ImageVector, 
    isSelected: Boolean, 
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primaryContainer 
        else Color.Transparent,
        label = "bg"
    )
    val contentColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer 
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "content"
    )
    val iconSize by animateDpAsState(if (isSelected) 28.dp else 24.dp, label = "size")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(tool.titleRes),
            modifier = Modifier.size(iconSize),
            tint = contentColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(tool.titleRes),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor
        )
    }
}

@Composable
fun SliderControl(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    icon: ImageVector
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon, 
                    contentDescription = null, 
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label, 
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = String.format("%.1f", value),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
fun FilterOptions(
    selectedFilter: ImageFilter, 
    onFilterSelected: (ImageFilter) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(ImageFilter.values()) { filter ->
            FilterOptionItem(
                filter = filter,
                isSelected = filter == selectedFilter,
                onClick = { onFilterSelected(filter) }
            )
        }
    }
}

@Composable
fun FilterOptionItem(
    filter: ImageFilter, 
    isSelected: Boolean, 
    onClick: () -> Unit
) {
    val borderSize by animateDpAsState(if (isSelected) 3.dp else 0.dp, label = "border")
    val scale by animateFloatAsState(if (isSelected) 1.05f else 1f, label = "scale")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .clip(RoundedCornerShape(12.dp))
                .background(filter.previewColor)
                .border(
                    width = borderSize,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp)
                )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(filter.titleRes),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun CropOptionsPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Crop Tool coming soon in premium UI",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

fun createPreviewColorMatrix(
    brightness: Float, contrast: Float, saturation: Float, filter: ImageFilter
): ColorMatrix {
    val matrix = ColorMatrix()

    val brightnessScale = brightness
    val brightnessMatrix = ColorMatrix()
    brightnessMatrix.setToScale(brightnessScale, brightnessScale, brightnessScale, 1f)

    val contrastMatrix = ColorMatrix()
    contrastMatrix.setToScale(contrast, contrast, contrast, 1f)

    val saturationMatrix = ColorMatrix()
    saturationMatrix.setToSaturation(saturation)

    val filterMatrix = when (filter) {
        ImageFilter.GRAYSCALE -> {
            val m = ColorMatrix()
            m.setToSaturation(0f)
            m
        }
        ImageFilter.SEPIA -> ColorMatrix().apply { setToScale(0.8f, 0.6f, 0.4f, 1f) }
        ImageFilter.VINTAGE -> ColorMatrix().apply { setToScale(0.7f, 0.7f, 0.5f, 1f) }
        ImageFilter.COLD -> ColorMatrix().apply { setToScale(0.6f, 0.8f, 1.2f, 1f) }
        ImageFilter.WARM -> ColorMatrix().apply { setToScale(1.2f, 0.8f, 0.6f, 1f) }
        ImageFilter.PURPLE -> ColorMatrix().apply { setToScale(0.9f, 0.5f, 1.2f, 1f) }
        ImageFilter.BLUE -> ColorMatrix().apply { setToScale(0.5f, 0.7f, 1.5f, 1f) }
        ImageFilter.GREEN -> ColorMatrix().apply { setToScale(0.6f, 1.2f, 0.6f, 1f) }
        ImageFilter.PINK -> ColorMatrix().apply { setToScale(1.4f, 0.7f, 0.9f, 1f) }
        ImageFilter.ORANGE -> ColorMatrix().apply { setToScale(1.5f, 0.9f, 0.5f, 1f) }
        ImageFilter.NONE -> null
    }

    matrix.timesAssign(brightnessMatrix)
    matrix.timesAssign(contrastMatrix)
    matrix.timesAssign(saturationMatrix)
    if (filterMatrix != null) {
        matrix.timesAssign(filterMatrix)
    }

    return matrix
}

enum class EditTool(val titleRes: Int) {
    BRIGHTNESS(R.string.editor_brightness), 
    CONTRAST(R.string.editor_contrast), 
    SATURATION(R.string.editor_saturation), 
    FILTER(R.string.editor_filter),
    CROP(R.string.editor_crop)
}

enum class ImageFilter(val titleRes: Int, val previewColor: Color) {
    NONE(R.string.editor_original, Color.White), 
    GRAYSCALE(R.string.editor_grayscale, Color.Gray),
    SEPIA(R.string.editor_sepia, Color(0xFFD2B48C)), 
    VINTAGE(R.string.editor_vintage, Color(0xFFCDC9A5)),
    COLD(R.string.editor_cold, Color(0xFF87CEFA)), 
    WARM(R.string.editor_warm, Color(0xFFFFB347)),
    PURPLE(R.string.editor_purple, Color(0xFFB19CD9)), 
    BLUE(R.string.editor_blue, Color(0xFF6495ED)),
    GREEN(R.string.editor_green, Color(0xFF90EE90)), 
    PINK(R.string.editor_pink, Color(0xFFFFB6C1)),
    ORANGE(R.string.editor_orange, Color(0xFFFF8C00))
}
