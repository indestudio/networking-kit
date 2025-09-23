# NetworkingKit 🚀

A comprehensive, production-ready networking library for Android applications built on top of Retrofit, OkHttp3, and Kotlinx Serialization. NetworkingKit provides enterprise-grade features including automatic token refresh, comprehensive error handling, debug tooling, and flexible configuration options.

## Features ✨

### 🌐 Core Networking
- **Multi-Gateway Support**: Separate endpoints for Main, Secure, and Authentication APIs
- **Modern Stack**: Built with Retrofit + OkHttp3 + Kotlinx Serialization
- **Smart Caching**: 10MB HTTP cache with intelligent control via custom headers
- **Security**: Certificate Transparency support for enhanced security
- **Performance**: Configurable timeouts (70s read/write, 30s connect) and connection pooling

### 🔐 Advanced Authentication
- **Automatic Token Refresh**: Thread-safe token refresh with mutex synchronization
- **Generic Token API**: Flexible configuration for any token refresh endpoint via reflection
- **Session Management**: Abstract token storage with lifecycle callbacks
- **Circuit Breaker**: Prevents cascading failures during token refresh
- **Retry Logic**: Configurable retry attempts (default: 3) with abort mechanisms

### 🔧 Comprehensive Interceptors
- **Headers**: Auto Bearer token injection + app version headers
- **API Failure**: HTTP error classification (4xx/5xx) with structured logging
- **Connectivity**: Network detection with custom exceptions (WiFi, Cellular, VPN)
- **Caching**: Dynamic cache control via `Cache-Duration` and `Cache-Unit` headers
- **Mocking**: Development-time API mocking from Android raw resources (debug only)

### ⚠️ Sophisticated Error Handling
- **Custom Exceptions**: `ClientHttpException`, `ServerHttpException`, Connection errors
- **JSON Error Parsing**: Extract error messages and codes from API responses (`code`/`error_code` fields)
- **Safe API Calls**: `safeApiCall` wrapper with `Result<T>` sealed class
- **Error Classification**: Utility functions for error type detection

### 🧪 Development & Testing
- **HTTP Logging**: Full request/response debugging (debug builds only)
- **Chucker Integration**: Visual network inspector (debug builds only)
- **Flipper Support**: Facebook's debugging tools (debug builds only)
- **Mock System**: JSON-based response mocking from `res/raw/` resources

## Quick Start 🚀

### Basic Setup

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize NetworkingKit
        val networkingKit = NetworkingKit.builder(this)
            .gatewayUrls(object : GatewaysBaseUrls {
                override fun getMainGatewayUrl() = "https://api.example.com/"
                override fun getSecureGatewayUrl() = "https://secure.example.com/"
                override fun getAuthGatewayUrl() = "https://auth.example.com/"
            })
            .build()
        
        // Store instance for use in repositories
        MyApp.networkingKit = networkingKit
    }
}
```

### Creating API Services

```kotlin
interface UserApi {
    @GET("users/{id}")
    suspend fun getUser(@Path("id") userId: String): User
    
    @POST("users")
    suspend fun createUser(@Body user: CreateUserRequest): User
}

// Create service instance
val userApi = networkingKit.createMainService(UserApi::class.java)
```

## Advanced Configuration ⚙️

### With Authentication

```kotlin
class MySessionManager : SessionManager {
    override fun getAuthToken(): String = // Your stored access token
    
    override fun onTokenRefreshed(accessToken: String, refreshToken: String, expiresIn: Long) {
        // Save new tokens
    }
    
    override fun onTokenExpires() {
        // Handle token expiration (logout user)
    }
    
    override fun getTokenRefreshConfig(): TokenRefreshConfig<*, *> {
        return MyTokenRefreshConfig()
    }
}

// Token refresh service interface
interface MyTokenRefreshService : TokenRefreshService {
    @Headers("client-key: your-client-key")
    @POST("v1/identity/token/renew")
    suspend fun renewToken(@Body request: RefreshTokenRequest): RefreshTokenResponse
}

