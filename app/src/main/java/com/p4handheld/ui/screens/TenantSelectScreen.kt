package com.p4handheld.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.p4handheld.ui.screens.viewmodels.TenantUiState
import com.p4handheld.ui.screens.viewmodels.TenantViewModel

@Composable
fun TenantSelectScreen(
    onNavigateToLogin: () -> Unit
) {
    val viewModel: TenantViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    var tenantName by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf("") }

    // Load existing tenant configuration on screen initialization
    LaunchedEffect(Unit) {
        val existingConfig = viewModel.getTenantConfig()
        if (existingConfig != null) {
            tenantName = existingConfig.tenantName
            baseUrl = existingConfig.baseTenantUrl
        }
    }

    // Navigate to Login when configuration is saved
    LaunchedEffect(uiState.isConfigurationSaved) {
        if (uiState.isConfigurationSaved) {
            onNavigateToLogin()
        }
    }

    TenantSelectScreenContent(
        uiState = uiState,
        tenantName = tenantName,
        baseUrl = baseUrl,
        onTenantNameChange = { tenantName = it },
        onBaseUrlChange = { baseUrl = it },
        onApplyClick = {
            viewModel.saveTenantConfiguration(tenantName, baseUrl)
        },
        logoUrl = viewModel.getLogoUrl()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantSelectScreenContent(
    uiState: TenantUiState,
    tenantName: String,
    baseUrl: String,
    onTenantNameChange: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onApplyClick: () -> Unit,
    showAdvanced: Boolean = false,
    logoUrl: String = ""
) {

    val showBaseUrl = remember { mutableStateOf(showAdvanced) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
            .padding(16.dp, 8.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        //region Header with Company Logo
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF1F5F9)
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F5F9)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = "App Logo",
                    modifier = Modifier.size(140.dp)
                )
            }
        }
        //endregion

        //region Input Form
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Tenant Configuration",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                //region Tenant Name Input
                OutlinedTextField(
                    value = tenantName,
                    onValueChange = onTenantNameChange,
                    label = { Text("Tenant Name") },
                    placeholder = { Text("Enter tenant name") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    enabled = !uiState.isLoading
                )
                //endregion

                //region Base URL Input
                if (showBaseUrl.value) {
                    OutlinedTextField(
                        value = baseUrl ?: "https://app.pro4soft.com",
                        onValueChange = onBaseUrlChange,
                        label = { Text("Base URL") },
                        placeholder = { Text("https://app.pro4soft.com") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (tenantName.isNotBlank() && baseUrl.isNotBlank()) {
                                    onApplyClick()
                                }
                            }
                        ),
                        trailingIcon = {
                            if (baseUrl.isNotBlank()) {
                                IconButton(onClick = { onBaseUrlChange("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear"
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        enabled = !uiState.isLoading
                    )
                }
                //endregion

                //region Error message
                uiState.errorMessage?.let { errorMessage ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 14.sp
                        )
                    }
                }
                //endregion

                //region Apply Button Button
                Button(
                    onClick = onApplyClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = tenantName.isNotBlank() && baseUrl.isNotBlank() && !uiState.isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Apply configuration",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                //endregion
            }
        }
        //endregion

        //region Advanced Settings Button
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomStart
        ) {
            Button(
                onClick = { showBaseUrl.value = !showBaseUrl.value },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Advanced Settings",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Advanced",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        //endregion
    }
}
