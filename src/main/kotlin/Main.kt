import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import kotlinx.coroutines.*
import java.io.Writer
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import io.ktor.client.engine.cio.CIO as ClientCIO
import io.ktor.server.cio.CIO as ServerCIO

const val PORT = 9096
const val COROUTINES_NUMBER = 5
const val MAX_CONNECTIONS_NUMBER = 1
const val RESPONSE_DELAY = 5_000L
const val REQUEST_TIMEOUT = 7_000L

fun main() {
    val server = embeddedServer(ServerCIO, port = PORT, host = "0.0.0.0", configure = {

    }) {
        routing {
            get("/") {
                call.respondTextWriter(writer = ::writeResponse)
            }
        }
    }.start(wait = false)
    println("Start")

    val httpClient = HttpClient(ClientCIO) {
        engine {
            maxConnectionsCount = MAX_CONNECTIONS_NUMBER
        }
        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT
        }
    }

    val successCounter = AtomicInteger()
    val failCounter = AtomicInteger()
    runBlocking {
        (1..COROUTINES_NUMBER).map { launch(Dispatchers.Default) {
            try {
                httpClient.get<HttpResponse>("http://localhost:$PORT/").receive<ByteArray>()
                successCounter.incrementAndGet()
            } catch (e: Throwable) {
                e.printStackTrace()
                failCounter.incrementAndGet()
            }
        } }.joinAll()
    }
    println("Done with all")
    println("Successes: " + successCounter.get())
    println("Fails: " + failCounter.get())

    print("Closing client and server... ")
    httpClient.close()
    server.stop(10, 30, TimeUnit.SECONDS)
    print("OK")
}

suspend fun writeResponse(writer: Writer) {
    delay(RESPONSE_DELAY)
    withContext(Dispatchers.IO) {
        writer.write("A")
    }
}