package iz.mkao.mirasalon.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

actual fun getDatabaseBuilder(ctx: Any?): RoomDatabase.Builder<MiraDatabase> {
    val context = ctx as Context
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("mira_salon.db")
    return Room.databaseBuilder<MiraDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    )
        .setDriver(BundledSQLiteDriver())
        .fallbackToDestructiveMigration(true)
}

actual fun getMiraDatabase(builder: RoomDatabase.Builder<MiraDatabase>): MiraDatabase {
    return builder.build()
}
