package com.josephm101.chargingsoundchanger.ui.components.card

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.josephm101.chargingsoundchanger.ui.components.card.CardConstants.cardDefaultBodyTextStyle
import com.josephm101.chargingsoundchanger.ui.components.card.CardConstants.cardDefaultModifier
import com.josephm101.chargingsoundchanger.ui.components.card.CardConstants.cardInnerPadding

@Composable
fun SwitchCard(
    title: String,
    description: String,
    booleanValue: Boolean,
    onCheckedChange: (it: Boolean) -> Unit
) {
    var checked by remember { mutableStateOf(booleanValue) }
    OutlinedCard(
        //elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
        modifier = cardDefaultModifier,
        onClick = {
            checked = !checked
            onCheckedChange(checked)
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(cardInnerPadding)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight()
            ) {
                CustomCard.CardTitleText(text = title)
                Spacer(
                    modifier = Modifier
                        .size(width = 0.dp, height = 8.dp)
                )
                Text(
                    text = description,
                    style = cardDefaultBodyTextStyle,
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = {
                    checked = it
                    onCheckedChange(checked)
                }
            )
        }
    }
}