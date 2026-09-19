package com.md7parvez.voiceassistant

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class SensorManagerService(context: Context) : SensorEventListener {
    private val manager = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private var active = false
    private val values = mutableMapOf<Int, FloatArray>()

    val available: List<Sensor>
        get() = manager?.getSensorList(Sensor.TYPE_ALL).orEmpty().distinctBy { it.type }

    fun summary(): String = available.joinToString(", ") { it.name }.ifEmpty { "No sensors detected" }
    fun readings(): Map<Int, FloatArray> = values.mapValues { it.value.clone() }

    /** Sensor listeners are opt-in; Version 0.1 does not sample hardware continuously. */
    fun start() {
        val sensorManager = manager ?: return
        if (!active) {
            available.forEach { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
            active = true
        }
    }

    fun stop() {
        if (active) {
            manager?.unregisterListener(this)
            active = false
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        values[event.sensor.type] = event.values.clone()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
