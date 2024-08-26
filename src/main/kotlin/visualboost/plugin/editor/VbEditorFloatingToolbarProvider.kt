package visualboost.plugin.editor

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.toolbar.floating.AbstractFloatingToolbarProvider
import com.intellij.openapi.editor.toolbar.floating.FloatingToolbarComponent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import isJavascriptFile
import isVbExtension
import visualboost.plugin.editor.VbEditorFloatingActionGroup.Companion.ACTION_GROUP


class VbEditorFloatingToolbarProvider : AbstractFloatingToolbarProvider(ACTION_GROUP) {
    override val autoHideable: Boolean = false

    val documentListeners: MutableMap<String, DocumentListener> = mutableMapOf()

    override fun register(dataContext: DataContext, component: FloatingToolbarComponent, parentDisposable: Disposable) {
        super.register(dataContext, component, parentDisposable)

        val project = dataContext.getData(CommonDataKeys.PROJECT) ?: return

//        //Is triggered after file saved
//        project.messageBus.connect().subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
//            override fun after(events: List<VFileEvent>) {
//                for (event in events) {
//                    val vFile = event.file ?: continue
//                    if (!vFile.isJavascriptFile()) continue
//                    if (!vFile.isVbExtension(project)) continue
//
//                    val fileIsModified = FileDocumentManager.getInstance().isFileModified(vFile)
//                    if (fileIsModified) {
//                        component.scheduleShow()
//                    } else {
//                        component.scheduleHide()
//                    }
//                }
//            }
//        })

        GlobalEditorToolbarHandler.synchronizationTriggered = {
            component.scheduleHide()
        }

        /**
         * Message bus for document changes without saving it to storage
         */
        project.messageBus.connect().subscribe<FileEditorManagerListener>(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
                    if (!file.isJavascriptFile()) return
                    if (!file.isVbExtension(project)) return

                    val document = FileDocumentManager.getInstance().getDocument(file) ?: return
                    showSyncButtonIfFileIsModified(file, component)

                    val listener = object : DocumentListener {
                        override fun documentChanged(event: DocumentEvent) {
                            showSyncButtonIfFileIsModified(file, component)
                        }
                    }
                    documentListeners[file.path] = listener
                    document.addDocumentListener(listener)
                }

                override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
                    val document = FileDocumentManager.getInstance().getDocument(file) ?: return

                    val listener = documentListeners.getOrDefault(file.path, null) ?: return
                    document.removeDocumentListener(listener)
                    documentListeners.remove(file.path)
                }
            })

        component.scheduleShow()
    }

    private fun showSyncButtonIfFileIsModified(file: VirtualFile, component: FloatingToolbarComponent) {
        val fileIsModified = FileDocumentManager.getInstance().isFileModified(file)
        if (fileIsModified) {
            component.scheduleShow()
        } else {
            component.scheduleHide()
        }
    }
}

