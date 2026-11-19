package org.gasczoology.frogdissection.nativeapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Bundle
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Biotech
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import java.nio.ByteBuffer
import kotlin.math.hypot

private val Sea = Color(0xFF2DD4BF)
private val Background = Color(0xFF0B1220)
private val Panel = Color(0xFF172235)
private val Muted = Color(0xFFB9C8DD)
private val Amber = Color(0xFFFBBF24)
private val Danger = Color(0xFFFB7185)
private val nativeColours = darkColorScheme(
    primary = Sea, secondary = Amber, background = Background,
    surface = Panel, onSurface = Color(0xFFF3F7FD), onBackground = Color.White
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(11, 18, 32)
        window.navigationBarColor = android.graphics.Color.rgb(11, 18, 32)
        setContent {
            MaterialTheme(colorScheme = nativeColours) {
                NativeFrogLab()
            }
        }
    }
}

private fun readImage(context: Context, name: String): Bitmap? {
    return runCatching {
        val bytes = context.assets.open(name).use { it.readBytes() }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(bytes)))
    }.onFailure { Log.e("FrogNative", "Could not read plate " + name, it) }.getOrNull()
}

private fun readOverrides(prefs: SharedPreferences): SnapshotStateMap<String, Offset> {
    val values = mutableStateMapOf<String, Offset>()
    runCatching {
        val json = JSONObject(prefs.getString("calibrations", "{}") ?: "{}")
        for (key in json.keys()) {
            val item = json.getJSONObject(key)
            values[key] = Offset(item.getDouble("x").toFloat(), item.getDouble("y").toFloat())
        }
    }
    return values
}

private fun persistOverrides(prefs: SharedPreferences, values: SnapshotStateMap<String, Offset>) {
    val json = JSONObject()
    for ((key, point) in values) {
        json.put(key, JSONObject().put("x", point.x).put("y", point.y))
    }
    prefs.edit().putString("calibrations", json.toString()).apply()
}

private fun visiblePins(
    corpus: FrogCorpus,
    layer: Int,
    female: Boolean,
    overrides: Map<String, Offset>
): List<FrogPin> {
    val view = corpus.viewKey(layer, female)
    return corpus.platePins(layer, female).mapNotNull { source ->
        val custom = overrides[view + ":" + source.key]
        if (source.unplaced && custom == null) null
        else source.copy(x = custom?.x ?: source.x, y = custom?.y ?: source.y)
    }
}

