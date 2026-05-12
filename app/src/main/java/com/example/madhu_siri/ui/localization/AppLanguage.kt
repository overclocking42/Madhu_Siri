package com.example.madhu_siri.ui.localization

enum class AppLanguage(
    val tag: String,
    val nativeLabel: String,
    val englishLabel: String
) {
    ENGLISH("en", "English", "English"),
    KANNADA("kn", "ಕನ್ನಡ", "Kannada"),
    HINDI("hi", "हिन्दी", "Hindi"),
    TELUGU("te", "తెలుగు", "Telugu"),
    TAMIL("ta", "தமிழ்", "Tamil");

    companion object {
        fun fromTag(tag: String?): AppLanguage = entries.firstOrNull { it.tag == tag } ?: ENGLISH
    }
}
