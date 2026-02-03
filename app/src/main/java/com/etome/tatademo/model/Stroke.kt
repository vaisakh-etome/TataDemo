package com.etome.tatademo.model

data class Stroke(
    val points: MutableList<Pair<Float, Float>>,
    val color: String,
    val width: Float
)