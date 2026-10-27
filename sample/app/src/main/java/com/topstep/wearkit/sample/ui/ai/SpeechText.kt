package com.topstep.wearkit.sample.ui.ai

import android.content.Context
import com.topstep.aikit.exception.AiOperationException
import com.topstep.wearkit.sample.R

enum class SpeechTextKind(val color: Int) {
    /** 问。 */
    SOURCE(0xFF333333.toInt()),

    /** 答。 */
    TARGET(0xFF1565C0.toInt()),

    /** 警告。 */
    WARNING(0xFFF57C00.toInt()),

    /** 出错。 */
    ERROR(0xFFD32F2F.toInt()),
}

/** 按 [AiOperationException.errCode] 生成通用出错文案。 */
fun Context.speechTextError(error: Throwable): String {
    val op = error as? AiOperationException
        ?: return getString(R.string.ds_speech_err_unknown)
    val res = when (op.errCode) {
        AiOperationException.ERR_UNAUTHORIZED -> R.string.ds_speech_err_unauthorized
        AiOperationException.ERR_NETWORK_SOCKET_TIMEOUT,
        AiOperationException.ERR_NETWORK_DISCONNECTED -> R.string.ds_speech_err_network
        AiOperationException.ERR_EMPTY_REQUEST -> R.string.ds_speech_err_empty
        AiOperationException.ERR_DUPLICATE_REQUEST,
        AiOperationException.ERR_SEND_REQUEST -> R.string.ds_speech_err_request
        AiOperationException.ERR_QUOTA_INSUFFICIENT -> R.string.ds_speech_err_quota
        AiOperationException.ERR_UNRECOGNIZED -> return getString(R.string.ds_speech_err_recognize)
        AiOperationException.ERR_UNKNOWN -> return getString(R.string.ds_speech_err_unknown)
        else -> R.string.ds_speech_err_code
    }
    return getString(res, op.errCode)
}
