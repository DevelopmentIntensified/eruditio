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
    version = 3,
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

        /** v3: dropped reading_positions.paragraphIndex (write-only — the reader
         *  re-derives the item index from charOffset via ReaderText). Rebuild the
         *  table because Android API 26 SQLite cannot DROP COLUMN. */
        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE reading_positions_v3 (" +
                        "textId TEXT NOT NULL, " +
                        "charOffset INTEGER NOT NULL, " +
                        "updatedAtMillis INTEGER NOT NULL, " +
                        "PRIMARY KEY(textId))",
                )
                db.execSQL(
                    "INSERT INTO reading_positions_v3 " +
                        "(textId, charOffset, updatedAtMillis) " +
                        "SELECT textId, charOffset, updatedAtMillis FROM reading_positions",
                )
                db.execSQL("DROP TABLE reading_positions")
                db.execSQL("ALTER TABLE reading_positions_v3 RENAME TO reading_positions")
            }
        }

        fun build(context: Application): EruditioDatabase =
            Room.databaseBuilder(context, EruditioDatabase::class.java, "eruditio.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
    }
}
