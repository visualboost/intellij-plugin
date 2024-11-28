package visualboost.plugin.intellijProject

import addTextChangeListener
import com.intellij.ide.util.projectWizard.SettingsStep
import com.intellij.ide.util.projectWizard.WebProjectTemplate
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.util.ProgressWindow
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ProjectGeneratorPeer
import com.intellij.ui.TitledSeparator
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import kotlinx.coroutines.*
import visualboost.plugin.api.API
import visualboost.plugin.api.models.JwtContent
import visualboost.plugin.api.models.exception.HttpException
import visualboost.plugin.api.models.intellij.project.github.UserData
import visualboost.plugin.components.GithubComponent
import visualboost.plugin.components.LoginComponent
import visualboost.plugin.icons.IconRes.VBNotificationLogo
import visualboost.plugin.tasks.impl.project.queue.CreateNewVBProjectQueue
import visualboost.plugin.util.isVersionOrLower
import visualboost.plugin.util.runCommand
import javax.swing.BoxLayout
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel
import kotlin.coroutines.CoroutineContext


class VBProjectGenerator : WebProjectTemplate<VBProjectGenerationSettings>(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Default + SupervisorJob()

    lateinit var vbSettings: VBProjectGenerationSettings
    var stateListener: ProjectGeneratorPeer.SettingsListener? = null

    lateinit var projectNameTextField: JBTextField

    var mainComponent: JPanel? = null

    lateinit var loginComponent: LoginComponent
    lateinit var githubPanel: GithubComponent

    override fun getName(): String {
        return "VisualBoost"
    }

    override fun getDescription(): String? {
        return "Create a new VisualBoost Project"
    }

    private fun initProjectGeneratorPeer(): ProjectGeneratorPeer<VBProjectGenerationSettings> {
        val peer = object : ProjectGeneratorPeer<VBProjectGenerationSettings> {

            override fun validate(): ValidationInfo? {
                updateUI()

                if (vbSettings.projectName.isNullOrBlank()) {
                    return ValidationInfo("Enter a valid project name").asWarning()
                }

                if (vbSettings.userData == null) {
                    return ValidationInfo("Enter your Username and Password and click login").asWarning()
                }

                if (!vbSettings.prerequisites.gitExists) {
                    githubPanel.enableLoginButton(false)
                    return ValidationInfo("Git can't be found on your system. Please install Git first.").asWarning()
                }

                val githubData = vbSettings.githubData ?: return ValidationInfo("Login into Github").asWarning()
                if (githubData.repoUrlData?.isValid() != true && githubData.repoNameData?.isValid() != true) {
                    return ValidationInfo("Add a new or existing repository").asWarning()
                }

                return null
            }

            override fun getSettings(): VBProjectGenerationSettings {
                return vbSettings
            }

            override fun buildUI(step: SettingsStep) {
                /**
                 * [ProjectGeneratorPeer.getComponent] is not deprecated yet in Intellij Ultimate 2024.2 and lower.
                 */
                if(ApplicationInfo.getInstance().isVersionOrLower("2024", "2")){
                    step.addSettingsComponent(this.component)
                }else {
                    /**
                     * [ProjectGeneratorPeer.getComponent] is deprecated in Intellij Ultimate 2024.3 and higher.
                     */
                    vbSettings = VBProjectGenerationSettings()
                    mainComponent = FormBuilder.createFormBuilder()
                        .addComponent(initProjectNamePanel())
                        .addComponent(initLoginPanel())
                        .addComponent(initGithubSection())
                        .panel

                    loginComponent.enableInputForms(false)
                    step.addSettingsComponent(mainComponent as JComponent)
                }
            }

            @Deprecated("Deprecated since 2024.3.*")
            override fun getComponent(): JComponent {
                //This function won't be called in ide version > 2024.3.*
                vbSettings = VBProjectGenerationSettings()
                mainComponent = FormBuilder.createFormBuilder()
                    .addComponent(initProjectNamePanel())
                    .addComponent(initLoginPanel())
                    .addComponent(initGithubSection())
                    .panel

                loginComponent.enableInputForms(false)
                return mainComponent!!
            }

            private fun initProjectNamePanel(): JPanel {
                projectNameTextField = JBTextField()

                projectNameTextField.addTextChangeListener {
                    validateAndSetProjectName(it)
                }

                return FormBuilder.createFormBuilder()
                    .addLabeledComponent("Name:*   ", projectNameTextField, 1, false)
                    .panel
            }

            override fun isBackgroundJobRunning(): Boolean {
                return false
            }

            private fun validateAndSetProjectName(projectName: String) {
                val projectNameIsValid = projectNameIsValid(projectName)
                if (!projectNameIsValid) {
                    loginComponent.enableInputForms(false)
                    vbSettings.projectName = null
                    validateProjectSettings()
                    return
                }

                vbSettings.projectName = projectName
                if (vbSettings.userData == null) {
                    loginComponent.enableInputForms(true)
                }

                validateProjectSettings()
            }

            private fun projectNameIsValid(projectName: String): Boolean {
                return "[\\w-]+".toRegex().matches(projectName)
            }

            private fun setCredentials(userId: String, email: String, password: String, token: String) {
                vbSettings.userData = UserData(userId, email, password, token)

                loginComponent.enableEmailTextField(false)
                loginComponent.enablePasswordTextField(false)
                loginComponent.showSuccessButton()
            }

            private fun updateUI() {
                val showGithubSection = vbSettings.userData != null
                githubPanel.isVisible = showGithubSection

                val enableGithubInputFields = vbSettings.githubData != null
                githubPanel.enableInputFields(enableGithubInputFields)

                mainComponent?.updateUI()
            }

            private fun initLoginPanel(): JPanel {
                val loginPanel = JPanel()
                loginPanel.layout = BoxLayout(loginPanel, BoxLayout.Y_AXIS)

                loginComponent = LoginComponent().showHeader(false)

                loginComponent.onLoginButtonClicked {
                    launch {
                        try {
                            val loginResponseBody = withContext(Dispatchers.IO) {
                                API.login(loginComponent.getEmail(), loginComponent.getPassword())
                            }
                            loginComponent.showError(null)

                            val userId = JwtContent.fromJwt(loginResponseBody.token).userId
                            setCredentials(
                                userId,
                                loginComponent.getEmail(),
                                loginComponent.getPassword(),
                                loginResponseBody.token
                            )

                            vbSettings.prerequisites.gitExists = checkIfGitExists()
                            validateProjectSettings()
                        } catch (e: HttpException) {
                            loginComponent.showError(e.message)
                        }
                    }
                }

                loginComponent.onEmailChanged = {
                    if (it.isBlank()) {
                        loginComponent.enableButton(false)
                    } else {
                        loginComponent.enableButton(true)
                    }
                }

                loginComponent.onPasswordChanged = {
                    if (it.isBlank()) {
                        loginComponent.enableButton(false)
                    } else {
                        loginComponent.enableButton(true)
                    }
                }

                loginPanel.add(TitledSeparator("VisualBoost"))
                loginPanel.add(loginComponent)
                return loginPanel
            }

            private fun checkIfGitExists(): Boolean {
                return try {
                    "git --version".runCommand()
                    true
                } catch (e: Exception) {
                    false
                }
            }

            private fun initGithubSection(): JPanel {
                githubPanel = GithubComponent(vbSettings)
                githubPanel.isVisible = false
                githubPanel.enableInputFields(vbSettings.githubData != null)

                githubPanel.onLoginSuccess = {
                    vbSettings.githubData = it
                    validateProjectSettings()
                    githubPanel.selectTab(0)
                    githubPanel.focusBackendTextfield()

                    launch {
                        val repositories = withContext(Dispatchers.IO) {
                            val jwt = vbSettings.userData?.token ?: return@withContext emptyList()
                            val githubData = vbSettings.githubData ?: return@withContext emptyList()
                            API.Github.listGithubRepositories(jwt, githubData.githubAccessToken)
                        }

                        githubPanel.repositories = repositories
                        githubPanel.showSuccessButton()
                    }
                }

                githubPanel.onRepositoryNamesChanged = onRepositoryNamesChanged@{ repoName ->
                    vbSettings.setRepoName(repoName)
                    validateProjectSettings()
                }

                githubPanel.onRepositoryUrlChanged = onRepositoryUrlChanged@{ backendRepo ->
                    vbSettings.setExistingGithubRepo(backendRepo)
                    validateProjectSettings()
                }


                return githubPanel
            }


            /**
             * SetS the listener that can be used to execute [ProjectGeneratorPeer.SettingsListener.stateChanged].
             * If [ProjectGeneratorPeer.SettingsListener.stateChanged] is executed, the [ProjectGeneratorPeer.validate] will be triggered.
             *
             * Prerequisites: The current class needs to extend [WebProjectTemplate]
             */
            override fun addSettingsListener(listener: ProjectGeneratorPeer.SettingsListener) {
                stateListener = listener
            }
        }

        return peer
    }

    override fun createPeer(): ProjectGeneratorPeer<VBProjectGenerationSettings> {
        return initProjectGeneratorPeer()
    }

    override fun getLogo(): Icon {
        return VBNotificationLogo
    }

    override fun generateProject(project: Project, p1: VirtualFile, settings: VBProjectGenerationSettings, p3: Module) {

        val backgroundTask = object : Task.Backgroundable(project, "Synchronizing data", true) {
            override fun run(indicator: ProgressIndicator) {
                val createNewVBProjectQueue = CreateNewVBProjectQueue(
                    project,
                    indicator,
                    settings,
                    githubPanel.createRepositoryTabIsSelected(),
                    githubPanel.connectExistingRepositoryTabIsSelected()
                )
                createNewVBProjectQueue.execute()
            }
        }

        val progressWindow = ProgressWindow(false, true, project)
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(backgroundTask, progressWindow)
    }

    private fun validateProjectSettings() {
        stateListener?.stateChanged(true)
    }


}