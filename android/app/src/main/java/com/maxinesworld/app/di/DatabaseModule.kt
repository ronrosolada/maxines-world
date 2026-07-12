package com.maxinesworld.app.di

import android.content.Context
import androidx.room.Room
import com.maxinesworld.coredatabase.MaxinesDatabase
import com.maxinesworld.coredatabase.ParentAccountDao
import com.maxinesworld.coredatabase.ChildProfileDao
import com.maxinesworld.coredatabase.ProgressEventDao
import com.maxinesworld.coredatabase.MasteryRecordDao
import com.maxinesworld.coredatabase.RewardDao
import com.maxinesworld.coredatabase.ScreenTimeLimitDao
import com.maxinesworld.coredatabase.DailyQuestDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MaxinesDatabase {
        return Room.databaseBuilder(
            context,
            MaxinesDatabase::class.java,
            "maxines_world.db"
        ).build()
    }

    @Provides
    fun provideParentAccountDao(db: MaxinesDatabase): ParentAccountDao = db.parentAccountDao()

    @Provides
    fun provideChildProfileDao(db: MaxinesDatabase): ChildProfileDao = db.childProfileDao()

    @Provides
    fun provideProgressEventDao(db: MaxinesDatabase): ProgressEventDao = db.progressEventDao()

    @Provides
    fun provideMasteryRecordDao(db: MaxinesDatabase): MasteryRecordDao = db.masteryRecordDao()

    @Provides
    fun provideRewardDao(db: MaxinesDatabase): RewardDao = db.rewardDao()

    @Provides
    fun provideScreenTimeLimitDao(db: MaxinesDatabase): ScreenTimeLimitDao = db.screenTimeLimitDao()

    @Provides
    fun provideDailyQuestDao(db: MaxinesDatabase): DailyQuestDao = db.dailyQuestDao()
}
