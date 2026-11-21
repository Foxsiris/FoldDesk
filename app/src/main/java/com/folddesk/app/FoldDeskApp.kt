package com.folddesk.app

import android.app.Application
import com.folddesk.app.data.AppDatabase
import com.folddesk.app.data.DeskRepository

class FoldDeskApp : Application() {
    val repository by lazy { DeskRepository(AppDatabase.build(this)) }
}
