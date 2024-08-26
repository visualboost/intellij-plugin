package visualboost.plugin.api.models

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

data class JwtContent(val userId: String, val tenantId: String){

    companion object {
        fun fromJwt(jwt: String): JwtContent{
            val decoder: Base64.Decoder = Base64.getUrlDecoder()
            val chunks = jwt.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val jwtPayload = String(decoder.decode(chunks[1]))

            val type = object : TypeToken<JwtContent>() {}.type
            return Gson().fromJson(jwtPayload, type)
        }
    }

}
