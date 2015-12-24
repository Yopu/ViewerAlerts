package vieweralerts

import javafx.animation.FillTransition
import javafx.application.Platform
import javafx.collections.FXCollections.observableArrayList
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.util.Duration
import java.net.URL
import java.util.*

class Controller : Initializable {

    lateinit var settingsHandler: SettingsHandler

    val allUsersList = observableArrayList<String>()
    val newUsersList = observableArrayList<String>()

    @FXML lateinit var allUsersListView: ListView<String>
    @FXML lateinit var allUsersCountLabel: Label

    @FXML lateinit var newUsersListView: ListView<String>
    @FXML lateinit var newUsersCountLabel: Label


    @FXML lateinit var clearButton: Button
    @FXML lateinit var alertRectangle: Rectangle
    var transition: FillTransition? = null

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        allUsersListView.items = allUsersList
        newUsersListView.items = newUsersList

        allUsersList.addSizeLabel(allUsersCountLabel)
        newUsersList.addSizeLabel(newUsersCountLabel)

        alertRectangle.heightProperty().bind(clearButton.heightProperty())
        alertRectangle.widthProperty().bind(clearButton.widthProperty())


        allUsersList.addListener(ListChangeListener {
            while (it.next()) {
                if (it.wasAdded())
                    newUsersList.addAll(it.addedSubList)
                if (it.wasRemoved())
                    newUsersList.removeAll(it.removed)
            }
        })

        newUsersList.addListener(ListChangeListener {
            var added = false
            while (it.next()) {
                if (it.wasAdded())
                    added = true
            }
            if (added)
                flashAlert()
            if (newUsersList.isEmpty())
                stopFlashing()
        })
    }

    fun closeMenuPressed() {
        Platform.exit()
    }

    fun settingsMenuPressed() {
        try {
            val settings = promptForSettings()
            settingsHandler.handleSettings(settings)
        } catch (e: SettingsCancelException) {
        }
    }

    fun clearButtonPressed() {
        newUsersList.clear()
        stopFlashing()
    }

    fun flashAlert() {
        alertRectangle.isVisible = true

        transition = FillTransition(Duration.seconds(0.5), alertRectangle, Color.TRANSPARENT, Color.RED).apply {
            cycleCount = FillTransition.INDEFINITE
            isAutoReverse = true
            play()
        }
    }

    fun stopFlashing() {
        alertRectangle.isVisible = false
        transition?.stop()
    }

    fun <T> ObservableList<T>.addSizeLabel(label: Label) {
        label.text = size.toString()
        addListener(ListChangeListener {
            label.text = size.toString()
        })
    }
}
