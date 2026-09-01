package com.sigmotoa.gitdash.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Crea el [HttpClient] de Ktor compartido por todas las APIs.
 *
 * No se especifica engine: Ktor lo resuelve desde el classpath de cada target
 * (OkHttp en `androidMain`, Darwin/NSURLSession en `iosMain`).
 *
 * `expectSuccess = true` reproduce el comportamiento de Retrofit: las respuestas
 * fuera del rango 2xx lanzan una excepción, que las capas superiores capturan.
 */
internal fun createHttpClient(): HttpClient = HttpClient {
    expectSuccess = true

    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            }
        )
    }

    install(Logging) {
        level = LogLevel.HEADERS
    }
}
