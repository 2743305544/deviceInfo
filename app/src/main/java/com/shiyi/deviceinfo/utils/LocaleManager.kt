package com.shiyi.deviceinfo.utils

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import androidx.compose.runtime.mutableStateOf
import java.util.Locale

/**
 * 语言管理器：负责切换应用程序的语言
 */
class LocaleManager private constructor(private val context: Context) {
    private val preferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        const val LANGUAGE_ENGLISH = "en"
        const val LANGUAGE_CHINESE = "zh"
        private const val PREFS_NAME = "language_prefs"
        private const val KEY_LANGUAGE = "selected_language"
        
        private var instance: LocaleManager? = null
        
        // 当前语言状态，可被观察
        val currentLanguage = mutableStateOf(LANGUAGE_CHINESE)
        
        @Synchronized
        fun getInstance(context: Context): LocaleManager {
            if (instance == null) {
                instance = LocaleManager(context.applicationContext)
            }
            return instance!!
        }
    }
    
    init {
        // 初始化时加载保存的语言设置
        val savedLanguage = preferences.getString(KEY_LANGUAGE, LANGUAGE_CHINESE)
        currentLanguage.value = savedLanguage ?: LANGUAGE_CHINESE
        setLocale(currentLanguage.value)
    }
    
    /**
     * 设置应用程序的语言
     */
    fun setLocale(languageCode: String) {
        val locale = when (languageCode) {
            LANGUAGE_ENGLISH -> Locale(LANGUAGE_ENGLISH)
            LANGUAGE_CHINESE -> Locale(LANGUAGE_CHINESE)
            else -> Locale(LANGUAGE_CHINESE)
        }
        
        // 设置默认区域设置
        Locale.setDefault(locale)
        
        // 更新应用程序上下文的配置
        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale)
            val newContext = context.createConfigurationContext(config)
            // 确保资源配置被更新
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        }
        
        // 保存语言设置
        preferences.edit().putString(KEY_LANGUAGE, languageCode).apply()
        
        // 更新可观察的状态
        currentLanguage.value = languageCode
    }
    
    /**
     * 切换语言并重新创建 Activity 以应用更改
     */
    fun toggleLanguage(activity: Activity) {
        // 切换语言
        val newLanguage = if (currentLanguage.value == LANGUAGE_CHINESE) LANGUAGE_ENGLISH else LANGUAGE_CHINESE
        
        // 设置新的区域设置
        val locale = when (newLanguage) {
            LANGUAGE_ENGLISH -> Locale(LANGUAGE_ENGLISH)
            LANGUAGE_CHINESE -> Locale(LANGUAGE_CHINESE)
            else -> Locale(LANGUAGE_CHINESE)
        }
        
        // 设置默认区域设置
        Locale.setDefault(locale)
        
        // 更新应用程序的配置
        val config = Configuration(activity.resources.configuration)
        config.setLocale(locale)
        activity.resources.updateConfiguration(config, activity.resources.displayMetrics)
        
        // 保存语言设置
        preferences.edit().putString(KEY_LANGUAGE, newLanguage).apply()
        
        // 更新可观察的状态
        currentLanguage.value = newLanguage
        
        // 重新创建 Activity 以应用语言更改
        activity.recreate()
    }

    /**
     * 为 Activity 的 attachBaseContext 方法提供正确的语言配置
     */
    fun applyOverrideConfigurationForContext(baseContext: Context): Context {
        val savedLanguage = preferences.getString(KEY_LANGUAGE, currentLanguage.value)
        val finalLanguageCode = savedLanguage ?: LANGUAGE_CHINESE
        
        val locale = Locale(finalLanguageCode)
        Locale.setDefault(locale)
        
        // 创建新的配置
        val config = Configuration(baseContext.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale)
            // 创建新的上下文并返回
            return baseContext.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
            @Suppress("DEPRECATION")
            baseContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)
            return baseContext
        }
    }
}
