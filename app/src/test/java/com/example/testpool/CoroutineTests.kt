package com.example.testpool

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.rx2.asCompletable
import kotlinx.coroutines.rx2.rxObservable
import kotlinx.coroutines.rx2.rxSingle
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import kotlin.coroutines.coroutineContext

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
                timedWorkProcess(it)
            }
        }
    }

    @Test
    fun defaultConcurrentProcesses_work() {
        runBlocking {
            (0..COUNT).toList().parallelMap {
                timedWorkProcess(it)
            }
        }
    }

    @Test
    fun defaultConcurrentProcesses_sleep() {
        runBlocking {
            (0..COUNT).toList().parallelMap {
                sleepProcess(it)
            }
        }
    }

    @Test
    fun defaultConcurrentProcesses_delay() {
        runBlocking {
            (0..COUNT).toList().parallelMap {
                delayProcess(it)
            }
        }
    }

    @Test
    fun limitedConcurrentProcesses_work() {
        runBlocking {
            (0..COUNT).toList().parallelMapLimited(block = {
                timedWorkProcess(it)
            }, maxConcurrency = CONCURRENCY)
        }
    }

    @Test
    fun limitedConcurrentProcesses_sleep() {
        runBlocking {
            (0..COUNT).toList().parallelMapLimited(block = {
                sleepProcess(it)
            }, maxConcurrency = CONCURRENCY)
        }
    }

    @Test
    fun limitedConcurrentProcesses_delay() {
        runBlocking {
            (0..COUNT).toList().parallelMapLimited(block = {
                delayProcess(it)
            }, maxConcurrency = CONCURRENCY)
        }
    }

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

    val ITEM_COUNT = 5000
    val BATCH_SIZE = 250
    val verbose = true
//    @Test
    fun mockMetadataSync() {
        val asyncTask = GlobalScope.async {
            println("w8")
            delay(1500)
            println("hi")

            doThis()
            doThatIn(this)
            doNotDoThis()
        }

        val ids = (0..ITEM_COUNT).map { "ID:${it}" }.chunked(BATCH_SIZE)
        val successfulBatches = ArrayList<BatchResult>()
        runBlocking {

            asyncTask.await()


            //Immediately executes after this line
            ids.parallelMapFromProduceLimited(scope = this, block = {
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

    private suspend fun fetchAndSyncBatch(list:List<String>): BatchResult {
        loggy("Fetching batch ${list.firstOrNull()} to ${list.lastOrNull()}")
        delay(500)
        loggy("Done Fetching batch ${list.firstOrNull()} to ${list.lastOrNull()}")
        return BatchResult(list)
    }

    private class BatchResult(val syncedIds: List<String>)

    private fun loggy(string: String) {
        if (verbose) {
            println(string)
        }
    }

//    @Test
    fun scopeTest() {
        val asyncTask = GlobalScope.async {
            println("hi")
//            doThis()
//            doThatIn(this)
            doNotDoThis()
        }

        runBlocking {
            asyncTask.await()
            println("asyncTask done")
        }
    }

    //This is bad because its not explicitly saying its making a scope
    suspend fun doNotDoThis() {
        CoroutineScope(coroutineContext).launch {
            delay(1000)
            println("done?")
        }
    }
    fun CoroutineScope.doThis() {
        launch {
            delay(400)
            println("I'm fine") }
    }

    fun doThatIn(scope: CoroutineScope) {
        scope.launch {
            delay(600)
            println("I'm fine, too") }
    }

    fun CHEAT_SHEET() {


        val sing = GlobalScope.rxSingle { }
        sing.blockingGet()

        if (GlobalScope.isActive) GlobalScope.cancel()

        runBlocking {
            launch { }

            val prod = produce {
                send("string")
            }
            val act = actor<String> {
                receive()
            }

            prod.consume {  }

            coroutineScope {
                rxObservable<String> { }

                val job = launch { rxSingle { } }
                job.invokeOnCompletion {}
                job.asCompletable(this.coroutineContext)
                return@coroutineScope null
            }

            val a = newCoroutineContext(this.coroutineContext)
            a
        }


    }

    //does cancelling parent context also cancel child?
//    @Test
    fun parent_cancel_child() {

        try {

            runBlocking {
                println("Before scope1")
//            val scope1 = coroutineScope {
                async {
                    var i = 0
                    while (true) {
                        delayProcess(i)
                        i++
//                        yield()
                    }
                }
//            }
                println("Past scope1")
                cancel(CancellationException("waddup"))

                val context2 = newCoroutineContext(this.coroutineContext)
                val scope2 = coroutineScope {
                    async(context2) {
                        var i = 0
                        while (true) {
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

    //    @Test
    fun fizzBuzz() {
        val calcuated = (1..100).map { number ->
            when {
                number % 15 == 0 -> "FizzBuzz"
                number % 5 == 0 -> "Buzz"
                number % 3 == 0 -> "Fizz"
                else -> number
            }
        }
            .toList()

        val answers = listOf(
            "1",
            "2",
            "Fizz",
            "4",
            "Buzz",
            "Fizz",
            "7",
            "8",
            "Fizz",
            "Buzz",
            "11",
            "Fizz",
            "13",
            "14",
            "FizzBuzz",
            "16",
            "17",
            "Fizz",
            "19",
            "Buzz",
            "Fizz",
            "22",
            "23",
            "Fizz",
            "Buzz",
            "26",
            "Fizz",
            "28",
            "29",
            "FizzBuzz",
            "31",
            "32",
            "Fizz",
            "34",
            "Buzz",
            "Fizz",
            "37",
            "38",
            "Fizz",
            "Buzz",
            "41",
            "Fizz",
            "43",
            "44",
            "FizzBuzz",
            "46",
            "47",
            "Fizz",
            "49",
            "Buzz",
            "Fizz",
            "52",
            "53",
            "Fizz",
            "Buzz",
            "56",
            "Fizz",
            "58",
            "59",
            "FizzBuzz",
            "61",
            "62",
            "Fizz",
            "64",
            "Buzz",
            "Fizz",
            "67",
            "68",
            "Fizz",
            "Buzz",
            "71",
            "Fizz",
            "73",
            "74",
            "FizzBuzz",
            "76",
            "77",
            "Fizz",
            "79",
            "Buzz",
            "Fizz",
            "82",
            "83",
            "Fizz",
            "Buzz",
            "86",
            "Fizz",
            "88",
            "89",
            "FizzBuzz",
            "91",
            "92",
            "Fizz",
            "94",
            "Buzz",
            "Fizz",
            "97",
            "98",
            "Fizz",
            "Buzz"
        )
        for (index in 0 until answers.size) {
            assert(calcuated[index] == calcuated[index])
        }
    }

    companion object {
        val COUNT = 30
        val CONCURRENCY = 4
        val WAIT_TIME_MS = 1000L

        public suspend fun timedWorkProcess(int: Int): Int {
            println("${int} Going to work at ${System.currentTimeMillis()} with thread ${Thread.currentThread().name}")
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
            println("${int} Going to sleep at ${System.currentTimeMillis()} with thread ${Thread.currentThread().name}")
            Thread.sleep(WAIT_TIME_MS)
            println("${int} Done sleeping at ${System.currentTimeMillis()} with thread ${Thread.currentThread().name}")
            return int
        }

        public suspend fun delayProcess(int: Int): Int {
            println("${int} Delay at ${System.currentTimeMillis()} with thread ${Thread.currentThread().name}")
            delay(WAIT_TIME_MS)
            return int
        }

    }
}
