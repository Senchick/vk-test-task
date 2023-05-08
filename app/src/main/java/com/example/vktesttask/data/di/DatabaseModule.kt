package com.example.vktesttask.data.di

import android.content.Context
import androidx.room.Room
import com.example.vktesttask.data.dao.FolderDao
import com.example.vktesttask.data.database.FolderDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Singleton
    @Provides
    fun provideFolderDatabase(@ApplicationContext context: Context): FolderDatabase {
        return Room.databaseBuilder(context, FolderDatabase::class.java, "folder_database").build()
    }

    @Provides
    fun provideFolderDao(folderDatabase: FolderDatabase): FolderDao {
        return folderDatabase.dao()
    }
}
