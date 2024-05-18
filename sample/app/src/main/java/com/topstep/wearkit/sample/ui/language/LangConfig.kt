package com.topstep.wearkit.sample.ui.language

import java.util.Locale

object LangConfig {

    fun getLanguageId(language: String): Byte {
        var languageType = language
        var languageValue: Byte = 0x03
        if (language == "system") {
            languageType = getLanguageTypeByLocale(Locale.getDefault())
        }
        when (languageType) {
            "en" -> languageValue = 0x03
            "zh" -> languageValue = 0x01
            else -> {}
        }
        return languageValue
    }

    fun convertWmLanguage(language: Byte): String {
        return when (language.toInt() and 0xff) {
            0x03 -> "en"
            0x01 -> "zh"
            else -> "en"
        }
    }

    private fun getLanguageTypeByLocale(locale: Locale): String {
        if (isLocaleLanguageEqual(locale, Locale.CHINESE)) {
            return "zh"
        } else if (isLocaleLanguageEqual(locale, Locale.ENGLISH)) {
            return "en"
        }
        return "en"
    }

    private fun isLocaleLanguageEqual(locale1: Locale, locale2: Locale): Boolean {
        return locale1.language == locale2.language
    }
}