package com.josephm101.chargingsoundchanger.ui.components.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.josephm101.chargingsoundchanger.ui.components.card.CardConstants.cardDefaultModifier
import com.josephm101.chargingsoundchanger.ui.components.card.CardConstants.cardIconSize
import com.josephm101.chargingsoundchanger.ui.components.card.CardConstants.cardInnerPadding

object CustomCard {
    @Composable
    fun CustomCardBase(cardContainerColor: Color? = null, content: @Composable () -> Unit) {
        OutlinedCard(
            colors = if (cardContainerColor != null) {
                CardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = CardDefaults.outlinedCardColors().contentColor,
                    disabledContainerColor = CardDefaults.outlinedCardColors().disabledContainerColor,
                    disabledContentColor = CardDefaults.outlinedCardColors().disabledContentColor,
                )
            } else {
                CardColors(
                    containerColor = CardDefaults.outlinedCardColors().containerColor,
                    contentColor = CardDefaults.outlinedCardColors().contentColor,
                    disabledContainerColor = CardDefaults.outlinedCardColors().disabledContainerColor,
                    disabledContentColor = CardDefaults.outlinedCardColors().disabledContentColor,
                )
            },
                    /*
                    colors = CardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = CardDefaults.outlinedCardColors().contentColor,
                        disabledContainerColor = CardDefaults.outlinedCardColors().disabledContainerColor,
                        disabledContentColor = CardDefaults.outlinedCardColors().disabledContentColor,
                    ),
                     */
            //elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
            modifier = cardDefaultModifier
        ) {
            content()
        }
    }

    @Composable
    fun CustomColumnWithPaddingForCard(content: @Composable () -> Unit) {
        Column(
            modifier = Modifier.padding(cardInnerPadding)
        ) {
            content()
        }
    }

    @Composable
    fun CardTitleText(text: String) {
        val titleTextFontSize = 20.sp
        Text(
            text = text,
            style = TextStyle(fontSize = titleTextFontSize)
        )
    }


    @Composable
    fun CustomCardWithTitleAndIconAndContent(
        title: String,
        iconResId: Int,
        cardContainerColor: Color? = null,
        content: @Composable () -> Unit,
    ) {
        CustomCardBase(cardContainerColor = cardContainerColor) {
            CustomColumnWithPaddingForCard {
                Row(
                    modifier = Modifier.padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (iconResId != -1) {
                        Icon(
                            painter = painterResource(id = iconResId),
                            modifier = Modifier.size(cardIconSize, cardIconSize),
                            contentDescription = ""
                        )
                        Spacer(modifier = Modifier.size(width = 8.dp, height = 0.dp))
                    }
                    CardTitleText(title)
                }
                content()
            }
        }
    }
}