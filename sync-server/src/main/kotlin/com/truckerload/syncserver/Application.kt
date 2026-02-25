package com.truckerload.syncserver

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.gson.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.request.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureSerialization()
        configureRouting()
    }.start(wait = true)
}

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        gson {}
    }
}

fun Application.configureRouting() {
    val db = SyncDatabase()
    routing {
        post("/api/webhook/sync") {
            val body = call.receive<WebhookRequest>()
            val result = db.syncLoadsCdc(body.loads ?: emptyList(), body.messageDateSeconds)
            call.respond(WebhookResponse.from(result))
        }
    }
}
