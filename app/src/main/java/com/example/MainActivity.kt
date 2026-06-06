package com.example

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigInteger

// ViewModel managing Spectral Matrices Factorization state
class FactorizerViewModel : ViewModel() {
    var inputNumber by mutableStateOf("114837291011")
    var customPrimesText by mutableStateOf("19, 113, 239")
    var isAnalyzing by mutableStateOf(false)
    var spectralResult by mutableStateOf<SpectralResult?>(null)
    var activeTab by mutableStateOf("analyze") // analyze, matrices, history, info
    
    // History log of analyzed values N and their discovered factors
    val historyLog = mutableStateListOf<Pair<String, List<BigInteger>>>()

    fun analyze(context: Context, isArabic: Boolean, silent: Boolean = false) {
        val nStr = inputNumber.trim().filter { it.isDigit() }
        if (nStr.isEmpty()) {
            if (!silent) {
                val msg = if (isArabic) "يرجى إدخال عدد صحيح موجب" else "Please enter a valid positive number"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
            return
        }

        val n = try {
            BigInteger(nStr)
        } catch (e: Exception) {
            if (!silent) {
                val msg = if (isArabic) "العدد غير صالح" else "Invalid number"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
            return
        }

        if (n <= BigInteger.ONE) {
            if (!silent) {
                val msg = if (isArabic) "يرجى إدخال عدد أكبر من 1" else "Please enter a number greater than 1"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
            return
        }

        isAnalyzing = true
        // Launch mathematical factorization in background thread
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.Default) {
            val customPrimes = try {
                customPrimesText.split(",")
                    .map { it.trim().filter { c -> c.isDigit() } }
                    .filter { it.isNotEmpty() }
                    .map { it.toBigInteger() }
                    .filter { it > BigInteger.ONE }
            } catch (e: Exception) {
                emptyList()
            }

            val res = MatrixHelper.factorizeSpectral(n, emptyMap(), customPrimes)
            
            withContext(Dispatchers.Main) {
                spectralResult = res
                isAnalyzing = false
                
                // Add to history if not duplicate
                if (historyLog.none { it.first == nStr }) {
                    historyLog.add(0, Pair(nStr, res.factors))
                }
                
                if (!silent) {
                    val msg = if (isArabic) "اكتمل التحليل المصفوفي الطيفي!" else "Spectral matrix analysis completed!"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isArabic by rememberSaveable { mutableStateOf(true) }

            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFC084FC),
                    secondary = Color(0xFFF59E0B),
                    background = Color(0xFF0F172A),
                    surface = Color(0x13FFFFFF)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F172A)
                ) {
                    val fViewModel: FactorizerViewModel = viewModel()
                    MainLayout(
                        viewModel = fViewModel,
                        isArabic = isArabic,
                        onLanguageToggle = { isArabic = !isArabic }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayout(
    viewModel: FactorizerViewModel,
    isArabic: Boolean,
    onLanguageToggle: () -> Unit
) {
    val activeTab = viewModel.activeTab

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isArabic) "محلل العوامل المصفوفي الطيفي" else "Spectral Matrix Factorizer",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                actions = {
                    IconButton(onClick = onLanguageToggle) {
                        Text(
                            text = if (isArabic) "EN" else "عربي",
                            color = Color(0xFFC084FC),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A),
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            GlassBottomNav(
                activeTab = activeTab,
                onTabSelected = { viewModel.activeTab = it },
                isArabic = isArabic
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0F172A), Color(0xFF080D1A))
                    )
                )
        ) {
            // Main tab layouts with footer embedded to ensure visibility on all paths
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (activeTab) {
                    "analyze" -> AnalyzeTab(viewModel, isArabic)
                    "matrices" -> MatricesTab(isArabic)
                    "history" -> HistoryTab(viewModel, isArabic)
                    "info" -> InformationTab(isArabic)
                }
            }

            // Dr. Saoudi Mohamed Copyright Footer (Mandatory across all tabs)
            FooterLayout(isArabic)
        }
    }
}

@Composable
fun AnalyzeTab(viewModel: FactorizerViewModel, isArabic: Boolean) {
    val context = LocalContext.current
    val result = viewModel.spectralResult
    var showAllDets by remember { mutableStateOf(false) }

    val quickExamples = listOf("19", "114", "6236", "114837291011", "9023476901")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 24.dp, top = 8.dp)
    ) {
        // Welcome and Input Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x13FFFFFF)),
                border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isArabic) "تحليل العوامل الطيفي للعدد N" else "Spectral Factorization of Number N",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isArabic) 
                            "يقوم هذا المحلل برسم مصفوفة الأبعاد الـ 114 للقرآن الكريم ويبحث عن العوامل المشتركة بالاعتماد على المحددات والقيم الطيفية."
                            else "This analyzer constructs the 114 Quranic matrices and computes determinants, eigenvalues, and rank drop projection to find factors.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // TextField N Input
                    OutlinedTextField(
                        value = viewModel.inputNumber,
                        onValueChange = { viewModel.inputNumber = it.filter { c -> c.isDigit() } },
                        label = { Text(if (isArabic) "أدخل العدد N المراد تحليله" else "Enter Number N") },
                        placeholder = { Text("e.g. 114837291011") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFFA5B4FC),
                            focusedBorderColor = Color(0xFFC084FC),
                            unfocusedBorderColor = Color(0x33FFFFFF)
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Examples Flow
                    Text(
                        text = if (isArabic) "أمثلة سريعة للأعداد N:" else "Quick N Examples:",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickExamples.forEach { ex ->
                            Box(
                                modifier = Modifier
                                    .background(Color(0x12FFFFFF), RoundedCornerShape(8.dp))
                                    .clickable {
                                        viewModel.inputNumber = ex
                                        viewModel.analyze(context, isArabic)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = ex,
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Factorization Action Trigger Button
                    Button(
                        onClick = { viewModel.analyze(context, isArabic) },
                        enabled = !viewModel.isAnalyzing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFC084FC),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (viewModel.isAnalyzing) {
                            CircularProgressIndicator(
                                color = Color.Black,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                if (isArabic) "جاري الاحتساب وحل المعادلات..." else "Calculating & Reducing Matrices...",
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Run", tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (isArabic) "بدء التحليل الطيفي والمحددات" else "Execute Spectral Determinants Analysis",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Display results if analysis is available
        if (result != null) {
            // Factor Output Card with Copy prime factors button
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x1A8B5CF6)),
                    border = BorderStroke(1.5.dp, Color(0x40C084FC)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isArabic) "نتائج تحليل العوامل المفككة" else "Factorization Decomposition Results",
                                color = Color(0xFFE9D5FF),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified",
                                tint = Color(0xFFC084FC)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        // Prime Factors List Display
                        val factorsText = if (result.factors.isEmpty()) {
                            if (isArabic) "لا توجد عوامل أولية غير بديهية مكشوفة تحت هذا النطاق" 
                            else "No non-trivial prime factors discovered under this scope"
                        } else {
                            result.factors.joinToString(" × ")
                        }

                        Text(
                            text = if (isArabic) "العوامل الأولية المترشفة لـ N:" else "Discovered Prime Factors of N:",
                            color = Color(0xFFC7D2FE),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x19000000), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = factorsText,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Copy Prime Factors Button (User Request 1)
                        val clipboardManager = LocalClipboardManager.current
                        Button(
                            onClick = {
                                if (result.factors.isNotEmpty()) {
                                    val textToCopy = result.factors.joinToString(", ")
                                    clipboardManager.setText(AnnotatedString(textToCopy))
                                    Toast.makeText(
                                        context,
                                        if (isArabic) "تم نسخ العوامل الأولية: $textToCopy" else "Prime factors copied: $textToCopy",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        if (isArabic) "لا توجد عوامل لنسخها" else "No factors to copy",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFC084FC),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isArabic) "نسخ العوامل الأولية" else "Copy Prime Factors",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Advanced Factoring Algorithm Step-by-Step Tracker (User Request 3)
            item {
                AdvancedFactoringTracker(
                    nStr = viewModel.inputNumber,
                    deltaN = result.deltaN,
                    gResult = result.advancedFactor,
                    isArabic = isArabic
                )
            }

            // Spectral Fingerprint Φ(N) Visualizer (User Request 2)
            item {
                SpectralFingerprintChart(
                    fingerprint = result.spectralFingerprint,
                    isArabic = isArabic
                )
            }

            // Modular Rank Reduction Test for Primality and Factorization (User Request 1)
            item {
                RankReductionTestTracker(
                    results = result.rankReductionResults,
                    isArabic = isArabic
                )
            }

            // Individual Surah Determinants D_s(N) Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x13FFFFFF)),
                    border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isArabic) "محددات السور الفردية D_s(N)" else "Individual Surah Determinants D_s(N)",
                                color = Color(0xFFC7D2FE),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = { showAllDets = !showAllDets },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0x1F94A3B8),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (showAllDets) {
                                        if (isArabic) "إخفاء التفاصيل" else "HIDE"
                                    } else {
                                        if (isArabic) "عرض الكل (114)" else "SHOW ALL (114)"
                                    },
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (showAllDets) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                                    .verticalScroll(rememberScrollState())
                                    .background(Color(0x0FFFFFFF), RoundedCornerShape(12.dp))
                                    .border(BorderStroke(1.dp, Color(0x12FFFFFF)), RoundedCornerShape(12.dp))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                result.individualDets.forEachIndexed { index, d_s ->
                                    val surahNum = index + 1
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0x0AFFFFFF), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Surah $surahNum (D_$surahNum)",
                                            color = Color(0xFFA5B4FC),
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        val dText = d_s.toString()
                                        val displayedText = if (dText.length > 20) {
                                            dText.take(8) + "..." + dText.takeLast(8) + " (${dText.length} d)"
                                        } else {
                                            dText
                                        }
                                        Text(
                                            text = displayedText,
                                            color = Color(0xFFE2E8F0),
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            overflow = TextOverflow.Ellipsis,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatricesTab(isArabic: Boolean) {
    var selectedSurah by remember { mutableStateOf<Int?>(null) }
    
    if (selectedSurah == null) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (isArabic) "مستكشف مصفوفات السور الـ 114" else "The 114 Quranic Matrices Explorer",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isArabic) 
                    "انقر على أي سورة لاستكشاف مصفوفتها الطيفية ذات الحجم 28x28 والمنشأة بناءً على معاييرها الحسابية وعدد آياتها."
                    else "Click any Surah to inspect its generated 28x28 symmetric matrix based on historical verses count.",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Grid of 114 Surahs
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 74.dp),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items((1..114).toList()) { s ->
                    val verses = MatrixHelper.SURAH_VERSES[s - 1]
                    Card(
                        onClick = { selectedSurah = s },
                        colors = CardDefaults.cardColors(containerColor = Color(0x0FFFFFFF)),
                        border = BorderStroke(1.dp, Color(0x1AFFFFFF)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$s",
                                color = Color(0xFFC084FC),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isArabic) "$verses آية" else "$verses Verses",
                                color = Color(0xFFCBD5E1),
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }
    } else {
        val s = selectedSurah!!
        val m_s = MatrixHelper.generateQuranicMatrix(s)
        val verses = MatrixHelper.SURAH_VERSES[s - 1]

        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedSurah = null }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFFC084FC))
                }
                Text(
                    text = (if (isArabic) "مصفوفة السورة " else "Matrix for Surah ") + s,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
                Text(
                    text = if (isArabic) "$verses آية" else "$verses Ayat",
                    color = Color(0xFFF59E0B),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Draw interactive 28x28 matrix viewport
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x19000000)),
                border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val stateVertical = rememberScrollState()
                    val stateHorizontal = rememberScrollState()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(stateVertical)
                            .horizontalScroll(stateHorizontal)
                            .padding(12.dp)
                    ) {
                        for (i in 0 until 28) {
                            Row {
                                for (j in 0 until 28) {
                                    val valCell = m_s[i][j]
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(
                                                if (i == j) Color(0x33C084FC) 
                                                else if (valCell > 0) Color(0x1AF59E0B) 
                                                else Color.Transparent
                                            )
                                            .border(0.5.dp, Color(0x12FFFFFF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$valCell",
                                            fontSize = 9.sp,
                                            color = if (i == j) Color(0xFFE9D5FF) else if (valCell > 0) Color(0xFFFDE68A) else Color(0xFF64748B),
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryTab(viewModel: FactorizerViewModel, isArabic: Boolean) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = if (isArabic) "سجل العمليات السابقة" else "Calculations History Log",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isArabic) 
                "الأعداد المدخلة سابقاً وحصيلتها من العوامل الطيفية الميكرومكتشفة."
                else "Locally cached analysis runs with their respective primary factors.",
            color = Color(0xFF94A3B8),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (viewModel.historyLog.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isArabic) "السجل فارغ حالياً" else "No calculations run yet.",
                    color = Color(0xFF64748B),
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(viewModel.historyLog) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x0FFFFFFF)),
                        border = BorderStroke(1.dp, Color(0x1DFFFFFF)),
                        shape = RoundedCornerShape(12.dp),
                        onClick = {
                            viewModel.inputNumber = item.first
                            viewModel.activeTab = "analyze"
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "N = ${item.first}",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isArabic) "انقر لإعادة التحميل" else "Click to re-analyze",
                                    color = Color(0xFFC084FC),
                                    fontSize = 11.sp
                                )
                            }
                            
                            val facs = item.second
                            val facsStr = if (facs.isEmpty()) "Prime / Prime candidates" else facs.joinToString(", ")
                            Text(
                                text = facsStr,
                                color = Color(0xFF34D399),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InformationTab(isArabic: Boolean) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = if (isArabic) "معلومات نظرية ومكتشفات الدكتور سعودي" else "Theoretical Context & Discoveries of Dr. Saoudi",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isArabic)
                    "يمزج التطبيق بين علوم الجبر الخطي وتناسق حساب القرآن الكريم لدراسة صفة العوامل الطيفية."
                    else "Mixing clean linear algebra with Quranic numerical systems backends for factor analyses.",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x0FFFFFFF)),
                border = BorderStroke(1.dp, Color(0x15FFFFFF)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isArabic) "نموذج المصفوفة المصاحبة A(N)" else "The Combined Matrix A(N)",
                        color = Color(0xFFC084FC),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isArabic)
                            "يجمع طاقة بواقي القسمة (N mod s) للأبعاد 114 سورة عبر ضربها في مصفوفات السور M_s. تعتمد مصفوفات الاستنساخ 28x28 على عدد آيات السورة لفرض اتساق عددي متين ومحدد طيفي دقيق."
                            else "Calculates the dynamic sum A(N) = sum_{s=1}^{114} (N mod s) * M_s. The 28x28 symmetric matrix of each Surah derives elements from historical verses count for rigorous mathematical evaluation.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x0FFFFFFF)),
                border = BorderStroke(1.dp, Color(0x15FFFFFF)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isArabic) "محددات الفروقات D_s(N) والقواسم" else "D_s(N) Determinants & gcd(G, N)",
                        color = Color(0xFFF59E0B),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isArabic)
                            "إن حساب D_s(N) = det(M_s - N * I) لكل سورة من سور القرآن الـ 114 يوفر محددات جبارة. القاسم المشترك الأكبر G(N) لمحددات كافة السور يكشف العوامل الأولية للعدد N غير المعروفة سابقاً بشكل مباشر."
                            else "Calculates D_s(N) = det(M_s - N*I) for each of the 114 matrices. G(N) = gcd(D_1,...,D_114) then acts as a potential identifier of n-factors when intersecting with N.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun GlassBottomNav(
    activeTab: String,
    onTabSelected: (String) -> Unit,
    isArabic: Boolean
) {
    NavigationBar(
        containerColor = Color(0xFF0F172A),
        tonalElevation = 8.dp,
        modifier = Modifier.drawBehind {
            drawLine(
                color = Color(0x15FFFFFF),
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f),
                strokeWidth = 1.dp.toPx()
            )
        }
    ) {
        NavigationBarItem(
            selected = activeTab == "analyze",
            onClick = { onTabSelected("analyze") },
            icon = { Icon(Icons.Outlined.Calculate, contentDescription = "Analyze") },
            label = { Text(if (isArabic) "تحليل" else "Analyze", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFFC084FC),
                selectedTextColor = Color(0xFFC084FC),
                unselectedIconColor = Color(0xFF64748B),
                unselectedTextColor = Color(0xFF64748B),
                indicatorColor = Color(0x22C084FC)
            )
        )
        NavigationBarItem(
            selected = activeTab == "matrices",
            onClick = { onTabSelected("matrices") },
            icon = { Icon(Icons.Outlined.GridOn, contentDescription = "Matrices") },
            label = { Text(if (isArabic) "مصفوفات" else "Matrices", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFFC084FC),
                selectedTextColor = Color(0xFFC084FC),
                unselectedIconColor = Color(0xFF64748B),
                unselectedTextColor = Color(0xFF64748B),
                indicatorColor = Color(0x22C084FC)
            )
        )
        NavigationBarItem(
            selected = activeTab == "history",
            onClick = { onTabSelected("history") },
            icon = { Icon(Icons.Outlined.History, contentDescription = "History") },
            label = { Text(if (isArabic) "السجل" else "History", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFFC084FC),
                selectedTextColor = Color(0xFFC084FC),
                unselectedIconColor = Color(0xFF64748B),
                unselectedTextColor = Color(0xFF64748B),
                indicatorColor = Color(0x22C084FC)
            )
        )
        NavigationBarItem(
            selected = activeTab == "info",
            onClick = { onTabSelected("info") },
            icon = { Icon(Icons.Outlined.Book, contentDescription = "Info") },
            label = { Text(if (isArabic) "فكرة" else "Theory", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFFC084FC),
                selectedTextColor = Color(0xFFC084FC),
                unselectedIconColor = Color(0xFF64748B),
                unselectedTextColor = Color(0xFF64748B),
                indicatorColor = Color(0x22C084FC)
            )
        )
    }
}

