package com.topstep.wearkit.sample

import android.net.Uri
import com.topstep.wearkit.apis.model.config.WKDeviceInfo
import com.topstep.wearkit.apis.model.core.WKDeviceType
import com.topstep.wearkit.apis.model.dial.FitCloudDialStyleResources
import com.topstep.wearkit.apis.model.dial.ShenJuDialStyleResources
import com.topstep.wearkit.apis.model.dial.WKDialStyleResources
import com.topstep.wearkit.apis.model.file.WKResources
import com.topstep.wearkit.base.utils.Optional
import io.reactivex.rxjava3.core.Single

/**
 * Provide your own dial style resources
 */
object MyDialStyleProvider {

    /**
     * Use default resources provide by SDK
     */
    const val STRATEGY_DEFAULT = 0

    /**
     * Provide custom resources
     */
    const val STRATEGY_CUSTOM_RESOURCES = 1

    /**
     * Provide custom resources and constraint
     */
    const val STRATEGY_CUSTOM_ALL = 2

    var strategy = STRATEGY_CUSTOM_ALL

    fun getResources(deviceInfo: WKDeviceInfo): Single<Optional<WKDialStyleResources>> {
        if (strategy == STRATEGY_DEFAULT) {
            //Return null means use default resources and constraint provide by SDK
            //If the SDK can't provide the matching resources or constraint, it will emit [WKUnsupportedException]
            return Single.just(Optional(null))
        }
        if (deviceInfo.type == WKDeviceType.FIT_CLOUD) {
            if (deviceInfo.model.endsWith("4362")) {
                return Single.just(Optional(forDevice4362()))
            }
        } else if (deviceInfo.type == WKDeviceType.SHEN_JU) {
            if (deviceInfo.model == "OSW-802N") {
                return Single.just(Optional(forDeviceOSW802N()))
            }
        }
        //Others,provide null to use default resources provide by SDK
        return Single.just(Optional(null))
    }

    private fun forDevice4362(): WKDialStyleResources {
        val baseUrl = "file:///android_asset/dial/style/4362"
        val images = listOf(
            Uri.parse("$baseUrl/style1.png"),
            Uri.parse("$baseUrl/style2.png"),
            Uri.parse("$baseUrl/style3.png"),
            Uri.parse("$baseUrl/style4.png"),
            Uri.parse("$baseUrl/style5.png"),
        )
        val templates = listOf(
            WKResources(uri = Uri.parse("$baseUrl/template.bin"), size = 967396),
        )
        return if (strategy == STRATEGY_CUSTOM_RESOURCES) {
            WKDialStyleResources(
                images = images, templates = templates,
            )
        } else {
            FitCloudDialStyleResources(
                images = images, templates = templates,
                styleWidth = 200,
                styleHeight = 120,
                paddingX = 20,
                paddingY = 38,
            )
        }
    }

    private fun forDeviceOSW802N(): WKDialStyleResources {
        val baseUrl = "file:///android_asset/dial/style/OSW-802N"
        val images = listOf(
            Uri.parse("$baseUrl/style1.png"),
            Uri.parse("$baseUrl/style2.png"),
            Uri.parse("$baseUrl/style3.png"),
            Uri.parse("$baseUrl/style4.png"),
        )
        val templates = listOf(
            WKResources(uri = Uri.parse("$baseUrl/template1.json"), size = 2694),
            WKResources(uri = Uri.parse("$baseUrl/template2.json"), size = 2700),
            WKResources(uri = Uri.parse("$baseUrl/template3.json"), size = 2749),
            WKResources(uri = Uri.parse("$baseUrl/template4.json"), size = 2738),
        )
        return if (strategy == STRATEGY_CUSTOM_RESOURCES) {
            WKDialStyleResources(
                images = images, templates = templates,
            )
        } else {
            ShenJuDialStyleResources(
                images = images, templates = templates,
            )
        }
    }

}