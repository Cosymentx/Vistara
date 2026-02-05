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
object CoreBindings {

    @Provides
    @Singleton
    fun bindStrings(@ApplicationContext context: Context): StringProvider =
        StringProvider(context)

    @Provides
    @Singleton
    fun bindAppContext(@ApplicationContext context: Context): Context = context

    @Provides
    @Singleton
    fun bindPreferencesStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore
}
