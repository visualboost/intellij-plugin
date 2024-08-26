package visualboost.plugin.api.models.model

data class AllModelsResponseBody(val data: Data){
    data class Data(val allModels: List<Model>) {
        data class Model(val _id: String, val name: String, val pkg: Package?)
    }
}
