package visualboost.plugin.dialog

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.panel
import visualboost.plugin.settings.VbAppSettings
import visualboost.plugin.util.furtherInformation
import visualboost.plugin.util.title
import java.awt.Dimension
import javax.swing.JComponent


class IntroductionDialog(val width: Int = 600) : DialogWrapper(true) {

    init {
        title = "Introduction"

        addDoNotShowAgain()

        init()

        val cancelButton = getButton(cancelAction)
        cancelButton?.isVisible = false
        getButton(okAction)?.text = "Got it"
    }

    override fun init() {
        super.init()
        peer.window.preferredSize = Dimension(width, -1)
    }

    private fun addDoNotShowAgain() {
        setDoNotAskOption(object : com.intellij.openapi.ui.DoNotAskOption {
            override fun isToBeShown(): Boolean {
                return VbAppSettings.getInstance().showIntroductionDialog
            }

            override fun setToBeShown(showIntroductionDialog: Boolean, p1: Int) {
                VbAppSettings.getInstance().showIntroductionDialog = showIntroductionDialog
            }

            override fun canBeHidden(): Boolean {
                return true
            }

            override fun shouldSaveOptionsOnCancel(): Boolean {
                return true
            }

            override fun getDoNotShowMessage(): String {
                return "Don't show again"
            }

        })
    }

    override fun createCenterPanel(): JComponent {
        val panel = panel {
            val introText =
                "<p>Simplify your software development process by automating repetitive tasks with VisualBoost.<br>Create a fully functional project in seconds, deploy it instantly, expand it effortlessly, and maintain full ownership of your code.<br/><br/>With VisualBoost, you'll develop new backend applications significantly faster, without any drawbacks.</p>"

            title("Welcome to VisualBoost", 5)
            row {
                text(introText)
                bottomGap(BottomGap.SMALL)
            }

            val pluginText = """
                            <div>
                                <div><a href="https://wiki.visualboost.de/plugin/create-project">Simple Project Creation</a>
                                    <br>Quickly create new VisualBoost projects from within your IDE and start with a fully runnable backend application in less than 5 minutes.
                                </div>
                                <div><a href="https://wiki.visualboost.de/plugin/toolwindow">Integration into WebStorm</a>
                                    <br>Interact with VisualBoost directly within your JetBrains IDE via the VisualBoost tool window and auto pull changes after each build.
                                </div>
                                <div><a href="https://wiki.visualboost.de/plugin/extensions">Extension Builder</a>
                                    <br>Add extension files via Webstorm and use live templates to easily enhance your project with custom logic.
                                </div>
                                <div><a href="https://wiki.visualboost.de/plugin/extensions#synchronize-with-visualboost">Simple Synchronization</a>
                                    <br>Sync your extension with VisualBoost with one click.
                                </div>
                            </div>
                                """
            title("Plugin features")
            row {
                text(pluginText)
                bottomGap(BottomGap.SMALL)
            }

            furtherInformation()
        }

        return panel
    }

}