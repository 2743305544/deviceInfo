package com.shiyi.deviceinfo.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasStoragePermission.value = isGranted
        if (isGranted) {
            // Permission granted, save the file
            saveDeviceInfoToFile(deviceInfoCollector, snackbarHostState, scope)
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
                
                // Export button
                IOSButton(
                    text = "Export as JSON",
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            // For Android 11+, use the Storage Access Framework
                            saveDeviceInfoToFile(deviceInfoCollector, snackbarHostState, scope)
                        } else {
                            // For older Android versions, request permission if not granted
                            if (hasStoragePermission.value) {
                                saveDeviceInfoToFile(deviceInfoCollector, snackbarHostState, scope)
                            } else {
                                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            }
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private fun saveDeviceInfoToFile(
    deviceInfoCollector: DeviceInfoCollector,
    snackbarHostState: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope
) {
    try {
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
        val filePath = deviceInfoCollector.saveDeviceInfoToFile(downloadDir)
        scope.launch {
            snackbarHostState.showSnackbar("File saved to: $filePath")
        }
    } catch (e: Exception) {
        scope.launch {
            snackbarHostState.showSnackbar("Error saving file: ${e.message}")
        }
    }
}
