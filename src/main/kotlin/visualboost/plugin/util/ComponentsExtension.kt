package visualboost.plugin.util

import com.intellij.credentialStore.Credentials
import com.intellij.formatting.SpacingBuilder
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.remoteServer.util.CloudConfigurationUtil.createCredentialAttributes
import com.intellij.ui.JBColor
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.gridLayout.UnscaledGapsY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import visualboost.plugin.api.models.LoginRequestBody
import visualboost.plugin.settings.VbAppSettings
import java.awt.Color
import java.awt.Font
import javax.swing.JPanel

fun Panel.title(text: String, increasedFontSize: Int = 4, bottomGap: BottomGap = BottomGap.SMALL) {
    row {
        label(text).bold().applyToComponent {
            font = Font(font.name, Font.BOLD, font.size + increasedFontSize)
        }
    }
}

fun Panel.furtherInformation(){
    title("Further Information")
    row {
        text("""
                 <p>If you have any further questions about using VisualBoost, please refer to our <a href="https://wiki.visualboost.de">documentation</a>, watch our <a href="https://www.youtube.com/watch?v=XkCMKl2S3Ao&list=PL_KLQBBjBxQaxzPsSb6UckLbwmTkG27Fm">video tutorial</a>, or start a <a href="https://github.com/visualboost/VisualBoost/discussions">discussion</a> on GitHub.
                <br/>For additional information, you can also visit <a href="https://www.visualboost.de">our official website</a>.</p>
                <p><br/>If you find VisualBoost helpful, I would greatly appreciate your support by giving the project a <a href="https://github.com/visualboost/VisualBoost">star on GitHub</a>.</p>
                """)
    }}
