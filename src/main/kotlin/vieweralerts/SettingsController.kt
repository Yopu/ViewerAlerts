package vieweralerts

import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Spinner
import javafx.scene.control.SpinnerValueFactory
import javafx.scene.control.TextField
import javafx.stage.Stage
import java.net.URL
import java.util.*

class SettingsController : Initializable {

    var okayClicked = false

    lateinit var stage: Stage
    @FXML lateinit var channelTextField: TextField
    @FXML lateinit var delaySpinner: Spinner<Int>

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        delaySpinner.valueFactory = SpinnerValueFactory.IntegerSpinnerValueFactory(0, 60000, 10000, 500)
    }

    fun cancelClicked(event: ActionEvent?) = stage.close()
    fun okayClicked(event: ActionEvent?) {
        okayClicked = true
        stage.close()
    }
}
