package com.p4handheld.ui.screens

import android.graphics.Paint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.p4handheld.data.ScanStateHolder
import com.p4handheld.data.models.Message
import com.p4handheld.data.models.Prompt
import com.p4handheld.data.models.PromptItem
import com.p4handheld.data.models.PromptType
import com.p4handheld.ui.compose.theme.HandheldP4WTheme
import com.p4handheld.ui.screens.viewmodels.ActionUiState
import com.p4handheld.ui.screens.viewmodels.ActionViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Path as ComposePath

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionScreen(
    menuItemLabel: String,
    pageKey: String,
    onNavigateBack: () -> Unit
) {
    val viewModel: ActionViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Observe scan data from DataWedge
    val scanViewState by ScanStateHolder.scanViewStatus.observeAsState()

    BackHandler {
        onNavigateBack()
    }

    // Initialize action on first composition with debug logging
    LaunchedEffect(pageKey) {
        if (pageKey.isNotEmpty()) {
            println("ActionScreen: Initializing action with state: $pageKey")
            viewModel.processAction(pageKey)
        } else {
            println("ActionScreen: Warning - menuItemState is empty, cannot initialize action")
        }
    }

    // Auto-scroll to bottom when messages change
    LaunchedEffect(uiState.messageStack.size) {
        if (uiState.messageStack.isNotEmpty()) {
            println("ActionScreen: Message stack updated, size: ${uiState.messageStack.size}")
            coroutineScope.launch {
                listState.animateScrollToItem(uiState.messageStack.size - 1)
            }
        }
    }

    // Debug logging for UI state changes
    LaunchedEffect(uiState.currentPrompt) {
        uiState.currentPrompt?.let { prompt ->
            println("ActionScreen: Current prompt type: ${prompt.promptType}")
        }
    }

    // Handle scan data from DataWedge for Scan prompt type
    LaunchedEffect(scanViewState?.dwOutputData) {
        scanViewState?.dwOutputData?.let { outputData ->
            if (uiState.currentPrompt?.promptType == PromptType.SCAN && outputData.data.isNotEmpty()) {
                println("ActionScreen: Scan data received from DataWedge: ${outputData.data}")
                viewModel.updatePromptValue(outputData.data)
                // Automatically send the scanned data
                viewModel.processAction(pageKey, outputData.data)
            }
        }
    }

    val prompt: @Composable () -> Unit = when (uiState.currentPrompt?.promptType) {
        PromptType.PHOTO -> {
            {
                PhotoPromptScreen(
                    capturedImage = uiState.capturedImage,
                    onImageCaptured = { imageBase64 ->
                        viewModel.setCapturedImage(imageBase64)
                    },
                    onSendImage = { imageBase64 ->
                        viewModel.processAction(pageKey, imageBase64)
                    },
                    onRetakePhoto = {
                        viewModel.setCapturedImage(null)
                    }
                )
            }
        }

        PromptType.SIGN -> {
            if (uiState.showSignature) {
                {
                    SignaturePromptScreen(
                        onSignatureSaved = { signatureBase64 ->
                            viewModel.processAction(pageKey, signatureBase64)
                            viewModel.setShowSignature(false)
                        },
                        onCancel = {
                            viewModel.setShowSignature(false)
                        }
                    )
                }
            } else {
                {
                    DefaultActionScreen(
                        uiState = uiState,
                        listState = listState,
                        menuItemState = pageKey,
                        viewModel = viewModel
                    )
                }
            }
        }

        else -> {
            {
                DefaultActionScreen(
                    uiState = uiState,
                    listState = listState,
                    menuItemState = pageKey,
                    viewModel = viewModel
                )
            }
        }
    }

    ActionScreenWrapper(
        menuItemLabel = menuItemLabel,
        menuItemState = pageKey,
        prompt = prompt,
        uiState = uiState
    )

}

@Composable
fun ActionScreenWrapper(
    menuItemLabel: String,
    menuItemState: String,
    prompt: @Composable () -> Unit,
    uiState: ActionUiState
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = menuItemLabel,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )

                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }

        prompt()
    }
}

