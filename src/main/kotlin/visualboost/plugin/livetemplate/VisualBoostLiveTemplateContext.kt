package visualboost.plugin.livetemplate

import com.intellij.codeInsight.template.TemplateActionContext
import com.intellij.codeInsight.template.TemplateContextType

class VisualBoostLiveTemplateContext() : TemplateContextType("VisualBoost") {

    override fun isInContext(templateActionContext: TemplateActionContext): Boolean {
        //TODO: check if the file is an extension file
        return templateActionContext.file.name.endsWith(".js");
    }
}