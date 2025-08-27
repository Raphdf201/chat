package net.raphdf201
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {
        val connections = Collections.newSetFromMap<Connection>(ConcurrentHashMap())
        webSocket("/chat") {
            val connection = Connection(this)
            connections += connection

            try {
                // Send a welcome message
                send("You are connected! Use /nick <name> to set your username.")

                incoming.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        when {
                            text.startsWith("/nick ") -> {
                                connection.name = text.removePrefix("/nick ").trim()
                                connection.session.send("Your name is now ${connection.name}")
                            }
                            else -> {
                                val sender = connection.name ?: "Anonymous"
                                connections.forEach {
                                    it.session.send("[$sender] $text")
                                }
                            }
                        }
                    }
                }
            } finally {
                connections -= connection
            }
        }
    }
}

data class Connection(val session: DefaultWebSocketServerSession) {
    var name: String? = null
}
