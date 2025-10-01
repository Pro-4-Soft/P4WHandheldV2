package com.p4handheld.ui.screens.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.p4handheld.ui.compose.theme.HandheldP4WTheme
import com.p4handheld.ui.screens.TenantSelectScreenContent
import com.p4handheld.ui.screens.viewmodels.TenantUiState


@Preview(showBackground = true, device = "spec:width=340dp,height=650dp,dpi=320")
@Composable
fun TenantSelectScreenPreview() {
    HandheldP4WTheme {
        TenantSelectScreenContent(
            uiState = TenantUiState(
                isLoading = false,
                isConfigurationSaved = false,
                errorMessage = null
            ),
            tenantName = "Demo Tenant",
            baseUrl = "https://api.example.com",
            onTenantNameChange = {},
            onBaseUrlChange = {},
            onApplyClick = {},
            showAdvanced = false,
            logoUrl = "https://p4dev.p4warehouse.com/data/logo"
        )
    }
}

@Preview(showBackground = true, device = "spec:width=340dp,height=650dp,dpi=320")
@Composable
fun TenantSelectScreenAdvancedPreview() {
    HandheldP4WTheme {
        TenantSelectScreenContent(
            uiState = TenantUiState(
                isLoading = false,
                isConfigurationSaved = false,
                errorMessage = null
            ),
            tenantName = "Demo Tenant",
            baseUrl = "https://api.example.com",
            onTenantNameChange = {},
            onBaseUrlChange = {},
            onApplyClick = {},
            showAdvanced = true,
            logoUrl = "https://p4dev.p4warehouse.com/data/logo"
        )
    }
}
