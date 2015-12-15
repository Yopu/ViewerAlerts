package vieweralerts

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import javafx.application.Platform
import javafx.collections.ObservableList
import java.net.URL
import kotlin.concurrent.thread

fun startLooperThread(channel: String, observableList: ObservableList<String>, sleepDuration: Int): Thread {
    val thread = thread(isDaemon = true) {
        while (!Thread.interrupted()) {
            try {
                val updatedUserList = downloadUsers(channel)

                val newUsers = updatedUserList.filterNot { observableList.contains(it) }.toList()
                val removedUsers = observableList.filterNot { updatedUserList.contains(it) }.toList()
                log.info("new: ${newUsers.size} removed: ${removedUsers.size}")
                Platform.runLater {
                    observableList.addAll(newUsers)
                    observableList.removeAll(removedUsers)
                }

                Thread.sleep(sleepDuration.toLong())
            } catch (e: InterruptedException) {
                break
            } catch (e: TypeCastException) {
                continue
            }
        }
    }
    return thread
}

fun downloadUsers(channel: String): List<String> {
    val url = URL("http://tmi.twitch.tv/group/user/$channel/chatters")
    val parsed = Parser().parse(url.openStream()) as JsonObject
    val chatters = parsed["chatters"] as JsonObject
    return chatters.values.flatMap { it as JsonArray<*> }.map { it as String }
}