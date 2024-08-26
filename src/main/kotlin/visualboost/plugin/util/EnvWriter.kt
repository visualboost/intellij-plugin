package visualboost.plugin.util

import create
import visualboost.plugin.api.models.project.ProjectConfigResponseBody
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths


object EnvWriter {

    const val ENV_FILE_NAME = ".env"
    const val HTTP_FILE_NAME = "http-client.env.json"

    fun writeMainEnvFile(rootDir: File, config: ProjectConfigResponseBody.Data.ProjectConfig): File? {
        val mainEnvTemplate = EnvWriter::class.java.getResource("/templates/main_env.template")?.readText() ?: return null

        val envContent = mainEnvTemplate.replaceAll(
            Pair("\${dir_routes_gen}", config.directories.routesDirs.generation),
            Pair("\${dir_routes_ext}", config.directories.routesDirs.extension),
            Pair("\${http_port}", config.server.port.toString()),
            Pair("\${db_domain}", config.server.domain),
            Pair("\${db_port}", config.database.port.toString()),
            Pair("\${db_name}", config.database.dbName),
            Pair("\${db_user}", config.database.dbUser),
            Pair("\${db_password}", config.database.dbPassword),
            Pair("\${api_key}", config.security.apiKey),
        )

        val file = initFile(rootDir, "backend", ENV_FILE_NAME)
        if(file.exists()) return null

        file.create()
        file.writeText(envContent)

        return file
    }

    fun writeDbEnvFile(rootDir: File, config: ProjectConfigResponseBody.Data.ProjectConfig): File? {
        val dbEnvTemplate = EnvWriter::class.java.getResource("/templates/db_env.template")?.readText() ?: return null

        val envContent = dbEnvTemplate.replaceAll(
            Pair("\${db_port}", config.database.port.toString()),
            Pair("\${db_user}", config.database.dbUser),
            Pair("\${db_password}", config.database.dbPassword),
        )

        val file = initFile(rootDir, "db", ENV_FILE_NAME)
        if(file.exists()) return null

        file.create()
        file.writeText(envContent)

        return file
    }

    fun httpEnvFile(rootDir: File, config: ProjectConfigResponseBody.Data.ProjectConfig): File? {
        val httpEnvFileTemplate = EnvWriter::class.java.getResource("/templates/http_env.template")?.readText() ?: return null

        val envContent = httpEnvFileTemplate.replaceAll(Pair("\${api_key}", config.security.apiKey))

        val file = initFile(rootDir, "backend", HTTP_FILE_NAME)
        if(file.exists()) return null

        file.create()
        file.writeText(envContent)

        return file
    }

    private fun initFile(rootDir: File, vararg pathSegments: String): File {
        val path: Path = Paths.get(rootDir.absolutePath, *pathSegments)
        return path.toFile()
    }

}