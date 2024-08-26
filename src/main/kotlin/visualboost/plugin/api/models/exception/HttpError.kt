package visualboost.plugin.api.models.exception

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class HttpError(val message: String) {

    companion object {

        fun fromString(json: String): HttpError {
            val type = object : TypeToken<HttpError>() {}.type
            return Gson().fromJson<HttpError>(json, type)
        }

    }
}
