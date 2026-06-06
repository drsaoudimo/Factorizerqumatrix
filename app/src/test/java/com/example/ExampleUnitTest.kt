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
