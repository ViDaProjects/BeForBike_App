package com.beforbike.app.database

import android.content.Context
import android.util.Log
import com.beforbike.app.database.RideDbHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SeedData {

    private const val SAMPLE_RIDE_ID = 777L
    private const val RIDES_TABLE_NAME = "Rides"
    private const val TELEMETRY_TABLE_NAME = "TelemetryData"
    private const val COLUMN_RIDE_ID = "ride_id"
    private const val TAG = "BeForBike"

    fun insertSampleRide(context: Context) {
        val dbHelper = RideDbHelper(context)
        Log.d(TAG, "Checking if sample ride $SAMPLE_RIDE_ID already exists")

        // 1. Limpeza prévia para garantir dados frescos
        // (Optei por remover sempre para garantir que o seed reflita mudanças na estrutura)
        removeSampleRide(context)

        Log.d(TAG, "Proceeding with insertion")

        // Data base: 30 Nov 2024
        val baseTime = 1732920000000L

        val path = listOf(
            Triple(-25.4290f, -49.2721f, 880f),
            Triple(-25.4270f, -49.2700f, 885f),
            Triple(-25.4310f, -49.2680f, 890f),
            Triple(-25.4300f, -49.2660f, 895f),
            Triple(-25.4250f, -49.2640f, 900f)
        )

        val totalPoints = path.size
        // 75 segundos entre pontos
        val timeIntervalSeconds = 75

        val velocities = listOf(17.5f, 18.2f, 16.8f, 19.1f, 17.9f)
        val powers = listOf(165f, 172f, 158f, 185f, 175f)
        val cadences = listOf(88f, 92f, 85f, 95f, 89f)

        // Distâncias e calorias CUMULATIVAS (Importante para a lógica do seu DbHelper)
        val cumulativeDistances = listOf(0.3, 0.8, 1.1, 1.4, 1.7)
        val cumulativeCalories = listOf(12.4, 25.3, 37.1, 51.0, 64.1)

        // Formatadores de data essenciais
        // O DbHelper usa "yyyy-MM-dd HH:mm:ss" para calcular estatísticas
        val dbDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val timeOnlyFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        // O formato GPS geralmente inclui milissegundos
        val gpsTimestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

        val startDate = Date(baseTime)

        // Cria o registro na tabela pai (Rides)
        if (!dbHelper.ensureRideExists(SAMPLE_RIDE_ID, gpsTimestampFormat.format(startDate))) {
            Log.e(TAG, "Failed to ensure ride exists")
            return
        }

        Log.d(TAG, "Inserting $totalPoints GPS points for sample ride")

        path.forEachIndexed { index, (lat, lon, alt) ->
            val timestamp = baseTime + (index * timeIntervalSeconds * 1000L)
            val currentDate = Date(timestamp)

            val dateStr = dbDateFormat.format(currentDate) // "2024-11-30 00:01:15"
            val timeStr = timeOnlyFormat.format(currentDate)
            val gpsTsStr = gpsTimestampFormat.format(currentDate)

            // CRUCIAL: "date" deve ser datetime completo para o parser do DbHelper funcionar
            val infoMap = mapOf(
                "date" to dateStr,
                "time" to timeStr
            )

            val gpsMap = mapOf(
                "timestamp" to gpsTsStr,
                "latitude" to lat.toDouble(),
                "longitude" to lon.toDouble(),
                "altitude" to alt.toDouble(),
                "speed" to (velocities[index] / 3.6), // km/h -> m/s
                "direction" to 0.0,
                "fix_satellites" to 8,
                "fix_quality" to 1
            )

            val crankMap = mapOf(
                "power" to powers[index].toDouble(),
                "cadence" to cadences[index].toDouble(),
                "joules" to 0.0,
                "calories" to cumulativeCalories[index], // Deve ser cumulativo
                "speed_ms" to (velocities[index] / 3.6),
                "speed" to velocities[index].toDouble(), // Prioridade no cálculo de Max Speed
                "distance" to cumulativeDistances[index] // Deve ser cumulativo
            )

            val success = dbHelper.insertTelemetryData(SAMPLE_RIDE_ID, infoMap, gpsMap, crankMap)
            if (!success) {
                Log.w(TAG, "Failed to insert telemetry data for point $index")
            }
        }

        // Força o recálculo do sumário para preencher a tabela Rides com médias/máximas corretas
        val stats = dbHelper.calculateRideStatistics(SAMPLE_RIDE_ID)
        if (stats != null) {
            dbHelper.updateRideSummary(SAMPLE_RIDE_ID, stats)
            Log.d(TAG, "Ride summary updated with calculated stats")
        }

        Log.d(TAG, "Sample ride insertion completed")
    }

    fun removeSampleRide(context: Context) {
        val dbHelper = RideDbHelper(context)
        val db = dbHelper.writableDatabase
        try {
            db.delete(TELEMETRY_TABLE_NAME, "$COLUMN_RIDE_ID = ?", arrayOf(SAMPLE_RIDE_ID.toString()))
            db.delete(RIDES_TABLE_NAME, "$COLUMN_RIDE_ID = ?", arrayOf(SAMPLE_RIDE_ID.toString()))
            Log.d(TAG, "Old sample ride removed")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing sample ride: ${e.message}")
        }
    }
}