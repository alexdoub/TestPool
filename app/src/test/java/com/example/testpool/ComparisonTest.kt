package com.example.testpool

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import org.junit.Test
import java.math.BigInteger


class ComparisonTest {

    val iterations = 0..50000
    val digitLength = 2000
    val maxConcurrency = 8

    @Test
    fun test1_single() {
        iterations.forEach {
            calculateHugeNumber(it)
        }
    }

    @Test
    fun test1_rx_computation() {
        Observable.fromIterable(iterations)
            .flatMap({
                Observable.fromCallable { return@fromCallable calculateHugeNumber(it) }
                    .subscribeOn(Schedulers.computation())
            }, maxConcurrency)
            .test().await()
    }

    @Test
    fun test1_rx_newthread() {
        Observable.fromIterable(iterations)
            .flatMap({
                Observable.fromCallable { return@fromCallable calculateHugeNumber(it) }
                    .subscribeOn(Schedulers.newThread())
            }, maxConcurrency)
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
    fun test1_coru_async_unlimited() {
        runBlocking {
            (iterations).forEach {
                async {
                    calculateHugeNumber(it)
                }
            }
        }
    }

    @Test
    fun test1_coru_launch_unlimited() {
        runBlocking {
            (iterations).forEach {
                launch {
                    calculateHugeNumber(it)
                }
            }
        }
    }

    //This can be substituted with any work operation of your choice
    private fun calculateHugeNumber(input: Int): BigInteger {
        var number = BigInteger(input.toString())
        while (number.toString().length < digitLength) {
            number = number.times(number).plus(BigInteger.ONE)
        }
        return number
    }

}