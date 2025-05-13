package com.shiyi.deviceinfo.utils

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.annotation.RequiresApi
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.InputStreamReader
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.Scanner

class DeviceInfoCollector(private val context: Context) {
    
    /**
     * 获取上下文对象
     */
    fun getContext(): Context {
        return context
    }

    /**
     * 收集所有设备信息
     */
    fun collectDeviceInfo(): JSONObject {
        return collectDeviceInfo(null) // 默认收集所有信息
    }
    
    /**
     * 可选择的信息类别
     */
    companion object {
        const val CATEGORY_BASIC = "basic"
        const val CATEGORY_SYSTEM = "system"
        const val CATEGORY_KERNEL = "kernel"
        const val CATEGORY_RADIO = "radio"
        const val CATEGORY_SCREEN = "screen"
        const val CATEGORY_MEMORY = "memory"
        const val CATEGORY_STORAGE = "storage"
        const val CATEGORY_CPU = "cpu"
        const val CATEGORY_NETWORK = "network"
        const val CATEGORY_BATTERY = "battery"
        
        // 所有可选类别
        val ALL_CATEGORIES = setOf(
            CATEGORY_BASIC,
            CATEGORY_SYSTEM,
            CATEGORY_KERNEL,
            CATEGORY_RADIO,
            CATEGORY_SCREEN,
            CATEGORY_MEMORY,
            CATEGORY_STORAGE,
            CATEGORY_CPU,
            CATEGORY_NETWORK,
            CATEGORY_BATTERY
        )
    }
    
