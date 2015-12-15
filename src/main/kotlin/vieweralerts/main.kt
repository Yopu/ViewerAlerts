package vieweralerts

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import javafx.animation.FillTransition
import javafx.animation.FillTransition.INDEFINITE
import javafx.application.Application
import javafx.application.Application.launch
import javafx.application.Platform.runLater
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ListView
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.stage.Stage
import javafx.util.Duration
import java.net.URL
import kotlin.concurrent.thread

fun main(args: Array<String>) = launch(DisplayApplication::class.java, *args)

class DisplayApplication : Application() {

    private lateinit var rectangle: Rectangle
    private var transition: FillTransition? = null

    override fun start(primaryStage: Stage) {
        val newUsersList = FXCollections.observableArrayList<String>()
        newUsersList.addListener(ListChangeListener {
            var added = false
            while (it.next()) {
                if (it.wasAdded())
                    added = true
            }
            if (added)
                flashAlert()
        })


        val allUsersList = FXCollections.observableArrayList<String>()
        allUsersList.addListener(ListChangeListener {
            while (it.next()) {
                if (it.wasAdded())
                    newUsersList.addAll(it.addedSubList)
            }
        })


        startLooperThread(allUsersList, 10000)


        val allUsersListView = ListView<String>(allUsersList)
        allUsersListView.isFocusTraversable = false


        val newUsersListView = ListView<String>(newUsersList)
        newUsersListView.isFocusTraversable = false


        val clearButton = Button("Clear")
        clearButton.prefWidthProperty().bind(newUsersListView.widthProperty())
        clearButton.setOnAction {
            newUsersList.clear()
            stopFlashing()
        }


        rectangle = Rectangle()
        rectangle.isMouseTransparent = true
        rectangle.isVisible = false
        rectangle.heightProperty().bind(clearButton.heightProperty())
        rectangle.widthProperty().bind(newUsersListView.widthProperty())


        val buttonStackPane = StackPane(clearButton, rectangle)


        val vBox = VBox(newUsersListView, buttonStackPane)
        vBox.maxHeightProperty().bind(allUsersListView.heightProperty())
        val rootHBox = HBox(allUsersListView, vBox)

        primaryStage.scene = Scene(rootHBox)
        primaryStage.isResizable = false
        primaryStage.show()
    }


    fun flashAlert() {
        rectangle.isVisible = true

        transition = FillTransition(Duration.seconds(0.5), rectangle, Color.TRANSPARENT, Color.RED).apply {
            cycleCount = INDEFINITE
            isAutoReverse = true
            play()
        }
    }

    fun stopFlashing() {
        rectangle.isVisible = false
        transition?.stop()
    }
}

fun startLooperThread(observableList: ObservableList<String>, sleepDuration: Long): Thread {
    val thread = thread(isDaemon = true) {
        while (true) {
            val newUsers = downloadUsers().filterNot { observableList.contains(it) }.toList()
            runLater { observableList.addAll(newUsers) }
            Thread.sleep(sleepDuration)
        }
    }
    return thread
}

fun downloadUsers(): List<String> {
    val url = URL("http://tmi.twitch.tv/group/user/mrssteelix/chatters")
    val parsed = Parser().parse(url.openStream()) as JsonObject
    val chatters = parsed["chatters"] as JsonObject
    return chatters.values.flatMap { it as JsonArray<String> }
}


