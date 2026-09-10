package com.paradox.finance.ui.dialogs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.paradox.finance.ui.theme.*
import com.paradox.finance.ui.viewmodels.ExpenseViewModel
import java.io.File
import java.io.FileOutputStream

@Composable
fun ImportCsvDialog(
    expenseViewModel: ExpenseViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    val isSubmitting by expenseViewModel.isSubmitting.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val tempFile = File(context.cacheDir, "import_statement_${System.currentTimeMillis()}.csv")
                val outputStream = FileOutputStream(tempFile)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
                selectedFile = tempFile
                selectedFileName = "Bank_Statement.csv"
            } catch (e: Exception) {
                selectedFileName = null
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(ObsidianCanvas)
                    .border(1.dp, GlassBorderStroke, RoundedCornerShape(24.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Import Bank Statement CSV", color = OnSurfaceHigh, fontSize = 16.sp, style = Typography.headlineSmall)
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = MutedOutline)
                    }
                }

                // File Upload Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(GlassSurface1)
                        .border(1.dp, GlassBorderStroke, RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.CloudUpload, contentDescription = "Upload", tint = ElectricEmerald, modifier = Modifier.size(40.dp))
                        Text(
                            text = selectedFileName ?: "Tap to select CSV statement",
                            color = if (selectedFileName != null) ElectricEmerald else OnSurface,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Text(
                            text = "Supports HDFC, ICICI, SBI, Axis, Kotak, CRED CSVs",
                            color = MutedOutline,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Button(
                    onClick = { filePickerLauncher.launch("text/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = GlassSurface2),
                    shape = RoundedCornerShape(9999.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Choose CSV File", color = OnSurface, fontSize = 13.sp)
                }

                if (selectedFile != null) {
                    Button(
                        onClick = {
                            selectedFile?.let { expenseViewModel.importCsv(it) }
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricEmerald),
                        shape = RoundedCornerShape(9999.dp),
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        Text(
                            text = if (isSubmitting) "Classifying with AI..." else "Batch Import & Categorize",
                            color = PitchBlack,
                            fontSize = 13.sp,
                            style = Typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}
