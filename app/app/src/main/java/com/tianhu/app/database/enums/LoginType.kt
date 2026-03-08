package com.tianhu.app.database.enums

enum class LoginType(val value: String) {
    GUEST("GUEST"),
    PHONE("PHONE"),
    WECHAT("WECHAT"),
    ALIPAY("ALIPAY");

    companion object {
        fun fromValue(value: String): LoginType {
            return values().find { it.value == value } ?: GUEST
        }
    }
}