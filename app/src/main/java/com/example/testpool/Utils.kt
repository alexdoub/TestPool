package com.example.testpool

import android.util.SparseArray
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.produce

/**
 * Notes:
 * When doing parallel, must use Dispatchers.Default.
 * If your worker is in the main group then its just going to share the same scheduler
 * */

suspend fun <A, B> Iterable<A>.concurrentMap(
    scope: CoroutineScope = GlobalScope,
    block: suspend (A) -> B
) = map {
    withContext(scope.coroutineContext) { block(it) }
}

//working
suspend fun Iterable<Int>.parallelForEach(
    scope: CoroutineScope = GlobalScope,
    block: suspend (Int) -> Any
) = map {
    scope.async(Dispatchers.Default) { block(it) }
}.forEach { it.await() }

//This is BROKEN for high collection sizes! after 10k+ iterations it stops
suspend fun Iterable<Int>.parallelForEachLimited(
    scope: CoroutineScope = GlobalScope,
    block: suspend (Int) -> Any,
    maxConcurrency: Int
) {
//    withContext(Dispatchers.Default) {
        val jobs = HashMap<Int, Job>()
        forEach { id ->
            var waiting = true
            while (waiting) {
                synchronized(jobs) {
                    waiting = jobs.size >= maxConcurrency
                }

                if (waiting) {
                    yield()
                } else {
                    val job = scope.async { block(id) }
                    job.invokeOnCompletion {
                        synchronized(jobs) {
                            jobs.remove(id)
                        }
//                    println("removed job ${id}. now has:${jobs.size}")
                    }
                    jobs[id] = job
//                println("started job:${id}. now has:${jobs.size}")
                }
            }
        }
//    }
}

//duz work??
suspend fun <A> Iterable<Int>.parallelMapFromProduceLimited(
    scope: CoroutineScope = GlobalScope,
    block: suspend (Int) -> A,
    maxConcurrency: Int
) = scope.produce {
//    withContext(Dispatchers.Default) {
        val jobs = HashMap<Int, Job>()
        forEach { id ->
            var waiting = true
            while (waiting) {
                synchronized(jobs) {
                    waiting = jobs.size >= maxConcurrency
                }
                if (waiting) {
                    yield()
                } else {
                    val job = scope.async {
                        val rval = block(id)
                        send(rval)
                    }
                    job.invokeOnCompletion {
                        synchronized(jobs) {
                            jobs.remove(id)
                        }
//                    println("removed job ${id}. now has:${jobs.size}")
                    }
                    jobs[id] = job
//                println("started job:${id}. now has:${jobs.size}")
                }
            }
        }
//    }
}

////BROKEN FOR HIGH ITERATIVE VALUES
suspend fun <A, B> Iterable<A>.parallelMapFromProduceLimitedOld(
    scope: CoroutineScope = GlobalScope,
    block: suspend (A) -> B,
    maxConcurrency: Int
) = scope.produce {

    val jobs = ArrayList<Job>()
    forEach {
        while (jobs.size >= maxConcurrency) {
            yield()
        }

        //Do NOT change scope here.
        //This async needs to be in ProducerScope (this)
        //Or else the parent scope wont know those jobs are running. (And resumes before they finished)
        val job = async(Dispatchers.Default) {
            send(block(it))
        }
        job.invokeOnCompletion { jobs.remove(job) }
        jobs.add(job)
    }
}
