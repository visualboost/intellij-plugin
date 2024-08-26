package visualboost.plugin.components

import com.intellij.ui.components.JBLabel
import java.awt.Color
import java.awt.Font
import javax.swing.BorderFactory



class CodeBlockLabel(text: String): JBLabel(text) {

    init {
        // Setze die Schriftart auf Monospace (z.B. Courier New)
        font = Font("Courier New", Font.PLAIN, font.size)

        background = Color(230, 230, 230)
        isOpaque = true
        border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

    }
}