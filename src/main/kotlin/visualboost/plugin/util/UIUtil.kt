import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.observable.util.addDocumentListener
import com.intellij.openapi.observable.util.whenDocumentChanged
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBTextField
import visualboost.plugin.settings.VbProjectSettings
import visualboost.plugin.util.ProjectDirectories
import java.io.File
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import kotlin.io.path.Path
import kotlin.io.path.invariantSeparatorsPathString

fun JTextField.addTextChangeListener(listener: (text: String) -> Unit){
    this.document.addDocumentListener(object: javax.swing.event.DocumentListener {
        override fun insertUpdate(e: DocumentEvent?) {
            listener.invoke(this@addTextChangeListener.text)
        }

        override fun removeUpdate(e: DocumentEvent?) {
            listener.invoke(this@addTextChangeListener.text)
        }

        override fun changedUpdate(e: DocumentEvent?) {
            listener.invoke(this@addTextChangeListener.text)
        }
    })
}

fun JBTextField.addTextChangeListener(listener: (text: String) -> Unit){
    (this as JTextField).addTextChangeListener(listener)
}