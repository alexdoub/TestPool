package com.example.testpool.coroutines

import com.example.testpool.Utils
import com.example.testpool.Utils.delayProcess
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.rx2.asCompletable
import kotlinx.coroutines.rx2.rxObservable
import kotlinx.coroutines.rx2.rxSingle
import org.junit.Test
import kotlin.coroutines.coroutineContext

class Random {

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
            println("I'm fine")
        }
    }

    fun doThatIn(scope: CoroutineScope) {
        scope.launch {
            delay(600)
            println("I'm fine, too")
        }
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
                val a = receive()
                println("got $a")
            }

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
                        Utils.delayProcess(i, 500)
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
                            Utils.delayProcess(i, 500)
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
}