@Composable
private fun NativeFrogLab() {
    val context = LocalContext.current
    val corpus = remember { FrogCorpus.load(context) }
    val prefs = remember { context.getSharedPreferences("native_frog_lab", Context.MODE_PRIVATE) }
    val overrides = remember { readOverrides(prefs) }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var layer by rememberSaveable { mutableIntStateOf(3) }
    var female by rememberSaveable { mutableStateOf(false) }
    var selectedKey by rememberSaveable { mutableStateOf("liver") }
    var tool by rememberSaveable { mutableStateOf("Probe") }
    var calibrating by rememberSaveable { mutableStateOf(false) }
    var showTamil by rememberSaveable { mutableStateOf(true) }
    var step by rememberSaveable { mutableIntStateOf(2) }
    var pinned by rememberSaveable { mutableStateOf(false) }
    var incised by rememberSaveable { mutableStateOf(false) }
    var exerciseExpanded by rememberSaveable { mutableStateOf(false) }
    val retracted = remember { mutableStateListOf<String>() }
    var showPrivacy by rememberSaveable { mutableStateOf(false) }
    val selectLayer: (Int) -> Unit = { number ->
        layer = number
        selectedKey = corpus.organsForLayer(number, female).firstOrNull()?.key ?: ""
        calibrating = false
    }
    val selectStep: (Int) -> Unit = { number ->
        step = number.coerceIn(0, corpus.steps.lastIndex)
        val guided = corpus.steps[step]
        selectLayer(guided.layer)
        tool = guided.tool.replaceFirstChar { it.uppercase() }
    }

    if (showPrivacy) {
        AlertDialog(
            onDismissRequest = { showPrivacy = false },
            title = { Text("Privacy and scientific status") },
            text = {
                Text(PRIVACY_DISCLOSURE.trimIndent(),
                    modifier = Modifier.heightIn(max = 340.dp)
                        .verticalScroll(rememberScrollState()),
                    fontSize = 12.sp, lineHeight = 18.sp)
            },
            confirmButton = {
                TextButton(onClick = { showPrivacy = false }) { Text("Close") }
            }
        )
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            Surface(color = Panel) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Biotech, contentDescription = null,
                        tint = Sea, modifier = Modifier.size(25.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text("Frog Dissection", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                            maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                        Text("Native Zoology Lab", fontSize = 10.sp, color = Muted,
                            maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                    }
                    TextButton(onClick = { showTamil = !showTamil }) {
                        Text(if (showTamil) "தமிழ்" else "EN", fontSize = 11.sp,
                            maxLines = 1, softWrap = false)
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Panel) {
                val names = listOf("Dissection", "Quiz", "Glossary")
                val icons = listOf(Icons.Outlined.Science, Icons.Outlined.Quiz, Icons.Outlined.MenuBook)
                names.forEachIndexed { index, name ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(icons[index], contentDescription = null) },
                        label = { Text(name, fontSize = 11.sp, maxLines = 1) }
                    )
                }
            }
        }
    ) { contentPadding ->
        when (tab) {
            0 -> LazyColumn(
                Modifier.fillMaxSize().padding(contentPadding),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                item {
                    Text("Anatomical plates", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Choose a layer · Tap an organ for notes", color = Muted,
                        fontSize = 11.sp, maxLines = 2)
                }
                item {
                    LayerPicker(corpus, layer, selectLayer)
                }
                if (layer == 5) item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !female,
                            onClick = { female = false; selectedKey = "gonads" },
                            label = { Text("Male specimen") }
                        )
                        FilterChip(
                            selected = female,
                            onClick = { female = true; selectedKey = "gonads" },
                            label = { Text("Female specimen") }
                        )
                    }
                }
                item {
                    AnatomyPlate(
                        imageName = corpus.imageName(layer, female),
                        pins = visiblePins(corpus, layer, female, overrides),
                        selectedKey = selectedKey,
                        isCalibrating = calibrating,
                        onTap = { key ->
                            selectedKey = key
                            if (!calibrating) {
                                when (tool) {
                                    "Pin" -> pinned = true
                                    "Scalpel" -> incised = true
                                    "Forceps" -> {
                                        val plateKey = corpus.viewKey(layer, female) + ":" + key
                                        if (plateKey !in retracted) retracted.add(plateKey)
                                    }
                                }
                            }
                        },
                        onCalibrate = { pos ->
                            val code = corpus.viewKey(layer, female) + ":" + selectedKey
                            overrides[code] = pos
                            persistOverrides(prefs, overrides)
                            calibrating = false
                        },
                        retractedKeys = retracted
                            .filter { it.startsWith(corpus.viewKey(layer, female) + ":") }
                            .map { it.substringAfter(":") }.toSet()
                    )
                }
                item {
                    val isCalibrated = overrides.keys.any {
                        it.startsWith(corpus.viewKey(layer, female) + ":")
                    }
                    Text(
                        if (isCalibrated) "Pins adjusted locally; anatomical review pending."
                        else "Pins provisional · Verify against this photograph.",
                        modifier = Modifier.fillMaxWidth()
                            .background(Color(0xFF2B2519), RoundedCornerShape(9.dp))
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        color = Color(0xFFFFDF91),
                        fontSize = 11.sp, lineHeight = 15.sp,
                        maxLines = 3
                    )
                }
                item {
                    Text("Tap a marker or choose an organ below. Pinch to zoom.",
                        fontSize = 11.sp, color = Muted, maxLines = 2)
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val candidates = corpus.organsForLayer(layer, female)
                        items(candidates.size) { index ->
                            val organ = candidates[index]
                            FilterChip(
                                selected = selectedKey == organ.key,
                                onClick = { selectedKey = organ.key },
                                label = { Text(if (organ.key == "gonads")
                                    (if (female) "Ovaries" else "Testes") else organ.name,
                                    maxLines = 1) }
                            )
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = calibrating,
                            onClick = { calibrating = !calibrating },
                            label = { Text(if (calibrating) "Tap actual organ" else "Calibrate pin") }
                        )
                        OutlinedButton(onClick = {
                            overrides.remove(corpus.viewKey(layer, female) + ":" + selectedKey)
                            persistOverrides(prefs, overrides)
                            calibrating = false
                        }) { Text("Reset pin", fontSize = 12.sp) }
                    }
                }
                item {
                    val organ = corpus.organs[selectedKey]
                    if (organ != null) {
                        if (selectedKey == "brain") {
                            Text("Supplementary dorsal neuroanatomy — not visible in the ventral urogenital photograph.",
                                color = Amber, fontSize = 11.sp, lineHeight = 16.sp)
                        }
                        OrganReader(sexSpecificOrgan(organ, female), showTamil, corpus.notes[selectedKey])
                    }
                }
                item {
                    ElevatedCard {
                        Column(Modifier.fillMaxWidth().padding(11.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Virtual practical tools", color = Sea,
                                        fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("Pin · Scalpel · Forceps · Probe",
                                        fontSize = 10.sp, color = Muted, maxLines = 1,
                                        overflow = TextOverflow.Ellipsis)
                                }
                                TextButton(onClick = { exerciseExpanded = !exerciseExpanded }) {
                                    Text(if (exerciseExpanded) "Hide" else "Open", fontSize = 11.sp)
                                }
                            }
                            Text("Pins: " + (if (pinned) "placed" else "—") +
                                " · Incision: " + (if (incised) "marked" else "—") +
                                " · Retracted markers: " + retracted.size,
                                color = Muted, fontSize = 11.sp, maxLines = 2)
                            if (exerciseExpanded) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(listOf("Pin", "Scalpel", "Forceps", "Probe")) { item ->
                                        FilterChip(
                                            selected = tool == item,
                                            onClick = { tool = item },
                                            label = { Text(item, fontSize = 11.sp) }
                                        )
                                    }
                                }
                                Button(onClick = {
                                    when (tool) {
                                        "Pin" -> pinned = true
                                        "Scalpel" -> incised = true
                                        "Forceps" -> if (selectedKey.isNotBlank()) {
                                            val plateKey = corpus.viewKey(layer, female) + ":" + selectedKey
                                            if (plateKey !in retracted) retracted.add(plateKey)
                                        }
                                    }
                                }) {
                                    Text(when (tool) {
                                        "Pin" -> "Place virtual pins"
                                        "Scalpel" -> "Record virtual incision"
                                        "Forceps" -> "Retract selected marker"
                                        else -> "Inspect selected organ"
                                    }, fontSize = 12.sp, maxLines = 2)
                                }
                                Text("Tap a photo marker to use the chosen tool. " +
                                    "Retraction changes marker status, not the underlying specimen photo.",
                                    color = Muted, fontSize = 11.sp, lineHeight = 15.sp)
                                TextButton(onClick = {
                                    pinned = false
                                    incised = false
                                    retracted.clear()
                                    tool = "Probe"
                                }) { Text("Reset virtual exercise", fontSize = 11.sp) }
                            }
                        }
                    }
                }
                item {
                    val current = corpus.steps[step]
                    ElevatedCard {
                        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("GUIDED LAB PROTOCOL · " + (step + 1) + "/" + corpus.steps.size,
                                fontSize = 12.sp, color = Amber, fontWeight = FontWeight.Bold)
                            Text(current.instruction, fontSize = 12.sp, lineHeight = 19.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { selectStep(step - 1) }, enabled = step > 0) {
                                    Text("Previous")
                                }
                                Button(onClick = { selectStep(step + 1) }, enabled = step < corpus.steps.lastIndex) {
                                    Text("Next step")
                                }
                            }
                        }
                    }
                }
                item {
                    Text("Scientific review pending for photographic marker positions and image licences.",
                        color = Muted, fontSize = 11.sp)
                    TextButton(onClick = { showPrivacy = true }) {
                        Text("Privacy and local data", fontSize = 12.sp)
                    }
                }
            }
            1 -> QuizScreen(corpus, Modifier.padding(contentPadding))
            else -> GlossaryScreen(corpus, showTamil, Modifier.padding(contentPadding)) {
                selectedKey = it.key
                layer = it.layer
                if (it.key == "oviducts") female = true
                tab = 0
            }
        }
    }
}

