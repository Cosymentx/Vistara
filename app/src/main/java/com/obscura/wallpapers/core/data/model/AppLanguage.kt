package com.obscura.wallpapers.core.data.model

/**
 * 应用语言设置
 */
enum class AppLanguage(val code: String) {
    SYSTEM("system"),      // 系统默认
    ENGLISH("en"),   // 英文
    JAPANESE("ja"),  // 日文
    KOREAN("ko"),   // 韩文
    CHINESE("zh"),  // 中文
    INDONESIAN("in"); // 印尼语

    fun apiCode(): String {
        println("apiCode")
        return code
    }

    fun apiIsSystem(): Boolean = this == SYSTEM
}
