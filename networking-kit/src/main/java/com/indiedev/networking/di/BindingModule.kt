package com.indiedev.networking.di

import com.indiedev.networking.event.EventHelperImp
import com.indiedev.networking.event.EventsHelper
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingModule {

    @Binds
    internal abstract fun bindEventHelper(eventHelperImp: EventHelperImp): EventsHelper
}
