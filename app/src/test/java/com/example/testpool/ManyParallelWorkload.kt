package com.example.testpool

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.rx2.rxObservable
import org.junit.Test
import java.math.BigInteger

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ManyParallelWorkload {

    @Test
    fun singleThread() {
        runBlocking(Dispatchers.Default) {
            (0..COUNT).toList().map {
                lightWorkProcess(it)
            }
            println("past operation")
        }
        println("past blocking")
    }

    //    @Test //Single thread
    fun map() {
        runBlocking(Dispatchers.Default) {
            (0..COUNT).toList().concurrentMap {
                lightWorkProcess(it)
            }
            println("past operation")
        }
        println("past blocking")
    }

    @Test
    fun forEach() {
        runBlocking(Dispatchers.Default) {
            (0..COUNT).toList().parallelForEach {
                lightWorkProcess(it)
            }
            println("past operation")
        }
        println("past blocking")
    }

    @Test
    fun forEach_limited() {
        runBlocking(Dispatchers.Default) {
            (0..COUNT).toList().parallelForEachLimited(block = { id: Int ->
                lightWorkProcess(id)
            }, maxConcurrency = CONCURRENCY)
            println("past operation")
        }
        println("past blocking")
    }


    companion object {
        val COUNT = 50000
        val CONCURRENCY = 8

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

    }

    @Test
    fun rx_computation() {
        Observable.fromIterable((0..COUNT))
            .flatMap({
                lightWorkProcessRx(it)
                    .subscribeOn(Schedulers.computation())
            })
            .test().await().assertValueCount(COUNT + 1)
    }

    @Test   //Always last
    fun rx_newthread() {
        Observable.fromIterable((0..COUNT))
            .flatMap({
                lightWorkProcessRx(it)
                    .subscribeOn(Schedulers.newThread())
            })
            .test().await().assertValueCount(COUNT + 1)
    }

    @Test   //Always #1
    fun rx_computation_limited() {
        Observable.fromIterable((0..COUNT))
            .flatMap({
                lightWorkProcessRx(it)
                    .subscribeOn(Schedulers.computation())
            }, CONCURRENCY)
            .test().await().assertValueCount(COUNT + 1)
    }

    @Test
    fun rx_newthread_limited() {
        Observable.fromIterable((0..COUNT))
            .flatMap({
                lightWorkProcessRx(it)
                    .subscribeOn(Schedulers.newThread())
            }, CONCURRENCY)
            .test().await().assertValueCount(COUNT + 1)
    }
}

