package visualboost.plugin.api.models.project

data class ProjectConfigResponseBody(val data: Data) {
    data class Data(val getExpressProjectConfig: ProjectConfig) {
        data class ProjectConfig(
            val nodeVersion: String,
            val name: String,
            val directories: Directories,
            val server: Server,
            val database: Database,
            val security: Security
        ) {

            data class Directories(
                val prefix: String,
                val dbDir: String,
                val routesDirs: RouteDirs,
                val httpDir: String
            ) {
                data class RouteDirs(val extension: String, val generation: String)
            }

            data class Server(val domain: String, val port: Int)
            data class Database(val dbName: String, val port: Int, val dbUser: String, val dbPassword: String)
            data class Security(val apiKey: String)


        }
    }
}
