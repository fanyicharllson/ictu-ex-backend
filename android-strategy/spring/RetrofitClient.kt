package com.fanyiadrien.ictu_ex.data.remote.spring

import com.fanyiadrien.ictu_ex.data.remote.api.AuthUser
import com.fanyiadrien.ictu_ex.data.remote.api.Conversation
import com.fanyiadrien.ictu_ex.data.remote.api.Listing
import com.fanyiadrien.ictu_ex.data.remote.api.Message
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Builds the Retrofit instance pointed at the live Spring Boot API.
 * Base URL: https://api.ictuex.teamnest.me
 */
object RetrofitClient {

    const val BASE_URL = "https://api.ictuex.teamnest.me/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val instance: IctuExApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(IctuExApiService::class.java)
    }
}
