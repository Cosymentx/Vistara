package com.obscura.wallpapers.features.editor

import android.graphics.Bitmap
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
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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

    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(wallpaperState) {
        if (wallpaperState is UiState.Success) {
        }
    }

    var selectedTool by remember { mutableStateOf(EditTool.BRIGHTNESS) }

    val (contentModifier, topBar) = GlassTopAppBar(
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
                    contentDescription = stringResource(R.string.save)
                )
            }
        })
    Scaffold(topBar = { topBar() }) { innerPadding ->
        val safe = innerPadding.safeVerticalContentPadding(64.dp, 80.dp)
        Box(
            modifier = contentModifier
                .applyVerticalInnerPadding(innerPadding)
                .padding(
                    top = safe.calculateTopPadding(),
                    bottom = safe.calculateBottomPadding()
                )
        ) {
            when (wallpaperState) {
                is UiState.Loading -> {
                    LoadingState()
                }

                is UiState.Error -> {
                    ErrorState(
                        message = (wallpaperState as UiState.Error).message, onRetry = onBackPressed
                    )
                }

                is UiState.Success -> {
                    val wallpaper = (wallpaperState as UiState.Success).data

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp)
                                .padding(16.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black)
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
                                    model = ImageRequest.Builder(context).data(wallpaper.url)
                                        .crossfade(true).memoryCacheKey("edit_${wallpaper.id}")
                                        .diskCacheKey("edit_${wallpaper.id}").build(),
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
                                        .background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = Color.White)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            EditToolButton(
                                tool = EditTool.BRIGHTNESS,
                                icon = ObscuraIcons.Brightness,
                                isSelected = selectedTool == EditTool.BRIGHTNESS,
                                onClick = { selectedTool = EditTool.BRIGHTNESS })
                            EditToolButton(
                                tool = EditTool.CONTRAST,
                                icon = ObscuraIcons.Contrast,
                                isSelected = selectedTool == EditTool.CONTRAST,
                                onClick = { selectedTool = EditTool.CONTRAST })
                            EditToolButton(
                                tool = EditTool.SATURATION,
                                icon = ObscuraIcons.Saturation,
                                isSelected = selectedTool == EditTool.SATURATION,
                                onClick = { selectedTool = EditTool.SATURATION })
                            EditToolButton(
                                tool = EditTool.FILTER,
                                icon = ObscuraIcons.Filter,
                                isSelected = selectedTool == EditTool.FILTER,
                                onClick = { selectedTool = EditTool.FILTER })
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            when (selectedTool) {
                                EditTool.BRIGHTNESS -> {
                                    SliderControl(
                                        label = stringResource(R.string.editor_brightness),
                                        value = editState.brightness,
                                        onValueChange = { viewModel.updateBrightness(it) },
                                        valueRange = 0.5f..1.5f
                                    )
                                }

                                EditTool.CONTRAST -> {
                                    SliderControl(
                                        label = stringResource(R.string.editor_contrast),
                                        value = editState.contrast,
                                        onValueChange = { viewModel.updateContrast(it) },
                                        valueRange = 0.5f..1.5f
                                    )
                                }

                                EditTool.SATURATION -> {
                                    SliderControl(
                                        label = stringResource(R.string.editor_saturation),
                                        value = editState.saturation,
                                        onValueChange = { viewModel.updateSaturation(it) },
                                        valueRange = 0f..2f
                                    )
                                }

                                EditTool.FILTER -> {
                                    FilterOptions(
                                        selectedFilter = editState.filter,
                                        onFilterSelected = { viewModel.applyFilter(it) })
                                }

                                EditTool.CROP -> {
                                    CropOptions(
                                        originalBitmap = originalBitmap,
                                        onCropComplete = { croppedBitmap ->
                                            viewModel.updateCroppedBitmap(croppedBitmap)
                                        })
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Button(
                                onClick = {
                                    viewModel.resetEdits()
                                    if (selectedTool == EditTool.CROP) {
                                        selectedTool = EditTool.BRIGHTNESS
                                    }
                                }, colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.reset),
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(stringResource(R.string.editor_reset_edits))
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
    tool: EditTool, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(4.dp)
    ) {
        IconButton(
            onClick = onClick, modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surface
                )
                .border(
                    width = 1.dp, color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline, shape = CircleShape
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(tool.titleRes),
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = stringResource(tool.titleRes),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun SliderControl(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label, style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = String.format("%.1f", value), style = MaterialTheme.typography.bodyMedium
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun FilterOptions(
    selectedFilter: ImageFilter, onFilterSelected: (ImageFilter) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.editor_filter),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            items(ImageFilter.values()) { filter ->
                FilterOption(
                    filter = filter,
                    isSelected = filter == selectedFilter,
                    onClick = { onFilterSelected(filter) })
            }
        }
    }
}

@Composable
fun CropOptions(
    originalBitmap: Bitmap?, onCropComplete: (Bitmap?) -> Unit
) {
    if (originalBitmap == null) {
        Text(
            text = stringResource(R.string.editor_loading_image),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    var showCropView by remember { mutableStateOf(true) }
    var cropType by remember { mutableStateOf(CropType.FREE) }
    var cropRect by remember { mutableStateOf<Rect?>(null) }

    LaunchedEffect(originalBitmap, cropType) {
        val width = originalBitmap.width
        val height = originalBitmap.height

        cropRect = when (cropType) {
            CropType.FREE -> {
                val cropWidth = width * 0.8f
                val cropHeight = height * 0.8f
                val left = (width - cropWidth) / 2
                val top = (height - cropHeight) / 2
                Rect(left, top, left + cropWidth, top + cropHeight)
            }

            CropType.SQUARE -> {
                val size = minOf(width, height) * 0.8f
                val left = (width - size) / 2
                val top = (height - size) / 2
                Rect(left, top, left + size, top + size)
            }

            CropType.CIRCLE -> {
                val size = minOf(width, height) * 0.8f
                val left = (width - size) / 2
                val top = (height - size) / 2
                Rect(left, top, left + size, top + size)
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (showCropView) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(vertical = 8.dp)
                    .background(Color.Black)
            ) {
                Image(
                    bitmap = originalBitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.editor_original),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                cropRect?.let { rect ->
                    val imageWidth = originalBitmap.width.toFloat()
                    val imageHeight = originalBitmap.height.toFloat()
                    val boxWidth = 300.dp.toPx()
                    val boxHeight = 300.dp.toPx()

                    val scaleX = boxWidth / imageWidth
                    val scaleY = boxHeight / imageHeight
                    val scale = minOf(scaleX, scaleY)

                    val uiRect = Rect(
                        rect.left * scale, rect.top * scale, rect.right * scale, rect.bottom * scale
                    )

                    Box(
                        modifier = Modifier
                            .absoluteOffset(uiRect.left.dp, uiRect.top.dp)
                            .size(
                                width = (uiRect.right - uiRect.left).dp,
                                height = (uiRect.bottom - uiRect.top).dp
                            )
                            .border(
                                width = 2.dp,
                                color = Color.White,
                                shape = if (cropType == CropType.CIRCLE) CircleShape else RoundedCornerShape(
                                    4.dp
                                )
                            )
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CropTypeButton(
                    title = stringResource(R.string.editor_free_crop),
                    isSelected = cropType == CropType.FREE,
                    onClick = { cropType = CropType.FREE })
                CropTypeButton(
                    title = stringResource(R.string.editor_square),
                    isSelected = cropType == CropType.SQUARE,
                    onClick = { cropType = CropType.SQUARE })
                CropTypeButton(
                    title = stringResource(R.string.editor_circle),
                    isSelected = cropType == CropType.CIRCLE,
                    onClick = { cropType = CropType.CIRCLE })
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        onCropComplete(null)
                        showCropView = false
                    }, colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cancel),
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(stringResource(R.string.cancel))
                }

                Button(
                    onClick = {
                        cropRect?.let { rect ->
                            val x = rect.left.toInt().coerceAtLeast(0)
                            val y = rect.top.toInt().coerceAtLeast(0)
                            val width = (rect.right - rect.left).toInt()
                                .coerceAtMost(originalBitmap.width - x)
                            val height = (rect.bottom - rect.top).toInt()
                                .coerceAtMost(originalBitmap.height - y)

                            if (width > 0 && height > 0) {
                                val cropped = Bitmap.createBitmap(
                                    originalBitmap, x, y, width, height
                                )

                                val finalBitmap = if (cropType == CropType.CIRCLE) {
                                    createCircularBitmap(cropped)
                                } else {
                                    cropped
                                }

                                onCropComplete(finalBitmap)
                                showCropView = false
                            }
                        }
                    }, colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.apply),
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(stringResource(R.string.apply))
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.editor_crop_applied),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Button(
                onClick = { showCropView = true },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.editor_recrop),
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(stringResource(R.string.editor_recrop))
            }
        }
    }
}

fun createCircularBitmap(bitmap: Bitmap): Bitmap {
    val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(output)
    val paint = android.graphics.Paint()
    val rect = android.graphics.Rect(0, 0, bitmap.width, bitmap.height)

    paint.isAntiAlias = true
    canvas.drawARGB(0, 0, 0, 0)
    paint.color = android.graphics.Color.WHITE

    canvas.drawCircle(
        bitmap.width / 2f, bitmap.height / 2f, minOf(bitmap.width, bitmap.height) / 2f, paint
    )

    paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(bitmap, rect, rect, paint)

    return output
}

enum class CropType {
    FREE, SQUARE, CIRCLE
}

@Composable
fun Dp.toPx(): Float {
    return with(LocalDensity.current) { this@toPx.toPx() }
}

@Composable
fun CropTypeButton(
    title: String, isSelected: Boolean, onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(4.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .border(
                    width = 1.dp, color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(8.dp)
                ), contentAlignment = Alignment.Center
        ) {
            Text(
                text = title.take(1),
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun FilterOption(
    filter: ImageFilter, isSelected: Boolean, onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(4.dp)
            .width(80.dp)
            .clickable(onClick = onClick)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp), border = BorderStroke(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else Color.Transparent
            ), modifier = Modifier.size(64.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(filter.previewColor)
            )
        }
        Text(
            text = stringResource(filter.titleRes),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

enum class EditTool(val titleRes: Int) {
    BRIGHTNESS(R.string.editor_brightness), CONTRAST(R.string.editor_contrast), SATURATION(R.string.editor_saturation), FILTER(
        R.string.editor_filter
    ),
    CROP(R.string.editor_crop)
}

enum class ImageFilter(val titleRes: Int, val previewColor: Color) {
    NONE(R.string.editor_original, Color.White), GRAYSCALE(
        R.string.editor_grayscale,
        Color.Gray
    ),
    SEPIA(R.string.editor_sepia, Color(0xFFD2B48C)), VINTAGE(
        R.string.editor_vintage,
        Color(0xFFCDC9A5)
    ),
    COLD(R.string.editor_cold, Color(0xFF87CEFA)), WARM(
        R.string.editor_warm,
        Color(0xFFFFB347)
    ),
    PURPLE(R.string.editor_purple, Color(0xFFB19CD9)), BLUE(
        R.string.editor_blue,
        Color(0xFF6495ED)
    ),
    GREEN(R.string.editor_green, Color(0xFF90EE90)), PINK(
        R.string.editor_pink,
        Color(0xFFFFB6C1)
    ),
    ORANGE(R.string.editor_orange, Color(0xFFFF8C00))
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
