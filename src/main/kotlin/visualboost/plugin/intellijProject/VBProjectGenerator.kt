package visualboost.plugin.intellijProject

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.impl.ProjectUtil
import com.intellij.ide.util.projectWizard.SettingsStep
import com.intellij.ide.util.projectWizard.WebProjectTemplate
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.module.Module
import com.intellij.openapi.observable.util.whenTextChanged
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.util.ProgressWindow
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.platform.ProjectGeneratorPeer
import com.intellij.ui.TitledSeparator
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import create
import kotlinx.coroutines.*
import setLineSeparatorToLF
import visualboost.plugin.api.API
import visualboost.plugin.api.models.JwtContent
import visualboost.plugin.api.models.exception.HttpException
import visualboost.plugin.api.models.intellij.project.github.GithubRepository
import visualboost.plugin.api.models.intellij.project.github.UserData
import visualboost.plugin.api.models.project.VisualBoostProjectResponseBody
import visualboost.plugin.api.models.project.git.GitRepositoryType
import visualboost.plugin.components.GithubComponent
import visualboost.plugin.components.LoginComponent
import visualboost.plugin.dialog.InitProjectDialog.Companion.MONGO_REPLICA_KEY_FILE
import visualboost.plugin.dialog.ProjectSetupDialog
import visualboost.plugin.icons.IconRes.VBNotificationLogo
import visualboost.plugin.models.GenerationTarget
import visualboost.plugin.settings.VbProjectSettings
import visualboost.plugin.util.*
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import javax.swing.*
import kotlin.coroutines.CoroutineContext
import kotlin.io.path.absolutePathString


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

            override fun buildUI(step: SettingsStep) {
                step.addSettingsComponent(this.component)
            }

            override fun getSettings(): VBProjectGenerationSettings {
                return vbSettings
            }

            override fun getComponent(): JComponent {
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

                projectNameTextField.whenTextChanged {
                    validateAndSetProjectName(projectNameTextField.text)
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

                runBlocking {
                    try {
                        //Step 1: Create new VB Project
                        val newVbProject = createVBProject(settings, indicator)

                        if (settings.githubData == null) return@runBlocking

                        //Step 2: Create a new github reppository or choose an existing one
                        val repoNameData = settings.githubData?.repoNameData
                        val repoUrlData = settings.githubData?.repoUrlData

                        val githubRepositoryToConnect =
                            if (repoNameData != null && repoNameData.isValid() && githubPanel.createRepositoryTabIsSelected()) {
                                createNewGithubRepository(settings, indicator)
                            } else if (repoUrlData != null && repoUrlData.isValid() && githubPanel.connectExistingRepositoryTabIsSelected()) {
                                settings.getGithubRepoOrThrowException()
                            } else {
                                null
                            }

                        if (githubRepositoryToConnect == null) {
                            //TODO: show error
                            return@runBlocking
                        }

                        //Step 3: Connect Github repo with Visualboost project
                        val connectedGithubRepo = connectGithubRepositoriesWithVisualBoost(
                            githubRepositoryToConnect,
                            settings,
                            newVbProject._id,
                            indicator
                        )

                        if (connectedGithubRepo == null) {
                            //TODO: show error
                            return@runBlocking
                        }

                        //Step 4: Start init build
                        runInitBuild(settings, newVbProject._id, indicator)

                        //Step 5: Clone repository
                        checkoutRemoteRepository(project, connectedGithubRepo, settings, indicator)

                        //Step 6: Init Environment variables
                        initEnvironmentVariables(project, newVbProject._id, settings, indicator)

                        //Step 8: Create replica key file
                        createReplicaSetKey(project, settings, indicator)

                        //Step 9: Set lines separators
                        setLinuxLineSeparatorsForInitScripts(project, settings, indicator)

                        //Step 10: install npm dependencies
                        installNpmDependencies(project, settings, indicator)

                        val newWebstormProject = createNewWebstormProjectForVbProject(project, settings, indicator)
                        if (newWebstormProject == null) {
//                            TODO: show error
                            return@runBlocking
                        }

                        //Step 11: Open project
//                        val openedProject = openProject(project, settings, indicator) ?: return@runBlocking

                        //Step 12: Store credentials for project
                        setSettings(settings, newWebstormProject, newVbProject)

//                        Step 13: Create Configurations
                        newWebstormProject.createNpmRunConfig(project.getStartApplicationOnceRunConfigName(), "start")
                        newWebstormProject.createNpmRunConfig(
                            project.getStartApplicationInDevModeRunConfigName(),
                            "dev"
                        )
                        newWebstormProject.createDockerConfiguration()

                        //Step 13: Open Toolwindow
                        openVbToolWindow(newWebstormProject)

                        // Close current project
                        clearOldIdeaDir(project)
                        withContext(Dispatchers.EDT) {
                            ProjectManager.getInstance().closeAndDispose(project)
                        }

                        showSetupDialog(newWebstormProject, newVbProject, connectedGithubRepo)

                        loadProjectConfiguration()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        project.showError(
                            "Unexpected error",
                            e.message ?: "An unexpected error occurred during creation process"
                        )
                    }
                }
            }
        }

        val progressWindow = ProgressWindow(false, true, project)
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(backgroundTask, progressWindow)
    }

    private fun showSetupDialog(
        newWebstormProject: Project,
        newVbProject: VisualBoostProjectResponseBody.Data.VisualBoostProject,
        connectedGithubRepo: GithubRepository
    ) {
        runInEdt {
            val projectSetupDialog =
                ProjectSetupDialog(newWebstormProject, newVbProject.name, newVbProject._id, connectedGithubRepo.url!!)
            projectSetupDialog.show()
        }
    }

    private fun setSettings(
        settings: VBProjectGenerationSettings,
        project: Project,
        newProject: VisualBoostProjectResponseBody.Data.VisualBoostProject
    ) {
        CredentialUtil.storeVisualBoostCredentials(settings.userData!!.email, settings.userData!!.password)
        val ideProjectSettings = VbProjectSettings.getInstance(project)
        ideProjectSettings.projectId = newProject._id
        ideProjectSettings.target = GenerationTarget.SERVER
    }


    private suspend fun createVBProject(
        settings: VBProjectGenerationSettings,
        indicator: ProgressIndicator
    ): VisualBoostProjectResponseBody.Data.VisualBoostProject {
        indicator.text = "Create new Project"

        val jwt = settings.getVbTokenOrThrowException()
        val projectName = settings.getProjectnameOrThrowException()

        val project = withContext(Dispatchers.IO) {
            API.createNewVisualBoostProject(jwt, projectName)
        }

        return project
    }

    private suspend fun createNewGithubRepository(
        settings: VBProjectGenerationSettings,
        indicator: ProgressIndicator
    ): GithubRepository {
        val repoName = settings.getGithubRepoNameOrThrowException()
        indicator.text = """Create new repository "$repoName" in Github """

        val createdGithubRepository = withContext(Dispatchers.IO) {
            API.Github.createGithubRepository(
                settings.getVbTokenOrThrowException(),
                settings.getGithubAccessTokenOrThrowException(),
                repoName
            )
        }

        return createdGithubRepository
    }

    private suspend fun connectGithubRepositoriesWithVisualBoost(
        githubRepository: GithubRepository,
        settings: VBProjectGenerationSettings,
        projectId: String,
        indicator: ProgressIndicator
    ): GithubRepository? {
        indicator.text = "Connect repository with VisualBoost"
        if (githubRepository.url == null) return null


        val githubData = settings.githubData!!
        val connectedBackendRepository = withContext(Dispatchers.IO) {
            API.connectGitRepository(
                settings.getVbTokenOrThrowException(),
                projectId,
                GitRepositoryType.TARGET,
                githubRepository.url,
                githubData.githubUsername,
                githubData.githubEmail,
                githubData.githubAccessToken
            )
        }

        return githubRepository
    }

    private suspend fun runInitBuild(
        settings: VBProjectGenerationSettings,
        projectId: String,
        indicator: ProgressIndicator
    ) {
        indicator.text = "Initialize Project"
        val projectName = settings.getProjectnameOrThrowException()
        val jwt = settings.getVbTokenOrThrowException()

        withContext(Dispatchers.IO) {
            API.Build.executeBackendBuildProcessAndWait(jwt, projectId, """Init project "$projectName""")
        }
    }


    private suspend fun checkoutRemoteRepository(
        project: Project,
        githubRepository: GithubRepository,
        settings: VBProjectGenerationSettings,
        indicator: ProgressIndicator
    ) {
        indicator.text = "Checkout Project"

        val projectName = settings.getProjectnameOrThrowException()
        val projectDir = Paths.get(project.basePath, settings.getProjectnameOrThrowException()).toFile()
        projectDir.mkdirs()

        val githubData = settings.githubData!!
        val username = githubData.githubUsername
        val acccessToken = settings.getGithubAccessTokenOrThrowException()

        val githubRepoUrl = githubRepository.url!!
        val backendUrl = githubRepoUrl.removePrefix("https://")
        val cloneCmd = "git clone https://$username:$acccessToken@$backendUrl $projectName"
        cloneCmd.runCommand(project)

        //Refresh project dir
        val virtualSourceDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(projectDir)
        virtualSourceDir?.refresh(false, false)
    }


    private suspend fun initEnvironmentVariables(
        project: Project,
        vbProjectId: String,
        settings: VBProjectGenerationSettings,
        indicator: ProgressIndicator
    ) {
        val projectDir = getProjectDir(settings, project)!!.toFile()

        indicator.text = "Setup environment variables"
        val jwt = settings.getVbTokenOrThrowException()

        val projectConfig = withContext(Dispatchers.IO) {
            return@withContext API.getProjectConfig(jwt, vbProjectId)
        }

        val mainEnvFile = EnvWriter.writeMainEnvFile(projectDir, projectConfig)
        if (mainEnvFile == null) {
            indicator.text2 = """Skipped: "./backend/.env" already exist."""
            delay(500)
        }

        val dbEnvFile = EnvWriter.writeDbEnvFile(projectDir, projectConfig)
        if (dbEnvFile == null) {
            if (mainEnvFile == null) {
                indicator.text2 = """Skipped: "./db/.env" already exist."""
                delay(500)
            }
        }

        val httpEnvFile = EnvWriter.httpEnvFile(projectDir, projectConfig)
        if (httpEnvFile == null) {
            indicator.text2 = """Skipped: "./backend/src/http/http-client.env.json" already exist."""
            delay(500)
        }

        indicator.text2 = null
    }

    private suspend fun createReplicaSetKey(
        project: Project,
        settings: VBProjectGenerationSettings,
        indicator: ProgressIndicator
    ): File? {
        indicator.text = "Create key file for replica set"

        val projectDir = getProjectDir(settings, project)!!.toFile()
        val dbDir = Paths.get(projectDir.absolutePath, "db")
        val replicaKeyFile = Paths.get(dbDir.absolutePathString(), "key", MONGO_REPLICA_KEY_FILE).toFile()

        var createdReplicaKeyFile: File? = null
        if (replicaKeyFile.exists()) {
            indicator.text2 = """Skipped: "${replicaKeyFile.invariantSeparatorsPath}" already exist."""
            delay(1000)
        } else {
            createdReplicaKeyFile = initReplicaKeyFile(replicaKeyFile)
        }

        //Refresh db dir
        val virtualSourceDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(dbDir.toFile())
        virtualSourceDir?.refresh(true, true)

        indicator.text2 = null

        return createdReplicaKeyFile
    }

    private fun initReplicaKeyFile(replicaKeyFile: File): File {
        val createdReplicaKeyFile = replicaKeyFile.create()

        val randomString = String.random()
        val replicaKey = Base64.getEncoder().encodeToString(randomString.toByteArray()).substring(0, 756)

        createdReplicaKeyFile.writeText(replicaKey)
        return createdReplicaKeyFile
    }

    private suspend fun setLinuxLineSeparatorsForInitScripts(
        project: Project,
        settings: VBProjectGenerationSettings,
        indicator: ProgressIndicator
    ) {
        indicator.text = "Adapt line separators of db config files"

        val projectDir = getProjectDir(settings, project)!!.toFile()
        val initReplicaKeyScriptFile = Paths.get(projectDir.absolutePath, "db", ".scripts", "init_replica_key.sh")
        initReplicaKeyScriptFile.toFile().setLineSeparatorToLF()

        val initReplicasetScriptKey = Paths.get(projectDir.absolutePath, "db", ".scripts", "init_replicaset.sh")
        initReplicasetScriptKey.toFile().setLineSeparatorToLF()

        delay(1000)
    }

    private suspend fun installNpmDependencies(
        project: Project,
        settings: VBProjectGenerationSettings,
        indicator: ProgressIndicator
    ) {
        indicator.text = "Install npm dependencies"

        val projectDir = getProjectDir(settings, project)!!.toFile()
        val backendDir = Paths.get(projectDir.absolutePath, "backend")

        val isWindows = System.getProperty("os.name").lowercase(Locale.getDefault()).contains("win");
        val npmCommand = if (isWindows) "npm.cmd" else "npm"

        val processBuilder = ProcessBuilder(npmCommand, "install")
        processBuilder.directory(backendDir.toFile())

        val result = processBuilder.start().waitFor()
        if (result != 0) {
            indicator.text2 = "Failed to install NPM dependencies."
            delay(1000)
        }

        //Refresh backendDir dir
        val virtualSourceDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(backendDir.toFile())
        virtualSourceDir?.refresh(true, false)
    }

    suspend fun createNewWebstormProjectForVbProject(
        project: Project,
        settings: VBProjectGenerationSettings,
        indicator: ProgressIndicator
    ): Project? {
        val projectName = settings.getProjectnameOrThrowException()
        val projectManager = ProjectManagerEx.getInstanceEx()

        indicator.text = """Create Webstorm-Project "$projectName" """

        val localRepositoryPath = getProjectDir(settings, project) ?: return null
        val newProjectPathAsFile = localRepositoryPath.toFile()

        if (newProjectPathAsFile.exists().not()) {
            newProjectPathAsFile.mkdirs()
        }

        OpenProjectTask()
        val task = OpenProjectTask {
            this.isNewProject = true
            this.forceOpenInNewFrame = true
            this.isRefreshVfsNeeded = true
            this.runConversionBeforeOpen = true
            this.useDefaultProjectAsTemplate = true
            this.runConfigurators = true
        }
        val newProject = projectManager.newProject(localRepositoryPath, task)!!

        val openedProject = withContext(Dispatchers.EDT) {
            val openTask = OpenProjectTask {
                this.project = newProject
                this.forceOpenInNewFrame = true
            }

            ProjectUtil.openProject(
                localRepositoryPath,
                task
            )
        }

        return openedProject
    }

    /**
     * Alter stand von createProject... - 16.08.2024
     *
     * suspend fun createNewWebstormProjectForVbProject(
     *         project: Project,
     *         settings: VBProjectGenerationSettings,
     *         indicator: ProgressIndicator
     *     ): Project? {
     *         val projectName = settings.getProjectnameOrThrowException()
     *         val projectManager = ProjectManagerEx.getInstanceEx()
     *
     *         indicator.text = """Create Webstorm-Project "$projectName" """
     *
     *         val localRepositoryPath = getProjectDir(settings, project) ?: return null
     *         val newProjectPathAsFile = localRepositoryPath.toFile()
     *
     *         if (newProjectPathAsFile.exists().not()) {
     *             newProjectPathAsFile.mkdirs()
     *         }
     *
     *         val task = OpenProjectTask {
     *             this.isNewProject = true
     *             this.forceOpenInNewFrame = true
     *             this.isRefreshVfsNeeded = true
     *             this.runConversionBeforeOpen = true
     *             this.useDefaultProjectAsTemplate = true
     *             this.runConfigurators = true
     *         }
     *         val newProject = projectManager.newProject(localRepositoryPath, task)!!
     *
     *         val demoDir = File(localRepositoryPath.toFile(), "dir")
     *         demoDir.mkdirs()
     *
     *         val demoFile = File(demoDir, "temp.txt")
     *         demoFile.create()
     *
     *         demoFile.writeText("hallo")
     *
     *
     * //        invokeLater {
     * //            val loadedProject = ProjectManagerEx.getInstanceEx().loadProject(localRepositoryPath)
     * //
     * //            val openTask = OpenProjectTask {
     * //                this.project = loadedProject
     * //                this.forceOpenInNewFrame = true
     * //                this.isRefreshVfsNeeded = true
     * //                this.runConversionBeforeOpen = true
     * //                this.useDefaultProjectAsTemplate = true
     * //                this.runConfigurators = true
     * //            }
     * //
     * //            ProjectUtil.openProject(
     * //                localRepositoryPath,
     * //                openTask
     * //            )
     * //            val moduleManager = ModuleManager.getInstance(project)
     * //            val moduleModel = moduleManager.getModifiableModel()
     * //            val newModule: Module = moduleManager.newModule(localRepositoryPath, ModuleTypeId.WEB_MODULE)
     * //            moduleModel.commit()
     * //        }
     *
     *
     * //        ApplicationManager.getApplication().invokeLater {
     * //            WriteAction.run<RuntimeException> {
     * //                val moduleManager = ModuleManager.getInstance(project)
     * //
     * //                val newModule: Module = moduleManager.newModule(Paths.get(localRepositoryPath.toFile().absolutePath, ".idea", "${projectName}.iml"), ModuleTypeId.WEB_MODULE)
     * //                moduleManager.getModifiableModel().commit()
     * //            }
     * //        }
     *
     * //        StartupManager.getInstance(project).runWhenProjectIsInitialized {
     * //            // Zusätzliche Konfigurationen oder Aktionen
     * //            VirtualFileManager.getInstance().syncRefresh()
     * //        }
     *
     * //        val loadedProject = ProjectManagerEx.getInstanceEx().loadProject(localRepositoryPath)
     *         val openedProject = withContext(Dispatchers.EDT) {
     *             val openTask = OpenProjectTask {
     *                 this.project = newProject
     *                 this.forceOpenInNewFrame = true
     * //                this.isRefreshVfsNeeded = true
     * //                this.configureToOpenDotIdeaOrCreateNewIfNotExists(localRepositoryPath, newProject)
     * //                this.runConversionBeforeOpen = true
     * //                this.useDefaultProjectAsTemplate = true
     * //                this.runConfigurators = true
     *             }
     *
     *             ProjectUtil.openProject(
     *                 localRepositoryPath,
     *                 task
     *             )
     *         }
     *
     *         return openedProject
     *     }
     */


