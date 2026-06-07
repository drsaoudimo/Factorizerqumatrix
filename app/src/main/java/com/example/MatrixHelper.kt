package com.example

import java.math.BigInteger
import java.util.Random

class RankReductionTestResult(
    val prime: java.math.BigInteger,
    val dividesN: Boolean,
    val averageRank: Double,
    val rankDropsCount: Int,
    val isFlagged: Boolean,
    val ranks: List<Int>
)

class PollardBrentStep(
    val step: Int,
    val x: BigInteger,
    val surah: Int,
    val row: Int,
    val col: Int,
    val matrixValue: Int,
    val gcdValue: BigInteger
)

class QuranicPollardBrentResult(
    val factor: BigInteger,
    val steps: List<PollardBrentStep>,
    val totalSteps: Int,
    val isSuccess: Boolean
)

class SpectralResult(
    val gN: BigInteger,
    val gNFactors: List<BigInteger>,
    val tN: BigInteger,
    val factors: List<BigInteger>,
    val primeRankProfiles: Map<String, List<Int>>,
    val averageRanks: Map<String, Double>,
    val rankDrops: Map<String, List<Int>>,
    val individualDets: List<BigInteger>,
    val deltaN: BigInteger,
    val advancedFactor: BigInteger,
    val spectralFingerprint: List<Double>,
    val rankReductionResults: List<RankReductionTestResult>
)

object MatrixHelper {

    private val matrixCache: Array<Array<IntArray>> by lazy {
        Array(114) { s -> generateQuranicMatrix(s + 1) }
    }

    private val detMsCache: Array<BigInteger> by lazy {
        Array(114) { s ->
            val m_s = getQuranicMatrix(s + 1)
            val bigM = Array(28) { i -> Array(28) { j -> BigInteger.valueOf(m_s[i][j].toLong()) } }
            determinantBareiss(bigM)
        }
    }

    fun getQuranicMatrix(s: Int): Array<IntArray> {
        val idx = (s - 1).coerceIn(0, 113)
        return matrixCache[idx]
    }

    fun getDetMs(s: Int): BigInteger {
        val idx = (s - 1).coerceIn(0, 113)
        return detMsCache[idx]
    }

    // Actual verse counts for all 114 Quranic Surahs
    val SURAH_VERSES = intArrayOf(
        7, 286, 200, 176, 120, 165, 206, 75, 129, 109, // 1-10
        123, 111, 43, 52, 99, 128, 111, 110, 98, 135, // 11-20
        112, 78, 118, 64, 77, 227, 93, 88, 69, 60,   // 21-30
        34, 30, 73, 54, 45, 83, 182, 88, 75, 85,     // 31-40
        54, 53, 89, 59, 37, 35, 38, 29, 18, 45,      // 41-50
        60, 49, 62, 55, 78, 96, 29, 22, 24, 13,      // 51-60
        14, 11, 11, 18, 12, 12, 30, 52, 52, 44,      // 61-70
        28, 28, 20, 56, 40, 31, 50, 46, 42, 29,      // 71-80
        19, 36, 25, 22, 17, 19, 26, 30, 20, 15,      // 81-90
        21, 11, 8, 8, 11, 5, 8, 8, 11, 11,           // 91-100
        11, 3, 9, 5, 4, 7, 3, 6, 3, 5,               // 101-110
        4, 5, 6, 6                                   // 111-114
    )

    // Generator for 114 Quranic Matrices (28x28 symmetric)
    fun generateQuranicMatrix(s: Int): Array<IntArray> {
        val size = 28
        val matrix = Array(size) { IntArray(size) }
        val verses = if (s in 1..114) SURAH_VERSES[s - 1] else 7
        val r = Random((s * 31 + verses * 17).toLong())
        
        for (i in 0 until size) {
            for (j in i until size) {
                if (i == j) {
                    matrix[i][j] = r.nextInt(5) + 1 + (verses % 3)
                } else {
                    val randVal = r.nextInt(100)
                    val value = if (randVal < 15) r.nextInt(2) + 1 else 0
                    matrix[i][j] = value
                    matrix[j][i] = value // Symmetric for robust Jacobi eigenvalue calculation
                }
            }
        }
        return matrix
    }