@Composable
fun DefaultActionScreen(
    uiState: ActionUiState,
    listState: LazyListState,
    menuItemState: String,
    viewModel: ActionViewModel
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        // Toolbar actions
        if (uiState.currentResponse?.toolbarActions?.isNotEmpty() == true) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.currentResponse.toolbarActions) { action ->
                        Button(
                            onClick = {
                                println("ActionScreen: Toolbar action clicked: $action")
                                viewModel.processAction(menuItemState, null, action)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text(action)
                        }
                    }
                }
            }
        }

        // Messages panel with enhanced scrollable area
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 1.dp, vertical = 4.dp),
            shape = RectangleShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column {
                // Messages list
                if (uiState.messageStack.isNotEmpty()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.messageStack.size) { index ->
                            val message = uiState.messageStack[index]
                            MessageCard(
                                message = message,
                                onClick = {
                                    println("ActionScreen: Message clicked: ${message.title}")
                                    viewModel.onMessageClick(message, index)
                                }
                            )
                        }
                    }
                } else {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {


                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (uiState.isLoading) "Loading messages..." else "No messages yet",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Input area based on prompt type
        uiState.currentPrompt?.let { prompt ->
            PromptInputArea(
                prompt = prompt,
                promptValue = uiState.promptValue,
                onPromptValueChange = {
                    println("ActionScreen: Prompt value changed: $it")
                    viewModel.updatePromptValue(it)
                },
                onSendPrompt = { value ->
                    println("ActionScreen: Sending prompt value: $value")
                    viewModel.processAction(menuItemState, value)
                },
                onShowSignature = {
                    println("ActionScreen: Showing signature prompt")
                    viewModel.setShowSignature(true)
                }
            )
        } ?: run {
            // Show placeholder when no prompt is available
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RectangleShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (uiState.isLoading) "Loading..." else "Waiting for prompt...",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun MessageCard(
    message: Message,
    onClick: () -> Unit
) {
    if (message.title == "divider") {
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = Color.Gray
        )
        return
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = when (message.severity) {
                "Info" -> if (message.isCommitted) Color(0xFFE3F2FD) else Color.White
                "Warn" -> Color(0xFFFFF3E0)
                "Error" -> Color(0xFFFFEBEE)
                "Success" -> Color(0xFFE8F5E8)
                else -> if (message.isCommitted) Color(0xFFF5F5F5) else Color.White
            }
        ),
        shape = RoundedCornerShape(5.dp),
        border = BorderStroke(
            width = 1.dp,
            color = when (message.severity) {
                "Info" -> Color(0xFFBBDEFB)
                "Warn" -> Color(0xFFFFE0B2)
                "Error" -> Color(0xFFFFCDD2)
                "Success" -> Color(0xFFC8E6C9)
                else -> Color(0xFFE0E0E0)
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp)
        ) {
            // Image if available
            message.imageResource?.let { imageResource ->
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = message.title,
                    fontSize = 18.sp,
                    color = when (message.severity) {
                        "Error" -> Color(0xFFD32F2F)
                        "Warn" -> Color(0xFFF57C00)
                        "Success" -> Color(0xFF388E3C)
                        else -> Color.Black
                    },
                )

                if (message.subtitle?.isNotEmpty() == true) {
                    Text(
                        text = spaceCamel(message.subtitle),
                        fontSize = 12.sp,
                        color = Color(0xFF1976D2),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (message.subtitle2?.isNotEmpty() == true) {
                    Text(
                        text = spaceCamel(message.subtitle2),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                }
            }
        }
    }
}

@Composable
fun PromptInputArea(
    prompt: Prompt,
    promptValue: String,
    onPromptValueChange: (String) -> Unit,
    onSendPrompt: (String) -> Unit,
    onShowSignature: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.background)
    ) {
        when (prompt.promptType) {
            PromptType.SCAN -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Scan input field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        OutlinedTextField(
                            value = promptValue,
                            onValueChange = onPromptValueChange,
                            textStyle = TextStyle(textAlign = TextAlign.Center),
                            label = {
                                if (prompt.promptPlaceholder != null && prompt.promptPlaceholder.length <= 20) {
                                    Text(prompt.promptPlaceholder)
                                } else {
                                    Text(
                                        text = prompt.promptPlaceholder.take(20) + "...",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Send
                            ),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (promptValue.isNotEmpty()) {
                                        onSendPrompt(promptValue)
                                    }
                                }
                            ),
                            trailingIcon = {
                                if (promptValue.isNotEmpty()) {
                                    IconButton(onClick = { onPromptValueChange("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (promptValue.isNotEmpty()) {
                                    onSendPrompt(promptValue)
                                }
                            },
                            enabled = promptValue.isNotEmpty(),
                            modifier = Modifier.height(56.dp),
                            shape = RoundedCornerShape(5.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Text(
                                text = "Send",
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            PromptType.CONFIRM -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(
                        text = "Confirm?",
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    Button(
                        onClick = { onSendPrompt("true") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        shape = RoundedCornerShape(5.dp)
                    ) {
                        Text("Yes")
                    }
                    Button(
                        onClick = { onSendPrompt("false") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF44336)
                        ),
                        shape = RoundedCornerShape(5.dp)
                    ) {
                        Text("No")
                    }
                }
            }

            PromptType.SIGN -> {
                Button(
                    onClick = onShowSignature,
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(5.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Sign")
                }
            }

            PromptType.PICKER -> {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    items(prompt.items) { item ->
                        Button(
                            onClick = { onSendPrompt(item.value) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(5.dp)
                        ) {
                            Text(item.label)
                        }
                    }
                }
            }

            PromptType.GO_TO_NEW_PAGE -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    OutlinedTextField(
                        value = promptValue,
                        onValueChange = onPromptValueChange,
                        label = { Text(prompt.promptPlaceholder) },
                        modifier = Modifier.weight(1f),
                        enabled = false
                    )
                }
            }

            else -> {
                // Text input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = promptValue,
                        onValueChange = onPromptValueChange,
                        label = { Text(prompt.promptPlaceholder) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (promptValue.isNotEmpty()) {
                                    onSendPrompt(promptValue)
                                }
                            }
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (promptValue.isNotEmpty()) {
                                onSendPrompt(promptValue)
                            }
                        },
                        enabled = promptValue.isNotEmpty(),
                        modifier = Modifier.height(56.dp),
                        shape = RoundedCornerShape(5.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text("Send")
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoPromptScreen(
    capturedImage: String?,
    onImageCaptured: (String) -> Unit,
    onSendImage: (String) -> Unit,
    onRetakePhoto: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (capturedImage != null) {
            // Show captured image placeholder
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.LightGray, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("Captured Image")
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { onSendImage(capturedImage) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(5.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Send")
                }

                Button(
                    onClick = onRetakePhoto,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9800)
                    ),
                    shape = RoundedCornerShape(5.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Retake")
                }
            }
        } else {
            // Show camera placeholder
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.LightGray, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp)
                    )
                    Text("Camera Preview")
                }
            }

            Button(
                onClick = {
                    // Simulate capturing an image
                    onImageCaptured("data:image/jpeg;base64,fake_image_data")
                },
                shape = RoundedCornerShape(5.dp),
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Capture")
            }
        }
    }
}

@Composable
fun SignaturePromptScreen(
    onSignatureSaved: (String) -> Unit,
    onCancel: () -> Unit
) {

    var paths by remember { mutableStateOf(listOf<ComposePath>()) }
    val paint = remember {
        Paint().apply {
            color = Color.Black.toArgb()
            strokeWidth = 5f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .navigationBarsPadding()
    ) {
        Text(
            text = "Please sign below",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Signature canvas
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(8.dp))
                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            paths = paths + ComposePath().apply {
                                moveTo(offset.x, offset.y)
                            }
                        },
                        onDrag = { change, _ ->
                            if (paths.isNotEmpty()) {
                                val newPath = ComposePath().apply {
                                    addPath(paths.last())
                                    lineTo(change.position.x, change.position.y)
                                }
                                paths = paths.dropLast(1) + newPath
                            }
                        }
                    )
                }
        ) {
            paths.forEach { path ->
                drawPath(
                    path = path,
                    color = Color.Black,
                    style = Stroke(width = 5.dp.toPx())
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { paths = emptyList() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9800)
                ),
                shape = RoundedCornerShape(5.dp)
            ) {
                Text("Clear")
            }

            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF44336)
                ),
                shape = RoundedCornerShape(5.dp)
            ) {
                Text("Cancel")
            }

            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(MaterialTheme.colorScheme.primary.value)
                ),
                shape = RoundedCornerShape(5.dp)
            ) {
                Text("Save")
            }
        }
    }
}

