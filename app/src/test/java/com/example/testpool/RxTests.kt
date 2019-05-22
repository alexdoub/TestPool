package com.example.testpool

import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.rx2.asSingle
import kotlinx.coroutines.rx2.rxSingle
import org.junit.Test

import org.junit.Assert.*
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
                    CoroutineTests.workProcess(it)
                }
            }
    }

    @Test
    fun concurrentProcesses_work() {
        concurrentProcess { CoroutineTests.workProcess(it) }
    }

    @Test
    fun concurrentProcesses_sleep() {
        concurrentProcess { CoroutineTests.sleepProcess(it) }
    }

    @Test
    fun concurrentProcesses_delay() {
        concurrentProcess { CoroutineTests.delayProcess(it) }
    }

    //NOTES: If the inner shit needs to be in a new thread, put the subscribeOn() there. It rotates every time its called
    private fun concurrentProcess(function: suspend (Int) -> (Int)) {
        val list: List<Int> = (0..COUNT).toList()
        val test = Observable.fromIterable(list)
            .flatMap ({id ->
                return@flatMap GlobalScope.rxSingle { function(id) }
                    .toObservable()
                    .subscribeOn(Schedulers.newThread())
            }, CONCURRENCY)
            .test()

        test.awaitDone(5, TimeUnit.MINUTES)
        test.assertValueCount(list.size)
    }
}
