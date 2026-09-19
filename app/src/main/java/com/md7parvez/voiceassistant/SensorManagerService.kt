package com.md7parvez.voiceassistant

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class SensorManagerService(context: Context) : SensorEventListener {
    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var active = false
    val available: List<Sensor> get() = manager.getSensorList(Sensor.TYPE_ALL)
    private val values = mutableMapOf<Int, FloatArray>()
    fun summary(): String = available.distinctBy { it.type }.joinToString(", ") { it.name }.ifEmpty { "No sensors detected" }
    fun readings(): Map<Int, FloatArray> = values.toMap()
    fun start() { if (!active) { available.forEach { manager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }; active = true } }
    fun stop() { if (active) { manager.unregisterListener(this); active = false } }
    override fun onSensorChanged(event: SensorEvent) { values[event.sensor.type] = event.values.clone() }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
