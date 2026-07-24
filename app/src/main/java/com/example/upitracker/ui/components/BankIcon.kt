package com.example.upitracker.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.upitracker.R
import com.example.upitracker.util.ExpressiveTokens

/**
 * Shows a bundled bank mark for recognized transactions and a category initial otherwise.
 */
@Composable
fun BankIcon(
    bankName: String?,
    fallbackLabel: String?,
    fallbackColor: Color,
    modifier: Modifier = Modifier
) {
    val logoResource = remember(bankName) { bankLogoResource(bankName) }
    val shape = ExpressiveTokens.corners.medium

    Box(
        modifier = modifier
            .size(42.dp)
            .clip(shape)
            .background(
                if (logoResource != null) Color.White
                else fallbackColor.copy(alpha = 0.14f)
            )
            .border(
                width = 1.dp,
                color = if (logoResource != null) {
                    Color.Black.copy(alpha = 0.07f)
                } else {
                    fallbackColor.copy(alpha = 0.12f)
                },
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (logoResource != null) {
            Image(
                painter = painterResource(logoResource),
                contentDescription = null,
                modifier = Modifier.padding(7.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            Text(
                text = fallbackLabel
                    ?.trim()
                    ?.firstOrNull()
                    ?.uppercase()
                    ?: "•",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = fallbackColor
            )
        }
    }
}

private fun bankLogoResource(bankName: String?): Int? {
    val key = bankName
        ?.lowercase()
        ?.filter { it.isLetterOrDigit() }
        .orEmpty()

    return when {
        key.contains("hdfc") -> R.drawable.bank_hdfc
        key.contains("icici") -> R.drawable.bank_icici
        key == "sbi" || key.contains("statebankofindia") -> R.drawable.bank_sbi
        key.contains("axis") -> R.drawable.bank_axis
        key.contains("kotak") -> R.drawable.bank_kotak
        key.contains("paytm") -> R.drawable.bank_paytm
        key.contains("citibank") || key == "citi" -> R.drawable.bank_citi
        key.contains("yesbank") -> R.drawable.bank_yes
        key.contains("idfc") -> R.drawable.bank_idfc_first
        key.contains("indusind") -> R.drawable.bank_indusind
        key == "bob" || key.contains("bankofbaroda") -> R.drawable.bank_bob
        key == "pnb" || key.contains("punjabnationalbank") -> R.drawable.bank_pnb
        key == "ubi" || key.contains("unionbankofindia") -> R.drawable.bank_union
        key.contains("canara") -> R.drawable.bank_canara
        key == "au" || key.contains("ausmallfinance") || key.contains("aubank") ->
            R.drawable.bank_au
        key.contains("federal") -> R.drawable.bank_federal
        key == "rbl" || key.contains("rblbank") -> R.drawable.bank_rbl
        key == "idbi" || key.contains("idbibank") -> R.drawable.bank_idbi
        else -> null
    }
}
