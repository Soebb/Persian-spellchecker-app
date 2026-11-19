package com.soebb.nevise

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NeviseScreen()
                }
            }
        }
    }
}

@Composable
private fun NeviseScreen() {
    var inputText by remember { mutableStateOf("") }
    var correctedText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "نویسه",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "متن فارسی‌ات را بنویس تا از نظر املایی بررسی و اصلاح شود.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = inputText,
                onValueChange = {
                    inputText = it
                    errorMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("متن برای اصلاح") },
                placeholder = { Text("متن فارسی را اینجا وارد کن...") },
                minLines = 5,
                maxLines = 10,
                textStyle = TextStyle(textDirection = TextDirection.Rtl),
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    if (inputText.isNotBlank()) {
                        scope.launch {
                            isLoading = true
                            errorMessage = null
                            correctedText = ""
                            try {
                                correctedText = NeviseApi.correctText(inputText.trim())
                            } catch (exception: Exception) {
                                errorMessage = exception.localizedMessage
                                    ?: "ارتباط با سرور با خطا مواجه شد."
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = inputText.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("تصحیح متن")
                }
            }

            if (isLoading) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "در حال بررسی و اصلاح متن...",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = errorMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textDirection = TextDirection.Ltr
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(errorMessage.orEmpty()))
                    }
                ) {
                    Text("کپی متن خطا")
                }
            }

            if (correctedText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(26.dp))
                Text(
                    text = "متن اصلاح‌شده",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = correctedText,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            textDirection = TextDirection.Rtl
                        ),
                        textAlign = TextAlign.Start
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { clipboard.setText(AnnotatedString(correctedText)) }
                    ) {
                        Text("کپی متن")
                    }
                    OutlinedButton(
                        onClick = {
                            inputText = correctedText
                            correctedText = ""
                        }
                    ) {
                        Text("ویرایش دوباره")
                    }
                }
            }
        }
    }
}
