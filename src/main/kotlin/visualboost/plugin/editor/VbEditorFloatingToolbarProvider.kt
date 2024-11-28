package visualboost.plugin.editor

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.toolbar.floating.AbstractFloatingToolbarProvider
import com.intellij.openapi.editor.toolbar.floating.FloatingToolbarComponent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBViewport
import isJavascriptFile
import isVbExtension
import visualboost.plugin.editor.VbEditorFloatingActionGroup.Companion.ACTION_GROUP
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionListener
import javax.swing.JComponent


class VbEditorFloatingToolbarProvider : AbstractFloatingToolbarProvider(ACTION_GROUP) {
    override val autoHideable: Boolean = false

    val documentListeners: MutableMap<String, DocumentListener> = mutableMapOf()
    var showToolbar = false

    private fun showToolbar(show: Boolean, component: FloatingToolbarComponent) {
        runInEdt {
            if (show) {
                component.scheduleShow()
            } else {
                component.scheduleHide()
            }
        }

        showToolbar = show
    }

    override fun register(dataContext: DataContext, component: FloatingToolbarComponent, parentDisposable: Disposable) {
        super.register(dataContext, component, parentDisposable)

        val project = dataContext.getData(CommonDataKeys.PROJECT) ?: return

        project.messageBus.connect().subscribe<FileEditorManagerListener>(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            FileEditorListener(project, component) { s, c ->
                if (showToolbar == s) return@FileEditorListener
                showToolbar(s, c)
            }
        )

        val messageBusConnection = project.messageBus.connect()
        messageBusConnection.subscribe(
            FileDocumentManagerListener.TOPIC,
            FileSaveListener(project, component) { s,c ->
                showToolbar(s,c)
            }
        )

        component.hideImmediately()
    }

    inner class FileSaveListener(
        val project: Project,
        val component: FloatingToolbarComponent,
        val onShowToolbar: (show: Boolean, component: FloatingToolbarComponent) -> Unit
    ) :
        FileDocumentManagerListener {

        override fun beforeDocumentSaving(document: Document) {
            val vFile = FileDocumentManager.getInstance().getFile(document) ?: return
            if (!vFile.isJavascriptFile() || !vFile.isVbExtension(project)) {
                onShowToolbar(false, component)
                showToolbar = false
                return
            }

            onShowToolbar(true, component)
        }

    }


    inner class FileEditorListener(
        val project: Project,
        val component: FloatingToolbarComponent,
        val onShowToolbar: (show: Boolean, component: FloatingToolbarComponent) -> Unit
    ) :
        FileEditorManagerListener {

        var mouseListener: MouseMotionListener? = null

        private fun addMouseListener(editorContentComponent: JComponent) {
            mouseListener = object : MouseMotionListener {
                override fun mouseDragged(e: MouseEvent?) {}

                override fun mouseMoved(e: MouseEvent) {
                    val viewWidth =
                        (editorContentComponent.parent as? JBViewport)?.width ?: editorContentComponent.width

                    val isMouseInTopRight = e.x > viewWidth - 200 && e.y < 150
                    onShowToolbar(isMouseInTopRight, component)
                }
            }

            editorContentComponent.addMouseMotionListener(mouseListener)
        }

        private fun clearMouseListener(editorContentComponent: JComponent) {
            if (mouseListener == null) return

            editorContentComponent.removeMouseMotionListener(mouseListener)
            mouseListener = null
        }

        override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
            /* Check if the currently opened file is a vb extension.
             */
            if (!file.isJavascriptFile() || !file.isVbExtension(project)) {
                onShowToolbar(false, component)
                return
            }

            onShowToolbar(true, component)
            val editorContentComponent = source.selectedTextEditor?.contentComponent ?: return
            addMouseListener(editorContentComponent)
        }

        override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
            val document = FileDocumentManager.getInstance().getDocument(file) ?: return

            val listener = documentListeners.getOrDefault(file.path, null) ?: return
            document.removeDocumentListener(listener)
            documentListeners.remove(file.path)

            val editorContentComponent = source.selectedTextEditor?.contentComponent ?: return
            clearMouseListener(editorContentComponent)
        }
    }
}

