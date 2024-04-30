package com.topstep.wearkit.sample.ui.ota

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.base.download.UriCopyDownloader
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.databinding.ActivityLocalOtaBinding
import com.topstep.wearkit.sample.files.AppFiles
import com.topstep.wearkit.sample.ui.base.BaseActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import timber.log.Timber

class LocalOtaActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityLocalOtaBinding

    private val selectFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        if (result.resultCode == Activity.RESULT_OK) {
            if (uri == null) {
                toast("Select file error!")
            } else {
                startOta(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityLocalOtaBinding.inflate(layoutInflater)
        setContentView(viewBind.root)

        supportActionBar?.title = "OTA"

        viewBind.btnSeleftFile.clickTrigger {
            //本地升级
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.type = "*/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            try {
                selectFileLauncher.launch(intent)
            } catch (e: Exception) {
                Timber.w(e)
                toast("Select file error!")
            }
        }
    }

    private var otaDisposable: Disposable? = null

    private fun startOta(uri: Uri) {
        otaDisposable?.dispose()
        val downloader = UriCopyDownloader(this, AppFiles.dirDownload(this), 30_000)
        otaDisposable = downloader.download(uri.toString(), null, true)
            .filter {
                it.progress == 100
            }
            .firstOrError()
            .flatMapObservable {
                wearKit.otaAbility.ota(it.result!!)
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                viewBind.tvOtaState.text = "Progress:$it"
            }, {
                viewBind.tvOtaState.text = it.stackTraceToString()
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        otaDisposable?.dispose()
    }

    private fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }
}