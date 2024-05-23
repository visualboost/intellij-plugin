package visualboost.plugin.settings

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.components.JBTextField
import com.intellij.util.io.URLUtil
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.UIUtil
import org.jdesktop.swingx.HorizontalLayout
import visualboost.plugin.models.GenerationTarget
import java.awt.Font
import java.util.function.Supplier
import javax.swing.JPanel
import javax.swing.JProgressBar


class VbPluginSettingsComponent(val project: Project) {

    private val myMainPanel: JPanel
    private val urlTextField = initUrlTextField()
    private val projectIdTextField = initProjectIdTextField()
    private val targetGroup = initTargetSelectField()

    init {
        val projectIdDescription = JBLabel(
            "<html>The projectId is necessary to pull your changes (made via VisualBoost) directly into you local git repository.<br/>" +
                    "It's also necessary to synchronize your local routes with VisualBoost.</html>",
            UIUtil.ComponentStyle.SMALL
        )
        projectIdDescription.font = Font(projectIdDescription.font.name, Font.ITALIC, projectIdDescription.font.size)

        val targetDescription = JBLabel(
            "<html>Select 'Client' if this project should contain your client code.Select 'Backend' if this project should contain your backend application.</html>",
            UIUtil.ComponentStyle.SMALL
        )
        targetDescription.font = Font(targetDescription.font.name, Font.ITALIC, targetDescription.font.size)

        myMainPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(getUrlTitle(), urlTextField, 1, true)
            .addVerticalGap(15)
            .addLabeledComponent(getProjectIdTitle(), projectIdTextField, 1, true)
            .addComponent(projectIdDescription)
            .addVerticalGap(15)
            .addLabeledComponent(
                getTargetTitle(),
                targetGroup,
                1,
                true
            )
            .addComponent(targetDescription)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    private fun getUrlTitle(): JBLabel {
        val urlTitle = JBLabel("URL of VisualBoost:")
        urlTitle.font = Font(urlTitle.font.name, Font.BOLD, urlTitle.font.size)
        return urlTitle
    }

    private fun getProjectIdTitle(): JBLabel {
        val urlTitle = JBLabel("Project ID:")
        urlTitle.font = Font(urlTitle.font.name, Font.BOLD, urlTitle.font.size)
        return urlTitle
    }

    private fun getTargetTitle(): JBLabel {
        val urlTitle = JBLabel("Project Type:")
        urlTitle.font = Font(urlTitle.font.name, Font.BOLD, urlTitle.font.size)
        return urlTitle
    }

    fun getPanel(): JPanel {
        return myMainPanel
    }

    fun getUrl(): String {
        return urlTextField.text
    }

    fun isUrlValid(): Boolean {
        val url = getUrl()
        return url.startsWith("http://") || url.startsWith("https://") && URLUtil.URL_PATTERN.matcher(url).matches()
    }

    fun setUrl(url: String) {
        urlTextField.text = url
    }

    fun getProjectId(): String {
        return projectIdTextField.text
    }

    fun setProjectId(projectId: String) {
        projectIdTextField.text = projectId
    }

    fun setTarget(target: GenerationTarget?) {
        if (target == null) return

        val rbToSelect = targetGroup.components.find { it.name == target.value } as JBRadioButton
        val rbToUnselect = targetGroup.components.find { it.name != target.value } as JBRadioButton

        rbToSelect.isSelected = true
        rbToUnselect.isSelected = false
    }

    fun getTarget(): GenerationTarget? {
        val selectedRadioButton =
            targetGroup.components.find { (it as JBRadioButton).isSelected } as? JBRadioButton ?: return null
        return GenerationTarget.fromValue(selectedRadioButton.name)
    }


    private fun initTargetSelectField(): JPanel {
        val targetButtonGroup = JPanel(HorizontalLayout(10))

        val targetClientRB = JBRadioButton("Client")
        targetClientRB.name = GenerationTarget.CLIENT.value

        val targetBackendRB = JBRadioButton("Backend")
        targetBackendRB.name = GenerationTarget.SERVER.value


        targetBackendRB.addItemListener {
            targetClientRB.isSelected = !targetBackendRB.isSelected
        }
        targetClientRB.addItemListener {
            targetBackendRB.isSelected = !targetClientRB.isSelected
        }

        targetButtonGroup.add(targetClientRB)
        targetButtonGroup.add(targetBackendRB)

        return targetButtonGroup
    }

    private fun initUrlTextField(): JBTextField {
        val urlTextField = JBTextField()
        urlTextField.toolTipText =
            "The url of your VisualBoost-Instance. The default value is '${VbPluginSettingsState.DEFAULT_URL}'"

        ComponentValidator(project).withValidator(Supplier<ValidationInfo> {
            val url = getUrl()

            if (url.isEmpty()) {
                return@Supplier ValidationInfo("Empty url is not allowed", urlTextField)
            }

            if (!isUrlValid()) {
                return@Supplier ValidationInfo("Invalid url", urlTextField)
            }

            null
        }).installOn(urlTextField)

        urlTextField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: javax.swing.event.DocumentEvent) {
                ComponentValidator.getInstance(urlTextField).ifPresent { v: ComponentValidator -> v.revalidate() }
            }
        })

        return urlTextField
    }

    private fun initCredentialsComponent(): JPanel {
        val email = JBTextField()
        val passwordTextField = JBPasswordField()
        val progressBar = JProgressBar()
        progressBar.isIndeterminate = true

        return FormBuilder.createFormBuilder()
            .addComponent(JBLabel("VisualBoost Credentials:"))
            .addLabeledComponent(JBLabel("E-Mail:"), email, 1, false)
            .addLabeledComponent(JBLabel("Password:"), passwordTextField, 1, false)
            .addComponent(progressBar)
            .panel
    }

    private fun initProjectSelection(): JPanel {
        val projects = listOf<String>("MyProject", "MySecondProject")
        val projectsComboBox = ComboBox<String>(projects.toTypedArray())
        projectsComboBox.item

        return FormBuilder.createFormBuilder()
            .addComponent(JBLabel("Project:"))
            .addComponent(projectsComboBox)
            .panel
    }


    private fun initProjectIdTextField(): JBTextField {
        val projectIdTextField = JBTextField()
        projectIdTextField.toolTipText =
            "The id of your VisualBoost project"

        ComponentValidator(project).withValidator(Supplier<ValidationInfo> {
            val projectId = getProjectId()

            if (projectId.isEmpty()) {
                return@Supplier ValidationInfo("Empty projectId is not allowed", projectIdTextField)
            }

            null
        }).installOn(projectIdTextField)

        projectIdTextField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: javax.swing.event.DocumentEvent) {
                ComponentValidator.getInstance(projectIdTextField).ifPresent { v: ComponentValidator -> v.revalidate() }
            }
        })

        return projectIdTextField
    }

}