package com.shiyi.deviceinfo

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.shiyi.deviceinfo.ui.screens.HomeScreen
import com.shiyi.deviceinfo.ui.theme.DeviceInfoTheme
import com.shiyi.deviceinfo.utils.LocaleManager
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var localeManager: LocaleManager
    
    override fun attachBaseContext(newBase: Context) {
        // 在 Activity 创建时应用正确的语言配置
        localeManager = LocaleManager.getInstance(newBase)
        super.attachBaseContext(localeManager.applyOverrideConfigurationForContext(newBase))
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 确保 localeManager 已初始化
        if (!::localeManager.isInitialized) {
            localeManager = LocaleManager.getInstance(applicationContext)
        }
        
        // 确保当前语言设置已应用
        val currentLocale = Locale(LocaleManager.currentLanguage.value)
        Locale.setDefault(currentLocale)
        val config = resources.configuration
        config.setLocale(currentLocale)
        resources.updateConfiguration(config, resources.displayMetrics)
        
        setContent {
            // 观察当前语言状态以触发重组
            val currentLanguage by LocaleManager.currentLanguage
            
            DeviceInfoTheme {
                // 传递 Activity 实例以便可以调用 recreate()
                HomeScreen(localeManager = localeManager, activity = this)
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 确保在应用恢复时应用正确的语言设置
        localeManager.setLocale(LocaleManager.currentLanguage.value)
    }
}