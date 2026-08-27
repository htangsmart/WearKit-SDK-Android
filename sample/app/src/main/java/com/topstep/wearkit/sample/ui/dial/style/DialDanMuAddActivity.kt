package com.topstep.wearkit.sample.ui.dial.style

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.RadioGroup
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topstep.wearkit.apis.ability.dial.DanMuCoord
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityDialDanmuAddBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import com.topstep.wearkit.sample.ui.dialog.SelectIntDialogFragment
import com.topstep.wearkit.sample.widget.ColorPickerView

class DialDanMuAddActivity : BaseActivity(), SelectIntDialogFragment.Listener {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityDialDanmuAddBinding

    private var fontStyleIndex = 0
    private var fontSizePx = DanMuTextRenderer.DEFAULT_FONT_SIZE_PX
    private var walkSpeedPxPerSec = DanMuTextRenderer.DEFAULT_WALK_SPEED_PX_PER_SEC
    private var textColor = Color.WHITE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityDialDanmuAddBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.setTitle(R.string.dial_custom_style_danmu_add)

        val shape = wearKit.deviceAbility.getDeviceInfo().shape
        val displayWidth = intent.getIntExtra(EXTRA_DISPLAY_WIDTH, shape.width)
        val defaultY = intent.getIntExtra(EXTRA_DEFAULT_Y, 0)

        viewBind.editDanmuText.setText(R.string.dial_custom_style_danmu_text_default)
        viewBind.editDanmuText.setSelection(viewBind.editDanmuText.text?.length ?: 0)

        viewBind.editTextX.setText(displayWidth.toString())
        viewBind.editTextY.setText(defaultY.toString())
        viewBind.editAnimX.setText("0")
        viewBind.editAnimY.setText("0")

        bindXHints(viewBind.rgTextX, viewBind.rbTextXAbsolute.id, viewBind.editTextX)
        bindYHints(viewBind.rgTextY, viewBind.rbTextYAbsolute.id, viewBind.editTextY)
        bindXHints(viewBind.rgAnimX, viewBind.rbAnimXAbsolute.id, viewBind.editAnimX)
        bindYHints(viewBind.rgAnimY, viewBind.rbAnimYAbsolute.id, viewBind.editAnimY)

        viewBind.btnFontStyle.clickTrigger { selectFontStyle() }
        viewBind.btnFontSize.clickTrigger { selectFontSize() }
        viewBind.btnWalkSpeed.clickTrigger { selectWalkSpeed() }
        viewBind.btnTextColor.clickTrigger { selectTextColor() }
        viewBind.btnAdd.clickTrigger { submit() }

        updateFontStyleButton()
        updateFontSizeButton()
        updateWalkSpeedButton()

