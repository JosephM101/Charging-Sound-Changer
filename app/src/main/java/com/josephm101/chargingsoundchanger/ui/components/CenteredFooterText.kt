package com.josephm101.chargingsoundchanger.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CenteredFooterText(text: String) {
    Text(
        text,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(0.6f)
            .basicMarquee(),
        textAlign = TextAlign.Center,
        style = TextStyle(
            fontSize = 14.sp,
        )
    )
}