package com.example.testpool

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.rx2.rxObservable
import kotlinx.coroutines.rx2.rxSingle
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */


class ParallelWorkload {

    private class BatchResult(val syncedIds: List<String>)

    protected companion object {
        val COUNT = 30
        val CONCURRENCY = 4
        val WAIT_TIME_MS = 1000
    }

    /** NOTES
     *
     * -Sleeping a process is the same as working it.
     * -Coroutines automatically utilizes threads. Like Rx calculation() scheduler
     * */

    @Before
    fun setup() {
        println("setup")
    }

    class CoroutineTests {

        @Test
        fun coroutines_singleThread() {
            runBlocking {
                (0..COUNT).forEach {
                    Utils.timedWorkProcess(it, WAIT_TIME_MS)
                }
            }
        }

        @Test
        fun defaultConcurrentProcesses_work() {
            runBlocking {
                (0..COUNT).parallelForEach {
                    Utils.heavyWorkProcess(it)
                }
            }
        }

        @Test
        fun defaultConcurrentProcesses_timed() {
            runBlocking {
                (0..COUNT).parallelForEach {
                    Utils.timedWorkProcess(it, WAIT_TIME_MS)
                }
            }
        }

        @Test
        fun defaultConcurrentProcesses_sleep() {
            runBlocking {
                (0..COUNT).parallelForEach {
                    Utils.sleepProcess(it, WAIT_TIME_MS)
                }
            }
        }

        @Test
        fun defaultConcurrentProcesses_delay() {
            runBlocking {
                (0..COUNT).parallelForEach {
                    Utils.delayProcess(it, WAIT_TIME_MS)
                }
            }
        }

        @Test
        fun limitedConcurrentProcesses_work() {
            val values = ArrayList<Int>()
            runBlocking {
                (0..COUNT).parallelForEachLimited(block = {
                    val v = Utils.heavyWorkProcess(it)
                    values.add(v)
                }, maxConcurrency = CONCURRENCY)
                awaitAll<Int>()
            }
            println("Test complete. Jobs finished: ${values.size}/$COUNT")
        }

        @Test
        fun limitedConcurrentProcesses_timed() {
            runBlocking {
                (0..COUNT).parallelForEachLimited(block = {
                    Utils.timedWorkProcess(it, WAIT_TIME_MS)
                }, maxConcurrency = CONCURRENCY)
            }
        }

        @Test
        fun limitedConcurrentProcesses_sleep() {
            runBlocking {
                (0..COUNT).parallelForEachLimited(block = {
                    Utils.sleepProcess(it, WAIT_TIME_MS)
                }, maxConcurrency = CONCURRENCY)
            }
        }

        @Test
        fun limitedConcurrentProcesses_delay() {
            runBlocking {
                (0..COUNT).parallelForEachLimited(block = {
                    Utils.delayProcess(it, WAIT_TIME_MS)
                }, maxConcurrency = CONCURRENCY)
            }
        }
    }

    class RxTests {

        //    @Test
        fun rxObservable() {
            runBlocking {
                val observable = rxObservable {
                    println("in here")
                    (0..100).forEach {
                        send(it)
                    }
                }
                observable.subscribe { println("got ${it}") }
            }
        }

        @Test
        fun rx_singleThread() {
            Observable.fromIterable((0..COUNT))
                .subscribe {
                    runBlocking {
                        Utils.timedWorkProcess(it, WAIT_TIME_MS)
                    }
                }
        }

        @Test
        fun limited_concurrentProcesses_work() {
            concurrentProcess(function = { Utils.timedWorkProcess(it, WAIT_TIME_MS) })
        }

        @Test
        fun limited_concurrentProcesses_sleep() {
            concurrentProcess(function = { Utils.sleepProcess(it, WAIT_TIME_MS) })
        }

        @Test
        fun limited_concurrentProcesses_delay() {
            concurrentProcess(function = { Utils.delayProcess(it, WAIT_TIME_MS) })
        }

        @Test
        fun default_concurrentProcesses_work() {
            concurrentProcess(function = { Utils.timedWorkProcess(it, WAIT_TIME_MS) }, limit = 999)
        }

        @Test
        fun default_concurrentProcesses_sleep() {
            concurrentProcess(function = { Utils.sleepProcess(it, WAIT_TIME_MS) }, limit = 999)
        }

        @Test
        fun default_concurrentProcesses_delay() {
            concurrentProcess(function = { Utils.delayProcess(it, WAIT_TIME_MS) }, limit = 999)
        }

        //NOTES: If the inner shit needs to be in a new thread, put the subscribeOn() there. It rotates every time its called
        private fun concurrentProcess(function: suspend (Int) -> (Int), limit: Int = CONCURRENCY) {
            val list = (0..COUNT)
            val test = Observable.fromIterable(list)
                .flatMap({ id ->
                    return@flatMap GlobalScope.rxSingle { function(id) }
                        .toObservable()
                        .subscribeOn(Schedulers.newThread())
                }, limit)
                .test()

            test.awaitDone(5, TimeUnit.MINUTES)
            test.assertValueCount(list.count())
        }
    }

    class MockSync {

        val ITEM_COUNT = 5000
        val BATCH_SIZE = 250
        val verbose = true
        @Test
        fun mockMetadataSync() {
            val ids = (0..ITEM_COUNT).map { "ID:${it}" }.chunked(BATCH_SIZE)
            val successfulBatches = ArrayList<BatchResult>()
            runBlocking {

                //Immediately executes after this line
                ids.parallelMapFromProduceLimitedOld(scope = this, block = {
                    fetchAndSyncBatch(it)
                }, maxConcurrency = CONCURRENCY)
                    .consumeEach {
                        successfulBatches.add(it)
                    }

                loggy("end of blocking. ids: ${ids.size} batch results: ${successfulBatches.size}")
            }
            loggy("before assert. ids: ${ids.size} batch results: ${successfulBatches.size}")
            assertEquals(ids.size, successfulBatches.size)
        }

        private suspend fun fetchAndSyncBatch(list: List<String>): BatchResult {
            loggy("Fetching batch ${list.firstOrNull()} to ${list.lastOrNull()}")
            delay(WAIT_TIME_MS.toLong())
            loggy("Done Fetching batch ${list.firstOrNull()} to ${list.lastOrNull()}")
            return BatchResult(list)
        }

        private fun loggy(string: String) {
            if (verbose) {
                println(string)
            }
        }
    }
}