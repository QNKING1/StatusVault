package com.statusvault.app.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.statusvault.app.data.entity.StatusFile
import com.statusvault.app.ui.theme.SelectionActiveColor
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StatusGridItem(
    status: StatusFile,
    isSelected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(2.dp)
            .aspectRatio(1f) // Ensure grid items are visible squares
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) SelectionActiveColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .then(
                if (selectionMode) {
                    Modifier
                } else {
                    Modifier
                }
            ),
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Thumbnail
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(File(status.path))
                    .crossfade(true)
                    .build(),
                contentDescription = status.fileName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Video play icon overlay
            if (status.type == StatusFile.StatusType.VIDEO) {
                Icon(
                    imageVector = Icons.Default.PlayCircleOutline,
                    contentDescription = "Video",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }

            // Selection checkmark
            if (selectionMode && isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SelectionActiveColor.copy(alpha = 0.3f))
                )
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(24.dp),
                    tint = SelectionActiveColor
                )
            }

            // Saved indicator
            if (status.isSaved && !selectionMode) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Saved",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
