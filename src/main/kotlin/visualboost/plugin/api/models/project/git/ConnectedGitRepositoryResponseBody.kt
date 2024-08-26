package visualboost.plugin.api.models.project

data class ConnectedGitRepositoryResponseBody(val data: Data) {
    data class Data(val connectGitRepository: ConnectedGitRepository) {
        data class ConnectedGitRepository(
            val type: String,
            val repository: String,
            val branch: String,
            val branches: List<String>,
        )
    }
}
