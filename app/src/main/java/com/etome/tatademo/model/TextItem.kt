package com.etome.tatademo.model
data class TextItem(
    var text: String,
    var x: Float,
    var y: Float,
    val color: String,
    var fontSize: Float,

    var width: Float = 0f,
    var height: Float = 0f,
    var selected: Boolean = false

)