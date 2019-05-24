package com.example.testpool

import kotlinx.coroutines.delay
import java.math.BigInteger

object Utils {

    public suspend fun heavyWorkProcess(int: Int): Int {
        println("${int} Going to heavy work at ${System.currentTimeMillis()} with thread ${Thread.currentThread().name}")
        var number = BigInteger(int.toString())
        while (number.toString().length < 700000) {
            number = number.times(number).plus(BigInteger.ONE)
        }

//            println("done with iteration: ${int}, ${number.testBit(0)}")
        return int
    }

    public suspend fun timedWorkProcess(int: Int, delay: Int): Int {
        println("${int} Going to timed work at ${System.currentTimeMillis()} with thread ${Thread.currentThread().name}")
        val stopTime = System.currentTimeMillis() + delay
        while (System.currentTimeMillis() < stopTime) {
            var i: Long = 0;
            if (Math.random() > Math.random()) {
                i++
            }
        }
        return int
    }

    public suspend fun sleepProcess(int: Int, delay: Int): Int {
        println("${int} Going to sleep at ${System.currentTimeMillis()} with thread ${Thread.currentThread().name}")
        Thread.sleep(delay.toLong())
        println("${int} Done sleeping at ${System.currentTimeMillis()} with thread ${Thread.currentThread().name}")
        return int
    }

    public suspend fun delayProcess(int: Int, delay: Int): Int {
        println("${int} Delay at ${System.currentTimeMillis()} with thread ${Thread.currentThread().name}")
        delay(delay.toLong())
        return int
    }
}