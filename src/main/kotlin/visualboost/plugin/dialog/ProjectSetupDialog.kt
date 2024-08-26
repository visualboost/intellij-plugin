package visualboost.plugin.dialog

import com.intellij.execution.RunManager
import com.intellij.execution.actions.EditRunConfigurationsAction
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.panel
import visualboost.plugin.api.API
import visualboost.plugin.util.*
import java.awt.Font
import javax.swing.JComponent
import javax.swing.event.HyperlinkEvent


class ProjectSetupDialog(
    val project: Project,
    val projectName: String,
    val vbProjectId: String,
    val githubRepoUrl: String
) : DialogWrapper(true) {


    init {
        title = "Project created"

//        setDoNotAskOption(object : com.intellij.openapi.ui.DoNotAskOption {
//            override fun isToBeShown(): Boolean {
//                return true
//            }
//
//            override fun setToBeShown(p0: Boolean, p1: Int) {
//                println(p0)
//            }
//
//            override fun canBeHidden(): Boolean {
//                return true
//            }
//
//            override fun shouldSaveOptionsOnCancel(): Boolean {
//                return true
//            }
//
//            override fun getDoNotShowMessage(): String {
//                return "Do not show again"
//            }
//
//        })

        init()
        val cancelButton = getButton(cancelAction)
        cancelButton?.isVisible = false
        getButton(okAction)?.text = "Got it"
    }

    override fun createCenterPanel(): JComponent {
        val panel = panel {
            row {
                label("Project Initialization and Setup Report").bold().applyToComponent {
                    font = Font(font.name, Font.BOLD, font.size + 5)
                }
            }

            row {
                text(
                    "We have created a new VisualBoost project <strong>$projectName</strong> and connected it with a Github repository for you.\nThe project is fully set up and ready for you to start coding.\n\nYou can visit the VisualBoost Project or the Github repository by clicking one of the following links:"
                )
            }

            row {
                link("VisualBoost: $projectName") {
                    BrowserUtil.browse(API.getProjectUrl(vbProjectId))
                }.applyToComponent {
                    setExternalLinkIcon()
                }

                link("Github") {
                    BrowserUtil.browse(githubRepoUrl)
                }.applyToComponent {
                    setExternalLinkIcon()
                }
                bottomGap(BottomGap.SMALL)
            }

            val htmlText = "Before you can run the application, please ensure that " +
                    "<strong>Docker</strong> " +
                    "is installed and running. This is necessary to start the database.<br/>" +
                    "Additionally, <strong>NPM</strong> " +
                    "must be installed to run the Node.js application.<br/>These prerequisites are essential for the proper functioning of the application."

            title("Prerequisites")

            row {
                text(htmlText)
                bottomGap(BottomGap.SMALL)
            }


            val howToStartText =
                """To get the application running, the database must be started first, followed by the server application.<br/>Both tasks can be done by executing the provided <a href="openEditConfiguration">Run Configurations</a>.
                <br/><br/>To start the database, simply run <a href="showDbConfig">${project.getStartDatabaseConfigurationName()}</a>. The initial startup of the database may take a moment, as it needs to be initialized the first time it is launched.
                <br/><br/>After the database has started successfully, you can <a href="startAppConfig">start the application once</a> or <a href="startDeamonConfig">start the application as a daemon process</a>. Starting the application as a daemon process will allow your IDE to reload the project every time you make changes.
                """

            title("How to start")

            row {
                this.text(howToStartText).applyToComponent {
                    this.addHyperlinkListener {
                        if (it.eventType != HyperlinkEvent.EventType.ACTIVATED) return@addHyperlinkListener

                        val configurationSettings = if (it.description == "showDbConfig") {
                            RunManager.getInstance(project)
                                .findConfigurationByName(project.getStartDatabaseConfigurationName())
                        } else if (it.description == "startAppConfig") {
                            RunManager.getInstance(project)
                                .findConfigurationByName(project.getStartApplicationOnceRunConfigName())
                        } else if (it.description == "startDeamonConfig") {
                            RunManager.getInstance(project)
                                .findConfigurationByName(project.getStartApplicationInDevModeRunConfigName())
                        } else {
                            null
                        }

                        if (configurationSettings != null) {
                            RunManager.getInstance(project).selectedConfiguration = configurationSettings
                        }

                        ActionManager.getInstance()
                            .tryToExecute(EditRunConfigurationsAction(), null, null, null, false)
                    }
                }
                bottomGap(BottomGap.SMALL)
            }

            furtherInformation()
        }

        return panel
    }

}