package com.example.testpool

import io.reactivex.Observable
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.rx2.rxObservable
import java.math.BigInteger

object Utils {

    /** Work based functions */
    public suspend fun heavyWorkProcess(int: Int): Int {
        println("${int} Going to heavy work at ${System.currentTimeMillis()} with thread ${Thread.currentThread().name}")
        var number = BigInteger(int.toString())
        while (number.toString().length < 700000) {
            number = number.times(number).plus(BigInteger.ONE)
        }
        return int
    }

    public suspend fun lightWorkProcess(int: Int): Int {
        var number = BigInteger(int.toString())
        while (number.toString().length < 4000) {
            number = number.times(number).plus(BigInteger.ONE)
        }
        return int
    }

    public fun lightWorkProcessRx(int: Int): Observable<Int> {
        return GlobalScope.rxObservable { send(lightWorkProcess(int)) }
    }

    /** Delay based functions */

    public suspend fun delayProcess(int: Int, delay: Int): Int {
        println("${int} Delay at ${System.currentTimeMillis()} with thread ${Thread.currentThread().name}")
        delay(delay.toLong())
        return int
    }

    //same as delay?
    public suspend fun sleepProcess(int: Int, delay: Int): Int {
        println("${int} Going to sleep at ${System.currentTimeMillis()} with thread ${Thread.currentThread().name}")
        Thread.sleep(delay.toLong())
        println("${int} Done sleeping at ${System.currentTimeMillis()} with thread ${Thread.currentThread().name}")
        return int
    }

    //Same as delay, but eats up a thread in the meantime
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
}