// ==================== PREVIEW COMPOSABLES ====================

// Sample data for previews
private val sampleMessages = listOf(
    Message(
        title = "Welcome to the action screen",
        subtitle = "Partially Received",
        subtitle2 = "Walmart",
        severity = "Info",
        isCommitted = false,
        actionName = ""
    ),
    Message(
        title = "Processing your request...",
        severity = "Warn",
        subtitle = "Received",
        subtitle2 = "Amazon",
        isCommitted = true,
        actionName = "process"
    ),
    Message(
        title = "Action completed successfully",
        severity = "Success",
        isCommitted = true,
        actionName = "complete"
    ),
    Message(
        title = "Error occurred during processing",
        severity = "Error",
        isCommitted = false,
        actionName = "error"
    ),
    Message(title = "divider", severity = "", isCommitted = false, actionName = "")
)

private val sampleTextPrompt = Prompt(
    promptType = PromptType.TEXT,
    promptPlaceholder = "Enter your response here",
    items = emptyList()
)

fun spaceCamel(s: String?): String {
    return s?.replace(Regex("([a-z])([A-Z])"), "$1 $2") ?: ""
}

private val samplePickerPrompt = Prompt(
    promptType = PromptType.PICKER,
    promptPlaceholder = "Select an option",
    items = listOf(
        PromptItem(label = "Option 1", value = "opt1"),
        PromptItem(label = "Option 2", value = "opt2"),
        PromptItem(label = "Option 3", value = "opt3")
    )
)

