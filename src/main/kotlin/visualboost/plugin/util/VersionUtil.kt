package visualboost.plugin.util

import com.intellij.openapi.application.ApplicationInfo
import kotlin.math.min

/**
 * Validates if the current ide version is equal or lower than the given
 */
fun ApplicationInfo.isVersionOrLower(majorVersion: String, minorVersion: String): Boolean{
    val currentIdeVersion = "${ApplicationInfo.getInstance().majorVersion}${ApplicationInfo.getInstance().minorVersion}".toInt()
    val aimingVersion = "$majorVersion$minorVersion".toInt()

    return currentIdeVersion <= aimingVersion
}