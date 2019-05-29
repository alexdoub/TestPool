package com.example.testpool

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import org.junit.Test
import java.math.BigInteger


class OldTests {

    val iterations = 0..5//00000
    val digitLength = 20//00
    val concurrency = 1
//
//    @Test
//    fun singleThread() {
//        iterations.forEach {
//            calculateHugeNumber(it)
//        }
//    }
//
//    @Test
//    fun rx_computation() {
//        Observable.fromIterable(iterations)
//            .flatMap({
//                Observable.fromCallable { return@fromCallable calculateHugeNumber(it) }
//                    .subscribeOn(Schedulers.computation())
//            })
//            .test().await()
//    }
//
//    @Test   //Always last
//    fun rx_newthread() {
//        Observable.fromIterable(iterations)
//            .flatMap({
//                Observable.fromCallable{ return@fromCallable calculateHugeNumber(it) }
//                    .subscribeOn(Schedulers.newThread())
//            })
//            .test().await()
//    }
//
//    @Test   //Always #1
//    fun rx_computation_limited() {
//        Observable.fromIterable(iterations)
//            .flatMap({
//                Observable.fromCallable { return@fromCallable calculateHugeNumber(it) }
//                    .subscribeOn(Schedulers.computation())
//            }, concurrency)
//            .test().await()
//    }
//
//    @Test
//    fun rx_newthread_limited() {
//        Observable.fromIterable(iterations)
//            .flatMap({
//                Observable.fromCallable { return@fromCallable calculateHugeNumber(it) }
//                    .subscribeOn(Schedulers.newThread())
//            }, concurrency)
//            .test().await()
//    }

    @Test   //Why is this broken?
    fun coroutines_async_limited() {
        val values = ArrayList<Int>()
        runBlocking(Dispatchers.Default) {
            (iterations).parallelMapFromProduceLimited(this, {
                calculateHugeNumber(it); it
            }, concurrency)
                .consumeEach {
                    values.add(it)
                    println("got ${it}")
                }
            println("Past block. Jobs finished: ${values.size}/${iterations.endInclusive}")
        }
        println("Test complete. Jobs finished: ${values.size}/${iterations.endInclusive}")
    }

    @Test
    fun limitedConcurrentProcesses_work() {
        val values = ArrayList<Int>()
        runBlocking(Dispatchers.Default) {
            (iterations).parallelForEachLimited(block = {
                calculateHugeNumber(it)
                values.add(it)
                println("got $it")
            }, maxConcurrency = concurrency)
            awaitAll<Int>()
            println("Past block. Jobs finished: ${values.size}/${iterations.endInclusive}")
        }
        println("Test complete. Jobs finished: ${values.size}/${iterations.endInclusive}")
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