package com.topstep.wearkit.sample.ui.dial.style

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topstep.wearkit.apis.model.dial.WKDialQuality
import com.topstep.wearkit.apis.model.dial.WKDialSpace
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.DialogDialInstallOptionsBinding
import com.topstep.wearkit.sample.databinding.ItemDialInstallSpaceBinding

/**
 * If [qualityLevels] is not empty, show a quality picker; otherwise use [WKDialQuality.SD].
 */
internal fun Activity.chooseDialQuality(
    qualityLevels: List<WKDialQuality>,
    onChosen: (WKDialQuality) -> Unit,
) {
    if (qualityLevels.isEmpty()) {
        onChosen(WKDialQuality.SD)
        return
    }
    val labels = qualityLevels.map { qualityLabel(it) }.toTypedArray()
    MaterialAlertDialogBuilder(this)
        .setTitle(R.string.dial_quality_select)
        .setItems(labels) { _, which ->
            onChosen(qualityLevels[which])
        }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
}

/**
 * Show space and/or quality pickers in one dialog.
 *
 * Space picker is shown only when there are multiple spaces.
 * Quality picker is shown only when [qualityLevels] is not empty.
 * Neither skips the dialog.
 */
internal fun Activity.chooseDialInstallOptions(
    spaces: List<WKDialSpace>,
    qualityLevels: List<WKDialQuality>,
    onChosen: (quality: WKDialQuality, spaceIndex: Int?) -> Unit,
) {
    val chooseSpace = spaces.size > 1
    val hasQuality = qualityLevels.isNotEmpty()
    if (!chooseSpace && !hasQuality) {
        onChosen(WKDialQuality.SD, if (spaces.isEmpty()) null else 0)
        return
    }

    val binding = DialogDialInstallOptionsBinding.inflate(layoutInflater)
    val spaceList = SpaceChoiceList(layoutInflater, binding.spaceList)
    binding.spaceSection.isVisible = chooseSpace
    if (chooseSpace) {
        spaces.forEachIndexed { index, space ->
            spaceList.add(
                title = getString(R.string.dial_space_item_title, index + 1),
                summary = spaceSummary(space),
            )
        }
    }

    binding.qualitySection.isVisible = hasQuality
    if (hasQuality) {
        qualityLevels.forEachIndexed { index, quality ->
            binding.rgQuality.addView(
                qualityButton(binding.rgQuality, qualityLabel(quality), selected = index == 0)
            )
        }
    }
    binding.sectionDivider.isVisible = chooseSpace && hasQuality

    MaterialAlertDialogBuilder(this)
        .setTitle(
            when {
                chooseSpace && hasQuality -> R.string.dial_install_options
                chooseSpace -> R.string.dial_space_select
                else -> R.string.dial_quality_select
            }
        )
        .setView(binding.root, 0, 0, 0, 0)
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(android.R.string.ok) { _, _ ->
            val quality = if (hasQuality) {
                qualityLevels[binding.rgQuality.selectedIndex()]
            } else {
                WKDialQuality.SD
            }
            val spaceIndex = when {
                chooseSpace -> spaceList.selectedIndex
                spaces.isEmpty() -> null
                else -> 0
            }
            onChosen(quality, spaceIndex)
        }
        .show()
}

private fun Activity.qualityLabel(quality: WKDialQuality): String {
    return when (quality) {
        WKDialQuality.SD -> getString(R.string.dial_quality_sd)
        WKDialQuality.LOSSLESS -> getString(R.string.dial_quality_lossless)
    }
}

private fun Activity.spaceSummary(space: WKDialSpace): String {
    val dialLabel = space.dialId?.takeIf { it.isNotEmpty() } ?: getString(R.string.dial_space_empty)
    val freeLabel = if (space.free == Long.MAX_VALUE) {
        getString(R.string.dial_space_unlimited)
    } else {
        "${space.free / 1024}KB"
    }
    return getString(R.string.dial_space_item_summary, dialLabel, freeLabel)
}

private fun Activity.qualityButton(
    parent: RadioGroup,
    label: String,
    selected: Boolean,
): RadioButton {
    return (layoutInflater.inflate(R.layout.item_dial_install_quality, parent, false) as RadioButton).apply {
        id = View.generateViewId()
        text = label
        isChecked = selected
    }
}

private fun RadioGroup.selectedIndex(): Int {
    for (i in 0 until childCount) {
        if (getChildAt(i).id == checkedRadioButtonId) return i
    }
    return 0
}

private class SpaceChoiceList(
    private val inflater: LayoutInflater,
    private val container: LinearLayout,
) {
    var selectedIndex = 0
        private set

    private val radios = mutableListOf<RadioButton>()

    fun add(title: String, summary: String) {
        val index = radios.size
        val item = ItemDialInstallSpaceBinding.inflate(inflater, container, true)
        item.tvTitle.text = title
        item.tvSummary.text = summary
        item.radio.isChecked = index == 0
        item.root.setOnClickListener { select(index) }
        radios.add(item.radio)
    }

    private fun select(index: Int) {
        selectedIndex = index
        radios.forEachIndexed { i, radio ->
            radio.isChecked = i == index
        }
    }
}