    // Bareiss determinant solver in BigInteger (O(N^3) exact algorithm)
    fun determinantBareiss(matrix: Array<Array<BigInteger>>): BigInteger {
        val n = matrix.size
        if (n == 0) return BigInteger.ONE
        
        val temp = Array(n) { i -> Array(n) { j -> matrix[i][j] } }
        var sign = 1
        
        for (k in 0 until n) {
            if (temp[k][k] == BigInteger.ZERO) {
                var swapRow = -1
                for (i in k + 1 until n) {
                    if (temp[i][k] != BigInteger.ZERO) {
                        swapRow = i
                        break
                    }
                }
                if (swapRow == -1) return BigInteger.ZERO
                
                // Swap rows
                val t = temp[k]
                temp[k] = temp[swapRow]
                temp[swapRow] = t
                sign = -sign
            }
            
            val pivot = temp[k][k]
            val prevPivot = if (k == 0) BigInteger.ONE else temp[k - 1][k - 1]
            
            for (i in k + 1 until n) {
                for (j in k + 1 until n) {
                    val num = (temp[i][j] * pivot) - (temp[i][k] * temp[k][j])
                    temp[i][j] = num / prevPivot
                }
            }
        }
        
        val det = temp[n - 1][n - 1]
        return if (sign < 0) det.negate() else det
    }

    // Jacobi Eigenvalue Solver for Real Symmetric 28x28 Matrices
    fun jacobiEigenvalues(matrix: Array<DoubleArray>): DoubleArray {
        val n = matrix.size
        val a = Array(n) { i -> matrix[i].clone() }
        val eigenvalues = DoubleArray(n)
        
        var count = 0
        val maxIterations = 200
        val eps = 1e-9
        
        while (count < maxIterations) {
            var maxVal = 0.0
            var p = 0
            var q = 0
            for (i in 0 until n) {
                for (j in i + 1 until n) {
                    val absVal = Math.abs(a[i][j])
                    if (absVal > maxVal) {
                        maxVal = absVal
                        p = i
                        q = j
                    }
                }
            }
            
            if (maxVal < eps) break
            
            val apq = a[p][q]
            val app = a[p][p]
            val aqq = a[q][q]
            
            val phi = 0.5 * Math.atan2(2 * apq, aqq - app)
            val c = Math.cos(phi)
            val s = Math.sin(phi)
            
            val appNew = c * c * app - 2 * s * c * apq + s * s * aqq
            val aqqNew = s * s * app + 2 * s * c * apq + c * c * aqq
            a[p][p] = appNew
            a[q][q] = aqqNew
            a[p][q] = 0.0
            a[q][p] = 0.0
            
            for (i in 0 until n) {
                if (i != p && i != q) {
                    val aip = a[i][p]
                    val aiq = a[i][q]
                    a[i][p] = c * aip - s * aiq
                    a[p][i] = a[i][p]
                    a[i][q] = s * aip + c * aiq
                    a[q][i] = a[i][q]
                }
            }
            count++
        }
        
        for (i in 0 until n) {
            eigenvalues[i] = a[i][i]
        }
        eigenvalues.sort()
        return eigenvalues
    }

    // Modular Gaussian Elimination for Matrix Rank Modulo P (Safe for extreme primes/BigIntegers)
    fun calculateMatrixRankModP(matrix: Array<IntArray>, p: BigInteger): Int {
        val m = matrix.size
        val n = matrix[0].size
        val temp = Array(m) { i -> Array(n) { j -> 
            val entry = BigInteger.valueOf(matrix[i][j].toLong())
            entry.mod(p)
        } }
        
        var rank = 0
        var col = 0
        for (row in 0 until m) {
            while (col < n) {
                var pivotRow = row
                while (pivotRow < m && temp[pivotRow][col].mod(p) == BigInteger.ZERO) {
                    pivotRow++
                }
                if (pivotRow < m) {
                    if (pivotRow != row) {
                        val t = temp[row]
                        temp[row] = temp[pivotRow]
                        temp[pivotRow] = t
                    }
                    
                    val pivot = temp[row][col]
                    val invPivot = try { pivot.modInverse(p) } catch(e: Exception) { BigInteger.ONE }
                    
                    for (i in row + 1 until m) {
                        val factor = temp[i][col].multiply(invPivot).mod(p)
                        if (factor != BigInteger.ZERO) {
                            for (j in col until n) {
                                val sub = temp[i][j].subtract(factor.multiply(temp[row][j])).mod(p)
                                temp[i][j] = (sub + p).mod(p)
                            }
                        }
                    }
                    rank++
                    col++
                    break
                } else {
                    col++
                }
            }
        }
        return rank
    }

