package com.autoclicker.app.ui

import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.autoclicker.app.databinding.ActivitySettingsBinding
import com.autoclicker.app.model.AppSettings
import com.autoclicker.app.model.ClickMode
import com.autoclicker.app.utils.PreferenceManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var settings: AppSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Cài đặt"
        }

        settings = PreferenceManager.loadSettings(this)
        populateUI()
        setupSaveButton()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish(); return true
    }

    private fun populateUI() {
        // Fixed delay
        binding.etFixedDelay.setText(settings.fixedDelay.toString())

        // Random delays
        binding.etRandomMin.setText(settings.randomDelayMin.toString())
        binding.etRandomMax.setText(settings.randomDelayMax.toString())

        // Click position
        binding.etClickX.setText(settings.clickX.toInt().toString())
        binding.etClickY.setText(settings.clickY.toInt().toString())

        // Indicator toggle
        binding.switchIndicator.isChecked = settings.showTouchIndicator
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            if (!validate()) return@setOnClickListener

            val fixedDelay = binding.etFixedDelay.text.toString().toLongOrNull() ?: 1000L
            val rMin       = binding.etRandomMin.text.toString().toLongOrNull() ?: 100L
            val rMax       = binding.etRandomMax.text.toString().toLongOrNull() ?: 1000L
            val clickX     = binding.etClickX.text.toString().toFloatOrNull() ?: 500f
            val clickY     = binding.etClickY.text.toString().toFloatOrNull() ?: 1000f

            val updated = settings.copy(
                fixedDelay        = fixedDelay.coerceAtLeast(50L),
                randomDelayMin    = rMin.coerceAtLeast(50L),
                randomDelayMax    = rMax.coerceAtLeast(rMin + 50L),
                clickX            = clickX,
                clickY            = clickY,
                showTouchIndicator = binding.switchIndicator.isChecked
            )

            PreferenceManager.saveSettings(this, updated)
            settings = updated
            Toast.makeText(this, "✅ Đã lưu cài đặt!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun validate(): Boolean {
        val min = binding.etRandomMin.text.toString().toLongOrNull()
        val max = binding.etRandomMax.text.toString().toLongOrNull()
        val fixed = binding.etFixedDelay.text.toString().toLongOrNull()

        if (fixed == null || fixed < 50) {
            binding.etFixedDelay.error = "Tối thiểu 50ms"
            return false
        }
        if (min == null || min < 50) {
            binding.etRandomMin.error = "Tối thiểu 50ms"
            return false
        }
        if (max == null || max <= min) {
            binding.etRandomMax.error = "Phải lớn hơn min"
            return false
        }
        return true
    }
}
