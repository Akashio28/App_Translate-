package com.example.app_translate.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.app_translate.data.model.Language
import com.example.app_translate.data.model.languages
import androidx.compose.ui.text.font.FontWeight
import com.example.app_translate.ui.theme.PurpleColor

@Composable
fun LanguagePickerDialog(
    title: String,
    currentLang: Language,
    onLanguageSelected: (Language) -> Unit,
    onDismiss: () -> Unit,
    showAutoDetect: Boolean = false,
    onAutoDetectSelected: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                if (showAutoDetect && onAutoDetectSelected != null) {
                    TextButton(
                        onClick = {
                            onAutoDetectSelected()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Auto Detect",
                            color = PurpleColor,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                }
                languages.forEach { lang ->
                    TextButton(
                        onClick = {
                            onLanguageSelected(lang)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = lang.name,
                            color = if (lang.code == currentLang.code) PurpleColor else Color.Black
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = PurpleColor)
            }
        }
    )
}