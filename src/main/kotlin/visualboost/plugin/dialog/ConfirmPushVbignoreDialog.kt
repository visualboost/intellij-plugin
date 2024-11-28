package visualboost.plugin.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.dsl.builder.panel
import kotlinx.coroutines.*
import visualboost.plugin.api.API
import visualboost.plugin.api.models.model.AllModelsResponseBody
import visualboost.plugin.settings.VbProjectSettings
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import kotlin.coroutines.CoroutineContext


class ConfirmPushVbignoreDialog : DialogWrapper(true) {

    val models: MutableList<AllModelsResponseBody.Data.Model> = mutableListOf()

    init {
        title = "Confirmation required"
        init()
    }


    override fun createCenterPanel(): JComponent {
        val panel = panel {
            row {
                label("""
                    <html>
                        Are you sure you want to push the <code>.vbignore</code> file to make the changes take effect?<br/>
                        The following steps will be proceeded if you confirm:
                        <ol>
                        <li>Unstage the current changes in Git</li>
                        <li>Stage the <code>.vbignore</code> file</li>
                        <li>Commit the changes</li>
                        <li>Push the commit to the remote repository</li>
                        </ol>
                        Please confirm if you want to proceed.
                    </html>
                """.trimIndent())
            }
        }

        return panel
    }

}