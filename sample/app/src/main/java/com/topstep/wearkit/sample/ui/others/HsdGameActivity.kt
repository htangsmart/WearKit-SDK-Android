package com.topstep.wearkit.sample.ui.others

import android.annotation.SuppressLint
import android.os.Bundle
import com.github.kilnn.tool.widget.ktx.clickTrigger
import com.topstep.wearkit.apis.model.b2b.HsdGameRankingTrend
import com.topstep.wearkit.sample.MyApplication
import com.topstep.wearkit.sample.R
import com.topstep.wearkit.sample.databinding.ActivityHsdGameBinding
import com.topstep.wearkit.sample.ui.base.BaseActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import timber.log.Timber

class HsdGameActivity : BaseActivity() {

    private val wearKit = MyApplication.wearKit
    private lateinit var viewBind: ActivityHsdGameBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBind = ActivityHsdGameBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
        supportActionBar?.title = "Game"

        viewBind.btnRequestRecords.clickTrigger {
            requestGameHighestRecords()
        }
        viewBind.btnSetRankingTrends.clickTrigger {
            setGameRankingTrends()
        }
    }

    @SuppressLint("CheckResult", "SetTextI18n")
    private fun requestGameHighestRecords() {
        val gameTypeStr = viewBind.etGameType.text.toString().trim()
        if (gameTypeStr.isEmpty()) {
            toast(R.string.tip_failed)
            return
        }
        val gameType = gameTypeStr.toIntOrNull() ?: return

        wearKit.b2b.hsdAbility.requestGameHighestRecords(gameType)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ records ->
                val sb = StringBuilder()
                sb.appendLine("Game Highest Records (gameType=$gameType ${getGameTypeName(gameType)})")
                sb.appendLine("Count: ${records.size}")
                records.forEachIndexed { index, record ->
                    sb.appendLine("--- Record ${index + 1} ---")
                    sb.appendLine("  timestamp=${record.timestampSeconds}")
                    sb.appendLine("  gameType=${record.gameType}(${getGameTypeName(record.gameType)})")
                    sb.appendLine("  duration=${record.duration}s")
                    sb.appendLine("  score=${record.score}")
                    sb.appendLine("  level=${record.level}")
                }
                viewBind.tvResult.text = sb.toString()
                toast(R.string.tip_success)
            }, {
                Timber.w(it)
                toast(R.string.tip_failed)
            })
    }

    @SuppressLint("CheckResult")
    private fun setGameRankingTrends() {
        val gameTypeStr = viewBind.etGameType.text.toString().trim()
        if (gameTypeStr.isEmpty()) {
            toast(R.string.tip_failed)
            return
        }
        val gameType = gameTypeStr.toIntOrNull() ?: return

        val trends = listOf(
            HsdGameRankingTrend(gameType = gameType, ranking = 1, trend = HsdGameRankingTrend.Trend.UP),
            HsdGameRankingTrend(gameType = gameType, ranking = 5, trend = HsdGameRankingTrend.Trend.DOWN),
            HsdGameRankingTrend(gameType = gameType, ranking = 10, trend = HsdGameRankingTrend.Trend.UNCHANGED),
        )

        wearKit.b2b.hsdAbility.setGameRankingTrends(trends)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                toast(R.string.tip_success)
            }, {
                Timber.w(it)
                toast(R.string.tip_failed)
            })
    }

    private fun getGameTypeName(type: Int): String {
        return when (type) {
            0 -> "PetTraining"
            1 -> "2048"
            2 -> "CandyCrush"
            3 -> "Puzzle"
            4 -> "Airplane"
            5 -> "Racing"
            6 -> "Maze"
            7 -> "Basketball"
            8 -> "Math"
            9 -> "Tetris"
            10 -> "Sudoku"
            11 -> "QA"
            12 -> "Jump"
            13 -> "Stacking"
            14 -> "Connect"
            15 -> "Snake"
            16 -> "24Points"
            17 -> "CandyJump"
            18 -> "Breakout"
            19 -> "3PBasketball"
            20 -> "SoccerTarget"
            21 -> "CuteBlocks"
            22 -> "Gomoku"
            23 -> "BirdTap"
            24 -> "5ColorBricks"
            else -> "Unknown"
        }
    }

}
