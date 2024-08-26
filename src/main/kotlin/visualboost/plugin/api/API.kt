package visualboost.plugin.api

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import visualboost.plugin.api.models.AllProjectsResponseBody
import visualboost.plugin.api.models.exception.HttpError
import visualboost.plugin.api.models.LoginResponseBody
import visualboost.plugin.api.models.build.BuildProcessDescription
import visualboost.plugin.api.models.build.BuildProcessRequestBody
import visualboost.plugin.api.models.build.BuildProcessState
import visualboost.plugin.api.models.build.ProtocolResponseBody
import visualboost.plugin.api.models.exception.HttpException
import visualboost.plugin.api.models.intellij.project.github.GithubRepository
import visualboost.plugin.api.models.model.AllModelsResponseBody
import visualboost.plugin.api.models.plan.Plan
import visualboost.plugin.api.models.project.ConnectedGitRepositoryResponseBody
import visualboost.plugin.api.models.project.git.GitRepositoryType
import visualboost.plugin.api.models.project.ProjectConfigResponseBody
import visualboost.plugin.api.models.project.VisualBoostProjectResponseBody
import visualboost.plugin.api.models.user.ActivationState
import visualboost.plugin.models.GSON
import visualboost.plugin.settings.VbAppSettings
import visualboost.plugin.util.CredentialUtil
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

object API {

    const val PROTOCOL = "https://"
    const val DOMAIN = "app.visualboost.de"
    const val DEFAULT_URL = "$PROTOCOL$DOMAIN"
    const val DEFAULT_AUTH_PATH = "/auth"
    const val DEFAULT_MAIN_PATH = "/main"
    const val DEFAULT_BUILD_PATH = "/build"

    fun getAppUrl(): String {
        val settings = VbAppSettings.getInstance()
        return if (settings.useDefaultUrl) DEFAULT_URL else settings.defaultUrl
    }

    fun getAuthUrl(): String {
        val settings = VbAppSettings.getInstance()
        return if (settings.useDefaultUrl) (DEFAULT_URL + DEFAULT_AUTH_PATH) else settings.authUrl
    }

    fun getMainUrl(): String {
        val settings = VbAppSettings.getInstance()
        return if (settings.useDefaultUrl) (DEFAULT_URL + DEFAULT_MAIN_PATH) else settings.mainUrl
    }

    fun getBuildUrl(): String {
        val settings = VbAppSettings.getInstance()
        //TODO: add build url to settings
        return if (settings.useDefaultUrl) (DEFAULT_URL + DEFAULT_BUILD_PATH) else throw NotImplementedError("Custom BUILD url isn't supported yet")
    }

    fun getPlanUrl(tenantId: String): String {
        return "${getAuthUrl()}/tenant/$tenantId/plan"
    }

    fun getUserStateUrl(userId: String): String {
        return "${getAuthUrl()}/user/$userId/state"
    }

    fun getProjectUrl(projectId: String): String {
        return "${getAppUrl()}/#/project/$projectId"
    }

    fun getGithubOAuthAppUrl(userId: String): String {
        return "${getAuthUrl()}/github/auth/$userId"
    }


