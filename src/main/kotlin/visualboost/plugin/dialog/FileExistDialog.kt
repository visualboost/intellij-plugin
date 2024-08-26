package visualboost.plugin.dialog

import com.intellij.openapi.ui.DialogWrapper
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel


class FileExistDialog(val filePath: String) : DialogWrapper(true) {

    companion object {
        fun show(filePath: String, onOkAction: (() -> Unit)){
            val fileExist = FileExistDialog(filePath)
            val okPressed = fileExist.showAndGet()
            if(okPressed){
                onOkAction()
            }
        }
    }

    init {
        title = "Override File"
        init()

        getButton(okAction)?.text = "Override"
        size.width = 300
    }

    override fun createCenterPanel(): JComponent {
        val dialogPanel = JPanel(BorderLayout())
        dialogPanel.add(JLabel("""<html>The file <b>"$filePath"</b> does already exist.<br/>Would you like to override the file?</html>"""))
        return dialogPanel
    }






}