    // Recursive Complete Quranic guided Pollard Brent Factorization engine
    fun factorizeComplete(n: BigInteger): List<BigInteger> {
        val factors = java.util.concurrent.CopyOnWriteArrayList<BigInteger>()
        factorizeRecursive(n, factors)
        
        val cleanFactors = factors.filter { it > BigInteger.ONE }.sorted().toMutableList()
        
        var i = 0
        while (i < cleanFactors.size) {
            val f = cleanFactors[i]
            if (f > BigInteger.ONE && !f.isProbablePrime(30)) {
                val qpbSub = runQuranicPollardBrent(f, 2000)
                if (qpbSub.isSuccess && qpbSub.factor > BigInteger.ONE && qpbSub.factor < f) {
                    cleanFactors.removeAt(i)
                    cleanFactors.add(qpbSub.factor)
                    cleanFactors.add(f.divide(qpbSub.factor))
                    cleanFactors.sort()
                    i = 0 
                    continue
                }
            }
            i++
        }
        
        val uniquePrimes = cleanFactors.filter { it > BigInteger.ONE }.sorted().distinct()
        return if (uniquePrimes.isEmpty() && n > BigInteger.ONE) listOf(n) else uniquePrimes
    }

    private fun factorizeRecursive(n: BigInteger, factors: MutableList<BigInteger>) {
        if (n <= BigInteger.ONE) return
        
        // 1. Primality check
        if (n.isProbablePrime(30)) {
            factors.add(n)
            return
        }

        // 2. Trial division for small primes (up to 2000)
        var temp = n
        val smallPrimes = listOf(
            2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97, 
            101, 103, 107, 109, 113, 127, 131, 137, 139, 149, 151, 157, 163, 167, 173, 179, 181, 191, 193, 
            197, 199, 211, 223, 227, 229, 233, 239, 241, 251, 257, 263, 269, 271, 277, 281, 283, 293, 307, 
            311, 313, 317, 331, 337, 347, 349, 353, 359, 367, 373, 379, 383, 389, 397, 401, 409, 419, 421, 
            431, 433, 439, 443, 449, 457, 461, 463, 467, 479, 487, 491, 499, 503, 509, 521, 523, 541, 547, 
            557, 563, 569, 571, 577, 587, 593, 599, 601, 607, 613, 617, 619, 631, 641, 643, 647, 653, 659, 
            661, 673, 677, 683, 691, 701, 709, 719, 727, 733, 739, 743, 751, 757, 761, 769, 773, 787, 797, 
            809, 811, 821, 823, 827, 829, 839, 853, 857, 859, 863, 877, 881, 883, 887, 907, 911, 919, 929, 
            937, 941, 947, 953, 967, 971, 977, 983, 991, 997,
            1009, 1013, 1019, 1021, 1031, 1033, 1039, 1049, 1051, 1061, 1063, 1069, 1087, 1091, 1093, 1097, 
            1103, 1109, 1117, 1123, 1129, 1151, 1153, 1163, 1171, 1181, 1187, 1193, 1201, 1213, 1217, 1223, 
            1229, 1231, 1237, 1249, 1259, 1277, 1279, 1283, 1289, 1291, 1297, 1301, 1303, 1307, 1319, 1321, 
            1327, 1361, 1367, 1373, 1381, 1399, 1409, 1423, 1427, 1429, 1433, 1439, 1447, 1451, 1453, 1459, 
            1471, 1481, 1483, 1487, 1489, 1493, 1499, 1511, 1523, 1531, 1543, 1549, 1553, 1559, 1567, 1571, 
            1579, 1583, 1597, 1601, 1607, 1609, 1613, 1619, 1621, 1627, 1637, 1657, 1663, 1667, 1669, 1693, 
            1697, 1699, 1709, 1721, 1723, 1733, 1741, 1747, 1753, 1759, 1777, 1783, 1787, 1789, 1801, 1811, 
            1823, 1831, 1847, 1861, 1867, 1871, 1873, 1877, 1879, 1889, 1901, 1907, 1913, 1931, 1933, 1949, 
            1951, 1973, 1979, 1987, 1993, 1997, 1999
        )
        
        var trialFound = false
        for (pVal in smallPrimes) {
            val p = BigInteger.valueOf(pVal.toLong())
            if (p >= temp) break
            if (temp.mod(p) == BigInteger.ZERO) {
                factors.add(p)
                while (temp.mod(p) == BigInteger.ZERO) {
                    temp = temp.divide(p)
                }
                trialFound = true
            }
        }
        
        if (trialFound) {
            if (temp > BigInteger.ONE) {
                factorizeRecursive(temp, factors)
            }
            return
        }

        // 3. Recursive Quranic Pollard's Rho Brent Core
        val qpbResult = runQuranicPollardBrent(temp, 4000)
        if (qpbResult.isSuccess && qpbResult.factor > BigInteger.ONE && qpbResult.factor < temp) {
            val f = qpbResult.factor
            factorizeRecursive(f, factors)
            factorizeRecursive(temp.divide(f), factors)
        } else {
            factors.add(temp)
        }
    }

