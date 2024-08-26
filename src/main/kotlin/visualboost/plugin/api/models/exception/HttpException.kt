package visualboost.plugin.api.models.exception

class HttpException(val status: Int, message: String) : Exception(message)
