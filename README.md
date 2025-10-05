# NetworkingKit

<div align="center">

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-Apache%202.0-orange.svg)](LICENSE)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)

*Enterprise-grade Android networking - Multi-gateway architecture, universal caching, zero-config auth*

</div>

**NetworkingKit** is an enterprise-grade Android networking library designed for production applications. Built on Retrofit, OkHttp3, and Kotlinx Serialization, it provides **multi-gateway architecture**, **universal API caching**, **zero-config authentication**, and **comprehensive error handling** with minimal setup.

**Key Features:**
- 🏢 **Multi-Gateway support** - Separate Main, Secure (card transactions), and Auth gateways
🔄 **Flexible serialization** - Switch between Kotlinx Serialization and Moshi
- 💾 **No DB needed** - Cache any API response directly
- 🛡️ **Security by default** - Certificate transparency without manual SSL pinning
- 🔐 **Zero auth complexity** - Automatic token management built-in (access and refresh tokens)
- 🌐 **Smart connectivity** - Built-in network detection and connectivity checks
- ⚠️ **Advanced error handling** - Custom exceptions with detailed error info and automatic logging
- 🔍 **Built-in debugging** - Flipper, Chucker, and HTTP logging support
- 🧪 **Easy testing** - Mock any API with JSON files

## 🚀 Quick Start

### Installation

#### Step 1: Add JitPack repository

Add JitPack to your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
        }
    }
}
```

Or if using `settings.gradle`:

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

#### Step 2: Add NetworkingKit dependency

Add NetworkingKit to your module's `build.gradle.kts`:

##### Gradle (Kotlin DSL)
```kotlin
dependencies {
   debugImplementation("com.github.indestudio.networking-kit:debug:1.0.0")
   releaseImplementation("com.github.indestudio.networking-kit:release:1.0.0")
}
```

##### Gradle (Groovy)
```groovy
dependencies {
    releaseImplementation 'com.github.indestudio.networking-kit:debug:1.0.0'
    debugImplementation 'com.github.indestudio.networking-kit:release:1.0.0'
}
```

> **Note**: The debug variant includes Flipper, Chucker, and additional logging tools for development. Use the release variant for production builds.

### Easy Setup

#### Retrofit interface 

```kotlin
interface UserApi {
    @GET("users/{id}")
    suspend fun getUser(@Path("id") userId: String): User

    @POST("users")
    suspend fun createUser(@Body user: CreateUserRequest): User
}
```

#### Using Hilt

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkingModule {

    @Provides
    @Singleton
    fun provideNetworkingKit(
        @ApplicationContext context: Context,
    ): NetworkingKit {
        return NetworkingKit.builder(context)
            .gatewayUrls(createGatewayUrls())
            .build()
    }

    private fun createGatewayUrls(): GatewayBaseUrls {
        return object : GatewayBaseUrls {
           override fun getMainGatewayUrl(): String = "https://app.ticketmaster.com/"
        }
    }

   @Provides
   @Singleton
   fun provideUserApi(networkingKit: NetworkingKit): UserApi {
      return networkingKit.createMainService(UserApi::class.java)
   }
}
```



## ⚙️ Advanced Configuration

### Change Serialization or Json Mapping Strategy

NetworkingKit supports both Moshi and Kotlinx Serialization. By default, it uses Kotlinx Serialization.

```kotlin
// Use Kotlinx Serialization (default)
NetworkingKit.builder(context)
    .gatewayUrls(urls)
    .serializationStrategy(SerializationStrategy.KOTLINX_SERIALIZATION)
    .build()

// Use Moshi
NetworkingKit.builder(context)
    .gatewayUrls(urls)
    .serializationStrategy(SerializationStrategy.MOSHI)
    .build()

// Custom configuration
val customJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
}

NetworkingKit.builder(context)
    .gatewayUrls(urls)
    .kotlinxSerializationProvider(customJson)
    .build()
```

### Multi-Gateway Configuration

NetworkingKit supports **three separate gateways** for different security requirements. You only need to configure the gateways you actually use:

