package com.example.testpool

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.produce

/**
 * Notes:
 * When doing parallel, must use Dispatchers.Default.
 * If your worker is in the main group then its just going to share the same scheduler
 * */

suspend fun <A, B> Collection<A>.parallelMap(
    scope: CoroutineScope = GlobalScope,
    block: suspend (A) -> B
) = map {
    withContext(scope.coroutineContext) { block(it) }
}

suspend fun <A, B> Collection<A>.parallelMap(
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


suspend fun <A, B> Collection<A>.parallelForEach(
    scope: CoroutineScope = GlobalScope,
    block: suspend (A) -> B
) = map {
    scope.async(Dispatchers.Default) { block(it) }
}.forEach { it.await() }

suspend fun <A, B> Collection<A>.parallelForEach(
    scope: CoroutineScope = GlobalScope,
    block: suspend (A) -> B,
    maxConcurrency: Int
) {
    val jobs = ArrayList<Job>()
    forEach {
        while (jobs.size >= maxConcurrency) {
            yield()
        }
        val job = scope.async(Dispatchers.Default) { block(it) }
        job.invokeOnCompletion { jobs.remove(job) }
        jobs.add(job)
    }
}
