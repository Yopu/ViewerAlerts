package vieweralerts

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import javafx.application.Platform.runLater
import javafx.collections.ObservableList
import javafx.scene.control.ProgressBar
import javafx.scene.control.ProgressIndicator.INDETERMINATE_PROGRESS
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

class LooperThread(
        val channel: String,
        val observableList: ObservableList<String>,
        val remoteProgressBar: ProgressBar,
        val sleepDuration: Int
) : Thread() {

    private var restart = AtomicBoolean(false)

    init {
        name = "background_thread"
        isDaemon = true
    }

    override fun run() {
        log.info("Creating new thread")
        while (true) {
            try {
                runLater { remoteProgressBar.progress = INDETERMINATE_PROGRESS }
                val updatedUserList = downloadUsers()
                log.info("Downloaded: $updatedUserList")

                val newUsers = updatedUserList.filterNot { observableList.contains(it) }.toList()
                val removedUsers = observableList.filterNot { updatedUserList.contains(it) }.toList()
                log.info("new: ${newUsers.size} removed: ${removedUsers.size}")
                runLater {
                    observableList.addAll(newUsers)
                    observableList.removeAll(removedUsers)
                }

                runLater { remoteProgressBar.progress = 0.0 }
                Thread.sleep(sleepDuration.toLong())
            } catch (e: InterruptedException) {
                log.info("Caught interruption $e")
                if (restart.get()) {
                    log.info("restart triggered")
                    restart.set(false)
                } else {
                    log.warning("thread killed")
                    break
                }
            } catch (e: Exception) {
                log.info("Caught $e")
                continue
            }
        }
        log.info("Thread finished")
    }

    fun downloadUsers(): List<String> {
        val url = URL("http://tmi.twitch.tv/group/user/$channel/chatters")
        val parsed = Parser().parse(url.openStream()) as JsonObject
        val chatters = parsed["chatters"] as JsonObject
        return chatters.values.flatMap { it as JsonArray<*> }.map { it as String }
    }

    fun restart() {
        restart.set(true)
        interrupt()
    }
}
