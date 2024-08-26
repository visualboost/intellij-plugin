package visualboost.plugin.dialog

import com.google.gson.reflect.TypeToken
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import com.intellij.util.ui.UIUtil.FontColor
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandler
import org.cef.network.CefRequest
import visualboost.plugin.api.API.getAuthUrl
import visualboost.plugin.api.API.getGithubOAuthAppUrl
import visualboost.plugin.api.models.intellij.project.github.GithubAuthData
import visualboost.plugin.components.VbWebview
import visualboost.plugin.models.GSON
import java.awt.Color
import java.awt.Dimension
import java.awt.GridBagLayout
import javax.swing.*


class GithubLoginDialog(val userId: String) : DialogWrapper(true) {

    companion object {
        const val WIDTH = 700
        const val HEIGHT = 800
    }

    lateinit var panel: JPanel
    lateinit var browser: VbWebview
    lateinit var webView: JBCefBrowser
    lateinit var jsQuery: JBCefJSQuery

    lateinit var progressPanel: JPanel
    lateinit var progressLabel: JBLabel

    var onSuccess: ((githubAuthData: GithubAuthData) -> Unit)? = null

    init {
        title = "Github Authorization"
        init()

        getButton(okAction)?.isVisible = false
        getButton(cancelAction)?.isVisible = false
        size.width = 300
    }

    override fun init() {
        super.init()
        peer.window.preferredSize = Dimension(WIDTH, HEIGHT)
    }

    override fun createCenterPanel(): JComponent {
        panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

        progressPanel = JPanel()
        progressPanel.layout = GridBagLayout()

        progressLabel = JBLabel(
            "<html>Loading Github-App <b>VisualBoost</b> ...</html>",
            AnimatedIcon.Default(),
            SwingConstants.LEFT
        )
        progressPanel.add(progressLabel)

        panel.add(progressPanel)

        val webview = initBrowser()
        panel.add(webview.component)

        showLoadingLabel(true)
        loadGithubOAuthApp()
        return panel
    }

    private fun loadGithubOAuthApp() {
        val githubOAuthAppUrl = getGithubOAuthAppUrl(userId)
        webView.loadURL(githubOAuthAppUrl)
    }

    private fun initBrowser(): JBCefBrowser {
        browser = VbWebview(disposable)
        webView = browser.browser
        jsQuery = JBCefJSQuery.create((webView as JBCefBrowserBase))

        injectJsEvent()

        return webView
    }

    private fun injectJsEvent() {
        jsQuery.addHandler {
            val authData = initGithubAuthDataFromJson(it)
            SwingUtilities.invokeLater {
                onSuccess?.invoke(authData)
            }
            null
        }

        webView.jbCefClient.addLoadHandler(object : CefLoadHandler {
            override fun onLoadingStateChange(
                browser: CefBrowser?,
                isLoading: Boolean,
                canGoBack: Boolean,
                canGoForward: Boolean
            ) {
            }

            override fun onLoadStart(
                browser: CefBrowser?,
                frame: CefFrame?,
                transitionType: CefRequest.TransitionType?
            ) {
            }

            override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
                val injectedJavaScript =
                    """
                            window.addEventListener('github_ok_btn_pressed', function (e) {
                                const githubData = e.detail;
                                ${jsQuery.inject("JSON.stringify(githubData)")}
                            });
    
                    """.trimIndent()

                webView.cefBrowser.executeJavaScript(
                    injectedJavaScript,
                    webView.cefBrowser.url, 0
                );

                SwingUtilities.invokeLater {
                    showLoadingLabel(false)
                }
            }

            override fun onLoadError(
                browser: CefBrowser?,
                frame: CefFrame?,
                errorCode: CefLoadHandler.ErrorCode?,
                errorText: String?,
                failedUrl: String?
            ) {
            }

        }, webView.cefBrowser)
    }


    private fun initGithubAuthDataFromJson(json: String): GithubAuthData {
        val type = object : TypeToken<GithubAuthData>() {}.type
        return GSON.gson.fromJson<GithubAuthData>(json, type)
    }

    private fun showLoadingLabel(loading: Boolean) {
        progressPanel.isVisible = loading
        webView.component.isVisible = !loading
    }


}