    suspend fun login(email: String, password: String): LoginResponseBody {
        val client = HttpClient.newBuilder().build()
        val request = HttpRequest.newBuilder()
            .uri(URI.create(getAuthUrl() + "/login?email=$email&password=$password"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(""))
            .build()

        val sendRequest = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        val response = sendRequest.await()

        val status = response.statusCode()
        val body = response.body()
        if (status != 200) {
            val error = HttpError.fromString(body)
            throw HttpException(status, error.message)
        }
        val type = object : TypeToken<LoginResponseBody>() {}.type
        return Gson().fromJson(body, type)
    }

    suspend fun fetchToken(): String? {
        val loginRequestBody = CredentialUtil.getVBCredentials() ?: return null
        val loginResponseBody = login(loginRequestBody.email, loginRequestBody.password)
        return loginResponseBody.token
    }

    suspend fun fetchProjects(): List<AllProjectsResponseBody.Data.Project> {
        val token = fetchToken()

        val query = "{\"query\":\"query{allProjects{_id name}}\",\"variables\":{}}"
        val client = HttpClient.newBuilder().build()
        val request = HttpRequest.newBuilder()
            .uri(URI.create(getMainUrl()))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $token")
            .POST(HttpRequest.BodyPublishers.ofString(query))
            .build()

        val response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        val jsonResponse = response.await().body()

        val type = object : TypeToken<AllProjectsResponseBody>() {}.type
        val resp = Gson().fromJson<AllProjectsResponseBody>(jsonResponse, type)

        return resp.data.allProjects
    }

    suspend fun getPlan(jwt: String, tenantId: String): Plan {
        val client = HttpClient.newBuilder().build()
        val request = HttpRequest.newBuilder()
            .uri(URI.create(getPlanUrl(tenantId)))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $jwt")
            .GET()
            .build()

        val response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        val jsonResponse = response.await().body()

        val plan = Gson().fromJson<Plan>(jsonResponse, object : TypeToken<Plan>() {}.type)
        return plan
    }

    suspend fun getUserState(jwt: String, userId: String): ActivationState {
        val client = HttpClient.newBuilder().build()
        val request = HttpRequest.newBuilder()
            .uri(URI.create(getUserStateUrl(userId)))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $jwt")
            .GET()
            .build()

        val response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        val jsonResponse = response.await().body()

        return Gson().fromJson(jsonResponse, object : TypeToken<ActivationState>() {}.type)
    }

    suspend fun getProjectConfig(jwt: String, projectId: String): ProjectConfigResponseBody.Data.ProjectConfig {
        val query =
            """query getExpressProjectConfig (${'$'}projectId: ID!) { getExpressProjectConfig (projectId: ${'$'}projectId) { nodeVersion directories { prefix dbDir routesDirs { extension generation } httpDir } server{ domain port } database{ port dbName dbUser dbPassword } security{ apiKey } } }"""
        val body = """{"query": "$query", "variables": {"projectId": "$projectId"}}"""

        val client = HttpClient.newBuilder().build()
        val request = HttpRequest.newBuilder()
            .uri(URI.create(getMainUrl()))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $jwt")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        val response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        val jsonResponse = response.await().body()

        val type = object : TypeToken<ProjectConfigResponseBody>() {}.type
        val resp = Gson().fromJson<ProjectConfigResponseBody>(jsonResponse, type)
        return resp.data.getExpressProjectConfig
    }

    suspend fun getModels(jwt: String, projectId: String): List<AllModelsResponseBody.Data.Model> {
        val query =
            """query allModels (${'$'}projectId: ID!) { allModels (projectId: ${'$'}projectId) { _id name pkg: package { _id name description namespace } } }"""
        val body = """{"query": "$query", "variables": {"projectId": "$projectId"}}"""

        val client = HttpClient.newBuilder().build()
        val request = HttpRequest.newBuilder()
            .uri(URI.create(getMainUrl()))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $jwt")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        val response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        val jsonResponse = response.await().body()


        val type = object : TypeToken<AllModelsResponseBody>() {}.type
        val resp = Gson().fromJson<AllModelsResponseBody>(jsonResponse, type)
        return resp.data.allModels
    }

    suspend fun createNewVisualBoostProject(
        jwt: String,
        name: String
    ): VisualBoostProjectResponseBody.Data.VisualBoostProject {
        val query =
            """mutation createExpressProject (${'$'}name: String!, ${'$'}description: String) { createExpressProject (name: ${'$'}name, description: ${'$'}description) { _id name } }"""
        val body = """{"query": "$query", "variables": {"name": "$name", "description": ""}}"""

        val client = HttpClient.newBuilder().build()
        val request = HttpRequest.newBuilder()
            .uri(URI.create(getMainUrl()))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $jwt")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        val response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        val jsonResponse = response.await().body()

        val type = object : TypeToken<VisualBoostProjectResponseBody>() {}.type
        val resp = Gson().fromJson<VisualBoostProjectResponseBody>(jsonResponse, type)
        return resp.data.createExpressProject
    }

    suspend fun connectGitRepository(
        jwt: String,
        projectId: String,
        type: GitRepositoryType,
        url: String,
        user: String,
        email: String,
        accessToken: String
    ): ConnectedGitRepositoryResponseBody.Data.ConnectedGitRepository {
        val query =
            """mutation connectGitRepository (${'$'}projectId: ID!, ${'$'}type: GitRepositoryType!, ${'$'}repository: String!, ${'$'}user: String, ${'$'}email: String, ${'$'}token: String) { connectGitRepository (projectId: ${'$'}projectId, type: ${'$'}type, repository: ${'$'}repository, user: ${'$'}user, email: ${'$'}email, token: ${'$'}token) { type repository branch branches } }"""
        val body =
            """{"query": "$query", "variables": {"projectId": "$projectId", "type": "$type", "repository": "$url", "user": "$user", "email": "$email", "token": "$accessToken"}}"""

        val client = HttpClient.newBuilder().build()
        val request = HttpRequest.newBuilder()
            .uri(URI.create(getMainUrl()))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $jwt")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        val response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        val jsonResponse = response.await().body()

        val type = object : TypeToken<ConnectedGitRepositoryResponseBody>() {}.type
        val resp = Gson().fromJson<ConnectedGitRepositoryResponseBody>(jsonResponse, type)
        return resp.data.connectGitRepository
    }

    object Build {

        private fun getExecuteBuildProcessUrl(projectId: String): String {
            return "${getBuildUrl()}/generate/$projectId"
        }

        private fun getFetchProtocolUrl(processId: String): String {
            return "${getBuildUrl()}/logs/process/${processId}"
        }

        suspend fun executeBackendBuildProcessAndWait(jwt: String, projectId: String, commitMsg: String) {
            val processId = executeBackendBuildProcess(jwt, projectId, commitMsg)

            var protocol = fetchProtocol(jwt, processId)
            while (protocol.state == BuildProcessState.STARTED) {
                delay(2000)
                protocol = fetchProtocol(jwt, processId)
            }
        }

        suspend fun executeBackendBuildProcess(jwt: String, projectId: String, commitMsg: String): String {
            val requestBody = GSON.gson.toJson(BuildProcessRequestBody.init(BuildProcessDescription.init(commitMsg)))

            val client = HttpClient.newBuilder().build()
            val request = HttpRequest.newBuilder()
                .uri(URI.create(getExecuteBuildProcessUrl(projectId)))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer $jwt")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build()

            val response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            val jsonResponse = response.await().body()

            val processIds = Gson().fromJson<List<String>>(jsonResponse, object : TypeToken<List<String>>() {}.type)
            return processIds.first()
        }

        suspend fun fetchProtocol(jwt: String, processId: String): ProtocolResponseBody {
            val client = HttpClient.newBuilder().build()
            val request = HttpRequest.newBuilder()
                .uri(URI.create(getFetchProtocolUrl(processId)))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer $jwt")
                .GET()
                .build()

            val response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            val jsonResponse = response.await().body()

            return Gson().fromJson(jsonResponse, object : TypeToken<ProtocolResponseBody>() {}.type)
        }


    }

    object Github {

        private fun getListGithubRepositoriesUrl(accessToken: String): String {
            val domain = getAuthUrl()
            return "$domain/github/repos?access_token=$accessToken"
        }

        private fun createGithubRepositoryUrl(accessToken: String): String {
            val domain = getAuthUrl()
            return "$domain/github/repo?access_token=$accessToken"
        }

        suspend fun listGithubRepositories(jwt: String, githubAccessToken: String): List<GithubRepository> {
            val client = HttpClient.newBuilder().build()
            val request = HttpRequest.newBuilder()
                .uri(URI.create(getListGithubRepositoriesUrl(githubAccessToken)))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer $jwt")
                .GET()
                .build()

            val response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            val jsonResponse = response.await().body()

            return Gson().fromJson(jsonResponse, object : TypeToken<List<GithubRepository>>() {}.type)
        }

        suspend fun createGithubRepository(jwt: String, githubAccessToken: String, repoName: String): GithubRepository {
            val client = HttpClient.newBuilder().build()
            val request = HttpRequest.newBuilder()
                .uri(URI.create(createGithubRepositoryUrl(githubAccessToken)))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer $jwt")
                .POST(HttpRequest.BodyPublishers.ofString("""{"name": "$repoName"}"""))
                .build()

            val response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            val jsonResponse = response.await().body()

            return Gson().fromJson(jsonResponse, object : TypeToken<GithubRepository>() {}.type)
        }

    }


}