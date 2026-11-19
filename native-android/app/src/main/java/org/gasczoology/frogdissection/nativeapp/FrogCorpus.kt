package org.gasczoology.frogdissection.nativeapp

import android.content.Context
import org.json.JSONObject

data class Layer(val id: Int, val title: String, val image: String, val femaleImage: String?)
data class Organ(
    val key: String, val layer: Int, val system: String, val name: String,
    val tamil: String, val latin: String, val morphology: String,
    val function: String, val viva: String
)
data class FrogPin(
    val key: String, val x: Float, val y: Float, val radius: Float,
    val label: String, val unplaced: Boolean
)
data class FrogQuestion(
    val category: String, val topic: String, val question: String,
    val options: List<String>, val correct: Int, val explanation: String
)
data class GuidedStep(val number: Int, val layer: Int, val tool: String, val instruction: String)

class FrogCorpus(
    val layers: List<Layer>,
    val organs: LinkedHashMap<String, Organ>,
    val questions: List<FrogQuestion>,
    val steps: List<GuidedStep>,
    val notes: Map<String, String>,
    val pins: Map<String, List<FrogPin>>
) {
    companion object {
        fun fromJson(text: String): FrogCorpus {
            val root = JSONObject(text)
            val layers = root.getJSONArray("layers").let { array ->
                (0 until array.length()).map { i ->
                    val obj = array.getJSONObject(i)
                    Layer(obj.getInt("id"), obj.getString("title"), obj.getString("image"),
                        obj.optString("femaleImage").takeIf { it.isNotBlank() })
                }
            }
            val organs = linkedMapOf<String, Organ>()
            val rawOrgans = root.getJSONObject("organs")
            for (key in rawOrgans.keys()) {
                val value = rawOrgans.getJSONObject(key)
                organs[key] = Organ(
                    key = key, layer = value.getInt("layer"),
                    system = value.getString("system"), name = value.getString("name"),
                    tamil = value.getString("tamil"), latin = value.getString("latin"),
                    morphology = value.getString("morphology"),
                    function = value.getString("function"), viva = value.getString("viva")
                )
            }
            val rawQuestions = root.getJSONArray("questions")
            val questions = (0 until rawQuestions.length()).map { i ->
                val obj = rawQuestions.getJSONObject(i)
                val opts = obj.getJSONArray("options")
                FrogQuestion(obj.getString("category"), obj.getString("topic"),
                    obj.getString("question"),
                    (0 until opts.length()).map { j -> opts.getString(j) },
                    obj.getInt("correct"), obj.getString("explanation"))
            }
            val rawSteps = root.getJSONArray("steps")
            val steps = (0 until rawSteps.length()).map { i ->
                val obj = rawSteps.getJSONObject(i)
                GuidedStep(obj.getInt("step"), obj.getInt("layer"), obj.getString("tool"),
                    obj.getString("text").replace(Regex("<[^>]*>"), ""))
            }
            val notes = linkedMapOf<String, String>()
            val noteJson = root.getJSONObject("notes")
            for (key in noteJson.keys()) notes[key] = noteJson.getString(key)
            val pins = linkedMapOf<String, List<FrogPin>>()
            val rawPins = root.getJSONObject("coordinates")
            for (viewKey in rawPins.keys()) {
                val view = rawPins.getJSONObject(viewKey)
                pins[viewKey] = view.keys().asSequence().map { key ->
                    val point = view.getJSONObject(key)
                    FrogPin(key, point.getDouble("x").toFloat(), point.getDouble("y").toFloat(),
                        point.optDouble("r", 18.0).toFloat(),
                        point.getString("label"), point.optBoolean("unplaced", false))
                }.toList()
            }
            return FrogCorpus(layers, organs, questions, steps, notes, pins)
        }

        fun load(context: Context): FrogCorpus {
            val json = context.assets.open("frog_corpus.json").bufferedReader().use { it.readText() }
            return fromJson(json)
        }
    }

    fun viewKey(layer: Int, female: Boolean): String =
        if (layer == 5) "5_" + if (female) "female" else "male" else layer.toString()

    fun imageName(layer: Int, female: Boolean): String {
        val plate = layers.first { it.id == layer }
        return if (layer == 5 && female) plate.femaleImage ?: plate.image else plate.image
    }

    fun platePins(layer: Int, female: Boolean): List<FrogPin> =
        pins[viewKey(layer, female)].orEmpty()

    fun organsForLayer(layer: Int, female: Boolean): List<Organ> =
        organs.values.filter { it.layer == layer && (female || it.key != "oviducts") }
}
