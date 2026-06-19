package com.statusvault.app.di

import android.content.Context
import androidx.room.Room
import com.statusvault.app.data.db.AppDatabase
import com.statusvault.app.data.db.RecoveredMessageDao
import com.statusvault.app.data.db.StatusFileDao
import com.statusvault.app.data.prefs.SettingsDataStore
import com.statusvault.app.data.repository.MessageRepository
import com.statusvault.app.data.repository.StatusRepository
import com.statusvault.app.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            Constants.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideStatusFileDao(database: AppDatabase): StatusFileDao {
        return database.statusFileDao()
    }

    @Provides
    @Singleton
    fun provideRecoveredMessageDao(database: AppDatabase): RecoveredMessageDao {
        return database.recoveredMessageDao()
    }

    @Provides
    @Singleton
    fun provideStatusRepository(
        @ApplicationContext context: Context,
        statusFileDao: StatusFileDao
    ): StatusRepository {
        return StatusRepository(context, statusFileDao)
    }

    @Provides
    @Singleton
    fun provideMessageRepository(
        messageDao: RecoveredMessageDao
    ): MessageRepository {
        return MessageRepository(messageDao)
    }

    @Provides
    @Singleton
    fun provideSettingsDataStore(
        @ApplicationContext context: Context
    ): SettingsDataStore {
        return SettingsDataStore(context)
    }
}
