package com.augmentalis.fileavanue.model

internal actual fun String.Companion.format(format: String, vararg args: Any?): String =
    java.lang.String.format(format, *args)
