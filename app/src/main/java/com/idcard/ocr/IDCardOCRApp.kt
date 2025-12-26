package com.idcard.ocr

import android.app.Application

/**
 * Main Application class for ID Card OCR app
 */
class IDCardOCRApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: IDCardOCRApp
            private set
    }
}
