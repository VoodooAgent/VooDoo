package com.example.voodoo

import android.app.Application
import com.example.voodoo.data.AppDatabase

class VooDooApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
}