@Composable
private fun LayerPicker(corpus: FrogCorpus, layer: Int, onChoose: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            corpus.layers.forEach { item ->
                val selected = layer == item.id
                Button(
                    onClick = { onChoose(item.id) },
                    modifier = Modifier.weight(1f).heightIn(min = 46.dp),
                    shape = RoundedCornerShape(9.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) Sea else Panel,
                        contentColor = if (selected) Background else Color(0xFFE2E8F0)
                    )
                ) {
                    Text(item.id.toString(), fontSize = 15.sp,
                        fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
        Text("Layer $layer · " + corpus.layers.first { it.id == layer }.title,
            fontWeight = FontWeight.SemiBold, color = Sea,
            fontSize = 12.sp, maxLines = 2)
    }
}

@Composable
private fun AnatomyPlate(
    imageName: String,
    pins: List<FrogPin>,
    selectedKey: String,
    isCalibrating: Boolean,
    onTap: (String) -> Unit,
    onCalibrate: (Offset) -> Unit,
    retractedKeys: Set<String> = emptySet()
) {
    val context = LocalContext.current
    val bitmap = remember(imageName) { readImage(context, imageName) }
    var zoom by remember(imageName) { mutableFloatStateOf(1f) }
    var pan by remember(imageName) { mutableStateOf(Offset.Zero) }

    if (bitmap == null) {
        ElevatedCard {
            Text("The plate could not be decoded: " + imageName,
                modifier = Modifier.padding(18.dp), color = Danger)
        }
        return
    }
    val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
    Column {
        Box(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF40566C), RoundedCornerShape(12.dp))
                .background(Color.Black)
        ) {
            Box(
                Modifier.fillMaxWidth().aspectRatio(ratio)
                    .clip(RoundedCornerShape(12.dp))
                    .graphicsLayer {
                        scaleX = zoom
                        scaleY = zoom
                        translationX = pan.x
                        translationY = pan.y
                    }
                    .pointerInput(imageName) {
                        detectTransformGestures { _, changePan, changeZoom, _ ->
                            zoom = (zoom * changeZoom).coerceIn(1f, 5f)
                            pan = if (zoom <= 1f) Offset.Zero else pan + changePan
                        }
                    }
                    .pointerInput(pins, isCalibrating, selectedKey) {
                        detectTapGestures { tap ->
                            val x = tap.x / size.width * 1000f
                            val y = tap.y / size.height * 562f
                            if (isCalibrating) {
                                if (selectedKey.isNotBlank()) {
                                    onCalibrate(Offset(x.coerceIn(0f, 1000f), y.coerceIn(0f, 562f)))
                                }
                            } else {
                                val nearest = pins.minByOrNull {
                                    hypot(it.x - x, it.y - y)
                                }
                                if (nearest != null && hypot(nearest.x - x, nearest.y - y) <= 62f) {
                                    onTap(nearest.key)
                                }
                            }
                        }
                    }
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Frog specimen: " + imageName,
                    modifier = Modifier.fillMaxWidth().aspectRatio(ratio),
                    contentScale = ContentScale.FillBounds
                )
                Canvas(Modifier.fillMaxSize()) {
                    val scaleX = size.width / 1000f
                    val scaleY = size.height / 562f
                    for (point in pins) {
                        val at = Offset(point.x * scaleX, point.y * scaleY)
                        val selected = point.key == selectedKey
                        val retracted = point.key in retractedKeys
                        // Markers must not hide organs or the photographs' original labels.
                        // Keep unselected points tiny and only ring the selected structure.
                        val radius = markerRadiusPx(size.minDimension, selected)
                        val paint = if (retracted) Muted else if (selected) Amber else Sea
                        drawCircle(Color.Black.copy(alpha = 0.68f), radius + 1.5.dp.toPx(), at)
                        if (selected) {
                            drawCircle(paint.copy(alpha = 0.14f), radius + 2.dp.toPx(), at)
                            drawCircle(paint, radius, at, style = Stroke(width = 1.4.dp.toPx()))
                        }
                        drawCircle(paint, if (selected) radius * .44f else radius, at)
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(pins.firstOrNull { it.key == selectedKey }?.label ?: "Tap an organ",
                modifier = Modifier.weight(1f),
                color = Muted, fontSize = 10.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { zoom = (zoom + .5f).coerceAtMost(5f) }) {
                    Text("Zoom +", fontSize = 11.sp, maxLines = 1)
                }
                TextButton(onClick = { zoom = 1f; pan = Offset.Zero }) {
                    Text("Reset", fontSize = 11.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun OrganReader(organ: Organ, showTamil: Boolean, note: String?) {
    ElevatedCard {
        Column(Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(organ.system.uppercase(), color = Sea, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(organ.name, fontWeight = FontWeight.Bold, fontSize = 19.sp)
            if (showTamil) Text(organ.tamil, color = Amber, fontSize = 16.sp)
            Text(organ.latin, color = Muted, fontSize = 12.sp)
            HorizontalDivider(color = Color(0xFF3A5068))
            Text("ANATOMY & MORPHOLOGY", color = Sea, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(organ.morphology, fontSize = 14.sp, lineHeight = 21.sp)
            Text("PHYSIOLOGICAL FUNCTION", color = Sea, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(organ.function, fontSize = 14.sp, lineHeight = 21.sp)
            Surface(color = Color(0xFF382F20), shape = RoundedCornerShape(10.dp)) {
                Column(Modifier.padding(10.dp)) {
                    Text("PRACTICAL VIVA", fontSize = 12.sp, color = Amber, fontWeight = FontWeight.Bold)
                    Text(organ.viva, fontSize = 13.sp, lineHeight = 19.sp)
                }
            }
            if (!note.isNullOrBlank()) {
                Surface(color = Color(0xFF0D3433), shape = RoundedCornerShape(10.dp)) {
                    Column(Modifier.padding(10.dp)) {
                        Text("SPOTTER CHECK", fontSize = 12.sp, color = Sea, fontWeight = FontWeight.Bold)
                        Text(note, fontSize = 13.sp, lineHeight = 19.sp)
                    }
                }
            }
        }
    }
}


@Composable
private fun QuizScreen(corpus: FrogCorpus, modifier: Modifier = Modifier) {
    var position by rememberSaveable { mutableIntStateOf(0) }
    var selectedOption by rememberSaveable { mutableIntStateOf(-1) }
    var marks by rememberSaveable { mutableIntStateOf(0) }
    var spotter by rememberSaveable { mutableStateOf(false) }
    var spotterIndex by rememberSaveable { mutableIntStateOf(0) }
    var spotterScore by rememberSaveable { mutableIntStateOf(0) }
    val spotterPins = remember(corpus) {
        corpus.platePins(3, false).filter { !it.unplaced && corpus.organs.containsKey(it.key) }
    }

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Practical assessment", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                FilterChip(selected = !spotter, onClick = { spotter = false },
                    label = { Text("MCQs (" + corpus.questions.size + ")") })
                FilterChip(selected = spotter, onClick = { spotter = true },
                    label = { Text("Photo spotter") })
            }
        }

        if (spotter) {
            item {
                Text("Unverified photo markers · Educational practice only, not graded assessment.",
                    fontSize = 12.sp, color = Amber, lineHeight = 17.sp)
            }
            if (spotterPins.isEmpty()) {
                item { Text("No visible anatomical markers are available for this plate.") }
            } else if (spotterIndex >= spotterPins.size) {
                item {
                    ElevatedCard {
                        Column(Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Spotter practice complete", color = Sea,
                                fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Correct: " + spotterScore + "/" + spotterPins.size,
                                fontSize = 17.sp)
                            Text("Scores are provisional because the original photographic marker positions have not passed anatomical sign-off.",
                                color = Amber, fontSize = 12.sp)
                            Button(onClick = { spotterIndex = 0; spotterScore = 0 }) {
                                Text("Restart spotter")
                            }
                        }
                    }
                }
            } else {
                val target = spotterPins[spotterIndex]
                item {
                    Text("Identify: " + target.label, fontWeight = FontWeight.Bold,
                        color = Amber, fontSize = 16.sp)
                    Text("Target " + (spotterIndex + 1) + "/" + spotterPins.size,
                        color = Muted, fontSize = 12.sp)
                }
                item {
                    AnatomyPlate(
                        imageName = "frog viscera.jpg", pins = spotterPins,
                        selectedKey = "", isCalibrating = false,
                        onTap = { key ->
                            if (key == target.key) spotterScore++
                            spotterIndex = advanceAssessment(spotterIndex, spotterPins.size)
                        },
                        onCalibrate = {}
                    )
                    OutlinedButton(
                        onClick = { spotterIndex = advanceAssessment(spotterIndex, spotterPins.size) }
                    ) { Text("Skip target") }
                }
            }
        } else if (position >= corpus.questions.size) {
            item {
                ElevatedCard {
                    Column(Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("MCQ assessment complete", fontSize = 19.sp,
                            fontWeight = FontWeight.Bold, color = Sea)
                        Text("Score: " + marks + "/" + corpus.questions.size, fontSize = 18.sp)
                        Text("You have completed all 26 questions. Review the explanations or begin a fresh attempt.",
                            fontSize = 12.sp, color = Muted)
                        Button(onClick = {
                            position = 0
                            selectedOption = -1
                            marks = 0
                        }) { Text("Start again") }
                    }
                }
            }
        } else {
            val q = corpus.questions[position]
            item {
                Text("Question " + (position + 1) + "/" + corpus.questions.size +
                    " · Score " + marks, color = Amber, fontSize = 13.sp)
                Text(q.topic, color = Sea, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(q.question, fontSize = 18.sp, lineHeight = 25.sp,
                    fontWeight = FontWeight.SemiBold)
            }
            itemsIndexed(q.options) { index, option ->
                val correct = selectedOption >= 0 && index == q.correct
                val wrong = selectedOption == index && index != q.correct
                OutlinedCard(
                    onClick = {
                        if (selectedOption == -1) {
                            selectedOption = index
                            if (index == q.correct) marks++
                        }
                    },
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = when {
                            correct -> Color(0xFF173E33)
                            wrong -> Color(0xFF472734)
                            else -> Panel
                        }
                    )
                ) {
                    Text((index + 1).toString() + ". " + option,
                        modifier = Modifier.fillMaxWidth().padding(13.dp),
                        fontSize = 14.sp, lineHeight = 20.sp)
                }
            }
            if (selectedOption >= 0) item {
                ElevatedCard {
                    Column(Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (selectedOption == q.correct) "Correct" else "Review the answer",
                            color = if (selectedOption == q.correct) Sea else Amber,
                            fontWeight = FontWeight.Bold)
                        Text(q.explanation, fontSize = 13.sp, lineHeight = 20.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                position = advanceAssessment(position, corpus.questions.size)
                                selectedOption = -1
                            }) {
                                Text(if (position == corpus.questions.lastIndex) "Finish" else "Next")
                            }
                            TextButton(onClick = {
                                position = 0
                                selectedOption = -1
                                marks = 0
                            }) { Text("Restart") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlossaryScreen(
    corpus: FrogCorpus,
    showTamil: Boolean,
    modifier: Modifier = Modifier,
    onOpen: (Organ) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, corpus) {
        corpus.organs.values.filter {
            query.isBlank() || (it.name + it.tamil + it.latin + it.system)
                .contains(query.trim(), ignoreCase = true)
        }
    }
    LazyColumn(
        modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Anatomical glossary", fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Text(corpus.organs.size.toString() + " structures · English and Tamil",
                fontSize = 12.sp, color = Muted)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(), label = { Text("Search anatomy") },
                singleLine = true
            )
        }
        items(filtered, key = { it.key }) { organ ->
            OutlinedCard(onClick = { onOpen(organ) }) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text(organ.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    if (showTamil) Text(organ.tamil, color = Amber, fontSize = 13.sp)
                    Text(organ.latin + " · Layer " + organ.layer, color = Muted, fontSize = 12.sp)
                    Text(organ.morphology, fontSize = 12.sp,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
