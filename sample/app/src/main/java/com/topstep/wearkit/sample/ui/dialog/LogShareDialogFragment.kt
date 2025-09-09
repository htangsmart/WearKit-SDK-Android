package com.topstep.wearkit.sample.ui.dialog

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.content.FileProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topstep.wearkit.sample.utils.FILE_PROVIDER_AUTHORITY
import java.io.File

class LogShareDialogFragment : AppCompatDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val files = requireArguments().getStringArray(EXTRA_ARGS)!!
        val names = files.map {
            File(it).name
        }.toTypedArray()
        return MaterialAlertDialogBuilder(requireContext())
            .setItems(names) { _, which ->
                shareFile(requireContext(), File(files[which]), "*/*")
            }
            .create()
    }

    companion object {
        private const val EXTRA_ARGS = "extraArgs"
        fun newInstance(files: List<File>): LogShareDialogFragment {
            val arguments = Bundle()
            arguments.putStringArray(EXTRA_ARGS, files.map {
                it.path
            }.toTypedArray())
            val fragment = LogShareDialogFragment()
            fragment.arguments = arguments
            return fragment
        }

        fun shareFile(context: Context, file: File, mimeType: String) {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = mimeType
            val uri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(Intent.createChooser(intent, null))
        }
    }
}