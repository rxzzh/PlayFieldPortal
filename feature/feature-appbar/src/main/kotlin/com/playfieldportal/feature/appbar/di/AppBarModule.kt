package com.playfieldportal.feature.appbar.di

import android.content.Context
import com.playfieldportal.core.domain.presentation.LaunchPresentation
import com.playfieldportal.feature.appbar.InstalledAppRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppBarModule {

    @Provides
    @Singleton
    fun provideInstalledAppRepository(
        @ApplicationContext context: Context,
        presentation: LaunchPresentation,
    ): InstalledAppRepository = InstalledAppRepository(context, presentation)
}