```kotlin
// Configure all three gateways
private fun createGatewayUrls(): GatewayBaseUrls {
    return object : GatewayBaseUrls {
        // Required - Main gateway for standard API operations
        override fun getMainGatewayUrl(): String = "https://api.example.com/"

        // Optional - Secure gateway for sensitive operations (card transactions, payments)
        override fun getSecureGatewayUrl(): String = "https://secure.example.com/"

        // Optional - Auth gateway for authentication endpoints
        override fun getAuthGatewayUrl(): String = "https://auth.example.com/"
    }
}

// Or configure only the main gateway (secure and auth are optional)
private fun createGatewayUrls(): GatewayBaseUrls {
    return object : GatewayBaseUrls {
        override fun getMainGatewayUrl(): String = "https://api.example.com/"
        // getSecureGatewayUrl and getAuthGatewayUrl default to empty strings
    }
}
```

### API Caching

Cache any API response without a database using the `@Cache` annotation. Works with **GET, POST, PUT, PATCH** APIs.

```kotlin
import com.indiedev.networking.annotations.Cache
import java.util.concurrent.TimeUnit

interface UserApi {
    // Cache GET request for 5 minutes
    @GET("users")
    @Cache(duration = 5, timeUnit = TimeUnit.MINUTES)
    suspend fun getUsers(): List<User>

    // Cache POST request for 30 seconds
    @POST("users/search")
    @Cache(duration = 30, timeUnit = TimeUnit.SECONDS)
    suspend fun searchUsers(@Body query: SearchQuery): List<User>

    // Cache PUT request for 1 minute
    @PUT("users/{id}")
    @Cache(duration = 1, timeUnit = TimeUnit.MINUTES)
    suspend fun updateUser(@Path("id") id: String, @Body user: User): User

    // Cache PATCH request for 2 minutes
    @PATCH("users/{id}")
    @Cache(duration = 2, timeUnit = TimeUnit.MINUTES)
    suspend fun patchUser(@Path("id") id: String, @Body updates: Map<String, Any>): User
}
```

**Supported TimeUnits:** `SECONDS`, `MINUTES`, `HOURS`, `DAYS`

### Easy API Mocking (Debug builds only)

Mock API responses using the `@MockResponse` annotation:

```kotlin
import com.indiedev.networking.annotations.MockResponse

interface UserApi {
    // Auto-loads from res/raw/users_list.json
    @GET("users")
    @MockResponse("users_list")
    suspend fun getUsers(): List<User>

    // Custom status code and delay
    @POST("login")
    @MockResponse(resourceName = "login_success", statusCode = 200, delay = 1000)
    suspend fun login(@Body request: LoginRequest): LoginResponse

    // Simulate error response
    @GET("users/{id}")
    @MockResponse(resourceName = "user_not_found", statusCode = 404)
    suspend fun getUser(@Path("id") id: String): User
}
```

**How it works:**
1. Create JSON files in `res/raw/` (e.g., `users_list.json`, `login_success.json`)
2. Add `@MockResponse` annotation to your API methods
3. Mock responses are automatically served in debug builds only

### Automatic Token Management

NetworkingKit automatically handles access/refresh tokens with **zero manual intervention**. Simply implement the required interfaces:

#### Step 1: Create Token Refresh Service

Define your refresh service interface and request/response models. **You have complete freedom** to define any fields or types based on your API requirements:

```kotlin
interface AppTokenRefreshService : TokenRefreshService {
    @POST("auth/refresh")
    suspend fun renewToken(@Body request: RefreshTokenRequest): RefreshTokenResponse
}

// Define request/response models based on YOUR API structure
data class RefreshTokenRequest(
    val refreshToken: String,
    // Add any fields your API requires
    val deviceId: String? = null,
    val clientKey: String? = null
)

data class RefreshTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    // Add any fields your API returns
    val tokenType: String? = null,
    val userId: String? = null
)
```

#### Step 2: Implement TokenRefreshConfig

```kotlin
class AppTokenRefreshConfig(
    private val prefs: SharedPreferences
) : TokenRefreshConfig<RefreshTokenRequest, RefreshTokenResponse> {

    override fun getServiceClass() = AppTokenRefreshService::class.java

    override fun createRefreshRequest() = RefreshTokenRequest(
        refreshToken = prefs.getString("refresh_token", "") ?: ""
    )

    override fun extractTokens(response: RefreshTokenResponse) = AuthTokens(
        accessToken = response.accessToken,
        refreshToken = response.refreshToken,
        expiresIn = response.expiresIn
    )

    override fun isRefreshTokenExpired(exception: HttpException): Boolean {
        return exception.code() == 401 || exception.code() == 403
    }

    override fun getRetryCount(): Int = 3 // Optional, default is 3
}
```

#### Step 3: Implement SessionTokenManager

Store tokens anywhere you want - **SharedPreferences, Room DB, DataStore, or any other storage**:

