package com.rizmi.cleanshield.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.rizmi.cleanshield.data.local.dao.AllowlistDao
import com.rizmi.cleanshield.data.local.dao.BlocklistDao
import com.rizmi.cleanshield.data.local.dao.DnsEventDao
import com.rizmi.cleanshield.data.local.entity.AllowedDomainEntity
import com.rizmi.cleanshield.data.local.entity.BlockedDomainEntity
import com.rizmi.cleanshield.data.local.entity.BlocklistSourceEntity
import com.rizmi.cleanshield.data.local.entity.DnsEventEntity

@Database(
    entities = [
        DnsEventEntity::class,
        BlocklistSourceEntity::class,
        BlockedDomainEntity::class,
        AllowedDomainEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CleanShieldDatabase : RoomDatabase() {

    abstract fun dnsEventDao(): DnsEventDao
    abstract fun blocklistDao(): BlocklistDao
    abstract fun allowlistDao(): AllowlistDao

    companion object {
        @Volatile
        private var INSTANCE: CleanShieldDatabase? = null

        fun getInstance(context: Context): CleanShieldDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CleanShieldDatabase::class.java,
                    "cleanshield_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
