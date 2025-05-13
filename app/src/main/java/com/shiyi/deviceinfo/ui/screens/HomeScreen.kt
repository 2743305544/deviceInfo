package com.shiyi.deviceinfo.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
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
    
    Scaffold(
        containerColor = IOSColors.background,
        topBar = { IOSNavigationBar(title = "Device Info") },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            deviceInfo?.let { info ->
                // Device section
                IOSGroupHeader(text = "Device")
                IOSCard {
                    IOSListItem(
                        title = "Manufacturer",
                        value = info.optString("manufacturer"),
                        icon = Icons.Default.Smartphone
                    )
                    IOSListItem(
                        title = "Model",
                        value = info.optString("model"),
                        icon = null
                    )
                    IOSListItem(
                        title = "Brand",
                        value = info.optString("brand"),
                        icon = null,
                        showDivider = false
                    )
                }
                
                // System section
                IOSGroupHeader(text = "System")
                IOSCard {
                    val systemInfo = info.optJSONObject("system") ?: JSONObject()
                    IOSListItem(
                        title = "Android Version",
                        value = systemInfo.optString("androidVersion"),
                        icon = Icons.Default.Info
                    )
                    IOSListItem(
                        title = "API Level",
                        value = systemInfo.optString("apiLevel"),
                        icon = null
                    )
                    IOSListItem(
                        title = "Build ID",
                        value = systemInfo.optString("buildId"),
                        icon = null
                    )
                    IOSListItem(
                        title = "Security Patch",
                        value = systemInfo.optString("securityPatch"),
                        icon = null
                    )
                    IOSListItem(
                        title = "Bootloader",
                        value = systemInfo.optString("bootloader"),
                        icon = null,
                        showDivider = false
                    )
                }
                
                // Hardware section
                IOSGroupHeader(text = "Hardware")
                IOSCard {
                    // CPU info
                    val cpuInfo = info.optJSONObject("cpu")
                    
                    // 添加 CPU 型号显示
                    IOSListItem(
                        title = "CPU Model",
                        value = cpuInfo?.optString("model") ?: "Unknown",
                        icon = Icons.Default.Memory
                    )
                    IOSListItem(
                        title = "CPU Cores",
                        value = cpuInfo?.optString("cores") ?: "Unknown",
                        icon = null
                    )
                    IOSListItem(
                        title = "CPU Architecture",
                        value = cpuInfo?.optString("architecture") ?: "Unknown",
                        icon = null
                    )
                    IOSListItem(
                        title = "CPU Frequency",
                        value = cpuInfo?.optString("maxFrequency") ?: "Unknown",
                        icon = null
                    )
                    
                    // Memory info
                    val memoryInfo = info.optJSONObject("memory")
                    IOSListItem(
                        title = "Total RAM",
                        value = memoryInfo?.optString("totalRam") ?: "Unknown",
                        icon = null
                    )
                    IOSListItem(
                        title = "Available RAM",
                        value = memoryInfo?.optString("availableRam") ?: "Unknown",
                        icon = null
                    )
                    
                    // Storage info
                    val storageInfo = info.optJSONObject("storage")
                    IOSListItem(
                        title = "Internal Storage",
                        value = storageInfo?.optString("internalTotal") ?: "Unknown",
                        icon = Icons.Default.Storage
                    )
                    IOSListItem(
                        title = "Free Storage",
                        value = storageInfo?.optString("internalFree") ?: "Unknown",
                        icon = null,
                        showDivider = false
                    )
                }
                
                // Kernel section
                IOSGroupHeader(text = "Kernel")
                IOSCard {
                    val kernelInfo = info.optJSONObject("kernel") ?: JSONObject()
                    IOSListItem(
                        title = "Kernel Version",
                        value = kernelInfo.optString("version", "Unknown"),
                        icon = null
                    )
                    IOSListItem(
                        title = "Kernel Architecture",
                        value = kernelInfo.optString("architecture", "Unknown"),
                        icon = null,
                        showDivider = false
                    )
                }
                
                // Radio/Baseband section
                IOSGroupHeader(text = "Baseband")
                IOSCard {
                    val radioInfo = info.optJSONObject("radio") ?: JSONObject()
                    IOSListItem(
                        title = "Baseband Version",
                        value = radioInfo.optString("basebandVersion", "Unknown"),
                        icon = null
                    )
                    IOSListItem(
                        title = "Radio Version",
                        value = radioInfo.optString("radioVersion", "Unknown"),
                        icon = null
                    )
                    
                    // SIM info
                    val simInfo = radioInfo.optJSONObject("sim")
                    if (simInfo != null) {
                        IOSListItem(
                            title = "SIM Operator",
                            value = simInfo.optString("operatorName", "Unknown"),
                            icon = null
                        )
                        IOSListItem(
                            title = "SIM State",
                            value = simInfo.optString("state", "Unknown"),
                            icon = null,
                            showDivider = false
                        )
                    }
                }
                
                // Screen section
                IOSGroupHeader(text = "Display")
                IOSCard {
                    val screenInfo = info.optJSONObject("screen")
                    IOSListItem(
                        title = "Resolution",
                        value = "${screenInfo?.optString("widthPixels") ?: "?"} × ${screenInfo?.optString("heightPixels") ?: "?"}",
                        icon = null
                    )
                    IOSListItem(
                        title = "Density",
                        value = screenInfo?.optString("densityDpi") ?: "Unknown",
                        icon = null,
                        showDivider = false
                    )
                }
                
                // Network section
                IOSGroupHeader(text = "Network")
                IOSCard {
                    val networkInfo = info.optJSONObject("network")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        IOSListItem(
                            title = "Wi-Fi",
                            value = if (networkInfo?.optBoolean("hasWifi", false) == true) "Connected" else "Disconnected",
                            icon = null
                        )
                        IOSListItem(
                            title = "Cellular",
                            value = if (networkInfo?.optBoolean("hasCellular", false) == true) "Connected" else "Disconnected",
                            icon = null,
                            showDivider = false
                        )
                    } else {
                        IOSListItem(
                            title = "Connected",
                            value = if (networkInfo?.optBoolean("connected", false) == true) "Yes" else "No",
                            icon = null
                        )
                        IOSListItem(
                            title = "Type",
                            value = networkInfo?.optString("type") ?: "Unknown",
                            icon = null,
                            showDivider = false
                        )
                    }
                }
                
                // Battery section
                IOSGroupHeader(text = "Battery")
                IOSCard {
                    val batteryInfo = info.optJSONObject("battery")
                    IOSListItem(
                        title = "Level",
                        value = "${batteryInfo?.optString("level")}%",
                        icon = null
                    )
                    IOSListItem(
                        title = "Status",
                        value = batteryInfo?.optString("status") ?: "Unknown",
                        icon = null
                    )
                    IOSListItem(
                        title = "Temperature",
                        value = "${batteryInfo?.optString("temperature")}°C",
                        icon = null,
                        showDivider = false
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Export buttons
                IOSButton(
                    text = "Export to Downloads",
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
                    text = "Choose Save Location",
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
                    text = "${if (showCategorySelection) "隐藏类别选择" else "选择要导出的类别"}",
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
                                text = "选择要导出的信息类别",
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
                                    text = "全选",
                                    onClick = { selectedCategories = DeviceInfoCollector.ALL_CATEGORIES.toSet() },
                                    modifier = Modifier.weight(1f)
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                IOSButton(
                                    text = "取消全选",
                                    onClick = { selectedCategories = emptySet() },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = IOSColors.lightGray)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // 基本信息
                            CategoryCheckbox(
                                title = "基本设备信息",
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
                                title = "系统信息",
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
                                title = "内核信息",
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
                                title = "基带信息",
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
                                title = "屏幕信息",
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
                                title = "内存信息",
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
                                title = "存储信息",
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
                                title = "CPU信息",
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
                                title = "网络信息",
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
                                title = "电池信息",
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
                                text = "已选择 ${selectedCategories.size} / ${DeviceInfoCollector.ALL_CATEGORIES.size} 项",
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
