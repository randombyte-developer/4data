package de.randombyte.fourdata

abstract class JobReporter {
    companion object {
        const val UNKNOWN_TOTAL = -1
    }

    private var ended = false

    protected abstract fun onProgress(current: Int, total: Int)
    protected abstract fun onEnd(result: TarArchiver.Result)

    fun progress(current: Int, total: Int) = onProgress(current, total)

    fun end(result: TarArchiver.Result) {
        if (ended) throw IllegalStateException("This job already ended!")
        ended = true
        onEnd(result)
    }
}