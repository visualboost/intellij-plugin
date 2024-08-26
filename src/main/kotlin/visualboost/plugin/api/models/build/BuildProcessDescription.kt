package visualboost.plugin.api.models.build

data class BuildProcessDescription(
    var metadata: Metadata?,
    var processType: BuildProcessType = BuildProcessType.CODE_GENERATION,
    var target: ExecutionTarget = ExecutionTarget.SERVER,
    val db: Database = Database.MONGODB,
    val language: Language = Language.JAVASCRIPT,
) {

    companion object {
        fun init(commitMessage: String): BuildProcessDescription {
            return BuildProcessDescription(Metadata(commitMessage, "0.0.1"))
        }
    }

    enum class BuildProcessType {
        CODE_GENERATION
    }

    enum class ExecutionTarget {
        SERVER
    }

    enum class Language {
        JAVASCRIPT
    }

    enum class Database {
        MONGODB
    }
}