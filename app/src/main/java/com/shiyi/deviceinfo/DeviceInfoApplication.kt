package com.shiyi.deviceinfo

import android.app.Application
import com.shiyi.deviceinfo.utils.LocaleManager

class DeviceInfoApplication : Application() {
    private lateinit var localeManager: LocaleManager
    
    override fun onCreate() {
        super.onCreate()
        
        // 初始化语言管理器
        localeManager = LocaleManager.getInstance(this)
        
        // 应用保存的语言设置
        localeManager.setLocale(LocaleManager.currentLanguage.value)
    }
}
