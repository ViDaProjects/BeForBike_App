package com.beforbike.app.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import android.database.Cursor

class RideDbHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    // --- Schema Definition (2 Tables: Summary + Full Telemetry) ---
    companion object {
        // INCREMENTED to 13 to force schema update
        const val DATABASE_VERSION = 13 //
        const val DATABASE_NAME = "BikeRides_v13.db"
        private const val TAG = "RideDbHelper"

        // --- Table 1: Rides (General/Summary) ---
        object RidesEntry : BaseColumns {
            const val TABLE_NAME = "Rides"
            const val COLUMN_RIDE_ID = "ride_id" // Unique ID (ex: 123)
            const val COLUMN_START_TIME = "start_time"
            const val COLUMN_END_TIME = "end_time"
            const val COLUMN_TOTAL_DISTANCE_KM = "total_distance_km"
            const val COLUMN_AVG_VELOCITY_KMH = "avg_velocity_kmh"
            const val COLUMN_AVG_ALTITUDE = "avg_altitude"
            const val COLUMN_AVG_POWER = "avg_power"
            const val COLUMN_AVG_CADENCE = "avg_cadence"
            const val COLUMN_CALORIES = "calories"
        }

        // --- Table 2: TelemetryData ---
        object TelemetryEntry : BaseColumns {
            const val TABLE_NAME = "TelemetryData"
            const val COLUMN_TELEMETRY_ID = "_id" // Internal primary key
            const val COLUMN_RIDE_ID = "ride_id" // Foreign key to Rides

            // PacketInfo fields (info)
            const val COLUMN_PACKET_DATE = "packet_date"
            const val COLUMN_PACKET_TIME = "packet_time"

            // GpsData fields (gps)
            const val COLUMN_GPS_TIMESTAMP = "gps_timestamp"
            const val COLUMN_LATITUDE = "latitude"
            const val COLUMN_LONGITUDE = "longitude"
            const val COLUMN_ALTITUDE = "altitude"
            const val COLUMN_GPS_SPEED = "gps_speed"
            const val COLUMN_DIRECTION = "direction"
            const val COLUMN_FIX_SATELLITES = "fix_satellites"
            const val COLUMN_FIX_QUALITY = "fix_quality"

            // CrankData fields
            const val COLUMN_POWER = "power"
            const val COLUMN_CADENCE = "cadence"
            const val COLUMN_JOULES = "joules"
            const val COLUMN_CRANK_CALORIES = "crank_calories"
            const val COLUMN_CRANK_SPEED_MS = "crank_speed_ms"
            const val COLUMN_CRANK_SPEED = "crank_speed"
            const val COLUMN_CRANK_DISTANCE = "crank_distance"
        }

        // --- SQL Creation Commands ---
        private const val SQL_CREATE_RIDES =
            "CREATE TABLE ${RidesEntry.TABLE_NAME} (" +
                    "${RidesEntry.COLUMN_RIDE_ID} INTEGER PRIMARY KEY," +
                    "${RidesEntry.COLUMN_START_TIME} TEXT NOT NULL," +
                    "${RidesEntry.COLUMN_END_TIME} TEXT," +
                    "${RidesEntry.COLUMN_TOTAL_DISTANCE_KM} REAL," +
                    "${RidesEntry.COLUMN_AVG_VELOCITY_KMH} REAL," +
                    "${RidesEntry.COLUMN_AVG_ALTITUDE} REAL," +
                    "${RidesEntry.COLUMN_AVG_POWER} REAL," +
                    "${RidesEntry.COLUMN_AVG_CADENCE} REAL," +
                    "${RidesEntry.COLUMN_CALORIES} REAL)"

        private const val SQL_CREATE_TELEMETRY_DATA =
            "CREATE TABLE ${TelemetryEntry.TABLE_NAME} (" +
                    "${TelemetryEntry.COLUMN_TELEMETRY_ID} INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "${TelemetryEntry.COLUMN_RIDE_ID} INTEGER NOT NULL," +
                    // Info fields
                    "${TelemetryEntry.COLUMN_PACKET_DATE} TEXT," +
                    "${TelemetryEntry.COLUMN_PACKET_TIME} TEXT," +
                    // GpsData fields
                    "${TelemetryEntry.COLUMN_GPS_TIMESTAMP} TEXT," +
                    "${TelemetryEntry.COLUMN_LATITUDE} REAL," +
                    "${TelemetryEntry.COLUMN_LONGITUDE} REAL," +
                    "${TelemetryEntry.COLUMN_ALTITUDE} REAL," +
                    "${TelemetryEntry.COLUMN_GPS_SPEED} REAL," +
                    "${TelemetryEntry.COLUMN_DIRECTION} REAL," +
                    "${TelemetryEntry.COLUMN_FIX_SATELLITES} INTEGER," +
                    "${TelemetryEntry.COLUMN_FIX_QUALITY} INTEGER," +
                    // CrankData fields
                    "${TelemetryEntry.COLUMN_POWER} REAL," +
                    "${TelemetryEntry.COLUMN_CADENCE} REAL," +
                    "${TelemetryEntry.COLUMN_JOULES} REAL," +
                    "${TelemetryEntry.COLUMN_CRANK_CALORIES} REAL," +
                    "${TelemetryEntry.COLUMN_CRANK_SPEED_MS} REAL," +
                    "${TelemetryEntry.COLUMN_CRANK_SPEED} REAL," +
                    "${TelemetryEntry.COLUMN_CRANK_DISTANCE} REAL," +
                    "FOREIGN KEY (${TelemetryEntry.COLUMN_RIDE_ID}) REFERENCES ${RidesEntry.TABLE_NAME}(${RidesEntry.COLUMN_RIDE_ID}) ON DELETE CASCADE)"

        // --- SQL Deletion Commands ---
        private const val SQL_DELETE_RIDES = "DROP TABLE IF EXISTS ${RidesEntry.TABLE_NAME}"
        private const val SQL_DELETE_TELEMETRY_DATA = "DROP TABLE IF EXISTS ${TelemetryEntry.TABLE_NAME}"
        // Nomes antigos
        private const val SQL_DELETE_OLD_POWER = "DROP TABLE IF EXISTS Power"
        private const val SQL_DELETE_OLD_MAPDATA = "DROP TABLE IF EXISTS Localization"
        private const val SQL_DELETE_OLD_VELOCITY = "DROP TABLE IF EXISTS Velocity"
        private const val SQL_DELETE_OLD_CADENCE = "DROP TABLE IF EXISTS Cadence"
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        Log.i(TAG, "Creating database tables (v10)...")
        db.execSQL(SQL_CREATE_RIDES)
        db.execSQL(SQL_CREATE_TELEMETRY_DATA)
        Log.i(TAG, "Tables created successfully.")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.w(TAG, "Updating database from v$oldVersion to v$newVersion. Old data will be lost.")
        // Delete ALL tables (new and old)
        db.execSQL(SQL_DELETE_TELEMETRY_DATA) // Delete TelemetryData (v9) or TelemetryData (v10)
        db.execSQL(SQL_DELETE_OLD_POWER) //
        db.execSQL(SQL_DELETE_OLD_MAPDATA) //
        db.execSQL(SQL_DELETE_OLD_VELOCITY) //
        db.execSQL(SQL_DELETE_OLD_CADENCE) //
        db.execSQL(SQL_DELETE_RIDES) // Delete Rides last
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.w(TAG, "Downgrading database from v$oldVersion to v$newVersion. Old data will be lost.")
        onUpgrade(db, oldVersion, newVersion)
    }

    private fun getCurrentTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
    }

    /**
     * Ensures that the "General Ride" (Table 1) exists.
     */
    fun ensureRideExists(rideIdFromDevice: Long, startTime: String?): Boolean {
        if (rideIdFromDevice <= 0) {
            Log.e(TAG, "ensureRideExists: ID da corrida inválido ($rideIdFromDevice).")
            return false
        }
        val db = this.writableDatabase

        val values = ContentValues().apply {
            put(RidesEntry.COLUMN_RIDE_ID, rideIdFromDevice)
            put(RidesEntry.COLUMN_START_TIME, startTime ?: getCurrentTimestamp())
        }

        try {
            val result = db.insertWithOnConflict(RidesEntry.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE)
            if (result == -1L) {
                // Log.d(TAG, "ensureRideExists: Ride ID $rideIdFromDevice already exists.")
            } else {
                Log.i(TAG, "New entry in 'Rides' table created for ID: $rideIdFromDevice")
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "CRITICAL ERROR when trying to create entry in 'Rides' table for ID $rideIdFromDevice: ${e.message}")
            return false
        }
    }


    /**
     * Inserts a complete telemetry point (Table 2).
     */
    fun insertTelemetryData(
        rideId: Long,
        infoMap: Map<String, Any?>,
        gpsMap: Map<String, Any?>,
        crankMap: Map<String, Any?>? // Can be null
    ): Boolean {
        if (rideId <= 0) return false
        val db = this.writableDatabase

        val values = ContentValues().apply {
            put(TelemetryEntry.COLUMN_RIDE_ID, rideId)

            // Info data
            put(TelemetryEntry.COLUMN_PACKET_DATE, infoMap["date"] as? String)
            put(TelemetryEntry.COLUMN_PACKET_TIME, infoMap["time"] as? String)

            // GpsData data (gps) - all fields obligatory, use 0 as default
            put(TelemetryEntry.COLUMN_GPS_TIMESTAMP, gpsMap["timestamp"] as? String ?: "")
            put(TelemetryEntry.COLUMN_LATITUDE, (gpsMap["latitude"] as? Double) ?: 0.0)
            put(TelemetryEntry.COLUMN_LONGITUDE, (gpsMap["longitude"] as? Double) ?: 0.0)
            put(TelemetryEntry.COLUMN_ALTITUDE, (gpsMap["altitude"] as? Double) ?: 0.0)
            put(TelemetryEntry.COLUMN_GPS_SPEED, (gpsMap["speed"] as? Double) ?: 0.0)
            put(TelemetryEntry.COLUMN_DIRECTION, (gpsMap["direction"] as? Double) ?: 0.0)
            put(TelemetryEntry.COLUMN_FIX_SATELLITES, (gpsMap["fix_satellites"] as? Int) ?: 0)
            put(TelemetryEntry.COLUMN_FIX_QUALITY, (gpsMap["fix_quality"] as? Int) ?: 0)

            // CrankData data (crank) - all fields obligatory, use 0 as default
            crankMap?.let {
                put(TelemetryEntry.COLUMN_POWER, (it["power"] as? Double) ?: 0.0)
                put(TelemetryEntry.COLUMN_CADENCE, (it["cadence"] as? Double) ?: 0.0)
                put(TelemetryEntry.COLUMN_JOULES, (it["joules"] as? Double) ?: 0.0)
                put(TelemetryEntry.COLUMN_CRANK_CALORIES, (it["calories"] as? Double) ?: 0.0)
                put(TelemetryEntry.COLUMN_CRANK_SPEED_MS, (it["speed_ms"] as? Double) ?: 0.0)
                put(TelemetryEntry.COLUMN_CRANK_SPEED, (it["speed"] as? Double) ?: 0.0)
                put(TelemetryEntry.COLUMN_CRANK_DISTANCE, (it["distance"] as? Double) ?: 0.0)
            } ?: run {
                // If no crank data provided, insert zeros for all crank fields
                put(TelemetryEntry.COLUMN_POWER, 0.0)
                put(TelemetryEntry.COLUMN_CADENCE, 0.0)
                put(TelemetryEntry.COLUMN_JOULES, 0.0)
                put(TelemetryEntry.COLUMN_CRANK_CALORIES, 0.0)
                put(TelemetryEntry.COLUMN_CRANK_SPEED_MS, 0.0)
                put(TelemetryEntry.COLUMN_CRANK_SPEED, 0.0)
                put(TelemetryEntry.COLUMN_CRANK_DISTANCE, 0.0)
            }
        }

        val result = db.insert(TelemetryEntry.TABLE_NAME, null, values)
        if (result == -1L) Log.w(TAG, "Failed to insert TelemetryData for ride $rideId")
        return result != -1L
    }

    // --- QUERY AND CALCULATION FUNCTIONS ---

    /**
     * Returns a list of ride IDs (Table 1).
     */
    fun getAllRideIds(): List<Long> {
        val db = this.readableDatabase
        val cursor = db.query(
            RidesEntry.TABLE_NAME,
            arrayOf(RidesEntry.COLUMN_RIDE_ID),
            null, null, null, null,
            "${RidesEntry.COLUMN_START_TIME} DESC"
        )
        val rideIds = mutableListOf<Long>()
        cursor.use {
            while (it.moveToNext()) {
                rideIds.add(it.getLong(0))
            }
        }
        return rideIds
    }

    /**
     * Returns the summary data of a ride (Table 1).
     * If the totals are not calculated (distance or calories zero), calculates and updates.
     */
    fun getRideSummary(rideId: Long): Map<String, Any?>? {
        val db = this.readableDatabase
        val cursor = db.query(
            RidesEntry.TABLE_NAME,
            null, // Get all columns
            "${RidesEntry.COLUMN_RIDE_ID} = ?",
            arrayOf(rideId.toString()),
            null, null, null
        )
        cursor.use {
            if (it.moveToFirst()) {
                val map = mutableMapOf<String, Any?>()
                for (i in 0 until it.columnCount) {
                    when (it.getType(i)) {
                        Cursor.FIELD_TYPE_NULL -> map[it.getColumnName(i)] = null
                        Cursor.FIELD_TYPE_INTEGER -> map[it.getColumnName(i)] = it.getLong(i)
                        Cursor.FIELD_TYPE_FLOAT -> map[it.getColumnName(i)] = it.getDouble(i)
                        Cursor.FIELD_TYPE_STRING -> map[it.getColumnName(i)] = it.getString(i)
                        Cursor.FIELD_TYPE_BLOB -> map[it.getColumnName(i)] = it.getBlob(i)
                        else -> map[it.getColumnName(i)] = null // Padrão
                    }
                }

                // Always recalculate start/end times from telemetry data to ensure accuracy
                val stats = calculateRideStatistics(rideId)
                if (stats != null) {
                    // Update start/end times in database
                    val timeValues = ContentValues().apply {
                        stats["start_time"]?.let { put(RidesEntry.COLUMN_START_TIME, it as String) }
                        stats["end_time"]?.let { put(RidesEntry.COLUMN_END_TIME, it as String) }
                    }
                    if (timeValues.size() > 0) {
                        db.update(
                            RidesEntry.TABLE_NAME,
                            timeValues,
                            "${RidesEntry.COLUMN_RIDE_ID} = ?",
                            arrayOf(rideId.toString())
                        )
                        // Update the map with correct times
                        map[RidesEntry.COLUMN_START_TIME] = stats["start_time"]
                        map[RidesEntry.COLUMN_END_TIME] = stats["end_time"]
                    }
                    
                    // Check if totals need to be calculated
                    val totalDistance = map[RidesEntry.COLUMN_TOTAL_DISTANCE_KM] as? Double ?: 0.0
                    val totalCalories = map[RidesEntry.COLUMN_CALORIES] as? Double ?: 0.0
                    
                    if (totalDistance == 0.0 && totalCalories == 0.0) {
                        // Update all statistics if not present
                        updateRideSummary(rideId, stats)
                        // Updates the map with the new values
                        map[RidesEntry.COLUMN_TOTAL_DISTANCE_KM] = stats["total_distance_km"]
                        map[RidesEntry.COLUMN_AVG_VELOCITY_KMH] = stats["avg_velocity_kmh"]
                        map[RidesEntry.COLUMN_AVG_ALTITUDE] = stats["avg_altitude"]
                        map[RidesEntry.COLUMN_AVG_POWER] = stats["avg_power"]
                        map[RidesEntry.COLUMN_AVG_CADENCE] = stats["avg_cadence"]
                        map[RidesEntry.COLUMN_CALORIES] = stats["calories"]
                    }
                }

                return map
            }
        }
        return null
    }

    /**
     * Calculates summary statistics (for Table 1)
     * reading all points from Table 2.
     */
    fun calculateRideStatistics(rideId: Long): Map<String, Any?>? {
        val db = this.readableDatabase

        // First, check if statistics are already calculated and stored in the Rides table
        val existingStatsCursor = db.query(
            RidesEntry.TABLE_NAME,
            arrayOf(
                RidesEntry.COLUMN_RIDE_ID,
                RidesEntry.COLUMN_START_TIME,
                RidesEntry.COLUMN_END_TIME,
                RidesEntry.COLUMN_TOTAL_DISTANCE_KM,
                RidesEntry.COLUMN_AVG_ALTITUDE,
                RidesEntry.COLUMN_AVG_VELOCITY_KMH,
                RidesEntry.COLUMN_AVG_POWER,
                RidesEntry.COLUMN_AVG_CADENCE,
                RidesEntry.COLUMN_CALORIES
            ),
            "${RidesEntry.COLUMN_RIDE_ID} = ? AND ${RidesEntry.COLUMN_TOTAL_DISTANCE_KM} IS NOT NULL",
            arrayOf(rideId.toString()),
            null, null, null
        )

        existingStatsCursor.use { cursor ->
            if (cursor.moveToFirst()) {
                // Statistics already exist, return them
                val existingStats = mapOf(
                    "ride_id" to cursor.getLong(cursor.getColumnIndexOrThrow(RidesEntry.COLUMN_RIDE_ID)),
                    "start_time" to cursor.getString(cursor.getColumnIndexOrThrow(RidesEntry.COLUMN_START_TIME)),
                    "end_time" to cursor.getString(cursor.getColumnIndexOrThrow(RidesEntry.COLUMN_END_TIME)),
                    "total_distance_km" to cursor.getDouble(cursor.getColumnIndexOrThrow(RidesEntry.COLUMN_TOTAL_DISTANCE_KM)),
                    "avg_altitude" to cursor.getDouble(cursor.getColumnIndexOrThrow(RidesEntry.COLUMN_AVG_ALTITUDE)),
                    "avg_velocity_kmh" to cursor.getDouble(cursor.getColumnIndexOrThrow(RidesEntry.COLUMN_AVG_VELOCITY_KMH)),
                    "avg_power" to cursor.getDouble(cursor.getColumnIndexOrThrow(RidesEntry.COLUMN_AVG_POWER)),
                    "avg_cadence" to cursor.getDouble(cursor.getColumnIndexOrThrow(RidesEntry.COLUMN_AVG_CADENCE)),
                    "calories" to cursor.getDouble(cursor.getColumnIndexOrThrow(RidesEntry.COLUMN_CALORIES))
                )
                Log.v(TAG, "Returning cached statistics for ride $rideId")
                return existingStats
            }
        }

        // Statistics don't exist, calculate them from telemetry data
        Log.v(TAG, "Calculating statistics for ride $rideId (first time)")
        val cursor = db.query(
            TelemetryEntry.TABLE_NAME,
            arrayOf(
                TelemetryEntry.COLUMN_GPS_TIMESTAMP,
                TelemetryEntry.COLUMN_LATITUDE,
                TelemetryEntry.COLUMN_LONGITUDE,
                TelemetryEntry.COLUMN_ALTITUDE,
                TelemetryEntry.COLUMN_GPS_SPEED,
                TelemetryEntry.COLUMN_CRANK_SPEED,
                TelemetryEntry.COLUMN_POWER,
                TelemetryEntry.COLUMN_CADENCE,
                TelemetryEntry.COLUMN_CRANK_CALORIES,
                TelemetryEntry.COLUMN_CRANK_DISTANCE
            ),
            "${TelemetryEntry.COLUMN_RIDE_ID} = ?",
            arrayOf(rideId.toString()),
            null, null,
            "${TelemetryEntry.COLUMN_GPS_TIMESTAMP} ASC" // Order by time
        )

        if (!cursor.moveToFirst()) {
            cursor.close()
            Log.w(TAG, "calculateRideStatistics: No telemetry data found for ride $rideId")
            return null
        }

        var totalDistanceKm = 0.0
        val velocities = mutableListOf<Double>()
        val altitudes = mutableListOf<Double>()
        val powers = mutableListOf<Double>()
        val cadences = mutableListOf<Double>()
        var totalCalories = 0.0

        var startTime: Long? = null
        var endTime: Long? = null
        var prevLat = 0.0
        var prevLon = 0.0
        var firstGpsPoint = true

        // The ISO 8601 format includes the 'T' and has 6 fractional second digits (microseconds)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
        val dateFormatFallback = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()) // If there are no microseconds
        val dateFormatFallbackMillis = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault()) // If there are milliseconds
        // Additional formats with slashes and space separator
        val dateFormatSlashes = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
        val dateFormatSlashesMillis = SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS", Locale.getDefault())
        val dateFormatSlashesMicro = SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSSSSS", Locale.getDefault())
        // Format used by seed data: space separator with milliseconds
        val dateFormatSpaceMillis = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

        val outputDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())


        cursor.use {
            do {
                // --- Time and Duration ---
                val timestampStr = it.getString(it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_GPS_TIMESTAMP))
                // Log.v(TAG, "Parsing timestamp: '$timestampStr'")
                try {
                    // Try the main format (with microseconds)
                    val timestamp = dateFormat.parse(timestampStr)?.time
                    if (timestamp != null) {
                        if (startTime == null) startTime = timestamp
                        endTime = timestamp
                    }
                } catch (e: Exception) {
                    try {
                        // Try the format with milliseconds
                        val ts = dateFormatFallbackMillis.parse(timestampStr)?.time
                        if (ts != null) {
                            if (startTime == null) startTime = ts
                            endTime = ts
                        }
                    } catch (e2: Exception) {
                        try {
                            // Try the format without fractional seconds
                            val ts = dateFormatFallback.parse(timestampStr)?.time
                            if (ts != null) {
                                if (startTime == null) startTime = ts
                                endTime = ts
                            }
                        } catch (e3: Exception) {
                            try {
                                // Try yyyy/MM/dd HH:mm:ss.SSSSSS
                                val ts = dateFormatSlashesMicro.parse(timestampStr)?.time
                                if (ts != null) {
                                    if (startTime == null) startTime = ts
                                    endTime = ts
                                }
                            } catch (e4: Exception) {
                                try {
                                    // Try yyyy/MM/dd HH:mm:ss.SSS
                                    val ts = dateFormatSlashesMillis.parse(timestampStr)?.time
                                    if (ts != null) {
                                        if (startTime == null) startTime = ts
                                        endTime = ts
                                    }
                                } catch (e5: Exception) {
                                    try {
                                        // Try yyyy/MM/dd HH:mm:ss
                                        val ts = dateFormatSlashes.parse(timestampStr)?.time
                                        if (ts != null) {
                                            if (startTime == null) startTime = ts
                                            endTime = ts
                                        }
                                    } catch (e6: Exception) {
                                        try {
                                            // Try yyyy-MM-dd HH:mm:ss.SSS (seed data format)
                                            val ts = dateFormatSpaceMillis.parse(timestampStr)?.time
                                            if (ts != null) {
                                                if (startTime == null) startTime = ts
                                                endTime = ts
                                            }
                                        } catch (e7: Exception) {
                                            Log.w(TAG, "Formato de timestamp inválido: $timestampStr")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // GPS (keeps for altitude)
                val latIdx = it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_LATITUDE)
                val lonIdx = it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_LONGITUDE)
                if (!it.isNull(latIdx) && !it.isNull(lonIdx)) {
                    val lat = it.getDouble(latIdx)
                    val lon = it.getDouble(lonIdx)

                    if (firstGpsPoint) {
                        prevLat = lat
                        prevLon = lon
                        firstGpsPoint = false
                    } else {
                        prevLat = lat
                        prevLon = lon
                    }
                }

                // Distance (uses cumulative BLE data)
                val distIdx = it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_CRANK_DISTANCE)
                if (!it.isNull(distIdx)) {
                    val distance = it.getDouble(distIdx)
                    if (distance > totalDistanceKm) {
                        totalDistanceKm = distance // Takes the last/largest value (total accumulated distance)
                    }
                }

                // --- Altitude ---
                val altIdx = it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_ALTITUDE)
                if (!it.isNull(altIdx)) {
                    altitudes.add(it.getDouble(altIdx))
                }

                // Speed (Uses GPS_SPEED as primary)
                val velIdx = it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_GPS_SPEED)
                val crankVelIdx = it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_CRANK_SPEED)

                if (!it.isNull(velIdx)) {
                    val velKmh = it.getDouble(velIdx) // Assuming speed is already in km/h
                    velocities.add(velKmh)
                } else if (!it.isNull(crankVelIdx)) {
                    // Fallback to crank speed (assuming km/h)
                    velocities.add(it.getDouble(crankVelIdx))
                }

                // --- Potência ---
                val powerIdx = it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_POWER)
                if (!it.isNull(powerIdx)) {
                    powers.add(it.getDouble(powerIdx))
                }

                // --- Cadência ---
                val cadIdx = it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_CADENCE)
                if (!it.isNull(cadIdx)) {
                    cadences.add(it.getDouble(cadIdx))
                }

                // Cumulative calories
                val calIdx = it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_CRANK_CALORIES)
                if (!it.isNull(calIdx)) {
                    totalCalories = it.getDouble(calIdx) // Last value is the total
                }

            } while (it.moveToNext())
        }

        // Calculate Averages and Totals ---
        val durationSec = if (startTime != null && endTime != null) (endTime!! - startTime!!) / 1000.0 else 0.0
        val durationHours = durationSec / 3600.0

        val avgVelocityKmh = if (velocities.isNotEmpty()) velocities.average() else 0.0
        val avgAltitude = if (altitudes.isNotEmpty()) altitudes.average() else 0.0
        val avgPower = if (powers.isNotEmpty()) powers.average() else 0.0
        val avgCadence = if (cadences.isNotEmpty()) cadences.average() else 0.0

        // If calories not received via BLE (cumulative == 0), calculate from power
        val calculatedCalories = if (totalCalories == 0.0) avgPower * durationHours * 3.6 else totalCalories

        val result = mapOf(
            "ride_id" to rideId,
            // Uses the DB output format
            "start_time" to (startTime?.let { outputDateFormat.format(Date(it)) } ?: getCurrentTimestamp()),
            "end_time" to (endTime?.let { outputDateFormat.format(Date(it)) } ?: getCurrentTimestamp()),
            "total_distance_km" to totalDistanceKm,
            "avg_altitude" to avgAltitude,
            "avg_velocity_kmh" to avgVelocityKmh,
            "avg_power" to avgPower,
            "avg_cadence" to avgCadence,
            "calories" to calculatedCalories
        )

        Log.v(TAG, "Statistics calculated for $rideId: $result")

        // Save the calculated statistics to the Rides table
        updateRideSummary(rideId, result)

        return result
    }

    /**
     * Updates Table 1 (Rides) with calculated summary data.
     */
    fun updateRideSummary(rideId: Long, stats: Map<String, Any?>): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {

            stats["start_time"]?.let { put(RidesEntry.COLUMN_START_TIME, it as String) }

            stats["end_time"]?.let { put(RidesEntry.COLUMN_END_TIME, it as String) }
            stats["total_distance_km"]?.let { put(RidesEntry.COLUMN_TOTAL_DISTANCE_KM, it as Double) }
            stats["avg_altitude"]?.let { put(RidesEntry.COLUMN_AVG_ALTITUDE, it as Double) }
            stats["avg_velocity_kmh"]?.let { put(RidesEntry.COLUMN_AVG_VELOCITY_KMH, it as Double) }
            stats["avg_power"]?.let { put(RidesEntry.COLUMN_AVG_POWER, it as Double) }
            stats["avg_cadence"]?.let { put(RidesEntry.COLUMN_AVG_CADENCE, it as Double) }
            stats["calories"]?.let { put(RidesEntry.COLUMN_CALORIES, it as Double) }
        }

        if (values.size() == 0) {
            Log.w(TAG, "updateRideSummary: No valid data to update for ride $rideId")
            return false
        }

        val result = db.update(
            RidesEntry.TABLE_NAME,
            values,
            "${RidesEntry.COLUMN_RIDE_ID} = ?",
            arrayOf(rideId.toString())
        )

        Log.i(TAG, "Table 'Rides' (ID: $rideId) updated with statistics. Rows affected: $result")
        return result > 0
    }

    /**
     * Returns all raw telemetry data from a ride (Table 2).
     */
    fun getRideTelemetryData(rideId: Long): List<Map<String, Any?>> {
        val db = this.readableDatabase
        val cursor = db.query(
            TelemetryEntry.TABLE_NAME,
            null, // Get all columns
            "${TelemetryEntry.COLUMN_RIDE_ID} = ?",
            arrayOf(rideId.toString()),
            null, null,
            "${TelemetryEntry.COLUMN_TELEMETRY_ID} ASC"
        )

        val results = mutableListOf<Map<String, Any?>>()
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val map = mutableMapOf<String, Any?>()
                    for (i in 0 until it.columnCount) {
                        when (it.getType(i)) {
                            Cursor.FIELD_TYPE_NULL -> map[it.getColumnName(i)] = null
                            Cursor.FIELD_TYPE_INTEGER -> map[it.getColumnName(i)] = it.getLong(i)
                            Cursor.FIELD_TYPE_FLOAT -> map[it.getColumnName(i)] = it.getDouble(i)
                            Cursor.FIELD_TYPE_STRING -> map[it.getColumnName(i)] = it.getString(i)
                            Cursor.FIELD_TYPE_BLOB -> map[it.getColumnName(i)] = it.getBlob(i)
                            else -> map[it.getColumnName(i)] = null // Default
                        }
                    }
                    results.add(map)
                } while (it.moveToNext())
            }
        }
        return results
    }

    fun deleteRide(rideId: Long) {
        val db = writableDatabase
        db.delete(RidesEntry.TABLE_NAME, "${RidesEntry.COLUMN_RIDE_ID} = ?", arrayOf(rideId.toString()))
        // TelemetryData will be deleted automatically due to ON DELETE CASCADE
    }

    /**
     * Calculates max and average values for speed, cadence, power, and altitude for a ride.
     */
    fun getActivityStatistics(rideId: Long): Map<String, Any?>? {
        val db = this.readableDatabase

        val cursor = db.query(
            TelemetryEntry.TABLE_NAME,
            arrayOf(
                TelemetryEntry.COLUMN_GPS_SPEED,
                TelemetryEntry.COLUMN_CADENCE,
                TelemetryEntry.COLUMN_POWER,
                TelemetryEntry.COLUMN_ALTITUDE,
                TelemetryEntry.COLUMN_GPS_TIMESTAMP
            ),
            "${TelemetryEntry.COLUMN_RIDE_ID} = ?",
            arrayOf(rideId.toString()),
            null, null,
            "${TelemetryEntry.COLUMN_GPS_TIMESTAMP} ASC"
        )

        if (!cursor.moveToFirst()) {
            cursor.close()
            return null
        }

        // Date format parsers for robust timestamp parsing
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
        val dateFormatFallback = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val dateFormatFallbackMillis = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
        val dateFormatSlashes = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
        val dateFormatSlashesMillis = SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS", Locale.getDefault())
        val dateFormatSlashesMicro = SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSSSSS", Locale.getDefault())

        val speeds = mutableListOf<Double>()
        val cadences = mutableListOf<Double>()
        val powers = mutableListOf<Double>()
        val altitudes = mutableListOf<Double>()
        val timestamps = mutableListOf<Long>()

        cursor.use {
            do {
                // Speed
                val speedIdx = it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_GPS_SPEED)
                if (!it.isNull(speedIdx)) {
                    speeds.add(it.getDouble(speedIdx))
                }

                // Cadence
                val cadIdx = it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_CADENCE)
                if (!it.isNull(cadIdx)) {
                    cadences.add(it.getDouble(cadIdx))
                }

                // Power
                val powerIdx = it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_POWER)
                if (!it.isNull(powerIdx)) {
                    powers.add(it.getDouble(powerIdx))
                }

                // Altitude
                altitudes.add(it.getDouble(it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_ALTITUDE)))

                // Timestamp for data points
                val tsIdx = it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_GPS_TIMESTAMP)
                if (!it.isNull(tsIdx)) {
                    val timestampStr = it.getString(tsIdx)
                    try {
                        val timestamp = dateFormat.parse(timestampStr)?.time
                        if (timestamp != null) {
                            timestamps.add(timestamp)
                        } else {
                            timestamps.add(System.currentTimeMillis())
                        }
                    } catch (e: Exception) {
                        try {
                            val ts = dateFormatFallbackMillis.parse(timestampStr)?.time
                            if (ts != null) {
                                timestamps.add(ts)
                            } else {
                                timestamps.add(System.currentTimeMillis())
                            }
                        } catch (e2: Exception) {
                            try {
                                val ts = dateFormatFallback.parse(timestampStr)?.time
                                if (ts != null) {
                                    timestamps.add(ts)
                                } else {
                                    timestamps.add(System.currentTimeMillis())
                                }
                            } catch (e3: Exception) {
                                try {
                                    val ts = dateFormatSlashesMicro.parse(timestampStr)?.time
                                    if (ts != null) {
                                        timestamps.add(ts)
                                    } else {
                                        timestamps.add(System.currentTimeMillis())
                                    }
                                } catch (e4: Exception) {
                                    try {
                                        val ts = dateFormatSlashesMillis.parse(timestampStr)?.time
                                        if (ts != null) {
                                            timestamps.add(ts)
                                        } else {
                                            timestamps.add(System.currentTimeMillis())
                                        }
                                    } catch (e5: Exception) {
                                        try {
                                            val ts = dateFormatSlashes.parse(timestampStr)?.time
                                            if (ts != null) {
                                                timestamps.add(ts)
                                            } else {
                                                timestamps.add(System.currentTimeMillis())
                                            }
                                        } catch (e6: Exception) {
                                            timestamps.add(System.currentTimeMillis())
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            } while (it.moveToNext())
        }

        // Calculate max and avg values
        val maxSpeed = if (speeds.isNotEmpty()) speeds.maxOrNull() ?: 0.0 else 0.0
        val avgSpeed = if (speeds.isNotEmpty()) speeds.average() else 0.0

        val maxCadence = if (cadences.isNotEmpty()) cadences.maxOrNull() ?: 0.0 else 0.0
        val avgCadence = if (cadences.isNotEmpty()) cadences.average() else 0.0

        val maxPower = if (powers.isNotEmpty()) powers.maxOrNull() ?: 0.0 else 0.0
        val avgPower = if (powers.isNotEmpty()) powers.average() else 0.0

        val maxAltitude = if (altitudes.isNotEmpty()) altitudes.maxOrNull() ?: 0.0 else 0.0
        val avgAltitude = if (altitudes.isNotEmpty()) altitudes.average() else 0.0

        return mapOf(
            "max_speed" to maxSpeed,
            "avg_speed" to avgSpeed,
            "max_cadence" to maxCadence,
            "avg_cadence" to avgCadence,
            "max_power" to maxPower,
            "avg_power" to avgPower,
            "max_altitude" to maxAltitude,
            "avg_altitude" to avgAltitude
        )
    }

    /**
     * Returns raw telemetry data formatted for Flutter charts.
     */
    fun getActivityChartData(rideId: Long): List<Map<String, Any?>>? {
        val db = this.readableDatabase

        val cursor = db.query(
            TelemetryEntry.TABLE_NAME,
            arrayOf(
                TelemetryEntry.COLUMN_GPS_SPEED,
                TelemetryEntry.COLUMN_CADENCE,
                TelemetryEntry.COLUMN_POWER,
                TelemetryEntry.COLUMN_ALTITUDE,
                TelemetryEntry.COLUMN_GPS_TIMESTAMP
            ),
            "${TelemetryEntry.COLUMN_RIDE_ID} = ?",
            arrayOf(rideId.toString()),
            null, null,
            "${TelemetryEntry.COLUMN_GPS_TIMESTAMP} ASC"
        )

        if (!cursor.moveToFirst()) {
            cursor.close()
            return null
        }

        // Date format parsers for robust timestamp parsing - try most common first
        val dateFormatFallbackMillis = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        val dateFormatFallback = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
        val dateFormatSlashes = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
        val dateFormatSlashesMillis = SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS", Locale.getDefault())
        val dateFormatSlashesMicro = SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSSSSS", Locale.getDefault())

        val chartData = mutableListOf<Map<String, Any?>>()

        cursor.use {
            do {
                val timestampStr = it.getString(it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_GPS_TIMESTAMP))
                val timestamp = try {
                    dateFormatFallbackMillis.parse(timestampStr)?.time ?: System.currentTimeMillis()
                } catch (e: Exception) {
                    try {
                        dateFormatFallback.parse(timestampStr)?.time ?: System.currentTimeMillis()
                    } catch (e2: Exception) {
                        try {
                            dateFormat.parse(timestampStr)?.time ?: System.currentTimeMillis()
                        } catch (e3: Exception) {
                            try {
                                dateFormatSlashesMillis.parse(timestampStr)?.time ?: System.currentTimeMillis()
                            } catch (e4: Exception) {
                                try {
                                    dateFormatSlashes.parse(timestampStr)?.time ?: System.currentTimeMillis()
                                } catch (e5: Exception) {
                                    try {
                                        dateFormatSlashesMicro.parse(timestampStr)?.time ?: System.currentTimeMillis()
                                    } catch (e6: Exception) {
                                        System.currentTimeMillis()
                                    }
                                }
                            }
                        }
                    }
                }

                val speed = if (!it.isNull(it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_GPS_SPEED))) {
                    it.getDouble(it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_GPS_SPEED))
                } else {
                    0.0
                }

                val cadence = if (!it.isNull(it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_CADENCE))) {
                    it.getDouble(it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_CADENCE))
                } else {
                    0.0
                }

                val power = if (!it.isNull(it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_POWER))) {
                    it.getDouble(it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_POWER))
                } else {
                    0.0
                }

                val altitude = it.getDouble(it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_ALTITUDE))

                chartData.add(mapOf(
                    "timestamp" to timestamp,
                    "speed" to speed,
                    "cadence" to cadence,
                    "power" to power,
                    "altitude" to altitude
                ))

            } while (it.moveToNext())
        }

        return chartData
    }
}