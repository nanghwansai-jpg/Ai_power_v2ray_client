package com.aipower.v2ray.core.agent

import kotlin.system.exitProcess

/** Captures a minimal, redacted fatal event and always delegates normal process termination. */
class AgentCrashHandler(
    private val store: AgentLocalStore,
    private val delegate: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        runCatching { store.logFatal(thread.name, throwable.javaClass.simpleName) }
        if (delegate != null) delegate.uncaughtException(thread, throwable) else exitProcess(10)
    }
}
