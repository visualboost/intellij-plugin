package visualboost.plugin.livetemplate.macros

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.template.*
import com.intellij.codeInsight.template.macro.EnumMacro
import com.intellij.codeInsight.template.macro.MacroBase.getTextResult
import visualboost.plugin.livetemplate.VisualBoostLiveTemplateContext


class RequestBodyMacro : EnumMacro() {





//    override fun calculateResult(p0: Array<out Expression>, p1: ExpressionContext?, p2: Boolean): Result? {
//
//        // Retrieve the text from the macro or selection, if any is available.
//        var text = getTextResult(p0, p1, true) ?: return null
//        if (!text.isEmpty()) {
//            // Capitalize the start of every word
//            text = text.toUpperCase()
//        }
//
//        return TextResult(text)
//    }

    override fun isAcceptableInContext(context: TemplateContextType?): Boolean {
        return context is VisualBoostLiveTemplateContext
    }

}