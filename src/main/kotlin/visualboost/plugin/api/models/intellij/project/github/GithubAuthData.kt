package visualboost.plugin.api.models.intellij.project.github

data class GithubAuthData(
    val githubAccessToken: String,
    val githubEmail: String,
    val githubUserId: String,
    val githubUsername: String,
    val userId: String,
    var repoNameData: GithubRepoNameData? = GithubRepoNameData(),
    var repoUrlData: GithubRepoUrlData? = GithubRepoUrlData()
) {
    data class GithubRepoNameData(
        var backendRepoName: String? = null
    ){
        fun isValid(): Boolean{
            return backendRepoName.isNullOrBlank().not()
        }
    }

    data class GithubRepoUrlData(
        var repo: GithubRepository? = null
    ){
        fun isValid(): Boolean{
            return repo != null && repo?.url != null
        }
    }
}