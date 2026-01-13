package sincpro.expo.printer.infrastructure.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * INFRASTRUCTURE - Permission Service
 *
 * Handles Android runtime permissions for Bluetooth and storage.
 * This is an infrastructure concern (non-functional).
 */
class PermissionService(
    private val context: Context,
) {
    /**
     * Get required Bluetooth permissions based on Android version
     */
    fun getRequiredBluetoothPermissions(): List<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ requires new Bluetooth permissions
            listOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
            )
        } else {
            // Android 11 and below
            listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        }

    /**
     * Check if all required Bluetooth permissions are granted
     */
    fun hasBluetoothPermissions(): Boolean {
        val permissions = getRequiredBluetoothPermissions()
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Get list of missing Bluetooth permissions
     */
    fun getMissingBluetoothPermissions(): List<String> =
        getRequiredBluetoothPermissions().filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }

    /**
     * Get permission status for all required permissions
     */
    fun getPermissionStatus(): PermissionStatus {
        val required = getRequiredBluetoothPermissions()
        val granted =
            required.filter { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        val denied = required - granted.toSet()

        return PermissionStatus(
            allGranted = denied.isEmpty(),
            grantedPermissions = granted,
            deniedPermissions = denied,
            androidVersion = Build.VERSION.SDK_INT,
        )
    }

    /**
     * Check if location permission is required (Android 11 and below)
     */
    fun isLocationPermissionRequired(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S

    /**
     * Check a specific permission
     */
    fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

/**
 * Permission status information
 */
data class PermissionStatus(
    val allGranted: Boolean,
    val grantedPermissions: List<String>,
    val deniedPermissions: List<String>,
    val androidVersion: Int,
)
