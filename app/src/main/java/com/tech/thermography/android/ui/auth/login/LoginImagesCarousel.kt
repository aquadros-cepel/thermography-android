package com.tech.thermography.android.ui.auth.login

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.tech.thermography.android.R
import kotlinx.coroutines.delay

@Composable
fun LoginImagesCarousel(modifier: Modifier = Modifier) {
    val images = listOf(
        R.drawable.flir0,
        R.drawable.flir1,
        R.drawable.flir2,
        R.drawable.flir3,
        R.drawable.flir4,
        R.drawable.flir5,
        R.drawable.flir6,
        R.drawable.flir7,
        R.drawable.flir8,
        R.drawable.flir9,
        R.drawable.flir10
    )

    var index by remember { mutableStateOf(0) }

    // troca automática a cada 2500ms
    LaunchedEffect(Unit) {
        while (true) {
            delay(2500)
            index = (index + 1) % images.size
        }
    }

    Crossfade(
        targetState = index,
        animationSpec = tween(
            durationMillis = 600,
            easing = FastOutSlowInEasing
        )
    ) { currentIndex ->
        Image(
            painter = painterResource(id = images[currentIndex]),
            contentDescription = "Thermography Carousel",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}