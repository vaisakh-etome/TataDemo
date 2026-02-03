package com.etome.tatademo.service

import WhiteboardState
import android.content.Context
import com.google.gson.Gson
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WhiteboardFileService(private val context: Context) {

    private val gson = Gson()

    fun save(state: WhiteboardState): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "whiteboard_$timeStamp.json"
        val file = File(context.filesDir, fileName)

        file.writeText(gson.toJson(state))
        return file
    }

    fun load(file: File): WhiteboardState {
        return gson.fromJson(file.readText(), WhiteboardState::class.java)
    }
}
