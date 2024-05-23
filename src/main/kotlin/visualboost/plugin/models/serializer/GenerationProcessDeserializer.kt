package visualboost.plugin.models.serializer

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import visualboost.plugin.models.GenerationProcess
import visualboost.plugin.models.GenerationTarget
import java.lang.reflect.Type

class GenerationProcessDeserializer : JsonDeserializer<GenerationProcess> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): GenerationProcess {

        val projectId = json.asJsonObject.get("projectId").asString
        val processType = json.asJsonObject.get("processType").asString
        val target = json.asJsonObject.get("target").asString
        val version = json.asJsonObject.get("version").asString
        val branch = json.asJsonObject.get("branch").asString

        return GenerationProcess(projectId, processType, GenerationTarget.fromValue(target), version, branch)
    }
}