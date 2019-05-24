//package com.example.testpool
//
//import io.reactivex.Observable
//import io.reactivex.schedulers.Schedulers
//import kotlinx.coroutines.*
//import org.junit.Test
//import java.math.BigInteger
//
//
//class FamTests {
//
//    val WORKLOAD_ITERATIONS = 0..50000
//    val WORKLOAD_DIGIT_LENGTH = 2000
//    val THREAD_COUNT = 8
//
////    @Test
//    fun test1_single() {
//        WORKLOAD_ITERATIONS.forEach {
//            calculateHugeNumber(it)
//        }
//    }
//
//    @Test
//    fun test1_rx_computation_limited() {
//        Observable.fromIterable(WORKLOAD_ITERATIONS)
//            .flatMap({
//                Observable.fromCallable { return@fromCallable calculateHugeNumber(it) }
//                    .subscribeOn(Schedulers.computation())
//            }, THREAD_COUNT)
//            .test().await()
//    }
//
//
//    @Test
//    fun test1_coru_async_limited() {
//        runBlocking {
//            (WORKLOAD_ITERATIONS).toList().limitedParallelForEach(this, {
//                calculateHugeNumber(it)
//            }, THREAD_COUNT)
//        }
//    }
//
//    @Test
//    fun test1_coru_launch_limited() {
//        runBlocking {
//            (WORKLOAD_ITERATIONS).toList().limitedParallelForEach(this, {
//                calculateHugeNumber(it)
//            }, THREAD_COUNT)
//        }
//    }
//
//    @Test
//    fun test1_coru_async() {
//        runBlocking {
//            (WORKLOAD_ITERATIONS).forEach {
//                async(Dispatchers.Default) {
//                    calculateHugeNumber(it)
//                }
//            }
//        }
//    }
//
//    @Test
//    fun test1_coru_launch() {
//        runBlocking {
//            (WORKLOAD_ITERATIONS).forEach {
//                launch(Dispatchers.Default) {
//                    calculateHugeNumber(it)
//                }
//            }
//        }
//    }
//
//    suspend fun <A, B> Collection<A>.limitedParallelForEach(
//        scope: CoroutineScope = GlobalScope,
//        block: suspend (A) -> B,
//        maxConcurrency: Int
//    ) {
//        val jobs = ArrayList<Job>()
//        forEach {
//            while (jobs.size >= maxConcurrency) {
//                yield()
//            }
//            val job = scope.async(Dispatchers.Default) { block(it) }
//            job.invokeOnCompletion { jobs.remove(job) }
//            jobs.add(job)
//        }
//    }
//
//
//    //This can be substituted with any work operation of your choice
//    private fun calculateHugeNumber(input: Int): BigInteger {
//        var number = BigInteger(input.toString())
//        while (number.toString().length < WORKLOAD_DIGIT_LENGTH) {
//            number = number.times(number).plus(BigInteger.ONE)
//        }
//        return number
//    }
//
//}