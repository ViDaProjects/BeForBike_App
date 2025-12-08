package com.beforbike.app.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

class RideDbHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        // Incrementado para v11 para adicionar as novas colunas
        const val DATABASE_VERSION = 19
        const val DATABASE_NAME = "BikeRides.db"
        private const val TAG = "RideDbHelper"

        // --- Tabela 1: Rides (Resumo) ---
        object RidesEntry : BaseColumns {
            const val TABLE_NAME = "Rides"
            const val COLUMN_RIDE_ID = "ride_id"
            const val COLUMN_START_TIME = "start_time"
            const val COLUMN_END_TIME = "end_time"

            const val COLUMN_TOTAL_DISTANCE_KM = "total_distance_km"
            const val COLUMN_CALORIES = "calories"

            // Velocidade
            const val COLUMN_AVG_VELOCITY_KMH = "avg_velocity_kmh"
            const val COLUMN_MAX_VELOCITY_KMH = "max_velocity_kmh" // Novo

            // Potência
            const val COLUMN_AVG_POWER = "avg_power"
            const val COLUMN_MAX_POWER = "max_power" // Novo

            // Cadência
            const val COLUMN_AVG_CADENCE = "avg_cadence"
            const val COLUMN_MAX_CADENCE = "max_cadence" // Novo

            // Altitude
            const val COLUMN_AVG_ALTITUDE = "avg_altitude" // Novo
            const val COLUMN_MAX_ALTITUDE = "max_altitude" // Novo
        }

        // --- Tabela 2: TelemetryData (Dados Brutos) ---
        object TelemetryEntry : BaseColumns {
            const val TABLE_NAME = "TelemetryData"
            const val COLUMN_TELEMETRY_ID = "_id"
            const val COLUMN_RIDE_ID = "ride_id"

            // Campos Cruciais para o Tempo agora
            const val COLUMN_PACKET_DATE = "packet_date" // "yyyy-MM-dd HH:mm:ss"
            const val COLUMN_PACKET_TIME = "packet_time"

            const val COLUMN_GPS_TIMESTAMP = "gps_timestamp"
            const val COLUMN_LATITUDE = "latitude"
            const val COLUMN_LONGITUDE = "longitude"
            const val COLUMN_ALTITUDE = "altitude"
            const val COLUMN_GPS_SPEED = "gps_speed"
            const val COLUMN_DIRECTION = "direction"
            const val COLUMN_FIX_SATELLITES = "fix_satellites"
            const val COLUMN_FIX_QUALITY = "fix_quality"

            const val COLUMN_POWER = "power"
            const val COLUMN_CADENCE = "cadence"
            const val COLUMN_JOULES = "joules"
            const val COLUMN_CRANK_CALORIES = "crank_calories"
            const val COLUMN_CRANK_SPEED_MS = "crank_speed_ms"
            const val COLUMN_CRANK_SPEED = "crank_speed"
            const val COLUMN_CRANK_DISTANCE = "crank_distance"
        }

        // --- SQL Creation ---
        private const val SQL_CREATE_RIDES =
            "CREATE TABLE ${RidesEntry.TABLE_NAME} (" +
                    "${RidesEntry.COLUMN_RIDE_ID} INTEGER PRIMARY KEY," +
                    "${RidesEntry.COLUMN_START_TIME} TEXT NOT NULL," +
                    "${RidesEntry.COLUMN_END_TIME} TEXT," +
                    "${RidesEntry.COLUMN_TOTAL_DISTANCE_KM} REAL," +
                    "${RidesEntry.COLUMN_CALORIES} REAL," +
                    "${RidesEntry.COLUMN_AVG_VELOCITY_KMH} REAL," +
                    "${RidesEntry.COLUMN_MAX_VELOCITY_KMH} REAL," +
                    "${RidesEntry.COLUMN_AVG_POWER} REAL," +
                    "${RidesEntry.COLUMN_MAX_POWER} REAL," +
                    "${RidesEntry.COLUMN_AVG_CADENCE} REAL," +
                    "${RidesEntry.COLUMN_MAX_CADENCE} REAL," +
                    "${RidesEntry.COLUMN_AVG_ALTITUDE} REAL," +
                    "${RidesEntry.COLUMN_MAX_ALTITUDE} REAL)"

        private const val SQL_CREATE_TELEMETRY_DATA =
            "CREATE TABLE ${TelemetryEntry.TABLE_NAME} (" +
                    "${TelemetryEntry.COLUMN_TELEMETRY_ID} INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "${TelemetryEntry.COLUMN_RIDE_ID} INTEGER NOT NULL," +
                    "${TelemetryEntry.COLUMN_PACKET_DATE} TEXT," +
                    "${TelemetryEntry.COLUMN_PACKET_TIME} TEXT," +
                    "${TelemetryEntry.COLUMN_GPS_TIMESTAMP} TEXT," +
                    "${TelemetryEntry.COLUMN_LATITUDE} REAL," +
                    "${TelemetryEntry.COLUMN_LONGITUDE} REAL," +
                    "${TelemetryEntry.COLUMN_ALTITUDE} REAL," +
                    "${TelemetryEntry.COLUMN_GPS_SPEED} REAL," +
                    "${TelemetryEntry.COLUMN_DIRECTION} REAL," +
                    "${TelemetryEntry.COLUMN_FIX_SATELLITES} INTEGER," +
                    "${TelemetryEntry.COLUMN_FIX_QUALITY} INTEGER," +
                    "${TelemetryEntry.COLUMN_POWER} REAL," +
                    "${TelemetryEntry.COLUMN_CADENCE} REAL," +
                    "${TelemetryEntry.COLUMN_JOULES} REAL," +
                    "${TelemetryEntry.COLUMN_CRANK_CALORIES} REAL," +
                    "${TelemetryEntry.COLUMN_CRANK_SPEED_MS} REAL," +
                    "${TelemetryEntry.COLUMN_CRANK_SPEED} REAL," +
                    "${TelemetryEntry.COLUMN_CRANK_DISTANCE} REAL," +
                    "FOREIGN KEY (${TelemetryEntry.COLUMN_RIDE_ID}) REFERENCES ${RidesEntry.TABLE_NAME}(${RidesEntry.COLUMN_RIDE_ID}) ON DELETE CASCADE)"

        private const val SQL_DELETE_RIDES = "DROP TABLE IF EXISTS ${RidesEntry.TABLE_NAME}"
        private const val SQL_DELETE_TELEMETRY_DATA = "DROP TABLE IF EXISTS ${TelemetryEntry.TABLE_NAME}"

        // Comandos de limpeza antigos (mantidos para compatibilidade com seu código original)
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
        Log.i(TAG, "Criando tabelas do banco de dados (v$DATABASE_VERSION)...")
        db.execSQL(SQL_CREATE_RIDES)
        db.execSQL(SQL_CREATE_TELEMETRY_DATA)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.w(TAG, "Atualizando DB de v$oldVersion para v$newVersion.")
        db.execSQL(SQL_DELETE_TELEMETRY_DATA)
        db.execSQL(SQL_DELETE_OLD_POWER)
        db.execSQL(SQL_DELETE_OLD_MAPDATA)
        db.execSQL(SQL_DELETE_OLD_VELOCITY)
        db.execSQL(SQL_DELETE_OLD_CADENCE)
        db.execSQL(SQL_DELETE_RIDES)
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }

    private fun getCurrentTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
    }

    // --- FUNÇÕES DE INSERÇÃO ---

    fun ensureRideExists(rideIdFromDevice: Long, startTime: String?): Boolean {
        if (rideIdFromDevice <= 0) return false
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(RidesEntry.COLUMN_RIDE_ID, rideIdFromDevice)
            put(RidesEntry.COLUMN_START_TIME, startTime ?: getCurrentTimestamp())
        }
        try {
            db.insertWithOnConflict(RidesEntry.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ensureRideExists: ${e.message}")
            return false
        }
    }

    fun insertTelemetryData(
        rideId: Long,
        infoMap: Map<String, Any?>,
        gpsMap: Map<String, Any?>,
        crankMap: Map<String, Any?>?
    ): Boolean {
        if (rideId <= 0) return false
        val db = this.writableDatabase

        val values = ContentValues().apply {
            put(TelemetryEntry.COLUMN_RIDE_ID, rideId)

            put(TelemetryEntry.COLUMN_PACKET_DATE, infoMap["date"] as? String)
            put(TelemetryEntry.COLUMN_PACKET_TIME, infoMap["time"] as? String)

            put(TelemetryEntry.COLUMN_GPS_TIMESTAMP, gpsMap["timestamp"] as? String)
            put(TelemetryEntry.COLUMN_LATITUDE, (gpsMap["latitude"] as? Number)?.toDouble())
            put(TelemetryEntry.COLUMN_LONGITUDE, (gpsMap["longitude"] as? Number)?.toDouble())
            put(TelemetryEntry.COLUMN_ALTITUDE, (gpsMap["altitude"] as? Number)?.toDouble())
            put(TelemetryEntry.COLUMN_GPS_SPEED, (gpsMap["speed"] as? Number)?.toDouble())
            put(TelemetryEntry.COLUMN_DIRECTION, (gpsMap["direction"] as? Number)?.toDouble())
            put(TelemetryEntry.COLUMN_FIX_SATELLITES, gpsMap["fix_satellites"] as? Int)
            put(TelemetryEntry.COLUMN_FIX_QUALITY, gpsMap["fix_quality"] as? Int)

            crankMap?.let {
                // AQUI ESTAVA O BUG: Usamos (Number)?.toDouble() para aceitar Zeros
                put(TelemetryEntry.COLUMN_POWER, (it["power"] as? Number)?.toDouble())
                put(TelemetryEntry.COLUMN_CADENCE, (it["cadence"] as? Number)?.toDouble())
                put(TelemetryEntry.COLUMN_JOULES, (it["joules"] as? Number)?.toDouble())
                put(TelemetryEntry.COLUMN_CRANK_CALORIES, (it["calories"] as? Number)?.toDouble())
                put(TelemetryEntry.COLUMN_CRANK_SPEED_MS, (it["speed_ms"] as? Number)?.toDouble())
                put(TelemetryEntry.COLUMN_CRANK_SPEED, (it["speed"] as? Number)?.toDouble())
                put(TelemetryEntry.COLUMN_CRANK_DISTANCE, (it["distance"] as? Number)?.toDouble())
            }
        }
        return db.insert(TelemetryEntry.TABLE_NAME, null, values) != -1L
    }

    // --- FUNÇÕES DE CONSULTA ---

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

    fun getRideSummary(rideId: Long): Map<String, Any?>? {
        val db = this.readableDatabase
        val cursor = db.query(
            RidesEntry.TABLE_NAME, null,
            "${RidesEntry.COLUMN_RIDE_ID} = ?", arrayOf(rideId.toString()),
            null, null, null
        )

        cursor.use {
            if (it.moveToFirst()) {
                val map = mutableMapOf<String, Any?>()
                for (i in 0 until it.columnCount) {
                    val colName = it.getColumnName(i)
                    when (it.getType(i)) {
                        Cursor.FIELD_TYPE_INTEGER -> map[colName] = it.getLong(i)
                        Cursor.FIELD_TYPE_FLOAT -> map[colName] = it.getDouble(i)
                        Cursor.FIELD_TYPE_STRING -> map[colName] = it.getString(i)
                        else -> map[colName] = null
                    }
                }

                // Se a distância for 0, tenta recalcular
                val dist = map[RidesEntry.COLUMN_TOTAL_DISTANCE_KM] as? Double ?: 0.0
                if (dist == 0.0) {
                    val stats = calculateRideStatistics(rideId)
                    if (stats != null) {
                        updateRideSummary(rideId, stats)
                        map.putAll(stats)
                    }
                }
                return map
            }
        }
        return null
    }

    /**
     * Lógica REFEITA para usar info.date ("yyyy-MM-dd HH:mm:ss")
     */
    /**
     * Lógica REFEITA e CORRIGIDA conforme os TODOs:
     * 1. Distância: Pega o valor acumulado do último pacote (Crank Distance).
     * 2. Velocidade: Prioriza Crank Speed sobre GPS Speed.
     * 3. Cadência: Considera que o valor já vem somado/médio, pega o último registro para a média.
     * 4. Calorias: Pega o valor acumulado do último pacote.
     */
    fun calculateRideStatistics(rideId: Long): Map<String, Any?>? {
        val db = this.readableDatabase

        val cursor = db.query(
            TelemetryEntry.TABLE_NAME,
            arrayOf(
                TelemetryEntry.COLUMN_PACKET_DATE,
                TelemetryEntry.COLUMN_ALTITUDE,
                TelemetryEntry.COLUMN_GPS_SPEED,
                TelemetryEntry.COLUMN_CRANK_SPEED,
                TelemetryEntry.COLUMN_CRANK_DISTANCE,
                TelemetryEntry.COLUMN_POWER,
                TelemetryEntry.COLUMN_CADENCE,
                TelemetryEntry.COLUMN_CRANK_CALORIES
            ),
            "${TelemetryEntry.COLUMN_RIDE_ID} = ?", arrayOf(rideId.toString()),
            null, null,
            "${TelemetryEntry.COLUMN_PACKET_DATE} ASC"
        )

        if (!cursor.moveToFirst()) {
            cursor.close()
            return null
        }

        var startTime: Long? = null
        var endTime: Long? = null
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        // Variáveis para Máximas
        var maxSpeed = 0.0
        var maxPower = 0.0
        var maxCadence = 0.0
        var maxAltitude = 0.0

        // Listas para calcular as médias
        val speeds = mutableListOf<Double>()
        val powers = mutableListOf<Double>()
        val cadences = mutableListOf<Double>()
        val altitudes = mutableListOf<Double>()

        // Acumuladores finais
        var lastTotalDistanceMeters = 0.0
        var lastTotalCalories = 0.0

        cursor.use {
            // Índices das colunas
            val dateIdx = it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_PACKET_DATE)
            val altIdx = it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_ALTITUDE)
            val gpsSpeedIdx = it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_GPS_SPEED)
            val crankSpeedIdx = it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_CRANK_SPEED)
            val crankDistIdx = it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_CRANK_DISTANCE)
            val powerIdx = it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_POWER)
            val cadIdx = it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_CADENCE)
            val calIdx = it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_CRANK_CALORIES)

            do {
                // 1. Tempo
                val dateStr = it.getString(dateIdx)
                if (dateStr != null) {
                    try {
                        val date = dateFormat.parse(dateStr)
                        if (date != null) {
                            val ts = date.time
                            if (startTime == null) startTime = ts
                            endTime = ts
                        }
                    } catch (e: Exception) { Log.e(TAG, "Erro parse data: $dateStr") }
                }

                // 2. Distância (Acumulada em Metros)
                if (!it.isNull(crankDistIdx)) {
                    val dist = it.getDouble(crankDistIdx)
                    if (dist > lastTotalDistanceMeters) lastTotalDistanceMeters = dist
                }

                // 3. Altitude (Sempre adicionamos, pois existe altitude mesmo parado)
                if (!it.isNull(altIdx)) {
                    val alt = it.getDouble(altIdx)
                    // Filtramos 0 absoluto pois geralmente é erro de GPS iniciando,
                    // mas não filtramos "parado"
                    if (alt != 0.0) {
                        altitudes.add(alt)
                        if (alt > maxAltitude) maxAltitude = alt
                    }
                }

                // 4. Velocidade
                val cSpeed = if (!it.isNull(crankSpeedIdx)) it.getDouble(crankSpeedIdx) else null
                val gSpeed = if (!it.isNull(gpsSpeedIdx)) it.getDouble(gpsSpeedIdx) else null
                // Prioriza sensor de roda, senão GPS, senão 0
                val currentSpeed = cSpeed ?: gSpeed ?: 0.0

                speeds.add(currentSpeed) // Adiciona na lista para média
                if (currentSpeed > maxSpeed) maxSpeed = currentSpeed

                // 5. Potência
                val p = if (!it.isNull(powerIdx)) it.getDouble(powerIdx) else 0.0
                powers.add(p)
                if (p > maxPower) maxPower = p

                // 6. Cadência
                val c = if (!it.isNull(cadIdx)) it.getDouble(cadIdx) else 0.0
                cadences.add(c)
                if (c > maxCadence) maxCadence = c

                // 7. Calorias
                if (!it.isNull(calIdx)) {
                    val cal = it.getDouble(calIdx)
                    if (cal > lastTotalCalories) lastTotalCalories = cal
                }

            } while (it.moveToNext())
        }

        // --- CÁLCULOS FINAIS COM LÓGICA DE "ATIVA" (IGNORANDO ZEROS) ---

        // 1. Correção de Unidade: Metros para KM
        val totalDistanceKm = lastTotalDistanceMeters / 1000.0

        // 2. Velocidade Média em Movimento (Average Moving Speed)
        // Filtramos velocidades muito baixas (< 1.0 km/h) que são ruído
        val activeSpeeds = speeds.filter { it > 1.0 }
        val avgSpeed = if (activeSpeeds.isNotEmpty()) activeSpeeds.average() else 0.0

        // 3. Potência Média Ativa (Average Active Power)
        // Filtramos potências < 1.0 Watt (ignora roda livre/parado)
        val activePowers = powers.filter { it > 1.0 }
        val avgPower = if (activePowers.isNotEmpty()) activePowers.average() else 0.0

        // 4. Cadência Média Ativa
        // Filtramos zeros (ignora quando parou de pedalar)
        val activeCadences = cadences.filter { it > 0 }
        val avgCadence = if (activeCadences.isNotEmpty()) activeCadences.average() else 0.0

        // 5. Altitude Média (Geralmente se usa média total)
        val avgAltitude = if (altitudes.isNotEmpty()) altitudes.average() else 0.0

        val outputDateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

        return mapOf(
            "ride_id" to rideId,
            "start_time" to if (startTime != null) outputDateFmt.format(Date(startTime!!)) else null,
            "end_time" to if (endTime != null) outputDateFmt.format(Date(endTime!!)) else null,

            "total_distance_km" to totalDistanceKm, // Agora corrigido para KM
            "calories" to lastTotalCalories,

            "max_velocity_kmh" to maxSpeed,
            "avg_velocity_kmh" to avgSpeed, // Média apenas em movimento

            "max_power" to maxPower,
            "avg_power" to avgPower, // Média apenas pedalando

            "max_cadence" to maxCadence,
            "avg_cadence" to avgCadence, // Média apenas pedalando

            "max_altitude" to maxAltitude,
            "avg_altitude" to avgAltitude
        )
    }

    fun updateRideSummary(rideId: Long, stats: Map<String, Any?>): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            stats["start_time"]?.let { put(RidesEntry.COLUMN_START_TIME, it as String) }
            stats["end_time"]?.let { put(RidesEntry.COLUMN_END_TIME, it as String) }

            stats["total_distance_km"]?.let { put(RidesEntry.COLUMN_TOTAL_DISTANCE_KM, it as Double) }
            stats["calories"]?.let { put(RidesEntry.COLUMN_CALORIES, it as Double) }

            stats["avg_velocity_kmh"]?.let { put(RidesEntry.COLUMN_AVG_VELOCITY_KMH, it as Double) }
            stats["max_velocity_kmh"]?.let { put(RidesEntry.COLUMN_MAX_VELOCITY_KMH, it as Double) }

            stats["avg_power"]?.let { put(RidesEntry.COLUMN_AVG_POWER, it as Double) }
            stats["max_power"]?.let { put(RidesEntry.COLUMN_MAX_POWER, it as Double) }

            stats["avg_cadence"]?.let { put(RidesEntry.COLUMN_AVG_CADENCE, it as Double) }
            stats["max_cadence"]?.let { put(RidesEntry.COLUMN_MAX_CADENCE, it as Double) }

            stats["avg_altitude"]?.let { put(RidesEntry.COLUMN_AVG_ALTITUDE, it as Double) }
            stats["max_altitude"]?.let { put(RidesEntry.COLUMN_MAX_ALTITUDE, it as Double) }
        }

        if (values.size() == 0) return false

        return db.update(
            RidesEntry.TABLE_NAME,
            values,
            "${RidesEntry.COLUMN_RIDE_ID} = ?",
            arrayOf(rideId.toString())
        ) > 0
    }

    // Função que recupera os dados brutos (Restaurada do seu código original)
    fun getRideTelemetryData(rideId: Long): List<Map<String, Any?>> {
        val db = this.readableDatabase
        val cursor = db.query(
            TelemetryEntry.TABLE_NAME,
            null,
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
    }

    // =========================================================================
    // MÉTODOS DE COMPATIBILIDADE (Adicione isso antes do último '}' da classe)
    // =========================================================================

    /**
     * Recupera as estatísticas usando a NOVA lógica corrigida (calculateRideStatistics),
     * mas renomeia as chaves para o formato antigo que o MainActivity espera
     * (ex: "max_velocity_kmh" vira "max_speed").
     */
    fun getActivityStatistics(rideId: Long): Map<String, Any?>? {
        // Usa a lógica nova que corrige Distância, Cadência, etc.
        val stats = calculateRideStatistics(rideId) ?: return null

        // Mapeia para os nomes antigos que o Flutter/MainActivity usa
        return mapOf(
            "max_speed" to stats["max_velocity_kmh"],
            "avg_speed" to stats["avg_velocity_kmh"],
            "max_cadence" to stats["max_cadence"],
            "avg_cadence" to stats["avg_cadence"],
            "max_power" to stats["max_power"],
            "avg_power" to stats["avg_power"],
            "max_altitude" to stats["max_altitude"],
            "avg_altitude" to stats["avg_altitude"]
        )
    }

    /**
     * Retorna os dados brutos para os gráficos do Flutter.
     * (Restaurado do seu código original para garantir compatibilidade com os gráficos)
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

        // Formatadores de data robustos (do seu código original)
        val dateFormatFallbackMillis = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        val dateFormatFallback = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
        val dateFormatSlashes = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())

        val chartData = mutableListOf<Map<String, Any?>>()

        cursor.use {
            do {
                val timestampStr = it.getString(it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_GPS_TIMESTAMP))

                // Tenta fazer o parse da data de várias formas
                val timestamp = try {
                    dateFormatFallbackMillis.parse(timestampStr)?.time
                } catch (e: Exception) {
                    try {
                        dateFormatFallback.parse(timestampStr)?.time
                    } catch (e2: Exception) {
                        try {
                            dateFormat.parse(timestampStr)?.time
                        } catch (e3: Exception) {
                            System.currentTimeMillis() // Fallback final
                        }
                    }
                } ?: System.currentTimeMillis()

                val speed = if (!it.isNull(it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_GPS_SPEED))) {
                    it.getDouble(it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_GPS_SPEED))
                } else 0.0

                val cadence = if (!it.isNull(it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_CADENCE))) {
                    it.getDouble(it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_CADENCE))
                } else 0.0

                val power = if (!it.isNull(it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_POWER))) {
                    it.getDouble(it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_POWER))
                } else 0.0

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