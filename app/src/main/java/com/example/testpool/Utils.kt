package com.example.testpool

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

//Fastest
suspend fun Iterable<Int>.parallelForEach(
    scope: CoroutineScope = GlobalScope,
    block: suspend (Int) -> Any
) = map {
    scope.async(Dispatchers.Default) { block(it) }
}.forEach { it.await() }

//Limiting concurrency ends up being counterproductive
suspend fun Iterable<Int>.parallelForEachLimited(
    scope: CoroutineScope = GlobalScope,
    block: suspend (Int) -> Any,
    maxConcurrency: Int
) {
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

                }
                jobs[id] = job
            }
        }
    }
}

suspend fun <A, B> Iterable<A>.parallelMapFromProduceLimited(
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
        val job = scope.async {
            send(block(it))
        }
        job.invokeOnCompletion { jobs.remove(job) }
        jobs.add(job)
    }
}

//synchronized not necessary?
suspend fun <A> Iterable<Int>.parallelMapFromProduceLimitedSynchronized(
    scope: CoroutineScope = GlobalScope,
    block: suspend (Int) -> A,
    maxConcurrency: Int
) = scope.produce {

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
                }
                jobs[id] = job

            }
        }
    }
}