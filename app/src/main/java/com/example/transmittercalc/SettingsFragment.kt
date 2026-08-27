package com.example.transmittercalc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsFragment : Fragment() {

    private lateinit var exportLauncher: ActivityResultLauncher<String>
    private lateinit var setTxtFileStatus: TextView
    private lateinit var setTxtExportStatus: TextView
    private lateinit var setTxtSummary: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
            if (uri != null) {
                try {
                    requireContext().contentResolver.openOutputStream(uri)?.use {
                        it.write(buildReport().toByteArray(Charsets.UTF_8))
                    }
                    setTxtExportStatus.text = "已导出报告：" + uri.lastPathSegment
                } catch (e: Exception) {
                    setTxtExportStatus.text = "导出失败：" + (e.message ?: "未知错误")
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_settings, container, false)

        setTxtFileStatus = v.findViewById(R.id.setTxtFileStatus)
        setTxtExportStatus = v.findViewById(R.id.setTxtExportStatus)
        setTxtSummary = v.findViewById(R.id.setTxtSummary)

        v.findViewById<View>(R.id.btnExample).setOnClickListener {
            AppState.loadExample()
            setTxtFileStatus.text = "已载入示例：上限 80V、电阻 120Ω、输入 49.3V、实测 1.628V。"
            refreshSummary()
        }
        v.findViewById<View>(R.id.btnSave).setOnClickListener {
            val path = AppState.saveJson(requireContext())
            setTxtFileStatus.text = "已保存参数到本地文件：\n$path"
        }
        v.findViewById<View>(R.id.btnLoad).setOnClickListener {
            if (AppState.loadJson(requireContext())) {
                setTxtFileStatus.text = "已从本地文件加载参数。"
                refreshSummary()
            } else {
                setTxtFileStatus.text = "未找到已保存的参数文件。"
            }
        }
        v.findViewById<View>(R.id.btnExport).setOnClickListener {
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            exportLauncher.launch("变送输出计算_报告_$stamp.txt")
        }

        return v
    }

    override fun onResume() {
        super.onResume()
        refreshSummary()
    }

    private fun refreshSummary() {
        val p = AppState.params
        val sb = StringBuilder()
        sb.append("· 量程下限：").append(f(p.lower)).append(" V\n")
        sb.append("· 量程上限：").append(f(p.upper)).append(" V\n")
        sb.append("· 采样电阻：").append(f(p.resOhm)).append(" Ω\n")
        sb.append("· 当前输入：").append(f(p.inputV)).append(" V\n")
        sb.append("· 实测输出：").append(f(p.measuredV)).append(" V\n")
        val r = CalcEngine.calc(p)
        sb.append("· 理论电流：").append(f(r.theoryMa)).append(" mA\n")
        sb.append("· 理论电压：").append(f(r.theoryV)).append(" V\n")
        sb.append("· 相对偏差：").append(f2(r.relDevPct)).append(" %")
        setTxtSummary.text = sb.toString()
    }

    private fun buildReport(): String {
        val p = AppState.params
        val r = CalcEngine.calc(p)
        val sb = StringBuilder()
        sb.append("电压/电流变送输出计算器 — 计算报告\n")
        sb.append("生成时间：")
            .append(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
            .append("\n")
        sb.append("========================================\n\n")

        sb.append("一、参数\n")
        sb.append("量程下限：").append(f(p.lower)).append(" V\n")
        sb.append("量程上限：").append(f(p.upper)).append(" V\n")
        sb.append("采样电阻：").append(f(p.resOhm)).append(" Ω\n")
        sb.append("输出模式：4~20 mA（固定）\n\n")

        sb.append("二、输入\n")
        sb.append("当前输入电压：").append(f(p.inputV)).append(" V\n")
        sb.append("实测输出电压：").append(f(p.measuredV)).append(" V\n\n")

        sb.append("三、理论计算\n")
        sb.append("理论输出电流：").append(f(r.theoryMa)).append(" mA（4 + 16 × (输入−下限)/(上限−下限)）\n")
        sb.append("理论输出电压：").append(f(r.theoryV)).append(" V（= 电流 × 电阻 / 1000）\n\n")

        sb.append("四、偏差分析\n")
        sb.append("绝对偏差（实测−理论）：").append(f(r.absDevV)).append(" V\n")
        sb.append("相对偏差：").append(f2(r.relDevPct)).append(" %\n\n")

        sb.append("五、建议新上限\n")
        sb.append("以实测输出为目标反推：").append(f(r.sugFromMeasured)).append(" V\n")
        sb.append("以理论输出为目标反推：").append(f(r.sugFromTheory)).append(" V\n\n")

        sb.append("六、结论\n")
        for (line in CalcEngine.conclusion(r.relDevPct, r.sugFromMeasured)) {
            sb.append(line).append("\n")
        }
        sb.append("\n七、附加提醒\n")
        sb.append("· 本应用仅为辅助计算工具，不能替代标准源硬件校准。\n")
        sb.append("· 所有计算均基于线性模型，实际变送器可能存在非线性，多组数据验证更可靠。\n")
        sb.append("· 若偏差大于 ±5%，建议检查硬件连接或变送器本身。\n")
        return sb.toString()
    }

    private fun f(d: Double?): String =
        if (d == null || d.isNaN() || d.isInfinite()) "--" else String.format(Locale.US, "%.3f", d)

    private fun f2(d: Double?): String =
        if (d == null || d.isNaN() || d.isInfinite()) "--" else String.format(Locale.US, "%.2f", d)
}