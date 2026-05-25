package com.autoclicker.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autoclicker.app.R
import com.autoclicker.app.databinding.ActivitySwipeConfigBinding
import com.autoclicker.app.databinding.DialogAddSwipeBinding
import com.autoclicker.app.model.AppSettings
import com.autoclicker.app.model.SwipePoint
import com.autoclicker.app.utils.PreferenceManager

class SwipeConfigActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySwipeConfigBinding
    private lateinit var settings: AppSettings
    private val swipeList = mutableListOf<SwipePoint>()
    private lateinit var adapter: SwipeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySwipeConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Cấu hình Vuốt"
        }

        settings = PreferenceManager.loadSettings(this)
        swipeList.addAll(settings.swipePoints)

        adapter = SwipeAdapter(swipeList) { pos -> deleteSwipe(pos) }
        binding.recyclerSwipes.layoutManager = LinearLayoutManager(this)
        binding.recyclerSwipes.adapter = adapter

        // Drag to reorder
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val from = vh.adapterPosition
                val to   = target.adapterPosition
                swipeList.add(to, swipeList.removeAt(from))
                adapter.notifyItemMoved(from, to)
                return true
            }
            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {}
        }).attachToRecyclerView(binding.recyclerSwipes)

        binding.btnAddSwipe.setOnClickListener { showAddSwipeDialog() }
        binding.btnSaveSwipes.setOnClickListener { saveSwipes() }

        updateEmptyState()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    private fun showAddSwipeDialog(existing: SwipePoint? = null) {
        val db = DialogAddSwipeBinding.inflate(layoutInflater)
        existing?.let {
            db.etStartX.setText(it.startX.toInt().toString())
            db.etStartY.setText(it.startY.toInt().toString())
            db.etEndX.setText(it.endX.toInt().toString())
            db.etEndY.setText(it.endY.toInt().toString())
            db.etDuration.setText(it.duration.toString())
            db.etDelay.setText(it.delayBefore.toString())
            db.etLabel.setText(it.label)
        }

        AlertDialog.Builder(this)
            .setTitle(if (existing == null) "Thêm điểm vuốt" else "Sửa điểm vuốt")
            .setView(db.root)
            .setPositiveButton("Lưu") { _, _ ->
                val pt = SwipePoint(
                    id          = existing?.id ?: System.currentTimeMillis(),
                    startX      = db.etStartX.text.toString().toFloatOrNull() ?: 200f,
                    startY      = db.etStartY.text.toString().toFloatOrNull() ?: 800f,
                    endX        = db.etEndX.text.toString().toFloatOrNull() ?: 800f,
                    endY        = db.etEndY.text.toString().toFloatOrNull() ?: 800f,
                    duration    = db.etDuration.text.toString().toLongOrNull() ?: 300L,
                    delayBefore = db.etDelay.text.toString().toLongOrNull() ?: 500L,
                    label       = db.etLabel.text.toString().ifBlank { "Vuốt ${swipeList.size + 1}" }
                )
                if (existing == null) {
                    swipeList.add(pt)
                    adapter.notifyItemInserted(swipeList.size - 1)
                } else {
                    val idx = swipeList.indexOfFirst { it.id == existing.id }
                    if (idx >= 0) { swipeList[idx] = pt; adapter.notifyItemChanged(idx) }
                }
                updateEmptyState()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun deleteSwipe(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Xóa điểm vuốt")
            .setMessage("Bạn có chắc muốn xóa \"${swipeList[position].label}\"?")
            .setPositiveButton("Xóa") { _, _ ->
                swipeList.removeAt(position)
                adapter.notifyItemRemoved(position)
                updateEmptyState()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun saveSwipes() {
        val updated = settings.copy(swipePoints = swipeList.toList())
        PreferenceManager.saveSettings(this, updated)
        settings = updated
        Toast.makeText(this, "✅ Đã lưu ${swipeList.size} điểm vuốt!", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun updateEmptyState() {
        binding.tvEmpty.visibility = if (swipeList.isEmpty()) View.VISIBLE else View.GONE
    }

    // ── Adapter ───────────────────────────────────────────────────────────────
    class SwipeAdapter(
        private val items: List<SwipePoint>,
        private val onDelete: (Int) -> Unit
    ) : RecyclerView.Adapter<SwipeAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val tvLabel:  TextView = view.findViewById(R.id.tvSwipeLabel)
            val tvCoords: TextView = view.findViewById(R.id.tvSwipeCoords)
            val tvDelay:  TextView = view.findViewById(R.id.tvSwipeDelay)
            val btnDelete: Button  = view.findViewById(R.id.btnDeleteSwipe)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_swipe, parent, false)
            return VH(v)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.tvLabel.text  = "${position + 1}. ${item.label}"
            holder.tvCoords.text = "(${item.startX.toInt()},${item.startY.toInt()}) → (${item.endX.toInt()},${item.endY.toInt()})"
            holder.tvDelay.text  = "Trễ: ${item.delayBefore}ms  |  Thời gian vuốt: ${item.duration}ms"
            holder.btnDelete.setOnClickListener { onDelete(holder.adapterPosition) }
        }
    }
}
