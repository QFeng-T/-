package com.tianhu.app.database.enums

enum class RecognitionType(val value: Int) {
    LOCAL(0),
    CLOUD(1);

    companion object {
        fun fromValue(value: Int): RecognitionType {
            return values().find { it.value == value } ?: LOCAL
        }
    }
}