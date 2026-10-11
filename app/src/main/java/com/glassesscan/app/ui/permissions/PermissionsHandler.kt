package com.glassesscan.app.ui.permissions

import android.Manifest
import android.os.Build
import androidx.compose.runtime.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.glassesscan.app.R
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsHandler(
    onPermissionsGranted: @Composable () -> Unit
) {
    val context = LocalContext.current
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val bluetoothAdapter = bluetoothManager.adapter
    
    var isBluetoothEnabled by remember { 
        mutableStateOf(bluetoothAdapter?.isEnabled == true) 
    }

    DisposableEffect(Unit) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    if (state == BluetoothAdapter.STATE_ON) {
                        isBluetoothEnabled = true
                    } else if (state == BluetoothAdapter.STATE_OFF) {
                        isBluetoothEnabled = false
                    }
                }
            }
        }
        context.registerReceiver(receiver, android.content.IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    val enableBtLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        isBluetoothEnabled = bluetoothAdapter?.isEnabled == true
    }

    val permissions = mutableListOf<String>()
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
    } else {
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    permissions.add(Manifest.permission.CAMERA)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
    }

    val permissionState = rememberMultiplePermissionsState(permissions = permissions)

    val mandatoryPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        listOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    
    val allMandatoryGranted = permissionState.permissions
        .filter { it.permission in mandatoryPermissions }
        .all { it.status.isGranted }

    if (allMandatoryGranted) {
        if (isBluetoothEnabled) {
            onPermissionsGranted()
        } else {
            PermissionScreenLayout(
                icon = Icons.Default.Bluetooth,
                title = "Bluetooth Required",
                desc = "To scan for nearby smart glasses, your Bluetooth must be turned ON.",
                buttonText = "Enable Bluetooth",
                onClick = { 
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    enableBtLauncher.launch(enableBtIntent)
                }
            )
        }
    } else {
        PermissionScreenLayout(
            icon = Icons.Default.Security,
            title = "Privacy Shield",
            desc = "To detect smart glasses and hidden recording devices, we need access to your Bluetooth and Location.",
            buttonText = stringResource(R.string.grant_access),
            onClick = { permissionState.launchMultiplePermissionRequest() },
            content = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    PermissionRequirementRow(
                        icon = Icons.Default.Devices,
                        title = "Bluetooth & Nearby",
                        desc = "Scan for nearby smart device signals."
                    )
                    PermissionRequirementRow(
                        icon = Icons.Default.GpsFixed,
                        title = "Location",
                        desc = "Tag detection markers on your tactical map."
                    )
                }
            }
        )
    }
}

@Composable
fun PermissionScreenLayout(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String,
    buttonText: String,
    onClick: () -> Unit,
    content: @Composable (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.2f))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF007AFF), Color(0xFF5856D6))
                        ),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp),
                    tint = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = desc,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            
            if (content != null) {
                Spacer(modifier = Modifier.height(56.dp))
                content()
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun PermissionRequirementRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF007AFF),
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(20.dp))
        Column {
            Text(
                text = title, 
                fontWeight = FontWeight.Bold, 
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc, 
                style = MaterialTheme.typography.bodyMedium, 
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
