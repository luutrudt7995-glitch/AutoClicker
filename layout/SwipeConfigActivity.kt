package com.autoclicker.app.ui

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autoclicker.app.R
import com.autoclicker.app.model.SwipePoint
import com.autoclicker.app.utils.PreferenceManager

class SwipeConfigActivity : AppCompatActivity() {

    private lateinit var recyclerSwipes: RecyclerView
    private lateinit var btnAddSwipe: Button
    private lateinit var btnSaveSwipes: Button
    private lateinit var tvEmpty: TextView
    private val swipeList = mutableListOf<SwipePoint>()
    private lateinit var adapter: SwipeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_swipe_config)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Cau hinh Vuot"
        }

        recyclerSwipes = findViewById(R.id.recyclerSwipes)
        btnAddSwipe = findViewById(R.id.btnAddSwipe)
        btnSaveSwipes = findViewById(R.id.btnSaveSwipes)
        tvEmpty = findViewById(R.id.tvEmpty)

        val settings = PreferenceManager.loadSettings(this)
        swipeList.addAll(settings.swipePoints)

        adapter = SwipeAdapter(swipeList) { pos -> deleteSwipe(pos) }
        recyclerSwipes.layoutManager = LinearLayoutManager(this)
        recyclerSwipes.adapter = adapter

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val from = vh.adapterPosition
                val to = target.adapterPosition
                swipeList.add(to, swipeList.removeAt(from))
                adapter.notifyItemMoved(from, to)
                return true
            }
            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {}
        }).attachToRecyclerView(recyclerSwipes)

        btnAddSwipe.setOnClickListener { showAddSwipeDialog() }
        btnSaveSwipes.setOnClickListener { saveSwipes() }
        updateEmptyState()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    private fun showAddSwipeDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 16, 48, 16)
        }
        val etLabel = EditText(this).apply { hint = "Ten diem vuot" }
        val etStartX = EditText(this).apply { hint = "X bat dau"; inputType = android.text.InputType.TYPE_CLASS_NUMBER }
        val etStartY = EditText(this).apply { hint = "Y bat dau"; inputType = android.text.InputType.TYPE_CLASS_NUMBER }
        val etEndX = EditText(this).apply { hint = "X ket thuc"; inputType = android.text.InputType.TYPE_CLASS_NUMBER }
        val etEndY = EditText(this).apply { hint = "Y ket thuc"; inputType = android.text.InputType.TYPE_CLASS_NUMBER }
        val etDuration = EditText(this).apply { hint = "Thoi gian vuot (ms)"; inputType = android.text.InputType.TYPE_CLASS_NUMBER }
        val etDelay = EditText(this).apply { hint = "Do tre truoc khi vuot (ms)"; inputType = android.text.InputType.TYPE_CLASS_NUMBER }

        layout.addView(TextView(this).apply { text = "Nhan:" })
        layout.addView(etLabel)
        layout.addView(TextView(this).apply { text = "X bat dau:" })
        layout.addView(etStartX)
        layout.addView(TextView(this).apply { text = "Y bat dau:" })
        layout.addView(etStartY)
        layout.addView(TextView(this).apply { text = "X ket thuc:" })
        layout.addView(etEndX)
        layout.addView(TextView(this).apply { text = "Y ket thuc:" })
        layout.addView(etEndY)
        layout.addView(TextView(this).apply { text = "Thoi gian vuot (ms):" })
        layout.addView(etDuration)
        layout.addView(TextView(this).apply { text = "Do tre (ms):" })
        layout.addView(etDelay)

        AlertDialog.Builder(this)
            .setTitle("Them diem vuot")
            .setView(layout)
            .setPositiveButton("Luu") { _, _ ->
                val pt = SwipePoint(
                    startX = etStartX.text.toString().toFloatOrNull() ?: 200f,
                    startY = etStartY.text.toString().toFloatOrNull() ?: 800f,
                    endX = etEndX.text.toString().toFloatOrNull() ?: 800f,
                    endY = etEndY.text.toString().toFloatOrNull() ?: 800f,
                    duration = etDuration.text.toString().toLongOrNull() ?: 300L,
                    delayBefore = etDelay.text.toString().toLongOrNull() ?: 500L,
                    label = etLabel.text.toString().ifBlank { "Vuot ${swipeList.size + 1}" }
                )
                swipeList.add(pt)
                adapter.notifyItemInserted(swipeList.size - 1)
                updateEmptyState()
            }
            .setNegativeButton("Huy", null)
            .show()
    }

    private fun deleteSwipe(position: Int) {
        swipeList.removeAt(position)
        adapter.notifyItemRemoved(position)
        updateEmptyState()
    }

    private fun saveSwipes() {
        val settings = PreferenceManager.loadSettings(this)
        PreferenceManager.saveSettings(this, settings.copy(swipePoints = swipeList.toList()))
        Toast.makeText(this, "Da luu ${swipeList.size} diem vuot!", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun updateEmptyState() {
        tvEmpty.visibility = if (swipeList.isEmpty()) View.VISIBLE else View.GONE
    }

    class SwipeAdapter(
        private val items: List<SwipePoint>,
        private val onDelete: (Int) -> Unit
    ) : RecyclerView.Adapter<SwipeAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val tvLabel: TextView = view.findViewById(R.id.tvSwipeLabel)
            val tvCoords: TextView = view.findViewById(R.id.tvSwipeCoords)
            val tvDelay: TextView = view.findViewById(R.id.tvSwipeDelay)
            val btnDelete: Button = view.findViewById(R.id.btnDeleteSwipe)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_swipe, parent, false)
            return VH(v)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.tvLabel.text = "${position + 1}. ${item.label}"
            holder.tvCoords.text = "(${item.startX.toInt()},${item.startY.toInt()}) -> (${item.endX.toInt()},${item.endY.toInt()})"
            holder.tvDelay.text = "Tre: ${item.delayBefore}ms | Vuot: ${item.duration}ms"
            holder.btnDelete.setOnClickListener { onDelete(holder.adapterPosition) }
        }
    }
}
