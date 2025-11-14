package com.p4handheld.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.p4handheld.R
import com.p4handheld.ui.compose.theme.HandheldP4WTheme
import com.p4handheld.ui.screens.viewmodels.LoginUiState
import com.p4handheld.ui.screens.viewmodels.LoginViewModel
import com.p4handheld.utils.Translations

@Composable
fun LoginScreen(
    onNavigateToMenu: () -> Unit,
    onNavigateToTenantConfig: () -> Unit
) {
    val viewModel: LoginViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    val passwordFocusRequester = remember { FocusRequester() }

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val context = LocalContext.current
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val versionName = packageInfo.versionName.toString()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onNavigateToMenu()
        }
    }

    LoginScreenContent(
        uiState = uiState,
        username = username,
        password = password,
        passwordFocusRequester = passwordFocusRequester,
        onUsernameChange = {
            username = it
            viewModel.clearError()
        },
        onPasswordChange = {
            password = it
            viewModel.clearError()
        },
        onLoginClick = {
            viewModel.login(username, password)
        },
        onTenantConfigClick = onNavigateToTenantConfig,
        logoUrl = viewModel.getLogoUrl(),
        versionName = versionName
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreenContent(
    uiState: LoginUiState,
    username: String,
    password: String,
    passwordFocusRequester: FocusRequester,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onTenantConfigClick: () -> Unit,
    logoUrl: String = "",
    versionName: String = ""
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp, 8.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.background
            ),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Left button
                IconButton(
                    onClick = { onTenantConfigClick() },
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = Translations[R.string.tenant_button],
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Centered image
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(logoUrl)
                        .memoryCachePolicy(CachePolicy.DISABLED)
                        .diskCachePolicy(CachePolicy.DISABLED)
                        .build(),
                    contentDescription = Translations[R.string.app_logo_description],
                    modifier = Modifier
                        .size(140.dp)
                        .align(Alignment.Center)
                )

                // Right text
                Text(
                    text = "v$versionName",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceBright
                ),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = Translations[R.string.login_title],
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            onUsernameChange(it)
                        },
                        label = { Text(Translations[R.string.username_label]) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !uiState.isLoading,
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { passwordFocusRequester.requestFocus() }
                        )
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { onPasswordChange(it) },
                        label = { Text(Translations[R.string.password_label]) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(passwordFocusRequester),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        enabled = !uiState.isLoading,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Send // uncommented and included
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (password.isNotEmpty() && username.isNotEmpty()) {
                                    onLoginClick()
                                }
                            }
                        )
                    )

                    // Error message
                    uiState.errorMessage?.let { errorMessage ->
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 10.sp,
                        )
                    }

                    Button(
                        onClick = onLoginClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(Translations[R.string.login_button], fontSize = 16.sp)
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    buildAnnotatedString {
                        append(Translations[R.string.powered_by] + " ")
                    }, fontSize = 10.sp
                )
                Text(
                    buildAnnotatedString {
                        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                        append(Translations.format(R.string.copyright_text, currentYear))
                    },
                    fontSize = 10.sp,
                    color = Color(0xFF3553D0)
                )
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomEnd
            ) {


            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=340dp,height=650dp,dpi=320")
@Composable
fun LoginScreenPreview() {
    HandheldP4WTheme {
        LoginScreenContent(
            uiState = LoginUiState(
                isLoading = false,
                isSuccess = false,
                errorMessage = "Invalid credentials: Login failed. Username or password is incorrect. bla bla bla"
            ),
            username = "demo",
            password = "1234",
            passwordFocusRequester = remember { FocusRequester() },
            onUsernameChange = {},
            onPasswordChange = {},
            onLoginClick = {},
            onTenantConfigClick = {},
            versionName = "1.0",
        )
    }
}
