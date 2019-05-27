package com.example.testpool

import com.example.testpool.Utils.lightWorkProcess
import com.example.testpool.Utils.lightWorkProcessRx
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.runBlocking
import org.junit.Test

/**
 */
class ManyParallelLightWorkload {

//    companion object {
        val COUNT = 50000
        val CONCURRENCY = 4

        /* Single thread coroutines */

        //    @Test
        fun single_map() {
            runBlocking(Dispatchers.Default) {
                (0..COUNT).map {
                    lightWorkProcess(it)
                }
            }
        }

        //    @Test     //Same as single thread, slow
        fun single_concurrentMap() {
            runBlocking(Dispatchers.Default) {
                (0..COUNT).concurrentMap {
                    lightWorkProcess(it)
                }
            }
        }

        /* Multithread coroutines */

        @Test   //fastest coroutines approach
        fun parallelForEach() { //fastest coroutine
            runBlocking(Dispatchers.Default) {
                (0..COUNT).parallelForEach {
                    lightWorkProcess(it)
                }
            }
        }

        @Test   //slightly slower? dispatcher is better at managing parallelism
        fun parallelForEach_limited() {
            runBlocking(Dispatchers.Default) {
                (0..COUNT).parallelForEachLimited(block = { id: Int ->
                    lightWorkProcess(id)
                }, maxConcurrency = CONCURRENCY)
            }
        }

        @Test
        fun parallelFromProducer_limited() {
            runBlocking(Dispatchers.Default) {
                (0..COUNT).parallelMapFromProduceLimited(scope = this, block = {
                    lightWorkProcess(it); it
                }, maxConcurrency = CONCURRENCY)
                    .consumeEach { println("got ${it}") }
                println("Past block")
            }
            println("end of test")
        }

        /* RX */

        @Test   //slightly slower than limited
        fun rx_computation() {
            Observable.fromIterable((0..COUNT))
                .flatMap({
                    lightWorkProcessRx(it)
                        .subscribeOn(Schedulers.computation())
                })
                .test().await()//.assertValueCount(COUNT + 1)
        }

        @Test   //Slowest Rx approach
        fun rx_newthread() {
            Observable.fromIterable((0..COUNT))
                .flatMap({
                    lightWorkProcessRx(it)
                        .subscribeOn(Schedulers.newThread())
                })
                .test().await()//.assertValueCount(COUNT + 1)
        }

        @Test   //Fastest Rx approach
        fun rx_computation_limited() {
            Observable.fromIterable((0..COUNT))
                .flatMap({
                    lightWorkProcessRx(it)
                        .subscribeOn(Schedulers.computation())
                }, CONCURRENCY)
                .test().await()//.assertValueCount(COUNT + 1)
        }

        @Test   //Much faster than unlimited. Almost the same as computation unlimited
        fun rx_newthread_limited() {
            Observable.fromIterable((0..COUNT))
                .flatMap({
                    lightWorkProcessRx(it)
                        .subscribeOn(Schedulers.newThread())
                }, CONCURRENCY)
                .test().await()//.assertValueCount(COUNT + 1)
        }
    }/**

    class MultiTest {
        val ITERATIONS = 5

        @Test //46.5, 46.6, 47.1
        fun parallelForEach_benchmark() {
            repeatBlock { ManyParallelLightWorkload.parallelForEach() }
        }

        @Test
        fun parallelForEach_limited_benchmark() {
            repeatBlock { ManyParallelLightWorkload.parallelForEach_limited() }
        }

        @Test   //60.6
        fun rx_computation_benchmark() {
            repeatBlock { ManyParallelLightWorkload.rx_computation() }
        }

        @Test   //44.5, 44.5, 44.9, 45.1
        fun rx_computation_limited_benchmark() {
            repeatBlock { ManyParallelLightWorkload.rx_computation_limited() }
        }

        @Test
        fun rx_newthread_benchmark() {
            repeatBlock { ManyParallelLightWorkload.rx_newthread() }
        }

        @Test
        fun rx_newthread_limited_benchmark() {
            repeatBlock { ManyParallelLightWorkload.rx_newthread_limited() }
        }

        fun repeatBlock(block: () -> (Unit)) {
            (1..ITERATIONS).forEach {
                val startTime = System.currentTimeMillis()
                block()
                val duration = System.currentTimeMillis() - startTime
                println("iteration $it finished in ${duration}ms")
            }
        }
    }
}
*/