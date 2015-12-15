package vieweralerts

import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.error.YAMLException
import org.yaml.snakeyaml.nodes.Tag
import java.io.File
import java.io.FileNotFoundException

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
        val raw = Yaml().load(File(path).readText()) as Map<*, *>
        val channel = raw["channel"] as String
        val sleepDuration = raw["sleep_duration"] as Int
        return Settings(channel, sleepDuration)
    } catch (e: Exception) {
        if (e is FileNotFoundException || e is YAMLException || e is ClassCastException)
            log.warning("Failed to load settings: $e")
        else
            throw e
    }
    return null
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
