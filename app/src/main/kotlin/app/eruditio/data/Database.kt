package app.eruditio.data

import android.app.Application
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter fun longToBoxed(v: Long?): Long? = v
}

@Database(
    entities = [
        ReadingPositionEntity::class,
        SessionEntity::class,
        InterceptionEntity::class,
        LedgerEntity::class,
        EntitlementEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class EruditioDatabase : RoomDatabase() {
    abstract fun readingPositionDao(): ReadingPositionDao
    abstract fun sessionDao(): SessionDao
    abstract fun interceptionDao(): InterceptionDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun entitlementDao(): EntitlementDao

    companion object {
        /** v2: per-session reading time (progress "time learning, not scrolling"). */
        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sessions ADD COLUMN readMillis INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun build(context: Application): EruditioDatabase =
            Room.databaseBuilder(context, EruditioDatabase::class.java, "eruditio.db")
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
    }
}
