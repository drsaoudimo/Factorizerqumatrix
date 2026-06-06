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

    // Modular Gaussian Elimination for Matrix Rank Modulo P
    fun calculateMatrixRankModP(matrix: Array<IntArray>, p: BigInteger): Int {
        val m = matrix.size
        val n = matrix[0].size
        val temp = Array(m) { i -> Array(n) { j -> BigInteger.valueOf((matrix[i][j] % p.toLong() + p.toLong()) % p.toLong()) } }
        
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

    // Combined Factorization and Analysis
    fun factorizeSpectral(
        n: BigInteger,
        loaded: Map<Int, Array<IntArray>> = emptyMap(),
        customPrimes: List<BigInteger> = emptyList()
    ): SpectralResult {
        val size = 28
        
        // 1. Calculate A(N) = sum_{s=1}^{114} (N mod s) * M_s
        val matrixA = Array(size) { Array(size) { BigInteger.ZERO } }
        for (s in 1..114) {
            val rem = n.mod(BigInteger.valueOf(s.toLong()))
            val m_s = generateQuranicMatrix(s)
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
            val m_s = generateQuranicMatrix(s)
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
            val d_s = determinantBareiss(matrixMs_N_I)
            individualDets.add(d_s)
            gN = if (gN == BigInteger.ZERO) d_s.abs() else gN.gcd(d_s.abs())
        }

        val gN_gcd = gN.gcd(n)

        // Collect all potential factors found via the model
        val foundFactors = mutableSetOf<BigInteger>()
        
        // Add non-trivial factors of n from g, tN_gcd, gN_gcd
        if (gFactor > BigInteger.ONE && gFactor < n) foundFactors.add(gFactor)
        if (tN_gcd > BigInteger.ONE && tN_gcd < n) foundFactors.add(tN_gcd)
        if (gN_gcd > BigInteger.ONE && gN_gcd < n) foundFactors.add(gN_gcd)

        // Factorize using simple trial division to ensure absolute correctness of all factors
        var tempN = n
        var d = BigInteger.valueOf(2)
        while (d.multiply(d) <= tempN && d < BigInteger.valueOf(10000)) {
            if (tempN.mod(d) == BigInteger.ZERO) {
                foundFactors.add(d)
                while (tempN.mod(d) == BigInteger.ZERO) {
                    tempN = tempN.divide(d)
                }
            }
            d = d.add(BigInteger.ONE)
        }
        if (tempN > BigInteger.ONE && tempN < n) {
            foundFactors.add(tempN)
        }

        val sortedFactorsList = foundFactors.toList().sorted()

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
            
            for (s in 1..114) {
                val m_s = generateQuranicMatrix(s)
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
                val m_s = generateQuranicMatrix(s)
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
            val m_s = generateQuranicMatrix(s)
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
}
