package vieweralerts

import com.github.kittinunf.fuel.httpPost
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.time.Duration
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.logging.Handler
import java.util.logging.LogRecord
import kotlin.concurrent.read
import kotlin.concurrent.thread
import kotlin.concurrent.write

class RemoteLogHandler(val url: String) : Handler() {

    val lock = ReentrantReadWriteLock()
    val cache = arrayListOf<LogRecord>()

    init {
        thread(isDaemon = true, name = "log_dispatcher") {
            while (true) {
                processRecords()
                Thread.sleep(Duration.ofSeconds(1).toMillis())
            }
        }
    }

    override fun publish(record: LogRecord) {
        lock.write {
            if (cache.size > 1000)
                cache.removeAt(0)
            cache += record
        }
    }

    fun processRecords() {
        val recordsToSend = arrayListOf<LogRecord>()
        lock.read { recordsToSend.addAll(cache) }
        for (record in recordsToSend) {
            sendLogRecord(record)
        }
    }

    fun sendLogRecord(any: LogRecord) {
        val outputStream = ByteArrayOutputStream()
        ObjectOutputStream(outputStream).writeObject(any)
        url.httpPost()
                .header("Content-Type" to "application/octet-stream")
                .body(outputStream.toByteArray())
                .response { request, response, either ->
                    when (response.httpStatusCode) {
                        200 -> lock.write { cache.remove(any) }
                    }
                }
    }

    override fun flush() {
        // NOOP
    }

    override fun close() {
        // NOOP
    }
}
