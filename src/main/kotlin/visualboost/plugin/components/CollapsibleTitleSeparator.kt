package visualboost.plugin.components

import com.intellij.icons.AllIcons
import com.intellij.ui.components.JBLabel
import java.awt.*
import java.awt.image.BufferedImage
import javax.swing.*


class CollapsibleTitleSeparator(val title: String): JPanel() {

   val titleJBLabel: JBLabel

    init {
       layout = GridBagLayout() // Verwende GridBagLayout für die Zentrierung

       titleJBLabel = JBLabel(AllIcons.General.ChevronRight)
       titleJBLabel.text = title
       val separator = JSeparator(SwingConstants.HORIZONTAL) // Horizontaler Separator

       val gbc = GridBagConstraints()
       gbc.insets = Insets(0, 0, 0, 5) // Abstand zwischen Titel und Separator

       // Füge das Titel-Label hinzu
       gbc.gridx = 0
       gbc.gridy = 0
       gbc.anchor = GridBagConstraints.WEST // Titel links ausrichten

       add(titleJBLabel, gbc)

       // Füge den Separator hinzu
       gbc.gridx = 1
       gbc.fill = GridBagConstraints.HORIZONTAL
       gbc.weightx = 1.0
       gbc.anchor = GridBagConstraints.CENTER

       add(separator, gbc)

    }

//   private fun toggleCollapse() {
//      collapsed = !collapsed
//      rotateArrow(if (collapsed) 0 else 90) // Pfeil drehen
//   }
//
//   // Methode zum Drehen des Pfeils
//   private fun rotateArrow(degrees: Int) {
//      arrowLabel.setIcon(createArrowIcon(degrees))
//   }

   private fun createArrowIcon(degrees: Int): Icon? {
      val size = 10
      val arrowImage = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
      val g2 = arrowImage.graphics as Graphics2D
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

      // Pfeil zeichnen
      g2.stroke = BasicStroke(2f)
      g2.color = Color.BLACK
      val old = g2.transform
      g2.rotate(Math.toRadians(degrees.toDouble()), size / 2.0, size / 2.0)
      g2.drawLine(size / 4, size / 2, 3 * size / 4, size / 2) // Horizontale Linie
      g2.drawLine(3 * size / 4, size / 2, size / 2, size / 4) // Diagonale nach oben
      g2.drawLine(3 * size / 4, size / 2, size / 2, 3 * size / 4) // Diagonale nach unten
      g2.transform = old
      g2.dispose()
      return ImageIcon(arrowImage)
   }

}