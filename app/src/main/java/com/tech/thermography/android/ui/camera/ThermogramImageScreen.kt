package com.tech.thermography.android.ui.camera

import android.graphics.BitmapFactory
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThermogramImageScreen(
    imagePath: String,
    imagePaths: List<String> = emptyList(),
    onBack: () -> Unit
) {
    val images = remember(imagePath, imagePaths) {
        buildList {
            addAll(imagePaths.filter { it.isNotBlank() })
            if (imagePath.isNotBlank() && !contains(imagePath)) {
                add(0, imagePath)
            }
        }.distinct()
    }
    val initialIndex = remember(images, imagePath) { images.indexOf(imagePath).takeIf { it >= 0 } ?: 0 }
    var imagesState by remember { mutableStateOf(images) }
    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { imagesState.size })
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Termograma") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Excluir termograma")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(innerPadding)
        ) {
            if (imagesState.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Imagem nao encontrada", color = MaterialTheme.colorScheme.onSurface)
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { page ->
                    val pagePath = imagesState[page]
                    val imageBitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
                        initialValue = null,
                        key1 = pagePath
                    ) {
                        value = withContext(Dispatchers.IO) {
                            BitmapFactory.decodeFile(pagePath)?.asImageBitmap()
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (imageBitmap != null) {
                            Image(
                                bitmap = imageBitmap!!,
                                contentDescription = "Termograma",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            CircularProgressIndicator()
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xCC101010))
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    imagesState.forEachIndexed { index, _ ->
                        val selected = index == pagerState.currentPage
                        val dotSize by animateDpAsState(
                            targetValue = if (selected) 10.dp else 7.dp,
                            animationSpec = spring(),
                            label = "dotSize"
                        )
                        val dotColor by animateColorAsState(
                            targetValue = if (selected) Color.White else Color.Gray,
                            animationSpec = spring(),
                            label = "dotColor"
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(dotSize)
                                .background(dotColor, CircleShape)
                        )
                    }
                }

                Surface(
                    color = Color(0xCC101010),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(98.dp)
                ) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(imagesState) { index, path ->
                            val thumb by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
                                initialValue = null,
                                key1 = path
                            ) {
                                value = withContext(Dispatchers.IO) {
                                    BitmapFactory.decodeFile(path)?.asImageBitmap()
                                }
                            }

                            val selected = index == pagerState.currentPage
                            Box(
                                modifier = Modifier
                                    .size(74.dp)
                                    .border(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) Color.White else Color.Gray,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        scope.launch { pagerState.animateScrollToPage(index) }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (thumb != null) {
                                    Image(
                                        bitmap = thumb!!,
                                        contentDescription = "Thumbnail",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Excluir termograma?") },
                text = { Text("Deseja realmente excluir este termograma?") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        scope.launch {
                            try {
                                val toDelete = imagesState.getOrNull(pagerState.currentPage)
                                if (toDelete != null) {
                                    java.io.File(toDelete).delete()
                                    val newList = imagesState.filter { it != toDelete }
                                    imagesState = newList
                                    // Ajusta o pager para índice válido
                                    if (newList.isNotEmpty()) {
                                        val newIndex = pagerState.currentPage.coerceAtMost(newList.lastIndex)
                                        pagerState.scrollToPage(newIndex)
                                    } else {
                                        onBack()
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                    }) {
                        Text("Excluir")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}
