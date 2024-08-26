package visualboost.plugin.api.models.project

data class VisualBoostProjectResponseBody(val data: Data) {
    data class Data(val createExpressProject: VisualBoostProject) {
        data class VisualBoostProject(
            val _id: String,
            val name: String
        )
    }
}