private val sampleConfirmPrompt = Prompt(
    promptType = PromptType.CONFIRM,
    promptPlaceholder = "Confirm action",
    items = emptyList()
)

private val sampleSignPrompt = Prompt(
    promptType = PromptType.SIGN,
    promptPlaceholder = "Please provide your signature",
    items = emptyList()
)

// Sample scan prompt for previews
private val sampleScanPrompt = Prompt(
    promptType = PromptType.SCAN,
    promptPlaceholder = "Scan barcode or enter manually",
    items = emptyList()
)

// Photo Prompt Previews
@Preview(name = "Photo Prompt - No Image")
@Composable
fun PhotoPromptScreenNoImagePreview() {
    HandheldP4WTheme {
        PhotoPromptScreen(
            capturedImage = null,
            onImageCaptured = {},
            onSendImage = {},
            onRetakePhoto = {}
        )
    }
}

@Preview(name = "Photo Prompt - With Image")
@Composable
fun PhotoPromptScreenWithImagePreview() {
    HandheldP4WTheme {
        PhotoPromptScreen(
            capturedImage = "data:image/jpeg;base64,sample_image_data",
            onImageCaptured = {},
            onSendImage = {},
            onRetakePhoto = {}
        )
    }
}

// Signature Prompt Preview
@Preview(name = "Signature Prompt")
@Composable
fun SignaturePromptScreenPreview() {
    HandheldP4WTheme {
        SignaturePromptScreen(
            onSignatureSaved = {},
            onCancel = {}
        )
    }
}

// Multiple Message Types Preview
@Preview(name = "Multiple Messages", showBackground = true)
@Composable
fun MultipleMessagesPreview() {
    HandheldP4WTheme {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sampleMessages.size) { index ->
                MessageCard(
                    message = sampleMessages[index],
                    onClick = {}
                )
            }
        }
    }
}

// All Prompt Types Preview
@Preview(name = "All Prompt Types", showBackground = true)
@Composable
fun AllPromptTypesPreview() {
    HandheldP4WTheme {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text("Scan:", fontWeight = FontWeight.Bold)
                PromptInputArea(
                    prompt = sampleScanPrompt,
                    promptValue = "123456789",
                    onPromptValueChange = {},
                    onSendPrompt = {},
                    onShowSignature = {}
                )
            }

            item {
                Text("Text Prompt:", fontWeight = FontWeight.Bold)
                PromptInputArea(
                    prompt = sampleTextPrompt,
                    promptValue = "Sample text",
                    onPromptValueChange = {},
                    onSendPrompt = {},
                    onShowSignature = {}
                )
            }

            item {
                Text("Picker Prompt:", fontWeight = FontWeight.Bold)
                PromptInputArea(
                    prompt = samplePickerPrompt,
                    promptValue = "",
                    onPromptValueChange = {},
                    onSendPrompt = {},
                    onShowSignature = {}
                )
            }

            item {
                Text("Confirm Prompt:", fontWeight = FontWeight.Bold)
                PromptInputArea(
                    prompt = sampleConfirmPrompt,
                    promptValue = "",
                    onPromptValueChange = {},
                    onSendPrompt = {},
                    onShowSignature = {}
                )
            }

            item {
                Text("Sign Prompt:", fontWeight = FontWeight.Bold)
                PromptInputArea(
                    prompt = sampleSignPrompt,
                    promptValue = "",
                    onPromptValueChange = {},
                    onSendPrompt = {},
                    onShowSignature = {}
                )
            }
        }
    }
}

// Multiple Message Types Preview
@Preview(name = "Multiple Messages", showBackground = true)
@Composable
fun MultipleMessagesPreview2() {

    val uiState = ActionUiState();
    HandheldP4WTheme {
        ActionScreenWrapper(
            menuItemLabel = "Adjust In",
            menuItemState = "main.AdjustIn",
            prompt = {
                PromptInputArea(
                    prompt = sampleTextPrompt,
                    promptValue = "",
                    onPromptValueChange = {},
                    onSendPrompt = {},
                    onShowSignature = {}
                )
            },
            uiState
        )
    }
}