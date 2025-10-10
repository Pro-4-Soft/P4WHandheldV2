# Translation System Implementation

This document describes the comprehensive translation system implemented for the P4W Handheld application.

## Overview

The translation system provides:
- **API-based translations** with language code routing
- **Local caching** for offline support
- **Fallback to R.string resources** when translations are unavailable
- **Easy-to-use helper functions** for developers
- **Automatic initialization** on app startup

## Architecture

### 1. Data Models (`TranslationModels.kt`)

```kotlin
// Request model for translation API
data class TranslationRequest(
    val keys: List<String>
)

// Response model from translation API
data class TranslationResponse(
    val translations: Map<String, String>
)

// Cached translation data
data class CachedTranslations(
    val languageCode: String,
    val translations: Map<String, String>,
    val lastUpdated: Long = System.currentTimeMillis()
)
```

### 2. API Service (`ApiService.kt`)

```kotlin
suspend fun getTranslations(
    languageCode: String, 
    translationRequest: TranslationRequest
): ApiResponse<TranslationResponse>
```

**API Endpoint:** `POST /api/TranslationApi/GetTranslations/{languageCode}`
- **Route Parameter:** `languageCode` (e.g., "en", "es", "fr")
- **Request Body:** Array of string keys to translate
- **Response:** Map of key-value pairs with translations

### 3. Translation Manager (`TranslationManager.kt`)

The core component that handles:
- **API calls** to fetch translations
- **Local caching** with 24-hour expiry
- **Fallback logic** to R.string resources
- **Language detection** from system locale

#### Key Methods:

```kotlin
// Get single translated string
fun getString(key: String, fallback: String = key): String

// Get multiple translated strings
fun getStrings(keys: List<String>): Map<String, String>

// Load translations for language
suspend fun loadTranslations(
    languageCode: String = getCurrentLanguageCode(),
    keys: List<String>,
    forceRefresh: Boolean = false
): Result<Boolean>

// Clear cached translations
fun clearCache()
```

### 4. Helper Functions (`StringExtensions.kt`)

#### Translation Helper Functions:

```kotlin
// Get translated string with fallback
fun getTranslatedString(
    context: Context,
    key: String,
    stringResId: Int,
    vararg formatArgs: Any
): String

// Composable function for UI
@Composable
fun translatedStringResource(
    key: String,
    stringResId: Int,
    vararg formatArgs: Any
): String
```

#### Pre-built Helper Functions:

```kotlin
object TranslationHelper {
    @Composable fun loginTitle()
    @Composable fun usernameLabel()
    @Composable fun passwordLabel()
    fun loginErrorEmptyFields(context: Context)
    fun loginErrorNetwork(context: Context, error: String)
    // ... and many more
}
```

## Usage Examples

### 1. In Composable Functions

```kotlin
@Composable
fun LoginScreen() {
    Text(text = TranslationHelper.loginTitle())
    
    OutlinedTextField(
        label = { Text(TranslationHelper.usernameLabel()) }
    )
    
    // Or using the generic helper
    Text(text = translatedStringResource(
        key = "login_title",
        stringResId = R.string.login_title
    ))
}
```

### 2. In ViewModels and Activities

```kotlin
class LoginViewModel(application: Application) : AndroidViewModel(application) {
    fun validateInput() {
        val errorMessage = TranslationHelper.loginErrorEmptyFields(getApplication())
        // Use errorMessage...
    }
}
```

### 3. Manual Translation Loading

```kotlin
class MainActivity : ComponentActivity() {
    private fun initializeTranslations() {
        lifecycleScope.launch {
            val translationManager = TranslationManager.getInstance(this@MainActivity)
            val keys = translationManager.getAllTranslationKeys()
            translationManager.loadTranslations(keys = keys)
        }
    }
}
```

## String Resources (`strings.xml`)

All translatable strings are defined in `res/values/strings.xml`:

```xml
<resources>
    <!-- Login Screen -->
    <string name="login_title">Login</string>
    <string name="username_label">Username</string>
    <string name="password_label">Password</string>
    <string name="login_button">Login</string>
    <string name="login_error_empty_fields">Please enter both username and password</string>
    <string name="login_error_network">Unexpected error: %1$s</string>
    
    <!-- Menu Screen -->
    <string name="menu_title">Menu</string>
    <string name="logout_button">Logout</string>
    
    <!-- Common -->
    <string name="loading">Loading...</string>
    <string name="error">Error</string>
    <string name="success">Success</string>
    
    <!-- And many more... -->
</resources>
```

## Translation Keys

All translation keys are centralized in `TranslationKeys` object:

```kotlin
object TranslationKeys {
    const val LOGIN_TITLE = "login_title"
    const val USERNAME_LABEL = "username_label"
    const val PASSWORD_LABEL = "password_label"
    // ... etc
}
```

## API Implementation

### Server-Side Requirements

The server should implement the following endpoint:

```
POST /api/TranslationApi/GetTranslations/{languageCode}
```

**Request Body:**
```json
{
    "keys": [
        "login_title",
        "username_label",
        "password_label"
    ]
}
```

**Response:**
```json
{
    "translations": {
        "login_title": "Iniciar Sesión",
        "username_label": "Usuario",
        "password_label": "Contraseña"
    }
}
```

### Supported Language Codes

