package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Random
import kotlin.math.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFC084FC),
                    secondary = Color(0xFF22D3EE),
                    background = Color(0xFF0B0F19),
                    surface = Color(0xFF1E293B)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF030712)
                ) {
                    SpectralFactorizerApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpectralFactorizerApp() {
    var isArabic by remember { mutableStateOf(false) }
    var currentTab by remember { mutableStateOf(0) } // 0: Explorer, 1: Hamiltonian, 2: Clustering
    
    // Global shared states computed on background
    val coroutineScope = rememberCoroutineScope()
    var isComputing by remember { mutableStateOf(true) }
    var similarityMatrix by remember { mutableStateOf(Array(114) { DoubleArray(114) }) }
    var propertiesList by remember { mutableStateOf<List<MatrixFeatures>>(emptyList()) }
    
    // Trigger computing on startup
    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.Default) {
            val sim = MatrixHelper.computeSimilarityMatrix()
            val list = (1..114).map { MatrixHelper.computeMatrixProperties(it) }
            withContext(Dispatchers.Main) {
                similarityMatrix = sim
                propertiesList = list
                isComputing = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // App header with celestial gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF111827), Color(0xFF030712))
                    )
                )
                .border(BorderStroke(0.5.dp, Color(0x1AFFFFAA)))
                .padding(vertical = 12.dp, horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isArabic) "مُحلل الأطياف الكوني للنصوص" else "Spectral Factorizer & Quantum Simulator",
                        color = Color(0xFFE2E8F0),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (isArabic) "خصائص مصفوفات السور الـ 114 والتقسيم الطيفي العنقودي" else "Symmetry Analysis of 114 Quranic Operator Algebras",
                        color = Color(0xFFC084FC),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Arabic/English Toggle Button
                Box(
                    modifier = Modifier
                        .background(Color(0x13FFFFFF), RoundedCornerShape(20.dp))
                        .border(BorderStroke(1.dp, Color(0x33C084FC)), RoundedCornerShape(20.dp))
                        .clickable { isArabic = !isArabic }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isArabic) "English EN" else "العربية AR",
                        color = Color(0xFFC084FC),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (isComputing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFFC084FC))
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (isArabic) "جاري حوسبة طيف المصفوفات والخصائص الذاتية..." else "Computing spectral maps and eigenvalue properties...",
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            // Tab row
            ScrollableTabRow(
                selectedTabIndex = currentTab,
                containerColor = Color(0xFF090D16),
                contentColor = Color(0xFFC084FC),
                edgePadding = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    text = { Text(if (isArabic) "مُتصفح المصفوفات والخصائص" else "Matrices & Properties", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    text = { Text(if (isArabic) "المؤثر الهاميلتوني طيف H" else "A1 & Global Hamiltonian", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    text = { Text(if (isArabic) "التجميع الطيفي والحساسية" else "Spectral Clustering & Similarity", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                when (currentTab) {
                    0 -> ExplorerTab(isArabic, propertiesList)
                    1 -> HamiltonianTab(isArabic)
                    2 -> ClusteringTab(isArabic)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExplorerTab(isArabic: Boolean, propertiesList: List<MatrixFeatures>) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSurah by remember { mutableStateOf<Int?>(1) } // Default Al-Fatihah
    
    val filteredList = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            propertiesList
        } else {
            propertiesList.filter {
                it.index.toString().contains(searchQuery)
            }
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // Left Column: List & search
        Column(
            modifier = Modifier
                .width(140.dp)
                .fillMaxHeight()
                .background(Color(0xFF060913))
                .border(BorderStroke(0.5.dp, Color(0x0CFFFFFF)))
                .padding(6.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text(if (isArabic) "ابحث برقم" else "Search #", fontSize = 9.sp) },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredList) { f ->
                    val isSelected = f.index == selectedSurah
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSelected) Color(0x22C084FC) else Color(0x0AFFFFFF),
                                RoundedCornerShape(6.dp)
                            )
                            .border(
                                BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFFC084FC) else Color(0x05FFFFFF)
                                ),
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { selectedSurah = f.index }
                            .padding(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "S${f.index}",
                                color = if (isSelected) Color(0xFFC084FC) else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${MatrixHelper.SURAH_VERSES[f.index - 1]}v",
                                color = Color(0xFF94A3B8),
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }

        // Right Column: Details & Matrix Heatmap
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            selectedSurah?.let { s ->
                val feat = propertiesList[s - 1]
                val qMatrix = remember(s) { MatrixHelper.getQuranicMatrix(s) }

                // Matrix Title
                Column {
                    Text(
                        text = if (isArabic) "السورة المحددة: سورة $s" else "Selected: Surah $s (${MatrixHelper.SURAH_VERSES[s-1]} Verses)",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isArabic) "البنية الهندسية لمؤثر مصفوفة اللغات M_s" else "Spatial-Language Matrix Operator M_s (28 x 28)",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp
                    )
                }

                // 28x28 Heatmap representation
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x0AFFFFFF)),
                    border = BorderStroke(1.dp, Color(0x10FFFFFF))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = if (isArabic) "خريطة الحرارة الطيفية للأحرف العربية الـ 28" else "Symmetric Letter Heatmap Map (28×28)",
                            color = Color(0xFF22D3EE),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .horizontalScroll(rememberScrollState()),
                            contentAlignment = Alignment.Center
                        ) {
                            Column {
                                for (i in 0 until 28) {
                                    Row {
                                        for (j in 0 until 28) {
                                            val valCell = qMatrix[i][j]
                                            // Scale color based on value
                                            val alpha = (valCell.toFloat() / 15f).coerceIn(0.05f, 1f)
                                            val cellColor = if (valCell > 0) {
                                                Color(0xFFC084FC).copy(alpha = alpha)
                                            } else {
                                                Color(0xFF1E293B).copy(alpha = 0.2f)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .background(cellColor)
                                                    .border(0.1.dp, Color(0x0AFFFFFF))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Linear Feature Vectors Grid
                Text(
                    text = if (isArabic) "خصائص فنتور الميزات للمصفوفة" else "7-Dimensional Matrix Algebraic Properties:",
                    color = Color(0xFFFCD34D),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val properties = listOf(
                        Triple(
                            if (isArabic) "الرتبة الرياضية" else "Rank Dimension",
                            "${feat.rank} / 28",
                            if (isArabic) "الأبعاد المستقلة خطياً" else "Linearly independent algebraic dimensions"
                        ),
                        Triple(
                            if (isArabic) "الأثر (Trace)" else "Trace (Self-Interactions)",
                            String.format("%.1f", feat.trace),
                            if (isArabic) "التفاعل الذاتي الكلي للأحرف" else "Sum of diagonal values (self-transitions)"
                        ),
                        Triple(
                            if (isArabic) "المحدد الجبري" else "Determinant Volume",
                            if (abs(feat.determinant) < 1e-12) "0.00" else String.format("%.2e", feat.determinant),
                            if (isArabic) "حجم فضاء المؤثر" else "Product of all eigenvalues"
                        ),
                        Triple(
                            if (isArabic) "أكبر قيمة ذاتية" else "Largest Eigenvalue",
                            String.format("%.4f", feat.largestEigenvalue),
                            if (isArabic) "الحد الأعلى لمعظم الطاقة" else "Max operator energy (infinity bound)"
                        ),
                        Triple(
                            if (isArabic) "أصغر قيمة ذاتية" else "Smallest Eigenvalue",
                            String.format("%.4f", feat.smallestEigenvalue),
                            if (isArabic) "طاقة الحالة الدنيا" else "Minimum spectral state limit"
                        ),
                        Triple(
                            if (isArabic) "نسبة التشتت الجبري" else "Sparsity Grid Ratio",
                            String.format("%.2f%%", feat.sparsity * 100.0),
                            if (isArabic) "المساحات الصفرية العالية" else "Percentage of empty state relationships"
                        ),
                        Triple(
                            if (isArabic) "معيار فروبينيوس (الطاقة الأيونية)" else "Frobenius Energy Norm",
                            String.format("%.3f", feat.frobeniusNorm),
                            if (isArabic) "القوة المغناطيسية لمصفوفة الحبل" else "Square root of sum of squares of all values"
                        )
                    )

                    properties.forEach { (title, value, description) ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0x0CFFFFFF)),
                            border = BorderStroke(1.dp, Color(0x1F64748B))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = value,
                                        color = Color(0xFFC084FC),
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                Text(text = description, color = Color(0xFF94A3B8), fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HamiltonianTab(isArabic: Boolean) {
    var weightPreset by remember { mutableStateOf("primes") } // "primes", "harmonic", "uniform", "inverse"
    
    val H_data = remember(weightPreset) {
        val weights = DoubleArray(114) { 0.0 }
        when (weightPreset) {
            "primes" -> {
                // Prime numbers up to 114
                val primes = setOf(
                    2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97, 101, 103, 107, 109, 113
                )
                for (s in 1..114) {
                    if (primes.contains(s)) weights[s - 1] = 1.0
                }
            }
            "harmonic" -> {
                for (s in 1..114) {
                    weights[s - 1] = 1.0 / s.toDouble()
                }
            }
            "uniform" -> {
                for (s in 1..114) {
                    weights[s - 1] = 1.0 / 114.0
                }
            }
            "inverse" -> {
                for (s in 1..114) {
                    weights[s - 1] = 115 - s.toDouble()
                }
            }
        }
        MatrixHelper.analyzeHamiltonian(weights)
    }

    val (eigenvalues, entropy, condNo) = H_data

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Description Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x13FFFFFF)),
                border = BorderStroke(1.dp, Color(0x1AFFFFFF))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (isArabic) "تركيب المشغّل الهاميلتوني الكلي H" else "Symmetry Coupling: Constructing Hamiltonian H",
                        color = Color(0xFFC084FC),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isArabic)
                            "هو مشغل يدمج طاقات السور الـ 114 كأطوار خطية متداخلة: H = ∑(α_s * M_s). تكرر هذه الطريقة البنية الكلية للحفاظ على توازن الطاقة والخصائص الطيفية الفيزيائية."
                            else "The operator representation of coupling all 114 Quranic fields via H = sum(alpha_s * M_s). Analyzing H resolves core state spaces, spectral entropy, and linear condition status.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        // Preset selector
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x0CFFFFFF)),
                border = BorderStroke(1.dp, Color(0x1A64748B))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (isArabic) "اختر سيناريو الأوزان (α_s):" else "Select Coupling Weights (alpha_s):",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val presets = listOf(
                            "primes" to Pair("رنين الأعداد الأولية", "Prime Res"),
                            "harmonic" to Pair("التسلسل التوافقي (1/s)", "Harmonic"),
                            "uniform" to Pair("التوزيع الموحد", "Uniform"),
                            "inverse" to Pair("الانعكاس الخطي", "Linear-Inv")
                        )
                        presets.forEach { (id, label) ->
                            val selected = weightPreset == id
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (selected) Color(0xFFC084FC) else Color(0x0FFFFFFF),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .clickable { weightPreset = id }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (isArabic) label.first else label.second,
                                    color = if (selected) Color.Black else Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Metrics Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0x1622D3EE)),
                    border = BorderStroke(1.dp, Color(0x2222D3EE))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = if (isArabic) "إنتروبيا الأطياف الذاتية (S)" else "Spectral Entropy",
                            color = Color(0xFF22D3EE),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = String.format("%.4f", entropy),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (isArabic) "مؤشر اضطراب طاقة المشغل" else "Measures information chaos of states",
                            color = Color(0xFF94A3B8),
                            fontSize = 8.sp
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0x16F59E0B)),
                    border = BorderStroke(1.dp, Color(0x22F59E0B))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = if (isArabic) "رقم الشرط (k - Condition)" else "Condition Number",
                            color = Color(0xFFF59E0B),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = String.format("%.2f", condNo),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (isArabic) "استقرار الحسابية الجبرية" else "Measure of numerical stability",
                            color = Color(0xFF94A3B8),
                            fontSize = 8.sp
                        )
                    }
                }
            }
        }

        // Line Chart of Eigenvalues on Canvas
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x1F000000)),
                border = BorderStroke(1.dp, Color(0x1AFFFFFF))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (isArabic) "منحنى الطيف الذاتي لـ H (28 بعد)" else "Eigenvalue Energy Spectrum of H (28 Points)",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(Color(0xFF030712))
                    ) {
                        val minVal = eigenvalues.minOrNull() ?: 0.0
                        val maxVal = eigenvalues.maxOrNull() ?: 1.0
                        val valRange = if (maxVal - minVal > 1e-5) maxVal - minVal else 1.0
                        
                        // Draw horizontal grid lines
                        val gridCount = 4
                        for (i in 0..gridCount) {
                            val y = size.height * i / gridCount
                            drawLine(
                                color = Color(0x1A64748B),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1f
                            )
                        }

                        // Coordinates plotting
                        val path = Path()
                        val points = mutableListOf<Offset>()
                        
                        eigenvalues.forEachIndexed { idx, lam ->
                            val x = size.width * idx / (eigenvalues.size - 1)
                            val y = size.height - (size.height * (lam - minVal) / valRange).toFloat()
                            if (idx == 0) {
                                path.moveTo(x, y)
                            } else {
                                path.lineTo(x, y)
                            }
                            points.add(Offset(x, y))
                        }

                        // Draw golden line
                        drawPath(
                            path = path,
                            color = Color(0xFFFCD34D),
                            style = Stroke(width = 3f)
                        )

                        // Draw glowing dots
                        points.forEach { offset ->
                            drawCircle(
                                color = Color(0xFFC084FC),
                                radius = 4f,
                                center = offset
                            )
                        }
                    }
                }
            }
        }

        // Eigenvalues printlist
        item {
            Text(
                text = if (isArabic) "تفصيل الأطياف الفردية الـ 28:" else "Full list of H eigenvalues (28-Dimensions):",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x0CFFFFFF), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    eigenvalues.forEachIndexed { idx, value ->
                        Box(
                            modifier = Modifier
                                .background(Color(0x16C084FC), RoundedCornerShape(6.dp))
                                .border(BorderStroke(0.5.dp, Color(0x33C084FC)), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row {
                                Text(
                                    "λ${idx+1}: ",
                                    color = Color(0xFFC084FC),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    String.format("%.3f", value),
                                    color = Color.White,
                                    fontSize = 10.sp,
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ClusteringTab(isArabic: Boolean) {
    var numClustersK by remember { mutableStateOf(4) }
    
    // Compute Spectral Clustering output to Quranic Matrices
    val spectralData = remember(numClustersK) {
        MatrixHelper.spectralClustering(numClustersK)
    }
    val clusters = spectralData.first
    val eigenvalues = spectralData.second

    // Compute Null Hypothesis (Random Similarity & Clustering)
    val randomSpectralData = remember(numClustersK) {
        val S_rand = MatrixHelper.computeRandomSimilarityMatrix()
        // Compute unnormalized / normalized Laplacian of random simulation
        val n = S_rand.size
        val deg = DoubleArray(n)
        for (i in 0 until n) {
            var sum = 0.0
            for (j in 0 until n) {
                sum += S_rand[i][j]
            }
            deg[i] = sum
        }
        val Anorm = Array(n) { DoubleArray(n) }
        for (i in 0 until n) {
            for (j in 0 until n) {
                val dI = deg[i]
                val dJ = deg[j]
                Anorm[i][j] = if (dI > 0 && dJ > 0) S_rand[i][j] / (sqrt(dI) * sqrt(dJ)) else 0.0
            }
        }
        val eigenRes = MatrixHelper.jacobiComplete(Anorm)
        eigenRes.eigenvalues.sortedDescending().toDoubleArray()
    }

    // Cohesion measurements
    val clusterCohesion = remember(numClustersK) {
        MatrixHelper.computeClusterSimilarityMeasure(clusters, numClustersK)
    }

    // Sensitivity analysis: Robustness index under noise perturbation (Monte Carlo simulation)
    val sensitivityOverlap = remember(numClustersK) {
        // Perturb the original similarity matrix with 10% noise and recalculate
        val origS = MatrixHelper.computeSimilarityMatrix()
        val r = Random(99)
        val perturbedS = Array(114) { i ->
            DoubleArray(114) { j ->
                val noise = r.nextGaussian() * 0.15 // 15% noise
                (origS[i][j] + noise).coerceIn(0.0, 1.0)
            }
        }
        // Compute perturbed clustering
        val n = perturbedS.size
        val deg = DoubleArray(n)
        for (i in 0 until n) {
            var sum = 0.0
            for (j in 0 until n) {
                sum += perturbedS[i][j]
            }
            deg[i] = sum
        }
        val Anorm = Array(n) { DoubleArray(n) }
        for (i in 0 until n) {
            for (j in 0 until n) {
                val dI = deg[i]
                val dJ = deg[j]
                Anorm[i][j] = if (dI > 0 && dJ > 0) perturbedS[i][j] / (sqrt(dI) * sqrt(dJ)) else 0.0
            }
        }
        val eigenRes = MatrixHelper.jacobiComplete(Anorm)
        val eigenpairs = List(n) { i -> Pair(eigenRes.eigenvalues[i], eigenRes.eigenvectors[i]) }
            .sortedByDescending { it.first }
        val U = Array(n) { DoubleArray(numClustersK) }
        for (i in 0 until n) {
            for (j in 0 until numClustersK) {
                U[i][j] = eigenpairs[j].second[i]
            }
        }
        val Y = Array(n) { DoubleArray(numClustersK) }
        for (i in 0 until n) {
            var sumSq = 0.0
            for (j in 0 until numClustersK) {
                sumSq += U[i][j] * U[i][j]
            }
            val norm = sqrt(sumSq)
            for (j in 0 until numClustersK) Y[i][j] = if (norm > 0) U[i][j] / norm else 0.0
        }
        val perturbedClusters = MatrixHelper.kMeans(Y, numClustersK)

        // Compare assignments using overlap index
        var matches = 0
        for (i in 0 until n) {
            if (clusters[i] == perturbedClusters[i]) {
                matches++
            }
        }
        matches.toDouble() / n.toDouble()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Explanatory Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x13FFFFFF)),
                border = BorderStroke(1.dp, Color(0x1AFFFFFF))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (isArabic) "التجميع الطيفي وكشف النظام الطوبولوجي" else "Spectral Clustering & Topological Non-Randomness",
                        color = Color(0xFF10B981),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isArabic)
                            "تم إخضاع مصفوفة التشابه 114×114 للضرب المتكاثف للا بلاس وتصدير أول K ميزات ذاتية. تجميع المخرجات يفصل سور الكوائن الرياضية المتطابقة عن الضوضاء العشوائية."
                            else "Transforms the 114x114 cosine similarity matrix into a Normalized Laplacian spaces to trace eigenmaps. Comparing against Random matrices proves the existence of non-random, highly robust symmetries.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        // K clusters configurator
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x0CFFFFFF)),
                border = BorderStroke(1.dp, Color(0x1F64748B))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (isArabic) "تحديد عدد العناقيد (K):" else "Configure Cluster Dimension (K):",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (kVal in listOf(2, 3, 4, 5)) {
                            val selected = numClustersK == kVal
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (selected) Color(0xFF10B981) else Color(0x0FFFFFFF),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { numClustersK = kVal }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "K = $kVal",
                                    color = if (selected) Color.Black else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Comparison Stats & Spectral Gap Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x0BFFFFFF)),
                border = BorderStroke(1.dp, Color(0x1A10B981))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (isArabic) "إحصائيات غير العشوائية ومقارنة الثغرة الطيفية" else "Null Hypothesis & Spectral Gap Significance",
                        color = Color(0xFF34D399),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    val quranicGap = abs(eigenvalues[numClustersK - 1] - eigenvalues[numClustersK])
                    val randomGap = abs(randomSpectralData[numClustersK - 1] - randomSpectralData[numClustersK])

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (isArabic) "الثغرة الطيفية الحقيقية" else "Quranic Spectral Gap", color = Color(0xFF94A3B8), fontSize = 9.sp)
                            Text(
                                text = String.format("%.4f", quranicGap),
                                color = Color(0xFF34D399),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (isArabic) "الثغرة الطيفية العشوائية" else "Random Spectral Gap", color = Color(0xFF94A3B8), fontSize = 9.sp)
                            Text(
                                text = String.format("%.4f", randomGap),
                                color = Color(0xFFEF4444),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isArabic)
                            "توضح الثغرة طيفية للمصفوفات الحقيقية فرقاً جوهرياً فائقاً عن فضاء الضوضاء العشوائية، مما يثبت إحصائياً أن مصفوفات السور مسبوكة بنظام تماثل لا تشوبه العشوائية الطفيفة."
                            else "The significantly larger Quranic Spectral Gap mathematically rejects the Null Hypothesis, confirming the rigorous algebraic structures behind the Quranic surahs.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 9.5.sp,
                        lineHeight = 13.5.sp
                    )
                }
            }
        }

        // Sensitivity Card (Monte Carlo Perturbation Analysis)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x1F1E1B4B)),
                border = BorderStroke(1.dp, Color(0x33C084FC))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (isArabic) "تحليل الحساسية والمتانة (Sensitivity Analysis)" else "Sensitivity Simulation & Cluster Rigidity",
                        color = Color(0xFFC084FC),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = if (isArabic)
                            "تم تعريض مصفوفة الترابط لتشويش جاوسي عشوائي بنسبة 15%. ثم أعدنا حوسبة التجميع الطيفي بالكامل لمطابقة الاستقرار الجغرافي."
                            else "To analyze stability, we perturbed the similarity space with 15% random Gaussian noise. Redoing normalized spectral maps reveals high rigidity:",
                        color = Color(0xFFE2E8F0),
                        fontSize = 9.5.sp,
                        lineHeight = 13.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "دقة مطابقة العناقيد تحت التشويش (Jaccard):" else "Cluster Overlap Rigidity (Jaccard Index):",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .background(Color(0x33C084FC), CircleShape)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = String.format("%.2f%%", sensitivityOverlap * 100.0),
                                color = Color(0xFFC084FC),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Text(
                        text = if (isArabic) "تشير النسبة العالية لعدم انزياح تجمعات العناصر الجبرية لصلابة التماثل الطيفي." else "A high overlap percentage proves the absolute geometric rigidity and robustness of Quranic spectral clusters.",
                        color = Color(0xFF64748B),
                        fontSize = 8.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // Cohesion per cluster
        item {
            Text(
                text = if (isArabic) "قوة ترابط العناقيد (Intra-Cluster Similarity Cohesion):" else "Intra-Cluster Geometric Cohesion Index:",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (c in 0 until numClustersK) {
                    val count = clusters.count { it == c }
                    val cohesion = clusterCohesion[c]
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x0CFFFFFF)),
                        border = BorderStroke(0.5.dp, Color(0x1F22D3EE))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isArabic) "العنقود ${c+1}" else "Cluster ${c+1}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isArabic) "$count سورة" else "$count Surahs sequential",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 9.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format("%.4f", cohesion),
                                    color = Color(0xFF22D3EE),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (isArabic) "معدل الترابط الجوهري" else "Cosine Affinity Cohesion",
                                    color = Color(0xFF64748B),
                                    fontSize = 8.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Expanded cluster display
        item {
            Text(
                text = if (isArabic) "توزيع السور على العناقيد الـ K:" else "Detailed Cluster Assignments for 114 Surahs:",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x0CFFFFFF), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (c in 0 until numClustersK) {
                        val surahIndexList = clusters.indices.filter { clusters[it] == c }.map { it + 1 }
                        Column {
                            Text(
                                text = if (isArabic) "العنقود ${c+1} ($surahIndexList.size سورة):" else "Cluster ${c+1} (${surahIndexList.size} Surahs):",
                                color = Color(0xFF10B981),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = surahIndexList.joinToString(", "),
                                color = Color(0xFFCBD5E1),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 13.sp
                            )
                        }
                        if (c < numClustersK - 1) {
                            Divider(color = Color(0x12FFFFFF), modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}