```kotlin
// Example using SharedPreferences
class AppSessionManager(
    private val prefs: SharedPreferences
) : SessionTokenManager {

    override fun getAccessToken(): String {
        return prefs.getString("access_token", "") ?: ""
    }

    override fun onTokenRefreshed(accessToken: String, refreshToken: String, expiresIn: Long) {
        // Called automatically when tokens are refreshed
        // Store in your preferred storage (Preferences, Room, DataStore, etc.)
        prefs.edit()
            .putString("access_token", accessToken)
            .putString("refresh_token", refreshToken)
            .putLong("expires_in", expiresIn)
            .commit()
    }

    override fun onTokenExpires() {
        // Called when refresh token expires - handle logout
        prefs.edit().clear().apply()
        // Navigate to login screen
    }

    override fun getTokenRefreshConfig(): TokenRefreshConfig<*, *> {
        return AppTokenRefreshConfig(prefs)
    }
}

```

#### Step 4: Provide via Hilt

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkingModule {

    @Provides
    @Singleton
    fun provideSessionManager(
        @ApplicationContext context: Context
    ): SessionTokenManager {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return AppSessionManager(prefs)
    }

    @Provides
    @Singleton
    fun provideNetworkingKit(
        @ApplicationContext context: Context,
        sessionManager: SessionTokenManager
    ): NetworkingKit {
        return NetworkingKit.builder(context)
            .gatewayUrls(createGatewayUrls())
            .sessionManager(sessionManager)
            .build()
    }
}
```

**What NetworkingKit handles automatically:**
- ✅ Adds `Authorization: Bearer <token>` to all requests
- ✅ Detects 401 responses and refreshes tokens
- ✅ Thread-safe token refresh (prevents multiple simultaneous refreshes)
- ✅ Retries failed requests with new token
- ✅ Calls `onTokenExpires()` when refresh token is invalid
- ✅ Automatic retry logic with configurable retry count
- 

### Certificate Transparency

NetworkingKit provides **automatic SSL/TLS security** using Certificate Transparency (CT) to protect against man-in-the-middle attacks and rogue certificates.

#### What is Certificate Transparency?

Certificate Transparency is a security mechanism that validates SSL certificates against public CT logs maintained by Google and other organizations. Unlike traditional SSL pinning (which requires manual certificate management), CT provides:

- **Automatic validation** - No manual certificate pinning required
- **Protection against rogue certificates** - Detects fraudulently issued certificates
- **Zero maintenance** - No need to update your app when certificates rotate
- **Industry standard** - Uses Google's public CT log list

#### Under the Hood

NetworkingKit uses **[Appmattus Certificate Transparency](https://github.com/appmattus/certificatetransparency)** library (`v2.5.72`) which:

1. **Validates certificates** against Google's CT log list (`https://www.gstatic.com/ct/log_list/v3/`)
2. **Pins gateway URLs** - Automatically pins your Main, Secure, and Auth gateway URLs
3. **Fails secure** - Blocks connections if certificates aren't found in CT logs
4. **Zero configuration** - Works automatically once enabled

#### Setup

```kotlin
class CertTransparencyProvider : CertTransparencyConfig {
    override fun isFlagEnable(): Boolean = BuildConfig.ENABLE_CERT_TRANSPARENCY
}

// In your build.gradle.kts
android {
    buildTypes {
        release {
            buildConfigField("boolean", "ENABLE_CERT_TRANSPARENCY", "true")
        }
        debug {
            buildConfigField("boolean", "ENABLE_CERT_TRANSPARENCY", "false")
        }
    }
}

// Provide via Hilt
@Module
@InstallIn(SingletonComponent::class)
object NetworkingModule {

    @Provides
    @Singleton
    fun provideCertTransparency(): CertTransparencyConfig {
        return CertTransparencyProvider()
    }

    @Provides
    @Singleton
    fun provideNetworkingKit(
        @ApplicationContext context: Context,
        certTransparency: CertTransparencyConfig
    ): NetworkingKit {
        return NetworkingKit.builder(context)
            .gatewayUrls(createGatewayUrls())
            .certTransparencyProvider(certTransparency)
            .build()
    }
}
```

**Benefits:**
- ✅ No manual SSL pinning required
- ✅ Automatic certificate validation against public logs
- ✅ Protection against man-in-the-middle attacks
- ✅ Zero maintenance when certificates rotate
- ✅ Production-ready security with minimal setup

### Automatic Network Connectivity Checks