    /**
     * 根据用户选择收集设备信息
     * @param selectedCategories 用户选择的信息类别，如果为 null 则收集所有信息
     */
    fun collectDeviceInfo(selectedCategories: Set<String>?): JSONObject {
        val deviceInfo = JSONObject()
        
        // 如果 selectedCategories 为 null，则收集所有信息
        val categories = selectedCategories ?: ALL_CATEGORIES

        // Basic device information
        if (categories.contains(CATEGORY_BASIC)) {
            val basicInfo = JSONObject()
            basicInfo.put("manufacturer", Build.MANUFACTURER)
            basicInfo.put("model", Build.MODEL)
            basicInfo.put("device", Build.DEVICE)
            basicInfo.put("product", Build.PRODUCT)
            basicInfo.put("brand", Build.BRAND)
            basicInfo.put("board", Build.BOARD)
            basicInfo.put("hardware", Build.HARDWARE)
            basicInfo.put("fingerprint", Build.FINGERPRINT)
            basicInfo.put("serial", try { 
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                        Build.getSerial()
                    } else {
                        "Permission denied"
                    }
                } else {
                    @Suppress("DEPRECATION")
                    Build.SERIAL
                }
            } catch (e: Exception) { "Unknown" })
            basicInfo.put("deviceId", try { Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) } catch (e: Exception) { "Unknown" })
            basicInfo.put("buildTags", Build.TAGS)
            basicInfo.put("buildType", Build.TYPE)
            basicInfo.put("buildUser", Build.USER)
            basicInfo.put("buildHost", Build.HOST)
            deviceInfo.put("basic", basicInfo)
        }
        
        // System information
        if (categories.contains(CATEGORY_SYSTEM)) {
            val systemInfo = JSONObject()
            systemInfo.put("androidVersion", Build.VERSION.RELEASE)
            systemInfo.put("apiLevel", Build.VERSION.SDK_INT)
            systemInfo.put("buildId", Build.ID)
            systemInfo.put("buildTime", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(Build.TIME)))
            systemInfo.put("securityPatch", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else "Not available")
            systemInfo.put("bootloader", Build.BOOTLOADER)
            systemInfo.put("androidId", Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID))
            systemInfo.put("codename", Build.VERSION.CODENAME)
            systemInfo.put("incremental", Build.VERSION.INCREMENTAL)
            systemInfo.put("baseOS", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.BASE_OS else "Not available")
            systemInfo.put("previewSdkInt", Build.VERSION.PREVIEW_SDK_INT)
            deviceInfo.put("system", systemInfo)
        }
        
        // Kernel information
        if (categories.contains(CATEGORY_KERNEL)) {
            val kernelInfo = JSONObject()
            try {
                val kernelVersion = System.getProperty("os.version") ?: "Unknown"
                kernelInfo.put("version", kernelVersion)
                
                // 获取更详细的内核信息
                try {
                    val process = Runtime.getRuntime().exec("cat /proc/version")
                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    val kernelDetails = reader.readLine() ?: "Unknown"
                    kernelInfo.put("details", kernelDetails)
                    reader.close()
                } catch (e: Exception) {
                    kernelInfo.put("details", "Could not retrieve: ${e.message}")
                }
                
                // 获取内核架构
                kernelInfo.put("architecture", System.getProperty("os.arch") ?: "Unknown")
                
                // 获取内核参数
                try {
                    val kernelParams = File("/proc/cmdline")
                    if (kernelParams.exists()) {
                        val scanner = Scanner(kernelParams)
                        val params = scanner.useDelimiter("\\A").next()
                        kernelInfo.put("parameters", params)
                        scanner.close()
                    }
                } catch (e: Exception) {
                    kernelInfo.put("parameters", "Could not retrieve: ${e.message}")
                }
            } catch (e: Exception) {
                kernelInfo.put("error", e.message ?: "Unknown error")
            }
            deviceInfo.put("kernel", kernelInfo)
        }
        
        // 基带信息
        if (categories.contains(CATEGORY_RADIO)) {
            val radioInfo = JSONObject()
            try {
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                
                // 基带版本
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        if (context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                            radioInfo.put("baseband", telephonyManager.getImei() ?: "Unknown")
                        } else {
                            radioInfo.put("baseband", "Permission denied")
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        if (context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                            radioInfo.put("baseband", telephonyManager.getDeviceId() ?: "Unknown")
                        } else {
                            radioInfo.put("baseband", "Permission denied")
                        }
                    }
                } catch (e: Exception) {
                    radioInfo.put("baseband", "Error: ${e.message}")
                }
                
                // 使用反射获取系统属性
                try {
                    val basebandVersion = getSystemProperty("gsm.version.baseband", "Unknown")
                    radioInfo.put("basebandVersion", basebandVersion)
                    
                    val radioVersion = getSystemProperty("gsm.version.ril-impl", "Unknown")
                    radioInfo.put("radioVersion", radioVersion)
                    
                    // 获取 SIM 卡信息
                    val simInfo = JSONObject()
                    simInfo.put("country", telephonyManager.simCountryIso)
                    simInfo.put("operator", telephonyManager.simOperator)
                    simInfo.put("operatorName", telephonyManager.simOperatorName)
                    simInfo.put("state", getSimStateString(telephonyManager.simState))
                    radioInfo.put("sim", simInfo)
                    
                    // 网络类型
                    radioInfo.put("networkType", getNetworkTypeString(telephonyManager.networkType))
                    radioInfo.put("phoneType", getPhoneTypeString(telephonyManager.phoneType))
                    
                } catch (e: Exception) {
                    radioInfo.put("error", "Could not retrieve all radio info: ${e.message}")
                }
            } catch (e: Exception) {
                radioInfo.put("error", e.message ?: "Unknown error")
            }
            deviceInfo.put("radio", radioInfo)
        }

        // Screen information
        if (categories.contains(CATEGORY_SCREEN)) {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val displayMetrics = DisplayMetrics()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val display = context.display
                display?.getRealMetrics(displayMetrics)
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.getRealMetrics(displayMetrics)
            }
            val screenInfo = JSONObject()
            screenInfo.put("widthPixels", displayMetrics.widthPixels)
            screenInfo.put("heightPixels", displayMetrics.heightPixels)
            screenInfo.put("density", displayMetrics.density)
            screenInfo.put("densityDpi", displayMetrics.densityDpi)
            screenInfo.put("scaledDensity", displayMetrics.scaledDensity)
            screenInfo.put("xdpi", displayMetrics.xdpi)
            screenInfo.put("ydpi", displayMetrics.ydpi)
            deviceInfo.put("screen", screenInfo)
        }

        // Memory information
        if (categories.contains(CATEGORY_MEMORY)) {
            val memoryInfo = ActivityManager.MemoryInfo()
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.getMemoryInfo(memoryInfo)
            val memoryJson = JSONObject()
            memoryJson.put("totalRam", formatSize(memoryInfo.totalMem))
            memoryJson.put("availableRam", formatSize(memoryInfo.availMem))
            memoryJson.put("lowMemory", memoryInfo.lowMemory)
            memoryJson.put("threshold", formatSize(memoryInfo.threshold))
            deviceInfo.put("memory", memoryJson)
        }

        // Storage information
        if (categories.contains(CATEGORY_STORAGE)) {
            val storageJson = JSONObject()
            try {
                val internalStat = StatFs(Environment.getDataDirectory().path)
                val externalStat = StatFs(Environment.getExternalStorageDirectory().path)

                val internalTotal = internalStat.blockCountLong * internalStat.blockSizeLong
                val internalFree = internalStat.availableBlocksLong * internalStat.blockSizeLong
                
                val externalTotal = externalStat.blockCountLong * externalStat.blockSizeLong
                val externalFree = externalStat.availableBlocksLong * externalStat.blockSizeLong

                storageJson.put("internalTotal", formatSize(internalTotal))
                storageJson.put("internalFree", formatSize(internalFree))
                storageJson.put("externalTotal", formatSize(externalTotal))
                storageJson.put("externalFree", formatSize(externalFree))
            } catch (e: Exception) {
                storageJson.put("error", e.message ?: "Unknown error")
            }
            deviceInfo.put("storage", storageJson)
        }

        // CPU information
        if (categories.contains(CATEGORY_CPU)) {
            val cpuInfo = JSONObject()
            cpuInfo.put("cores", Runtime.getRuntime().availableProcessors())
            cpuInfo.put("architecture", Build.SUPPORTED_ABIS.joinToString(", "))
            
            // 获取CPU频率和更多信息
            try {
                // 获取CPU最大频率
                val cpuInfoFile = File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq")
                if (cpuInfoFile.exists()) {
                    val scanner = Scanner(cpuInfoFile)
                    val maxFreq = scanner.nextLong() / 1000 // 转换为MHz
                    cpuInfo.put("maxFrequency", "$maxFreq MHz")
                    scanner.close()
                }
                
                // 获取CPU当前频率
                val cpuCurrentFreqFile = File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq")
                if (cpuCurrentFreqFile.exists()) {
                    val scanner = Scanner(cpuCurrentFreqFile)
                    val currentFreq = scanner.nextLong() / 1000 // 转换为MHz
                    cpuInfo.put("currentFrequency", "$currentFreq MHz")
                    scanner.close()
                }
                
                // 获取CPU温度
                try {
                    val thermalZones = File("/sys/class/thermal").listFiles { file -> file.name.startsWith("thermal_zone") }
                    if (thermalZones != null && thermalZones.isNotEmpty()) {
                        val temperatures = JSONObject()
                        for (zone in thermalZones) {
                            val typeFile = File(zone, "type")
                            val tempFile = File(zone, "temp")
                            if (typeFile.exists() && tempFile.exists()) {
                                val type = BufferedReader(FileReader(typeFile)).readLine()
                                val temp = BufferedReader(FileReader(tempFile)).readLine().toLong() / 1000.0 // 转换为摄氏度
                                temperatures.put(type ?: zone.name, "$temp°C")
                            }
                        }
                        cpuInfo.put("temperatures", temperatures)
                    }
                } catch (e: Exception) {
                    cpuInfo.put("temperatureError", e.message ?: "Unknown error")
                }
                
                // 获取CPU详细信息
                val cpuInfoDetailFile = File("/proc/cpuinfo")
                if (cpuInfoDetailFile.exists()) {
                    val reader = BufferedReader(FileReader(cpuInfoDetailFile))
                    var line: String?
                    val cpuDetails = JSONObject()
                    var processorCount = 0
                    var currentProcessor = JSONObject()
                    
                    // 用于提取CPU型号的变量
                    var cpuModel = ""
                    var hardware = ""
                    var implementer = ""
                    var architecture = ""
                    var variant = ""
                    var part = ""
                    var revision = ""
                    
                    while (reader.readLine().also { line = it } != null) {
                        if (line.isNullOrBlank()) {
                            if (currentProcessor.length() > 0) {
                                cpuDetails.put("processor$processorCount", currentProcessor)
                                currentProcessor = JSONObject()
                                processorCount++
                            }
                            continue
                        }
                        
                        val parts = line!!.split(":").map { it.trim() }
                        if (parts.size >= 2) {
                            val key = parts[0].replace("\\s+".toRegex(), "_")
                            val value = parts.subList(1, parts.size).joinToString(":")
                            currentProcessor.put(key, value)
                            
                            // 提取CPU型号相关信息
                            when (key.lowercase()) {
                                "model_name", "processor" -> if (cpuModel.isEmpty()) cpuModel = value
                                "hardware" -> hardware = value
                                "cpu_implementer" -> implementer = value
                                "cpu_architecture" -> architecture = value
                                "cpu_variant" -> variant = value
                                "cpu_part" -> part = value
                                "cpu_revision" -> revision = value
                            }
                        }
                    }
                    
                    // 添加最后一个处理器信息
                    if (currentProcessor.length() > 0) {
                        cpuDetails.put("processor$processorCount", currentProcessor)
                    }
                    
                    reader.close()
                    
                    // 根据提取的信息生成CPU型号
                    val modelInfo = JSONObject()
                    if (cpuModel.isNotEmpty()) modelInfo.put("model", cpuModel)
                    if (hardware.isNotEmpty()) modelInfo.put("hardware", hardware)
                    if (implementer.isNotEmpty()) modelInfo.put("implementer", implementer)
                    if (architecture.isNotEmpty()) modelInfo.put("architecture", architecture)
                    if (variant.isNotEmpty()) modelInfo.put("variant", variant)
                    if (part.isNotEmpty()) modelInfo.put("part", part)
                    if (revision.isNotEmpty()) modelInfo.put("revision", revision)
                    
                    // 尝试获取更友好的CPU型号名称
                    try {
                        val cpuHardware = getSystemProperty("ro.hardware", "")
                        val cpuBoardPlatform = getSystemProperty("ro.board.platform", "")
                        val cpuChipname = getSystemProperty("ro.chipname", "")
                        val socManufacturer = getSystemProperty("ro.soc.manufacturer", "")
                        val socModel = getSystemProperty("ro.soc.model", "")
                        
                        if (cpuHardware.isNotEmpty()) modelInfo.put("hardware_prop", cpuHardware)
                        if (cpuBoardPlatform.isNotEmpty()) modelInfo.put("platform", cpuBoardPlatform)
                        if (cpuChipname.isNotEmpty()) modelInfo.put("chipname", cpuChipname)
                        if (socManufacturer.isNotEmpty()) modelInfo.put("soc_manufacturer", socManufacturer)
                        if (socModel.isNotEmpty()) modelInfo.put("soc_model", socModel)
                        
                        // 尝试构建一个友好的CPU型号名称
                        val friendlyModel = when {
                            socManufacturer.isNotEmpty() && socModel.isNotEmpty() -> "$socManufacturer $socModel"
                            cpuChipname.isNotEmpty() -> cpuChipname
                            cpuBoardPlatform.isNotEmpty() -> cpuBoardPlatform
                            cpuHardware.isNotEmpty() -> cpuHardware
                            hardware.isNotEmpty() -> hardware
                            cpuModel.isNotEmpty() -> cpuModel
                            else -> null
                        }
                        
                        if (friendlyModel != null) {
                            cpuInfo.put("model", friendlyModel)
                        }
                    } catch (e: Exception) {
                        // 忽略异常
                    }
                    
                    cpuInfo.put("modelInfo", modelInfo)
                    cpuInfo.put("details", cpuDetails)
                    
                    // 尝试使用 getprop 命令获取更多信息
                    try {
                        val process = Runtime.getRuntime().exec("getprop ro.board.platform")
                        val reader = BufferedReader(InputStreamReader(process.inputStream))
                        val platform = reader.readLine()?.trim()
                        reader.close()
                        
                        if (!platform.isNullOrEmpty()) {
                            cpuInfo.put("platform_cmd", platform)
                        }
                    } catch (e: Exception) {
                        // 忽略异常
                    }
                }
            } catch (e: Exception) {
                cpuInfo.put("detailsError", e.message ?: "Unknown error")
            }
            
            deviceInfo.put("cpu", cpuInfo)
        }

        // Installed apps count
        if (categories.contains(CATEGORY_BASIC)) {
            try {
                val packageManager = context.packageManager
                val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                val basicInfo = deviceInfo.optJSONObject("basic") ?: JSONObject()
                basicInfo.put("installedAppsCount", installedApps.size)
                deviceInfo.put("basic", basicInfo)
            } catch (e: Exception) {
                val basicInfo = deviceInfo.optJSONObject("basic") ?: JSONObject()
                basicInfo.put("installedAppsCount", "Error: ${e.message}")
                deviceInfo.put("basic", basicInfo)
            }
        }

        // Battery information
        if (categories.contains(CATEGORY_BATTERY)) {
            val batteryInfo = JSONObject()
            try {
                val intentFilter = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
                val level = intentFilter?.getIntExtra("level", -1) ?: -1
                val scale = intentFilter?.getIntExtra("scale", -1) ?: -1
                val temperature = intentFilter?.getIntExtra("temperature", -1) ?: -1
                val voltage = intentFilter?.getIntExtra("voltage", -1) ?: -1
                val status = intentFilter?.getIntExtra("status", -1) ?: -1
                val health = intentFilter?.getIntExtra("health", -1) ?: -1
                val plugged = intentFilter?.getIntExtra("plugged", -1) ?: -1

                batteryInfo.put("level", if (scale > 0) (level * 100 / scale) else level)
                batteryInfo.put("temperature", temperature / 10.0)
                batteryInfo.put("voltage", voltage / 1000.0)
                batteryInfo.put("status", getBatteryStatus(status))
                batteryInfo.put("health", getBatteryHealth(health))
                batteryInfo.put("plugged", getChargingMethod(plugged))
            } catch (e: Exception) {
                batteryInfo.put("error", e.message ?: "Unknown error")
            }
            deviceInfo.put("battery", batteryInfo)
        }

        // Network information
        if (categories.contains(CATEGORY_NETWORK)) {
            val networkInfo = JSONObject()
            try {
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val network = connectivityManager.activeNetwork
                    val capabilities = connectivityManager.getNetworkCapabilities(network)
                    
                    if (capabilities != null) {
                        networkInfo.put("hasInternet", capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET))
                        networkInfo.put("hasWifi", capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI))
                        networkInfo.put("hasCellular", capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR))
                        networkInfo.put("hasEthernet", capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET))
                        networkInfo.put("hasVpn", capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN))
                        networkInfo.put("hasBluetoothInternet", capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_BLUETOOTH))
                    } else {
                        networkInfo.put("status", "No active network")
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val activeNetwork = connectivityManager.activeNetworkInfo
                    @Suppress("DEPRECATION")
                    networkInfo.put("connected", activeNetwork?.isConnected ?: false)
                    @Suppress("DEPRECATION")
                    networkInfo.put("type", activeNetwork?.typeName ?: "None")
                }
                
                // MAC 地址信息
                try {
                    val macAddresses = JSONObject()
                    val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
                    for (networkInterface in interfaces) {
                        if (!networkInterface.name.equals("lo", ignoreCase = true)) {
                            val macBytes = networkInterface.hardwareAddress
                            if (macBytes != null) {
                                val mac = StringBuilder()
                                for (b in macBytes) {
                                    mac.append(String.format("%02X:", b))
                                }
                                if (mac.isNotEmpty()) {
                                    mac.deleteCharAt(mac.length - 1) // 删除最后的冒号
                                    macAddresses.put(networkInterface.name, mac.toString())
                                }
                            }
                        }
                    }
                    networkInfo.put("macAddresses", macAddresses)
                } catch (e: Exception) {
                    networkInfo.put("macAddressesError", e.message ?: "Unknown error")
                }
                
                // IP 地址信息
                try {
                    val ipAddresses = JSONObject()
                    val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
                    for (networkInterface in interfaces) {
                        val addresses = Collections.list(networkInterface.inetAddresses)
                        val ipList = JSONArray()
                        for (address in addresses) {
                            if (!address.isLoopbackAddress) {
                                ipList.put(address.hostAddress)
                            }
                        }
                        if (ipList.length() > 0) {
                            ipAddresses.put(networkInterface.name, ipList)
                        }
                    }
                    networkInfo.put("ipAddresses", ipAddresses)
                } catch (e: Exception) {
                    networkInfo.put("ipAddressesError", e.message ?: "Unknown error")
                }
            } catch (e: Exception) {
                networkInfo.put("error", e.message ?: "Unknown error")
            }
            deviceInfo.put("network", networkInfo)
        }

        return deviceInfo
    }

    /**
     * 保存设备信息到文件，收集所有信息
     */
    fun saveDeviceInfoToFile(directory: String): String {
        return saveDeviceInfoToFile(directory, null)
    }
    
    /**
     * 根据用户选择保存设备信息到文件
     * @param directory 保存目录
     * @param selectedCategories 用户选择的信息类别，如果为 null 则收集所有信息
     */
    fun saveDeviceInfoToFile(directory: String, selectedCategories: Set<String>?): String {
        val deviceInfo = collectDeviceInfo(selectedCategories)
        
        // Create directory if it doesn't exist
        val dir = File(directory)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        
        // Generate filename with timestamp
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "device_info_$timestamp.json"
        val file = File(dir, filename)
        
        // Write to file
        FileWriter(file).use { writer ->
            writer.write(deviceInfo.toString(4))  // Pretty print with 4-space indentation
        }
        
        return file.absolutePath
    }
    
    private fun formatSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.2f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
    
    private fun getBatteryStatus(status: Int): String {
        return when (status) {
            1 -> "Unknown"
            2 -> "Charging"
            3 -> "Discharging"
            4 -> "Not charging"
            5 -> "Full"
            else -> "Unknown"
        }
    }
    
    private fun getBatteryHealth(health: Int): String {
        return when (health) {
            1 -> "Unknown"
            2 -> "Good"
            3 -> "Overheat"
            4 -> "Dead"
            5 -> "Over voltage"
            6 -> "Unspecified failure"
            7 -> "Cold"
            else -> "Unknown"
        }
    }
    
    private fun getChargingMethod(plugged: Int): String {
        return when (plugged) {
            0 -> "Battery"
            1 -> "AC"
            2 -> "USB"
            4 -> "Wireless"
            else -> "Unknown"
        }
    }
    
    // 辅助方法：将SIM卡状态转换为字符串
    private fun getSimStateString(simState: Int): String {
        return when (simState) {
            TelephonyManager.SIM_STATE_ABSENT -> "Absent"
            TelephonyManager.SIM_STATE_NETWORK_LOCKED -> "Network Locked"
            TelephonyManager.SIM_STATE_PIN_REQUIRED -> "PIN Required"
            TelephonyManager.SIM_STATE_PUK_REQUIRED -> "PUK Required"
            TelephonyManager.SIM_STATE_READY -> "Ready"
            TelephonyManager.SIM_STATE_UNKNOWN -> "Unknown"
            else -> "Unknown State: $simState"
        }
    }
    
    // 辅助方法：将网络类型转换为字符串
    private fun getNetworkTypeString(networkType: Int): String {
        return when (networkType) {
            TelephonyManager.NETWORK_TYPE_GPRS -> "GPRS"
            TelephonyManager.NETWORK_TYPE_EDGE -> "EDGE"
            TelephonyManager.NETWORK_TYPE_UMTS -> "UMTS"
            TelephonyManager.NETWORK_TYPE_HSDPA -> "HSDPA"
            TelephonyManager.NETWORK_TYPE_HSUPA -> "HSUPA"
            TelephonyManager.NETWORK_TYPE_HSPA -> "HSPA"
            TelephonyManager.NETWORK_TYPE_CDMA -> "CDMA"
            TelephonyManager.NETWORK_TYPE_EVDO_0 -> "EVDO_0"
            TelephonyManager.NETWORK_TYPE_EVDO_A -> "EVDO_A"
            TelephonyManager.NETWORK_TYPE_EVDO_B -> "EVDO_B"
            TelephonyManager.NETWORK_TYPE_1xRTT -> "1xRTT"
            TelephonyManager.NETWORK_TYPE_IDEN -> "IDEN"
            TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
            TelephonyManager.NETWORK_TYPE_EHRPD -> "EHRPD"
            TelephonyManager.NETWORK_TYPE_HSPAP -> "HSPAP"
            TelephonyManager.NETWORK_TYPE_GSM -> "GSM"
            TelephonyManager.NETWORK_TYPE_TD_SCDMA -> "TD_SCDMA"
            TelephonyManager.NETWORK_TYPE_IWLAN -> "IWLAN"
            TelephonyManager.NETWORK_TYPE_NR -> "5G NR"
            TelephonyManager.NETWORK_TYPE_UNKNOWN -> "Unknown"
            else -> "Unknown Type: $networkType"
        }
    }
    
    // 辅助方法：将电话类型转换为字符串
    private fun getPhoneTypeString(phoneType: Int): String {
        return when (phoneType) {
            TelephonyManager.PHONE_TYPE_NONE -> "None"
            TelephonyManager.PHONE_TYPE_GSM -> "GSM"
            TelephonyManager.PHONE_TYPE_CDMA -> "CDMA"
            TelephonyManager.PHONE_TYPE_SIP -> "SIP"
            else -> "Unknown Type: $phoneType"
        }
    }
    
    /**
     * 使用反射安全地获取系统属性
     * 这是一个替代 SystemProperties.get() 的方法，因为 SystemProperties 是隐藏 API
     */
    private fun getSystemProperty(key: String, defaultValue: String): String {
        try {
            val classLoader = context.classLoader
            val systemPropertiesClass = classLoader.loadClass("android.os.SystemProperties")
            val getMethod = systemPropertiesClass.getMethod("get", String::class.java, String::class.java)
            return getMethod.invoke(null, key, defaultValue) as String
        } catch (e: Exception) {
            return defaultValue
        }
    }
}
