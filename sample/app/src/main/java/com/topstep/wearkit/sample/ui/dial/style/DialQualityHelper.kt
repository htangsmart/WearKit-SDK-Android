package com.topstep.wearkit.sample.ui.dial.style

import android.app.Activity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topstep.wearkit.apis.model.dial.WKDialQuality
import com.topstep.wearkit.sample.R

/**
 * If [qualityLevels] is not empty, show a quality picker; otherwise use [WKDialQuality.HIGH].
 */
internal fun Activity.chooseDialQuality(
    qualityLevels: List<WKDialQuality>,
    onChosen: (WKDialQuality) -> Unit,
) {
    if (qualityLevels.isEmpty()) {
        onChosen(WKDialQuality.HIGH)
        return
    }
    val labels = qualityLevels.map { quality ->
        when (quality) {
            WKDialQuality.HIGH -> getString(R.string.dial_quality_high)
            WKDialQuality.LOSSLESS -> getString(R.string.dial_quality_lossless)
        }
    }.toTypedArray()
    MaterialAlertDialogBuilder(this)
        .setTitle(R.string.dial_quality_select)
        .setItems(labels) { _, which ->
            onChosen(qualityLevels[which])
        }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
}
