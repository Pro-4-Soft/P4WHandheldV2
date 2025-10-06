package com.p4handheld.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.p4handheld.data.ScanStateHolder
import com.p4handheld.data.models.Message
import com.p4handheld.data.models.Prompt
import com.p4handheld.data.models.PromptType
import com.p4handheld.data.models.ToolbarAction
import com.p4handheld.ui.components.TopBarWithIcons
import com.p4handheld.ui.screens.previews.spaceCamel
import com.p4handheld.ui.screens.viewmodels.ActionUiState
import com.p4handheld.ui.screens.viewmodels.ActionViewModel
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionScreen(
    menuItemLabel: String,
    initialPageKey: String,
    onNavigateBack: () -> Unit,
    hasUnreadMessages: Boolean = false,
    isTrackingLocation: Boolean = false,
    onMessageClick: () -> Unit = {},
    onNavigateToLogin: () -> Unit,
    onTasksClick: () -> Unit = {}
) {
    val viewModel: ActionViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val scanViewState by ScanStateHolder.scanViewStatus.observeAsState()

    BackHandler(enabled = !uiState.isLoading) {
        onNavigateBack()
    }

    //if 401 - navigate to login screen
    LaunchedEffect(viewModel.unauthorizedEvent) {
        viewModel.unauthorizedEvent.collect {
            onNavigateToLogin()
        }
    }

    // Initialize action on first composition with debug logging
    LaunchedEffect(initialPageKey) {
        if (initialPageKey.isNotEmpty()) {
            println("ActionScreen: Initializing action with state: $initialPageKey")
            viewModel.updatePageKey(initialPageKey, menuItemLabel)
            viewModel.processAction()
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

    // Handle scan data from DataWedge for SCAN, TEXT, NUMBER, and DATE prompt types
    LaunchedEffect(scanViewState?.dwOutputData) {
        scanViewState?.dwOutputData?.let { outputData ->
            val data = outputData.data
            if (data.isNotEmpty()) {
                when (uiState.currentPrompt.promptType) {
                    PromptType.SCAN -> {
                        println("ActionScreen: Scan data received (SCAN): $data")
                        viewModel.updatePromptValue(data)
                        viewModel.processAction(data)
                    }

                    PromptType.TEXT -> {
                        println("ActionScreen: Scan data received (TEXT): $data")
                        viewModel.updatePromptValue(data)
                    }

                    PromptType.DATE -> {
                        val trimmed = data.trim()
                        println("ActionScreen: Scan data received (DATE): $trimmed")
                        viewModel.updatePromptValue(trimmed)
                    }

                    PromptType.NUMBER -> {
                        val digitsOnly = data.filter { it.isDigit() }
                        println("ActionScreen: Scan data received (NUMBER): $digitsOnly")
                        viewModel.updatePromptValue(digitsOnly)
                    }

                    else -> {
                        // Ignore for other prompt types
                    }
                }
            }
        }
    }

    val prompt: @Composable () -> Unit = when (uiState.currentPrompt.promptType) {
        PromptType.PHOTO -> {
            {
                PhotoPromptScreen(
                    onImageCaptured = { imageBase64 ->
                        viewModel.setCapturedImage(imageBase64)
                    },
                    onSendImage = { imageBase64 ->
                        viewModel.processAction(imageBase64)
                    }
                )
            }
        }

        PromptType.SIGN -> {
            if (uiState.currentPrompt.promptType == PromptType.SIGN) {
                {
                    SignaturePromptScreen(
                        onSignatureSaved = { signatureBase64 ->
                            viewModel.processAction(signatureBase64)
                        }
                    )
                }
            } else {
                {
                    DefaultActionScreen(
                        uiState = uiState,
                        listState = listState,
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
                    viewModel = viewModel
                )
            }
        }
    }

    ActionScreenWrapper(
        menuItemLabel = menuItemLabel,
        promptInputComponent = prompt,
        uiState = uiState,
        hasUnreadMessages = hasUnreadMessages,
        isTrackingLocation = isTrackingLocation,
        onMessageClick = onMessageClick,
        onTasksClick = onTasksClick
    )
}

@Composable
fun ActionScreenWrapper(
    menuItemLabel: String,
    promptInputComponent: @Composable () -> Unit,
    uiState: ActionUiState,
    hasUnreadMessages: Boolean = false,
    isTrackingLocation: Boolean = false,
    onMessageClick: () -> Unit = {},
    onTasksClick: () -> Unit = {}
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Global TopBar with icons and username
        TopBarWithIcons(
            isTrackingLocation = isTrackingLocation,
            hasUnreadMessages = hasUnreadMessages,
            onMessageClick = onMessageClick,
            onTasksClick = onTasksClick,
            enabled = !uiState.isLoading
        )

        //region Header with page title
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
                    text = uiState.pageTitle ?: menuItemLabel,
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
        //endregion

        promptInputComponent()
    }
}

@Composable
fun DefaultActionScreen(
    uiState: ActionUiState,
    listState: LazyListState,
    viewModel: ActionViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        if (uiState.toolbarActions.isNotEmpty()) {
            ToolBarActions(
                toolbarActions = uiState.toolbarActions,
                isEnabled = !uiState.isLoading,
                onToolBarActionClick = { actionFor ->
                    viewModel.processAction(null, actionFor)
                }
            )
        }

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
        if (uiState.currentPrompt.promptType != PromptType.NOT_SELECTED) {
            PromptInputArea(
                prompt = uiState.currentPrompt,
                promptValue = uiState.promptValue,
                isLoading = uiState.isLoading,
                onPromptValueChange = {
                    println("ActionScreen: Prompt value changed: $it")
                    viewModel.updatePromptValue(it)
                },
                onSendPrompt = { value ->
                    println("ActionScreen: Sending prompt value: $value")
                    viewModel.processAction(value)
                },
                onShowSignature = {
                    println("ActionScreen: Showing signature prompt")
                }
            )
        } else {
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
fun ToolBarActions(
    toolbarActions: List<ToolbarAction>,
    isEnabled: Boolean,
    onToolBarActionClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            contentPadding = PaddingValues(horizontal = 4.dp),
        ) {
            items(toolbarActions) { action ->
                Button(
                    onClick = {
                        println("ActionScreen: Toolbar action clicked: $action")
                        onToolBarActionClick(action.action)
                    },
                    enabled = isEnabled,
                    shape = RoundedCornerShape(5.dp),
                ) {
                    Text(
                        text = action.label,
                        fontSize = 16.sp
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

    // Image-only message (inserted separately by appendLastMessageWithPhoto)
    if (message.imageResource != null && message.title.isBlank()) {
        val bmp = remember(message.imageResource) { decodeBase64Image(message.imageResource) }
        if (bmp != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            return
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = message.isActionable,
                onClick = { onClick() }
            ),
        colors = CardDefaults.cardColors(
            containerColor = when (message.severity) {
                "Info" -> if (message.isCommitted || !message.isActionable) Color(0xFFF1F1F1) else Color.White
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
                "Info" -> Color(0xFFDADADA)
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
            // Image if available (supports base64 data URI)
            message.imageResource?.let { src ->
                val bmp = remember(src) { decodeBase64Image(src) }
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(260.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                } else {
                    Spacer(modifier = Modifier.width(8.dp))
                }
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
                        color = Color(0xFF595959),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (message.subtitle2?.isNotEmpty() == true) {
                    Text(
                        text = spaceCamel(message.subtitle2),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF676767)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptInputArea(
    prompt: Prompt,
    promptValue: String,
    isLoading: Boolean,
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
            PromptType.DATE -> {
                var showPicker by remember { mutableStateOf(true) }
                val dateState = rememberDatePickerState()

                // Helper to format selected millis to yyyy-MM-dd
                fun formatSelectedDate(): String? {
                    val millis = dateState.selectedDateMillis ?: return null
                    return try {
                        val localDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        localDate.toString() // yyyy-MM-dd
                    } catch (e: Exception) {
                        null
                    }
                }

                if (showPicker) {
                    DatePickerDialog(
                        onDismissRequest = { showPicker = false },
                        confirmButton = {
                            Button(onClick = {
                                val formatted = formatSelectedDate()
                                if (formatted != null) {
                                    onPromptValueChange(formatted)
                                    onSendPrompt(formatted)
                                }
                                showPicker = false
                            }, enabled = !isLoading) {
                                Text("OK")
                            }
                        },
                        dismissButton = {
                            Button(onClick = { showPicker = false }, enabled = !isLoading) { Text("Cancel") }
                        }
                    ) {
                        DatePicker(state = dateState)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, top = 0.dp, end = 4.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    OutlinedTextField(
                        value = promptValue,
                        onValueChange = {},
                        label = { Text(prompt.promptPlaceholder.ifBlank { "Select date" }) },
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = LocalIndication.current,
                                onClick = { showPicker = true }
                            ),
                        readOnly = true,
                        enabled = true
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (promptValue.isNotEmpty()) {
                                onSendPrompt(promptValue)
                            } else {
                                // If not yet set but selected in dialog, try to send it
                                val formatted = formatSelectedDate()
                                if (formatted != null) onSendPrompt(formatted)
                            }
                        },
                        enabled = (promptValue.isNotEmpty() || dateState.selectedDateMillis != null) && !isLoading,
                        modifier = Modifier.height(56.dp),
                        shape = RoundedCornerShape(5.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text(text = "Send", fontSize = 18.sp)
                    }
                }
            }

            PromptType.NUMBER -> {
                val numberFocusRequester = remember { FocusRequester() }
                LaunchedEffect(Unit) {
                    numberFocusRequester.requestFocus()
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, top = 0.dp, end = 4.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    OutlinedTextField(
                        value = promptValue,
                        onValueChange = { newVal ->
                            val digitsOnly = newVal.filter { it.isDigit() }
                            onPromptValueChange(digitsOnly)
                        },
                        label = { Text(prompt.promptPlaceholder.ifBlank { "Enter number" }) },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(numberFocusRequester),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
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
                        enabled = promptValue.isNotEmpty() && !isLoading,
                        modifier = Modifier.height(56.dp),
                        shape = RoundedCornerShape(5.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text(text = "Send", fontSize = 18.sp)
                    }
                }
            }

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
                        Text(
                            text = prompt.promptPlaceholder,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
            }

            PromptType.CONFIRM -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(
                        text = prompt.promptPlaceholder,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    Button(
                        onClick = { onSendPrompt("true") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(5.dp)
                    ) {
                        Text(
                            text = "Yes",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Button(
                        onClick = { onSendPrompt("false") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF44336)
                        ),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(5.dp)
                    ) {
                        Text(
                            text = "No",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            PromptType.SIGN -> {
                Button(
                    onClick = onShowSignature,
                    modifier = Modifier
                        .fillMaxWidth(),
                    enabled = !isLoading,
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
                var showPicker by remember { mutableStateOf(true) }

                if (showPicker) {
                    AlertDialog(
                        onDismissRequest = { showPicker = false },
                        title = { Text(text = prompt.promptPlaceholder.ifBlank { "Select an option" }) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                prompt.items.forEach { item ->
                                    Button(
                                        onClick = {
                                            onSendPrompt(item.value)
                                            showPicker = false
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = !isLoading,
                                        shape = RoundedCornerShape(5.dp)
                                    ) {
                                        Text(item.label)
                                    }
                                }
                            }
                        },
                        confirmButton = { }
                    )
                }

                // Re-open control similar to DATE read-only field
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, top = 0.dp, end = 4.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        label = { Text(prompt.promptPlaceholder.ifBlank { "Select option" }) },
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = LocalIndication.current,
                                onClick = { showPicker = true }
                            ),
                        readOnly = true,
                        enabled = true
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { showPicker = true },
                        modifier = Modifier.height(56.dp),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(5.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(text = "Choose")
                    }
                }
            }

            PromptType.GO_TO_NEW_PAGE -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(text = prompt.promptPlaceholder, fontSize = 18.sp, modifier = Modifier.padding(16.dp))
                }
            }

            PromptType.TEXT -> {
                // Text input
                val textFocusRequester = remember { FocusRequester() }
                LaunchedEffect(Unit) {
                    textFocusRequester.requestFocus()
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, top = 0.dp, end = 4.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    OutlinedTextField(
                        value = promptValue,
                        onValueChange = onPromptValueChange,
                        label = { Text(prompt.promptPlaceholder) },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(textFocusRequester),
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
                        Text(text = "Send", fontSize = 18.sp)
                    }
                }
            }

            else -> {
                /* leave empty space */
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoPromptScreen(
    onImageCaptured: (String) -> Unit,
    onSendImage: (String) -> Unit,
) {
    val context = LocalContext.current

    // Convert Bitmap to Base64 JPEG
    fun Bitmap.toBase64Jpeg(quality: Int = 85): String {
        val output = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.JPEG, quality, output)
        val bytes = output.toByteArray()
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return "data:image/jpeg;base64,$base64"
    }

    var captureAttempted by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val previewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            val base64 = it.toBase64Jpeg()
            // Update local state and immediately send
            onImageCaptured(base64)
            onSendImage(base64)
            errorMsg = null
        }
        if (bitmap == null) {
            // User cancelled or camera failed
            captureAttempted = true
            errorMsg = "No photo captured"
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            previewLauncher.launch(null)
            errorMsg = null
        }
        if (!granted) {
            captureAttempted = true
            errorMsg = "Camera permission denied"
        }
    }

    fun launchCameraWithPermission() {
        val permissionStatus = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
            previewLauncher.launch(null)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Auto-launch camera when the Photo prompt appears
    LaunchedEffect(Unit) {
        launchCameraWithPermission()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Minimal placeholder; camera auto-launches and submission happens immediately
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Optional placeholder icon while waiting for camera result
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(64.dp)
            )
            errorMsg?.let { msg ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = msg, color = Color.Gray)
            }
            if (captureAttempted) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { launchCameraWithPermission() }, shape = RoundedCornerShape(5.dp)) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
fun SignaturePromptScreen(
    onSignatureSaved: (String) -> Unit
) {

    // Store strokes as list of points to reproduce when exporting
    var strokes by remember { mutableStateOf(listOf<List<Offset>>()) }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    val strokeWidthPx = 5f
    val strokeColor = Color.Black

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
        var canvasSize by remember { mutableStateOf(IntSize(0, 0)) }
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(8.dp))
                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentStroke = listOf(offset)
                        },
                        onDrag = { change, _ ->
                            currentStroke = currentStroke + change.position
                        },
                        onDragEnd = {
                            if (currentStroke.isNotEmpty()) {
                                strokes = strokes + listOf(currentStroke)
                                currentStroke = emptyList()
                            }
                        }
                    )
                }
        ) {
            canvasSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt())

            // Draw completed strokes
            strokes.forEach { stroke ->
                if (stroke.size > 1) {
                    for (i in 0 until stroke.size - 1) {
                        drawLine(
                            color = strokeColor,
                            start = stroke[i],
                            end = stroke[i + 1],
                            strokeWidth = strokeWidthPx
                        )
                    }
                }
            }
            // Draw current stroke while dragging
            if (currentStroke.size > 1) {
                for (i in 0 until currentStroke.size - 1) {
                    drawLine(
                        color = strokeColor,
                        start = currentStroke[i],
                        end = currentStroke[i + 1],
                        strokeWidth = strokeWidthPx
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { strokes = emptyList(); currentStroke = emptyList() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9800)
                ),
                shape = RoundedCornerShape(5.dp)
            ) {
                Text("Clean")
            }

            Button(
                onClick = {
                    // Render strokes to a bitmap and return as Base64 JPEG
                    val width = if (canvasSize.width > 0) canvasSize.width else 800
                    val height = if (canvasSize.height > 0) canvasSize.height else 300
                    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val c = android.graphics.Canvas(bmp)
                    c.drawColor(android.graphics.Color.WHITE)
                    val p = Paint().apply {
                        color = Color.Black.toArgb()
                        strokeWidth = strokeWidthPx
                        style = Paint.Style.STROKE
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                        isAntiAlias = true
                    }
                    // Draw all strokes
                    strokes.forEach { stroke ->
                        for (i in 0 until (stroke.size - 1).coerceAtLeast(0)) {
                            val s = stroke[i]
                            val e = stroke[i + 1]
                            c.drawLine(s.x, s.y, e.x, e.y, p)
                        }
                    }
                    // Include currentStroke if user taps without moving
                    if (currentStroke.size == 1) {
                        val pt = currentStroke.first()
                        c.drawPoint(pt.x, pt.y, p)
                    }
                    // Convert to Base64 JPEG
                    val output = ByteArrayOutputStream()
                    bmp.compress(Bitmap.CompressFormat.JPEG, 85, output)
                    val bytes = output.toByteArray()
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    val dataUri = "data:image/jpeg;base64,$base64"
                    onSignatureSaved(dataUri)
                },
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

fun decodeBase64Image(dataUri: String): Bitmap? {
    return try {
        val base64Part = dataUri.substringAfter(",", missingDelimiterValue = dataUri)
        val bytes = Base64.decode(base64Part, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
        null
    }
}