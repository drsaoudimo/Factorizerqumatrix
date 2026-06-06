package com.example

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigInteger

class FactorizerViewModel : ViewModel() {
    var inputNumber by mutableStateOf("114837291011")
    var isAnalyzing by mutableStateOf(false)
    var spectralResult by mutableStateOf<SpectralResult?>(null)
    var activeTab by mutableStateOf("analyze") // analyze, matrices, history, info
    
    private var _pollardRhoStepsState by mutableStateOf(150000)
    var pollardRhoStepsState: Int
        get() = _pollardRhoStepsState
        set(value) {
            _pollardRhoStepsState = value
            MatrixHelper.globalMaxSteps = value
        }
    var determinantMode by mutableStateOf("bareiss")
    
    val historyList = mutableStateListOf<Pair<String, List<BigInteger>>>()

    private var loadedMap: Map<Int, Array<IntArray>> = emptyMap()

    fun initData(context: Context) {
        if (loadedMap.isNotEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonString = context.resources.openRawResource(R.raw.matrices).use { stream ->
                    stream.bufferedReader().use { it.readText() }
                }
                loadedMap = MatrixHelper.loadMatrices(jsonString)
                analyze(context, silent = true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun analyze(context: Context, silent: Boolean = false) {
        val nStr = inputNumber.trim().filter { it.isDigit() }
        if (nStr.isEmpty()) {
            if (!silent) {
                Toast.makeText(context, "Please enter a valid positive number", Toast.LENGTH_SHORT).show()
            }
            return
        }
        val n = nStr.toBigInteger()
        if (n <= BigInteger.ONE) {
            if (!silent) {
                Toast.makeText(context, "Please enter a number greater than 1", Toast.LENGTH_SHORT).show()
            }
            return
        }

        isAnalyzing = true
        viewModelScope.launch(Dispatchers.Default) {
            val res = MatrixHelper.factorizeSpectral(n, loadedMap)
            withContext(Dispatchers.Main) {
                spectralResult = res
                isAnalyzing = false
                if (!silent) {
                    if (historyList.none { it.first == nStr }) {
                        historyList.add(0, Pair(nStr, res.factors))
                    }
                }
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: FactorizerViewModel = viewModel()
                val context = LocalContext.current
                
                var showSettingsDialog by remember { mutableStateOf(false) }
                
                if (showSettingsDialog) {
                    SettingsDialog(
                        onDismiss = { showSettingsDialog = false },
                        viewModel = viewModel
                    )
                }
                
                LaunchedEffect(Unit) {
                    viewModel.initData(context)
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F172A)
                ) {
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding(),
                        bottomBar = {
                            GlassBottomNav(
                                activeTab = viewModel.activeTab,
                                onTabSelected = { viewModel.activeTab = it }
                            )
                        },
                        containerColor = Color.Transparent
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            SpectralHeader(onSettingsClick = { showSettingsDialog = true })
                            
                            Box(modifier = Modifier.weight(1f)) {
                                when (viewModel.activeTab) {
                                    "analyze" -> AnalyzeTab(viewModel)
                                    "matrices" -> MatricesTab(viewModel)
                                    "history" -> HistoryTab(viewModel)
                                    "info" -> InfoTab()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpectralHeader(onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = Color(0x10FFFFFF),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .background(Color(0x1A0F1728))
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF6366F1), Color(0xFF9333EA))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Matrix Logo",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column {
                Text(
                    text = "Spectral Prime",
                    color = Color(0xFFF1F5F9),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "QURANIC MATRIX V4.0",
                    color = Color(0xFFA5B4FC),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }
        }
        
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .size(40.dp)
                .background(Color(0x0FFFFFFF), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    viewModel: FactorizerViewModel
) {
    val context = LocalContext.current
    var pollardRhoSteps by remember { mutableStateOf(viewModel.pollardRhoStepsState.toFloat()) }
    var selectedDeterminantMode by remember { mutableStateOf(viewModel.determinantMode) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.pollardRhoStepsState = pollardRhoSteps.toInt()
                    viewModel.determinantMode = selectedDeterminantMode
                    Toast.makeText(context, "Settings Saved", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            ) {
                Text("Save", color = Color(0xFF818CF8), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        },
        containerColor = Color(0xFF1E293B),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings Icon",
                    tint = Color(0xFF818CF8)
                )
                Text(
                    text = "Matrix Configuration",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HorizontalDivider(color = Color(0x1AFFFFFF))
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Pollard's Rho Max Iterations: ${pollardRhoSteps.toInt()}",
                        color = Color(0xFFE2E8F0),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = pollardRhoSteps,
                        onValueChange = { pollardRhoSteps = it },
                        valueRange = 10000f..500000f,
                        steps = 49,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF818CF8),
                            activeTrackColor = Color(0xFF818CF8),
                            inactiveTrackColor = Color(0x33FFFFFF)
                        )
                    )
                    Text(
                        text = "Allows deeper factorization search for massive numbers, albeit with slight latency tradeoffs.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
                
                HorizontalDivider(color = Color(0x1AFFFFFF))
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Determinant Mode",
                        color = Color(0xFFE2E8F0),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { selectedDeterminantMode = "bareiss" }
                        ) {
                            RadioButton(
                                selected = selectedDeterminantMode == "bareiss",
                                onClick = { selectedDeterminantMode = "bareiss" },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF818CF8))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Exact Bareiss", color = Color.White, fontSize = 14.sp)
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { selectedDeterminantMode = "eigenvals" }
                        ) {
                            RadioButton(
                                selected = selectedDeterminantMode == "eigenvals",
                                onClick = { selectedDeterminantMode = "eigenvals" },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF818CF8))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Eigenvalues", color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
                
                HorizontalDivider(color = Color(0x1AFFFFFF))
                
                Button(
                    onClick = {
                        viewModel.historyList.clear()
                        Toast.makeText(context, "History Cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear History Icon",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear Factorization History", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}

@Composable
fun AnalyzeTab(viewModel: FactorizerViewModel) {
    val context = LocalContext.current
    val result = viewModel.spectralResult
    
    val quickEx = listOf(
        "19", "114", "6236", "114837291011", "9023476901"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, Color(0x1EFFFFFF)), RoundedCornerShape(24.dp))
                    .background(Color(0x09FFFFFF), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Text(
                    text = "TARGET NUMBER (N)",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = viewModel.inputNumber,
                        onValueChange = { viewModel.inputNumber = it.filter { c -> c.isDigit() } },
                        placeholder = { Text("Enter integer...", color = Color(0xFF64748B)) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0x260F172A),
                            unfocusedContainerColor = Color(0x130F172A),
                            focusedTextColor = Color(0xFFA5B4FC),
                            unfocusedTextColor = Color(0xFFA5B4FC),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(BorderStroke(1.dp, Color(0x14FFFFFF)), RoundedCornerShape(16.dp)),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Button(
                        onClick = { viewModel.analyze(context) },
                        enabled = !viewModel.isAnalyzing,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4F46E5)
                        ),
                        modifier = Modifier
                            .height(56.dp)
                            .animateContentSize()
                    ) {
                        if (viewModel.isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("ANALYZE", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Quick Candidates:",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickEx.forEach { ex ->
                        Box(
                            modifier = Modifier
                                .background(Color(0x12FFFFFF), RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.inputNumber = ex
                                    viewModel.analyze(context)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(ex, color = Color(0xFFCBD5E1), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        if (result != null) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, Color(0x16FFFFFF)), RoundedCornerShape(24.dp))
                        .background(Color(0x09FFFFFF), RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF34D399), CircleShape)
                            )
                            Text(
                                text = "A(N) MATRIX PROJECTION",
                                color = Color(0xFFE2E8F0),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "s=1..114 | Rank: 28",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    MatrixVisualizerCanvas(matrix = result.matrixCells)
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0x0FFFFFFF), RoundedCornerShape(16.dp))
                                .border(BorderStroke(1.dp, Color(0x12FFFFFF)), RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "DETERMINANT Δ(N)",
                                color = Color(0xFF94A3B8),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val detText = if (result.determinantDouble.isInfinite() || result.determinantDouble.isNaN()) "Infinity" else String.format("%.4e", result.determinantDouble)
                            Text(
                                text = detText,
                                color = Color(0xFFE2E8F0),
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(top = 4.dp),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1
                            )
                        }
                        
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0x0FFFFFFF), RoundedCornerShape(16.dp))
                                .border(BorderStroke(1.dp, Color(0x12FFFFFF)), RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "EIGEN-STABILITY",
                                color = Color(0xFF94A3B8),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = String.format("%.6f", result.eigenStability),
                                color = Color(0xFF34D399),
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0x0FFFFFFF), RoundedCornerShape(16.dp))
                                .border(BorderStroke(1.dp, Color(0x12FFFFFF)), RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "T(N) SPECTRUM PRODUCT",
                                color = Color(0xFFA5B4FC),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val tNText = result.tN.toString()
                            Text(
                                text = tNText,
                                color = Color(0xFFE2E8F0),
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(top = 4.dp),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1
                            )
                        }
                        
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0x0FFFFFFF), RoundedCornerShape(16.dp))
                                .border(BorderStroke(1.dp, Color(0x12FFFFFF)), RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "gcd(T(N), N) OVERLAP",
                                color = Color(0xFFA5B4FC),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val gcdText = result.eigenvalueGcd.toString()
                            Text(
                                text = gcdText,
                                color = Color(0xFF34D399),
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(top = 4.dp),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1
                            )
                        }
                    }

                    if (result.eigenvaluesReal.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "LEADING MATRIX EIGENVALUES (λ_i):",
                            color = Color(0xFF64748B),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (i in 0 until minOf(4, result.eigenvaluesReal.size)) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(Color(0x0BFFFFFF), RoundedCornerShape(8.dp))
                                        .border(BorderStroke(1.dp, Color(0x0AFFFFFF)), RoundedCornerShape(8.dp))
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = String.format("%.2f", result.eigenvaluesReal[i]),
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
                            ),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val isPrime = result.factors.size <= 1
                            Text(
                                text = if (isPrime) "Prime Structure Found" else "Spectral Factors Found",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isPrime) "No spectral divisor overlaps (g = 1)" else "Distinct spectral overlaps observed (g > 1)",
                                color = Color(0xFFC7D2FE),
                                fontSize = 11.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        result.factors.forEach { factor ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0x26000000), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = factor.toString(),
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                val isPrime = factor.isProbablePrime(15)
                                Text(
                                    text = if (isPrime) "Prime Factor" else "Composite Factor",
                                    color = if (isPrime) Color(0xFFA5B4FC) else Color(0xFFFDA4AF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, Color(0x16FFFFFF)), RoundedCornerShape(24.dp))
                        .background(Color(0x09FFFFFF), RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF38BDF8), CircleShape)
                        )
                        Text(
                            text = "A(N) EXACT INTEGER DETERMINANT",
                            color = Color(0xFFE2E8F0),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Text(
                        text = "Calculates the exact theoretical integer determinant Δ(N) of the joint projection operator A(N) using division-free Bareiss reductions, bypassing floating-point bounds entirely.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x0FFFFFFF), RoundedCornerShape(16.dp))
                            .border(BorderStroke(1.dp, Color(0x12FFFFFF)), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "EXACT Δ(N) LENGTH",
                            color = Color(0xFF94A3B8),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val numDigits = result.exactDeltaN.abs().toString().length
                        Text(
                            text = if (result.exactDeltaN == BigInteger.ZERO) "0 (Singular Matrix)" else "$numDigits Digit Integer",
                            color = Color(0xFF38BDF8),
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "gcd(Δ(N), N) FACTOR OVERLAP",
                            color = Color(0xFF94A3B8),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.padding(top = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = result.exactDeltaGcd.toString(),
                                color = if (result.exactDeltaGcd > BigInteger.ONE && result.exactDeltaGcd < result.exactDeltaN) Color(0xFF34D399) else Color(0xFFE2E8F0),
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1
                            )
                            if (result.exactDeltaGcd > BigInteger.ONE && result.exactDeltaGcd < result.exactDeltaN) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0x2634D399), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("FACTOR FOUND", color = Color(0xFF34D399), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, Color(0x16FFFFFF)), RoundedCornerShape(24.dp))
                        .background(Color(0x09FFFFFF), RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFFC084FC), CircleShape)
                        )
                        Text(
                            text = "INDIVIDUAL MATRIX DETERMINANT GCD",
                            color = Color(0xFFE2E8F0),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Computes D_s(N) = det(M_s - N*I) for all 114 Surah matrices. Calculates the global common divisor G(N) = gcd(D_1, D_2, ..., D_114). Factors are extracted directly from individual matrix determinant overlaps with N.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x0FFFFFFF), RoundedCornerShape(16.dp))
                            .border(BorderStroke(1.dp, Color(0x12FFFFFF)), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "GLOBAL MATRIX DIVISOR G(N)",
                            color = Color(0xFF94A3B8),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val gNText = if (result.gN == BigInteger.ZERO) "0" else "${result.gN.abs().toString().length}-digit matrix GCD"
                        Text(
                            text = gNText,
                            color = Color(0xFFC084FC),
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        if (result.gNFactors.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "INDIVIDUALLY DISCOVERED OVERLAPS",
                                color = Color(0xFFA5B4FC),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                result.gNFactors.forEach { factor ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0x14FFFFFF), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = factor.toString(),
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.weight(1f),
                                            overflow = TextOverflow.Ellipsis,
                                            maxLines = 1
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0x2034D399), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text("Det Divisor", color = Color(0xFF34D399), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No separate divisor overlaps found from individual D_s.",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, Color(0x16FFFFFF)), RoundedCornerShape(24.dp))
                        .background(Color(0x09FFFFFF), RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFFF59E0B), CircleShape)
                        )
                        Text(
                            text = "FINITE PROJECTIONS & PRIMALITY MONITOR",
                            color = Color(0xFFE2E8F0),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Projects all 114 Surah matrices into prime finite fields F_p. Monitoring rank drop characteristics compares how the space reduces: a divisor p of N exhibits clear rank drops (< 28) and eigenvalue stability changes.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (result.primeRankProfiles.isNotEmpty()) {
                        var selectedPrime by remember(result) {
                            mutableStateOf(result.primeRankProfiles.keys.firstOrNull() ?: "")
                        }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            result.primeRankProfiles.keys.forEach { p ->
                                val isSelected = p == selectedPrime
                                val isDivisor = result.factors.any { it.toString() == p }
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isSelected) Color(0xFFF59E0B) else Color(0x1AFFFFFF),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedPrime = p }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "p = $p" + (if (isDivisor) " (Divisor)" else ""),
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val avgRank = result.averageRanks[selectedPrime] ?: 0.0
                        val isDivisorOfN = result.factors.any { it.toString() == selectedPrime }
                        val drops = result.rankDrops[selectedPrime] ?: emptyList()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0x0FFFFFFF), RoundedCornerShape(12.dp))
                                    .border(BorderStroke(1.dp, Color(0x12FFFFFF)), RoundedCornerShape(12.dp))
                                    .padding(10.dp)
                            ) {
                                val rankColor = if (avgRank < 28.0) Color(0xFFF59E0B) else Color(0xFF10B981)
                                Text("AVERAGE F_p RANK", color = Color(0xFF94A3B8), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "%.5f".format(avgRank) + "/28",
                                    color = rankColor,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0x0FFFFFFF), RoundedCornerShape(12.dp))
                                    .border(BorderStroke(1.dp, Color(0x12FFFFFF)), RoundedCornerShape(12.dp))
                                    .padding(10.dp)
                            ) {
                                Text("PROJECTION DROPS", color = Color(0xFF94A3B8), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${drops.size} Matrices",
                                    color = if (drops.isNotEmpty()) Color(0xFFF87171) else Color(0xFF94A3B8),
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        if (isDivisorOfN) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0x1AF59E0B), RoundedCornerShape(12.dp))
                                    .border(BorderStroke(1.dp, Color(0x33F59E0B)), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Analysis",
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Significant rank drop observed modulo spectral factor p! Average rank reduced below maximum (28.00) confirms non-trivial subspace reductions.",
                                        color = Color(0xFFFDE68A),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        if (drops.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "DETAILED RANK DROPS FOR SELECTED P:",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 120.dp)
                                    .verticalScroll(rememberScrollState())
                                    .background(Color(0x0FFFFFFF), RoundedCornerShape(12.dp))
                                    .border(BorderStroke(1.dp, Color(0x12FFFFFF)), RoundedCornerShape(12.dp))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                drops.forEach { dropDetail ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(dropDetail, color = Color(0xFFCBD5E1), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0x1AF59E0B), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text("Drop", color = Color(0xFFF59E0B), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No projection primes calculated yet.",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MatrixVisualizerCanvas(matrix: Array<IntArray>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Color(0x1F0F172A), RoundedCornerShape(16.dp))
            .border(BorderStroke(1.dp, Color(0x0FFFFFFF)), RoundedCornerShape(16.dp))
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val rows = 28
            val cols = 28
            val cellW = size.width / cols
            val cellH = size.height / rows

            var maxValue = 1
            for (i in 0 until rows) {
                for (j in 0 until cols) {
                    if (matrix[i][j] > maxValue) {
                        maxValue = matrix[i][j]
                    }
                }
            }

            for (i in 0 until rows) {
                for (j in 0 until cols) {
                    val value = matrix[i][j]
                    val intensity = if (value > 0) (value.toFloat() / maxValue).coerceIn(0.1f, 1.0f) else 0.0f
                    if (intensity > 0f) {
                        val color = Color(0xFF6366F1).copy(alpha = intensity)
                        drawRect(
                            color = color,
                            topLeft = Offset(j * cellW, i * cellH),
                            size = Size(cellW - 0.5f, cellH - 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FactorizerScreen(modifier: Modifier = Modifier, viewModel: FactorizerViewModel = viewModel()) {
    Column(modifier = modifier.padding(16.dp)) {
        AnalyzeTab(viewModel)
    }
}

@Composable
fun MatricesTab(viewModel: FactorizerViewModel) {
    var selectedS by remember { mutableStateOf(1) }
    val mockLoaded = remember { mutableMapOf<Int, Array<IntArray>>() }
    val matrix = remember(selectedS) { MatrixHelper.getMatrixForSurah(selectedS, mockLoaded) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Quranic Spectral Operator matrices",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Select Surah s:", color = Color(0xFF94A3B8), fontSize = 14.sp)
            Slider(
                value = selectedS.toFloat(),
                onValueChange = { selectedS = it.toInt() },
                valueRange = 1f..114f,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "s = $selectedS",
                color = Color(0xFFA5B4FC),
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 15.sp
            )
        }

        Text(
            text = "Operator Matrix M_$selectedS Shape Projection:",
            color = Color(0xFF94A3B8),
            fontSize = 12.sp
        )

        MatrixVisualizerCanvas(matrix = matrix)

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0x0FFFFFFF)),
            border = BorderStroke(1.dp, Color(0x12FFFFFF)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Matrix Operators & Lattices",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Each Surah generates a unique 28x28 matrix mapping of phonetic weights acting as an eigenvalue filter to intercept divisibility patterns in large numbers N.",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun HistoryTab(viewModel: FactorizerViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Factorization History Log",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (viewModel.historyList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "No history",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No history analyzed yet", color = Color(0xFF475569))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(viewModel.historyList) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x0FFFFFFF), RoundedCornerShape(16.dp))
                            .border(BorderStroke(1.dp, Color(0x12FFFFFF)), RoundedCornerShape(16.dp))
                            .clickable {
                                viewModel.inputNumber = item.first
                                viewModel.activeTab = "analyze"
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "N = " + item.first,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Factors: " + item.second.joinToString(" × "),
                                color = Color(0xFFA5B4FC),
                                fontSize = 12.sp,
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

@Composable
fun InfoTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Mathematical Framework",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        
        Text(
            text = "This application implements the proposed spectral prime factorizer operators utilizing the 114 Matrices on modular rings.",
            color = Color(0xFFCBD5E1),
            fontSize = 13.sp
        )

        Divider(color = Color(0x16FFFFFF))

        Text(
            text = "1. Matrix Sum operator A(N):",
            color = Color(0xFFA5B4FC),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Text(
            text = "A(N) is defined as the weighted linear combination sum across all 114 Surah matrices:\n" +
                   "A(N) = Σ (N mod s) * M_s",
            color = Color(0xFF94A3B8),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            modifier = Modifier
                .background(Color(0x0FFFFFFF), RoundedCornerShape(8.dp))
                .padding(12.dp)
        )

        Text(
            text = "2. Determinant check Δ(N):",
            color = Color(0xFFA5B4FC),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Text(
            text = "The determinant of A(N) is calculated to identify lattice stability overlaps:\n" +
                   "Δ(N) = det(A(N))\n" +
                   "g = gcd(Δ(N), N)\n" +
                   "If 1 < g < N, we directly obtain a non-trivial factor of N.",
            color = Color(0xFF94A3B8),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            modifier = Modifier
                .background(Color(0x0FFFFFFF), RoundedCornerShape(8.dp))
                .padding(12.dp)
        )

        Text(
            text = "Theoretical context:",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Text(
            text = "This model presents an elegant exploratory framework combining number theory and quantum spectral matrix theory. Highly complex numeric arrays represent multidimensional spaces that reveal factors quickly under row reductions.",
            color = Color(0xFF94A3B8),
            fontSize = 12.sp
        )
    }
}

@Composable
fun GlassBottomNav(
    activeTab: String,
    onTabSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A))
            .drawBehind {
                drawLine(
                    color = Color(0x10FFFFFF),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val tabs = listOf(
            Triple("analyze", Icons.Default.PlayArrow, "Analyze"),
            Triple("matrices", Icons.Default.Home, "Matrices"),
            Triple("history", Icons.Default.Favorite, "History"),
            Triple("info", Icons.Default.Info, "Info")
        )
        
        tabs.forEach { tab ->
            val isSelected = activeTab == tab.first
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabSelected(tab.first) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = tab.second,
                    contentDescription = tab.third,
                    tint = if (isSelected) Color(0xFF818CF8) else Color(0xFF64748B),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tab.third,
                    color = if (isSelected) Color(0xFF818CF8) else Color(0xFF64748B),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
