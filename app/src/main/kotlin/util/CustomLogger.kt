package util

import java.io.OutputStream

class CustomLogger {
    companion object {
        private var logLevel: LogLevel = LogLevel.DEBUG
        private var outputStream: OutputStream = System.out

        fun setLevel(level: LogLevel) {
            this.logLevel = level
        }

        fun setOutputStream(outputStream: OutputStream) {
            this.outputStream = outputStream
        }

        fun debug(message: String) {
            printMessage(LogLevel.DEBUG, message)
        }

        fun info(message: String) {
            printMessage(LogLevel.INFO, message)
        }

        fun error(message: String) {
            printMessage(LogLevel.ERROR, message)
        }

        private fun printMessage(level: LogLevel, message: String) {
            if (!logLevel.shouldLog(level)) {
                return
            }

            val message = "[$level] $message ${System.lineSeparator()}"
            outputStream.write(message.toByteArray())
        }
    }

}

/**
 * DEBUG 면, DEBUG & INFO & ERROR
 * INFO 면, INFO & ERROR
 */
enum class LogLevel(val level: Int) {
    NONE(0),
    DEBUG(1),
    INFO(2),
    ERROR(3);

    fun shouldLog(target: LogLevel) = level <= target.level
}