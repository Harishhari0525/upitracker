package com.example.upitracker.ui.components.expressive

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.upitracker.util.ExpressiveTokens
import com.example.upitracker.util.AppMotion
import com.example.upitracker.util.expressiveHeroGradient

@Composable
fun ExpressiveHeroCard(
    title: String,
    amount: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    debitLabel: String? = null,
    creditLabel: String? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = ExpressiveTokens.corners.hero,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = ExpressiveTokens.elevation.card
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 210.dp)
                .background(expressiveHeroGradient())
                .padding(
                    horizontal = ExpressiveTokens.spacing.xl,
                    vertical = ExpressiveTokens.spacing.lg
                ),
            contentAlignment = Alignment.BottomStart
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.sm)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Spacer(modifier = Modifier.height(ExpressiveTokens.spacing.md))

                AnimatedContent(
                    targetState = amount,
                    transitionSpec = {
                        fadeIn(animationSpec = AppMotion.defaultEffects()) togetherWith
                                fadeOut(animationSpec = AppMotion.fastEffects()) using
                                SizeTransform(clip = false)
                    },
                    label = "hero_amount_animation"
                ) { targetAmount ->
                    Text(
                        text = targetAmount,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(ExpressiveTokens.spacing.xs))

                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f)
                    )
                }

                if (!debitLabel.isNullOrBlank() || !creditLabel.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(ExpressiveTokens.spacing.md))

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.sm)
                    ) {
                        if (!debitLabel.isNullOrBlank()) {
                            AssistChip(
                                onClick = {},
                                enabled = false,
                                label = {
                                    Text(
                                        text = debitLabel,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowDownward,
                                        contentDescription = null
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.32f),
                                    disabledLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    disabledLeadingIconContentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }

                        if (!creditLabel.isNullOrBlank()) {
                            AssistChip(
                                onClick = {},
                                enabled = false,
                                label = {
                                    Text(
                                        text = creditLabel,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowUpward,
                                        contentDescription = null
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.32f),
                                    disabledLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    disabledLeadingIconContentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
