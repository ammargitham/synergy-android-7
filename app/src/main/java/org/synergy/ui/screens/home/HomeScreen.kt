package org.synergy.ui.screens.home

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import org.synergy.R
import org.synergy.data.db.entities.ServerConfig
import org.synergy.services.BarrierClientService
import org.synergy.services.ConnectionStatus
import org.synergy.ui.common.OnLifecycleEvent
import org.synergy.utils.DisplayUtils
import org.synergy.utils.LocalToolbarState
import org.synergy.utils.Timber
import org.synergy.utils.e

@Composable
fun HomeScreen(
    viewModel: HomeScreenViewModel = hiltViewModel(),
    title: String = stringResource(id = R.string.app_name),
    openSettings: () -> Unit = {},
) {
    val toolbarState = LocalToolbarState.current
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var barrierClientService: BarrierClientService? by remember { mutableStateOf(null) }
    var shouldConnectOnBind by remember { mutableStateOf(false) }

    fun showPermissionDialog(force: Boolean = false) {
        if ((force || !uiState.hasRequestedOverlayDrawPermission) && !uiState.hasOverlayDrawPermission) {
            viewModel.setShowOverlayDrawPermissionDialog(true)
            return
        }
        if ((force || !uiState.hasRequestedAccessibilityPermission) && !uiState.hasAccessibilityPermission) {
            viewModel.setShowAccessibilityPermissionDialog(true)
        }
    }

    val overlayPermActivityLauncher = rememberLauncherForActivityResult(StartActivityForResult()) {
        // refresh permission status in viewmodel
        val (hasOverlayDrawPermission, _) = viewModel.checkPermissions()
        if (hasOverlayDrawPermission) {
            return@rememberLauncherForActivityResult
        }
        Toast.makeText(
            context,
            context.getString(R.string.overlay_permission_denied),
            Toast.LENGTH_SHORT
        ).show()
    }

    val accessibilityPermLauncher = rememberLauncherForActivityResult(StartActivityForResult()) {
        // refresh permission status in viewmodel
        val (_, hasAccessibilityPermission) = viewModel.checkPermissions()
        if (hasAccessibilityPermission) {
            return@rememberLauncherForActivityResult
        }
        Toast.makeText(
            context,
            context.getString(R.string.accessibility_permission_denied),
            Toast.LENGTH_SHORT
        ).show()
    }

    val serviceConnection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                if (service !is BarrierClientService.LocalBinder) {
                    return
                }
                service.service
                    .also { barrierClientService = it }
                    .apply {
                        viewModel.setBarrierClientConnectionStatus(connectionStatus)
                        viewModel.setConnectedServerConfigId(configId)
                        addOnConnectionStatusChangeListener {
                            viewModel.setBarrierClientConnectionStatus(it)
                            viewModel.setConnectedServerConfigId(configId)
                        }
                    }
                    .also {
                        if (shouldConnectOnBind) {
                            uiState.serverConfigs
                                .find { it.id == uiState.selectedConfigId }
                                ?.run { connect(context, this, it) }
                            shouldConnectOnBind = false
                        }
                    }
                viewModel.setBarrierClientServiceBound(true)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                barrierClientService = null
                viewModel.setBarrierClientServiceBound(false)
            }
        }
    }

    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                toolbarState.run {
                    setTitle(title)
                    setActions {
                        IconButton(onClick = openSettings) {
                            Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                        }
                    }
                }
                bindToClientService(
                    context = context,
                    serviceConnection = serviceConnection,
                    autoCreate = false,
                )
                // show permission dialog if required
                showPermissionDialog()
            }
            Lifecycle.Event.ON_PAUSE -> {
                context.unbindService(serviceConnection)
            }
            Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> {
                toolbarState.setActions(null)
            }
            else -> Unit
        }
    }

    HomeScreenContent(
        modifier = Modifier.fillMaxHeight(),
        serverConfigs = uiState.serverConfigs,
        selectedConfigId = uiState.selectedConfigId,
        connectedServerConfig = uiState.connectedServerConfig,
        barrierClientConnectionStatus = uiState.barrierClientConnectionStatus,
        hasOverlayDrawPermission = uiState.hasOverlayDrawPermission,
        hasAccessibilityPermission = uiState.hasAccessibilityPermission,
        showOverlayDrawPermissionDialog = uiState.showOverlayDrawPermissionDialog,
        showAccessibilityPermissionDialog = uiState.showAccessibilityPermissionDialog,
        showAddServerConfigDialog = uiState.showAddServerConfigDialog,
        editServerConfig = uiState.editServerConfig,
        onServerConfigSelectionChange = viewModel::setSelectedConfig,
        onConnectClick = {
            if (uiState.barrierClientConnectionStatus == ConnectionStatus.Connected) {
                barrierClientService?.disconnect()
                return@HomeScreenContent
            }
            if (!uiState.barrierClientServiceBound) {
                shouldConnectOnBind = true
                startBarrierClientService(
                    context = context,
                    barrierClientServiceBound = uiState.barrierClientServiceBound,
                    serviceConnection = serviceConnection,
                )
                return@HomeScreenContent
            }
            uiState.serverConfigs
                .find { it.id == uiState.selectedConfigId }
                ?.run { connect(context, this, barrierClientService) }
        },
        onFixPermissionsClick = { showPermissionDialog(true) },
        onAcceptPermissionClick = {
            if (uiState.showOverlayDrawPermissionDialog) {
                requestOverlayDrawingPermission(
                    context,
                    overlayPermActivityLauncher,
                )
                viewModel.setRequestedOverlayDrawPermission(true)
                viewModel.setShowOverlayDrawPermissionDialog(false)
                return@HomeScreenContent
            }
            requestAccessibilityPermission(accessibilityPermLauncher)
            viewModel.setRequestedAccessibilityPermission(true)
            viewModel.setShowAccessibilityPermissionDialog(false)
        },
        onDismissPermissionDialog = {
            if (uiState.showOverlayDrawPermissionDialog) {
                viewModel.setShowOverlayDrawPermissionDialog(false)
                return@HomeScreenContent
            }
            viewModel.setShowAccessibilityPermissionDialog(false)
        },
        onAddServerConfigClick = { viewModel.setShowAddServerConfigDialog(true) },
        onSaveServerConfig = {
            viewModel.saveServerConfig(it)
            viewModel.setShowAddServerConfigDialog(false)
        },
        onDismissAddServerConfigDialog = { viewModel.setShowAddServerConfigDialog(false) },
        onEditServerConfigClick = { viewModel.setShowAddServerConfigDialog(true, it) },
    )
}

