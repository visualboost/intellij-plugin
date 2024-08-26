package visualboost.plugin.intellijProject

import visualboost.plugin.api.models.intellij.project.github.GithubAuthData
import visualboost.plugin.api.models.intellij.project.github.GithubRepository
import visualboost.plugin.api.models.intellij.project.github.UserData


//EmptyWebProjectTemplate
data class VBProjectGenerationSettings(var projectDir: String? = null, var projectName: String? = null, var userData: UserData? = null, var githubData: GithubAuthData? = null, val prerequisites: Prerequisites = Prerequisites()){

    class Prerequisites(var gitExists: Boolean = false)

    fun getGithubAccessTokenOrThrowException(): String{
        return githubData?.githubAccessToken ?: throw NullPointerException("No valid Github access token exist. Please login into Github first.")
    }

    fun getVbTokenOrThrowException(): String{
        return userData?.token ?: throw NullPointerException("No valid VisualBoost access token exist. Please login into VisualBoost first.")
    }

    fun getProjectnameOrThrowException(): String{
        return projectName ?: throw NullPointerException("Project name is empty. Please provide a valid Project name first.")
    }

    fun getGithubRepoNameOrThrowException(): String{
        return githubData?.repoNameData?.backendRepoName ?: throw NullPointerException("No repository name exist. Please provide a valid repository name to create a new repository")
    }

    fun getGithubRepoOrThrowException(): GithubRepository{
        return githubData?.repoUrlData?.repo ?: throw NullPointerException("No repository url exist. Please provide a valid repository url to connect VisualBoost with a github repository")
    }

    fun setRepoName(repoName: String?){
        if(githubData!!.repoNameData == null){
            githubData!!.repoNameData = GithubAuthData.GithubRepoNameData()
        }
        githubData!!.repoNameData!!.backendRepoName = repoName
    }

    fun setExistingGithubRepo(githubRepo: GithubRepository?){
        if(githubData!!.repoNameData == null){
            githubData!!.repoUrlData = GithubAuthData.GithubRepoUrlData()
        }
        githubData!!.repoUrlData!!.repo = githubRepo
    }
}

