package com.etome.tatademo.store

import com.etome.tatademo.viewmodel.WhiteboardViewModel

fun WhiteboardViewModel.toDto(): WhiteboardDto {
    return WhiteboardDto(
        strokes = state.strokes.map {
            StrokeDto(
                points = it.points.map { p -> listOf(p.first, p.second) },
                color = it.color,
                width = it.width
            )
        },
        shapes = state.shapes.map {
            ShapeDto(
                type = it.type.name.lowercase(),
                topLeft = listOf(it.startX, it.startY),
                bottomRight = listOf(it.endX, it.endY),
                color = it.color,
                sides = it.sides
            )
        },
        texts = state.texts.map {
            TextDto(
                text = it.text,
                position = listOf(it.x, it.y),
                color = it.color,
                size = it.fontSize
            )
        }
    )
}
