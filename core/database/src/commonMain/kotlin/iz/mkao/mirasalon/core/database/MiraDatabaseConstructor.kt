package iz.mkao.mirasalon.core.database

import androidx.room.RoomDatabaseConstructor

expect object MiraDatabaseConstructor : RoomDatabaseConstructor<MiraDatabase> {
    override fun initialize(): MiraDatabase
}