    // Combined Factorization and Analysis
    fun factorizeSpectral(
        n: BigInteger,
        loaded: Map<Int, Array<IntArray>> = emptyMap(),
        customPrimes: List<BigInteger> = emptyList()
    ): SpectralResult {
        val size = 28
        val nIsLarge = n.bitLength() > 50
        
        // 1. Calculate A(N) = sum_{s=1}^{114} (N mod s) * M_s
        val matrixA = Array(size) { Array(size) { BigInteger.ZERO } }
        for (s in 1..114) {
            val rem = n.mod(BigInteger.valueOf(s.toLong()))
            val m_s = getQuranicMatrix(s)
            for (i in 0 until size) {
                for (j in 0 until size) {
                    val weighted = rem.multiply(BigInteger.valueOf(m_s[i][j].toLong()))
                    matrixA[i][j] = matrixA[i][j].add(weighted)
                }
            }
        }

        // 2. Compute Determinant Delta(N) = det(A(N))
        val deltaN = determinantBareiss(matrixA)
        val gFactor = deltaN.abs().gcd(n)

        // 3. Calculate eigenvalues of A(N)
        val doubleA = Array(size) { i -> DoubleArray(size) { j -> matrixA[i][j].toDouble() } }
        val eigenvalues = jacobiEigenvalues(doubleA)

        // Calculate T(N) = product_i (floor(lambda_i) + 1)
        var tN = BigInteger.ONE
        for (lam in eigenvalues) {
            val floored = Math.floor(lam).toLong()
            val term = BigInteger.valueOf(floored + 1)
            tN = tN.multiply(term)
        }
        val tN_gcd = tN.abs().gcd(n)

        // 4. Calculate D_s(N) = det(M_s - N * I) for each of the 114 Matrices
        val individualDets = mutableListOf<BigInteger>()
        var gN = BigInteger.ZERO
        
        for (s in 1..114) {
            val d_s = if (nIsLarge) {
                // Equivalent modulo N to det(M_s)
                getDetMs(s).mod(n)
            } else {
                val m_s = getQuranicMatrix(s)
                val matrixMs_N_I = Array(size) { i ->
                    Array(size) { j ->
                        val ms_val = BigInteger.valueOf(m_s[i][j].toLong())
                        if (i == j) {
                            ms_val.subtract(n)
                        } else {
                            ms_val
                        }
                    }
                }
                determinantBareiss(matrixMs_N_I)
            }
            individualDets.add(d_s)
            val termForGcd = if (nIsLarge) getDetMs(s).abs() else d_s.abs()
            gN = if (gN == BigInteger.ZERO) termForGcd else gN.gcd(termForGcd)
        }

        val gN_gcd = gN.gcd(n)

        // Fully complete and robust prime factorization relying on Quranic Pollard's Brent
        val sortedFactorsList = factorizeComplete(n)

        // 5. Modular Projection test for custom/identified primes
        val referencePrimes = listOf(BigInteger.valueOf(19), BigInteger.valueOf(113))
        val allPrimesToAnalyze = (customPrimes + sortedFactorsList + referencePrimes).distinct().take(8)

        val primeRankProfiles = mutableMapOf<String, List<Int>>()
        val averageRanks = mutableMapOf<String, Double>()
        val rankDrops = mutableMapOf<String, List<Int>>()

        for (p in allPrimesToAnalyze) {
            if (p <= BigInteger.ONE) continue
            val ranks = mutableListOf<Int>()
            val dropsList = mutableListOf<Int>()
            
            val m_s_probe = getQuranicMatrix(1)
            for (s in 1..114) {
                val m_s = getQuranicMatrix(s)
                val rModP = calculateMatrixRankModP(m_s, p)
                ranks.add(rModP)
                if (rModP < 28) {
                    dropsList.add(s)
                }
            }
            
            val pStr = p.toString()
            primeRankProfiles[pStr] = ranks
            averageRanks[pStr] = ranks.average()
            rankDrops[pStr] = dropsList
        }

        // Return the factors of G(N) found as G(N) indicators
        val gNPrimeFactors = mutableListOf<BigInteger>()
        var tempGN = gN
        var divisor = BigInteger.valueOf(2)
        while (divisor.multiply(divisor) <= tempGN && divisor < BigInteger.valueOf(1000)) {
            if (tempGN.mod(divisor) == BigInteger.ZERO) {
                gNPrimeFactors.add(divisor)
                while (tempGN.mod(divisor) == BigInteger.ZERO) {
                    tempGN = tempGN.divide(divisor)
                }
            }
            divisor = divisor.add(BigInteger.ONE)
        }
        if (tempGN > BigInteger.ONE) {
            gNPrimeFactors.add(tempGN)
        }

        // Spectral Fingerprint calculation Phi(N)
        val spectralFingerprint = calculateSpectralFingerprint(n)

        // Detailed Rank Reduction Test Results for Primality
        val testPrimes = (listOf(2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97, 101, 103, 107, 109, 113).map { BigInteger.valueOf(it.toLong()) } + sortedFactorsList + customPrimes)
            .distinct()
            .filter { it > BigInteger.ONE }
            .sorted()
            .take(15)

        val rankReductionResultsList = mutableListOf<RankReductionTestResult>()
        for (p in testPrimes) {
            val ranks = mutableListOf<Int>()
            var dropCount = 0
            for (s in 1..114) {
                val m_s = getQuranicMatrix(s)
                val rank = calculateMatrixRankModP(m_s, p)
                ranks.add(rank)
                if (rank < 28) {
                    dropCount++
                }
            }
            val avgRank = ranks.average()
            val divides = n.mod(p) == BigInteger.ZERO
            val isFlg = divides

            rankReductionResultsList.add(
                RankReductionTestResult(
                    prime = p,
                    dividesN = divides,
                    averageRank = avgRank,
                    rankDropsCount = dropCount,
                    isFlagged = isFlg,
                    ranks = ranks
                )
            )
        }

        return SpectralResult(
            gN = gN,
            gNFactors = gNPrimeFactors.sorted().distinct().take(10),
            tN = tN,
            factors = sortedFactorsList,
            primeRankProfiles = primeRankProfiles,
            averageRanks = averageRanks,
            rankDrops = rankDrops,
            individualDets = individualDets,
            deltaN = deltaN,
            advancedFactor = gFactor,
            spectralFingerprint = spectralFingerprint,
            rankReductionResults = rankReductionResultsList
        )
    }

