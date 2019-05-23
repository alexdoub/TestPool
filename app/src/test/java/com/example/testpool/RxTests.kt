package com.example.testpool

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.rx2.rxSingle
import org.junit.Test

import java.util.concurrent.TimeUnit

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class RxTests {

    companion object {
        val COUNT = CoroutineTests.COUNT
        val CONCURRENCY = CoroutineTests.CONCURRENCY
    }

    /** NOTES
     *
     * -
     * -
     * */

    @Test
    fun singleThread() {
        Observable.fromIterable((0..COUNT))
            .subscribe {
                runBlocking {
                    CoroutineTests.timedWorkProcess(it)
                }
            }
    }

    @Test
    fun limited_concurrentProcesses_work() {
        concurrentProcess(function = { CoroutineTests.timedWorkProcess(it) })
    }

    @Test
    fun limited_concurrentProcesses_sleep() {
        concurrentProcess(function = { CoroutineTests.sleepProcess(it) })
    }

    @Test
    fun limited_concurrentProcesses_delay() {
        concurrentProcess(function = { CoroutineTests.delayProcess(it) })
    }

    @Test
    fun default_concurrentProcesses_work() {
        concurrentProcess(function = { CoroutineTests.timedWorkProcess(it) }, limit = 999)
    }

    @Test
    fun default_concurrentProcesses_sleep() {
        concurrentProcess(function = { CoroutineTests.sleepProcess(it) }, limit = 999)
    }

    @Test
    fun default_concurrentProcesses_delay() {
        concurrentProcess(function = { CoroutineTests.delayProcess(it) }, limit = 999)
    }

    //NOTES: If the inner shit needs to be in a new thread, put the subscribeOn() there. It rotates every time its called
    private fun concurrentProcess(function: suspend (Int) -> (Int), limit: Int = CONCURRENCY) {
        val list: List<Int> = (0..COUNT).toList()
        val test = Observable.fromIterable(list)
            .flatMap ({id ->
                return@flatMap GlobalScope.rxSingle { function(id) }
                    .toObservable()
                    .subscribeOn(Schedulers.newThread())
            }, limit)
            .test()

        test.awaitDone(5, TimeUnit.MINUTES)
        test.assertValueCount(list.size)
    }
}
