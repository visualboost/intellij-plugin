package visualboost.plugin.api.models.project

data class AuthenticationServiceConfigResponseBody(val data: Data) {
    data class Data(val getAuthenticationServiceConfig: AuthenticationServiceConfig) {
        data class AuthenticationServiceConfig(
            val isEnabled: Boolean,
            val roles: List<Role>
        ){
            data class Role(val name: String, val functions: List<RoleFunction>) {
                data class RoleFunction(val _id: String, val name: String)
            }
        }

    }
}