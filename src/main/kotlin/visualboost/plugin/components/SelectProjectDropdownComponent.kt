package visualboost.plugin.components

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import kotlinx.coroutines.*
import visualboost.plugin.api.API.fetchProjects
import visualboost.plugin.api.models.AllProjectsResponseBody
import visualboost.plugin.settings.VbProjectSettings
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*
import kotlin.coroutines.CoroutineContext

class SelectProjectDropdownComponent(val project: Project, val autoLoadProjects: Boolean = true) : JPanel(),
    CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.Default + SupervisorJob()

    //Input fields
    lateinit var fetchButton: JButton
    lateinit var comboBox: ComboBox<String>

    @set:JvmName("setVbProjects")
    var projects = mutableListOf<AllProjectsResponseBody.Data.Project>()

    init {
        initChildComponent(initProjectComponent())

        autoLoadProjects()
    }

    private fun initChildComponent(component: JComponent) {
        val constraints = GridBagConstraints()
        constraints.fill = GridBagConstraints.HORIZONTAL
        constraints.weightx = 1.0
        add(component, constraints)
    }

    private fun initProjectComponent(): JPanel {
        layout = GridBagLayout()

        val projectPanel = JPanel()
        projectPanel.layout = BoxLayout(projectPanel, BoxLayout.X_AXIS)

        initComboBox(projectPanel)
        initFetchButton(projectPanel)

        return FormBuilder.createFormBuilder()
            .addComponent(JBLabel("Projects:"))
            .addComponent(projectPanel)
            .panel
    }

    private fun initFetchButton(panel: JPanel) {
        fetchButton = JButton(AllIcons.Toolwindows.SettingSync)
        fetchButton.addActionListener {
            loadProjects()
        }

        panel.add(fetchButton)
    }

    private fun initComboBox(panel: JPanel) {
        comboBox = ComboBox<String>()
        panel.add(comboBox)
        initProjects()
    }

    private fun isLoading(loading: Boolean) {
        if (loading) {
            fetchButton.icon = AnimatedIcon.Default()
            fetchButton.isEnabled = false
        } else {
            fetchButton.icon = AllIcons.Toolwindows.SettingSync
            fetchButton.isEnabled = true
        }
    }

    fun loadProjects() {
        launch {
            try {
                isLoading(true)

                val projects = withContext(Dispatchers.IO) {
                    fetchProjects().sortedByDescending { it.name }
                }
                setProjects(projects)
                selectedProjectBySettingsProjectId()
            } finally {
                isLoading(false)
            }
        }
    }

    private fun initProjects() {
        projects.clear()
        projects.add(AllProjectsResponseBody.Data.Project(null, "<None>"))
        updateComboBox()
    }

    fun autoLoadProjects() {
        if (!autoLoadProjects) return

        val settings = VbProjectSettings.getInstance(project)
        if (!settings.projectSelected()) return

        loadProjects()
        selectedProjectBySettingsProjectId()
    }


    private fun updateComboBox() {
        comboBox.removeAllItems()
        projects.forEach {
            comboBox.addItem(it.name)
        }
    }

    private fun setProjects(newProjects: List<AllProjectsResponseBody.Data.Project>) {
        initProjects()
        projects.addAll(newProjects)
        updateComboBox()
    }

    private fun selectedProjectBySettingsProjectId() {
        val projectSettings = VbProjectSettings.getInstance(project)
        if (!projectSettings.projectSelected()) {
            comboBox.selectedIndex = 0
        }

        val projectIndex = projects.indexOfFirst { it._id == projectSettings.projectId }
        if (projectIndex <= 0) {
            return
        }

        comboBox.selectedIndex = projectIndex
    }

    fun getSelectedProject(): AllProjectsResponseBody.Data.Project? {
        return this.projects.getOrNull(comboBox.selectedIndex)
    }

    fun clearProjects() {
        initProjects()
        comboBox.selectedIndex = 0
    }


}