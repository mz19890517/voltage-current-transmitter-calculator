package com.example.transmittercalc

import java.util.Locale
import kotlin.math.abs

object CalcEngine {
    const val CUR_LOW = 4.0   // mA
    const val CUR_SPAN = 16.0 // mA

    data class CalcResult(
        val theoryMa: Double?,
        val theoryV: Double?,
        val absDevV: Double?,
        val relDevPct: Double?,
        val sugFromMeasured: Double?,
        val sugFromTheory: Double?
    )

    data class TwoPointResult(
        val zeroMa: Double?,
        val fullMa: Double?,
        val zeroOffsetMa: Double?,
        val actualSpanMa: Double?,
        val gainFactor: Double?,
        val suggestedUpper: Double?
    )

    fun theoryMa(lower: Double, upper: Double, inputV: Double): Double {
        val span = upper - lower
        if (span <= 0.0) return CUR_LOW
        return CUR_LOW + CUR_SPAN * (inputV - lower) / span
    }

    fun theoryV(ma: Double, rOhm: Double): Double =
        if (rOhm > 0.0) ma * rOhm / 1000.0 else 0.0

    /** 反推量程上限，使“期望输出电压”由电阻换算出的电流正好落在 4~20mA 线性线上。 */
    fun suggestUpper(lower: Double, upper: Double, inputV: Double, rOhm: Double, desiredV: Double): Double? {
        val span = upper - lower
        if (span <= 0.0 || rOhm <= 0.0) return null
        val i = desiredV * 1000.0 / rOhm // mA
        if (i <= CUR_LOW) return null
        return lower + CUR_SPAN * (inputV - lower) / (i - CUR_LOW)
    }

    fun calc(p: Params): CalcResult {
        val tMa = theoryMa(p.lower, p.upper, p.inputV)
        val tV = theoryV(tMa, p.resOhm)
        val absDev = if (p.measuredV.isFinite()) p.measuredV - tV else null
        val relDev = if (absDev != null && tV != 0.0) absDev / tV * 100.0 else null
        val sugM = if (p.measuredV.isFinite())
            suggestUpper(p.lower, p.upper, p.inputV, p.resOhm, p.measuredV) else null
        val sugT = suggestUpper(p.lower, p.upper, p.inputV, p.resOhm, tV)
        return CalcResult(tMa, tV, absDev, relDev, sugM, sugT)
    }

    fun twoPoint(p: Params, rOhm: Double, zeroInput: Double, zeroV: Double,
                 fullInput: Double, fullV: Double): TwoPointResult {
        val zeroMa = zeroV * 1000.0 / rOhm
        val fullMa = fullV * 1000.0 / rOhm
        val zeroOff = zeroMa - CUR_LOW
        val span = fullMa - zeroMa
        val gain = span / CUR_SPAN
        val sugUpper = if (gain > 0.0) p.lower + (p.upper - p.lower) / gain else null
        return TwoPointResult(zeroMa, fullMa, zeroOff, span, gain, sugUpper)
    }

    fun conclusion(relPct: Double?, sugFromMeas: Double?): List<String> {
        val lines = mutableListOf<String>()
        lines.add(
            "· 校准提醒：线性增益偏差通常无法仅通过修改量程上限完全消除；若各测试点偏差方向一致、幅值接近，属变送器硬件增益偏差，需硬件校准。"
        )
        if (relPct == null || !relPct.isFinite()) return lines
        when {
            abs(relPct) <= 2.0 ->
                lines.add(0, "· 当前偏差在 ±2% 以内，可保持当前量程上限不变。")
            abs(relPct) <= 5.0 -> {
                val sug = if (sugFromMeas != null && sugFromMeas.isFinite()) {
                    "（参考：以实测反推的建议上限 ≈ ${f2(sugFromMeas)} V）"
                } else ""
                lines.add(0, "· 当前偏差介于 ±2%~±5%，若可接受则保持上限；如需更精确，请使用“校准”页做单点或两点校准 $sug")
            }
            else ->
                lines.add(0, "· 当前偏差大于 ±5%，建议检查硬件连接、采样电阻取值或变送器本身。")
        }
        return lines
    }

    fun f(d: Double?): String =
        if (d == null || d.isNaN() || d.isInfinite()) "--" else String.format(Locale.US, "%.3f", d)

    fun f2(d: Double?): String =
        if (d == null || d.isNaN() || d.isInfinite()) "--" else String.format(Locale.US, "%.2f", d)
}