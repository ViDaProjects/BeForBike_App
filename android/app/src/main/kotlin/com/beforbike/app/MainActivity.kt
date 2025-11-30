package com.beforbike.app

import io.flutter.embedding.android.FlutterActivity
import io.flutter.plugin.common.MethodChannel
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.annotation.SuppressLint
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.os.ParcelUuid
import android.util.Log
import com.beforbike.app.BleServerService // Import the service
import com.beforbike.app.database.RideDbHelper
import com.beforbike.app.database.SeedData
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity: FlutterActivity() {
    private val CHANNEL = "com.beforbike.ble"
    private val TAG = "MainActivity"

    // Enable/disable seed data insertion for testing
    private val ENABLE_SEED_DATA = true

    // Variables for asynchronous permission logic
    private var permissionRequestResult: MethodChannel.Result? = null
    private val REQUEST_CODE_PERMISSIONS = 1 // Permission request code

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var connectedDevice: BluetoothDevice? = null
    private lateinit var dbHelper: RideDbHelper
    private val SCAN_PERIOD: Long = 10000
    private var scanning = false
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    /**
     * Callback for when the user responds to the permission request.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults) // Important

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            val allGranted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }

            if (allGranted) {
                Log.d("BLE", "All permissions were granted by the user.")
                // Sends success BACK to Flutter
                this.permissionRequestResult?.success(true)
            } else {
                Log.e("BLE", "Permissions denied by user.")
                // Sends an error BACK to Flutter
                this.permissionRequestResult?.error("PERMISSIONS_DENIED", "User denied the permissions", null)
            }
            // Clears the 'result' for next time
            this.permissionRequestResult = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dbHelper = RideDbHelper(this)

        // Enable seed data - run asynchronously to avoid blocking UI thread
        if (ENABLE_SEED_DATA) {
            Log.d("MainActivity", "ENABLE_SEED_DATA is true, scheduling insertSampleRide on background thread")
            // Run seed data insertion on background thread to avoid blocking UI
            Thread {
                try {
                    SeedData.insertSampleRide(applicationContext)
                    Log.d("MainActivity", "Seed data insertion completed on background thread")
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error inserting seed data", e)
                }
            }.start()
        } else {
            Log.d("MainActivity", "ENABLE_SEED_DATA is false, skipping seed data")
        }

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser

        // Starts the service ONLY if it's not running (using the static flag)
        if (bluetoothAdapter.isEnabled && !BleServerService.isServiceRunning) {
            Log.d("BLE", "Scheduling BLE service start on background thread to avoid blocking UI")
            // Start BLE service on background thread to avoid blocking main thread
            Thread {
                try {
                    Thread.sleep(100) // Small delay to let UI initialize first
                    val intent = Intent(applicationContext, BleServerService::class.java)
                    startService(intent)
                    Log.d("BLE", "BLE service started on background thread")
                } catch (e: Exception) {
                    Log.e("BLE", "Error starting BLE service", e)
                }
            }.start()
        } else if (BleServerService.isServiceRunning) {
            Log.d("BLE", "Service already running, no need to start automatically.")
        }

        MethodChannel(flutterEngine?.dartExecutor?.binaryMessenger!!, "com.beforbike.app/database").setMethodCallHandler { call, result ->
            when (call.method) {
                "getAllActivities" -> {
                    Thread {
                        try {
                            val rideIds = dbHelper.getAllRideIds()
                            val activities = mutableListOf<Map<String, Any>>()
                            for (id in rideIds) {
                                val data = dbHelper.getRideSummary(id)
                                if (data != null) {
                                    val activity = mapRideToActivity(id, data)
                                    activities.add(activity)
                                }
                            }
                            runOnUiThread { result.success(activities) }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error getting all activities: ${e.message}")
                            runOnUiThread { result.error("ACTIVITIES_ERROR", e.message, null) }
                        }
                    }.start()
                }
                "getActivityLocations" -> {
                    val activityId = call.argument<String>("activityId") ?: ""
                    val rideId = activityId.toLongOrNull() ?: 0L
                    Thread {
                        try {
                            val telemetryData = dbHelper.getRideTelemetryData(rideId)
                            val locations = telemetryData.mapNotNull { data ->
                                val timestampStr = data["gps_timestamp"] as? String
                                val lat = data["latitude"] as? Double
                                val lon = data["longitude"] as? Double
                                if (timestampStr != null && lat != null && lon != null) {
                                    val timestamp = try {
                                        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
                                        dateFormat.parse(timestampStr)?.time ?: System.currentTimeMillis()
                                    } catch (e: Exception) {
                                        try {
                                            val dateFormatFallback = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
                                            dateFormatFallback.parse(timestampStr)?.time ?: System.currentTimeMillis()
                                        } catch (e2: Exception) {
                                            try {
                                                val dateFormatFallbackNoMillis = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                                                dateFormatFallbackNoMillis.parse(timestampStr)?.time ?: System.currentTimeMillis()
                                            } catch (e3: Exception) {
                                                try {
                                                    val dateFormatSlashes = SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS", Locale.getDefault())
                                                    dateFormatSlashes.parse(timestampStr)?.time ?: System.currentTimeMillis()
                                                } catch (e4: Exception) {
                                                    try {
                                                        val dateFormatSlashesNoMillis = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
                                                        dateFormatSlashesNoMillis.parse(timestampStr)?.time ?: System.currentTimeMillis()
                                                    } catch (e5: Exception) {
                                                        System.currentTimeMillis()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    mapOf(
                                        "id" to "loc_${timestampStr}_${System.currentTimeMillis()}",
                                        "datetime" to timestamp,
                                        "latitude" to lat,
                                        "longitude" to lon
                                    )
                                } else null
                            }
                            runOnUiThread { result.success(locations) }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error getting activity locations: ${e.message}")
                            runOnUiThread { result.error("LOCATIONS_ERROR", e.message, null) }
                        }
                    }.start()
                }
                "getActivityData" -> {
                    val activityId = call.argument<String>("activityId") ?: ""
                    val rideId = activityId.toLongOrNull() ?: 0L
                    // Run database operation on background thread to avoid blocking UI
                    Thread {
                        try {
                            val stats = dbHelper.getActivityStatistics(rideId)
                            handler.post {
                                if (stats != null) {
                                    result.success(stats)
                                } else {
                                    result.success(emptyMap<String, Any>())
                                }
                            }
                        } catch (e: Exception) {
                            handler.post {
                                result.success(emptyMap<String, Any>())
                            }
                        }
                    }.start()
                }
                "getActivityChartData" -> {
                    val activityId = call.argument<String>("activityId") ?: ""
                    val rideId = activityId.toLongOrNull() ?: 0L
                    // Run database operation on background thread to avoid blocking UI
                    Thread {
                        try {
                            val chartData = dbHelper.getActivityChartData(rideId)
                            handler.post {
                                if (chartData != null) {
                                    result.success(chartData)
                                } else {
                                    result.success(emptyList<Map<String, Any>>())
                                }
                            }
                        } catch (e: Exception) {
                            handler.post {
                                result.success(emptyList<Map<String, Any>>())
                            }
                        }
                    }.start()
                }
                "deleteActivity" -> {
                    val activityId = call.argument<String>("activityId") ?: ""
                    val rideId = activityId.toLongOrNull() ?: 0L
                    Thread {
                        try {
                            dbHelper.deleteRide(rideId)
                            runOnUiThread { result.success(null) }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error deleting activity: ${e.message}")
                            runOnUiThread { result.error("DELETE_ERROR", e.message, null) }
                        }
                    }.start()
                }
                "sendData" -> {
                    val dataList = call.argument<List<Int>>("data")
                    if (dataList != null) {
                        val data = ByteArray(dataList.size) { dataList[it].toByte() }
                        val intent = Intent("com.beforbike.app.SEND_DATA").apply {
                            putExtra("EXTRA_DATA", data)
                        }
                        sendBroadcast(intent)
                        result.success(null)
                    } else {
                        result.error("INVALID_ARGUMENT", "Data is null", null)
                    }
                }
                else -> {
                    result.notImplemented()
                }
            }
        }

        MethodChannel(flutterEngine?.dartExecutor?.binaryMessenger!!, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                // Asynchronous permission logic
                "requestPermissions" -> {
                    // 1. Save the 'result' to respond later
                    this.permissionRequestResult = result
                    // 2. Call the function that opens the dialog box
                    requestBlePermissions()
                    // DO NOT call result.success(true) here!
                }

                // Starts the service ONLY if it's not running (using the static flag)
                "isBleEnabled" -> {
                    result.success(BleServerService.isServiceRunning)
                }
                "isBluetoothAdapterEnabled" -> {
                    result.success(bluetoothAdapter.isEnabled)
                }
                "requestEnableBluetooth" -> {
                    if (!bluetoothAdapter.isEnabled) {
                        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        startActivityForResult(enableBtIntent, 1)
                        result.success(true)
                    } else {
                        result.success(false) // Already enabled
                    }
                }
                "scanAndConnectToDevice" -> {
                    scanAndConnectDevice()
                    result.success(true)
                }
                "disconnectDevice" -> {
                    disconnectDevice()
                    result.success(true)
                }
                "getConnectedStatus" -> {
                    val status = connectedDevice != null && bluetoothGatt != null
                    result.success(status)
                }
                "getConnectedDeviceName" -> {
                    val name = if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        connectedDevice?.name ?: "Unknown Device"
                    } else {
                        "Permission denied"
                    }
                    result.success(name)
                }
                "getConnectedDeviceMac" -> {
                    val mac = if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        connectedDevice?.address ?: ""
                    } else {
                        "Permission denied"
                    }
                    result.success(mac)
                }
                "getLocalBluetoothName" -> {
                    val name = if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        bluetoothAdapter.name ?: "Unknown"
                    } else {
                        "Permission denied"
                    }
                    result.success(name)
                }
                "getLocalBluetoothMac" -> {
                    @SuppressLint("MissingPermission")
                    val mac = if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(this, "android.permission.LOCAL_MAC_ADDRESS") == PackageManager.PERMISSION_GRANTED) {
                        bluetoothAdapter.address ?: ""
                    } else {
                        "Permission denied"
                    }
                    result.success(mac)
                }

                // Starts the service ONLY if it's not running (using the static flag)
                "setBleEnabled" -> {
                    val enabled = call.argument<Boolean>("enabled") ?: false
                    val isRunning = BleServerService.isServiceRunning // Uses the new flag

                    if (enabled && !isRunning) {
                        // Start BLE service if not already running
                        Log.d("BLE", "Starting BLE service (via setBleEnabled)")
                        startBleServerService()
                    } else if (!enabled && isRunning) {
                        // Stop BLE service if running
                        Log.d("BLE", "Stopping BLE service (via setBleEnabled)")
                        stopBleServerService()
                    }
                    result.success(true)
                }
                "getAllRideIds" -> {
                    // Run database operation on background thread to avoid blocking UI
                    Thread {
                        try {
                            val ids = dbHelper.getAllRideIds()
                            handler.post {
                                result.success(ids)
                            }
                        } catch (e: Exception) {
                            handler.post {
                                result.success(emptyList<Long>())
                            }
                        }
                    }.start()
                }
                "getRideData" -> {
                    val rideId = call.argument<Long>("rideId") ?: 0L
                    // Run database operation on background thread to avoid blocking UI
                    Thread {
                        try {
                            val data = dbHelper.getRideSummary(rideId)
                            handler.post {
                                result.success(data)
                            }
                        } catch (e: Exception) {
                            handler.post {
                                result.success(null)
                            }
                        }
                    }.start()
                }
                "calculateRideStatistics" -> {
                    val rideId = call.argument<Long>("rideId") ?: 0L
                    Thread {
                        try {
                            val stats = dbHelper.calculateRideStatistics(rideId)
                            runOnUiThread { result.success(stats) }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error calculating ride statistics: ${e.message}")
                            runOnUiThread { result.error("CALC_ERROR", e.message, null) }
                        }
                    }.start()
                }
                "getRideVelocities" -> {
                    val rideId = call.argument<Long>("rideId") ?: 0L
                    Thread {
                        try {
                            val telemetryData = dbHelper.getRideTelemetryData(rideId)
                            val velocities = telemetryData.mapNotNull { data ->
                                data["gps_speed"] as? Double ?: data["crank_speed"] as? Double
                            }
                            runOnUiThread { result.success(velocities.map { it.toString() }) }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error getting ride velocities: ${e.message}")
                            runOnUiThread { result.error("VEL_ERROR", e.message, null) }
                        }
                    }.start()
                }
                "getRideMapData" -> {
                    val rideId = call.argument<Long>("rideId") ?: 0L
                    Thread {
                        try {
                            val telemetryData = dbHelper.getRideTelemetryData(rideId)
                            val mapData = telemetryData.mapNotNull { data ->
                                val timestamp = data["gps_timestamp"] as? String
                                val lat = data["latitude"] as? Double
                                val lon = data["longitude"] as? Double
                                if (timestamp != null && lat != null && lon != null) {
                                    "$timestamp:$lat:$lon"
                                } else null
                            }
                            runOnUiThread { result.success(mapData) }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error getting ride map data: ${e.message}")
                            runOnUiThread { result.error("MAP_ERROR", e.message, null) }
                        }
                    }.start()
                }
                else -> {
                    result.notImplemented()
                }
            }
        }
    }

    private fun requestBlePermissions() {
        val permissions = mutableListOf<String>()

        // Always include location permissions
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)

        // Add BLE permissions for Android 12+ (API 31+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        } else {
            // For older Android versions
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        }

        // Add notification permission for Android 13+ (API 33+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (missingPermissions.isNotEmpty()) {
            Log.d("BLE", "Requesting permissions: ${missingPermissions.joinToString()}")
            // Use the constant
            ActivityCompat.requestPermissions(this, missingPermissions, REQUEST_CODE_PERMISSIONS)
        } else {
            Log.d("BLE", "All permissions were already granted.")
            // If we already have them, respond to Flutter immediately
            this.permissionRequestResult?.success(true)
            this.permissionRequestResult = null
        }
    }

    private fun scanAndConnectDevice() {
        Log.d("BLE", "Starting BLE scan and connect process...")

        if (!bluetoothAdapter.isEnabled) {
            Log.e("BLE", "Bluetooth not enabled")
            return
        }

        // Check all required permissions
        val requiredPermissions = mutableListOf<String>()
        requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            requiredPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
            requiredPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            requiredPermissions.add(Manifest.permission.BLUETOOTH)
            requiredPermissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        }

        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            Log.e("BLE", "Missing required permissions: ${missingPermissions.joinToString()}")
            // Request permissions again if missing
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), REQUEST_CODE_PERMISSIONS)
            return
        }

        Log.d("BLE", "All required permissions granted, starting scan...")

        val scanner = bluetoothAdapter.bluetoothLeScanner
        if (scanner == null) {
            Log.e("BLE", "Bluetooth LE scanner not available")
            return
        }
        val scanCallback: ScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                super.onScanResult(callbackType, result)
                val device = result.device
                val data = result.scanRecord
                if (data?.serviceUuids?.contains(ParcelUuid.fromString("12345678-1234-5678-1234-56789abcdef0")) == true) {
                    // Found bike computer
                    connectToDevice(device)
                    @SuppressLint("MissingPermission")
                    scanner.stopScan(this)
                }
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                super.onBatchScanResults(results)
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                Log.e("BLE", "Scan failed: $errorCode")
            }
        }

        scanning = true
        scanner.startScan(null, ScanSettings.Builder().build(), scanCallback)

        handler.postDelayed({
            if (scanning) {
                scanning = false
                scanner.stopScan(scanCallback)
            }
        }, SCAN_PERIOD)
    }

    private fun connectToDevice(device: BluetoothDevice) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e("BLE", "No connect permission")
            return
        }
        connectedDevice = device
        bluetoothGatt = device.connectGatt(this, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i("BLE", "Connected to ${device.address}")
                    @SuppressLint("MissingPermission")
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i("BLE", "Disconnected from ${device.address}")
                    connectedDevice = null
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.i("BLE", "Services discovered")
                    // Could read/write characteristics here if needed
                }
            }
        })
    }

    private fun disconnectDevice() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e("BLE", "No connect permission")
            return
        }
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        connectedDevice = null
    }

    private fun startBleServerService() {
        val intent = Intent(this, BleServerService::class.java)
        startService(intent)
    }

    private fun stopBleServerService() {
        val intent = Intent(this, BleServerService::class.java)
        stopService(intent)
    }

    private fun mapRideToActivity(rideId: Long, data: Map<String, Any?>?): Map<String, Any> {
        val currentTime = System.currentTimeMillis()

        // Parse start and end times from strings with multiple format support
        val startTimeStr = data?.get("start_time") as? String
        val endTimeStr = data?.get("end_time") as? String
        
        val dateFormats = listOf(
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()),
            SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
            SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
        )
        
        val startTime = parseDateWithFallbacks(startTimeStr, dateFormats, currentTime)
        val endTime = parseDateWithFallbacks(endTimeStr, dateFormats, currentTime + 1000)

        return mapOf(
            "id" to rideId.toString(),
            "startDatetime" to startTime,
            "endDatetime" to endTime,
            "distance" to (data?.get("total_distance_km") as? Double ?: 0.0),
            "speed" to (data?.get("avg_velocity_kmh") as? Double ?: 0.0),
            "cadence" to (data?.get("avg_cadence") as? Double ?: 0.0),
            "calories" to (data?.get("calories") as? Double ?: 0.0),
            "power" to (data?.get("avg_power") as? Double ?: 0.0),
            "altitude" to 900.0, // Placeholder
            "time" to ((endTime - startTime) / 1000.0), // Duration in seconds
        )
    }

    private fun parseDateWithFallbacks(dateStr: String?, formats: List<SimpleDateFormat>, fallback: Long): Long {
        if (dateStr.isNullOrEmpty()) return fallback
        
        for (format in formats) {
            try {
                return format.parse(dateStr)?.time ?: fallback
            } catch (e: Exception) {
                continue
            }
        }
        return fallback
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectDevice()
        dbHelper.close()
    }
}