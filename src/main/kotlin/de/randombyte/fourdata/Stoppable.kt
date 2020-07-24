package de.randombyte.fourdata

import kotlin.concurrent.thread

class Stoppable {
    @Volatile
    var stopped: Boolean = false
        private set

    fun stop() {
        stopped = true
    }
}

fun stoppableThread(block: Stoppable.() -> Unit): Stoppable {
    val stoppable = Stoppable()
    thread {
        stoppable.block()
    }
    return stoppable
}

inline fun <T> Iterable<T>.forEachIndexedStoppable(action: (index: Int, T) -> Boolean?): Unit {
    for ((index, item) in this.withIndex()) {
        val stop = action(index, item) ?: false
        if (stop) return
    }
}