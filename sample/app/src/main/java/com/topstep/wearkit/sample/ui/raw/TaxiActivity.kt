package com.topstep.wearkit.sample.ui.raw

import android.annotation.SuppressLint
import android.os.Bundle
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.fitcloud.sdk.v2.FcSDK
import com.topstep.fitcloud.sdk.v2.model.special.taxi.FcTaxiInfo
import com.topstep.wearkit.apis.model.core.WKConnectorState
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityTaxiBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import timber.log.Timber

class TaxiActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityTaxiBinding

    private var setTaxiDisposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityTaxiBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.title = "Taxi"

        viewBind.btnOrderConfirm.clickTrigger {
            setTaxiInfo(
                FcTaxiInfo.OrderConfirm(
                    origin = "半里花汇",
                    destination = "深圳前海壹方城",
                    carType = "快车",
                    estimatedPrice = "22元",
                    estimatedPickupMinutes = "4分钟",
                )
            )
        }
        viewBind.btnPlacingOrder.clickTrigger {
            setTaxiInfo(FcTaxiInfo.PlacingOrder)
        }
        viewBind.btnNoDriver.clickTrigger {
            setTaxiInfo(FcTaxiInfo.NoDriver)
        }
        viewBind.btnDriverAccepted.clickTrigger {
            setTaxiInfo(
                FcTaxiInfo.DriverAccepted(
                    carModel = "丰田卡罗拉（白色）",
                    licensePlate = "粤B·12345",
                    driverName = "王师傅",
                    driverPhone = "123xxxx6789",
                    pickupPoint = "半里花汇",
                    distance = "1.2KM",
                    estimatedArrival = "4分钟",
                )
            )
        }
        viewBind.btnOrderCancelled.clickTrigger {
            setTaxiInfo(FcTaxiInfo.OrderCancelled)
        }
        viewBind.btnArrived.clickTrigger {
            setTaxiInfo(
                FcTaxiInfo.Arrived(
                    carModel = "丰田卡罗拉（白色）",
                    licensePlate = "粤B·12345",
                    driverName = "王师傅",
                    driverPhone = "123xxxx6789",
                    pickupPoint = "半里花汇",
                    freeWaitSeconds = 180,
                )
            )
        }
        viewBind.btnInTrip.clickTrigger {
            setTaxiInfo(
                FcTaxiInfo.InTrip(
                    remainingDistance = "3.2KM",
                    estimatedArrival = "8分钟",
                    estimatedTotal = "22元",
                )
            )
        }
        viewBind.btnPaymentFailed.clickTrigger {
            setTaxiInfo(FcTaxiInfo.PaymentFailed)
        }
        viewBind.btnTripEnded.clickTrigger {
            setTaxiInfo(
                FcTaxiInfo.TripEnded(
                    actualPrice = "22元",
                )
            )
        }
    }

    @SuppressLint("CheckResult")
    private fun setTaxiInfo(info: FcTaxiInfo) {
        if (wearKit.connector.getConnectorState() != WKConnectorState.CONNECTED) {
            toast("Device not connected!")
            return
        }
        val fcSDK = wearKit.getRawSDK() as? FcSDK
        if (fcSDK == null) {
            toast("This is not a FcSDK device!")
            return
        }
        setTaxiDisposable?.dispose()
        setTaxiDisposable = fcSDK.connector.specialFeature().setTaxiInfo(info)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                toast(R.string.tip_success)
            }, {
                Timber.w(it)
                toast(R.string.tip_failed)
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        setTaxiDisposable?.dispose()
    }

}
