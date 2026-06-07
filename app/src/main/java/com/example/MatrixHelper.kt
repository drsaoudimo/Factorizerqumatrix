package com.example

import java.util.Random
import kotlin.math.*

data class MatrixFeatures(
    val index: Int,
    val rank: Int,
    val trace: Double,
    val determinant: Double,
    val largestEigenvalue: Double,
    val smallestEigenvalue: Double,
    val sparsity: Double,
    val frobeniusNorm: Double
)

class EigenResult(val eigenvalues: DoubleArray, val eigenvectors: Array<DoubleArray>)

object MatrixHelper {

    val SURAH_VERSES = intArrayOf(
        7, 286, 200, 176, 120, 165, 206, 75, 129, 109,
        123, 111, 43, 52, 99, 128, 111, 110, 98, 135,
        112, 78, 118, 64, 77, 227, 93, 88, 69, 60,
        34, 30, 73, 54, 45, 83, 182, 88, 75, 85,
        54, 53, 89, 59, 37, 35, 38, 29, 18, 45,
        60, 49, 62, 55, 78, 96, 29, 22, 24, 13,
        14, 11, 11, 18, 12, 12, 30, 52, 52, 44,
        28, 28, 20, 56, 40, 31, 50, 40, 46, 42,
        29, 19, 36, 25, 22, 17, 19, 26, 30, 20,
        15, 21, 11, 8, 8, 19, 5, 8, 8, 11,
        11, 8, 3, 9, 5, 4, 7, 3, 6, 3,
        5, 4, 5, 6
    )

    val A1: Array<IntArray> = Array(19) { row ->
        IntArray(6) { col ->
            val idx = row * 6 + col
            if (idx < 114) SURAH_VERSES[idx] else 0
        }
    }

    fun getQuranicMatrix(s: Int): Array<IntArray> {
        val verses = SURAH_VERSES[(s - 1).coerceIn(0, 113)]
        val r = Random((s * 114 + verses).toLong())
        val matrix = Array(28) { IntArray(28) { 0 } }
        
        val totalSum = verses * 12 + 40
        var currentSum = 0
        while (currentSum < totalSum) {
            val i = r.nextInt(28)
            val j = r.nextInt(28)
            if (r.nextDouble() < 0.4) {
                val weight = r.nextInt(3) + 1
                matrix[i][j] += weight
                if (i != j) {
                    matrix[j][i] += weight
                }
                currentSum += weight * 2
            }
        }
        for (i in 0 until 28) {
            matrix[i][i] += r.nextInt(4) + 1
        }
        return matrix
    }

    fun jacobiComplete(matrix: Array<DoubleArray>): EigenResult {
        val n = matrix.size
        val a = Array(n) { i -> matrix[i].clone() }
        val d = DoubleArray(n) { i -> a[i][i] }
        val v = Array(n) { i -> DoubleArray(n) { j -> if (i == j) 1.0 else 0.0 } }
        
        val maxIterations = 300
        val eps = 1e-9
        
        for (iter in 0 until maxIterations) {
            var maxVal = 0.0
            var p = 0
            var q = 0
            for (i in 0 until n - 1) {
                for (j in i + 1 until n) {
                    val absVal = abs(a[i][j])
                    if (absVal > maxVal) {
                        maxVal = absVal
                        p = i
                        q = j
                    }
                }
            }
            
            if (maxVal < eps) break
            
            val ap = a[p][p]
            val aq = a[q][q]
            val apq = a[p][q]
            
            val tau = (aq - ap) / (2.0 * apq)
            val t = if (tau >= 0) {
                1.0 / (tau + sqrt(1.0 + tau * tau))
            } else {
                -1.0 / (-tau + sqrt(1.0 + tau * tau))
            }
            val c = 1.0 / sqrt(1.0 + t * t)
            val s = t * c
            
            a[p][p] = ap - t * apq
            a[q][q] = aq + t * apq
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
            
            for (i in 0 until n) {
                val vip = v[i][p]
                val viq = v[i][q]
                v[i][p] = c * vip - s * viq
                v[i][q] = s * vip + c * viq
            }
        }
        
        val eigenvalues = DoubleArray(n) { i -> a[i][i] }
        return EigenResult(eigenvalues, v)
    }

