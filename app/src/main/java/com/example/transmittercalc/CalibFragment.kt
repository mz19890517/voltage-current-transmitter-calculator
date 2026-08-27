package com.example.transmittercalc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import java.util.Locale

class CalibFragment : Fragment() {

    private lateinit var cardSingle: View
    private lateinit var cardTwo: View

    private lateinit var calInUin: TextInputEditText
    private lateinit var calInUmeas: TextInputEditText
    private lateinit var calTxtSugSingle: TextView
    private lateinit var calTxtSingleNote: TextView

    private lateinit var calInZeroIn: TextInputEditText
    private lateinit var calInZeroV: TextInputEditText
    private lateinit var calInFullIn: TextInputEditText
    private lateinit var calInFullV: TextInputEditText
    private lateinit var calTxtZeroMa: TextView
    private lateinit var calTxtFullMa: TextView
    private lateinit var calTxtZeroOff: TextView
    private lateinit var calTxtSpan: TextView
    private lateinit var calTxtGain: TextView
    private lateinit var calTxtSugTwo: TextView
    private lateinit var calTxtTwoNote: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_calib, container, false)

        cardSingle = v.findViewById(R.id.cardSingle)
        cardTwo = v.findViewById(R.id.cardTwo)

        calInUin = v.findViewById(R.id.calInUin)
        calInUmeas = v.findViewById(R.id.calInUmeas)
        calTxtSugSingle = v.findViewById(R.id.calTxtSugSingle)
        calTxtSingleNote = v.findViewById(R.id.calTxtSingleNote)

        calInZeroIn = v.findViewById(R.id.calInZeroIn)
        calInZeroV = v.findViewById(R.id.calInZeroV)
        calInFullIn = v.findViewById(R.id.calInFullIn)
        calInFullV = v.findViewById(R.id.calInFullV)
        calTxtZeroMa = v.findViewById(R.id.calTxtZeroMa)
        calTxtFullMa = v.findViewById(R.id.calTxtFullMa)
        calTxtZeroOff = v.findViewById(R.id.calTxtZeroOff)
        calTxtSpan = v.findViewById(R.id.calTxtSpan)
        calTxtGain = v.findViewById(R.id.calTxtGain)
        calTxtSugTwo = v.findViewById(R.id.calTxtSugTwo)
        calTxtTwoNote = v.findViewById(R.id.calTxtTwoNote)

        val toggle = v.findViewById<MaterialButtonToggleGroup>(R.id.calibToggle)
        toggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            cardSingle.visibility = if (checkedId == R.id.btnSingle) View.VISIBLE else View.GONE
            cardTwo.visibility = if (checkedId == R.id.btnTwo) View.VISIBLE else View.GONE
        }

        v.findViewById<View>(R.id.btnCalSingle).setOnClickListener { runSingle() }
        v.findViewById<View>(R.id.btnCalTwo).setOnClickListener { runTwo() }

        return v
    }

    override fun onResume() {
        super.onResume()
        calInFullIn.setText(fmt2(AppState.params.upper))
    }

    private fun runSingle() {
        val p = AppState.params
        val uin = calInUin.text?.toString()?.toDoubleOrNull()
        val umeas = calInUmeas.text?.toString()?.toDoubleOrNull()
        if (uin == null || umeas == null) {
            calTxtSugSingle.text = "--"
            calTxtSingleNote.text = "请输入有效的输入电压与实测输出电压，再点击计算。"
            return
        }
        val sug = CalcEngine.suggestUpper(p.lower, p.upper, uin, p.resOhm, umeas)
        calTxtSugSingle.text = fmt2(sug) + " V"
        val theoryMa = CalcEngine.theoryMa(p.lower, p.upper, uin)
        val theoryV = CalcEngine.theoryV(theoryMa, p.resOhm)
        val measMa = umeas * 1000.0 / p.resOhm
        val devPct = if (theoryV != 0.0) (umeas - theoryV) / theoryV * 100.0 else 0.0
        val note = buildString {
            append("在输入 ${fmt2(uin)} V 处：实测电流 ≈ ${fmt2(measMa)} mA，理论电流 ≈ ${fmt2(theoryMa)} mA，相对偏差 ≈ ${fmt2(devPct)} %。\n")
            if (sug != null) {
                append("若将量程上限改为 ${fmt2(sug)} V，在该输入点实测将与理论一致（其余点仅近似）。\n")
            } else {
                append("此时无法求得上限建议：请检查实测值是否低于 4mA 对应电压（${fmt2(p.resOhm * 4.0 / 1000.0)} V）。\n")
            }
            append("若实测电流偏差由硬件增益引起（各点同方向同幅值），单独修改上限无法完全消除非线性偏差，需硬件校准。")
        }
        calTxtSingleNote.text = note
    }

    private fun runTwo() {
        val p = AppState.params
        val zeroIn = calInZeroIn.text?.toString()?.toDoubleOrNull()
        val zeroV = calInZeroV.text?.toString()?.toDoubleOrNull()
        val fullIn = calInFullIn.text?.toString()?.toDoubleOrNull()
        val fullV = calInFullV.text?.toString()?.toDoubleOrNull()
        if (zeroIn == null || zeroV == null || fullIn == null || fullV == null) {
            calTxtZeroMa.text = "--"
            calTxtFullMa.text = "--"
            calTxtZeroOff.text = "--"
            calTxtSpan.text = "--"
            calTxtGain.text = "--"
            calTxtSugTwo.text = "--"
            calTxtTwoNote.text = "请输入完整的零点与满度数据，再点击计算。"
            return
        }
        val r = CalcEngine.twoPoint(p, p.resOhm, zeroIn, zeroV, fullIn, fullV)
        calTxtZeroMa.text = fmt2(r.zeroMa) + " mA"
        calTxtFullMa.text = fmt2(r.fullMa) + " mA"
        calTxtZeroOff.text = fmt2(r.zeroOffsetMa) + " mA"
        calTxtSpan.text = fmt2(r.actualSpanMa) + " mA"
        calTxtGain.text = fmt2(r.gainFactor)
        calTxtSugTwo.text = fmt2(r.suggestedUpper) + " V"

        val zeroOffsetV = r.zeroOffsetMa?.times(p.resOhm)?.div(1000.0)
        val note = buildString {
            append("零点零偏 ${fmt2(r.zeroOffsetMa)} mA（${fmt2(zeroOffsetV)} V）。\n")
            append("若零偏明显（超过 ±0.5mA），建议优先调节变送器零点电位器或检查接线。\n")
            append("增益系数 ${fmt2(r.gainFactor)}：接近 1 表示满度增益基本正常；明显偏离 1 时可参考修正上限调整量程，但最终仍需标准源校准。")
        }
        calTxtTwoNote.text = note
    }

    private fun fmt2(d: Double?): String =
        if (d == null || d.isNaN() || d.isInfinite()) "--" else String.format(Locale.US, "%.2f", d)
}