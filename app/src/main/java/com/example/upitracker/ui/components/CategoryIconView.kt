package com.example.upitracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.upitracker.util.CategoryIcon

@Composable
fun CategoryIconView(
    categoryIcon: CategoryIcon,
    size: Dp = 24.dp, // Default size
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    iconTint: Color = MaterialTheme.colorScheme.secondary
) {
    when (categoryIcon) {
        is CategoryIcon.VectorIcon -> {
            Icon(
                imageVector = categoryIcon.image,
                contentDescription = null,
                modifier = Modifier.size(size),
                tint = iconTint
            )
        }
        is CategoryIcon.EmojiIcon -> {
            // ✨ Render Emoji centered in a circle
            Box(
                modifier = Modifier
                    .size(size)
                    .background(color = containerColor, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = categoryIcon.emoji,
                    style = MaterialTheme.typography.titleMedium, // Adjust size as needed
                    fontWeight = FontWeight.Normal
                )
            }
        }
        is CategoryIcon.LetterIcon -> {
            Box(
                modifier = Modifier
                    .size(size)
                    .background(color = containerColor, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = categoryIcon.letter.toString(),
                    color = contentColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}