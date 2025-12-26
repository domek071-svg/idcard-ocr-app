package com.idcard.ocr.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.idcard.ocr.network.ApiResult
import com.idcard.ocr.network.NetworkClient
import com.idcard.ocr.network.OCRData
import com.idcard.ocr.network.OCRRequest
import com.idcard.ocr.ui.theme.AppColors
import kotlinx.coroutines.launch

/**
 * Result screen displaying extracted OCR data
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    frontBase64: String,
    backBase64: String,
    onNavigateBack: () -> Unit,
    onScanAnother: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var ocrData by remember { mutableStateOf<OCRData?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Load OCR results
    LaunchedEffect(frontBase64, backBase64) {
        scope.launch {
            isLoading = true
            errorMessage = null

            val apiService = NetworkClient.createApiService()
            val request = OCRRequest(frontBase64, backBase64)

            when (val result = NetworkClient.safeApiCall {
                apiService.processIdCard(request)
            }) {
                is ApiResult.Success -> {
                    ocrData = result.data.data
                    isLoading = false
                }
                is ApiResult.Error -> {
                    errorMessage = result.message
                    isLoading = false
                }
                is ApiResult.Exception -> {
                    errorMessage = "An error occurred: ${result.throwable.message}"
                    isLoading = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Scan Results",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            copyJsonToClipboard(context, ocrData)
                        },
                        enabled = ocrData != null
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy JSON"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    LoadingContent()
                }
                errorMessage != null -> {
                    ErrorContent(
                        message = errorMessage!!,
                        onRetry = onNavigateBack,
                        onScanAnother = onScanAnother
                    )
                }
                ocrData != null -> {
                    ResultsContent(
                        data = ocrData!!,
                        onScanAnother = onScanAnother
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Processing...",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Extracting data from ID card",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    onScanAnother: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Error",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.ErrorRed
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = onRetry) {
                Text("Retry")
            }
            Button(onClick = onScanAnother) {
                Text("Scan Another")
            }
        }
    }
}

@Composable
private fun ResultsContent(
    data: OCRData,
    onScanAnother: () -> Unit
) {
    val gson = remember { GsonBuilder().setPrettyPrinting().create() }
    val jsonString = remember(data) { gson.toJson(data) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // JSON Preview (collapsible)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF5F5F5)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "JSON Data",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = jsonString,
                    fontSize = 10.sp,
                    color = Color.DarkGray,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }

        // Field list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Front side fields
            item {
                SectionHeader(title = "Front Side")
            }

            item {
                FieldCard(label = "Surname", value = data.fNazwisko)
            }
            item {
                FieldCard(label = "Given Names", value = data.fImiona)
            }
            item {
                FieldCard(label = "Citizenship", value = data.fObywatelstwo)
            }
            item {
                FieldCard(label = "Date of Birth", value = data.fDataUrodzenia)
            }
            item {
                FieldCard(label = "Gender", value = data.fPlec)
            }
            item {
                FieldCard(label = "ID Number", value = data.fNumerId)
            }
            item {
                FieldCard(label = "Expiry Date", value = data.fDataWaznosci)
            }

            // Back side fields
            item {
                SectionHeader(title = "Back Side")
            }

            item {
                FieldCard(label = "Series ID", value = data.bSeriaId)
            }
            item {
                FieldCard(label = "Number ID", value = data.bNumerId)
            }
            item {
                FieldCard(label = "Personal ID", value = data.bNumerIdent)
            }
            item {
                FieldCard(label = "Issue Date", value = data.bDataWydania)
            }
            item {
                FieldCard(label = "Issuing Authority", value = data.bKtoWydal)
            }
            item {
                FieldCard(label = "Parents' Names", value = data.bImionaRodzicow)
            }
            item {
                FieldCard(label = "Family Name", value = data.bNazwiskoRodowe)
            }
            item {
                FieldCard(label = "Place of Birth", value = data.bMiejsceUrodzenia)
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Scan Another button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Button(
                onClick = onScanAnother,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan Another ID")
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun FieldCard(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (value.isNotEmpty()) {
                Color.White
            } else {
                Color(0xFFFFEBEE)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = value.ifEmpty { "-" },
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (value.isNotEmpty()) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    AppColors.ErrorRed
                }
            )
        }
    }
}

private fun copyJsonToClipboard(context: Context, data: OCRData?) {
    if (data == null) return

    val gson = Gson()
    val json = gson.toJson(data)

    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("OCR Data", json)
    clipboard.setPrimaryClip(clip)

    Toast.makeText(context, "JSON copied to clipboard", Toast.LENGTH_SHORT).show()
}
