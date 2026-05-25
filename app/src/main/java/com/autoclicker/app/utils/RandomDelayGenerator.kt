package com.autoclicker.app.utils

import kotlin.random.Random

/**
 * Generates non-repeating random delays similar to the pattern:
 * 0.5, 0.7, 0.6, 0.1, 0.3, 0.6, 0.2, 0.5, 0.3, 0.4, 0.5 ...
 * The delays cycle through a pre-generated list and never repeat consecutively.
 */
object RandomDelayGenerator {

    // Pre-defined humanlike pattern (in ms) that cycles and loops
    private val humanPatternMs = longArrayOf(
        500, 700, 600, 100, 300, 600, 200, 500, 300, 400, 500,
        800, 150, 450, 650, 250, 550, 350, 750, 200, 600, 400
    )

    private var patternIndex = 0
    private var lastDelay = -1L

    /**
     * Returns next delay from humanlike pattern, never same as previous
     */
    fun nextHumanDelay(): Long {
        var delay = humanPatternMs[patternIndex % humanPatternMs.size]
        // Avoid consecutive same value
        if (delay == lastDelay) {
            patternIndex++
            delay = humanPatternMs[patternIndex % humanPatternMs.size]
        }
        lastDelay = delay
        patternIndex++
        return delay
    }

    /**
     * Returns a purely random delay between min and max, never same as previous
     */
    fun nextRandomDelay(minMs: Long, maxMs: Long): Long {
        val range = maxMs - minMs
        if (range <= 0) return minMs
        var delay: Long
        var attempts = 0
        do {
            delay = minMs + Random.nextLong(range + 1)
            attempts++
        } while (delay == lastDelay && attempts < 10)
        lastDelay = delay
        return delay
    }

    fun reset() {
        patternIndex = 0
        lastDelay = -1L
    }
}
