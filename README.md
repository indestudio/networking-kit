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
- 🏢 **Multi-Gateway Architecture** - Separate Main, Secure (card transactions), and Auth gateways
- 🔐 **Zero auth complexity** - Automatic token management built-in (access and refresh tokens)
- 💾 **No DB needed** - Cache any API response directly
- 🛡️ **Security by default** - Certificate transparency without manual SSL pinning
- 🌐 **Smart connectivity** - Built-in network detection and connectivity checks
- ⚠️ **Advanced error handling** - Custom exceptions with detailed error info and automatic logging
- 🔍 **Built-in debugging** - Flipper, Chucker, and HTTP logging support
- 🧪 **Easy testing** - Mock any API with JSON files

## ✨ Core Features

### 🏢 Multi-Gateway Architecture
- **Main Gateway** - Standard API operations
- **Secure Gateway** - Card transactions & sensitive operations
- **Auth Gateway** - Authentication & token management

### 🔐 Zero Auth Complexity
- Automatic access/refresh token management
- Thread-safe session management
- No manual token handling required in app
- Built-in retry logic for auth failures

### 💾 Universal Caching (No DB Needed)
- Cache any API response (GET, POST, PUT, PATCH)
- Custom cache duration via headers
- No Room DB dependency needed
- Intelligent cache management

### 🛡️ Security by Default
- Built-in SSL certificate validation
- Certificate transparency without manual SSL pinning
- Enhanced security without complexity
- Production-ready security features

### 🌐 Smart Connectivity
- WiFi, Cellular, VPN detection
- Built-in network connectivity checks
- Connection type awareness
- Automatic connectivity validation per API call

### ⚠️ Advanced Error Handling
- **Custom exceptions** - `ClientHttpException`, `ServerHttpException`, `NoConnectivityException`
- **Automatic API exception logging** - Structured error tracking
- **JSON error response parsing** - Extract error codes and messages
- **Event-based error logging** - Integrated with analytics platforms

### 🔍 Built-in Debugging
- **Built-in logger support** - Flipper, Chucker, HTTP logging
- Debug-only features automatically enabled
- Production-safe logging (auto-disabled in release)
- Visual network inspection tools

### 🧪 Easy Testing
- Mock API responses from JSON resources
- Easy backend API testing without server changes
- Automatic mock file serving
- Debug-only mock interceptor

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

### Your Retrofit interface 

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



## ⚙️ Configuration

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

### Universal API Caching

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

### Zero-Config Authentication

**No manual token handling required!** NetworkingKit automatically manages access/refresh tokens:

```kotlin
class MySessionManager : SessionTokenManager {
    override fun getAccessToken(): String = prefs.getString("access_token", "")

    override fun onTokenRefreshed(accessToken: String, refreshToken: String, expiresIn: Long) {
        // NetworkingKit calls this automatically when tokens are refreshed
        prefs.edit()
            .putString("access_token", accessToken)
            .putString("refresh_token", refreshToken)
            .apply()
    }

    override fun onTokenExpires() {
        // Handle logout when refresh token expires
        navigateToLogin()
    }

    override fun getTokenRefreshConfig(): TokenRefreshConfig<*, *> {
        return MyTokenRefreshConfig() // One-time setup
    }
}

// That's it! NetworkingKit handles everything else automatically:
// ✅ Adds Bearer tokens to requests
// ✅ Detects 401 responses
// ✅ Refreshes tokens automatically
// ✅ Retries failed requests
// ✅ Thread-safe token refresh
```

### Event Logging & Error Tracking

```kotlin
class MyEventLogger : EventLogger {
    override fun logEvent(eventName: String, properties: HashMap<String, Any>) {
        Analytics.logEvent(eventName, properties)
    }
}

class MyExceptionLogger : ExceptionLogger {
    override fun logException(throwable: Throwable) {
        Crashlytics.recordException(throwable)
    }

    override fun logException(throwable: Throwable, customKeys: Map<String, Any>) {
        customKeys.forEach { (key, value) ->
            Crashlytics.setCustomKey(key, value.toString())
        }
        Crashlytics.recordException(throwable)
    }
}
```

### Certificate Transparency

```kotlin
class CertTransparencyProvider : CertTransparencyConfig {
    override fun isFlagEnable(): Boolean = BuildConfig.ENABLE_CERT_TRANSPARENCY
}
```

