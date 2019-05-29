package com.example.testpool

import com.example.testpool.Utils.lightWorkProcess
import com.example.testpool.Utils.lightWorkProcessRx
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.Rule

/**
 */
class ManyParallelLightWorkload {

    companion object {
        val COUNT = 50000
        val CONCURRENCY = 8 //@@Todo: set to device core count

        /* Single thread coroutines */

        @Test
        fun single_map() {
            runBlocking(Dispatchers.Default) {
                (0..COUNT).map {
                    lightWorkProcess(it)
                }
            }
        }

        @Test     //Same as single thread, slow
        fun single_concurrentMap() {
            runBlocking(Dispatchers.Default) {
                (0..COUNT).concurrentMap {
                    lightWorkProcess(it)
                }
            }
        }

        /* Multithread coroutines */

        @Test   //fastest coroutines approach
        fun cr_parallelForEachAsync() {
            runBlocking(Dispatchers.Default) {
                (0..COUNT).parallelForEachAsync(scope = this, block = {
                    lightWorkProcess(it)
                })
            }
        }

        @Test
        fun cr_parallelMapAsync() {
            runBlocking(Dispatchers.Default) {
                (0..COUNT).parallelMapAsync(scope = this, block = {
                    lightWorkProcess(it)
                })
            }
        }

        @Test
        fun cr_parallelForEachLaunch() {
            runBlocking(Dispatchers.Default) {
                (0..COUNT).parallelForEachLaunch(scope = this, block = {
                    lightWorkProcess(it)
                })
            }
        }

        @Test   //slightly slower? dispatcher is better at managing parallelism
        fun cr_parallelForEach_limited() {
            runBlocking(Dispatchers.Default) {
                (0..COUNT).parallelForEachLimited(scope = this, block = { id: Int ->
                    lightWorkProcess(id)
                }, maxConcurrency = CONCURRENCY)
            }
        }

        @Test
        fun cr_parallelProduce_limited() {
            runBlocking(Dispatchers.Default) {
                (0..COUNT).parallelProduceLimited(scope = this, block = {
                    lightWorkProcess(it); it
                }, maxConcurrency = CONCURRENCY)
                    .consumeEach { }
            }
        }

        @Test
        fun cr_parallelProduceLimitedSynchronized() {
            runBlocking(Dispatchers.Default) {
                (0..COUNT).parallelProduceLimitedSynchronized(scope = this, block = {
                    lightWorkProcess(it); it
                }, maxConcurrency = CONCURRENCY)
                    .consumeEach { }
            }
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
    }

    class MultiTest {
        val ITERATIONS = 5

        @Rule @JvmField
        val globalTimeout = Timeout.seconds(60 * 2)

        @Test //Fastest coroutines
        fun cr_parallelForEachAsync_benchmark() {
            repeatBlock { ManyParallelLightWorkload.cr_parallelForEachAsync() }
        }

        @Test
        fun cr_parallelForEachLaunch_benchmark() {
            repeatBlock { ManyParallelLightWorkload.cr_parallelForEachLaunch() }
        }

        @Test
        fun cr_parallelForEach_limited_benchmark() {
            repeatBlock { ManyParallelLightWorkload.cr_parallelForEach_limited() }
        }

        @Test
        fun cr_parallelMapAsync_benchmark() {
            repeatBlock { ManyParallelLightWorkload.cr_parallelMapAsync() }
        }

        @Test
        fun cr_parallelProduce_limited_benchmark() {
            repeatBlock { ManyParallelLightWorkload.cr_parallelProduce_limited() }
        }

        @Test
        fun cr_parallelProduceLimitedSynchronized_benchmark() {
            repeatBlock { ManyParallelLightWorkload.cr_parallelProduceLimitedSynchronized() }
        }

        @Test
        fun rx_computation_benchmark() {
            repeatBlock { ManyParallelLightWorkload.rx_computation() }
        }

        @Test   //Fastest Rx
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
