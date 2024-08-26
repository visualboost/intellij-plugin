package visualboost.plugin.components

import com.intellij.icons.AllIcons
import com.intellij.ide.HelpTooltip
import com.intellij.openapi.observable.util.whenItemSelected
import com.intellij.openapi.observable.util.whenTextChanged
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.TitledSeparator
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.impl.CollapsibleTitledSeparatorImpl
import com.intellij.util.ui.FormBuilder
import visualboost.plugin.api.models.intellij.project.github.GithubAuthData
import visualboost.plugin.api.models.intellij.project.github.GithubRepository
import visualboost.plugin.dialog.GithubLoginDialog
import visualboost.plugin.intellijProject.VBProjectGenerationSettings
import visualboost.plugin.util.CredentialUtil
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*

class GithubComponent(val vbProjectGenerationSettings: VBProjectGenerationSettings) : JPanel() {

    lateinit var githubPanel: JPanel

    //Input fields
    lateinit var tabbedPane: JBTabbedPane
    lateinit var backendRepoNameTextField: JBTextField
    lateinit var backendRepoUrlCombobox: ComboBox<String>

    //Button
    lateinit var buttonPanel: JPanel
    lateinit var githubLoginButton: JButton

    var onLoginSuccess: ((githubData: GithubAuthData) -> Unit)? = null
    var onRepositoryNamesChanged: ((backendRepoName: String?) -> Unit)? = null
    var onRepositoryUrlChanged: ((backendRepoName: GithubRepository?) -> Unit)? = null

    var repositories = listOf<GithubRepository>()
        set(value) {
            val nullRepo = GithubRepository("<None>", null)
            field = listOf(nullRepo) + value.sortedByDescending { it.name }
            updateComboxBox()
        }

    init {
        layout = GridBagLayout()

        val panel = FormBuilder.createFormBuilder()
            .addComponent(initGithubPanel())
            .panel

        initChildComponent(initGithubPanel())
    }

    private fun initChildComponent(component: JComponent) {
        val constraints = GridBagConstraints()
        constraints.fill = GridBagConstraints.HORIZONTAL
        constraints.weightx = 1.0
        add(component, constraints)
    }

    private fun initGithubPanel(): JPanel {
        githubPanel = JPanel()
        githubPanel.layout = BoxLayout(githubPanel, BoxLayout.Y_AXIS)

        val collapsibleTitledSeparator = TitledSeparator("Github")
        githubPanel.add(collapsibleTitledSeparator)

        tabbedPane = JBTabbedPane()
        initCreateRepositoryTab("Create new repository", tabbedPane)
        initSelectRepositoryTab("Choose existing repository", tabbedPane)
        initToolTips()

        tabbedPane.selectedIndex = 0
        githubPanel.add(tabbedPane)
        githubPanel.add(initGithubButton())

        return githubPanel
    }

    private fun initToolTips() {
        HelpTooltip().setDescription("Enter a name for your new backend repository. Example: MyBackendRepository")
            .installOn(backendRepoNameTextField)
        HelpTooltip().setDescription("Enter a name for your new backend repository.\nExample: https://github.com/user/repository.git")
            .installOn(backendRepoUrlCombobox)
    }

    private fun initCreateRepositoryTab(title: String, tabbedPane: JBTabbedPane) {
        backendRepoNameTextField = JBTextField()

        val createRepositoryName = FormBuilder.createFormBuilder()
            .addLabeledComponent(
                "Repository (Name):*",
                backendRepoNameTextField,
                1,
                false
            )
            .panel

        backendRepoNameTextField.whenTextChanged {
            onRepositoryNamesChanged?.invoke(
                if (backendRepoNameTextField.text.isNullOrBlank()) null else backendRepoNameTextField.text
            )
        }

        tabbedPane.addTab(title, createRepositoryName)
    }

    private fun initSelectRepositoryTab(title: String, tabbedPane: JBTabbedPane) {
        backendRepoUrlCombobox = ComboBox()

        val selectExistingRepositories = FormBuilder.createFormBuilder()
            .addLabeledComponent(
                "Backend-Repository:* (URL)",
                backendRepoUrlCombobox,
                1,
                false
            )
            .panel

        backendRepoUrlCombobox.whenItemSelected {
            val selectedRepo = repositories[backendRepoUrlCombobox.selectedIndex]

            onRepositoryUrlChanged?.invoke(if (selectedRepo.url == null) null else selectedRepo)
        }

        tabbedPane.addTab(title, selectExistingRepositories)
    }

    private fun initGithubButton(): JPanel {
        buttonPanel = JPanel()
        githubLoginButton = JButton("Login into Github")

        buttonPanel.layout = BoxLayout(buttonPanel, BoxLayout.X_AXIS)
        buttonPanel.add(Box.createHorizontalGlue())
        buttonPanel.add(githubLoginButton)

        githubLoginButton.addActionListener {
            val userId = vbProjectGenerationSettings.userData?.userId ?: return@addActionListener

            val dialog = GithubLoginDialog(userId)
            dialog.onSuccess = {
                CredentialUtil.storeGithubAccessToken(it.githubAccessToken)
                onLoginSuccess?.invoke(it)
                dialog.close(0)
            }
            dialog.show()
        }

        return buttonPanel
    }

    private fun collapse(collapse: Boolean) {
        tabbedPane.isVisible = !collapse
        buttonPanel.isVisible = !collapse
    }

    fun enableInputFields(enable: Boolean) {
        backendRepoNameTextField.isEnabled = enable
        backendRepoUrlCombobox.isEnabled = enable
    }

    fun selectTab(index: Int) {
        tabbedPane.selectedIndex = index
    }

    fun focusBackendTextfield() {
        if (tabbedPane.selectedIndex == 0) {
            backendRepoNameTextField.requestFocus()
        } else {
            backendRepoUrlCombobox.requestFocus()
        }
    }

    private fun updateComboxBox() {
        backendRepoUrlCombobox.removeAllItems()

        repositories.forEach {
            backendRepoUrlCombobox.addItem(it.name)
        }
    }

    fun showSuccessButton(btnText: String = "Logged in") {
        githubLoginButton.icon = AllIcons.General.InspectionsOK
        githubLoginButton.isEnabled = false
        githubLoginButton.text = btnText
    }

    fun createRepositoryTabIsSelected(): Boolean {
        return tabbedPane.selectedIndex == 0
    }

    fun connectExistingRepositoryTabIsSelected(): Boolean {
        return tabbedPane.selectedIndex == 1
    }

    fun enableLoginButton(enable: Boolean) {
        githubLoginButton.isEnabled = enable
    }


}