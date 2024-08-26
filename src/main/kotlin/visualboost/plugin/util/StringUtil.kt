package visualboost.plugin.util

import com.intellij.openapi.project.Project
import java.io.BufferedReader
import java.io.File
import java.rmi.UnexpectedException
import java.util.concurrent.TimeUnit
import kotlin.random.Random

fun String.runCommand(): String {
    val process = ProcessBuilder(*this.split(" ").toTypedArray()).start()

    process.waitFor(20, TimeUnit.SECONDS)
    val returnValue = process.exitValue()

    if (returnValue != 0) {
        val errorMsg = BufferedReader(process.errorStream.reader()).readText()
        throw UnexpectedException(errorMsg)
    }

    val result = BufferedReader(process.inputStream.reader()).readText()
    return result
}

fun String.runCommand(project: Project): String {
    val workingDir = File(project.basePath)

    val process = ProcessBuilder(*this.split(" ").toTypedArray())
        .directory(workingDir)
//            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .start()

    process.waitFor(20, TimeUnit.SECONDS)
    val returnValue = process.exitValue()

    if (returnValue != 0) {
        val errorMsg = BufferedReader(process.errorStream.reader()).readText()
        throw UnexpectedException(errorMsg)
    }

    val result = BufferedReader(process.inputStream.reader()).readText()
    return result
}

fun String.replaceAll(vararg toReplace: Pair<String, String>): String {
    var templateWithPlaceholders = this
    toReplace.forEach {
        templateWithPlaceholders = templateWithPlaceholders.replace(it.first, it.second)
    }
    return templateWithPlaceholders
}

fun String.Companion.random(length: Int = 1024): String {
    val alphaNumeric = ('0'..'9') + ('A'..'Z') + ('a'..'z')
    return List(length) { Random.nextInt(0, alphaNumeric.size) }.map { alphaNumeric[it] }.joinToString("")
}
