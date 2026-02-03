package com.etome.tatademo.viewmodel

import WhiteboardState
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.etome.tatademo.model.Shape
import com.etome.tatademo.model.ShapeType
import com.etome.tatademo.model.Stroke
import com.etome.tatademo.model.TextItem
import com.etome.tatademo.model.ToolType
import com.etome.tatademo.store.WhiteboardDto
import com.etome.tatademo.store.toDto
import com.google.gson.Gson
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WhiteboardViewModel : ViewModel() {

    private val _tool = MutableLiveData(ToolType.PEN)
    val tool: LiveData<ToolType> = _tool
    val selectedShape = MutableLiveData<ShapeType>()

    private val _polygonSides = MutableLiveData(5)
    val polygonSides: LiveData<Int> = _polygonSides

    fun setPolygonSides(count: Int) {
        _polygonSides.value = count.coerceAtLeast(4)
    }

    // Stroke color
    private val _color = MutableLiveData("#000000")
    val color: LiveData<String> = _color

    // Stroke width
    private val _strokeWidth = MutableLiveData(6f)
    val strokeWidth: LiveData<Float> = _strokeWidth


    val state = WhiteboardState()

    var strokeColor: String = "#000000"
    var eraserWidth: Float = 40f


    private val _textSize = MutableLiveData(40f)
    val textSize: LiveData<Float> = _textSize

    private val _textColor = MutableLiveData("#000000")
    val textColor: LiveData<String> = _textColor

    fun selectPen() {
        _tool.value = ToolType.PEN
        selectedShape.value = null
    }

    fun selectEraser() {
        _tool.value = ToolType.ERASER
        selectedShape.value = null
    }


    fun setColor(hex: String) {
        _color.value = hex
    }

    fun setStrokeWidth(width: Float) {
        _strokeWidth.value = width
    }

    fun addStroke(stroke: Stroke) {
        state.strokes.add(stroke)
    }

    fun selectShape(type: ShapeType) {
        selectedShape.value = type
//        selectPen()
        _tool.value = ToolType.PEN // use pen to draw shape

    }

    fun addShape(shape: Shape) {
        state.shapes.add(shape)
    }

    fun setTextSize(size: Float) {
        _textSize.value = size
    }

    fun setTextColor(color: String) {
        _textColor.value = color
    }

    fun selectTextTool() {
        _tool.value = ToolType.TEXT
    }


    fun saveWhiteboard(context: Context): File {

        val gson = Gson()
        val dto = toDto()
        val json = gson.toJson(dto)

//        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val timeStamp = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())

        val fileName = "whiteboard_$timeStamp.json"

        val file = File(context.filesDir, fileName)
        file.writeText(json)

        Log.i("",json)

        return file
    }

    fun loadFromJson(json: String) {

        val gson = Gson()
        val dto = gson.fromJson(json, WhiteboardDto::class.java)

        // Clear current canvas
        state.strokes.clear()
        state.shapes.clear()
        state.texts.clear()

        // Restore strokes
        dto.strokes.forEach {
            state.strokes.add(
                Stroke(
                    points = it.points.map { p -> Pair(p[0], p[1]) }.toMutableList(),
                    color = it.color,
                    width = it.width
                )
            )
        }

        // Restore shapes
        dto.shapes.forEach {
            state.shapes.add(
                Shape(
                    type = ShapeType.valueOf(it.type.uppercase()),
                    startX = it.topLeft[0],
                    startY = it.topLeft[1],
                    endX = it.bottomRight[0],
                    endY = it.bottomRight[1],
                    color = it.color,
                    sides = it.sides
                )
            )
        }

        // Restore texts
        dto.texts.forEach {
            state.texts.add(
                TextItem(
                    text = it.text,
                    x = it.position[0],
                    y = it.position[1],
                    color = it.color,
                    fontSize = it.size,
                    width = 0f,
                    height = 0f
                )
            )
        }
    }

}
