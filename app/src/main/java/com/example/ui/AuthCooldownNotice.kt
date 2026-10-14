package com.example.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.util.AuthAttemptLimiter
import kotlinx.coroutines.delay

/** Display-only countdown. Every authentication callback still checks the persisted policy. */
@Composable
internal fun AuthCooldownNotice(limiter: AuthAttemptLimiter) {
    var seconds by remember(limiter) { mutableLongStateOf(limiter.remainingSeconds()) }
    LaunchedEffect(limiter) {
        while (true) {
            seconds = limiter.remainingSeconds()
            delay(250)
        }
    }
    if (seconds > 0) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("settings_auth_cooldown")
        ) {
            Text(
                text = stringResource(R.string.auth_try_again, seconds),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
