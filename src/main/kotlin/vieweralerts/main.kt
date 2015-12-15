package vieweralerts

import javafx.application.Application
import javafx.application.Application.launch
import javafx.beans.property.StringProperty
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.layout.VBox
import javafx.stage.Stage
import java.util.logging.Logger

val SETTINGS_PATH = "settings.yaml"

val log = Logger.getLogger("display_application")

fun main(args: Array<String>) = launch(DisplayApplication::class.java, *args)

fun loader(name: String) = FXMLLoader(ClassLoader.getSystemResource(name))

class DisplayApplication : Application(), SettingsHandler {

    lateinit var mainController: Controller
    lateinit var primaryTitle: StringProperty

    override fun start(primaryStage: Stage) {
        primaryTitle = primaryStage.titleProperty()
        primaryTitle.value = "Viewer Alerts"

        val settings = loadSettings(SETTINGS_PATH) ?: promptForSettings()

        val fxmlLoader = loader("main.fxml")
        val vBox = fxmlLoader.load<VBox>()

        mainController = fxmlLoader.getController<Controller>()
        mainController.settingsHandler = this
        handleSettings(settings)

        primaryStage.scene = Scene(vBox)
        primaryStage.show()
    }

    var runningThread: Thread? = null
    override fun handleSettings(settings: Settings) {
        runningThread?.interrupt()
        mainController.allUsersList.clear()
        primaryTitle.value = "Viewer Alerts - ${settings.channel}"
        runningThread = startLooperThread(settings.channel, mainController.allUsersList, settings.sleepDuration)
    }
}

interface SettingsHandler {
    fun handleSettings(settings: Settings)
}