class MyTokenRefreshConfig : TokenRefreshConfig<RefreshTokenRequest, RefreshTokenResponse> {
    override fun getServiceClass(): Class<out TokenRefreshService> = MyTokenRefreshService::class.java
    
    override fun createRefreshRequest(): RefreshTokenRequest = 
        RefreshTokenRequest(refreshToken = getStoredRefreshToken())
    
    override fun extractTokens(response: RefreshTokenResponse): AuthTokens =
        AuthTokens(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
            expiresIn = response.expiresIn
        )
    
    override fun isRefreshTokenExpired(exception: HttpException): Boolean = 
        exception.code() == 401 && exception.message().contains("refresh_token_expired")
    
    override fun getRetryCount(): Int = 3
}

val networkingKit = NetworkingKit.builder(context)
    .gatewayUrls(gatewayUrls)
    .sessionManager(MySessionManager())
    .build()
```

### With Event Logging

```kotlin
class MyEventLogger : NetworkEventLogger {
    override fun logEvent(eventName: String, properties: HashMap<String, Any>) {
        // Log to your analytics service
        analytics.logEvent(eventName, properties)
    }
}

class MyExceptionLogger : NetworkApiExceptionLogger {
    override fun logException(throwable: Throwable) {
        crashlytics.recordException(throwable)
    }
    
    override fun logException(throwable: Throwable, customKeys: Map<String, Any>) {
        customKeys.forEach { (key, value) ->
            crashlytics.setCustomKey(key, value.toString())
        }
        crashlytics.recordException(throwable)
    }
}

val networkingKit = NetworkingKit.builder(context)
    .gatewayUrls(gatewayUrls)
    .eventLogger(MyEventLogger())
    .exceptionLogger(MyExceptionLogger())
    .build()
```

### With Certificate Transparency

```kotlin
class MyCertTransparencyProvider : CertTransparencyFlagProvider {
    override fun isFlagEnable(): Boolean = BuildConfig.ENABLE_CERT_TRANSPARENCY
}

val networkingKit = NetworkingKit.builder(context)
    .gatewayUrls(gatewayUrls)
    .certTransparencyProvider(MyCertTransparencyProvider())
    .build()
```

## API Usage Examples 📖

### Using Safe API Calls

```kotlin
class UserRepository(private val userApi: UserApi) {
    
    suspend fun getUser(userId: String): Result<User> {
        return safeApiCall {
            userApi.getUser(userId)
        }
    }
    
    suspend fun handleUserRequest(userId: String) {
        when (val result = getUser(userId)) {
            is Result.Success -> {
                val user = result.data
                // Handle success
            }
            is Result.Error -> {
                val exception = result.exception
                // Handle error
            }
            is Result.Loading -> {
                // Show loading state
            }
        }
    }
}
```

### Error Handling

```kotlin
suspend fun handleApiCall() {
    val result = safeApiCall { userApi.getUser("123") }
    
    when (result) {
        is Result.Success -> {
            val user = result.data
            // Success
        }
        is Result.Error -> {
            when (val exception = result.exception) {
                is ClientHttpException -> {
                    // Handle 4xx errors
                    val errorCode = exception.errorCode() // Custom error code from JSON
                    val message = exception.message() // Parsed error message
                    val httpCode = exception.code() // HTTP status code
                }
                is ServerHttpException -> {
                    // Handle 5xx errors
                }
                is NoConnectivityException -> {
                    // Handle no network connection
                }
                is NoInternetException -> {
                    // Handle no internet access
                }
            }
        }
    }
}
```

### Using Different Gateways

```kotlin
// Main gateway (default)
val mainApi = networkingKit.createMainService(MainApi::class.java)

// Secure gateway for sensitive operations  
val secureApi = networkingKit.createSecureService(SecureApi::class.java)

// Auth gateway for authentication
val authApi = networkingKit.createAuthService(AuthApi::class.java)
```

## Caching 💾

### Custom Cache Control

Add custom headers to control caching behavior:

```kotlin
interface ApiService {
    @Headers(
        "Cache-Duration: 5",
        "Cache-Unit: MINUTES"
    )
    @GET("data")
    suspend fun getCachedData(): ApiResponse
    