        @Suppress("DEPRECATION")
        val draft = intent.getParcelableExtra<DanMuDraft>(EXTRA_DRAFT)
        if (draft != null) {
            applyDraft(draft)
            supportActionBar?.setTitle(R.string.dial_custom_style_danmu_edit)
            viewBind.btnAdd.setText(R.string.action_save)
        }
    }

    private fun selectFontStyle() {
        val items = arrayOf(
            getString(R.string.dial_custom_style_danmu_font_normal),
            getString(R.string.dial_custom_style_danmu_font_bold),
            getString(R.string.dial_custom_style_danmu_font_italic),
            getString(R.string.dial_custom_style_danmu_font_bold_italic),
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dial_custom_style_danmu_font_style)
            .setSingleChoiceItems(items, fontStyleIndex) { dialog, which ->
                fontStyleIndex = which
                updateFontStyleButton()
                dialog.dismiss()
            }
            .show()
    }

    private fun selectFontSize() {
        SelectIntDialogFragment.newInstance(
            min = DanMuTextRenderer.MIN_FONT_SIZE_PX,
            max = DanMuTextRenderer.MAX_FONT_SIZE_PX,
            value = fontSizePx,
            title = getString(R.string.dial_custom_style_danmu_font_size),
        ).show(supportFragmentManager, DIALOG_FONT_SIZE)
    }

    private fun selectWalkSpeed() {
        SelectIntDialogFragment.newInstance(
            min = DanMuTextRenderer.MIN_WALK_SPEED_PX_PER_SEC,
            max = DanMuTextRenderer.MAX_WALK_SPEED_PX_PER_SEC,
            value = walkSpeedPxPerSec,
            title = getString(R.string.dial_custom_style_danmu_walk_speed),
        ).show(supportFragmentManager, DIALOG_WALK_SPEED)
    }

    private fun selectTextColor() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_color_picker, null)
        val colorPickerView = view.findViewById<ColorPickerView>(R.id.color_pick_view)
        MaterialAlertDialogBuilder(this)
            .setView(view)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                textColor = colorPickerView.selectedColor
            }
            .show()
    }

    private fun submit() {
        val text = viewBind.editDanmuText.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) {
            toast(R.string.dial_custom_style_danmu_text_hint)
            return
        }
        val draft = DanMuDraft(
            text = text,
            textColor = textColor,
            fontStyleIndex = fontStyleIndex,
            fontSizePx = fontSizePx,
            imageX = encodeX(viewBind.rgTextX.checkedRadioButtonId, viewBind.editTextX.intValue()),
            imageY = encodeY(viewBind.rgTextY.checkedRadioButtonId, viewBind.editTextY.intValue()),
            walkSpeed = walkSpeedPxPerSec,
            ltr = viewBind.rgDirection.checkedRadioButtonId == R.id.rb_direction_right,
            animationUri = when (viewBind.rgAnimation.checkedRadioButtonId) {
                R.id.rb_anim_1 -> "file:///android_asset/gif/circle.gif"
                R.id.rb_anim_2 -> "file:///android_asset/gif/square.gif"
                R.id.rb_anim_3 -> "file:///android_asset/gif/triangle.gif"
                else -> null
            },
            animX = encodeX(viewBind.rgAnimX.checkedRadioButtonId, viewBind.editAnimX.intValue()),
            animY = encodeY(viewBind.rgAnimY.checkedRadioButtonId, viewBind.editAnimY.intValue()),
        )
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(EXTRA_DRAFT, draft)
            putExtra(EXTRA_EDIT_INDEX, intent.getIntExtra(EXTRA_EDIT_INDEX, NO_EDIT_INDEX))
        })
        finish()
    }

    private fun updateFontStyleButton() {
        viewBind.btnFontStyle.text = when (fontStyleIndex) {
            1 -> getString(R.string.dial_custom_style_danmu_font_bold)
            2 -> getString(R.string.dial_custom_style_danmu_font_italic)
            3 -> getString(R.string.dial_custom_style_danmu_font_bold_italic)
            else -> getString(R.string.dial_custom_style_danmu_font_normal)
        }
    }

    private fun updateFontSizeButton() {
        viewBind.btnFontSize.text = getString(R.string.dial_custom_style_danmu_font_size_value, fontSizePx)
    }

    private fun updateWalkSpeedButton() {
        viewBind.btnWalkSpeed.text =
            getString(R.string.dial_custom_style_danmu_walk_speed_value, walkSpeedPxPerSec)
    }

    override fun onDialogSelectInt(tag: String?, selectValue: Int) {
        when (tag) {
            DIALOG_FONT_SIZE -> {
                fontSizePx = selectValue
                updateFontSizeButton()
            }
            DIALOG_WALK_SPEED -> {
                walkSpeedPxPerSec = selectValue
                updateWalkSpeedButton()
            }
        }
    }

    override fun dialogSelectIntFormat(tag: String?, value: Int): String {
        return when (tag) {
            DIALOG_FONT_SIZE -> getString(R.string.dial_custom_style_danmu_font_size_value, value)
            DIALOG_WALK_SPEED -> getString(R.string.dial_custom_style_danmu_walk_speed_value, value)
            else -> super.dialogSelectIntFormat(tag, value)
        }
    }

    private fun bindXHints(group: RadioGroup, absoluteId: Int, edit: EditText) {
        val updateHint = {
            edit.hint = getString(
                if (group.checkedRadioButtonId == absoluteId) {
                    R.string.dial_custom_style_danmu_coord_hint_left
                } else {
                    R.string.dial_custom_style_danmu_coord_hint_offset
                }
            )
        }
        group.setOnCheckedChangeListener { _, _ -> updateHint() }
        updateHint()
    }

    private fun bindYHints(group: RadioGroup, absoluteId: Int, edit: EditText) {
        val updateHint = {
            edit.hint = getString(
                if (group.checkedRadioButtonId == absoluteId) {
                    R.string.dial_custom_style_danmu_coord_hint_top
                } else {
                    R.string.dial_custom_style_danmu_coord_hint_offset
                }
            )
        }
        group.setOnCheckedChangeListener { _, _ -> updateHint() }
        updateHint()
    }

    private fun applyDraft(draft: DanMuDraft) {
        viewBind.editDanmuText.setText(draft.text)
        viewBind.editDanmuText.setSelection(viewBind.editDanmuText.text?.length ?: 0)
        applyX(
            spec = draft.imageX,
            group = viewBind.rgTextX,
            leftId = R.id.rb_text_x_left,
            rightId = R.id.rb_text_x_right,
            centerId = R.id.rb_text_x_center,
            absoluteId = R.id.rb_text_x_absolute,
            edit = viewBind.editTextX,
        )
        applyY(
            spec = draft.imageY,
            group = viewBind.rgTextY,
            topId = R.id.rb_text_y_top,
            bottomId = R.id.rb_text_y_bottom,
            centerId = R.id.rb_text_y_center,
            absoluteId = R.id.rb_text_y_absolute,
            edit = viewBind.editTextY,
        )
        fontStyleIndex = draft.fontStyleIndex
        fontSizePx = draft.fontSizePx
        textColor = draft.textColor
        walkSpeedPxPerSec = draft.walkSpeed
        updateFontStyleButton()
        updateFontSizeButton()
        updateWalkSpeedButton()
        viewBind.rgDirection.check(
            if (draft.ltr) R.id.rb_direction_right else R.id.rb_direction_left
        )
        viewBind.rgAnimation.check(
            when {
                draft.animationUri?.contains("circle") == true -> R.id.rb_anim_1
                draft.animationUri?.contains("square") == true -> R.id.rb_anim_2
                draft.animationUri?.contains("triangle") == true -> R.id.rb_anim_3
                else -> R.id.rb_anim_none
            }
        )
        applyX(
            spec = draft.animX,
            group = viewBind.rgAnimX,
            leftId = R.id.rb_anim_x_left,
            rightId = R.id.rb_anim_x_right,
            centerId = R.id.rb_anim_x_center,
            absoluteId = R.id.rb_anim_x_absolute,
            edit = viewBind.editAnimX,
        )
        applyY(
            spec = draft.animY,
            group = viewBind.rgAnimY,
            topId = R.id.rb_anim_y_top,
            bottomId = R.id.rb_anim_y_bottom,
            centerId = R.id.rb_anim_y_center,
            absoluteId = R.id.rb_anim_y_absolute,
            edit = viewBind.editAnimY,
        )
    }

    private fun applyX(
        spec: Int,
        group: RadioGroup,
        leftId: Int,
        rightId: Int,
        centerId: Int,
        absoluteId: Int,
        edit: EditText,
    ) {
        group.check(
            when (DanMuCoord.getMode(spec)) {
                DanMuCoord.LEFT -> leftId
                DanMuCoord.RIGHT -> rightId
                DanMuCoord.CENTER -> centerId
                else -> absoluteId
            }
        )
        edit.setText(DanMuCoord.getOffset(spec).toString())
    }

    private fun applyY(
        spec: Int,
        group: RadioGroup,
        topId: Int,
        bottomId: Int,
        centerId: Int,
        absoluteId: Int,
        edit: EditText,
    ) {
        group.check(
            when (DanMuCoord.getMode(spec)) {
                DanMuCoord.TOP -> topId
                DanMuCoord.BOTTOM -> bottomId
                DanMuCoord.CENTER -> centerId
                else -> absoluteId
            }
        )
        edit.setText(DanMuCoord.getOffset(spec).toString())
    }

    private fun encodeX(checkedId: Int, value: Int): Int {
        return when (checkedId) {
            R.id.rb_text_x_left, R.id.rb_anim_x_left -> DanMuCoord.relative(DanMuCoord.LEFT, value)
            R.id.rb_text_x_right, R.id.rb_anim_x_right -> DanMuCoord.relative(DanMuCoord.RIGHT, value)
            R.id.rb_text_x_center, R.id.rb_anim_x_center -> DanMuCoord.relative(DanMuCoord.CENTER, value)
            else -> DanMuCoord.absolute(value)
        }
    }

    private fun encodeY(checkedId: Int, value: Int): Int {
        return when (checkedId) {
            R.id.rb_text_y_top, R.id.rb_anim_y_top -> DanMuCoord.relative(DanMuCoord.TOP, value)
            R.id.rb_text_y_bottom, R.id.rb_anim_y_bottom -> DanMuCoord.relative(DanMuCoord.BOTTOM, value)
            R.id.rb_text_y_center, R.id.rb_anim_y_center -> DanMuCoord.relative(DanMuCoord.CENTER, value)
            else -> DanMuCoord.absolute(value)
        }
    }

    private fun EditText.intValue(default: Int = 0): Int {
        return text?.toString()?.trim()?.toIntOrNull() ?: default
    }

    companion object {
        const val EXTRA_DRAFT = "danmu_draft"
        const val EXTRA_EDIT_INDEX = "edit_index"
        const val EXTRA_DISPLAY_WIDTH = "display_width"
        const val EXTRA_DISPLAY_HEIGHT = "display_height"
        const val EXTRA_DEFAULT_Y = "default_y"
        const val NO_EDIT_INDEX = -1
        private const val DIALOG_FONT_SIZE = "font_size"
        private const val DIALOG_WALK_SPEED = "walk_speed"

        fun createIntent(
            context: Context,
            displayWidth: Int,
            displayHeight: Int,
            defaultY: Int = 0,
            draft: DanMuDraft? = null,
            editIndex: Int = NO_EDIT_INDEX,
        ): Intent {
            return Intent(context, DialDanMuAddActivity::class.java).apply {
                putExtra(EXTRA_DISPLAY_WIDTH, displayWidth)
                putExtra(EXTRA_DISPLAY_HEIGHT, displayHeight)
                putExtra(EXTRA_DEFAULT_Y, defaultY)
                putExtra(EXTRA_EDIT_INDEX, editIndex)
                if (draft != null) {
                    putExtra(EXTRA_DRAFT, draft)
                }
            }
        }
    }
}
