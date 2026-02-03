package com.etome.tatademo.store

data class WhiteboardDto(
    val strokes: List<StrokeDto>,
    val shapes: List<ShapeDto>,
    val texts: List<TextDto>
)

data class StrokeDto(
    val points: List<List<Float>>,
    val color: String,
    val width: Float
)

data class ShapeDto(
    val type: String,
    val topLeft: List<Float>,
    val bottomRight: List<Float>,
    val color: String,
    val sides: Int = 0
)

data class TextDto(
    val text: String,
    val position: List<Float>,
    val color: String,
    val size: Float
)
