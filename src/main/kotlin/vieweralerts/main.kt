package vieweralerts

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import javafx.application.Application
import javafx.application.Application.launch
import javafx.application.Platform
import javafx.collections.ObservableList
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.layout.VBox
import javafx.stage.Stage
import java.net.URL
import kotlin.concurrent.thread

val CHANNEL = "sheevergaming"

fun main(args: Array<String>) = launch(DisplayApplication::class.java, *args)

class DisplayApplication : Application() {
    override fun start(primaryStage: Stage) {
        val fxmlLoader = FXMLLoader(ClassLoader.getSystemResource("main.fxml"))
        val vBox = fxmlLoader.load<VBox>()

        val controller = fxmlLoader.getController<Controller>()
        startLooperThread(CHANNEL, controller.allUsersList, 10000)

        primaryStage.scene = Scene(vBox)
        primaryStage.show()
    }
}

fun startLooperThread(channel: String, observableList: ObservableList<String>, sleepDuration: Long): Thread {
    val thread = thread(isDaemon = true) {
        while (true) {
            val newUsers = downloadUsers(channel).filterNot { observableList.contains(it) }.toList()
            Platform.runLater { observableList.addAll(newUsers) }
            Thread.sleep(sleepDuration)
        }
    }
    return thread
}

fun downloadUsers(channel: String): List<String> {
    val url = URL("http://tmi.twitch.tv/group/user/$channel/chatters")
    val parsed = Parser().parse(url.openStream()) as JsonObject
    val chatters = parsed["chatters"] as JsonObject
    return chatters.values.flatMap { it as JsonArray<String> }
}
