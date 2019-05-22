package com.example.testpool

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.GlobalScope

/*
 * Copyright 2018 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

suspend fun <A, B> Collection<A>.parallelMap(
    context: CoroutineContext = GlobalScope.coroutineContext,
    block: suspend (A) -> B
) = map {
    GlobalScope.async(context) { block(it) }.await()
}

suspend fun <A, B> Collection<A>.parallelForEach(
    context: CoroutineContext = GlobalScope.coroutineContext,
    block: suspend (A) -> B
) = map {
    GlobalScope.async(context) { block(it) }
}.forEach { it.await() }

suspend fun <A, B> Collection<A>.parallelForEach(
    context: CoroutineContext = GlobalScope.coroutineContext,
    block: suspend (A) -> B,
    maxConcurrency: Int
) {
    val jobs = ArrayList<Job>()
    forEach {
        println("before job ${it}. it has ${jobs.size}")
        while (jobs.size >= maxConcurrency) {
//                println("yielding at ${System.currentTimeMillis()}")
            yield()
        }
        println("starting job ${it}. it has ${jobs.size}")
        val job = GlobalScope.async(context) { block(it) }
        job.invokeOnCompletion { jobs.remove(job) }
        jobs.add(job)
        println("added job ${it}. it has ${jobs.size}")
    }

}