## 📖 Usage Examples

### API Error Handling

```kotlin
class UserRepository(private val userApi: UserApi) {

    suspend fun getUser(userId: String): User? {
        return try {
            userApi.getUser(userId)
        } catch (exception: Exception) {
            handleError(exception)
            null
        }
    }

    private fun handleError(exception: Throwable) {
        when (exception) {
            is ClientHttpException -> {
                // Handle 4xx errors
                val errorCode = exception.errorCode()
                val httpCode = exception.code()
                Log.e("API", "Client error: $errorCode")
            }
            is ServerHttpException -> {
                // Handle 5xx errors
                Log.e("API", "Server error: ${exception.code()}")
            }
            is NoConnectivityException -> {
                // Handle no network
                Log.e("API", "No network connection")
            }
            else -> {
                Log.e("API", "Unknown error", exception)
            }
        }
    }
}
```

### Enterprise Multi-Gateway Usage

```kotlin
class ApiRepository {
    // Enterprise-grade gateway separation for different security requirements
    private val mainApi = networkingKit.createMainService(MainApi::class.java)           // Standard operations
    private val secureApi = networkingKit.createSecureService(SecureApi::class.java)     // Card transactions
    private val authApi = networkingKit.createAuthService(AuthApi::class.java)           // Authentication

    // Main Gateway - Standard business operations
    suspend fun getUserProfile() = try { mainApi.getProfile() } catch (e: Exception) { null }
    suspend fun getProductCatalog() = try { mainApi.getProducts() } catch (e: Exception) { null }

    // Secure Gateway - Financial transactions & sensitive data
    suspend fun processPayment(card: CardDetails) = try { secureApi.processPayment(card) } catch (e: Exception) { null }
    suspend fun getTransactionHistory() = try { secureApi.getTransactions() } catch (e: Exception) { null }

    // Auth Gateway - Token management
    suspend fun refreshToken() = try { authApi.refreshToken() } catch (e: Exception) { null }
    suspend fun validateSession() = try { authApi.validateSession() } catch (e: Exception) { null }
}
```

## 💾 Universal API Caching

**No Room DB Required!** Cache any HTTP method with simple headers:

```kotlin
interface ApiService {
    @Headers("Cache-Duration: 5", "Cache-Unit: MINUTES")
    @GET("users/profile")
    suspend fun getProfile(): UserProfile

    @Headers("Cache-Duration: 30", "Cache-Unit: SECONDS")
    @POST("users/search")
    suspend fun searchUsers(@Body query: SearchQuery): List<User>

    @Headers("Cache-Duration: 1", "Cache-Unit: HOURS")
    @PUT("users/settings")
    suspend fun updateSettings(@Body settings: UserSettings): Result
}
```

**Benefits:**
- ✅ Cache GET, POST, PUT, PATCH responses
- ✅ Reduce server load and improve performance
- ✅ Works offline automatically
- ✅ No database setup or entity classes needed

**Supported units:** `SECONDS`, `MINUTES`, `HOURS`, `DAYS`

## 🧪 Development & Testing

### API Mocking (Debug builds only)

1. **Create JSON files** in `src/main/res/raw/`:
   ```
   res/raw/users_profile.json
   res/raw/api_data.json
   ```

2. **Add mock endpoints**:
   ```kotlin
   @GET("users/profile/mock")
   suspend fun getProfile(): Profile
   ```

3. **Automatic serving**: MockInterceptor serves the corresponding JSON file

**File naming:** `/api/users/123/mock` → `api_users.json`

### Enterprise Debugging Tools

NetworkingKit provides built-in support for enterprise debugging tools:

```kotlin
// Automatic integration - no additional setup required
val networkingKit = NetworkingKit.builder(context)
    .gatewayUrls(urls)
    .build()

// Debug builds automatically enable:
// ✅ Flipper - Facebook's debugging platform
// ✅ Chucker - Visual network inspector
// ✅ HTTP Logging - Detailed request/response logs
// ✅ Mock interceptor - JSON-based API mocking
```

**Features:**
- **Production-safe** - All debug features auto-disabled in release builds
- **Zero configuration** - Works out of the box
- **Multiple loggers** - Flipper, Chucker, and HTTP logging simultaneously
- **Visual inspection** - See all network traffic in real-time

## 🔒 Security Features