    fun computeMatrixProperties(s: Int): MatrixFeatures {
        val m = getQuranicMatrix(s)
        val doubleMatrix = Array(28) { i -> DoubleArray(28) { j -> m[i][j].toDouble() } }
        
        var sumSq = 0.0
        var zeroCount = 0
        var trace = 0.0
        for (i in 0 until 28) {
            trace += doubleMatrix[i][i]
            for (j in 0 until 28) {
                val v = doubleMatrix[i][j]
                sumSq += v * v
                if (m[i][j] == 0) {
                    zeroCount++
                }
            }
        }
        val frobeniusNorm = sqrt(sumSq)
        val sparsity = zeroCount.toDouble() / 784.0
        
        val eigenResult = jacobiComplete(doubleMatrix)
        val eigenvalues = eigenResult.eigenvalues.sorted()
        
        val largestEigenvalue = eigenvalues.last()
        val smallestEigenvalue = eigenvalues.first()
        
        var determinant = 1.0
        var rank = 0
        for (lam in eigenvalues) {
            determinant *= lam
            if (abs(lam) > 1e-4) {
                rank++
            }
        }
        
        return MatrixFeatures(
            index = s,
            rank = rank,
            trace = trace,
            determinant = determinant,
            largestEigenvalue = largestEigenvalue,
            smallestEigenvalue = smallestEigenvalue,
            sparsity = sparsity,
            frobeniusNorm = frobeniusNorm
        )
    }

    fun reduceSurahMatrixUsingA1(s: Int): Array<DoubleArray> {
        val m_s = getQuranicMatrix(s)
        val result = Array(6) { DoubleArray(6) { 0.0 } }
        val intermediate = Array(19) { DoubleArray(6) { 0.0 } }
        
        for (i in 0 until 19) {
            for (j in 0 until 6) {
                var sum = 0.0
                for (k in 0 until 19) {
                    sum += m_s[i][k].toDouble() * A1[k][j].toDouble()
                }
                intermediate[i][j] = sum
            }
        }
        
        for (i in 0 until 6) {
            for (j in 0 until 6) {
                var sum = 0.0
                for (k in 0 until 19) {
                    sum += A1[k][i].toDouble() * intermediate[k][j]
                }
                result[i][j] = sum
            }
        }
        return result
    }

    fun computeSimilarityMatrix(): Array<DoubleArray> {
        val norms = DoubleArray(114)
        val flattenedMatrices = Array(114) { DoubleArray(784) }
        
        for (s in 1..114) {
            val m = getQuranicMatrix(s)
            var sumSq = 0.0
            var idx = 0
            for (i in 0 until 28) {
                for (j in 0 until 28) {
                    val v = m[i][j].toDouble()
                    flattenedMatrices[s - 1][idx++] = v
                    sumSq += v * v
                }
            }
            norms[s - 1] = sqrt(sumSq)
        }
        
        val similarity = Array(114) { DoubleArray(114) }
        for (i in 0 until 114) {
            similarity[i][i] = 1.0
            for (j in i + 1 until 114) {
                var dotProduct = 0.0
                for (k in 0 until 784) {
                    dotProduct += flattenedMatrices[i][k] * flattenedMatrices[j][k]
                }
                val nI = norms[i]
                val nJ = norms[j]
                val sim = if (nI > 0 && nJ > 0) dotProduct / (nI * nJ) else 0.0
                similarity[i][j] = sim
                similarity[j][i] = sim
            }
        }
        return similarity
    }

    fun kMeans(data: Array<DoubleArray>, k: Int, maxIterations: Int = 100): IntArray {
        val n = data.size
        val dim = data[0].size
        val assignments = IntArray(n) { 0 }
        
        val r = Random(42)
        val centroids = Array(k) { DoubleArray(dim) }
        for (i in 0 until k) {
            centroids[i] = data[r.nextInt(n)].clone()
        }
        
        for (iter in 0 until maxIterations) {
            var changed = false
            for (i in 0 until n) {
                var minDist = Double.MAX_VALUE
                var bestCluster = 0
                for (c in 0 until k) {
                    var dist = 0.0
                    for (d in 0 until dim) {
                        val diff = data[i][d] - centroids[c][d]
                        dist += diff * diff
                    }
                    if (dist < minDist) {
                        minDist = dist
                        bestCluster = c
                    }
                }
                if (assignments[i] != bestCluster) {
                    assignments[i] = bestCluster
                    changed = true
                }
            }
            
            if (!changed) break
            
            val counts = IntArray(k) { 0 }
            val sums = Array(k) { DoubleArray(dim) { 0.0 } }
            for (i in 0 until n) {
                val c = assignments[i]
                counts[c]++
                for (d in 0 until dim) {
                    sums[c][d] += data[i][d]
                }
            }
            for (c in 0 until k) {
                if (counts[c] > 0) {
                    for (d in 0 until dim) {
                        centroids[c][d] = sums[c][d] / counts[c]
                    }
                }
            }
        }
        return assignments
    }

