package com.tianhu.app.database.enums

enum class SyncStatus(val value: Int) {
    NOT_SYNCED(0),
    SYNCED(1),
    SYNC_FAILED(2);

    companion object {
        fun fromValue(value: Int): SyncStatus {
            return values().find { it.value == value } ?: NOT_SYNCED
        }
    }
}