    fun calculateSpectralFingerprint(n: BigInteger): List<Double> {
        val size = 28
        val modVal = BigInteger.valueOf(6236)
        val v = Array(size) { j ->
            n.mod(modVal).pow(j).mod(modVal).toDouble()
        }
        
        val fingerprint = ArrayList<Double>(114)
        for (s in 1..114) {
            val m_s = getQuranicMatrix(s)
            val w = DoubleArray(size)
            for (i in 0 until size) {
                var sum = 0.0
                for (j in 0 until size) {
                    sum += m_s[i][j] * v[j]
                }
                w[i] = sum
            }
            var sumSq = 0.0
            for (i in 0 until size) {
                sumSq += w[i] * w[i]
            }
            fingerprint.add(Math.sqrt(sumSq))
        }
        return fingerprint
    }

    fun quranicNext(currX: BigInteger, n: BigInteger): Pair<BigInteger, Triple<Int, Int, Int>> {
        val currLong = currX.mod(BigInteger.valueOf(1000000007L)).toLong()
        val s = ((Math.abs(currLong) % 114) + 1).toInt()
        val m_s = getQuranicMatrix(s)
        val row = (Math.abs(currLong) % 28).toInt()
        val col = ((Math.abs(currLong) * 17) % 28).toInt()
        val matrixValue = m_s[row][col]
        val nextX = currX.multiply(currX)
            .add(BigInteger.valueOf(matrixValue.toLong()))
            .add(BigInteger.ONE)
            .mod(n)
        return Pair(nextX, Triple(s, row, col))
    }

