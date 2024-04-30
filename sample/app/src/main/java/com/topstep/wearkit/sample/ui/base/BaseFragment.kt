package com.topstep.wearkit.sample.ui.base

import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import com.topstep.wearkit.sample.utils.promptProgress
import com.topstep.wearkit.sample.utils.promptToast

abstract class BaseFragment : Fragment {

    constructor() : super()

    constructor(@LayoutRes contentLayoutId: Int) : super(contentLayoutId)

    protected val promptToast by promptToast()
    protected val promptProgress by promptProgress()

}