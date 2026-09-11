package com.topstep.wearkit.sample.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.topstep.wearkit.apis.exception.WKLocationException
import com.topstep.wearkit.apis.model.gps.WKLocation
import com.topstep.wearkit.apis.model.gps.WKLocationAccuracy
import com.topstep.wearkit.apis.model.gps.WKLocationObserveOptions
import com.topstep.wearkit.apis.provider.WKLocationProvider
import com.topstep.wearkit.base.utils.Optional
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Sample 全局定位：用系统 [LocationManager] 提供单次/持续定位。
 * SDK 在 EPO 或设备请求 GPS 流时会回调本 provider。
 */
class SampleLocationProvider(
    private val context: Context,
) : WKLocationProvider {

    override fun requestLocation(): Single<Optional<WKLocation>> {
        val cached = lastKnownLocation()
        if (cached != null) {
            return Single.just(Optional(cached))
        }
        return observeLocation(WKLocationObserveOptions(intervalMs = 0L))
            .filter { it.value != null }
            .firstOrError()
            .timeout(SINGLE_FIX_TIMEOUT_SEC, TimeUnit.SECONDS)
            .onErrorReturnItem(Optional<WKLocation>(null))
    }

    @SuppressLint("MissingPermission")
    override fun observeLocation(options: WKLocationObserveOptions): Observable<Optional<WKLocation>> {
        return Observable.create { emitter ->
            if (!hasLocationPermission()) {
                Timber.w("No location permission")
                emitter.tryOnError(WKLocationException(WKLocationException.ERROR_NO_PERMISSION))
                return@create
            }
            val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providerName = selectProvider(manager, options.accuracy)
            if (providerName == null) {
                Timber.w("No enabled location provider")
                emitter.tryOnError(WKLocationException(WKLocationException.ERROR_SERVICE_DISABLED))
                return@create
            }
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (!emitter.isDisposed) {
                        emitter.onNext(Optional(location.toWK()))
                    }
                }

                override fun onProviderDisabled(provider: String) {
                    if (provider != providerName || emitter.isDisposed) {
                        return
                    }
                    Timber.w("Location provider disabled: %s", provider)
                    emitter.tryOnError(WKLocationException(WKLocationException.ERROR_SERVICE_DISABLED))
                }
            }
            emitter.setCancellable {
                manager.removeUpdates(listener)
            }
            if (emitter.isDisposed) {
                return@create
            }
            lastKnown(manager)?.let { location ->
                if (!emitter.isDisposed) {
                    emitter.onNext(Optional(location.toWK()))
                }
            }
            if (emitter.isDisposed) {
                return@create
            }
            try {
                manager.requestLocationUpdates(
                    providerName,
                    options.intervalMs.coerceAtLeast(0L),
                    options.minDistanceM.coerceAtLeast(0f),
                    listener,
                    Looper.getMainLooper()
                )
            } catch (e: SecurityException) {
                Timber.w(e, "requestLocationUpdates failed")
                if (!emitter.isDisposed) {
                    emitter.tryOnError(WKLocationException(WKLocationException.ERROR_NO_PERMISSION, e))
                }
            } catch (e: Exception) {
                Timber.w(e, "requestLocationUpdates failed")
                if (!emitter.isDisposed) {
                    emitter.tryOnError(WKLocationException(WKLocationException.ERROR_UNKNOWN, e))
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun lastKnownLocation(): WKLocation? {
        if (!hasLocationPermission()) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lastKnown(manager)?.toWK()
    }

    @SuppressLint("MissingPermission")
    private fun lastKnown(manager: LocationManager): Location? {
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        )
        return providers.mapNotNull { name ->
            try {
                manager.getLastKnownLocation(name)
            } catch (_: SecurityException) {
                null
            }
        }.maxByOrNull { it.time }
    }

    private fun selectProvider(manager: LocationManager, accuracy: WKLocationAccuracy): String? {
        val fine = hasFineLocationPermission()
        val preferred = when {
            !fine -> listOf(LocationManager.NETWORK_PROVIDER)
            accuracy == WKLocationAccuracy.HIGH -> listOf(LocationManager.GPS_PROVIDER)
            accuracy == WKLocationAccuracy.LOW -> listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            else -> listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        }
        return preferred.firstOrNull { manager.isProviderEnabled(it) }
    }

    private fun hasLocationPermission(): Boolean {
        return hasFineLocationPermission() ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun hasFineLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun Location.toWK(): WKLocation {
        val snr = if (hasAccuracy()) {
            (100f - accuracy).toInt().coerceIn(0, 100)
        } else {
            0
        }
        val timestamp = if (time > 0) time else System.currentTimeMillis()
        return WKLocation(
            lat = latitude,
            lng = longitude,
            timestampMillis = timestamp,
            snr = snr,
            provider = provider,
        )
    }

    companion object {
        private const val SINGLE_FIX_TIMEOUT_SEC = 10L
    }
}