@Composable
fun FooterLayout(isArabic: Boolean) {
    // Dr. Saoudi Mohamed Copyright Footer (Mandatory - User Request 3)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0A0F1D))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isArabic) 
                    "جميع الحقوق محفوظة للدكتور سعودي محمد ©" 
                    else "All rights reserved to Dr. Saoudi Mohamed ©",
                color = Color(0xFFCBD5E1),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (isArabic) 
                    "الهاتف: 00213657348908" 
                    else "Phone: 00213657348908",
                color = Color(0xFFF59E0B),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SpectralFingerprintChart(
    fingerprint: List<Double>,
    isArabic: Boolean
) {
    if (fingerprint.isEmpty()) return

    val maxVal = fingerprint.maxOrNull() ?: 1.0
    val minVal = fingerprint.minOrNull() ?: 0.0
    val range = if (maxVal == minVal) 1.0 else (maxVal - minVal)

    var hoveredIndex by remember { mutableStateOf<Int?>(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x0AFFFFFF), RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, Color(0x15FFFFFF)), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isArabic) "البصمة الطيفية للقرآن الكريم Φ(N)" else "Quranic Spectral Fingerprint Φ(N)",
                color = Color(0xFFA5B4FC),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "114 Surahs",
                color = Color(0xFF64748B),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0x0FFFFFFF), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Column {
                    Text(
                        text = if (isArabic) "العظمى (Max)" else "Peak Peak (Max)",
                        color = Color(0xFF94A3B8),
                        fontSize = 9.sp
                    )
                    Text(
                        text = "%.3f".format(maxVal),
                        color = Color(0xFF34D399),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0x0FFFFFFF), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Column {
                    Text(
                        text = if (isArabic) "الصغرى (Min)" else "Base Base (Min)",
                        color = Color(0xFF94A3B8),
                        fontSize = 9.sp
                    )
                    Text(
                        text = "%.3f".format(minVal),
                        color = Color(0xFFF59E0B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .background(Color(0xFF070B14), RoundedCornerShape(8.dp))
                .border(BorderStroke(1.dp, Color(0x12FFFFFF)), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                fingerprint.forEachIndexed { sIndex, value ->
                    val heightFactor = (((value - minVal) / range).toFloat()).coerceIn(0.12f, 1.0f)
                    val isHovered = hoveredIndex == sIndex

                    Box(
                        modifier = Modifier
                            .width(8.dp)
                            .fillMaxHeight(heightFactor)
                            .background(
                                Brush.verticalGradient(
                                    colors = if (isHovered) listOf(Color(0xFFFBBF24), Color(0xFFF59E0B))
                                    else listOf(Color(0xFFC084FC), Color(0xFF6366F1))
                                ),
                                RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                            )
                            .clickable { hoveredIndex = sIndex }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val activeIndex = hoveredIndex ?: 0
        val activeVal = fingerprint.getOrElse(activeIndex) { 0.0 }
        val verses = if (activeIndex in 0..113) MatrixHelper.SURAH_VERSES[activeIndex] else 0
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x0FFFFFFF), RoundedCornerShape(8.dp))
                .padding(vertical = 6.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isArabic) "سورة ${activeIndex + 1} ($verses آية)" else "Surah ${activeIndex + 1} ($verses Verses)",
                color = Color(0xFFE2E8F0),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "E_${activeIndex + 1} = %.4f".format(activeVal),
                color = Color(0xFFA5B4FC),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun AdvancedFactoringTracker(
    nStr: String,
    deltaN: BigInteger,
    gResult: BigInteger,
    isArabic: Boolean
) {
    val n = try { BigInteger(nStr.trim().filter { it.isDigit() }) } catch(e: Exception) { BigInteger.ZERO }
    val isFactorSuccessful = gResult > BigInteger.ONE && gResult < n

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x0AFFFFFF), RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, Color(0x15FFFFFF)), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isArabic) "خوارزمية التحليل المتقدم للعامليات" else "Advanced Factoring Algorithm",
                color = Color(0xFFF59E0B),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .background(Color(0x1AA7F3D0), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "A(N) Model",
                    color = Color(0xFF34D399),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isArabic) "بناء مصفوفة A(N) بالمعادلة أدناه وحساب المحدد ثم إيجاد القاسم المشترك الأكبر."
                   else "Constructing A(N) sum, computing Delta(N) Bareiss determinant, and gcd intersection.",
            color = Color(0xFF94A3B8),
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x08FFFFFF), RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Column {
                Text(
                    text = if (isArabic) "1. بناء المصفوفة التجميعية A(N):" else "1. Construct Matrix A(N):",
                    color = Color(0xFFC7D2FE),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "A(N) = \u2211_{s=1}^{114} (N mod s) \u00d7 M_s",
                    color = Color(0xFFF59E0B),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x08FFFFFF), RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Column {
                Text(
                    text = if (isArabic) "2. محدد المصفوفة التجميعية \u0394(N):" else "2. Determinant calculation \u0394(N) = det(A(N)):",
                    color = Color(0xFFC7D2FE),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                val detStr = deltaN.toString()
                val truncatedDet = if (detStr.length > 24) {
                    detStr.take(12) + "..." + detStr.takeLast(12) + " (${detStr.length} digits)"
                } else {
                    detStr
                }
                Text(
                    text = "\u0394(N) = $truncatedDet",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x08FFFFFF), RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Column {
                Text(
                    text = if (isArabic) "3. القاسم المشترك الأكبر للربط الحسابي:" else "3. Greatest Common Divisor g = gcd(\u0394(N), N):",
                    color = Color(0xFFC7D2FE),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "g = gcd(\u0394(N), N) = $gResult",
                    color = if (isFactorSuccessful) Color(0xFF34D399) else Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        if (isFactorSuccessful) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x1A34D399), RoundedCornerShape(8.dp))
                    .border(BorderStroke(1.dp, Color(0x3334D399)), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = Color(0xFF34D399),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isArabic) "نجاح مذهل: اكتشاف القاسم المشترك g = $gResult كأحد عوامل N غير البديهية!"
                               else "Successful factorization! Found non-trivial factor g = $gResult!",
                        color = Color(0xFF34D399),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x0AFFFFFF), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isArabic) "القاسم g لا يعطي عاملاً غير بديهي (g=1 أو N). هذا يعد مؤشراً قوياً على أن N عدد أولي أو مرشح أولي متماسك."
                               else "g is trivial (1 or N) indicating that N behaves as a robust prime candidate.",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun RankReductionTestTracker(
    results: List<RankReductionTestResult>,
    isArabic: Boolean
) {
    if (results.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x0AFFFFFF), RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, Color(0x15FFFFFF)), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(
            text = if (isArabic) "اختبار تناقص الرتب المصفوفي للأعداد الأولية" else "Modular Rank Reduction Primality Test",
            color = Color(0xFFC084FC),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isArabic) "دراسة تناقص رتبة مصفوفات سور القرآن modulo p. تناقص الرتبة لـ p القاسم يؤكد كونه عاملاً لـ N."
                   else "Investigates matrix rank drops. A drop below 28 when p divides N indicates key mathematical alignments.",
            color = Color(0xFF94A3B8),
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            results.forEach { res ->
                val cardColor = if (res.isFlagged) Color(0x1D34D399) else Color(0x07FFFFFF)
                val borderColor = if (res.isFlagged) Color(0x5534D399) else Color(0x10FFFFFF)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cardColor, RoundedCornerShape(8.dp))
                        .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Prime p = ${res.prime}",
                                color = if (res.isFlagged) Color(0xFF34D399) else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            if (res.dividesN) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF34D399), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isArabic) "يقسم N" else "Divides N",
                                        color = Color.Black,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0x14FFFFFF), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isArabic) "لا يقسم N" else "Does not divide N",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isArabic) "متوسط معامل الرتبة mod p:" else "Average Matrix Rank mod p:",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                            Text(
                                text = "%.4f / 28".format(res.averageRank),
                                color = if (res.averageRank < 27.8) Color(0xFFFBBF24) else Color.White,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isArabic) "مصفوفات شهدت تناقصاً بالأبعاد (< 28):" else "Matrices with rank drop (< 28):",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                            Text(
                                text = "${res.rankDropsCount} / 114",
                                color = if (res.rankDropsCount > 0) Color(0xFFFBBF24) else Color.White,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (res.isFlagged) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isArabic) "✨ انخفاض الرتبة يؤكد أن العامل p عامل مفكك حقيقي لـ N طيفياً!"
                                       else "✨ Significant rank drop matches factor p dividing N!",
                                color = Color(0xFF34D399),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
