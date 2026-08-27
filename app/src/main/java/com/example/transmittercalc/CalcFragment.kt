package com.example.transmittercalc

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import java.util.Locale
import kotlin.math.abs

class CalcFragment : Fragment() {

    private lateinit var inLower: TextInputEditText
    private lateinit var inUpper: TextInputEditText
    private lateinit var inRes: TextInputEditText
    private lateinit var inInput: TextInputEditText
    private lateinit var inMeas: TextInputEditText

    private lateinit var txtTheoryMa: TextView
    private lateinit var txtTheoryV: TextView
    private lateinit var txtMeasuredV: TextView
    private lateinit var txtDevV: TextView
    private lateinit var txtDevPct: TextView
    private lateinit var txtSugMeas: TextView
    private lateinit var txtSugTheory: TextView
    private lateinit var txtConclusion: TextView

    private var updating = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_calc, container, false)

        inLower = v.findViewById(R.id.inLower)
        inUpper = v.findViewById(R.id.inUpper)
        inRes = v.findViewById(R.id.inRes)
        inInput = v.findViewById(R.id.inInput)
        inMeas = v.findViewById(R.id.inMeas)

        txtTheoryMa = v.findViewById(R.id.txtTheoryMa)
        txtTheoryV = v.findViewById(R.id.txtTheoryV)
        txtMeasuredV = v.findViewById(R.id.txtMeasuredV)
        txtDevV = v.findViewById(R.id.txtDevV)
        txtDevPct = v.findViewById(R.id.txtDevPct)
        txtSugMeas = v.findViewById(R.id.txtSugMeas)
        txtSugTheory = v.findViewById(R.id.txtSugTheory)
        txtConclusion = v.findViewById(R.id.txtConclusion)

        v.findViewById<View>(R.id.btnCalc).setOnClickListener {
            AppState.savePrefs(requireContext())
            refresh()
        }
        v.findViewById<View>(R.id.btnLoadExample).setOnClickListener {
            AppState.params = Params(lower = 0.0, upper = 80.0, resOhm = 120.0, inputV = 49.3, measuredV = 1.628)
            populate()
            refresh()
        }

        inLower.addTextChangedListener(watcher { s ->
            AppState.params.lower = s?.toDoubleOrNull() ?: 0.0
            refresh()
        })
        inUpper.addTextChangedListener(watcher { s ->
            AppState.params.upper = s?.toDoubleOrNull() ?: 0.0
            refresh()
        })
        inRes.addTextChangedListener(watcher { s ->
            AppState.params.resOhm = s?.toDoubleOrNull() ?: 0.0
            refresh()
        })
        inInput.addTextChangedListener(watcher { s ->
            AppState.params.inputV = s?.toDoubleOrNull() ?: 0.0
            refresh()
        })
        inMeas.addTextChangedListener(watcher { s ->
            AppState.params.measuredV = s?.toDoubleOrNull() ?: 0.0
            refresh()
        })

        return v
    }

    override fun onResume() {
        super.onResume()
        populate()
        refresh()
    }

    private fun watcher(onChange: (String?) -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (!updating) onChange(s?.toString())
        }
        override fun afterTextChanged(s: Editable?) {}
    }

    private fun populate() {
        updating = true
        inLower.setText(one(AppState.params.lower))
        inUpper.setText(one(AppState.params.upper))
        inRes.setText(one(AppState.params.resOhm))
        inInput.setText(two(AppState.params.inputV))
        inMeas.setText(three(AppState.params.measuredV))
        updating = false
    }

    private fun refresh() {
        val p = AppState.params
        val r = CalcEngine.calc(p)
        txtTheoryMa.text = fmt(r.theoryMa) + " mA"
        txtTheoryV.text = fmt(r.theoryV) + " V"
        txtMeasuredV.text = fmt(p.measuredV) + " V"
        txtDevV.text = fmt(r.absDevV) + " V"
        txtDevPct.text = fmt2(r.relDevPct) + " %"
        txtSugMeas.text = fmt2(r.sugFromMeasured) + " V"
        txtSugTheory.text = fmt2(r.sugFromTheory) + " V"

        val buffer = StringBuilder()
        buffer.append("公式：理论上限反推 = 16 × (输入−下限) / ((期望V × 1000 / ${fmt2(p.resOhm)}) − 4) + 下限\n")
        buffer.append("说明：\n")
        buffer.append("· “以实测输出为目标反推”——把本输入点的输出修到理论线上，量程需改为该值；但其余输入点仍可能有偏差（硬件增益固定）。\n")
        buffer.append("· “以理论输出为目标反推”——恒等于当前上限，说明该点理论值本就不需要调整；实测与理论的偏差由变送器硬件增益决定，改上限无法完全消除。\n\n")
        for (line in conclusionText(r)) buffer.append(line).append("\n")
        buffer.append("\n示例验证：输入 49.3V、上限 80、电阻 120Ω、实测 1.628V → 理论 1.663V、偏差约 −2.1%；" +
            "以实测反推的上限 ≈ 82.5V（仅让该点与理论一致），以理论反推仍为 80V。")
        txtConclusion.text = buffer.toString()
    }

    private fun conclusionText(r: CalcEngine.CalcResult): List<String> {
        return CalcEngine.conclusion(r.relDevPct, r.sugFromMeasured)
    }

    private fun fmt(d: Double?): String =
        if (d == null || d.isNaN() || d.isInfinite()) "--" else String.format(Locale.US, "%.3f", d)

    private fun fmt2(d: Double?): String =
        if (d == null || d.isNaN() || d.isInfinite()) "--" else String.format(Locale.US, "%.2f", d)

    private fun one(d: Double): String =
        String.format(Locale.US, "%.1f", if (d.isFinite()) d else 0.0)

    private fun two(d: Double): String =
        String.format(Locale.US, "%.2f", if (d.isFinite()) d else 0.0)

    private fun three(d: Double): String =
        String.format(Locale.US, "%.3f", if (d.isFinite()) d else 0.0)
}