package com.topstep.wearkit.sample.utils

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import androidx.annotation.IdRes
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.fragment.NavHostFragment
import com.github.kilnn.tool.dialog.prompt.PromptAutoCancel
import com.github.kilnn.tool.dialog.prompt.PromptDialogHolder
import com.github.kilnn.tool.system.SystemUtil
import com.polidea.rxandroidble3.exceptions.BleDisconnectedException
import com.topstep.fitcloud.sdk.exception.FcUnSupportFeatureException
import com.topstep.wearkit.sample.R
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import timber.log.Timber

fun CoroutineScope.launchWithLog(block: suspend CoroutineScope.() -> Unit): Job {
    return launch(CoroutineExceptionHandler { _, exception ->
        Timber.w(exception)
    }, block = block)
}

inline fun <T, R> T.runCatchingWithLog(block: T.() -> R): Result<R> {
    return try {
        Result.success(block())
    } catch (e: Throwable) {
        Timber.w(e)
        Result.failure(e)
    }
}

fun PromptDialogHolder.showFailed(throwable: Throwable, intercept: Boolean = false, cancelable: Boolean = false, autoCancel: PromptAutoCancel = PromptAutoCancel.DEFAULT, promptId: Int = 0) {
    val text = throwable.toReadableMessage(context)
    showFailed(text, intercept, cancelable, autoCancel, promptId)
}

fun Throwable.toReadableMessage(context: Context): String {
    val resId = when (this) {
//        is AccountException -> {
//            when (errorCode) {
//                AccountException.ERROR_CODE_USER_EXIST -> R.string.error_user_exist
//                AccountException.ERROR_CODE_USER_NOT_EXIST -> R.string.error_user_not_exist
//                AccountException.ERROR_CODE_PASSWORD -> R.string.error_password_incorrect
//                else -> 0
//            }
//        }
        is FcUnSupportFeatureException -> {
            R.string.error_device_un_support
        }
        is BleDisconnectedException -> {
            R.string.device_state_disconnected
        }
        else -> {
            0
        }
    }
    return if (resId != 0) {
        context.getString(resId)
    } else {
        message ?: this::class.java.name
    }
}

@MainThread
fun Fragment.promptToast(tag: String? = null): Lazy<PromptDialogHolder> = lazy(LazyThreadSafetyMode.NONE) {
    PromptDialogHolder(requireContext(), childFragmentManager, if (tag.isNullOrEmpty()) this::class.simpleName + "toast" else tag, theme = R.style.PromptToastTheme)
}

@MainThread
fun FragmentActivity.promptToast(tag: String? = null): Lazy<PromptDialogHolder> = lazy(LazyThreadSafetyMode.NONE) {
    PromptDialogHolder(this, supportFragmentManager, if (tag.isNullOrEmpty()) this::class.simpleName + "toast" else tag, theme = R.style.PromptToastTheme)
}

@MainThread
fun Fragment.promptProgress(tag: String? = null): Lazy<PromptDialogHolder> = lazy(LazyThreadSafetyMode.NONE) {
    PromptDialogHolder(requireContext(), childFragmentManager, if (tag.isNullOrEmpty()) this::class.simpleName + "progress" else tag, theme = R.style.PromptProgressTheme)
}

@MainThread
fun FragmentActivity.promptProgress(tag: String? = null): Lazy<PromptDialogHolder> = lazy(LazyThreadSafetyMode.NONE) {
    PromptDialogHolder(this, supportFragmentManager, if (tag.isNullOrEmpty()) this::class.simpleName + "progress" else tag, theme = R.style.PromptProgressTheme)
}

fun Lifecycle.launchRepeatOnStarted(
    block: suspend CoroutineScope.() -> Unit,
) {
    coroutineScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED, block)
    }
}

val Fragment.viewLifecycle
    get() = viewLifecycleOwner.lifecycle

val Fragment.viewLifecycleScope
    get() = viewLifecycleOwner.lifecycleScope

fun AppCompatActivity.findNavControllerInNavHost(@IdRes containerViewId: Int): NavHostController {
    val navHost = supportFragmentManager.findFragmentById(containerViewId) as NavHostFragment
    return navHost.navController as NavHostController
}

fun flowBluetoothAdapterState(context: Context) = callbackFlow {
    val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    trySend(manager.adapter.isEnabled)
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
            trySend(state == BluetoothAdapter.STATE_ON)
        }
    }
    context.registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
    awaitClose { context.unregisterReceiver(receiver) }
}.distinctUntilChanged()

fun flowLocationServiceState(context: Context) = callbackFlow {
    trySend(SystemUtil.isLocationEnabled(context))
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            trySend(SystemUtil.isLocationEnabled(context))
        }
    }
    context.registerReceiver(receiver, IntentFilter(LocationManager.MODE_CHANGED_ACTION))
    awaitClose { context.unregisterReceiver(receiver) }
}
