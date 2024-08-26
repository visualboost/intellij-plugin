package visualboost.plugin.api.models.plan

import com.google.gson.Gson

data class Plan(val name: NAME, val features: List<String>){

    enum class NAME{
        FREE,
        ENTERPRISE
    }

    fun toJsonString(): String {
        return Gson().toJson(this)
    }
}