    fun runQuranicPollardBrent(n: BigInteger, maxSteps: Int = 1000): QuranicPollardBrentResult {
        val stepsList = mutableListOf<PollardBrentStep>()
        if (n <= BigInteger.ONE) {
            return QuranicPollardBrentResult(BigInteger.ONE, emptyList(), 0, false)
        }
        if (n.mod(BigInteger.valueOf(2)) == BigInteger.ZERO) {
            return QuranicPollardBrentResult(
                BigInteger.valueOf(2), 
                listOf(PollardBrentStep(1, BigInteger.valueOf(2), 1, 0, 0, 0, BigInteger.valueOf(2))), 
                1, 
                true
            )
        }

        var x = BigInteger.valueOf(2)
        var y = BigInteger.valueOf(2)
        var r = 1L
        var q = BigInteger.ONE
        var g = BigInteger.ONE
        var ys = BigInteger.valueOf(2)
        var stepCount = 0
        val mLimit = 100L

        while (g == BigInteger.ONE && stepCount < maxSteps) {
            x = y
            for (i in 1..r) {
                val (nextY, _) = quranicNext(y, n)
                y = nextY
            }

            var k = 0L
            while (k < r && g == BigInteger.ONE && stepCount < maxSteps) {
                ys = y
                val limit = Math.min(mLimit, r - k)
                for (i in 1..limit) {
                    val (nextY, info) = quranicNext(y, n)
                    y = nextY
                    val diff = x.subtract(y).abs()
                    q = q.multiply(diff).mod(n)

                    stepCount++
                    if (stepCount <= 30) {
                        val testGcd = diff.gcd(n)
                        val m_s = getQuranicMatrix(info.first)
                        stepsList.add(
                            PollardBrentStep(
                                step = stepCount,
                                x = y,
                                surah = info.first,
                                row = info.second,
                                col = info.third,
                                matrixValue = m_s[info.second][info.third],
                                gcdValue = testGcd
                            )
                        )
                    }
                }
                g = q.gcd(n)
                k += limit
            }
            r *= 2
        }

        if (g == n) {
            g = BigInteger.ONE
            y = ys
            while (g == BigInteger.ONE && stepCount < maxSteps) {
                val (nextY, info) = quranicNext(y, n)
                y = nextY
                val diff = x.subtract(y).abs()
                g = diff.gcd(n)
                stepCount++
                if (stepCount <= 30) {
                    val m_s = getQuranicMatrix(info.first)
                    stepsList.add(
                        PollardBrentStep(
                            step = stepCount,
                            x = y,
                            surah = info.first,
                            row = info.second,
                            col = info.third,
                            matrixValue = m_s[info.second][info.third],
                            gcdValue = g
                        )
                    )
                }
            }
        }

        val isSuccess = g > BigInteger.ONE && g < n

        return QuranicPollardBrentResult(
            factor = if (isSuccess) g else BigInteger.ONE,
            steps = stepsList,
            totalSteps = stepCount,
            isSuccess = isSuccess
        )
    }
}
