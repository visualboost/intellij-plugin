package visualboost.plugin.util

import com.intellij.openapi.project.Project
import java.io.File

/**
 * Initialize a local git repository
 */
fun Project.initGitRepository() {
    "git init -b main".runCommand(this)
}

fun Project.addGitOrigin(originUrl: String) {
    "git remote add origin $originUrl".runCommand(this)
}

fun Project.pull(branch: String) {
    "git pull origin $branch:$branch".runCommand(this)
}

fun Project.currentBranch(): String {
    return "git branch --show-current".runCommand(this).replace("\n", "")
}

fun Project.fetch(branch: String) {
    "git fetch origin $branch:$branch".runCommand(this)
}

fun Project.unstage(): String {
    return "git reset".runCommand(this)
}

fun Project.stage(vararg file: File): String {
    return "git add ${file.joinToString(" ") { it.absolutePath }}".runCommand(this)
}

/**
 * Get the difference of an unstages filed
 */
fun Project.getDiff(file: File): String {
    return "git diff ${file.absolutePath}".runCommand(this)
}

/**
 * Get the difference of an unstages filed
 */
fun Project.getDiffStaged(file: File): String {
    return "git diff --cached ${file.absolutePath}".runCommand(this)
}

/**
 * Validate if the current file is already tracked by Git
 */
fun Project.isTracked(file: File): Boolean{
    val result = "git ls-files --others --exclude-standard ${file.absolutePath}".runCommand(this)
    return result.isBlank()
}

/**
 * Checks if the file (staged and unstaged) is modified
 */
fun Project.hasDiffs(file: File): Boolean {
    return this.getDiff(file).isNotBlank() || this.getDiffStaged(file).isNotBlank()
}

fun Project.commit(message: String): String {
    val command = """git commit -m "$message""""
    return command.runCommand(this)
}

fun Project.getCurrentBranch(): String{
    return "git rev-parse --abbrev-ref HEAD".runCommand(this)
}

fun Project.push(branch: String): String {
    return "git push origin $branch:$branch".runCommand(this)
}

fun Project.pushCurrentBranch(): String {
    val currentBranch = this.getCurrentBranch().replace("\n", "")
    if(currentBranch.isBlank()) throw NullPointerException("Can't determine the current branch")

    return "git push origin $currentBranch:$currentBranch".runCommand(this)
}