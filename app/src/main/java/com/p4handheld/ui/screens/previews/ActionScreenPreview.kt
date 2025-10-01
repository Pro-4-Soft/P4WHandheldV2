package com.p4handheld.ui.screens.previews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.p4handheld.data.models.Message
import com.p4handheld.data.models.Prompt
import com.p4handheld.data.models.PromptItem
import com.p4handheld.data.models.PromptType
import com.p4handheld.data.models.ToolbarAction
import com.p4handheld.ui.compose.theme.HandheldP4WTheme
import com.p4handheld.ui.screens.ActionScreenWrapper
import com.p4handheld.ui.screens.MessageCard
import com.p4handheld.ui.screens.PhotoPromptScreen
import com.p4handheld.ui.screens.PromptInputArea
import com.p4handheld.ui.screens.SignaturePromptScreen
import com.p4handheld.ui.screens.viewmodels.ActionUiState


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
    promptPlaceholder = "Enter your response here"
)

private val sampleDatePrompt = Prompt(
    promptType = PromptType.DATE,
    promptPlaceholder = "Enter your response here"
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
    promptPlaceholder = "Scan barcode",
    items = emptyList()
)

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

    val toolbar = listOf(
        ToolbarAction(label = "Option 1", action = "opt1"),
    )

    val uiState = ActionUiState(
        messageStack = sampleMessages,
        currentPrompt = sampleTextPrompt,
        toolbarActions = toolbar
    );
    HandheldP4WTheme {
        ActionScreenWrapper(
            menuItemLabel = "Adjust In",
            promptInputComponent = {
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