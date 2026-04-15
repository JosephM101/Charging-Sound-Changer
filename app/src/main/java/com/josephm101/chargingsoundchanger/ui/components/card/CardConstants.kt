package com.josephm101.chargingsoundchanger.ui.components.card

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object CardConstants {
    // Constants
    val cardDefaultBodyTextStyle = TextStyle(fontSize = 13.sp)
    val cardElevation = 0.dp
    val cardInnerPadding = 20.dp
    val cardOuterPaddingBottom = 16.dp
    val cardIconSize = 30.dp
    var cardDefaultModifier = Modifier.Companion
        //.size(width = 300.dp, height = 100.dp)
        .fillMaxWidth()
        .wrapContentHeight()
        .padding(bottom = cardOuterPaddingBottom)
}