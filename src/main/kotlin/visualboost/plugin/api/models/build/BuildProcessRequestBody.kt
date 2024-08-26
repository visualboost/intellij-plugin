package visualboost.plugin.api.models.build

data class BuildProcessRequestBody(val processes: List<BuildProcessDescription>) {

    companion object {
        fun init(vararg processes: BuildProcessDescription): BuildProcessRequestBody {
            return BuildProcessRequestBody(processes.toList())
        }
    }
}