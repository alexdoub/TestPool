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

suspend fun Iterable<Int>.parallelForEachAsync(
    scope: CoroutineScope = GlobalScope,
    block: suspend (Int) -> Any
) = map {
    scope.async(Dispatchers.Default) { block(it); }
}.forEach { it.await() }

suspend fun Iterable<Int>.parallelMapAsync(
    scope: CoroutineScope = GlobalScope,
    block: suspend (Int) -> Any
) = map {
    scope.async(Dispatchers.Default) { block(it); }
}.map { it.await() }

suspend fun Iterable<Int>.parallelForEachLaunch(
    scope: CoroutineScope = GlobalScope,
    block: suspend (Int) -> Any
) = map {
    scope.launch(Dispatchers.Default) { block(it); }
}.forEach { it.join() }

//Limiting concurrency ends up being counterproductive
suspend fun Iterable<Int>.parallelForEachLaunchLimited(
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
                val job = scope.launch { block(id) }
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

suspend fun <A, B> Iterable<A>.parallelProduceLaunchLimited(
    scope: CoroutineScope = GlobalScope,
    block: suspend (A) -> B,
    maxConcurrency: Int
) = scope.produce {

    val jobs = HashMap<Int, Job>()
    forEach {
        while (jobs.size >= maxConcurrency) {
            yield()
        }
        val job = scope.launch {
            send(block(it))
        }
        job.invokeOnCompletion { jobs.remove(job.hashCode()) }
        jobs[job.hashCode()] = job
    }
//    awaitAll<Any>() //no effect
//    joinAll() //no effect
    while (jobs.isNotEmpty()) {
        yield()
    }
}

//synchronized not necessary?
suspend fun <A, B> Iterable<A>.parallelProduceLaunchSynchronizedLimited(
    scope: CoroutineScope = GlobalScope,
    block: suspend (A) -> B,
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
                val job = scope.launch {
                    val rval = block(id)
                    send(rval)
                }
                job.invokeOnCompletion {
                    synchronized(jobs) {
                        jobs.remove(job.hashCode())
                    }
                }
                jobs[job.hashCode()] = job
            }
        }
    }
//    awaitAll<Any>() //no effect
//    joinAll() //no effect
    while (jobs.isNotEmpty()) {
        yield()
    }
}