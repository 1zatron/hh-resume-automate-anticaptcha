package com.github.s3rgeym.hh_resume_automate.api

import ru.s3rgeym.hhresumeautomate.data.network.CaptchaInterceptor
import android.content.SharedPreferences
import android.net.Uri
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

sealed class ApiException(val json: Map<String, Any?>, message: String) : Exception(message) {
    companion object {
        fun isLimitExceeded(json: Map<String, Any?>): Boolean {
            val errors = json["errors"] as? List<*> ?: return false
            return errors.any {
                (it as? Map<*, *>)?.get("value") == "limit_exceeded"
            }
        }
    }
}

class BadRequestException(json: Map<String, Any?>) : ApiException(json, "Bad Request")
class ForbiddenException(json: Map<String, Any?>) : ApiException(json, "Forbidden")
class ResourceNotFoundException(json: Map<String, Any?>) : ApiException(json, "Not Found")
class TooManyRequestsException(json: Map<String, Any?>) : ApiException(json, "Too Many Requests")
class LimitExceededException(json: Map<String, Any?>) : ApiException(json, "Limit Exceeded")
class ClientErrorException(json: Map<String, Any?>) : ApiException(json, "Client Error")
class ServerErrorException(json: Map<String, Any?>) : ApiException(json, "Server Error")

class ApiDelayInterceptor(private val apiDelay: Long) : Interceptor {
    private val lastRequestTime = AtomicLong(0)
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val lastTime = lastRequestTime.get()
        val waitTime = lastTime + apiDelay - System.currentTimeMillis()
        if (waitTime > 0) {
            Thread.sleep(waitTime)
        }
        lastRequestTime.set(System.currentTimeMillis())
        return chain.proceed(chain.request())
    }
}

