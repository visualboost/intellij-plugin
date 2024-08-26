package visualboost.plugin.api.models.user

data class ActivationState(val state: NAME){
    enum class NAME{
        ACTIVE,
        INACTIVE,
        DEACTIVATED
    }

}
