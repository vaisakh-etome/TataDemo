package com.etome.tatademo.model

import kotlin.math.abs

enum class ShapeType {
    RECTANGLE, CIRCLE, LINE, POLYGON
}

data class Shape(
    val type: ShapeType,
    var startX: Float,
    var startY: Float,
    var endX: Float,
    var endY: Float,
    val color: String,
    val sides: Int = 0   // ⚠ default kills your value

) {
    fun contains(x: Float, y: Float): Boolean {
        return when (type) {
            ShapeType.RECTANGLE -> x >= startX && x <= endX && y >= startY && y <= endY
            ShapeType.CIRCLE -> {
                val cx = (startX + endX) / 2
                val cy = (startY + endY) / 2
                val radius = abs(endX - startX) / 2
                (x - cx) * (x - cx) + (y - cy) * (y - cy) <= radius * radius
            }

            ShapeType.LINE -> {
                // simple line proximity check
                val dx = endX - startX
                val dy = endY - startY
                val length = Math.hypot(dx.toDouble(), dy.toDouble())
                val distance = abs(dy * x - dx * y + endX * startY - endY * startX) / length
                distance < 20 // 20px tolerance
            }

            ShapeType.POLYGON -> {
                // rough bounding box
                x >= minOf(startX, endX) && x <= maxOf(startX, endX) && y >= minOf(
                    startY,
                    endY
                ) && y <= maxOf(startY, endY)
            }
        }
    }
}