    @Headers(
        "Cache-Duration: 1", 
        "Cache-Unit: HOURS"
    )
    @GET("profile")
    suspend fun getProfile(): UserProfile
}
```

Supported cache units (from `TimeUnit`):
- `SECONDS`
- `MINUTES` 
- `HOURS`
- `DAYS`

The cache interceptor automatically applies cache control headers for successful responses (2xx) when these headers are present.

## Mocking for Development 🧪

### Setup Mock Responses (Debug builds only)

1. Create JSON files in `src/main/res/raw/`:
   ```
   res/raw/users_profile.json
   res/raw/api_data.json
   ```

2. Use mock URLs in your API calls:
   ```kotlin
   @GET("users/profile/mock")
   suspend fun getProfile(): Profile
   ```

3. The MockInterceptor automatically serves the corresponding JSON file.

### Mock File Naming Convention

The MockInterceptor processes URLs to generate file names:

- URL: `/api/users/123/mock` → File: `api_users.json`
- Removes numeric segments (`123`)
- Takes last 2 path segments (`api`, `users`)
- Joins with underscore (`api_users`)
- Removes "mock" suffix
- Looks in `res/raw/` for the file

### Mock Response Format

Mock responses are automatically GZIP compressed and returned with:
- HTTP 200 status
- `application/json` content type
- `gzip` encoding

## Debugging 🔍

### Automatic Debug Features (Debug builds only)

NetworkingKit automatically enables debugging features in debug builds:

1. **HTTP Logging**: Full request/response logging with `HttpLoggingInterceptor`
2. **Chucker**: Visual network inspector via notification
3. **Flipper**: Facebook's debugging tools (if available)
4. **Mock Interceptor**: API mocking from raw resources

### View Network Traffic

- **Logcat**: Filter by "OkHttp" tag for HTTP logs
- **Chucker**: Install debug APK → Check notification drawer → Tap "Chucker"
- **Flipper**: Use Flipper desktop app with network plugin

## Security 🔒

### Certificate Transparency

Certificate Transparency is automatically configured when enabled:

```kotlin
// In your app's configuration
class AppCertTransparencyProvider : CertTransparencyFlagProvider {
    override fun isFlagEnable(): Boolean = BuildConfig.ENABLE_CERT_PINNING
}
```

When enabled, the library:
- Validates certificates against CT logs
- Pins gateway URLs for enhanced security
- Uses Google's CT log list service

## Error Types Reference 📚

| Exception Type | Description | HTTP Codes | Usage |
|---|---|---|---|
| `ClientHttpException` | Client-side errors | 400-499 | Invalid requests, auth errors |
| `ServerHttpException` | Server-side errors | 500-599 | Server issues, maintenance |
| `NoConnectivityException` | No network connection | N/A | Device offline |
| `NoInternetException` | Connected but no internet | N/A | Captive portal, DNS issues |

### Error JSON Parsing

The library automatically parses error responses with this structure:

```json
{
  "message": "User not found",
  "code": "USER_404",
  "errors": ["Alternative error message"]
}
```

Or alternative format:
```json
{
  "message": "Invalid request",
  "error_code": "VALIDATION_ERROR"
}
```

Access parsed values:
```kotlin
val exception = result.exception as ClientHttpException
val errorCode = exception.errorCode()  // "USER_404" or null
val message = exception.message()      // "User not found"
val httpCode = exception.code()        // 404
```

## Advanced Usage 🔧

### Flow-based API Calls

```kotlin
class UserRepository(private val userApi: UserApi) {
    
    fun getUserFlow(userId: String): Flow<Result<User>> = flow {
        emit(userApi.getUser(userId))
    }.asResult()
    
    suspend fun observeUser(userId: String) {
        getUserFlow(userId).collect { result ->
            when (result) {
                is Result.Loading -> showLoading()
                is Result.Success -> showUser(result.data)
                is Result.Error -> showError(result.exception)
            }
        }
    }
}
```

### Custom Error Handling

```kotlin
// Extension functions for error classification
fun Throwable.isNetworkError(): Boolean = this is IOException
fun Throwable.isServerError(): Boolean = this is ServerHttpException
fun Throwable.isClientError(): Boolean = this is ClientHttpException

