package visualboost.plugin.settings

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.impl.CollapsibleTitledSeparatorImpl
import com.intellij.util.io.URLUtil
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.*
import org.jdesktop.swingx.HorizontalLayout
import visualboost.plugin.api.API
import visualboost.plugin.api.models.AllProjectsResponseBody
import visualboost.plugin.components.LoginComponent
import visualboost.plugin.components.SelectProjectDropdownComponent
import visualboost.plugin.models.GenerationTarget
import visualboost.plugin.util.CredentialUtil
import java.awt.Font
import javax.swing.*


class VbPluginSettingsComponent(val project: Project) : CoroutineScope {

    override val coroutineContext = Dispatchers.Default + SupervisorJob()

    private val appSettings = VbAppSettings.getInstance()
    private val projectSettings = VbProjectSettings.getInstance(project)

    private val myMainPanel: JPanel
    private val targetGroup = initTargetSelectField()

    private lateinit var loginPanel: LoginComponent

    private lateinit var logoutPanel: JPanel
    private val logoutButton = JButton("Logout")
    private lateinit var authLabel: JLabel

    lateinit var projectSelectionPanel: SelectProjectDropdownComponent

    private val defaultUrlTextField = JBTextField()
    private val mainUrlTextField = JBTextField()
    private val authUrlTextField = JBTextField()
    val adaptUrlCheckbox = JBCheckBox("Connect to custom VisualBoost instance")

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

        val loginPanel = initCredentialsComponent()
        val logoutPanel = initLogoutComponent()
        handleCredentialFormsVisibility()

        val advancedForm = initAdvancedComponent()

        myMainPanel = FormBuilder.createFormBuilder()
            .addComponent(loginPanel)
            .addComponent(logoutPanel)
            .addVerticalGap(15)
            .addLabeledComponent(
                getTargetTitle(),
                targetGroup,
                1,
                true
            )
            .addComponent(targetDescription)
            .addComponent(advancedForm)
            .addComponentFillVertically(JPanel(), 0)
            .panel


    }

    private suspend fun login() {
        try {
            loginPanel.load(true)

            val loginResponse = withContext(Dispatchers.IO) {
                API.login(getEmail(), getPassword())
            }

            CredentialUtil.storeVisualBoostCredentials(getEmail(), getPassword())
            handleCredentialFormsVisibility()
        } finally {
            loginPanel.load(false)
        }
    }

    private fun getTargetTitle(): JBLabel {
        val urlTitle = JBLabel("Project Type:")
        urlTitle.font = Font(urlTitle.font.name, Font.BOLD, urlTitle.font.size)
        return urlTitle
    }

    fun getPanel(): JPanel {
        return myMainPanel
    }

    fun isUrlValid(url: String): Boolean {
        return url.startsWith("http://") || url.startsWith("https://") && URLUtil.URL_PATTERN.matcher(url).matches()
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

    private fun initCredentialsComponent(): JPanel {
        loginPanel = LoginComponent()

        launch {
            loadVbCredentials()
        }

        loginPanel.onLoginButtonClicked {
            launch {
                login()
                projectSelectionPanel.loadProjects()
                addUsernameToLogoutPanel()
            }
        }
        return loginPanel
    }

    private suspend fun loadVbCredentials(){
        val credentials = withContext(Dispatchers.IO){
            CredentialUtil.getVBCredentials() ?: return@withContext null
        } ?: return

        withContext(Dispatchers.EDT){
            loginPanel.setEmail(credentials.email)
            loginPanel.setPassword(credentials.password)
        }

    }

    private fun initLogoutComponent(): JPanel {
        logoutButton.addActionListener {
            logout()
        }

        val infoPanel = JPanel()
        infoPanel.layout = BoxLayout(infoPanel, BoxLayout.X_AXIS)

        val successIcon = JLabel(AllIcons.General.InspectionsOK)
        successIcon.insets.right = 20
        infoPanel.add(successIcon)

        this.authLabel = JLabel("")
        addUsernameToLogoutPanel()
        infoPanel.add(authLabel)
        infoPanel.add(Box.createHorizontalGlue())
        infoPanel.add(logoutButton)

        val projectPanel = initProjectsPanel()

        val panel = FormBuilder.createFormBuilder()
            .addComponent(JBLabel("Authentication:"))
            .addComponent(infoPanel)
            .addComponent(projectPanel)
            .panel

        logoutPanel = panel
        return logoutPanel
    }

    private fun logout(){
        launch {
            CredentialUtil.clearCredentials()
            handleCredentialFormsVisibility()
            projectSelectionPanel.clearProjects()
            projectSettings.clearProjectId()
        }
    }

    private fun addUsernameToLogoutPanel() {
        val username = CredentialUtil.getUsername()
        if (username != null) {
            this.authLabel.text = "<html><b>$username</b></html> "
        }
    }

    private fun initProjectsPanel(): JPanel {
        projectSelectionPanel = SelectProjectDropdownComponent(project)
        return projectSelectionPanel
    }

    private fun initAdvancedComponent(): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

        val collapsibleTitledSeparator = CollapsibleTitledSeparatorImpl("Advanced")
        collapsibleTitledSeparator.expanded = false

        val settings = appSettings
        adaptUrlCheckbox.isSelected = !settings.useDefaultUrl

        defaultUrlTextField.text = settings.defaultUrl
        mainUrlTextField.text = settings.mainUrl
        authUrlTextField.text = settings.authUrl
        handleUrlTextFieldState()

        //Create advances input form
        val form = FormBuilder.createFormBuilder()
            .addLabeledComponent(
                "Domain (URL):",
                defaultUrlTextField,
                1,
                false
            )
            .addLabeledComponent(
                "Authentication-Service (URL):",
                authUrlTextField,
                1,
                false
            )
            .addLabeledComponent(
                "Main-Service (URL):",
                mainUrlTextField,
                1,
                false
            )
            .addComponent(adaptUrlCheckbox)
            .panel
        form.isVisible = false

        panel.add(collapsibleTitledSeparator)
        panel.add(form)

        adaptUrlCheckbox.addActionListener {
            handleUrlTextFieldState()
        }

        collapsibleTitledSeparator.onAction {
            form.isVisible = it
        }

        return panel
    }

    fun handleUrlTextFieldState() {
        val isSelected = adaptUrlCheckbox.isSelected

        defaultUrlTextField.isEnabled = isSelected
        mainUrlTextField.isEnabled = isSelected
        authUrlTextField.isEnabled = isSelected
    }

    fun useCustomUrlIsSelected(): Boolean {
        return adaptUrlCheckbox.isSelected
    }

    fun getDefaultUrl(): String {
        return defaultUrlTextField.text
    }

    fun getMainUrl(): String {
        return mainUrlTextField.text
    }


    fun getAuthUrl(): String {
        return authUrlTextField.text
    }

    private fun getEmail(): String {
        return loginPanel.getEmail()
    }

    private fun getPassword(): String {
        return loginPanel.getPassword()
    }

    fun getProject(): AllProjectsResponseBody.Data.Project? {
        return projectSelectionPanel.getSelectedProject()
    }

    /**
     * Displays the login form if the user is not logged in.
     * Displays the logout form if the user is logged in.
     */
    private fun handleCredentialFormsVisibility() {
        launch {
            val userIsLoggedIn = CredentialUtil.isUserLoggedIn()
            loginPanel.isVisible = !userIsLoggedIn
            logoutPanel.isVisible = userIsLoggedIn
        }
    }
}