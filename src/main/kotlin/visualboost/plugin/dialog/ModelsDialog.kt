package visualboost.plugin.dialog

import com.intellij.openapi.observable.util.whenItemSelected
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.AnimatedIcon
import kotlinx.coroutines.*
import visualboost.plugin.api.API
import visualboost.plugin.api.models.model.AllModelsResponseBody
import visualboost.plugin.settings.VbAppSettings
import visualboost.plugin.settings.VbProjectSettings
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import kotlin.coroutines.CoroutineContext


class ModelsDialog(val project: Project, val jwt: String) : DialogWrapper(true), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Default + SupervisorJob()

    val comboBox = ComboBox<String>(arrayOf())
    val loadingLabel = JLabel("Load Models...", AnimatedIcon.Default(), SwingConstants.LEFT)
    val models: MutableList<AllModelsResponseBody.Data.Model> = mutableListOf()
    var selectedModel: AllModelsResponseBody.Data.Model? = null

    init {
        title = "Select Model"
        init()

        launch {
            loadModels()
        }

    }

    private suspend fun loadModels(){
        try{
            loadingLabel.isVisible = true
            comboBox.isVisible = false
            val settings = VbProjectSettings.getInstance(project)

            val models = withContext(Dispatchers.IO){
                return@withContext API.getModels(jwt, settings.projectId ?: return@withContext emptyList())
            }

            setModelsToCombobox(models.sortedByDescending { it.name })
        }finally {
            loadingLabel.isVisible = false
            comboBox.isVisible = true
        }

    }

    private fun setModelsToCombobox(models: List<AllModelsResponseBody.Data.Model>) {
        this.models.addAll(models)
        this.models.forEach{
            this.comboBox.addItem(it.name)
        }

        this.comboBox.updateUI()
    }

    override fun createCenterPanel(): JComponent {
        val dialogPanel = JPanel(BorderLayout())

        dialogPanel.add(loadingLabel)
        dialogPanel.add(comboBox, BorderLayout.CENTER)

        comboBox.whenItemSelected<String> {
            selectedModel = models[comboBox.selectedIndex]
        }

        return dialogPanel
    }




}