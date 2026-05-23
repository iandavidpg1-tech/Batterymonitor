package com.iandavidpg1.batterymonitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.iandavidpg1.batterymonitor.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var batteryManager: BatteryManager
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updateRunnable: Runnable

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateBatteryInfo(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        updateRunnable = Runnable {
            updateAmperageAndVoltage()
            handler.postDelayed(updateRunnable, 1000)
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        handler.post(updateRunnable)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(batteryReceiver)
        handler.removeCallbacks(updateRunnable)
    }

    private fun updateBatteryInfo(intent: Intent) {
        // Nivel
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct = if (scale > 0) (level * 100 / scale) else 0
        binding.tvLevel.text = "Nivel: $batteryPct%"

        // Voltaje
        val voltageMillivolts = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
        val voltageVolts = voltageMillivolts / 1000.0
        binding.tvVoltage.text = "Voltaje: ${voltageMillivolts} mV (${String.format("%.2f", voltageVolts)} V)"

        // Temperatura
        val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10.0
        binding.tvTemp.text = "Temperatura: ${temp}°C"

        // Estado de carga
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val statusText = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "⚡ Cargando"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "🔋 Descargando"
            BatteryManager.BATTERY_STATUS_FULL -> "✅ Carga completa"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "⏸ No cargando"
            else -> "Desconocido"
        }
        binding.tvStatus.text = "Estado: $statusText"

        // Fuente de carga
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val plugText = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "Cargador AC"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Inalámbrico"
            else -> "Sin cargador"
        }
        binding.tvPlug.text = "Fuente: $plugText"
    }

    private fun updateAmperageAndVoltage() {
        // Amperaje actual (µA → mA)
        val currentMicroAmps = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val currentMilliAmps = currentMicroAmps / 1000.0
        val currentText = if (currentMicroAmps != Long.MIN_VALUE) {
            "${String.format("%.0f", Math.abs(currentMilliAmps))} mA ${if (currentMilliAmps < 0) "(descargando)" else "(cargando)"}"
        } else {
            "No disponible"
        }
        binding.tvCurrent.text = "Amperaje: $currentText"

        // Voltaje promedio
        val avgVoltage = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_VOLTAGE)
        if (avgVoltage > 0) {
            binding.tvVoltageAvg.text = "Voltaje promedio: ${avgVoltage} mV"
        }

        // Energía restante
        val energy = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)
        if (energy != Long.MIN_VALUE && energy > 0) {
            binding.tvEnergy.text = "Energía restante: ${energy / 1000000} mWh"
        } else {
            binding.tvEnergy.text = "Energía restante: No disponible"
        }
    }
}
