package visualboost.plugin.models

enum class GenerationTarget(val value: String) {
    SERVER("SERVER"),
    CLIENT("CLIENT");

    companion object{
        fun fromValue(value: String): GenerationTarget {
            return GenerationTarget.values().find { it.value == value } ?: throw NullPointerException("Invalid GenerationTarget $value")
        }
    }
}