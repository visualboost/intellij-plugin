package visualboost.plugin.models

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import visualboost.plugin.models.serializer.GenerationProcessDeserializer

object GSON {

    var gson: Gson = initGson()

    private fun initGson(): Gson {
        val gsonBuilder = getDefaultGsonBuilder()
        return gsonBuilder.create()
    }

    private fun getDefaultGsonBuilder(): GsonBuilder {
        val gsonBuilder = GsonBuilder().serializeNulls().setPrettyPrinting().disableHtmlEscaping()
        gsonBuilder.registerTypeAdapter(GenerationProcess::class.java, GenerationProcessDeserializer())
        return gsonBuilder
    }

    fun <T> deserialize(json: String, clazz: Class<T>): T {
        return gson.fromJson(json, clazz)
    }

    fun <T> deserialize(json: JsonElement, clazz: Class<T>): T {
        return gson.fromJson(json, clazz)
    }

    fun serialize(obj: Any): String {
        return gson.toJson(obj)
    }

    fun <T> deserializeOrNull(json: JsonElement?, clazz: Class<T>): T? {
        return gson.fromJson(json, clazz)
    }

    fun JsonObject.getIfNotNull(key: String?): JsonElement? {
        return if (!this.has(key) || this.get(key).isJsonNull) null else this.get(key);
    }
}