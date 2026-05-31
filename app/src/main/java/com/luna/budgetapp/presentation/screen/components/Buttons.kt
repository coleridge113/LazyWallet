package com.luna.budgetapp.presentation.screen.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Preview
import com.luna.budgetapp.ui.theme.LazyWalletTheme

@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary
        )
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
fun SecondaryButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary
        )
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(
    uiMode = AndroidUiModes.UI_MODE_NIGHT_YES
)
@Composable
fun PrimaryButtonPreview() {
    LazyWalletTheme {
        PrimaryButton(
            onClick = {},
            text = "Sign In",
            modifier = Modifier
                .padding(16.dp)
        )
    }
}

@Preview(
    uiMode = AndroidUiModes.UI_MODE_NIGHT_YES
)
@Composable
fun SecondaryButtonPreview() {
    LazyWalletTheme {
        SecondaryButton(
            onClick = {},
            text = "Sign Up",
            modifier = Modifier
                .padding(16.dp)
        )
    }
}


@Preview(
    uiMode = AndroidUiModes.UI_MODE_NIGHT_YES
)
@Composable
fun ButtonsTogetherPreview() {
    LazyWalletTheme {
        Row {
            SecondaryButton(
                onClick = {},
                text = "Sign Up",
                modifier = Modifier.padding(16.dp)
            )

            PrimaryButton(
                onClick = {},
                text = "Sign In",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}