private fun connect(
    context: Context,
    serverConfig: ServerConfig,
    barrierClientService: BarrierClientService?
) {
    val displayBounds = DisplayUtils.getDisplayBounds(context)
    if (displayBounds == null) {
        Timber.e("displayBounds is null")
        return
    }
    barrierClientService?.connect(
        configId = serverConfig.id,
        clientName = serverConfig.clientName,
        ipAddress = serverConfig.serverHost,
        port = serverConfig.serverPortInt,
        screenWidth = displayBounds.width(),
        screenHeight = displayBounds.height(),
    )
}

private fun startBarrierClientService(
    context: Context,
    barrierClientServiceBound: Boolean,
    serviceConnection: ServiceConnection,
) {
    val intent = Intent(context, BarrierClientService::class.java)
    ContextCompat.startForegroundService(context.applicationContext, intent)
    if (!barrierClientServiceBound) {
        bindToClientService(
            context = context,
            serviceConnection = serviceConnection,
        )
    }
}

private fun bindToClientService(
    context: Context,
    serviceConnection: ServiceConnection,
    autoCreate: Boolean = true,
) = context.bindService(
    Intent(context, BarrierClientService::class.java),
    serviceConnection,
    if (autoCreate) ComponentActivity.BIND_AUTO_CREATE else 0
)

private fun requestOverlayDrawingPermission(
    context: Context,
    overlayPermActivityLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
    val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}")
    )
    overlayPermActivityLauncher.launch(intent)
}

private fun requestAccessibilityPermission(
    accessibilityPermLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>
) = accessibilityPermLauncher.launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