NetworkingKit **automatically validates network connectivity before every API call**, preventing wasted requests and providing clear error handling.

#### How It Works

Before each API request, NetworkingKit checks:

1. **Network Connection** - Is the device connected to WiFi, Cellular, VPN, or WiFi Aware?
2. **Internet Availability** - Does the connection have actual internet access?

If either check fails, the request is cancelled immediately with a specific exception.

#### Under the Hood

NetworkingKit uses Android's `ConnectivityManager` and `NetworkCapabilities` API to:

**Check for Active Network Connection:**
```kotlin
// Detects if device is connected to:
- WiFi (TRANSPORT_WIFI)
- Cellular/Mobile Data (TRANSPORT_CELLULAR)
- VPN (TRANSPORT_VPN)
- WiFi Aware (TRANSPORT_WIFI_AWARE)
```

**Validate Internet Access:**
```kotlin
// Checks if connection has actual internet (NET_CAPABILITY_VALIDATED)
// This detects "connected but no internet" scenarios like:
- WiFi connected but router offline
- Captive portals (login required)
- Network with no internet gateway
```

#### Exception Handling

NetworkingKit throws specific exceptions for different connectivity issues:

```kotlin
class UserRepository @Inject constructor(
    private val userApi: UserApi
) {
    suspend fun getUser(id: String): User? {
        return try {
            userApi.getUser(id)
        } catch (e: NoConnectivityException) {
            // No network connection (WiFi/Cellular/VPN)
            // Show: "No network available, please check your WiFi or Data connection"
            null
        } catch (e: NoInternetException) {
            // Connected but no internet access
            // Show: "No internet available, please check your connected WiFi or Data"
            null
        } catch (e: Exception) {
            // Other errors
            null
        }
    }
}
```

**Benefits:**
- ✅ Prevents wasted API calls when offline
- ✅ Instant error feedback (no network timeout delays)
- ✅ Distinguishes between "no connection" and "no internet"
- ✅ Works automatically on every request
- ✅ Supports WiFi, Cellular, VPN, and WiFi Aware

### Event Logging & Error Tracking

NetworkingKit **automatically logs all HTTP errors and authentication events** to your analytics and crash reporting tools. Simply implement the logging interfaces and NetworkingKit handles the rest.

#### What Gets Logged Automatically

**HTTP Errors (4xx & 5xx):**
- ✅ All **Client Errors** (400-499) - Throws `ClientHttpException`
- ✅ All **Server Errors** (500-599) - Throws `ServerHttpException`
- ✅ **IOException** during network calls
- ✅ **Automatic error parsing** - Extracts `message` and `code` from JSON error response body
- ✅ Includes: API URL, HTTP status code, error message, backend error code, exception name

**Error Body Parsing:**
NetworkingKit automatically parses JSON error responses and extracts:
```json
{
  "message": "User not found",
  "code": "USER_404"  // or "error_code"
}
```
You can access these via `exception.message()` and `exception.errorCode()` methods.

**Token Refresh Events:**
- ✅ `refresh_token_not_valid` - Refresh token expired/invalid
- ✅ `http_error` - HTTP errors during token refresh
- ✅ `refreshing_auth_token_failed` - Token refresh failed after retries
- ✅ `refresh_token_api_io_failure` - Network errors during refresh
- ✅ Includes: HTTP code, backend code, error message

#### Implementation

```kotlin
// 1. Implement EventLogger for analytics
class AppEventLogger : EventLogger {
    override fun logEvent(eventName: String, properties: HashMap<String, Any>) {
        // Log to Firebase Analytics, Mixpanel, etc.
        FirebaseAnalytics.getInstance(context).logEvent(eventName, Bundle().apply {
            properties.forEach { (key, value) ->
                putString(key, value.toString())
            }
        })
    }
}

// 2. Implement ExceptionLogger for crash reporting
class AppExceptionLogger : ExceptionLogger {
    override fun logException(throwable: Throwable) {
        // Log to Crashlytics, Sentry, etc.
        FirebaseCrashlytics.getInstance().recordException(throwable)
    }

    override fun logException(throwable: Throwable, customKeys: Map<String, Any>) {
        // Log with additional context (API URL, HTTP code, etc.)
        val crashlytics = FirebaseCrashlytics.getInstance()
        customKeys.forEach { (key, value) ->
            crashlytics.setCustomKey(key, value.toString())
        }
        crashlytics.recordException(throwable)
    }
}

// 3. Provide via Hilt
@Module
@InstallIn(SingletonComponent::class)
object NetworkingModule {

    @Provides
    @Singleton
    fun provideEventLogger(): EventLogger = AppEventLogger()

    @Provides
    @Singleton
    fun provideExceptionLogger(): ExceptionLogger = AppExceptionLogger()

    @Provides
    @Singleton
    fun provideNetworkingKit(
        @ApplicationContext context: Context,
        eventLogger: EventLogger,
        exceptionLogger: ExceptionLogger
    ): NetworkingKit {
        return NetworkingKit.builder(context)
            .gatewayUrls(createGatewayUrls())
            .eventLogger(eventLogger)
            .exceptionLogger(exceptionLogger)
            .build()
    }
}
```

