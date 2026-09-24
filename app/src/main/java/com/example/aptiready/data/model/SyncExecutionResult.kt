package com.example.aptiready.data.model

enum class SyncExecutionResult {
    COMPLETE,
    MORE_WORK_REMAINING,
    TRANSIENT_FAILURE,
    PERMANENT_FAILURE
}