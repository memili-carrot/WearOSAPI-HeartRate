package com.example.wearosheart.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.wearosheart.presentation.theme.WearOSHeartTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.io.IOException

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var heartRateSensor: Sensor? = null
    private var heartRate by mutableStateOf("측정중...")
    private val sensorDataList = JSONArray() // JSON 배열 선언
    private val coroutineScope = CoroutineScope(Dispatchers.IO) // 파일 저장을 위한 코루틴 스코프

    // 최신 방식의 권한 요청 API
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                registerHeartRateSensor()
            } else {
                heartRate = "Permission Denied"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)

        // 권한 확인 후 요청 (Wear OS 13 이상)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.BODY_SENSORS)
        } else {
            registerHeartRateSensor()
        }

        setContent {
            WearOSHeartTheme {
                HeartRateWearOSApp(heartRate)
            }
        }
    }

    // 심박수 센서 등록 함수
    private fun registerHeartRateSensor() {
        heartRateSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        } ?: run {
            heartRate = "Sensor Not Available"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val heartRateValue = "%.0f".format(it.values[0]).toFloat()
            heartRate = "$heartRateValue BPM"

            // JSON 데이터 생성
            val sensorData = JSONObject().apply {
                put("timestamp", System.currentTimeMillis())
                put("sensor_name", "Heart Rate Sensor")
                put("bpm", heartRateValue) // 심박수 값 (BPM)
            }

            // JSON 배열에 추가
            sensorDataList.put(sensorData)

            // JSON 파일로 저장 (비동기 처리)
            coroutineScope.launch {
                saveJsonToFile(sensorDataList)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun saveJsonToFile(jsonArray: JSONArray) {
        val fileName = "heart_rate_data.json"
        val file = File(getExternalFilesDir(null), fileName) // 외부 저장소에 저장

        try {
            FileWriter(file).use { writer ->
                writer.write(jsonArray.toString(4))
            }
            Log.d("SensorData", "JSON 파일 저장 완료: ${file.absolutePath}")
        } catch (e: IOException) {
            Log.e("FileError", "JSON 저장 실패: ${e.message}")
        }
    }
}

@Composable
fun HeartRateWearOSApp(heartRate: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Heart Rate Monitor", style = MaterialTheme.typography.body1, modifier = Modifier.padding(16.dp))
        Text("BPM: $heartRate", modifier = Modifier.padding(8.dp))
    }
}
