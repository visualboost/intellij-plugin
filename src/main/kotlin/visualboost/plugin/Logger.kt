package visualboost.plugin

import com.intellij.openapi.application.PathManager
import com.jetbrains.rd.util.string.printToString
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.io.path.Path

class Logger constructor() {

    companion object {
        val logPath = File(PathManager.getLogPath())
        val defaultLogFileName = "visualboost.log"
    }

    enum class LEVEL {
        DEBUG,
        INFO,
        WARNING,
        ERROR
    }

    enum class OUTPUT {
        CONSOLE,
        FILE
    }

    private val output = mutableListOf<OUTPUT>()
    private var logFileName : String? = null

    fun enableConsole(enable: Boolean): Logger{
        if(enable){
            output.add(OUTPUT.CONSOLE)
        }else{
            output.remove(OUTPUT.CONSOLE)
        }

        return this
    }

    fun useDefaultLogFile(): Logger{
        setLogFile(defaultLogFileName)
        return this
    }

    fun getLogFile(): File {
        return Path(logPath.absolutePath, logFileName ?: defaultLogFileName).toAbsolutePath().toFile()
    }

    fun setLogFile(fileName: String? = null){
        if(fileName == null){
            output.remove(OUTPUT.FILE)
            logFileName = null
            return
        }

        logFileName = fileName
        val logFile = getLogFile()
        if(!output.contains(OUTPUT.FILE)){
            output.add(OUTPUT.FILE)
        }

        if(logFile.exists().not()){
            logFile.createNewFile()
        }
    }

    fun clearLogFile() {
        val logFile = getLogFile()
        if(logFile.exists()){
            logFile.writeText("")
        }
    }

    fun clearAllLogs() {
        val logFiles = logPath.walk(FileWalkDirection.TOP_DOWN).filter { it.name.startsWith("visualboost") && it.extension === "log" }
        logFiles.forEach { it.delete() }
    }

    fun log(level: LEVEL, message: String, clazz: Class<*>? = null){
        val formattedMsg = if(clazz == null){
            "[$level] - ${SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'").format(Date())} - $message"
        }else{
            "[$level] - ${SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'").format(Date())} - ${clazz.name} - $message"
        }

        log(level, formattedMsg)
    }

    fun log(level: LEVEL, message: String){
        if(output.contains(OUTPUT.CONSOLE) && level !== LEVEL.ERROR){
            println(message)
        }else if(output.contains(OUTPUT.CONSOLE) && level === LEVEL.ERROR){
            System.err.println(message)
        }

        if(output.contains(OUTPUT.FILE)){
            getLogFile().appendText(message + "\n")
        }
    }

    fun debug(msg: String, clazz: Class<*>? = null){
        log(LEVEL.DEBUG, msg, clazz)
    }

    fun error(msg: String, clazz: Class<*>? = null){
        log(LEVEL.ERROR, msg, clazz)
    }

    fun error(err: Exception, clazz: Class<*>? = null){
        log(LEVEL.ERROR, err.printToString(), clazz)
    }


}