package visualboost.plugin.api.models

data class AllProjectsResponseBody(val data: Data){
    data class Data(val allProjects: List<Project>) {
        data class Project(val _id: String?, val name: String)
    }
}