// Usage
when {
    exception.isNetworkError() -> showNetworkError()
    exception.isServerError() -> showServerError()
    exception.isClientError() -> showValidationError()
}
```

## Testing 🧪

### Unit Testing with MockWebServer

```kotlin
class UserRepositoryTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var networkingKit: NetworkingKit
    private lateinit var userApi: UserApi
    
    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        
        networkingKit = NetworkingKit.builder(context)
            .gatewayUrls(object : GatewaysBaseUrls {
                override fun getMainGatewayUrl() = mockWebServer.url("/").toString()
                override fun getSecureGatewayUrl() = ""
                override fun getAuthGatewayUrl() = ""
            })
            .build()
            
        userApi = networkingKit.createMainService(UserApi::class.java)
    }
    
    @Test
    fun `getUser returns success`() = runTest {
        // Given
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"id": "123", "name": "John"}""")
        )
        
        // When
        val result = safeApiCall { userApi.getUser("123") }
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals("123", (result as Result.Success).data.id)
    }
    
    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }
}
```

## Migration Guide 🔄

### From OkHttp + Retrofit

```kotlin
// Before
val okHttpClient = OkHttpClient.Builder()
    .addInterceptor(AuthInterceptor())
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .build()

val retrofit = Retrofit.Builder()
    .baseUrl("https://api.example.com/")
    .client(okHttpClient)
    .addConverterFactory(GsonConverterFactory.create())
    .build()

val api = retrofit.create(ApiService::class.java)

// After  
val networkingKit = NetworkingKit.builder(context)
    .gatewayUrls(object : GatewaysBaseUrls {
        override fun getMainGatewayUrl() = "https://api.example.com/"
    })
    .sessionManager(sessionManager)
    .build()

val api = networkingKit.createMainService(ApiService::class.java)
```

### Key Changes
- Builder pattern with method chaining
- Automatic timeout configuration (70s read/write, 30s connect)
- Built-in authentication with automatic token refresh
- Kotlinx Serialization instead of Gson
- Comprehensive error handling with custom exceptions
- Built-in debug tools (Chucker, Flipper, HTTP logging)

## Performance Tips 🚀

1. **Use appropriate gateways**: Route sensitive calls through secure gateway
2. **Leverage caching**: Add `Cache-Duration` and `Cache-Unit` headers to reduce network calls
3. **Handle errors gracefully**: Use `safeApiCall` wrapper for consistent error handling
4. **Monitor token refresh**: Use event logging to track authentication issues
5. **Use Flow for reactive data**: Combine with `asResult()` for loading states
6. **Debug builds optimization**: Debug features are automatically disabled in release builds

## Best Practices 📋

### Repository Pattern

```kotlin
class UserRepository(
    private val networkingKit: NetworkingKit
) {
    private val userApi = networkingKit.createMainService(UserApi::class.java)
    private val secureUserApi = networkingKit.createSecureService(SecureUserApi::class.java)
    
    suspend fun getUser(userId: String): Result<User> = safeApiCall {
        userApi.getUser(userId)
    }
    
    suspend fun updateSensitiveData(data: SensitiveData): Result<Unit> = safeApiCall {
        secureUserApi.updateData(data)
    }
}
```

### Dependency Injection

```kotlin
// With Hilt
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideNetworkingKit(@ApplicationContext context: Context): NetworkingKit {
        return NetworkingKit.builder(context)
            .gatewayUrls(AppGatewayUrls())
            .sessionManager(AppSessionManager())
            .build()
    }
    
    @Provides
    fun provideUserApi(networkingKit: NetworkingKit): UserApi {
        return networkingKit.createMainService(UserApi::class.java)
    }
}
```

## License 📄

```
Copyright 2024 IndieDevTools

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
Built with ❤️ by IndieDevTools
</div>