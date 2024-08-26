package visualboost.plugin.components

import com.intellij.icons.AllIcons
import com.intellij.openapi.observable.util.whenTextChanged
import com.intellij.openapi.vcs.changes.ui.LocalChangesBrowser.AllChanges
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.UIUtil
import java.awt.*
import javax.swing.*


class LoginComponent : JPanel() {

    val header = JBLabel("Authentication:")

    val buttonPanel = JPanel()
    private val loginButton = JButton("Login")
    private var onClickEvent: (() -> Unit)? = null
    var onEmailChanged: ((email: String) -> Unit)? = null
    var onPasswordChanged: ((password: String) -> Unit)? = null

    private val emailTextField = JBTextField()
    private val passwordTextfield = JBPasswordField()

    val errorPanel = JPanel()
    private val errorLabel = JBLabel()


    init {
        layout = GridBagLayout()

        initButtonSection()
        initErrorLabel()

        emailTextField.whenTextChanged {
            onEmailChanged?.invoke(getEmail())
        }

        passwordTextfield.whenTextChanged {
            onPasswordChanged?.invoke(getPassword())
        }


        val infoLabelPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        (infoLabelPanel.layout as FlowLayout).vgap = 0
        val infoLabel = panel {
            row {
                comment(
                    """<p>No VisualBoost account yet? <a href="https://app.visualboost.de">Click here</a> to create an new one.</p>""".trimIndent()
                )
            }
        }
        infoLabelPanel.add(infoLabel)

        val panel = FormBuilder.createFormBuilder()
            .addComponent(header)
            .addLabeledComponent(JBLabel(AllIcons.General.User), emailTextField, 1, false)
            .addLabeledComponent(JBLabel(AllIcons.CodeWithMe.CwmPermissions), passwordTextfield, 1, false)
            .addComponent(errorPanel)
            .addComponent(infoLabelPanel)
            .addVerticalGap(10)
            .addComponent(buttonPanel)
            .panel

        val constraints = GridBagConstraints()
        constraints.fill = GridBagConstraints.HORIZONTAL
        constraints.weightx = 1.0

        add(panel, constraints)
    }

    private fun initButtonSection() {
        buttonPanel.layout = BoxLayout(buttonPanel, BoxLayout.X_AXIS)
        buttonPanel.add(Box.createHorizontalGlue())
        buttonPanel.add(loginButton)

        loginButton.addActionListener {
            onClickEvent?.invoke()
        }
    }

    private fun initErrorLabel() {
        errorPanel.layout = BoxLayout(errorPanel, BoxLayout.X_AXIS)
        errorPanel.add(Box.createHorizontalGlue())
        errorPanel.add(errorLabel)
        errorPanel.isVisible = false

        errorLabel.foreground = Color.decode("#C7222D")
        errorLabel.icon = AllIcons.General.Error
    }

    fun onLoginButtonClicked(onClick: () -> Unit): LoginComponent {
        this.onClickEvent = onClick
        return this
    }

    fun load(load: Boolean) {
        if (load) {
            loginButton.icon = AnimatedIcon.Default()
            loginButton.isEnabled = false
        } else {
            loginButton.isEnabled = true
            loginButton.icon = null
        }
    }

    fun getEmail(): String {
        return emailTextField.text
    }

    fun setEmail(email: String) {
        emailTextField.text = email
    }

    fun getPassword(): String {
        return String(passwordTextfield.password)
    }

    fun setPassword(password: String) {
        passwordTextfield.text = password
    }

    fun enableButton(enable: Boolean) {
        loginButton.isEnabled = enable
    }

    fun enableEmailTextField(enable: Boolean) {
        emailTextField.isEnabled = enable
    }

    fun enablePasswordTextField(enable: Boolean) {
        passwordTextfield.isEnabled = enable
    }

    fun showSuccessButton(btnText: String = "Logged in") {
        loginButton.icon = AllIcons.General.InspectionsOK
        loginButton.isEnabled = false
        loginButton.text = btnText
    }

    fun showHeader(show: Boolean): LoginComponent {
        header.isVisible = show
        return this
    }

    fun enableInputForms(enable: Boolean){
        enableEmailTextField(enable)
        enablePasswordTextField(enable)
        enableButton(enable)
    }

    fun showError(errorMsg: String?){
        errorLabel.text = errorMsg
        errorPanel.isVisible = errorMsg != null
    }

}