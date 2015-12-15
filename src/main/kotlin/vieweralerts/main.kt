package vieweralerts

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import javafx.application.Application
import javafx.application.Application.launch
import javafx.application.Platform.runLater
import javafx.collections.ObservableList
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.layout.VBox
import javafx.stage.Stage
import java.net.URL
import java.time.Duration
import kotlin.concurrent.thread

val CHANNEL = "sheevergaming"
val SLEEP_DURATION = Duration.ofSeconds(10)

fun main(args: Array<String>) = launch(DisplayApplication::class.java, *args)

class DisplayApplication : Application() {
    override fun start(primaryStage: Stage) {
        val fxmlLoader = FXMLLoader(ClassLoader.getSystemResource("main.fxml"))
        val vBox = fxmlLoader.load<VBox>()

        val controller = fxmlLoader.getController<Controller>()
        startLooperThread(CHANNEL, controller.allUsersList, SLEEP_DURATION)

        primaryStage.scene = Scene(vBox)
        primaryStage.show()
    }
}

fun startLooperThread(channel: String, observableList: ObservableList<String>, sleepDuration: Duration): Thread {
    val thread = thread(isDaemon = true) {
        while (true) {
            val updatedUserList = downloadUsers(channel)

            val newUsers = updatedUserList.filterNot { observableList.contains(it) }.toList()
            val removedUsers = observableList.filterNot { updatedUserList.contains(it) }.toList()
            println("new: ${newUsers.size} removed: ${removedUsers.size}")
            runLater {
                observableList.addAll(newUsers)
                observableList.removeAll(removedUsers)
            }

            Thread.sleep(sleepDuration.toMillis())
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
