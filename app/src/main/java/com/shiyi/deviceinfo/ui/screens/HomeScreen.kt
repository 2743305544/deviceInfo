package com.shiyi.deviceinfo.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.shiyi.deviceinfo.R
import com.shiyi.deviceinfo.utils.LocaleManager
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.shiyi.deviceinfo.ui.components.IOSButton
import com.shiyi.deviceinfo.ui.components.IOSCard
import com.shiyi.deviceinfo.ui.components.IOSColors
import com.shiyi.deviceinfo.ui.components.IOSGroupHeader
import com.shiyi.deviceinfo.ui.components.IOSListItem
import com.shiyi.deviceinfo.ui.components.IOSNavigationBar
import com.shiyi.deviceinfo.utils.DeviceInfoCollector
import com.shiyi.deviceinfo.utils.LocaleManager.Companion.currentLanguage
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(localeManager: LocaleManager, activity: Activity) {
    val context = LocalContext.current
    val deviceInfoCollector = remember { DeviceInfoCollector(context) }
    var deviceInfo by remember { mutableStateOf<JSONObject?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // 保存文件夹路径状态
    var selectedFolderUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFolderPath by remember { mutableStateOf<String?>(null) }
    var showFolderSelection by remember { mutableStateOf(false) }
    
    // 用户选择的信息类别
    var selectedCategories by remember { mutableStateOf(DeviceInfoCollector.ALL_CATEGORIES.toSet()) }
    var showCategorySelection by remember { mutableStateOf(false) }
    
    // Storage permission request
    val hasStoragePermission = remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        )
    }
    
    // 文件保存器 - 直接使用 ACTION_CREATE_DOCUMENT 意图
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            try {
                Log.d("DeviceInfo", "Selected document URI: $uri")
                // 直接保存到选定的 URI，传递用户选择的类别
                saveDeviceInfoToUri(deviceInfoCollector, uri, snackbarHostState, scope, selectedCategories = selectedCategories)
            } catch (e: Exception) {
                Log.e("DeviceInfo", "Error saving document: ${e.message}", e)
                scope.launch {
                    snackbarHostState.showSnackbar("Error saving file: ${e.message}")
                }
            }
        }
    }
    
    // 文件保存器
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            saveDeviceInfoToUri(deviceInfoCollector, uri, snackbarHostState, scope, selectedCategories = selectedCategories)
        }
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasStoragePermission.value = isGranted
        if (isGranted) {
            // 权限获取后显示选择器
            showFolderSelection = true
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Storage permission is required to save the file")
            }
        }
    }
    
    // Collect device info when the screen is first displayed
    LaunchedEffect(Unit) {
        deviceInfo = deviceInfoCollector.collectDeviceInfo()
    }
    
    // 当语言变化时重新收集设备信息
    LaunchedEffect(currentLanguage) {
        deviceInfo = deviceInfoCollector.collectDeviceInfo()
    }
    
    // 观察当前语言状态
    val currentLanguage by LocaleManager.currentLanguage
    
    // 分享设备信息的函数 - 非Composable函数
    fun shareDeviceInfo() {
        if (deviceInfo == null) return
        
        try {
            // 创建临时JSON文件
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "device_info_$timestamp.json"
            val cacheDir = context.cacheDir
            val tempFile = File(cacheDir, fileName)
            
            // 写入JSON数据
            tempFile.writeText(deviceInfo!!.toString(4))
            
            // 获取文件URI
            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            
            // 创建分享 Intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.app_name))
                putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_message))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            // 启动分享对话框
            val chooserIntent = Intent.createChooser(shareIntent, context.getString(R.string.share_title))
            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            Log.e("DeviceInfo", "Error sharing device info: ${e.message}", e)
            scope.launch {
                snackbarHostState.showSnackbar("Error sharing file: ${e.message}")
            }
        }
    }
    
    Scaffold(
        containerColor = IOSColors.background,
        topBar = { 
            IOSNavigationBar(
                title = stringResource(id = R.string.nav_title),
                onLanguageToggle = { localeManager.toggleLanguage(activity) },
                languageButtonText = if (LocaleManager.currentLanguage.value == LocaleManager.LANGUAGE_CHINESE) {
                    "EN" // Switch to English
                } else {
                    "中" // Switch to Chinese
                },
                onShareClick = { shareDeviceInfo() }
            ) 
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            state = rememberLazyListState(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
        ) {            
            item {
            deviceInfo?.let { info ->
                // Device section
                IOSGroupHeader(text = stringResource(id = R.string.group_device))
                IOSCard {
                    // 获取当前语言环境下的类别名称
                    val categoryBasicName = stringResource(id = R.string.category_basic)
                    // 尝试使用本地化的类别名称获取数据，如果失败则尝试使用英文类别名称
                    val basicInfo = info.optJSONObject(categoryBasicName) 
                        ?: info.optJSONObject("Basic Device Info")
                        ?: JSONObject()
                    
                    // 调试日志
                    Log.d("DeviceInfo", "当前语言: ${LocaleManager.currentLanguage.value}")
                    Log.d("DeviceInfo", "类别名称: $categoryBasicName")
                    Log.d("DeviceInfo", "基本信息对象: ${basicInfo.toString()}")
                    
                    IOSListItem(
                        title = stringResource(id = R.string.info_manufacturer),
                        value = basicInfo.optString("manufacturer"),
                        icon = Icons.Default.Smartphone
                    )
                    IOSListItem(
                        title = stringResource(id = R.string.info_model),
                        value = basicInfo.optString("model"),
                        icon = null
                    )
                    IOSListItem(
                        title = stringResource(id = R.string.info_device),
                        value = basicInfo.optString("device"),
                        icon = null,
                        showDivider = false
                    )
                }
                
                // System section
                IOSGroupHeader(text = stringResource(id = R.string.group_system))
                IOSCard {
                    val systemInfo = info.optJSONObject(DeviceInfoCollector.CATEGORY_SYSTEM) ?: JSONObject()
                    IOSListItem(
                        title = stringResource(id = R.string.info_android_version),
                        value = systemInfo.optString("androidVersion"),
                        icon = Icons.Default.Info
                    )
                    IOSListItem(
                        title = stringResource(id = R.string.info_api_level),
                        value = systemInfo.optString("apiLevel"),
                        icon = null
                    )
                    IOSListItem(
                        title = stringResource(id = R.string.info_build_id),
                        value = systemInfo.optString("buildId"),
                        icon = null
                    )
                    IOSListItem(
                        title = stringResource(id = R.string.info_security_patch),
                        value = systemInfo.optString("securityPatch"),
                        icon = null
                    )
                    IOSListItem(
                        title = stringResource(id = R.string.info_bootloader),
                        value = systemInfo.optString("bootloader"),
                        icon = null,
                        showDivider = false
                    )
                }
                
                // Hardware section
                IOSGroupHeader(text = stringResource(id = R.string.group_cpu))
                IOSCard {
                    // CPU info
                    val cpuInfo = info.optJSONObject(DeviceInfoCollector.CATEGORY_CPU)
                    
                    // 添加 CPU 型号显示
                    IOSListItem(
                        title = stringResource(id = R.string.info_processor),
                        value = cpuInfo?.optString("model") ?: "Unknown",
                        icon = Icons.Default.Memory
                    )
                    IOSListItem(
                        title = stringResource(id = R.string.info_cores),
                        value = cpuInfo?.optString("cores") ?: "Unknown",
                        icon = null
                    )
                    IOSListItem(
                        title = stringResource(id = R.string.info_cpu_architecture),
                        value = cpuInfo?.optString("architecture") ?: "Unknown",
                        icon = null
                    )
                    IOSListItem(
                        title = stringResource(id = R.string.info_cpu_frequency_range),
                        value = cpuInfo?.optString("maxFrequency") ?: "Unknown",
                        icon = null
                    )
                    
                    // Memory info
                    val memoryInfo = info.optJSONObject(DeviceInfoCollector.CATEGORY_MEMORY)
                    IOSListItem(
                        title = stringResource(id = R.string.info_total_ram),
                        value = memoryInfo?.optString("totalRam") ?: "Unknown",
                        icon = null
                    )
                    IOSListItem(
                        title = stringResource(id = R.string.info_available_ram),
                        value = memoryInfo?.optString("availableRam") ?: "Unknown",
                        icon = null
                    )
                    
                    // Storage info
                    val storageInfo = info.optJSONObject(DeviceInfoCollector.CATEGORY_STORAGE)
                    IOSListItem(
                        title = stringResource(id = R.string.info_internal_storage),
                        value = storageInfo?.optString("internalTotal") ?: "Unknown",
                        icon = Icons.Default.Storage
                    )
                    IOSListItem(
                        title = stringResource(id = R.string.info_available_internal_storage),
                        value = storageInfo?.optString("internalFree") ?: "Unknown",
                        icon = null,
                        showDivider = false
                    )
                }
                
                // Kernel section
                IOSGroupHeader(text = stringResource(id = R.string.group_kernel))
                IOSCard {
                    val kernelInfo = info.optJSONObject(DeviceInfoCollector.CATEGORY_KERNEL) ?: JSONObject()
                    IOSListItem(
                        title = stringResource(id = R.string.info_kernel_version),
                        value = kernelInfo.optString("version", "Unknown"),
                        icon = null
                    )
                    IOSListItem(
                        title = stringResource(id = R.string.info_kernel_architecture),
                        value = kernelInfo.optString("architecture", "Unknown"),
                        icon = null,
                        showDivider = false
                    )
                }
                
                // Radio/Baseband section
                IOSGroupHeader(text = stringResource(id = R.string.group_radio))
                IOSCard {
                    val radioInfo = info.optJSONObject(DeviceInfoCollector.CATEGORY_RADIO) ?: JSONObject()
                    IOSListItem(
                        title = stringResource(id = R.string.info_baseband_version),
                        value = radioInfo.optString("basebandVersion", "Unknown"),
                        icon = null
                    )
                    IOSListItem(
                        title = stringResource(id = R.string.info_radio_version),
                        value = radioInfo.optString("radioVersion", "Unknown"),
                        icon = null
                    )
                    
                    // SIM info
                    val simInfo = radioInfo.optJSONObject("sim")
                    if (simInfo != null) {
                        IOSListItem(
                            title = stringResource(id = R.string.info_sim_operator),
                            value = simInfo.optString("operatorName", "Unknown"),
                            icon = null
                        )
                        IOSListItem(
                            title = stringResource(id = R.string.info_sim_state),
                            value = simInfo.optString("state", "Unknown"),
                            icon = null,
                            showDivider = false
                        )
                    }
                }
                
                // Screen section
                IOSGroupHeader(text = stringResource(id = R.string.group_display))
                IOSCard {
                    val screenInfo = info.optJSONObject(DeviceInfoCollector.CATEGORY_SCREEN)
                    IOSListItem(
                        title = stringResource(id = R.string.info_resolution),
                        value = "${screenInfo?.optString("widthPixels") ?: "?"} × ${screenInfo?.optString("heightPixels") ?: "?"}",
                        icon = null
                    )
                    IOSListItem(
                        title = stringResource(id = R.string.info_density),
                        value = screenInfo?.optString("densityDpi") ?: "Unknown",
                        icon = null,
                        showDivider = false
                    )
                }
                
                // Network section
                IOSGroupHeader(text = stringResource(id = R.string.group_network))
                IOSCard {
                    val networkInfo = info.optJSONObject(DeviceInfoCollector.CATEGORY_NETWORK)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        IOSListItem(
                            title = stringResource(id = R.string.info_wifi),
                            value = if (networkInfo?.optBoolean("hasWifi", false) == true) stringResource(id = R.string.status_connected) else stringResource(id = R.string.status_disconnected),
                            icon = null
                        )
                        IOSListItem(
                            title = stringResource(id = R.string.info_cellular),
                            value = if (networkInfo?.optBoolean("hasCellular", false) == true) stringResource(id = R.string.status_connected) else stringResource(id = R.string.status_disconnected),
                            icon = null,
                            showDivider = false
                        )
                    } else {
                        IOSListItem(
                            title = stringResource(id = R.string.info_connected),
                            value = if (networkInfo?.optBoolean("connected", false) == true) stringResource(id = R.string.status_yes) else stringResource(id = R.string.status_no),
                            icon = null
                        )
                        IOSListItem(
                            title = stringResource(id = R.string.info_network_type),
                            value = networkInfo?.optString("type") ?: stringResource(id = R.string.status_unknown),
                            icon = null,
                            showDivider = false
                        )
                    }
                }
                
                // Battery section
                IOSGroupHeader(text = stringResource(id = R.string.group_battery))
                IOSCard {
                    val batteryInfo = info.optJSONObject(DeviceInfoCollector.CATEGORY_BATTERY)
                    IOSListItem(
                        title = stringResource(id = R.string.info_battery_level),
                        value = "${batteryInfo?.optString("level")}%",
                        icon = null
                    )
                    IOSListItem(
                        title = stringResource(id = R.string.info_battery_status),
                        value = batteryInfo?.optString("status") ?: stringResource(id = R.string.status_unknown),
                        icon = null
                    )
                    IOSListItem(
                        title = stringResource(id = R.string.info_battery_temperature),
                        value = "${batteryInfo?.optString("temperature")}°C",
                        icon = null,
                        showDivider = false
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Export buttons
                IOSButton(
                    text = stringResource(id = R.string.export_to_downloads),
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            // For Android 11+, use the Storage Access Framework
                            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
                            saveDeviceInfoToFile(deviceInfoCollector, snackbarHostState, scope, downloadDir, selectedCategories)
                        } else {
                            // For older Android versions, request permission if not granted
                            if (hasStoragePermission.value) {
                                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
                                saveDeviceInfoToFile(deviceInfoCollector, snackbarHostState, scope, downloadDir, selectedCategories)
                            } else {
                                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            }
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                
                IOSButton(
                    text = stringResource(id = R.string.choose_save_location),
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            // For Android 11+, directly launch document creator
                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                            val suggestedName = "device_info_$timestamp.json"
                            createDocumentLauncher.launch(suggestedName)
                        } else {
                            // For older Android versions, request permission if not granted
                            if (hasStoragePermission.value) {
                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                val suggestedName = "device_info_$timestamp.json"
                                createDocumentLauncher.launch(suggestedName)
                            } else {
                                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            }
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                
                // 类别选择按钮
                IOSButton(
                    text = stringResource(
                        id = if (showCategorySelection) R.string.hide_category_selection else R.string.show_category_selection
                    ),
                    onClick = { showCategorySelection = !showCategorySelection },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                
                // 类别选择界面
                if (showCategorySelection) {
                    IOSCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(id = R.string.select_export_categories),
                                fontSize = 16.sp,
                                color = IOSColors.primary
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // 全选/取消全选按钮
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                IOSButton(
                                    text = stringResource(id = R.string.select_all),
                                    onClick = { selectedCategories = DeviceInfoCollector.ALL_CATEGORIES.toSet() },
                                    modifier = Modifier.weight(1f)
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                IOSButton(
                                    text = stringResource(id = R.string.deselect_all),
                                    onClick = { selectedCategories = emptySet() },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = IOSColors.lightGray)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // 基本信息
                            CategoryCheckbox(
                                title = stringResource(id = R.string.category_basic),
                                category = DeviceInfoCollector.CATEGORY_BASIC,
                                isSelected = selectedCategories.contains(DeviceInfoCollector.CATEGORY_BASIC),
                                onCheckedChange = { checked ->
                                    selectedCategories = if (checked) {
                                        selectedCategories + DeviceInfoCollector.CATEGORY_BASIC
                                    } else {
                                        selectedCategories - DeviceInfoCollector.CATEGORY_BASIC
                                    }
                                }
                            )
                            
                            // 系统信息
                            CategoryCheckbox(
                                title = stringResource(id = R.string.category_system),
                                category = DeviceInfoCollector.CATEGORY_SYSTEM,
                                isSelected = selectedCategories.contains(DeviceInfoCollector.CATEGORY_SYSTEM),
                                onCheckedChange = { checked ->
                                    selectedCategories = if (checked) {
                                        selectedCategories + DeviceInfoCollector.CATEGORY_SYSTEM
                                    } else {
                                        selectedCategories - DeviceInfoCollector.CATEGORY_SYSTEM
                                    }
                                }
                            )
                            
                            // 内核信息
                            CategoryCheckbox(
                                title = stringResource(id = R.string.category_kernel),
                                category = DeviceInfoCollector.CATEGORY_KERNEL,
                                isSelected = selectedCategories.contains(DeviceInfoCollector.CATEGORY_KERNEL),
                                onCheckedChange = { checked ->
                                    selectedCategories = if (checked) {
                                        selectedCategories + DeviceInfoCollector.CATEGORY_KERNEL
                                    } else {
                                        selectedCategories - DeviceInfoCollector.CATEGORY_KERNEL
                                    }
                                }
                            )
                            
                            // 基带信息
                            CategoryCheckbox(
                                title = stringResource(id = R.string.category_radio),
                                category = DeviceInfoCollector.CATEGORY_RADIO,
                                isSelected = selectedCategories.contains(DeviceInfoCollector.CATEGORY_RADIO),
                                onCheckedChange = { checked ->
                                    selectedCategories = if (checked) {
                                        selectedCategories + DeviceInfoCollector.CATEGORY_RADIO
                                    } else {
                                        selectedCategories - DeviceInfoCollector.CATEGORY_RADIO
                                    }
                                }
                            )
                            
                            // 屏幕信息
                            CategoryCheckbox(
                                title = stringResource(id = R.string.category_screen),
                                category = DeviceInfoCollector.CATEGORY_SCREEN,
                                isSelected = selectedCategories.contains(DeviceInfoCollector.CATEGORY_SCREEN),
                                onCheckedChange = { checked ->
                                    selectedCategories = if (checked) {
                                        selectedCategories + DeviceInfoCollector.CATEGORY_SCREEN
                                    } else {
                                        selectedCategories - DeviceInfoCollector.CATEGORY_SCREEN
                                    }
                                }
                            )
                            
                            // 内存信息
                            CategoryCheckbox(
                                title = stringResource(id = R.string.category_memory),
                                category = DeviceInfoCollector.CATEGORY_MEMORY,
                                isSelected = selectedCategories.contains(DeviceInfoCollector.CATEGORY_MEMORY),
                                onCheckedChange = { checked ->
                                    selectedCategories = if (checked) {
                                        selectedCategories + DeviceInfoCollector.CATEGORY_MEMORY
                                    } else {
                                        selectedCategories - DeviceInfoCollector.CATEGORY_MEMORY
                                    }
                                }
                            )
                            
                            // 存储信息
                            CategoryCheckbox(
                                title = stringResource(id = R.string.category_storage),
                                category = DeviceInfoCollector.CATEGORY_STORAGE,
                                isSelected = selectedCategories.contains(DeviceInfoCollector.CATEGORY_STORAGE),
                                onCheckedChange = { checked ->
                                    selectedCategories = if (checked) {
                                        selectedCategories + DeviceInfoCollector.CATEGORY_STORAGE
                                    } else {
                                        selectedCategories - DeviceInfoCollector.CATEGORY_STORAGE
                                    }
                                }
                            )
                            
                            // CPU信息
                            CategoryCheckbox(
                                title = stringResource(id = R.string.category_cpu),
                                category = DeviceInfoCollector.CATEGORY_CPU,
                                isSelected = selectedCategories.contains(DeviceInfoCollector.CATEGORY_CPU),
                                onCheckedChange = { checked ->
                                    selectedCategories = if (checked) {
                                        selectedCategories + DeviceInfoCollector.CATEGORY_CPU
                                    } else {
                                        selectedCategories - DeviceInfoCollector.CATEGORY_CPU
                                    }
                                }
                            )
                            
                            // 网络信息
                            CategoryCheckbox(
                                title = stringResource(id = R.string.category_network),
                                category = DeviceInfoCollector.CATEGORY_NETWORK,
                                isSelected = selectedCategories.contains(DeviceInfoCollector.CATEGORY_NETWORK),
                                onCheckedChange = { checked ->
                                    selectedCategories = if (checked) {
                                        selectedCategories + DeviceInfoCollector.CATEGORY_NETWORK
                                    } else {
                                        selectedCategories - DeviceInfoCollector.CATEGORY_NETWORK
                                    }
                                }
                            )
                            
                            // 电池信息
                            CategoryCheckbox(
                                title = stringResource(id = R.string.category_battery),
                                category = DeviceInfoCollector.CATEGORY_BATTERY,
                                isSelected = selectedCategories.contains(DeviceInfoCollector.CATEGORY_BATTERY),
                                onCheckedChange = { checked ->
                                    selectedCategories = if (checked) {
                                        selectedCategories + DeviceInfoCollector.CATEGORY_BATTERY
                                    } else {
                                        selectedCategories - DeviceInfoCollector.CATEGORY_BATTERY
                                    }
                                }
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(
                                    id = R.string.selected_count,
                                    selectedCategories.size,
                                    DeviceInfoCollector.ALL_CATEGORIES.size
                                ),
                                fontSize = 14.sp,
                                color = IOSColors.gray,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
            }
        }
    }
}

/**
 * 类别选择复选框组件
 */
@Composable
private fun CategoryCheckbox(
    title: String,
    category: String,
    isSelected: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = onCheckedChange,
            colors = androidx.compose.material3.CheckboxDefaults.colors(
                checkedColor = IOSColors.primary,
                uncheckedColor = IOSColors.gray
            )
        )
        Text(
            text = title,
            fontSize = 16.sp,
            color = IOSColors.darkText
        )
    }
}

private fun saveDeviceInfoToFile(
    deviceInfoCollector: DeviceInfoCollector,
    snackbarHostState: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope,
    directory: String? = null,
    selectedCategories: Set<String>? = null
) {
    try {
        // 使用指定的文件夹路径或默认的下载目录
        val targetDir = directory ?: Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
        val filePath = deviceInfoCollector.saveDeviceInfoToFile(targetDir, selectedCategories)
        scope.launch {
            snackbarHostState.showSnackbar("File saved to: $filePath")
        }
    } catch (e: Exception) {
        scope.launch {
            snackbarHostState.showSnackbar("Error saving file: ${e.message}")
        }
    }
}

/**
 * 保存设备信息到指定的 URI
 */
private fun saveDeviceInfoToUri(
    deviceInfoCollector: DeviceInfoCollector,
    uri: Uri,
    snackbarHostState: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope,
    filename: String? = null,
    selectedCategories: Set<String>? = null
) {
    try {
        Log.d("DeviceInfo", "Saving to URI: $uri")
        val context = deviceInfoCollector.getContext()
        val deviceInfo = deviceInfoCollector.collectDeviceInfo(selectedCategories)
        
        // 尝试打开输出流
        val outputStream = context.contentResolver.openOutputStream(uri)
        if (outputStream != null) {
            outputStream.use { stream ->
                val jsonString = deviceInfo.toString(4) // Pretty print with 4-space indentation
                stream.write(jsonString.toByteArray())
            }
            
            val displayName = filename ?: uri.lastPathSegment ?: "device_info.json"
            scope.launch {
                snackbarHostState.showSnackbar("File saved successfully: $displayName")
            }
        } else {
            throw IOException("Could not open output stream for URI: $uri")
        }
    } catch (e: Exception) {
        Log.e("DeviceInfo", "Error saving to URI: ${e.message}", e)
        scope.launch {
            snackbarHostState.showSnackbar("Error saving file: ${e.message}")
        }
    }
}
