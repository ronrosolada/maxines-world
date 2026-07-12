package com.maxinesworld.app.di

import com.maxinesworld.coredatabase.ChildProfileDao
import com.maxinesworld.coredatabase.ParentAccountDao
import com.maxinesworld.featureauth.ParentAuthManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface StartupCheckEntryPoint {
    fun parentAccountDao(): ParentAccountDao
    fun childProfileDao(): ChildProfileDao
    fun authManager(): ParentAuthManager
}
