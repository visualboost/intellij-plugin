package visualboost.plugin.components

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefBrowser

class VbWebview(parentDisposeable: Disposable): Disposable {

    val browser: JBCefBrowser

    init {
        browser = JBCefBrowser()
        Disposer.register(parentDisposeable, this)
    }

    override fun dispose() {
        browser.jbCefClient.removeAllHandlers(browser.cefBrowser)
        browser.jbCefClient.dispose()
        browser.dispose()
        browser.cefBrowser.close(true)
    }


}