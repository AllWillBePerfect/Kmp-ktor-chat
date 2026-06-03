package org.example.project.v2.platform

class AppLoggerJvm : AppLogger {
    override fun v(tag: String, message: String) {
        print("VERBO", CYAN, tag, message)
    }

    override fun d(tag: String, message: String) {
        print("DEBUG", BLUE, tag, message)
    }

    override fun i(tag: String, message: String) {
        print("INFO ", GREEN, tag, message)
    }

    override fun w(tag: String, message: String) {
        print("WARN ", YELLOW, tag, message)
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        print("ERROR", RED, tag, message)
        throwable?.printStackTrace()
    }

    private fun print(level: String, color: String, tag: String, message: String) {
        println("$color$level$RESET [$tag] $message")
    }

    private companion object {
        const val RESET = "\u001B[0m"
        const val RED = "\u001B[31m"
        const val GREEN = "\u001B[32m"
        const val YELLOW = "\u001B[33m"
        const val BLUE = "\u001B[34m"
        const val CYAN = "\u001B[36m"
    }
}
