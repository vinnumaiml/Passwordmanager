package com.novavault.domain.model

data class Folder(
    val id: String,
    val name: String,
    val parentId: String? = null,
    val userId: String,
)
