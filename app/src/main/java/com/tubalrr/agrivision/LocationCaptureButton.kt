package com.tubalrr.agrivision

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
internal fun LocationCaptureButton(onLocationCaptured: (Double, Double) -> Unit) {
    val context = LocalContext.current
    val manager = remember {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }
    val captureLocation: () -> Unit = {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val candidates = buildList {
            if (fine) add(LocationManager.GPS_PROVIDER)
            if (coarse) add(LocationManager.NETWORK_PROVIDER)
        }
        val provider = candidates.firstOrNull { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
        if (provider == null) {
            Toast.makeText(context, "Turn on GPS/location and try again.", Toast.LENGTH_LONG).show()
        } else {
            try {
                @Suppress("DEPRECATION")
                manager.requestSingleUpdate(provider, object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        onLocationCaptured(location.latitude, location.longitude)
                        Toast.makeText(context, "Coordinates captured.", Toast.LENGTH_SHORT).show()
                    }

                    override fun onProviderEnabled(provider: String) = Unit
                    override fun onProviderDisabled(provider: String) {
                        Toast.makeText(context, "Location provider was disabled.", Toast.LENGTH_SHORT).show()
                    }

                    @Suppress("DEPRECATION")
                    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit
                }, Looper.getMainLooper())
                Toast.makeText(context, "Getting location…", Toast.LENGTH_SHORT).show()
            } catch (_: SecurityException) {
                Toast.makeText(context, "Location permission is required.", Toast.LENGTH_LONG).show()
            }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) captureLocation()
        else Toast.makeText(context, "Location permission was not granted.", Toast.LENGTH_LONG).show()
    }

    OutlinedButton(
        onClick = {
            val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (fine || coarse) captureLocation()
            else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Outlined.MyLocation, contentDescription = null)
        Text("  Capture current GPS coordinates")
    }
}
