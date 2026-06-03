package org.example.project.v2.platform

import kotlin.LazyThreadSafetyMode.NONE

interface TaggedLogger {
    fun v(message: () -> String)
    fun d(message: () -> String)
    fun i(message: () -> String)
    fun w(message: () -> String)
    fun e(message: () -> String)
    fun e(throwable: Throwable, message: () -> String)
}

fun taggedLogger(tag: String): Lazy<TaggedLogger> = lazy(NONE) {
    V2Logs.tagged(tag)
}

object V2Logs {
    @Volatile
    private var backend: AppLogger = NoOpAppLogger

    @Volatile
    private var level: AppLogLevel = AppLogLevel.ALL

    fun install(logger: AppLogger, level: AppLogLevel = AppLogLevel.ALL) {
        backend = logger
        this.level = level
    }

    fun tagged(tag: String): TaggedLogger = TaggedLoggerImpl(
        tag = tag,
        loggerProvider = { backend },
        levelProvider = { level },
    )
}

private class TaggedLoggerImpl(
    private val tag: String,
    private val loggerProvider: () -> AppLogger,
    private val levelProvider: () -> AppLogLevel,
) : TaggedLogger {
    override fun v(message: () -> String) {
        if (levelProvider().allowsVerbose()) {
            loggerProvider().v(tag, message())
        }
    }

    override fun d(message: () -> String) {
        if (levelProvider().allowsDebug()) {
            loggerProvider().d(tag, message())
        }
    }

    override fun i(message: () -> String) {
        if (levelProvider().allowsInfo()) {
            loggerProvider().i(tag, message())
        }
    }

    override fun w(message: () -> String) {
        if (levelProvider().allowsWarn()) {
            loggerProvider().w(tag, message())
        }
    }

    override fun e(message: () -> String) {
        if (levelProvider().allowsError()) {
            loggerProvider().e(tag, message())
        }
    }

    override fun e(throwable: Throwable, message: () -> String) {
        if (levelProvider().allowsError()) {
            loggerProvider().e(tag, message(), throwable)
        }
    }
}

private object NoOpAppLogger : AppLogger {
    override fun v(tag: String, message: String) = Unit
    override fun d(tag: String, message: String) = Unit
    override fun i(tag: String, message: String) = Unit
    override fun w(tag: String, message: String) = Unit
    override fun e(tag: String, message: String, throwable: Throwable?) = Unit
}

private fun AppLogLevel.allowsVerbose(): Boolean = this == AppLogLevel.ALL

private fun AppLogLevel.allowsDebug(): Boolean = when (this) {
    AppLogLevel.ALL,
    AppLogLevel.DEBUG,
    -> true
    AppLogLevel.WARN,
    AppLogLevel.ERROR,
    AppLogLevel.NOTHING,
    -> false
}

private fun AppLogLevel.allowsInfo(): Boolean = this == AppLogLevel.ALL

private fun AppLogLevel.allowsWarn(): Boolean = when (this) {
    AppLogLevel.ALL,
    AppLogLevel.DEBUG,
    AppLogLevel.WARN,
    -> true
    AppLogLevel.ERROR,
    AppLogLevel.NOTHING,
    -> false
}

private fun AppLogLevel.allowsError(): Boolean = when (this) {
    AppLogLevel.ALL,
    AppLogLevel.DEBUG,
    AppLogLevel.WARN,
    AppLogLevel.ERROR,
    -> true
    AppLogLevel.NOTHING,
    -> false
}
