package com.microblink.blinkcard.ux.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.microblink.blinkcard.ux.R

@Composable
fun DemoOverlay(modifier: Modifier) {
    Image(
        modifier = modifier,
        painter = painterResource(R.drawable.mb_blinkcard_logo),
        contentDescription = "Microblink demo overlay"
    )
}