- `en` - English (default)
- `es` - Spanish
- `fr` - French
- `de` - German
- `pt` - Portuguese
- `it` - Italian
- Add more as needed...

## Caching Strategy

### Local Cache
- **Storage:** SharedPreferences with JSON serialization
- **Expiry:** 24 hours
- **Key:** `cached_translations`
- **Language Tracking:** `last_language`

### Cache Validation
```kotlin
private fun isCacheValid(languageCode: String): Boolean {
    val cached = cachedTranslations ?: return false
    
    // Check language match
    if (cached.languageCode != languageCode) return false
    
    // Check expiry (24 hours)
    val cacheAge = System.currentTimeMillis() - cached.lastUpdated
    val cacheExpiryMs = 24 * 60 * 60 * 1000L
    
    return cacheAge < cacheExpiryMs
}
```

## Initialization Flow

### App Startup
1. **MainActivity.onCreate()** calls `initializeTranslations()`
2. **TranslationManager** detects system language
3. **API call** fetches translations for detected language
4. **Translations cached** locally for offline use
5. **UI components** can now use translated strings

### Language Change
1. **System language change** detected
2. **Cache invalidated** if language differs
3. **New translations** fetched from API
4. **UI updates** automatically with new language

## Error Handling

### Fallback Strategy
1. **Try cached translations** first
2. **If cache miss**, try API call
3. **If API fails**, use R.string resources
4. **If R.string missing**, use key as fallback

### Logging
```kotlin
Log.d("TranslationManager", "Using cached translations for $languageCode")
Log.e("TranslationManager", "Failed to load translations: $errorMessage")
Log.d("TranslationManager", "Successfully loaded ${count} translations")
```

## Best Practices

### 1. Always Provide Fallbacks
```kotlin
// Good - has fallback
Text(text = translatedStringResource(
    key = "login_title",
    stringResId = R.string.login_title
))

// Bad - no fallback
Text(text = translationManager.getString("login_title"))
```

### 2. Use Helper Functions
```kotlin
// Good - type-safe and convenient
Text(text = TranslationHelper.loginTitle())

// Okay - generic but safe
Text(text = translatedStringResource("login_title", R.string.login_title))

// Avoid - manual and error-prone
Text(text = translationManager.getString("login_title", "Login"))
```

### 3. Handle Format Arguments
```kotlin
// Good - supports formatting
val message = TranslationHelper.loginErrorNetwork(context, errorDetails)

// Good - with format args
val copyright = translatedStringResource(
    key = "copyright_text",
    stringResId = R.string.copyright_text,
    currentYear
)
```

### 4. Batch Load Translations
```kotlin
// Good - load all at once
val keys = translationManager.getAllTranslationKeys()
translationManager.loadTranslations(keys = keys)

// Avoid - loading one by one
keys.forEach { key ->
    translationManager.loadTranslations(keys = listOf(key))
}
```

## Testing

### Unit Tests
```kotlin
@Test
fun testTranslationFallback() {
    val manager = TranslationManager.getInstance(context)
    val result = manager.getString("nonexistent_key", "fallback")
    assertEquals("fallback", result)
}

@Test
fun testCacheExpiry() {
    // Test cache expiration logic
}
```

### Integration Tests
```kotlin
@Test
fun testApiIntegration() {
    // Test actual API calls
}
```

## Migration Guide

### From Hardcoded Strings
```kotlin
// Before
Text("Login")

// After
Text(TranslationHelper.loginTitle())
```

### From Direct R.string Usage
```kotlin
// Before
Text(stringResource(R.string.login_title))

// After
Text(TranslationHelper.loginTitle())
```

## Performance Considerations

### Memory Usage
- **Cached translations** stored in memory
- **JSON serialization** for persistence
- **Automatic cleanup** on language change

### Network Usage
- **Batch API calls** reduce requests
- **24-hour caching** minimizes network usage
- **Compression** recommended for large translation sets

### UI Performance
- **Synchronous access** to cached translations
- **No UI blocking** during API calls
- **Immediate fallback** to R.string resources

## Troubleshooting

### Common Issues

1. **Translations not loading**
   - Check API endpoint configuration
   - Verify network connectivity
   - Check server response format

2. **Cache not working**
   - Clear app data to reset cache
   - Check SharedPreferences permissions
   - Verify JSON serialization

3. **Fallbacks not working**
   - Ensure R.string resources exist
   - Check string resource IDs
   - Verify helper function implementation

### Debug Logging
Enable debug logging to trace translation loading:
```kotlin
Log.d("TranslationManager", "Cache status: ${isCacheValid(languageCode)}")
Log.d("TranslationManager", "API response: ${response.isSuccessful}")
Log.d("TranslationManager", "Fallback used for key: $key")
```

## Future Enhancements

### Planned Features
- **Real-time language switching** without app restart
- **Pluralization support** for count-based strings
- **RTL language support** for Arabic/Hebrew
- **Translation validation** and quality checks
- **A/B testing** for different translations
- **Analytics** for translation usage

### Server-Side Enhancements
- **Translation management UI** for non-developers
- **Version control** for translations
- **Approval workflow** for translation changes
- **Usage analytics** and optimization
- **Machine translation** integration for missing keys

This translation system provides a robust, scalable solution for internationalization while maintaining excellent performance and user experience.
