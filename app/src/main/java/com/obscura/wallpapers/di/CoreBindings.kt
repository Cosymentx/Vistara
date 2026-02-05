package com.obscura.wallpapers.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.obscura.wallpapers.core.common.StringProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "vistara_preferences")

@Module
@InstallIn(SingletonComponent::class)
object CoreDiBindings {

    @Provides
    @Singleton
    fun apiStrings(@ApplicationContext context: Context): StringProvider {
        println("apiStrings")
        return StringProvider(context)
    }

    @Provides
    @Singleton
    fun apiAppContext(@ApplicationContext context: Context): Context {
        println("apiAppContext")
        return context
    }

    @Provides
    @Singleton
    fun apiPreferencesStore(@ApplicationContext context: Context): DataStore<Preferences> {
        println("apiPreferencesStore")
        return context.dataStore
    }
}