//    suspend fun openProject(
//        project: Project,
//        settings: VBProjectGenerationSettings,
//        indicator: ProgressIndicator
//    ): Project? {
//        indicator.text = """Open initialized project"""
////
//        val projectName = settings.getProjectnameOrThrowException()
//        val localRepositoryPath = getProjectDir(settings, project) ?: return null
////
////        val projectManager = ProjectManagerEx.getInstanceEx()
////
////        val task = OpenProjectTask.build().asNewProject().withProjectName(projectName).withForceOpenInNewFrame(true)
////        val newProject = projectManager.newProject(localRepositoryPath, task) ?: return null
////        delay(1000)
////        val localRepositoryPath = getProjectDir(settings, project) ?: return null
////
////        val projectDir = localRepositoryPath.toFile()
////        if(projectDir.exists().not()){
////            projectDir.mkdirs()
////        }
//
//        val openedProject = withContext(Dispatchers.EDT) {
////            ProjectUtil.openProject(localRepositoryPath.toFile().absolutePath, null, true)
//            val task = OpenProjectTask {
//                this.isNewProject = true
//                this.forceOpenInNewFrame = true
//                this.isRefreshVfsNeeded = true
//                this.runConversionBeforeOpen = true
//                this.useDefaultProjectAsTemplate = true
//                this.runConfigurators = true
//            }
////            val task = OpenProjectTask.build().withForceOpenInNewFrame(true)
////
////            ProjectUtil.openProject(localRepositoryPath, task)
////
////            val moduleManager = ModuleManager.getInstance(project)
////            val moduleModel = moduleManager.getModifiableModel()
////            val newModule: Module = moduleModel.newModule("moduleFilePath", ModuleTypeId.WEB_MODULE)
////            moduleModel.commit()
//        }
//
//        return ProjectUtil.openProject(localRepositoryPath, OpenProjectTask.build().withForceOpenInNewFrame(true))
//
//    }

    private fun getProjectDir(settings: VBProjectGenerationSettings, project: Project): Path? {
        val projectName = settings.getProjectnameOrThrowException()
        val projectPath = project.basePath ?: return null
        return Paths.get(projectPath, projectName)
    }

    suspend fun openVbToolWindow(project: Project) {
        withContext(Dispatchers.EDT) {
            ToolWindowManager.getInstance(project).getToolWindow("VisualBoost")?.show()
        }
    }

    suspend fun clearOldIdeaDir(project: Project) {
        withContext(Dispatchers.Default) {
            val ideaDir = ProjectDirectories.getIdeaDir(project)
            if (ideaDir?.exists() == true) {
                val deleted = ideaDir.deleteRecursively()
            }
        }
    }

    private fun validateProjectSettings() {
        stateListener?.stateChanged(true)
    }

    private fun showSuccessDialog(project: Project, vbProject: VisualBoostProjectResponseBody.Data.VisualBoostProject) {
        val dialogBuilder = DialogBuilder()

        dialogBuilder.addOkAction().setText("Close")

        dialogBuilder.centerPanel(
            JLabel(
                """<html>
            <h1>${vbProject.name} created</h1>
            </html>""".trimMargin()
            )
        )

        dialogBuilder.show()
    }

    private fun loadProjectConfiguration() {
        runInEdt {
            val action =
                ActionManager.getInstance().getAction("visualboost.plugin.actions.LoadProjectConfigurationAction")
            ActionManager.getInstance().tryToExecute(action, null, null, null, false)
        }
    }


}