package com.etome.tatademo.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import com.etome.tatademo.model.Shape
import com.etome.tatademo.model.ShapeType
import com.etome.tatademo.model.Stroke
import com.etome.tatademo.model.TextItem
import com.etome.tatademo.model.ToolType
import com.etome.tatademo.viewmodel.WhiteboardViewModel
import com.google.android.material.internal.ViewUtils.hideKeyboard
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class WhiteboardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    lateinit var viewModel: WhiteboardViewModel

    // Drawing
    private var currentPath = Path()
    private var currentStroke: Stroke? = null

    // Shapes
    private var currentShape: Shape? = null
    private var selectedShapeForDrag: Shape? = null
    private var dragging = false
    private var resizing = false
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var activeCorner: Corner? = null

    private var draggingText = false

    val texts = mutableListOf<TextItem>()
    private var pendingTextX = 0f
    private var pendingTextY = 0f
    private var selectedText: TextItem? = null

    // Paint
    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val shapePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    private val cornerPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#B6B6B6")
        isAntiAlias = true
    }


    private val textBorderPaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }


    private val textHandlePaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.FILL
    }

    private val textHandleSize = 18f


    private val cornerSize = 10f // large corner for IFP touch

    private var resizingText = false


    // ---------------------- TOUCH ----------------------
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        val shapeType = viewModel.selectedShape.value



        when (event.action) {

            MotionEvent.ACTION_DOWN -> {

                val tool = viewModel.tool.value ?: ToolType.PEN

                if (tool == ToolType.ERASER) {
                    val textToErase =
                        viewModel.state.texts.reversed().find { isPointInText(it, x, y) }

                    if (textToErase != null) {
                        viewModel.state.texts.remove(textToErase)
                        invalidate()
                        return true
                    }
                }


                viewModel.state.texts.forEach { it.selected = false }
                selectedText = null

                selectedText = viewModel.state.texts.reversed().find {
                    x in it.x..(it.x + it.width) && y in (it.y - it.height)..it.y
                }

                selectedText?.let {
                    it.selected = true
                    lastTouchX = x
                    lastTouchY = y

                    resizingText =
                        x in (it.x + it.width - textHandleSize)..(it.x + it.width) && y in (it.y - textHandleSize)..it.y

                    draggingText = !resizingText
                    invalidate()
                    return true   // ⬅️ VERY IMPORTANT
                }







                if (tool == ToolType.TEXT) {
                    showTextEditor(x, y)
                    return true
                }


                // Check if user touched a corner for resizing
                val cornerHit = viewModel.state.shapes.reversed().find { shape ->
                    getTouchedCorner(shape, x, y) != null
                }
                if (cornerHit != null) {
                    selectedShapeForDrag = cornerHit
                    activeCorner = getTouchedCorner(cornerHit, x, y)
                    resizing = true
                    return true
                }

                // Check if user touched a shape to drag
                selectedShapeForDrag = viewModel.state.shapes.reversed().find { shape ->
                    isPointInShape(shape, x, y)
                }
                if (selectedShapeForDrag != null) {
                    dragging = true
                    lastTouchX = x
                    lastTouchY = y
                    return true
                }

                // Draw new shape
                if (shapeType != null) {
                    currentShape = Shape(
                        type = shapeType,
                        startX = x,
                        startY = y,
                        endX = x + 200, // default width
                        endY = y + 200, // default height
                        color = viewModel.color.value ?: "#000000",
                        sides = if (shapeType == ShapeType.POLYGON) viewModel.polygonSides.value
                            ?: 5 else 0
                    )
                } else if (tool == ToolType.PEN || tool == ToolType.ERASER) {
                    // Draw / erase
                    currentPath = Path().apply { moveTo(x, y) }
                    val isEraser = tool == ToolType.ERASER
                    currentStroke = Stroke(
                        points = mutableListOf(Pair(x, y)),
                        color = if (isEraser) "#FFFFFF" else viewModel.color.value ?: "#000000",
                        width = if (isEraser) viewModel.eraserWidth else viewModel.strokeWidth.value
                            ?: 6f
                    )


                }


            }

            MotionEvent.ACTION_MOVE -> {
                if (resizing && selectedShapeForDrag != null && activeCorner != null) {
                    resizeShape(selectedShapeForDrag!!, activeCorner!!, x, y)
                    invalidate()
                    return true
                }

                if (dragging && selectedShapeForDrag != null) {
                    val dx = x - lastTouchX
                    val dy = y - lastTouchY
                    selectedShapeForDrag!!.startX += dx
                    selectedShapeForDrag!!.endX += dx
                    selectedShapeForDrag!!.startY += dy
                    selectedShapeForDrag!!.endY += dy
                    lastTouchX = x
                    lastTouchY = y
                    invalidate()
                    return true
                }

                currentShape?.let {
                    it.endX = x
                    it.endY = y
                    invalidate()
                }

                currentStroke?.let {
                    currentPath.lineTo(x, y)
                    it.points.add(Pair(x, y))
                    invalidate()
                }


//                if (dragging && selectedText != null) {
//                    val dx = x - lastTouchX
//                    val dy = y - lastTouchY
//                    selectedText!!.x += dx
//                    selectedText!!.y += dy
//                    lastTouchX = x
//                    lastTouchY = y
//                    invalidate()
//                    return true
//                }


                selectedText?.let {
                    val dx = x - lastTouchX
                    val dy = y - lastTouchY

                    if (draggingText) {
                        it.x += dx
                        it.y += dy
                    }

                    if (resizingText) {
                        it.fontSize = (it.fontSize + dx * 0.3f).coerceIn(20f, 200f)

                        val paint = Paint().apply { textSize = it.fontSize }
                        it.width = paint.measureText(it.text)
                        val fm = paint.fontMetrics
                        it.height = fm.bottom - fm.top
                    }

                    lastTouchX = x
                    lastTouchY = y
                    invalidate()
                    return true
                }
            }

            MotionEvent.ACTION_UP -> {
                if (resizing) {
                    resizing = false
                    activeCorner = null
                    selectedShapeForDrag = null
                } else if (dragging) {
                    dragging = false
                    selectedShapeForDrag = null
                } else {
                    currentShape?.let {
                        viewModel.addShape(it)
                        currentShape = null
                    }
                    currentStroke?.let {
                        viewModel.addStroke(it)
                        currentStroke = null
                    }
                }

                draggingText = false
                resizingText = false
                selectedText = null
            }
        }

        return true
    }

    // ---------------------- DRAW ----------------------
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw strokes
        viewModel.state.strokes.forEach { stroke ->
            paint.color = Color.parseColor(stroke.color)
            paint.strokeWidth = stroke.width
            val path = Path()
            stroke.points.forEachIndexed { i, p ->
                if (i == 0) path.moveTo(p.first, p.second)
                else path.lineTo(p.first, p.second)
            }
            canvas.drawPath(path, paint)
        }

        // Draw shapes
        viewModel.state.shapes.forEach { drawShape(canvas, it) }

        // Active shape
        currentShape?.let { drawShape(canvas, it) }

        // Active stroke
        currentStroke?.let {
            paint.color = Color.parseColor(it.color)
            paint.strokeWidth = it.width
            canvas.drawPath(currentPath, paint)
        }

        // Draw resize corners
        viewModel.state.shapes.forEach { drawCorners(canvas, it) }

        viewModel.state.texts.forEach { drawText(canvas, it) }


    }

    private fun drawShape(canvas: Canvas, shape: Shape) {
        shapePaint.color = Color.parseColor(shape.color)

        when (shape.type) {
            ShapeType.RECTANGLE -> canvas.drawRect(
                shape.startX, shape.startY, shape.endX, shape.endY, shapePaint
            )

            ShapeType.CIRCLE -> {
                val cx = (shape.startX + shape.endX) / 2
                val cy = (shape.startY + shape.endY) / 2
                val radius = abs(shape.endX - shape.startX) / 2
                canvas.drawCircle(cx, cy, radius, shapePaint)
            }

            ShapeType.LINE -> canvas.drawLine(
                shape.startX, shape.startY, shape.endX, shape.endY, shapePaint
            )

            ShapeType.POLYGON -> {
                val sides = max(shape.sides, 4)
                val path = Path()
                val cx = (shape.startX + shape.endX) / 2
                val cy = (shape.startY + shape.endY) / 2
                val radius = abs(shape.endX - shape.startX) / 2

                for (i in 0 until sides) {
                    val angle = 2 * Math.PI * i / sides
                    val px = cx + radius * cos(angle)
                    val py = cy + radius * sin(angle)
                    if (i == 0) path.moveTo(px.toFloat(), py.toFloat())
                    else path.lineTo(px.toFloat(), py.toFloat())
                }
                path.close()
                canvas.drawPath(path, shapePaint)
            }
        }
    }

    // ---------------------- MOVE / RESIZE HELPERS ----------------------
    private fun isPointInShape(shape: Shape, x: Float, y: Float): Boolean {
        return when (shape.type) {
            ShapeType.RECTANGLE -> x in shape.startX..shape.endX && y in shape.startY..shape.endY
            ShapeType.CIRCLE -> {
                val cx = (shape.startX + shape.endX) / 2
                val cy = (shape.startY + shape.endY) / 2
                val radius = abs(shape.endX - shape.startX) / 2
                (x - cx).let { it * it } + (y - cy).let { it * it } <= radius * radius
            }

            ShapeType.LINE -> {
                val dx = shape.endX - shape.startX
                val dy = shape.endY - shape.startY
                val length = Math.hypot(dx.toDouble(), dy.toDouble())
                abs(dy * x - dx * y + shape.endX * shape.startY - shape.endY * shape.startX) / length < 20
            }

            ShapeType.POLYGON -> {
                x in min(shape.startX, shape.endX)..max(
                    shape.startX, shape.endX
                ) && y in min(shape.startY, shape.endY)..max(shape.startY, shape.endY)
            }
        }
    }

    // ---------------------- RESIZE CORNERS ----------------------
    private enum class Corner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    private fun drawCorners(canvas: Canvas, shape: Shape) {
        val corners = listOf(
            Pair(shape.startX, shape.startY),
            Pair(shape.endX, shape.startY),
            Pair(shape.startX, shape.endY),
            Pair(shape.endX, shape.endY)
        )
        corners.forEach { (cx, cy) ->
            canvas.drawRect(
                cx - cornerSize / 2,
                cy - cornerSize / 2,
                cx + cornerSize / 2,
                cy + cornerSize / 2,
                cornerPaint
            )
        }
    }

    private fun getTouchedCorner(shape: Shape, x: Float, y: Float): Corner? {
        val corners = mapOf(
            Corner.TOP_LEFT to Pair(shape.startX, shape.startY),
            Corner.TOP_RIGHT to Pair(shape.endX, shape.startY),
            Corner.BOTTOM_LEFT to Pair(shape.startX, shape.endY),
            Corner.BOTTOM_RIGHT to Pair(shape.endX, shape.endY)
        )
        return corners.entries.find { (_, pos) ->
            x in (pos.first - cornerSize)..(pos.first + cornerSize) && y in (pos.second - cornerSize)..(pos.second + cornerSize)
        }?.key
    }

    private fun resizeShape(shape: Shape, corner: Corner, x: Float, y: Float) {
        when (corner) {
            Corner.TOP_LEFT -> {
                shape.startX = x
                shape.startY = y
            }

            Corner.TOP_RIGHT -> {
                shape.endX = x
                shape.startY = y
            }

            Corner.BOTTOM_LEFT -> {
                shape.startX = x
                shape.endY = y
            }

            Corner.BOTTOM_RIGHT -> {
                shape.endX = x
                shape.endY = y
            }
        }
    }

    private fun drawText(canvas: Canvas, text: TextItem) {
        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor(text.color)
            textSize = text.fontSize
            style = Paint.Style.FILL
        }

        // Draw text
        canvas.drawText(text.text, text.x, text.y, paint)

        // Update width/height
        text.width = paint.measureText(text.text)
        val fm = paint.fontMetrics
        text.height = fm.bottom - fm.top

        // Draw border + handle if selected
        if (text.selected) {
            canvas.drawRect(
                text.x, text.y - text.height, text.x + text.width, text.y, textBorderPaint
            )

            // Resize handle (bottom-right)
            canvas.drawRect(
                text.x + text.width - textHandleSize,
                text.y - textHandleSize,
                text.x + text.width,
                text.y,
                textHandlePaint
            )
        }
    }


    private fun showTextEditor(x: Float, y: Float) {
        val parent = parent as ViewGroup

        val editText = EditText(context).apply {
            setTextColor(Color.parseColor(viewModel.textColor.value!!))
            textSize = viewModel.textSize.value!!
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(10, 10, 10, 10)
            this.x = x
            this.y = y
            requestFocus()
        }
        editText.imeOptions = EditorInfo.IME_ACTION_DONE
        editText.setSingleLine(true)

        parent.addView(editText)

        editText.setOnEditorActionListener { _, actionId, _ ->
//            val textStr = editText.text.toString()
//            if (textStr.isNotEmpty()) {
//                val paint = Paint().apply { textSize = viewModel.textSize.value!! }
//                val width = paint.measureText(textStr)
//                val fm = paint.fontMetrics
//                val height = fm.bottom - fm.top
//
//                viewModel.state.texts.add(
//                    TextItem(
//                        text = textStr,
//                        x = x,
//                        y = y,
//                        color = viewModel.textColor.value!!,
//                        fontSize = viewModel.textSize.value!!,
//                        width = width,
//                        height = height
//                    )
//                )
//            }
//
//            parent.removeView(editText)
//            invalidate()
//            true
//        }


            if (actionId == EditorInfo.IME_ACTION_DONE) {

                val textStr = editText.text.toString()
                if (textStr.isNotEmpty()) {
                    val paint = Paint().apply { textSize = viewModel.textSize.value!! }
                    val width = paint.measureText(textStr)
                    val fm = paint.fontMetrics
                    val height = fm.bottom - fm.top

                    viewModel.state.texts.add(
                        TextItem(
                            text = textStr,
                            x = x,
                            y = y,
                            color = viewModel.textColor.value!!,
                            fontSize = viewModel.textSize.value!!,
                            width = width,
                            height = height
                        )
                    )
                }

                // ✅ HIDE KEYBOARD
                hideKeyboard(editText)

                parent.removeView(editText)
                invalidate()
                true
            } else {
                false
            }
        }
    }

    private fun findTouchedText(x: Float, y: Float): TextItem? {
        return viewModel.state.texts.reversed().find {
            x in it.x..(it.x + paint.measureText(it.text)) && y in (it.y - it.fontSize)..it.y
        }
    }

    private fun isOnTextResizeHandle(text: TextItem, x: Float, y: Float): Boolean {
        return x in (text.x + text.width - textHandleSize)..(text.x + text.width) && y in (text.y - text.height)..(text.y - text.height + textHandleSize)
    }


    private fun editTextItem(textItem: TextItem) {
        val parent = parent as ViewGroup

        val editText = EditText(context).apply {
            setText(textItem.text)
            setTextColor(Color.parseColor(textItem.color))
            textSize = textItem.fontSize
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(10, 10, 10, 10)
            this.x = textItem.x
            this.y = textItem.y - textItem.fontSize
            requestFocus()
            imeOptions = EditorInfo.IME_ACTION_DONE
            setSingleLine(true)
        }

        parent.addView(editText)

        editText.setOnEditorActionListener { _, _, _ ->
            val newText = editText.text.toString()
            if (newText.isNotEmpty()) {
                textItem.text = newText
                // update width/height after text change
                val paint = Paint()
                paint.textSize = textItem.fontSize
                textItem.width = paint.measureText(newText)
                textItem.height = paint.fontMetrics.bottom - paint.fontMetrics.top
            }

            parent.removeView(editText)
            invalidate()
            true
        }
    }

    private fun isPointInText(text: TextItem, x: Float, y: Float): Boolean {
        return x in text.x..(text.x + text.width) && y in (text.y - text.height)..text.y
    }

    private fun hideKeyboard(view: View) {
        val imm =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

}
