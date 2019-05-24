package com.example.testpool

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import org.junit.Test
import java.math.BigInteger


class ParallelComputationTests {

    val iterations = 0..50000
    val digitLength = 2000
    val concurrency = 8

    @Test
    fun singleThread() {
        iterations.forEach {
            calculateHugeNumber(it)
        }
    }

    @Test
    fun rx_computation() {
        Observable.fromIterable(iterations)
            .flatMap({
                Observable.fromCallable { return@fromCallable calculateHugeNumber(it) }
                    .subscribeOn(Schedulers.computation())
            })
            .test().await()
    }

    @Test   //Always last
    fun rx_newthread() {
        Observable.fromIterable(iterations)
            .flatMap({
                Observable.fromCallable{ return@fromCallable calculateHugeNumber(it) }
                    .subscribeOn(Schedulers.newThread())
            })
            .test().await()
    }

    @Test   //Always #1
    fun rx_computation_limited() {
        Observable.fromIterable(iterations)
            .flatMap({
                Observable.fromCallable { return@fromCallable calculateHugeNumber(it) }
                    .subscribeOn(Schedulers.computation())
            }, concurrency)
            .test().await()
    }

    @Test
    fun rx_newthread_limited() {
        Observable.fromIterable(iterations)
            .flatMap({
                Observable.fromCallable { return@fromCallable calculateHugeNumber(it) }
                    .subscribeOn(Schedulers.newThread())
            }, concurrency)
            .test().await()
    }

    @Test
    fun coroutines_async_limited() {
        runBlocking {
            (iterations).toList().parallelMapFromProduceLimited(this, {
                calculateHugeNumber(it); it
            }, concurrency)
                .consumeEach { println("got ${it}") }
        }
    }

    @Test
    fun coroutines_async() {
        runBlocking {
            (iterations).toList().parallelForEach {
                calculateHugeNumber(it)
            }
        }
    }

    @Test
    fun coroutines_launch() {
        runBlocking {
            (iterations).toList().parallelForEach {
                calculateHugeNumber(it)
            }
        }
    }

    //This can be substituted with any work operation of your choice
    private fun calculateHugeNumber(input: Int): BigInteger {
        var number = BigInteger(input.toString())
        while (number.toString().length < digitLength) {
            number = number.times(number).plus(BigInteger.ONE)
        }
//        println("done with iteration: ${input}")
        return number
    }

}