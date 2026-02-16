package com.augmentalis.pdfavanue.model

import kotlinx.serialization.Serializable

@Serializable
data class PdfDocument(
    val uri: String,
    val title: String = "",
    val author: String = "",
    val pageCount: Int = 0,
    val fileSizeBytes: Long = 0,
    val isPasswordProtected: Boolean = false
)

@Serializable
data class PdfPage(
    val index: Int,
    val widthPoints: Float,
    val heightPoints: Float
) {
    val aspectRatio: Float get() = if (heightPoints > 0f) widthPoints / heightPoints else 1f
}

@Serializable
data class PdfSearchResult(
    val pageIndex: Int,
    val matchText: String,
    val matchIndex: Int
)