#### Logged Properties

Every HTTP error is automatically logged with:

| Property | Description | Example |
|----------|-------------|---------|
| `ExceptionName` | Type of exception | `ClientHttpException` |
| `httpCode` | HTTP status code | `404` |
| `ApiUrl` | Full API endpoint URL | `https://api.example.com/users/123` |
| `ErrorMessage` | Error message from response | `User not found` |
| `backendCode` | Custom backend error code | `USER_404` |

**Benefits:**
- ✅ Zero manual logging - all errors tracked automatically
- ✅ Rich context - API URL, HTTP codes, error messages included
- ✅ Token refresh monitoring - track authentication issues
- ✅ Works with any analytics/crash reporting tool
- ✅ Production-ready error tracking out of the box


### Built-in Debugging Tools

NetworkingKit includes **powerful debugging tools that work automatically in debug builds** with zero configuration. All tools are production-safe and automatically disabled in release builds.

#### Included Debug Tools

**1. Flipper Network Plugin** (Debug only)
- Facebook's powerful debugging platform
- Visualize all network requests/responses in desktop app
- Inspect request headers, response bodies, timing, and errors
- Additional plugins: CrashReporter, Databases, Navigation, Inspector
- **Automatically initialized and started** - just install Flipper desktop app

**2. Chucker** (Debug only)
- On-device HTTP inspector with visual UI
- See all API calls directly on your Android device
- Inspect requests, responses, headers, and body
- Search and filter network calls
- Share/export logs for debugging

**3. HTTP Logging Interceptor** (Debug only)
- Detailed Logcat output for all network calls
- Logs full request/response bodies
- Level: `BODY` (most verbose)
- Perfect for quick debugging in Android Studio

**4. Mock Response Interceptor** (Debug only)
- Serves `@MockResponse` annotated endpoints
- See [Easy API Mocking](#easy-api-mocking-debug-builds-only) section

#### How It Works
**Debug Builds:**
```kotlin
// Automatically enabled when BuildConfig.DEBUG = true
val networkingKit = NetworkingKit.builder(context)
    .gatewayUrls(urls)
    .build()

// NetworkingKit automatically adds:
// ✅ FlipperOkhttpInterceptor - Flipper network debugging
// ✅ ChuckerInterceptor - On-device HTTP inspector
// ✅ HttpLoggingInterceptor - Logcat output (BODY level)
// ✅ MockResponseInterceptor - Mock API responses
```

**Release Builds:**
```kotlin
// All debug interceptors automatically disabled
// Flipper returns null interceptor (no-op)
// Uses chucker-no-op library (zero overhead)
```

#### Flipper Setup (Optional but Recommended)

1. **Install Flipper Desktop:** [Download from Facebook](https://fbflipper.com/)
2. **Run your debug app** - Flipper automatically connects
3. **Enable Network plugin** - See all API calls in real-time

**Flipper Plugins Included:**
- `NetworkFlipperPlugin` - HTTP traffic inspection
- `CrashReporterPlugin` - Crash reporting
- `DatabasesFlipperPlugin` - Database inspection
- `NavigationFlipperPlugin` - Navigation tracking
- `InspectorFlipperPlugin` - View hierarchy inspection

#### Chucker In-App Notification

Chucker shows a notification for every API call. Tap it to see:
- Request/response details
- Headers and body
- Timing information
- Error details

**Benefits:**
- ✅ **Zero configuration** - Works automatically in debug builds
- ✅ **Production-safe** - All tools disabled in release (zero overhead)
- ✅ **Multiple tools** - Flipper, Chucker, and HTTP logging work together
- ✅ **Visual debugging** - Inspect network traffic on device and desktop
- ✅ **Full request/response logging** - See complete payloads
- ✅ **No manual setup** - NetworkingKit handles all interceptor configuration



```
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---