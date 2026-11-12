package com.playfieldportal.feature.launcher

import com.playfieldportal.core.domain.presentation.LaunchPresentation
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class LaunchPresentationModule {
    @Binds abstract fun presentation(gate: GameBootGate): LaunchPresentation
}
