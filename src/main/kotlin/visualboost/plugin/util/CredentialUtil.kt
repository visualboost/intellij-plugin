package visualboost.plugin.util

import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.remoteServer.util.CloudConfigurationUtil.createCredentialAttributes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import visualboost.plugin.api.models.LoginRequestBody
import visualboost.plugin.settings.VbAppSettings


object CredentialUtil {

    const val VISUALBOOST = "VISUALBOOST"
    const val GITHUB_ACCESSTOKEN = "GITHUB_ACCESSTOKEN"

    fun storeCredentials(key: String, password: String) {
        val attributes = createCredentialAttributes(VISUALBOOST, key)
        val credentials = Credentials(key, password)
        PasswordSafe.instance.set(attributes!!, credentials)
    }

    fun storeVisualBoostCredentials(username: String, password: String) {
        VbAppSettings.getInstance().username = username
        storeCredentials(username, password)
    }

    suspend fun clearCredentials() {
        val username = getVBCredentials()?.email ?: return
        val attributes = createCredentialAttributes(VISUALBOOST, username)

        VbAppSettings.getInstance().username = null
        PasswordSafe.instance.set(attributes!!, null)
    }

    suspend fun getCredentials(key: String): String? {
        val attributes = createCredentialAttributes(VISUALBOOST, key) ?: return null
        val passwordSafe = PasswordSafe.instance

        val credentials = withContext(Dispatchers.IO){
            passwordSafe[attributes]
        }?: return null

        return credentials.getPasswordAsString()
    }

    suspend fun getVBCredentials(): LoginRequestBody? {
        val username = VbAppSettings.getInstance().username ?: return null
        val password = getCredentials(username) ?: return null

        return LoginRequestBody(username, password)
    }

    fun storeGithubAccessToken(accessToken: String) {
        storeCredentials(GITHUB_ACCESSTOKEN, accessToken)
    }

    suspend fun getGithubAccessToken(accessToken: String): String? {
        return getCredentials(GITHUB_ACCESSTOKEN)
    }


    suspend fun isUserLoggedIn(): Boolean {
        return getVBCredentials() != null
    }

    fun getUsername(): String? {
        return VbAppSettings.getInstance().username
    }
}