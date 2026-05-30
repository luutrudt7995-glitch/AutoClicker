package com.autoclicker.app.ui

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.autoclicker.app.R
import com.autoclicker.app.model.ClickMode
import com.autoclicker.app.utils.PreferenceManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var etFixedDelay: EditText
    private lateinit var etRandomMin: EditText
    private lateinit var etRandomMax: EditText
    private lateinit var etClickX: EditText
    private lateinit var etClickY: EditText
    private lateinit var switchIndicator: com.google.android.material.switchmaterial.SwitchMaterial
    private lateinit var btnSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Cai dat"
        }

        etFixedDelay = findViewById(R.id.etFixedDelay)
        etRandomMin = findViewById(R.id.etRandomMin)
        etRandomMax = findViewById(R.id.etRandomMax)
        etClickX = findViewById(R.id.etClickX)
        etClickY = findViewById(R.id.etClickY)
        switchIndicator = findViewById(R.id.switchIndicator)
        btnSave = findViewById(R.id.btnSave)

        val settings = PreferenceManager.loadSettings(this)
        etFixedDelay.setText(settings.fixedDelay.toString())
        etRandomMin.setText(settings.randomDelayMin.toString())
        etRandomMax.setText(settings.randomDelayMax.toString())
        etClickX.setText(settings.clickX.toInt().toString())
        etClickY.setText(settings.clickY.toInt().toString())
        switchIndicator.isChecked = settings.showTouchIndicator

        btnSave.setOnClickListener {
            val fixedDelay = etFixedDelay.text.toString().toLongOrNull() ?: 1000L
            val rMin = etRandomMin.text.toString().toLongOrNull() ?: 100L
            val rMax = etRandomMax.text.toString().toLongOrNull() ?: 1000L
            val clickX = etClickX.text.toString().toFloatOrNull() ?: 500f
            val clickY = etClickY.text.toString().toFloatOrNull() ?: 1000f

            val updated = settings.copy(
                fixedDelay = fixedDelay.coerceAtLeast(50L),
                randomDelayMin = rMin.coerceAtLeast(50L),
                randomDelayMax = rMax.coerceAtLeast(rMin + 50L),
                clickX = clickX,
                clickY = clickY,
                showTouchIndicator = switchIndicator.isChecked
            )
            PreferenceManager.saveSettings(this, updated)
            Toast.makeText(this, "Da luu cai dat!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
