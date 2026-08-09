package com.maxinesworld.app.di

import android.content.Context
import com.maxinesworld.corenetwork.MediaCatalogClient
import com.maxinesworld.corenetwork.MediaDownloader
import com.maxinesworld.corenetwork.MediaLibrary
import com.maxinesworld.corenetwork.MediaStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MediaModule {
    // LAN-only DreamNAS Caddy endpoint. Keep the app's default lesson path
    // bundled; this endpoint serves optional media only.
    private const val MEDIA_BASE_URL = "http://10.10.10.33"
    private const val MEDIA_CATALOG_URL = "$MEDIA_BASE_URL/media/catalog.json"

    @Provides
    @Singleton
    fun provideMediaLibrary(@ApplicationContext context: Context): MediaLibrary {
        val client = OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
        val storage = MediaStorage(File(context.filesDir, "maxines-media"))
        return MediaLibrary(
            catalogUrl = MEDIA_CATALOG_URL,
            mediaBaseUrl = MEDIA_BASE_URL,
            catalogClient = MediaCatalogClient(client),
            downloader = MediaDownloader(client, storage),
            storage = storage,
        )
    }
}
