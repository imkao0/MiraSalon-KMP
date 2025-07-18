package iz.mkao.mirasalon.core.database

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/**
 * iOS implementation of the database builder.
 *
 * Configures the database to reside within the application support directory,
 * following Apple's data-storage guidelines.
 */
@OptIn(ExperimentalForeignApi::class)
actual fun getDatabaseBuilder(ctx: Any?): RoomDatabase.Builder<MiraDatabase> {
    val supportDir = NSFileManager.defaultManager.URLForDirectory(
        directory = NSApplicationSupportDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null
    )?.path ?: error("Unable to resolve Application Support directory")

    val dbFilePath = "$supportDir/mira_salon.db"
    return Room.databaseBuilder<MiraDatabase>(
        name = dbFilePath
    )
        .setDriver(BundledSQLiteDriver())
        .fallbackToDestructiveMigration(true)
}

actual fun getMiraDatabase(builder: RoomDatabase.Builder<MiraDatabase>): MiraDatabase {
    return builder.build()
}
