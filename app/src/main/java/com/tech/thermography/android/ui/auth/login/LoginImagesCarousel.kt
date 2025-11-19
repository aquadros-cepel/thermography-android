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
        R.drawable.thermography_0,
        R.drawable.thermography_1,
        R.drawable.thermography_2,
        R.drawable.thermography_3,
        R.drawable.thermography_4,
        R.drawable.thermography_5,
        R.drawable.thermography_6
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