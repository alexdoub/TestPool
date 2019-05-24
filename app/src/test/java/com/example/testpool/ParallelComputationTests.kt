package com.example.testpool

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import org.junit.Test
import java.math.BigInteger


class ParallelComputationTests {

    val iterations = 0..50000
    val digitLength = 2000
    val concurrency = 8

    @Test
    fun test1_single() {
        iterations.forEach {
            calculateHugeNumber(it)
        }
    }

    @Test
    fun test1_rx_computation_limited() {
        Observable.fromIterable(iterations)
            .flatMap({
                Observable.fromCallable { return@fromCallable calculateHugeNumber(it) }
                    .subscribeOn(Schedulers.computation())
            }, concurrency)
            .test().await()
    }

    @Test
    fun test1_rx_newthread_limited() {
        Observable.fromIterable(iterations)
            .flatMap({
                Observable.fromCallable { return@fromCallable calculateHugeNumber(it) }
                    .subscribeOn(Schedulers.newThread())
            }, concurrency)
            .test().await()
    }

    @Test
    fun test1_rx_computation_unlimited() {
        Observable.fromIterable(iterations)
            .flatMap({
                Observable.fromCallable { return@fromCallable calculateHugeNumber(it) }
                    .subscribeOn(Schedulers.computation())
            })
            .test().await()
    }

    @Test
    fun test1_rx_newthread_unlimited() {
        Observable.fromIterable(iterations)
            .flatMap({
                Observable.fromCallable{ return@fromCallable calculateHugeNumber(it) }
                    .subscribeOn(Schedulers.newThread())
            })
            .test().await()
    }

    @Test
    fun test1_coru_async_limited() {
        runBlocking {
            (iterations).toList().parallelMapLimited(this, {
                calculateHugeNumber(it)
            }, concurrency)
        }
    }

    @Test
    fun test1_coru_launch_limited() {
        runBlocking {
            (iterations).toList().parallelMapLimited(this, {
                calculateHugeNumber(it)
            }, concurrency)
        }
    }

    @Test
    fun test1_coru_async_unlimited() {
        runBlocking {
            (iterations).toList().parallelMap {
                calculateHugeNumber(it)
            }
        }
    }

    @Test
    fun test1_coru_launch_unlimited() {
        runBlocking {
            (iterations).toList().parallelMap {
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