package com.example.testpool

import android.util.Range
import kotlinx.coroutines.*
import kotlinx.coroutines.rx2.asCompletable
import kotlinx.coroutines.rx2.rxObservable
import kotlinx.coroutines.rx2.rxSingle
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import java.lang.Exception
import kotlin.coroutines.CoroutineContext

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class CoroutineTests {

    /** NOTES
     *
     * -Sleeping a process is the same as working it.
     * -Coroutines automatically utilizes threads. Like Rx calculation() scheduler
     * */

    @Before
    fun setup() {
        println("setup")
    }

    @Test
    fun singleThread() {
        runBlocking {
            (0..COUNT).toList().forEach {
                val a = workProcess(it)
            }
        }
    }

    @Test
    fun concurrentProcesses_work() {
        runBlocking {
            (0..COUNT).toList().parallelForEach {
                workProcess(it)
            }
        }
    }

    @Test
    fun concurrentProcesses_sleep() {
        runBlocking {
            (0..COUNT).toList().parallelForEach {
                sleepProcess(it)
            }
        }
    }

    @Test
    fun concurrentProcesses_delay() {
        runBlocking {
            (0..COUNT).toList().parallelForEach {
                delayProcess(it)
            }
        }
    }

    @Test
    fun parallelConcurrentProcesses_work() {
        runBlocking {
            (0..COUNT).toList().parallelForEach(this.coroutineContext, {
                workProcess(it)
            }, CONCURRENCY)
        }
    }

    @Test
    fun parallelConcurrentProcesses_sleep() {
        runBlocking {
            (0..COUNT).toList().parallelForEach(this.coroutineContext, {
                sleepProcess(it)
            }, CONCURRENCY)
        }
    }

    @Test
    fun parallelConcurrentProcesses_delay() {
        runBlocking {
            (0..COUNT).toList().parallelForEach(this.coroutineContext, {
                delayProcess(it)
            }, CONCURRENCY)
        }
    }

    fun derp() {


        val sing = GlobalScope.rxSingle {  }
        sing.blockingGet()

        if (GlobalScope.isActive) GlobalScope.cancel()

        runBlocking {
            launch {  }

            coroutineScope {
                rxObservable<String> {   }

                val job = launch { rxSingle {  } }
                job.invokeOnCompletion {}
                job.asCompletable(this.coroutineContext)
                return@coroutineScope  null
            }

            val a = newCoroutineContext(this.coroutineContext)
            a
        }


    }


    //does cancelling parent context also cancel child?
    @Test
    fun parent_cancel_child() {

        try {

        runBlocking {
            println("Before scope1")
//            val scope1 = coroutineScope {
                async{
                    var i = 0
                    while(true) {
                        delayProcess(i)
                        i++
//                        yield()
                    }
                }
//            }
            println("Past scope1")
//            cancel(CancellationException("waddup"))

            val context2 = newCoroutineContext(this.coroutineContext)
            val scope2 = coroutineScope {
                async(context2) {
                    var i = 0
                    while(true) {
                        delayProcess(i)
                        i++
//                        yield()
                    }
                }
            }


            println("Past scope2")
            delay(1000)
//            context2.cancel(CancellationException("Cheeze"))
            scope2.cancel(CancellationException("Whiz"))
            delay(1000)
            println("Proper finish!")
//            scope1.cancel()
            true
        }

        } catch (t: Throwable) {
            println("wut: ${t.message}")
        }
    }

    companion object {
        val COUNT = 20
        val CONCURRENCY = COUNT
        val WAIT_TIME_MS = 1000L

        public suspend fun workProcess(int: Int): Int {
            println("${int} Going to work at ${System.currentTimeMillis()} with thread ${Thread.currentThread().id}")
            val stopTime = System.currentTimeMillis() + WAIT_TIME_MS
            while (System.currentTimeMillis() < stopTime) {
                var i: Long = 0;
                if (Math.random() > Math.random()) {
                    i++
                }
            }
            return int
        }

        public suspend fun sleepProcess(int: Int): Int {
            println("${int} Going to sleep at ${System.currentTimeMillis()} with thread ${Thread.currentThread().id}")
            Thread.sleep(WAIT_TIME_MS)
            println("${int} Done sleeping at ${System.currentTimeMillis()} with thread ${Thread.currentThread().id}")
            return int
        }

        public suspend fun delayProcess(int: Int): Int {
            println("${int} Delay at ${System.currentTimeMillis()} with thread ${Thread.currentThread().id}")
            delay(WAIT_TIME_MS)
            return int
        }

    }
}
