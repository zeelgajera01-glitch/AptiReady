package com.example.aptiready.ui.ads

object AdLogger {
    var isTestMode: Boolean = false

    fun d(tag: String, msg: String) {
        if (isTestMode) {
            println("[$tag] $msg")
        } else {
            try {
                android.util.Log.d(tag, msg)
            } catch (_: Throwable) {
                println("[$tag] $msg")
            }
        }
    }

    fun w(tag: String, msg: String) {
        if (isTestMode) {
            println("WARN: [$tag] $msg")
        } else {
            try {
                android.util.Log.w(tag, msg)
            } catch (_: Throwable) {
                println("WARN: [$tag] $msg")
            }
        }
    }
}
