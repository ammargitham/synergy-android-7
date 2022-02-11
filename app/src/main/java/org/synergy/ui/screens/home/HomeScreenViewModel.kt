package org.synergy.ui.screens.home

import android.app.Application
import android.content.Context.MODE_PRIVATE
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.synergy.data.ServerConfig

class HomeScreenViewModel(application: Application) : AndroidViewModel(application) {
    private var preferences = application.getSharedPreferences(
        "app_preferences",
        MODE_PRIVATE
    )

    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState())

    val uiState: StateFlow<UiState> = _uiState

    init {
        preferences.run {
            val clientName = getString(PROP_clientName, null) ?: "android"
            val serverHost = getString(PROP_serverHost, null) ?: "192.168.0.1"
            val serverPort = getInt(PROP_serverPort, 24800)
            val deviceName = getString(PROP_deviceName, null) ?: "touchscreen"
            _uiState.update {
                it.copy(
                    serverConfig = ServerConfig(
                        clientName,
                        serverHost,
                        serverPort.toString(),
                        deviceName,
                    )
                )
            }
        }
        _uiState.update {
            val hasOverlayDrawPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                    || Settings.canDrawOverlays(application)
            it.copy(hasOverlayDrawPermission = hasOverlayDrawPermission)
        }
    }

    fun updateServerConfig(serverConfig: ServerConfig) {
        _uiState.update { it.copy(serverConfig = serverConfig) }
    }

    fun saveServerConfig() {
        val preferencesEditor = preferences.edit().apply {
            val serverConfig = uiState.value.serverConfig
            putString(PROP_clientName, serverConfig.clientName)
            putString(PROP_serverHost, serverConfig.serverHost)
            putInt(PROP_serverPort, serverConfig.serverPortInt)
            putString(PROP_deviceName, serverConfig.inputDeviceName)
        }
        preferencesEditor.apply()
    }

    fun setBarrierClientServiceBound(bound: Boolean) {
        _uiState.update { it.copy(barrierClientServiceBound = bound) }
    }

    fun setBarrierClientConnected(connected: Boolean) {
        _uiState.update { it.copy(barrierClientConnected = connected) }
    }

    fun setRequestedOverlayDrawPermission(requested: Boolean) {
        _uiState.update { it.copy(hasRequestedOverlayDrawPermission = requested) }
    }

    companion object {
        private const val PROP_clientName = "clientName"
        private const val PROP_serverHost = "serverHost"
        private const val PROP_serverPort = "serverPort"
        private const val PROP_deviceName = "deviceName"
    }
}

data class UiState(
    val serverConfig: ServerConfig = ServerConfig(),
    val hasRequestedOverlayDrawPermission: Boolean = false,
    val hasOverlayDrawPermission: Boolean = false,
    val barrierClientServiceBound: Boolean = false,
    val barrierClientConnected: Boolean = false,
)