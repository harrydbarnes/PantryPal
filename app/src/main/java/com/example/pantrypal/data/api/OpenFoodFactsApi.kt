package com.example.pantrypal.data.api

import retrofit2.http.GET
import retrofit2.http.Path

interface OpenFoodFactsApi {
    @GET("api/v0/product/{barcode}.json")
    suspend fun getProduct(@Path("barcode") barcode: String): ProductResponse
}

data class ProductResponse(val product: ProductData?)

data class ProductData(
    val product_name: String?,
    val brands: String?,
    val image_url: String?
)
