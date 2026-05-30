package com.luna.budgetapp.presentation.screen.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luna.budgetapp.ui.theme.LazyWalletTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorDialog(
    message: String? = null,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.height(124.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(
                    start = 24.dp,
                    end = 24.dp,
                    top = 32.dp,
                    bottom = 20.dp
                )
                    .fillMaxSize(),
            ) {
                Text(
                    text = message ?: "Something went wrong",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Text(
                        text = "Ok",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(

)
@Composable
fun ErrorDialogPreview() {
    LazyWalletTheme {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            ErrorDialog(
                message = "Error logging in",
                onDismiss = {}
            )
        }
    }
}
