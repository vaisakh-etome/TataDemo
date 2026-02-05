package com.etome.tatademo.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.etome.tatademo.model.Shape
import com.etome.tatademo.model.ShapeType
import com.etome.tatademo.model.Stroke
import com.etome.tatademo.model.TextItem
import com.etome.tatademo.model.ToolType
import com.etome.tatademo.viewmodel.WhiteboardViewModel
import kotlin.math.*

class WhiteboardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
        setBackgroundColor(Color.TRANSPARENT) // important for video background
    }

    lateinit var viewModel: WhiteboardViewModel

    // Current drawing
    private var currentPath = Path()
    private var currentStroke: Stroke? = null
    private var currentShape: Shape? = null

    // Shape move / resize
    private var selectedShape: Shape? = null
    private var dragging = false
    private var resizing = false
    private var activeCorner: Corner? = null
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    // Text
    private var selectedText: TextItem? = null
    private var draggingText = false
    private var resizingText = false
    private val textHandleSize = 18f

    // Paints
    private val drawPaint = Paint().apply {
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
    private val cornerSize = 10f

    // ---------------------- TOUCH ----------------------
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        val tool = viewModel.tool.value ?: ToolType.PEN
        val shapeType = viewModel.selectedShape.value

        when (event.action) {

            MotionEvent.ACTION_DOWN -> {

                // --- TEXT SELECTION ---
                selectedText = viewModel.state.texts.reversed().find {
                    x in it.x..(it.x + it.width) &&
                            y in (it.y - it.height)..it.y
                }

                selectedText?.let { text ->

                    // ✨ ERASE TEXT
                    if (tool == ToolType.ERASER) {
                        viewModel.state.texts.remove(text)
                        invalidate()
                        return true
                    }

                    // ✨ NORMAL TEXT SELECT
                    text.selected = true
                    lastTouchX = x
                    lastTouchY = y

                    resizingText =
                        x in (text.x + text.width - textHandleSize)..(text.x + text.width) &&
                                y in (text.y - textHandleSize)..text.y

                    draggingText = !resizingText
                    invalidate()
                    return true
                }

                // --- NEW TEXT ---
                if (tool == ToolType.TEXT) {
                    showTextEditor(x, y)
                    return true
                }

                // --- SHAPE RESIZE ---
                val cornerHit =
                    viewModel.state.shapes.reversed().find { getTouchedCorner(it, x, y) != null }
                if (cornerHit != null) {
                    selectedShape = cornerHit
                    activeCorner = getTouchedCorner(cornerHit, x, y)
                    resizing = true
                    return true
                }

                // --- SHAPE DRAG ---
                selectedShape = viewModel.state.shapes.reversed().find { isPointInShape(it, x, y) }
                if (selectedShape != null) {
                    dragging = true
                    lastTouchX = x
                    lastTouchY = y
                    return true
                }

                // --- NEW SHAPE ---
                if (shapeType != null) {
                    currentShape = Shape(
                        type = shapeType,
                        startX = x,
                        startY = y,
                        endX = x + 200,
                        endY = y + 200,
                        color = viewModel.color.value ?: "#000000",
                        sides = if (shapeType == ShapeType.POLYGON) viewModel.polygonSides.value
                            ?: 5 else 0
                    )
                    return true
                }

                // --- PEN / ERASER ---
                currentPath = Path().apply { moveTo(x, y) }
                currentStroke = Stroke(
                    points = mutableListOf(Pair(x, y)),
                    color = viewModel.color.value ?: "#000000",
                    width = if (tool == ToolType.ERASER) viewModel.eraserWidth else viewModel.strokeWidth.value
                        ?: 6f,
                    isEraser = tool == ToolType.ERASER
                )
            }

            MotionEvent.ACTION_MOVE -> {
                // --- SHAPE RESIZE ---
                if (resizing && selectedShape != null && activeCorner != null) {
                    resizeShape(selectedShape!!, activeCorner!!, x, y)
                    invalidate()
                    return true
                }

                // --- SHAPE DRAG ---
                if (dragging && selectedShape != null) {
                    val dx = x - lastTouchX
                    val dy = y - lastTouchY
                    selectedShape!!.startX += dx
                    selectedShape!!.endX += dx
                    selectedShape!!.startY += dy
                    selectedShape!!.endY += dy
                    lastTouchX = x
                    lastTouchY = y
                    invalidate()
                    return true
                }

                // --- UPDATE SHAPE ---
                currentShape?.let {
                    it.endX = x
                    it.endY = y
                    invalidate()
                }

                // --- UPDATE STROKE ---
                currentStroke?.let {
                    currentPath.lineTo(x, y)
                    it.points.add(Pair(x, y))
                    invalidate()
                }

                // --- DRAG / RESIZE TEXT ---
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
                        it.height = paint.fontMetrics.bottom - paint.fontMetrics.top
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
                    selectedShape = null
                } else if (dragging) {
                    dragging = false
                    selectedShape = null
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

        // Draw all strokes
        viewModel.state.strokes.forEach { drawStroke(canvas, it) }
        currentStroke?.let { drawStroke(canvas, it, currentPath) }

        // Draw shapes
        viewModel.state.shapes.forEach { drawShape(canvas, it) }
        currentShape?.let { drawShape(canvas, it) }

        // Draw corners
        viewModel.state.shapes.forEach { drawCorners(canvas, it) }

        // Draw texts
        viewModel.state.texts.forEach { drawText(canvas, it) }
    }

    private fun drawStroke(canvas: Canvas, stroke: Stroke, path: Path? = null) {
        drawPaint.strokeWidth = stroke.width
        if (stroke.isEraser) {
            drawPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            drawPaint.color = Color.TRANSPARENT
        } else {
            drawPaint.xfermode = null
            drawPaint.color = Color.parseColor(stroke.color)
        }

        val drawPath = path ?: Path().apply {
            stroke.points.forEachIndexed { i, p ->
                if (i == 0) moveTo(p.first, p.second)
                else lineTo(p.first, p.second)
            }
        }

        canvas.drawPath(drawPath, drawPaint)
    }

    // ---------------------- SHAPES ----------------------
    private fun drawShape(canvas: Canvas, shape: Shape) {
        shapePaint.color = Color.parseColor(shape.color)
        when (shape.type) {
            ShapeType.RECTANGLE -> canvas.drawRect(
                shape.startX,
                shape.startY,
                shape.endX,
                shape.endY,
                shapePaint
            )

            ShapeType.CIRCLE -> {
                val cx = (shape.startX + shape.endX) / 2
                val cy = (shape.startY + shape.endY) / 2
                val radius = abs(shape.endX - shape.startX) / 2
                canvas.drawCircle(cx, cy, radius, shapePaint)
            }

            ShapeType.LINE -> canvas.drawLine(
                shape.startX,
                shape.startY,
                shape.endX,
                shape.endY,
                shapePaint
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
                val length = hypot(dx.toDouble(), dy.toDouble())
                abs(dy * x - dx * y + shape.endX * shape.startY - shape.endY * shape.startX) / length < 20
            }

            ShapeType.POLYGON -> x in min(shape.startX, shape.endX)..max(
                shape.startX,
                shape.endX
            ) &&
                    y in min(shape.startY, shape.endY)..max(shape.startY, shape.endY)
        }
    }

    // ---------------------- RESIZE ----------------------
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
                shape.startX = x; shape.startY = y
            }

            Corner.TOP_RIGHT -> {
                shape.endX = x; shape.startY = y
            }

            Corner.BOTTOM_LEFT -> {
                shape.startX = x; shape.endY = y
            }

            Corner.BOTTOM_RIGHT -> {
                shape.endX = x; shape.endY = y
            }
        }
    }

    // ---------------------- TEXT ----------------------
    private fun drawText(canvas: Canvas, text: TextItem) {
        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor(text.color)
            textSize = text.fontSize
            style = Paint.Style.FILL
        }
        canvas.drawText(text.text, text.x, text.y, paint)
        text.width = paint.measureText(text.text)
        text.height = paint.fontMetrics.bottom - paint.fontMetrics.top

        if (text.selected) {
            canvas.drawRect(
                text.x,
                text.y - text.height,
                text.x + text.width,
                text.y,
                textBorderPaint
            )
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
            imeOptions = EditorInfo.IME_ACTION_DONE
            setSingleLine(true)
        }

        parent.addView(editText)
        editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {

                val imm =
                    context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(editText.windowToken, 0)

                val textStr = editText.text.toString()
                if (textStr.isNotEmpty()) {
                    val paint = Paint().apply { textSize = viewModel.textSize.value!! }
                    val width = paint.measureText(textStr)
                    val height = paint.fontMetrics.bottom - paint.fontMetrics.top
                    viewModel.state.texts.add(
                        TextItem(
                            textStr,
                            x,
                            y,
                            viewModel.textColor.value!!,
                            viewModel.textSize.value!!,
                            width,
                            height
                        )
                    )
                }

                parent.removeView(editText)
                invalidate()
                true
            } else false
        }
    }


}
