import com.epam.drill.transport.WSClientFactory
import com.epam.drill.transport.common.ws.URL
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class MainTest {
    private val iterations = 100
    private val messageSize = 100000

    @Test
    fun tst() = runBlocking {
        val atomic = atomic(0)
        val isOpened = atomic(false)
        val message = ByteArray(messageSize) { 'c'.toByte() }
        val client = WSClientFactory.createClient(
            URL("ws://echo.websocket.org/echo"),
            txBufferSize = messageSize,
            rxBufferSize = messageSize
        )

        client.onOpen {
            println("Opened")
            isOpened.value = true
        }

        client.onBinaryMessage {
            atomic.incrementAndGet()
        }

        client.onError {
            println("Error!")
        }

        client.onClose {
            println("Closed!")
        }

        while (!isOpened.value) {
            delay(10)
        }

        repeat(iterations) {
            client.send(message)
        }
        delay(20000)
        assertEquals(iterations, atomic.value)
    }
}


