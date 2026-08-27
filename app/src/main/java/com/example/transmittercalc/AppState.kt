package com.example.transmittercalc

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.io.File

data class Params(
    var lower: Double = 0.0,
    var upper: Double = 100.0,
    var resOhm: Double = 120.0,
    var inputV: Double = 0.0,
    var measuredV: Double = 0.0
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("lower", lower)
        put("upper", upper)
        put("resOhm", resOhm)
        put("inputV", inputV)
        put("measuredV", measuredV)
    }

    companion object {
        fun fromJson(o: JSONObject): Params = Params(
            lower = o.optDouble("lower", 0.0),
            upper = o.optDouble("upper", 100.0),
            resOhm = o.optDouble("resOhm", 120.0),
            inputV = o.optDouble("inputV", 0.0),
            measuredV = o.optDouble("measuredV", 0.0)
        )
    }
}

object AppState {
    var params = Params()

    private const val PREFS = "tc_prefs"
    private const val KEY_LOWER = "lower"
    private const val KEY_UPPER = "upper"
    private const val KEY_RES = "res_ohm"
    private const val KEY_IN = "input_v"
    private const val KEY_MEAS = "meas_v"

    fun loadPrefs(c: Context) {
        val p: SharedPreferences = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        params.lower = p.getFloat(KEY_LOWER, 0f).toDouble()
        params.upper = p.getFloat(KEY_UPPER, 100f).toDouble()
        params.resOhm = p.getFloat(KEY_RES, 120f).toDouble()
        params.inputV = p.getFloat(KEY_IN, 0f).toDouble()
        params.measuredV = p.getFloat(KEY_MEAS, 0f).toDouble()
    }

    fun savePrefs(c: Context) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putFloat(KEY_LOWER, params.lower.toFloat())
            .putFloat(KEY_UPPER, params.upper.toFloat())
            .putFloat(KEY_RES, params.resOhm.toFloat())
            .putFloat(KEY_IN, params.inputV.toFloat())
            .putFloat(KEY_MEAS, params.measuredV.toFloat())
            .apply()
    }

    fun saveJson(c: Context): String {
        val f = File(c.filesDir, "params.json")
        f.writeText(params.toJson().toString())
        return f.absolutePath
    }

    fun loadJson(c: Context): Boolean {
        val f = File(c.filesDir, "params.json")
        if (!f.exists()) return false
        params = Params.fromJson(JSONObject(f.readText()))
        return true
    }

    fun loadExample() {
        params = Params(lower = 0.0, upper = 80.0, resOhm = 120.0, inputV = 49.3, measuredV = 1.628)
    }
}