class ApiClient(
    public var accessToken: String? = null,
    public var refreshToken: String? = null,
    public var accessExpiresAt: Long = 0,
    protected val apiDelay: Long = 300L,
    protected val apiUrl: String = ApiConstants.API_URL,
    protected val oauthUrl: String = ApiConstants.OAUTH_URL,
    protected val clientId: String = ApiConstants.CLIENT_ID,
    protected val clientSecret: String = ApiConstants.CLIENT_SECRET,
    protected val sharedPrefs: SharedPreferences? = null,
) {
    protected val client = OkHttpClient.Builder()
        .followRedirects(false)
        .addInterceptor(CaptchaInterceptor()) // <-- Вызов без контекста, перехватчик разберется сам
        .addInterceptor(ApiDelayInterceptor(apiDelay))
        .build()

    init {
        sharedPrefs?.let { loadFromPrefs(it) }
    }

    val isAuthenticated: Boolean
        get() = !accessToken.isNullOrEmpty()

    val isAccessExpired: Boolean
        get() = System.currentTimeMillis() >= accessExpiresAt

    suspend fun api(
        method: String,
        endpoint: String,
        params: Map<String, Any?>? = null
    ): Map<String, Any?> = apiRequest(method, apiUrl, endpoint, params)

    fun getAuthorizeUrl(redirectUri: String = "", state: String = "", scope: String = ""): String {
        val params = mapOf(
            "client_id" to clientId,
            "client_secret" to clientSecret,
            "redirect_uri" to redirectUri,
            "response_type" to "code",
            "scope" to scope,
            "state" to state
        ).filterValues { it.isNotEmpty() }
        return buildUrl(oauthUrl, "/authorize", params)
    }

    suspend fun authenticate(code: String): ApiClient {
        val params = mapOf(
            "client_id" to clientId,
            "client_secret" to clientSecret,
            "grant_type" to "authorization_code",
            "code" to code,
        )
        val token = request("POST", oauthUrl, "/token", params)
        handleToken(token)
        return this
    }

    suspend fun refreshAccessToken(): ApiClient {
        val params = mapOf(
            "client_id" to clientId,
            "client_secret" to clientSecret,
            "grant_type" to "refresh_token",
            "refresh_token" to (refreshToken ?: throw IllegalStateException("No refresh token"))
        )
        val token = request("POST", oauthUrl, "/token", params)
        handleToken(token)
        return this
    }

    protected suspend fun apiRequest(
        method: String,
        baseUrl: String,
        endpoint: String,
        params: Map<String, Any?>? = null,
    ): Map<String, Any?> {
        return try {
            request(method, baseUrl, endpoint, params, true)
        } catch (e: ForbiddenException) {
            if (isAccessExpired && refreshToken != null) {
                refreshAccessToken()
                request(method, baseUrl, endpoint, params, true)
            } else {
                throw e
            }
        }
    }

    protected suspend fun request(
        method: String,
        baseUrl: String,
        endpoint: String,
        params: Map<String, Any?>? = null,
        includeAuthHeader: Boolean = false,
    ): Map<String, Any?> {
        require(method in listOf("GET", "POST", "PUT", "DELETE")) { "Invalid HTTP method: $method" }
        val hasBody = method in listOf("POST", "PUT")
        val url = buildUrl(baseUrl, endpoint, if (hasBody) null else params?.mapValues { it.value?.toString() ?: "" })
        val requestBuilder = Request.Builder().url(url).headers(defaultHeaders())
        if (includeAuthHeader) {
            accessToken?.let {
                requestBuilder.addHeader("Authorization", "Bearer $it")
            }
        }
        if (hasBody) {
            val builder = FormBody.Builder()
            params?.forEach { (key, value) ->
                builder.add(key, value?.toString() ?: "")
            }
            val requestBody = builder.build()
            requestBuilder.method(method, requestBody)
        } else if (method != "GET") {
            requestBuilder.method(method, null)
        }

        val response = client.newCall(requestBuilder.build()).execute()
        val bodyStr = response.body?.string()?.trim()
        val json = if (!bodyStr.isNullOrEmpty()) JSONObject(bodyStr).toMap() else emptyMap()

        if (!response.isSuccessful) {
            throwApiException(response, json)
        }
        return json
    }

    protected fun buildUrl(baseUrl: String, endpoint: String, params: Map<String, Any?>? = null): String {
        val u = Uri.parse(baseUrl)
        val ub = Uri.Builder().scheme(u.scheme).authority(u.authority)
        ub.path(u.path?.removeSuffix("/") + "/" + endpoint.removePrefix("/"))
        params?.forEach { (k, v) ->
            ub.appendQueryParameter(k, v?.toString() ?: "")
        }
        return ub.build().toString()
    }

    protected fun generateRandomDeviceModel(): String {
        val charset = ('A'..'Z') + ('0'..'9')
        return (1..10).map { charset.random(Random) }.joinToString("")
    }

    protected fun generateUserAgent(): String {
        val major = Random.nextInt(5, 7)
        val minor = Random.nextInt(100, 150)
        val patch = Random.nextInt(10000, 15000)
        val randomDeviceModel = generateRandomDeviceModel()
        return "ru.hh.android/$major.$minor.$patch, Device: $randomDeviceModel"
    }

    private fun loadFromPrefs(prefs: SharedPreferences) {
        accessToken = prefs.getString("access_token", null)
        refreshToken = prefs.getString("refresh_token", null)
        accessExpiresAt = prefs.getLong("access_expires_at", 0)
    }

    private fun handleToken(token: Map<String, Any?>) {
        accessToken = token["access_token"] as? String
        refreshToken = token["refresh_token"] as? String
        val expiresIn = (token["expires_in"] as? Number)?.toLong() ?: 0L
        accessExpiresAt = System.currentTimeMillis() + (expiresIn * 1000)
        sharedPrefs?.edit()?.apply {
            putString("access_token", accessToken)
            putString("refresh_token", refreshToken)
            putLong("access_expires_at", accessExpiresAt)
            apply()
        }
    }

    private fun defaultHeaders(): Headers {
        return Headers.Builder()
            .add("User-Agent", generateUserAgent())
            .add("Accept", "application/json")
            .build()
    }

    private fun throwApiException(response: Response, json: Map<String, Any?>) {
        when (response.code) {
            400 -> throw BadRequestException(json)
            403 -> throw ForbiddenException(json)
            404 -> throw ResourceNotFoundException(json)
            429 -> throw TooManyRequestsException(json)
            else -> {
                if (ApiException.isLimitExceeded(json)) throw LimitExceededException(json)
                if (response.code in 500..599) throw ServerErrorException(json)
                throw ClientErrorException(json)
            }
        }
    }

    private fun JSONObject.toMap(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        val keys = this.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            var value = this.get(key)
            if (value is org.json.JSONArray) {
                value = value.toList()
            } else if (value is JSONObject) {
                value = value.toMap()
            } else if (value == JSONObject.NULL) {
                value = null
            }
            map[key] = value
        }
        return map
    }

    private fun org.json.JSONArray.toList(): List<Any?> {
        val list = mutableListOf<Any?>()
        for (i in 0 until this.length()) {
            var value = this.get(i)
            if (value is org.json.JSONArray) {
                value = value.toList()
            } else if (value is JSONObject) {
                value = value.toMap()
            } else if (value == JSONObject.NULL) {
