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
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
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
    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { images.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Termograma") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
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
            if (images.isEmpty()) {
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
                    val pagePath = images[page]
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
                    images.forEachIndexed { index, _ ->
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
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(images) { index, path ->
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
    }
}

