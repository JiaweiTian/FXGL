package com.almasb.fxgl.core.concurrent

import javafx.application.Platform
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch

/**
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */

class Coroutine<T>(private val func: Callable<T>) : Async<T>() {

    private val deferred: Deferred<T> = async(CommonPool) {
        func.call()
    }

    override fun await() = runBlocking {
        deferred.await()
    }
}

class FXCoroutine<T>(private val func: Callable<T>) : Async<T>() {

    private val latch = CountDownLatch(1)

    private var value: T? = null

    init {
        if (Platform.isFxApplicationThread()) {
            try {
                value = func.call()
            } finally {
                latch.countDown()
            }
        } else {
            Platform.runLater {
                try {
                    value = func.call()
                } finally {
                    latch.countDown()
                }
            }
        }
    }

    override fun await(): T? {
        latch.await()
        return value
    }
}