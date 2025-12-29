package com.example.pantrypal.util

import com.example.pantrypal.data.repository.ExportData
import com.google.gson.Gson
import java.io.File
import java.io.FileWriter

object ExportUtil {
    fun exportToJson(data: ExportData, file: File) {
        val gson = Gson()
        val json = gson.toJson(data)
        FileWriter(file).use {
            it.write(json)
        }
    }
}
