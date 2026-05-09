
package com.example.farmapp.ui.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.farmapp.R
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.abs

@SuppressLint("SetTextI18n")
class CropInputActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CropInput"
    }

    private lateinit var inputN: EditText
    private lateinit var inputP: EditText
    private lateinit var inputK: EditText
    private lateinit var inputPH: EditText
    private lateinit var inputRain: EditText
    private lateinit var inputHumidity: EditText
    private lateinit var inputTemp: EditText
    private lateinit var tvResult: TextView

    private var cropRanges: List<CropRange> = emptyList()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_crop_input)

        inputN = findViewById(R.id.inputNitrogen)
        inputP = findViewById(R.id.inputPhosphorus)
        inputK = findViewById(R.id.inputPotassium)
        inputPH = findViewById(R.id.inputPH)
        inputRain = findViewById(R.id.inputRainfall)
        inputHumidity = findViewById(R.id.inputHumidity)
        inputTemp = findViewById(R.id.inputTemperature)
        tvResult = findViewById(R.id.tvCropResult)

        // Load CSV robustly using header-to-index mapping
        cropRanges = loadCropRanges()

        // For debugging: log first 3 crops loaded
        Log.d(TAG, "Loaded ${cropRanges.size} crops. Sample: ${cropRanges.take(3)}")

        findViewById<Button>(R.id.btnSuggestCrop).setOnClickListener {
            suggestCrop()
        }
    }

    private fun normalizeHeader(s: String): String {
        return s.trim().lowercase().replace(Regex("[^a-z0-9_]"), "_")
    }

    private fun doubleOrNullSafe(s: String?): Double? {
        if (s == null) return null
        val t = s.trim()
        if (t.isEmpty()) return null
        // guard for weird characters
        return t.toDoubleOrNull()
    }

    private fun loadCropRanges(): List<CropRange> {
        val list = mutableListOf<CropRange>()
        try {
            val reader = BufferedReader(InputStreamReader(assets.open("Crop_Ranges_MinMax.csv")))
            val headerLine = reader.readLine() ?: return emptyList()
            val headers = headerLine.split(",").map { normalizeHeader(it) }
            val idxMap = headers.withIndex().associate { it.value to it.index }

            fun idx(name: String): Int? = idxMap[name]

            reader.forEachLine { line ->
                if (line.isBlank()) return@forEachLine
                val cols = line.split(",").map { it.trim() }

                try {
                    val label =
                        cols.getOrNull(idx("label") ?: 0) ?: cols.getOrNull(0) ?: return@forEachLine

                    val nMin =
                        doubleOrNullSafe(cols.getOrNull(idx("n_min") ?: -1)) ?: return@forEachLine
                    val nMax =
                        doubleOrNullSafe(cols.getOrNull(idx("n_max") ?: -1)) ?: return@forEachLine

                    val pMin =
                        doubleOrNullSafe(cols.getOrNull(idx("p_min") ?: -1)) ?: return@forEachLine
                    val pMax =
                        doubleOrNullSafe(cols.getOrNull(idx("p_max") ?: -1)) ?: return@forEachLine

                    val kMin =
                        doubleOrNullSafe(cols.getOrNull(idx("k_min") ?: -1)) ?: return@forEachLine
                    val kMax =
                        doubleOrNullSafe(cols.getOrNull(idx("k_max") ?: -1)) ?: return@forEachLine

                    val tempMin = doubleOrNullSafe(cols.getOrNull(idx("temperature_min") ?: -1))
                        ?: doubleOrNullSafe(cols.getOrNull(idx("temp_min") ?: -1))
                        ?: return@forEachLine
                    val tempMax = doubleOrNullSafe(cols.getOrNull(idx("temperature_max") ?: -1))
                        ?: doubleOrNullSafe(cols.getOrNull(idx("temp_max") ?: -1))
                        ?: return@forEachLine

                    val humMin = doubleOrNullSafe(cols.getOrNull(idx("humidity_min") ?: -1))
                        ?: return@forEachLine
                    val humMax = doubleOrNullSafe(cols.getOrNull(idx("humidity_max") ?: -1))
                        ?: return@forEachLine

                    val phMin =
                        doubleOrNullSafe(cols.getOrNull(idx("ph_min") ?: -1)) ?: return@forEachLine
                    val phMax =
                        doubleOrNullSafe(cols.getOrNull(idx("ph_max") ?: -1)) ?: return@forEachLine

                    val rainMin = doubleOrNullSafe(cols.getOrNull(idx("rainfall_min") ?: -1))
                        ?: doubleOrNullSafe(cols.getOrNull(idx("rain_min") ?: -1))
                        ?: return@forEachLine
                    val rainMax = doubleOrNullSafe(cols.getOrNull(idx("rainfall_max") ?: -1))
                        ?: doubleOrNullSafe(cols.getOrNull(idx("rain_max") ?: -1))
                        ?: return@forEachLine

                    val cr = CropRange(
                        crop = label,
                        nMin = nMin, nMax = nMax,
                        pMin = pMin, pMax = pMax,
                        kMin = kMin, kMax = kMax,
                        tempMin = tempMin, tempMax = tempMax,
                        humMin = humMin, humMax = humMax,
                        phMin = phMin, phMax = phMax,
                        rainMin = rainMin, rainMax = rainMax
                    )
                    list.add(cr)
                } catch (e: Exception) {
                    Log.w(TAG, "Skipping line due to parse error: '$line' -> ${e.message}")
                }
            }
            reader.close()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read CSV: ${e.message}")
        }
        return list
    }

    private fun suggestCrop() {
        val n = inputN.text.toString().trim().toDoubleOrNull()
        val p = inputP.text.toString().trim().toDoubleOrNull()
        val k = inputK.text.toString().trim().toDoubleOrNull()
        val ph = inputPH.text.toString().trim().toDoubleOrNull()
        val rain = inputRain.text.toString().trim().toDoubleOrNull()
        val hum = inputHumidity.text.toString().trim().toDoubleOrNull()
        val temp = inputTemp.text.toString().trim().toDoubleOrNull()

        if (listOf(n, p, k, ph, rain, hum, temp).any { it == null }) {
            tvResult.text = " Please fill all numeric fields correctly."
            return
        }

        val nV = n!!
        val pV = p!!
        val kV = k!!
        val phV = ph!!
        val rainV = rain!!
        val humV = hum!!
        val tempV = temp!!

        // ---- check exact match ----
        val exactMatch = cropRanges.find {
            it.matchesAll(nV, pV, kV, phV, rainV, humV, tempV)
        }

        if (exactMatch != null) {
            val mainCrop = exactMatch.crop

            // top 2 closest (excluding main crop)
            val scored = cropRanges.map { cr ->
                val dist = cr.totalNormalizedDistance(nV, pV, kV, phV, rainV, humV, tempV)
                cr to dist
            }.sortedBy { it.second }

            val suggestions = scored.filter { it.first.crop != mainCrop }
                .take(2)
                .map { it.first.crop }

            tvResult.text = "✅ Best Crop: $mainCrop\n🌱 Other Suggestions: ${suggestions.joinToString(", ")}"
            return
        }

        // ---- fallback (no exact match) ----
        val scored = cropRanges.map { cr ->
            val dist = cr.totalNormalizedDistance(nV, pV, kV, phV, rainV, humV, tempV)
            cr to dist
        }.sortedBy { it.second }

        val top3 = scored.take(3).map { it.first.crop }
        tvResult.text = "Best Crop: ${top3.joinToString(", ")}"
    }



    /** Data model and helpers **/
    data class CropRange(
        val crop: String,
        val nMin: Double, val nMax: Double,
        val pMin: Double, val pMax: Double,
        val kMin: Double, val kMax: Double,
        val tempMin: Double, val tempMax: Double,
        val humMin: Double, val humMax: Double,
        val phMin: Double, val phMax: Double,
        val rainMin: Double, val rainMax: Double
    ) {
        fun matchesAll(
            n: Double,
            p: Double,
            k: Double,
            ph: Double,
            rain: Double,
            hum: Double,
            temp: Double
        ): Boolean {
            return (n in nMin..nMax) &&
                    (p in pMin..pMax) &&
                    (k in kMin..kMax) &&
                    (ph in phMin..phMax) &&
                    (rain in rainMin..rainMax) &&
                    (hum in humMin..humMax) &&
                    (temp in tempMin..tempMax)
        }

        // how many of 7 fields match
        fun matchFieldCount(
            n: Double,
            p: Double,
            k: Double,
            ph: Double,
            rain: Double,
            hum: Double,
            temp: Double
        ): Int {
            var cnt = 0
            if (n in nMin..nMax) cnt++
            if (p in pMin..pMax) cnt++
            if (k in kMin..kMax) cnt++
            if (ph in phMin..phMax) cnt++
            if (rain in rainMin..rainMax) cnt++
            if (hum in humMin..humMax) cnt++
            if (temp in tempMin..tempMax) cnt++
            return cnt
        }

        // normalized distance: sum of distances from range (0 if inside)
        fun totalNormalizedDistance(
            n: Double,
            p: Double,
            k: Double,
            ph: Double,
            rain: Double,
            hum: Double,
            temp: Double
        ): Double {
            fun d(value: Double, min: Double, max: Double): Double {
                return when {
                    value < min -> (min - value) / (max - min + 1e-6)
                    value > max -> (value - max) / (max - min + 1e-6)
                    else -> 0.0
                }
            }
            return d(n, nMin, nMax) + d(p, pMin, pMax) + d(k, kMin, kMax) +
                    d(ph, phMin, phMax) + d(rain, rainMin, rainMax) + d(hum, humMin, humMax) + d(
                temp,
                tempMin,
                tempMax
            )
        }

        // list of human-friendly mismatch messages
        fun fieldsMismatchList(
            n: Double,
            p: Double,
            k: Double,
            ph: Double,
            rain: Double,
            hum: Double,
            temp: Double
        ): List<String> {
            val out = mutableListOf<String>()
            if (n !in nMin..nMax) out.add("N=${n} (needs ${nMin}-${nMax})")
            if (p !in pMin..pMax) out.add("P=${p} (needs ${pMin}-${pMax})")
            if (k !in kMin..kMax) out.add("K=${k} (needs ${kMin}-${kMax})")
            if (ph !in phMin..phMax) out.add("pH=${ph} (needs ${phMin}-${phMax})")
            if (rain !in rainMin..rainMax) out.add("Rain=${rain} (needs ${rainMin}-${rainMax})")
            if (hum !in humMin..humMax) out.add("Hum=${hum} (needs ${humMin}-${humMax})")
            if (temp !in tempMin..tempMax) out.add("Temp=${temp} (needs ${tempMin}-${tempMax})")
            return out
        }
    }
}