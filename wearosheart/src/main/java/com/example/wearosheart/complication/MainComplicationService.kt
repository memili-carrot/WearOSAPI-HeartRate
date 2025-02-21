package com.example.wearosheart.complication

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import java.util.concurrent.atomic.AtomicReference

/**
 * Complication service that displays real-time heart rate data.
 */
class MainComplicationService : SuspendingComplicationDataSourceService(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var heartRateSensor: Sensor? = null
    private val heartRateValue = AtomicReference("Fetching...")

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        heartRateSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) {
            return null
        }
        return createComplicationData("72 BPM", "Heart Rate Data")
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val heartRateData = getHeartRateData()
        return createComplicationData(heartRateData, "Current Heart Rate")
    }

    private fun createComplicationData(text: String, contentDescription: String) =
        ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder(contentDescription).build()
        ).build()

    private fun getHeartRateData(): String {
        return heartRateValue.get()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val value = "%.0f BPM".format(it.values[0]) // 소수점 없이 표시
            heartRateValue.set(value)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}