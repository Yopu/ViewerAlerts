package vieweralerts

import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.RadioButton
import javafx.scene.control.Spinner
import javafx.scene.control.SpinnerValueFactory
import javafx.scene.control.TextField
import javafx.stage.Stage
import java.net.URL
import java.util.*

class SettingsController : Initializable {

    var okayClicked = false

    val channel: String
        get() = channelTextField.text.toLowerCase()

    val sleepDuration: Int
        get() = delaySpinner.value

    val remoteURL: String?
        get() = if (urlTextField.text.isNotBlank()) urlTextField.text else null

    val remoteLoggingEnabled: Boolean
        get() = urlRadioButton.isSelected


    lateinit var stage: Stage
    @FXML lateinit var channelTextField: TextField
    @FXML lateinit var urlTextField: TextField
    @FXML lateinit var urlRadioButton: RadioButton
    @FXML lateinit var delaySpinner: Spinner<Int>

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        delaySpinner.valueFactory = SpinnerValueFactory.IntegerSpinnerValueFactory(0, 60000, 10000, 500)
        urlTextField.textProperty().addListener({ observable, old, new ->
            urlRadioButton.isSelected = new.isNotBlank()
        })
    }

    fun cancelClicked() = stage.close()
    fun okayClicked() {
        okayClicked = true
        stage.close()
    }
}
