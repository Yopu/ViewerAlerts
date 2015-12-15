package vieweralerts

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import javafx.application.Application
import javafx.application.Application.launch
import javafx.application.Platform.runLater
import javafx.beans.property.StringProperty
import javafx.collections.ObservableList
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.layout.VBox
import javafx.stage.Stage
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.error.YAMLException
import org.yaml.snakeyaml.nodes.Tag
import java.io.File
import java.io.FileNotFoundException
import java.net.URL
import java.util.logging.Logger
import kotlin.concurrent.thread

val SETTINGS_PATH = "settings.yaml"

val log = Logger.getLogger("display_application")

fun main(args: Array<String>) = launch(DisplayApplication::class.java, *args)

fun loader(name: String) = FXMLLoader(ClassLoader.getSystemResource(name))

var mainController: Controller? = null
var primaryTitle: StringProperty? = null

class DisplayApplication : Application() {
    override fun start(primaryStage: Stage) {
        primaryTitle = primaryStage.titleProperty()
        primaryTitle!!.value = "Viewer Alerts"

        val settings = loadSettings(SETTINGS_PATH) ?: promptForSettings()

        val fxmlLoader = loader("main.fxml")
        val vBox = fxmlLoader.load<VBox>()

        mainController = fxmlLoader.getController<Controller>()
        handleSettings(settings)

        primaryStage.scene = Scene(vBox)
        primaryStage.show()
    }
}

fun promptForSettings(): Settings {
    val settingsStage = Stage()

    val loader = loader("settings.fxml")
    val root = loader.load<Parent>()
    val controller = loader.getController<SettingsController>()
    controller.stage = settingsStage

    settingsStage.scene = Scene(root)
    settingsStage.title = "Settings"
    settingsStage.showAndWait()

    if (controller.okayClicked) {
        val settings = Settings(controller.channelTextField.text, controller.delaySpinner.value)
        settings.save(SETTINGS_PATH)
        return settings
    } else {
        throw SettingsCancelException()
    }
}

fun loadSettings(path: String): Settings? {
    try {
        val raw = Yaml().load(File(path).readText())
        if (raw is Map<*, *>) {
            val channel = raw["channel"] as String
            val sleepDuration = raw["sleep_duration"] as Int
            return Settings(channel, sleepDuration)
        }
    } catch (e: Exception) {
        if (e is FileNotFoundException || e is YAMLException || e is ClassCastException)
            log.warning("Failed to load settings: $e")
        else
            throw e
    }
    return null
}

var runningThread: Thread? = null
fun handleSettings(settings: Settings) {
    if (mainController != null) {
        runningThread?.interrupt()
        mainController!!.allUsersList.clear()
        primaryTitle?.value = "Viewer Alerts - ${settings.channel}"
        runningThread = startLooperThread(settings.channel, mainController!!.allUsersList, settings.sleepDuration)
    }
}

class SettingsCancelException : Exception()

data class Settings(val channel: String, val sleepDuration: Int) {
    fun save(path: String) {
        val dump = Yaml().dumpAs(
                mapOf("channel" to channel, "sleep_duration" to sleepDuration),
                Tag.MAP,
                DumperOptions.FlowStyle.BLOCK
        )
        File(path).writeText(dump)
    }
}

fun startLooperThread(channel: String, observableList: ObservableList<String>, sleepDuration: Int): Thread {
    val thread = thread(isDaemon = true) {
        while (!Thread.interrupted()) {
            try {
                val updatedUserList = downloadUsers(channel)

                val newUsers = updatedUserList.filterNot { observableList.contains(it) }.toList()
                val removedUsers = observableList.filterNot { updatedUserList.contains(it) }.toList()
                log.info("new: ${newUsers.size} removed: ${removedUsers.size}")
                runLater {
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
