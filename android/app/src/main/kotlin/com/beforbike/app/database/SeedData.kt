package com.beforbike.app.database

import android.content.Context
import android.provider.BaseColumns
import com.beforbike.app.database.RideDbHelper

object SeedData {

    private const val SAMPLE_RIDE_ID = 777L
    private const val RIDES_TABLE_NAME = "Rides"
    private const val TELEMETRY_TABLE_NAME = "TelemetryData"
    private const val COLUMN_RIDE_ID = "ride_id"

    fun insertSampleRide(context: Context) {
        val dbHelper = RideDbHelper(context)
        android.util.Log.d("BeForBike", "Checking if sample ride $SAMPLE_RIDE_ID already exists")

        // Check if sample ride already exists
        val existingData = dbHelper.getRideSummary(SAMPLE_RIDE_ID)
        if (existingData != null) {
            android.util.Log.d("BeForBike", "Sample ride $SAMPLE_RIDE_ID already exists, skipping insertion")
            return
        }

        android.util.Log.d("BeForBike", "Sample ride does not exist, proceeding with insertion")
        // Remove any existing sample ride (cleanup)
        removeSampleRide(context)

        android.util.Log.d("BeForBike", "Proceeding with insertion")
        // Use a fixed historical time for the sample ride instead of current time
        val baseTime = 1732920000000L // November 30, 2024, 00:00:00 UTC (fixed historical time)
        val path = listOf(
            // Start: -25.4290, -49.2721
            Triple(-25.4290f, -49.2721f, 880f),
            // Checkpoint 1: -25.4270, -49.2700
            Triple(-25.4270f, -49.2700f, 885f),
            // Checkpoint 2: -25.4250, -49.2680
            Triple(-25.4310f, -49.2680f, 890f),
            // Checkpoint 3: -25.4230, -49.2660
            Triple(-25.4300f, -49.2660f, 895f),
            // End: -25.4210, -49.2640
            Triple(-25.4250f, -49.2640f, 900f)
        )

        val totalPoints = path.size
        val timeIntervalSeconds = 75 // 75 seconds between each checkpoint for a 5-minute ride (4 intervals * 75s = 300s = 5 minutes)

        // Generate corresponding velocities, powers, and cadences (5 values for 5 points)
        val velocities = listOf(17.5f, 18.2f, 16.8f, 19.1f, 17.9f)
        val powers = listOf(165f, 172f, 158f, 185f, 175f)
        val cadences = listOf(88f, 92f, 85f, 95f, 89f)

        // Pre-calculated cumulative distances and calories for the sample ride
        val cumulativeDistances = listOf(0.3, 0.8, 1.1, 1.4, 1.7) // km
        val cumulativeCalories = listOf(12.4, 25.3, 37.1, 51.0, 64.1) // calories

        // Ensure ride exists
        val startTimeStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date(baseTime))
        if (!dbHelper.ensureRideExists(SAMPLE_RIDE_ID, startTimeStr)) {
            android.util.Log.e("BeForBike", "Failed to ensure ride exists")
            return
        }

        android.util.Log.d("BeForBike", "Inserting $totalPoints GPS points for sample ride")
        path.forEachIndexed { index, (lat, lon, alt) ->
            val timestamp = baseTime + index * timeIntervalSeconds * 1000L // 75 seconds apart
            val timestampStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date(timestamp))

            val infoMap = mapOf(
                "date" to java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(timestamp)),
                "time" to java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
            )
            val gpsMap = mapOf(
                "timestamp" to timestampStr,
                "latitude" to lat.toDouble(),
                "longitude" to lon.toDouble(),
                "altitude" to alt.toDouble(),
                "speed" to velocities[index] / 3.6, // Convert km/h to m/s
                "direction" to 0.0,
                "fix_satellites" to 8,
                "fix_quality" to 1
            )
            val crankMap = mapOf(
                "power" to powers[index].toDouble(),
                "cadence" to cadences[index].toDouble(),
                "joules" to 0.0,
                "calories" to cumulativeCalories[index], // Pre-calculated cumulative calories
                "speed_ms" to velocities[index] / 3.6, // Convert km/h to m/s
                "speed" to velocities[index].toDouble(),
                "distance" to cumulativeDistances[index] // Pre-calculated cumulative distance
            )

            val success = dbHelper.insertTelemetryData(SAMPLE_RIDE_ID, infoMap, gpsMap, crankMap)
            if (!success) {
                android.util.Log.w("BeForBike", "Failed to insert telemetry data for point $index")
            } else {
                android.util.Log.d("BeForBike", "Successfully inserted telemetry data for point $index")
            }
        }
        android.util.Log.d("BeForBike", "Sample ride insertion completed")
    }

    fun removeSampleRide(context: Context) {
        val dbHelper = RideDbHelper(context)
        val db = dbHelper.writableDatabase

        // Delete telemetry data
        db.delete(TELEMETRY_TABLE_NAME, "$COLUMN_RIDE_ID = ?", arrayOf(SAMPLE_RIDE_ID.toString()))
        // Delete ride summary
        db.delete(RIDES_TABLE_NAME, "$COLUMN_RIDE_ID = ?", arrayOf(SAMPLE_RIDE_ID.toString()))
    }
}
