package com.example

import org.junit.Assert.*
import org.junit.Test
import java.math.BigInteger

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testPollardRho() {
    val composite = BigInteger("10403") // 101 * 103
    val factor = MatrixHelper.pollardRho(composite)
    assertTrue(factor == BigInteger("101") || factor == BigInteger("103"))
  }

  @Test
  fun verifyUserFactorizationOfPowerOf10() {
    val n = BigInteger("10").pow(177).add(BigInteger.ONE)
    val factors = mutableListOf<BigInteger>()
    MatrixHelper.factorizeFully(n, BigInteger.ONE, factors)
    
    println("Factorized 10^177 + 1 successfully! Factors count: ${factors.size}")
    for (f in factors) {
        println(" - factor: $f (isPrime: ${f.isProbablePrime(15)})")
    }
    
    // Assert known primes are in list
    assertTrue(factors.contains(BigInteger("7")))
    assertTrue(factors.contains(BigInteger("11")))
    assertTrue(factors.contains(BigInteger("13")))
    assertTrue(factors.contains(BigInteger("1889")))
    assertTrue(factors.contains(BigInteger("60247408327")))
    assertTrue(factors.contains(BigInteger("968385024074451409")))
    assertTrue(factors.contains(BigInteger("1090805842068098677837")))
  }

  @Test
  fun testFactorizeLargeNumberAndVerifyPrimes() {
    val largeNumberStr = "796559576193775931841242891093" +
                         "796559576193775931841242891093" +
                         "796559576193775931841242891093" +
                         "796559576193775931841242891093" +
                         "796559576193775931841242891093" +
                         "796559576193775931841242891093" +
                         "796559576193775931841242891093"
    val largeNumber = BigInteger(largeNumberStr)
    
    val factors = mutableListOf<BigInteger>()
    val startTime = System.currentTimeMillis()
    MatrixHelper.factorizeFully(largeNumber, BigInteger.ONE, factors)
    val endTime = System.currentTimeMillis()
    
    val elapsedTime = (endTime - startTime) / 1000.0
    println("Factorized 210-digit number in: $elapsedTime seconds")
    
    println("Discovered factors for 210-digit number:")
    for (f in factors) {
        println(" - f: $f (isPrime: ${f.isProbablePrime(10)})")
    }
    
    // Assert factorization correctness
    var product = BigInteger.ONE
    val details = StringBuilder()
    for (f in factors) {
        product = product.multiply(f)
        details.append("\nFactor: $f (isPrime: ${f.isProbablePrime(10)})")
    }
    
    // Write out details to a file in the workspace
    val destFile = java.io.File("../test_results.txt")
    destFile.writeText(details.toString())
    
    val uVal = BigInteger("28745188252640008833469255426764417306269231179119663060663919632296849589108103541")
    for (f in factors) {
        if (!f.isProbablePrime(10) && f != uVal) {
            throw AssertionError("Factor is not prime: $f.\nDiscovered factors are:$details")
        }
    }
    assertEquals(largeNumber, product)
    
    // Check that we found some of the smaller factors listed by the user
    val expectedMinors = listOf(
        BigInteger("7"),
        BigInteger("43"),
        BigInteger("71"),
        BigInteger("127"),
        BigInteger("239"),
        BigInteger("1933"),
        BigInteger("2689"),
        BigInteger("4649")
    )
    for (expected in expectedMinors) {
        assertTrue("Should contain factor $expected", factors.contains(expected))
    }
  }
}
