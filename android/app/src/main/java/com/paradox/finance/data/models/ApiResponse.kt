package com.paradox.finance.data.models

import com.google.gson.annotations.SerializedName

data class DataEnvelope<T>(
    @SerializedName("data") val data: T
)

data class PaginatedEnvelope<T>(
    @SerializedName("data") val data: List<T> = emptyList(),
    @SerializedName("meta") val meta: PaginationMeta? = null
)

data class PaginationMeta(
    @SerializedName("page") val page: Int = 1,
    @SerializedName("page_size") val pageSize: Int = 50,
    @SerializedName("total_items") val totalItems: Int = 0,
    @SerializedName("total_pages") val totalPages: Int = 1
)
