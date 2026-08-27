package com.example.transmittercalc

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import java.util.Locale
import kotlin.math.sin

class RealtimeFragment : Fragment() {

    private lateinit var swRealtime: MaterialSwitch
    private lateinit var sliderAmp: Slider
    private lateinit var sliderGainErr: Slider
    private lateinit var txtAmp: TextView
    private lateinit var txtGainErr: TextView

    private lateinit var rtTxtInput: TextView
    private lateinit var rtTxtTheory: TextView
    private lateinit var rtTxtMeas: TextView
    private lateinit var rtTxtDev: TextView
    private lateinit var rtTxtPct: TextView
    private lateinit var rtTxtStatus: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var tick = 0L

    private val loop = object : Runnable {
        override fun run() {
            if (!running) return
            advance()
            handler.postDelayed(this, 600L)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_realtime, container, false)

        swRealtime = v.findViewById(R.id.swRealtime)
        sliderAmp = v.findViewById(R.id.sliderAmp)
        sliderGainErr = v.findViewById(R.id.sliderGainErr)
        txtAmp = v.findViewById(R.id.txtAmp)
        txtGainErr = v.findViewById(R.id.txtGainErr)

        rtTxtInput = v.findViewById(R.id.rtTxtInput)
        rtTxtTheory = v.findViewById(R.id.rtTxtTheory)
        rtTxtMeas = v.findViewById(R.id.rtTxtMeas)
        rtTxtDev = v.findViewById(R.id.rtTxtDev)
        rtTxtPct = v.findViewById(R.id.rtTxtPct)
        rtTxtStatus = v.findViewById(R.id.rtTxtStatus)

        swRealtime.setOnCheckedChangeListener { _, checked ->
            running = checked
            if (checked) {
                handler.removeCallbacks(loop)
                handler.post(loop)
                rtTxtStatus.text = "● 实时监测中（模拟数据源，每 0.6s 刷新）"
            } else {
                handler.removeCallbacks(loop)
                rtTxtStatus.text = "已停止"
            }
            txtAmp.text = fmt1(sliderAmp.value) + " V"
            txtGainErr.text = String.format(Locale.US, "%.1f", sliderGainErr.value) + " %"
        }

        sliderAmp.addOnChangeListener { _, value, _ -> txtAmp.text = fmt1(value) + " V" }
        sliderGainErr.addOnChangeListener { _, value, _ ->
            txtGainErr.text = String.format(Locale.US, "%.1f", value) + " %"
        }

        txtAmp.text = fmt1(sliderAmp.value) + " V"
        txtGainErr.text = String.format(Locale.US, "%.1f", sliderGainErr.value) + " %"
        return v
    }

    override fun onResume() {
        super.onResume()
        if (running) {
            handler.removeCallbacks(loop)
            handler.post(loop)
        }
        advance()
    }

    override fun onPause() {
        handler.removeCallbacks(loop)
        super.onPause()
    }

    override fun onDestroyView() {
        handler.removeCallbacks(loop)
        super.onDestroyView()
    }

    private fun advance() {
        val p = AppState.params
        tick++
        val base = if (p.inputV.isFinite()) p.inputV else 0.0
        val amp = sliderAmp.value.toDouble()
        val gErr = sliderGainErr.value.toDouble() / 100.0
        val input = base + amp * sin(tick * 0.35) + 0.05 * sin(tick * 0.11)

        val tMa = CalcEngine.theoryMa(p.lower, p.upper, input)
        val tV = CalcEngine.theoryV(tMa, p.resOhm)
        val meas = tV * (1.0 - gErr) + 0.002 * sin(tick * 0.07)
        val dev = meas - tV
        val pct = if (tV != 0.0) dev / tV * 100.0 else 0.0

        rtTxtInput.text = fmt3(input) + " V"
        rtTxtTheory.text = fmt2(tV) + " V"
        rtTxtMeas.text = fmt3(meas) + " V"
        rtTxtDev.text = String.format(Locale.US, "%+.3f", dev) + " V"
        rtTxtPct.text = String.format(Locale.US, "%+.2f", pct) + " %"
    }

    private fun fmt1(d: Float): String = String.format(Locale.US, "%.1f", d)
    private fun fmt2(d: Double): String =
        if (d.isNaN() || d.isInfinite()) "--" else String.format(Locale.US, "%.3f", d)
    private fun fmt3(d: Double): String =
        if (d.isNaN() || d.isInfinite()) "--" else String.format(Locale.US, "%.3f", d)
}