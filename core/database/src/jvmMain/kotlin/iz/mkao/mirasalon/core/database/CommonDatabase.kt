package iz.mkao.mirasalon.core.database

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File

actual fun getDatabaseBuilder(ctx: Any?): RoomDatabase.Builder<MiraDatabase> {
    val dbFile = File(System.getProperty("java.io.tmpdir"), "mira_salon.db")
    return Room.databaseBuilder<MiraDatabase>(
        name = dbFile.absolutePath,
    )
        .setDriver(BundledSQLiteDriver())
        .fallbackToDestructiveMigration(true)
}

actual fun getMiraDatabase(builder: RoomDatabase.Builder<MiraDatabase>): MiraDatabase {
    return builder.build()
}
