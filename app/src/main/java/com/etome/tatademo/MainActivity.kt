package com.etome.tatademo

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.etome.tatademo.model.ShapeType
import com.etome.tatademo.model.ToolType
import com.etome.tatademo.view.WhiteboardView
import com.etome.tatademo.viewmodel.WhiteboardViewModel

class MainActivity : AppCompatActivity() {

    private val viewModel: WhiteboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setContentView(R.layout.activity_main)

        val whiteboardView = findViewById<WhiteboardView>(R.id.whiteboardView)
        whiteboardView.viewModel = viewModel

        val penBtn = findViewById<ImageView>(R.id.btnPen)
        val eraserBtn = findViewById<ImageView>(R.id.btnEraser)

        penBtn.setOnClickListener { viewModel.selectPen() }
        eraserBtn.setOnClickListener { viewModel.selectEraser() }

        viewModel.tool.observe(this) { tool ->
            penBtn.alpha = if (tool == ToolType.PEN) 1f else 0.5f
            eraserBtn.alpha = if (tool == ToolType.ERASER) 1f else 0.5f
        }

        // ---- Colors ----
        val colorViews = mapOf(
            "#000000" to findViewById<View>(R.id.colorBlack),
            "#F44336" to findViewById<View>(R.id.colorRed),
            "#2196F3" to findViewById<View>(R.id.colorBlue),
            "#4CAF50" to findViewById<View>(R.id.colorGreen),
            "#FFEB3B" to findViewById<View>(R.id.colorYellow)
        )

        colorViews.forEach { (color, view) ->
            view.setOnClickListener { viewModel.setColor(color) }
        }

        viewModel.color.observe(this) { selected ->
            colorViews.forEach { (color, view) ->
                highlightColor(view, color == selected)
            }
        }

        // ---- Stroke Width ----
        findViewById<ImageView>(R.id.btnThin).setOnClickListener {
            viewModel.setStrokeWidth(4f)
        }
        findViewById<ImageView>(R.id.btnMedium).setOnClickListener {
            viewModel.setStrokeWidth(8f)
        }
        findViewById<ImageView>(R.id.btnThick).setOnClickListener {
            viewModel.setStrokeWidth(14f)
        }

        // ---- Shapes ----
        findViewById<ImageView>(R.id.btnRect).setOnClickListener {
            viewModel.selectShape(ShapeType.RECTANGLE)
        }

        findViewById<ImageView>(R.id.btnCircle).setOnClickListener {
            viewModel.selectShape(ShapeType.CIRCLE)
        }

        findViewById<ImageView>(R.id.btnLine).setOnClickListener {
            viewModel.selectShape(ShapeType.LINE)
        }

        findViewById<ImageView>(R.id.btnPolygon).setOnClickListener {
            viewModel.setPolygonSides(5) // default polygon
            viewModel.selectShape(ShapeType.POLYGON)
        }
        findViewById<ImageView>(R.id.btnText).setOnClickListener {
            Toast.makeText(this, "Double-tap on the whiteboard to add text", Toast.LENGTH_LONG).show()
            viewModel.selectTextTool()
        }

        findViewById<Button>(R.id.buttonSave).setOnClickListener {
            viewModel.saveWhiteboard(this)
            Toast.makeText(this, "Saved Successfully", Toast.LENGTH_SHORT).show()

        }


        loadLatestWhiteboardSafe(whiteboardView)

    }

    private fun highlightColor(view: View, active: Boolean) {
        view.alpha = if (active) 1f else 0.4f
        view.scaleX = if (active) 1.15f else 1f
        view.scaleY = if (active) 1.15f else 1f
    }


    private fun loadLatestWhiteboardSafe(
        whiteboardView: WhiteboardView
    ) {
        val files = filesDir.listFiles()
            ?.filter { it.name.startsWith("whiteboard_") && it.name.endsWith(".json") }

        if (files.isNullOrEmpty()) {
            Toast.makeText(this, "No saved whiteboard found", Toast.LENGTH_SHORT).show()
            return
        }

        val latestFile = files.maxByOrNull { it.lastModified() } ?: run {
            Toast.makeText(this, "Failed to load file", Toast.LENGTH_SHORT).show()
            return
        }

        val json = latestFile.readText()
        viewModel.loadFromJson(json)
        whiteboardView.invalidate()

        Toast.makeText(this, "Loaded: ${latestFile.name}", Toast.LENGTH_SHORT).show()
    }
}