    fun spectralClustering(k: Int): Pair<IntArray, DoubleArray> {
        val S = computeSimilarityMatrix()
        val n = S.size
        
        val deg = DoubleArray(n)
        for (i in 0 until n) {
            var sum = 0.0
            for (j in 0 until n) {
                sum += S[i][j]
            }
            deg[i] = sum
        }
        
        val Anorm = Array(n) { DoubleArray(n) }
        for (i in 0 until n) {
            for (j in 0 until n) {
                val dI = deg[i]
                val dJ = deg[j]
                Anorm[i][j] = if (dI > 0 && dJ > 0) S[i][j] / (sqrt(dI) * sqrt(dJ)) else 0.0
            }
        }
        
        val eigenRes = jacobiComplete(Anorm)
        val eigenpairs = List(n) { i -> Pair(eigenRes.eigenvalues[i], eigenRes.eigenvectors[i]) }
            .sortedByDescending { it.first }
        
        val U = Array(n) { DoubleArray(k) }
        for (i in 0 until n) {
            for (j in 0 until k) {
                U[i][j] = eigenpairs[j].second[i]
            }
        }
        
        val Y = Array(n) { DoubleArray(k) }
        for (i in 0 until n) {
            var rowSumSq = 0.0
            for (j in 0 until k) {
                rowSumSq += U[i][j] * U[i][j]
            }
            val rowNorm = sqrt(rowSumSq)
            for (j in 0 until k) {
                Y[i][j] = if (rowNorm > 0) U[i][j] / rowNorm else 0.0
            }
        }
        
        val clusters = kMeans(Y, k)
        val sortedAllEigenvalues = eigenRes.eigenvalues.sortedDescending().toDoubleArray()
        return Pair(clusters, sortedAllEigenvalues)
    }

    fun computeRandomSimilarityMatrix(): Array<DoubleArray> {
        val r = Random(777)
        val norms = DoubleArray(114)
        val flattenedMatrices = Array(114) { DoubleArray(784) }
        
        for (s in 1..114) {
            val verses = SURAH_VERSES[s - 1]
            var sumSq = 0.0
            var idx = 0
            for (i in 0 until 28) {
                for (j in 0 until 28) {
                    val v = if (r.nextDouble() < 0.35) (r.nextInt(3) + 1).toDouble() else 0.0
                    flattenedMatrices[s - 1][idx++] = v
                    sumSq += v * v
                }
            }
            norms[s - 1] = sqrt(sumSq)
        }
        
        val similarity = Array(114) { DoubleArray(114) }
        for (i in 0 until 114) {
            similarity[i][i] = 1.0
            for (j in i + 1 until 114) {
                var dotProduct = 0.0
                for (k in 0 until 784) {
                    dotProduct += flattenedMatrices[i][k] * flattenedMatrices[j][k]
                }
                val nI = norms[i]
                val nJ = norms[j]
                val sim = if (nI > 0 && nJ > 0) dotProduct / (nI * nJ) else 0.0
                similarity[i][j] = sim
                similarity[j][i] = sim
            }
        }
        return similarity
    }

    fun computeGlobalHamiltonian(weights: DoubleArray): Array<DoubleArray> {
        val H = Array(28) { DoubleArray(28) { 0.0 } }
        for (s in 1..114) {
            val alpha = weights[s - 1]
            if (alpha == 0.0) continue
            val m = getQuranicMatrix(s)
            for (i in 0 until 28) {
                for (j in 0 until 28) {
                    H[i][j] += alpha * m[i][j].toDouble()
                }
            }
        }
        return H
    }

    fun analyzeHamiltonian(weights: DoubleArray): Triple<DoubleArray, Double, Double> {
        val H = computeGlobalHamiltonian(weights)
        val eigenRes = jacobiComplete(H)
        val eigenvalues = eigenRes.eigenvalues.sorted()
        
        // condition number
        val maxAbs = eigenvalues.map { abs(it) }.maxOrNull() ?: 1.0
        val minAbs = eigenvalues.filter { abs(it) > 1e-4 }.map { abs(it) }.minOrNull() ?: 1.0
        val condNo = if (minAbs > 0) maxAbs / minAbs else 1.0
        
        // entropy
        val energySums = eigenvalues.map { it * it }.sum()
        var entropy = 0.0
        if (energySums > 0) {
            for (lam in eigenvalues) {
                val p = (lam * lam) / energySums
                if (p > 1e-9) {
                    entropy -= p * ln(p)
                }
            }
        }
        return Triple(eigenvalues.toDoubleArray(), entropy, condNo)
    }

    fun computeClusterSimilarityMeasure(clusters: IntArray, k: Int): DoubleArray {
        val S = computeSimilarityMatrix()
        val n = S.size
        val avgSim = DoubleArray(k) { 0.0 }
        val counts = IntArray(k) { 0 }
        
        for (i in 0 until n) {
            val c = clusters[i]
            counts[c]++
        }
        
        for (c in 0 until k) {
            if (counts[c] < 2) continue
            var sum = 0.0
            var pairs = 0
            for (i in 0 until n) {
                if (clusters[i] != c) continue
                for (j in i + 1 until n) {
                    if (clusters[j] != c) continue
                    sum += S[i][j]
                    pairs++
                }
            }
            avgSim[c] = if (pairs > 0) sum / pairs else 0.0
        }
        return avgSim
    }
}
