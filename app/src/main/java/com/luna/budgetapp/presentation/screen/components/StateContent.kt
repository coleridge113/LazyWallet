package com.luna.budgetapp.presentation.screen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luna.budgetapp.R
import com.luna.budgetapp.ui.theme.LazyWalletTheme

@Composable
fun LoadingContent(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.state_loading_description)
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.semantics { contentDescription = description },
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun ErrorContent(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.state_error_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = message?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.state_error_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(onClick = onRetry) {
                Text(stringResource(R.string.state_retry))
            }
        }
    }
}

@Preview
@Composable
fun LoadingContentPreview() {
    LazyWalletTheme {
        Surface(modifier = Modifier.fillMaxSize()) { LoadingContent() }
    }
}

@Preview
@Composable
fun ErrorContentPreview() {
    LazyWalletTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            ErrorContent(onRetry = {})
        }
    }
}
