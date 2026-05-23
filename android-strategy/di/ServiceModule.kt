package com.fanyiadrien.ictu_ex.di

import android.content.Context
import com.fanyiadrien.ictu_ex.BuildConfig
import com.fanyiadrien.ictu_ex.data.remote.api.AuthService
import com.fanyiadrien.ictu_ex.data.remote.api.ListingService
import com.fanyiadrien.ictu_ex.data.remote.api.MessagingService
import com.fanyiadrien.ictu_ex.data.remote.firebase.FirebaseAuthService
import com.fanyiadrien.ictu_ex.data.remote.firebase.FirebaseListingService
import com.fanyiadrien.ictu_ex.data.remote.firebase.FirebaseMessagingService
import com.fanyiadrien.ictu_ex.data.remote.spring.IctuExApiService
import com.fanyiadrien.ictu_ex.data.remote.spring.RetrofitClient
import com.fanyiadrien.ictu_ex.data.remote.spring.SpringAuthService
import com.fanyiadrien.ictu_ex.data.remote.spring.SpringListingService
import com.fanyiadrien.ictu_ex.data.remote.spring.SpringMessagingService
import com.fanyiadrien.ictu_ex.data.remote.spring.TokenStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * STEP 4 — THE PROVIDER (The Switcher)
 *
 * This is the ONLY place in the entire app that knows about Firebase vs Spring.
 * Everything else just depends on the interfaces (AuthService, ListingService, etc.)
 *
 * To switch backends:
 *   - In build.gradle (app module), set:
 *       buildConfigField("boolean", "USE_SPRING_BACKEND", "true")   // → Spring Boot
 *       buildConfigField("boolean", "USE_SPRING_BACKEND", "false")  // → Firebase
 *
 * Or use a local.properties / gradle.properties flag:
 *       useSpringBackend=true
 */
@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    // ─── Retrofit ────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideIctuExApiService(): IctuExApiService = RetrofitClient.instance

    @Provides
    @Singleton
    fun provideTokenStore(@ApplicationContext context: Context): TokenStore =
        TokenStore(context)

    // ─── Firebase ────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    // ─── Auth Service ─────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideAuthService(
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore,
        api: IctuExApiService,
        tokenStore: TokenStore
    ): AuthService {
        return if (BuildConfig.USE_SPRING_BACKEND) {
            SpringAuthService(api, tokenStore)
        } else {
            FirebaseAuthService(firebaseAuth, firestore)
        }
    }

    // ─── Listing Service ──────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideListingService(
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore,
        api: IctuExApiService,
        tokenStore: TokenStore
    ): ListingService {
        return if (BuildConfig.USE_SPRING_BACKEND) {
            SpringListingService(api, tokenStore)
        } else {
            FirebaseListingService(firebaseAuth, firestore)
        }
    }

    // ─── Messaging Service ────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideMessagingService(
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore,
        api: IctuExApiService,
        tokenStore: TokenStore
    ): MessagingService {
        return if (BuildConfig.USE_SPRING_BACKEND) {
            SpringMessagingService(api, tokenStore)
        } else {
            FirebaseMessagingService(firebaseAuth, firestore)
        }
    }
}