### Certificate Transparency
When enabled, NetworkingKit validates certificates against CT logs and pins gateway URLs:

```kotlin
class AppCertTransparencyProvider : CertTransparencyConfig {
    override fun isFlagEnable(): Boolean = BuildConfig.ENABLE_CERT_PINNING
}
```

## 📚 Error Handling Reference

### Exception Types

| Exception | Description | Codes |
|-----------|-------------|--------|
| `ClientHttpException` | Client errors | 400-499 |
| `ServerHttpException` | Server errors | 500-599 |
| `NoConnectivityException` | No network | - |
| `NoInternetException` | No internet access | - |

### Error Response Parsing

NetworkingKit automatically parses JSON error responses:

```json
{
  "message": "User not found",
  "code": "USER_404"
}
```

```kotlin
// Access parsed error details
val exception = result.exception as ClientHttpException
val errorCode = exception.errorCode()  // "USER_404"
val message = exception.message()      // "User not found"
val httpCode = exception.code()        // 404
```

## 🏗️ Architecture Patterns

### Repository Pattern with Dependency Injection

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideNetworkingKit(@ApplicationContext context: Context): NetworkingKit {
        return NetworkingKit.builder(context)
            .gatewayUrls(AppGatewayUrls())
            .sessionManager(AppSessionManager())
            .eventLogger(AppEventLogger())
            .build()
    }

    @Provides
    fun provideUserApi(networkingKit: NetworkingKit): UserApi =
        networkingKit.createMainService(UserApi::class.java)
}

class UserRepository @Inject constructor(
    private val userApi: UserApi
) {
    suspend fun getUser(id: String): User? = try {
        userApi.getUser(id)
    } catch (e: Exception) {
        Log.e("UserRepository", "Failed to get user", e)
        null
    }
}
```

### Flow-based Reactive APIs

```kotlin
class UserRepository(private val userApi: UserApi) {

    fun getUserFlow(userId: String): Flow<User?> = flow {
        try {
            emit(userApi.getUser(userId))
        } catch (e: Exception) {
            emit(null)
        }
    }

    fun observeUsers(): Flow<List<User>?> = flow {
        while (true) {
            try {
                emit(userApi.getUsers())
            } catch (e: Exception) {
                emit(null)
            }
            delay(30.seconds)
        }
    }
}

## 🧪 Testing

### Unit Testing with MockWebServer

```kotlin
@Test
fun `getUser returns success`() = runTest {
    // Setup
    val mockWebServer = MockWebServer()
    val networkingKit = NetworkingKit.builder(context)
        .gatewayUrls(object : GatewayBaseUrls {
            override fun getMainGatewayUrl() = mockWebServer.url("/").toString()
            override fun getSecureGatewayUrl() = ""
            override fun getAuthGatewayUrl() = ""
        })
        .build()

    val userApi = networkingKit.createMainService(UserApi::class.java)

    // Given
    mockWebServer.enqueue(
        MockResponse()
            .setResponseCode(200)
            .setBody("""{"id": "123", "name": "John"}""")
    )

    // When
    val user = userApi.getUser("123")

    // Then
    assertNotNull(user)
    assertEquals("123", user.id)

    mockWebServer.shutdown()
}
```

## 📋 Best Practices

### Performance Optimization
- **Gateway Selection**: Use appropriate gateways for different security levels
- **Smart Caching**: Add cache headers to reduce network calls
- **Error Handling**: Always wrap API calls in try-catch blocks
- **Flow Integration**: Use reactive streams for real-time data updates

### Architecture Guidelines
- **Repository Pattern**: Encapsulate API calls in repository classes
- **Dependency Injection**: Use Hilt or similar for clean architecture
- **Error Recovery**: Implement proper retry logic for network failures
- **Security**: Enable certificate transparency in production builds

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details.

## 📄 License

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

<div align="center">

[![Made with ❤️ by IndieDevTools](https://img.shields.io/badge/Made%20with%20❤️%20by-IndieDevTools-red.svg)](https://github.com/indiedevtools)

**NetworkingKit** • Modern Android Networking Made Simple

[⭐ Star on GitHub](https://github.com/indiedevtools/networkingkit) • [📖 Documentation](https://docs.networkingkit.dev) • [🐛 Report Bug](https://github.com/indiedevtools/networkingkit/issues)

</div>