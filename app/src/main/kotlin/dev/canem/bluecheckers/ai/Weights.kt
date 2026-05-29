package dev.canem.bluecheckers.ai

/**
 * Linear evaluator weights, in feature-index order from [Features]. Hand-tuned defaults
 * chosen so a fresh agent plays an OK opening. They are the starting point for TD-Leaf
 * learning; the trainer mutates them after each game.
 *
 * Persisted as a JSON DoubleArray. Schema:
 *   { "version": 1, "values": [ ...COUNT doubles... ] }
 */
data class Weights(val values: DoubleArray) {

    init {
        require(values.size == Features.COUNT) {
            "weights must be length ${Features.COUNT}, was ${values.size}"
        }
    }

    fun evaluate(features: DoubleArray): Double {
        require(features.size == Features.COUNT)
        var s = 0.0
        for (i in 0 until Features.COUNT) s += features[i] * values[i]
        return s
    }

    fun copy(): Weights = Weights(values.copyOf())

    override fun equals(other: Any?): Boolean =
        this === other || (other is Weights && values.contentEquals(other.values))

    override fun hashCode(): Int = values.contentHashCode()

    companion object {
        const val SCHEMA_VERSION = 1

        val DEFAULT: Weights = Weights(
            doubleArrayOf(
                /* MAN_BALANCE   */ 1.00,
                /* KING_BALANCE  */ 2.50,
                /* ADVANCEMENT   */ 0.05,
                /* CENTER        */ 0.10,
                /* BACK_RANK     */ 0.08,
                /* EDGE          */ -0.04,
                /* TRAPPED_KING  */ 0.30,
            )
        )

        fun zero(): Weights = Weights(DoubleArray(Features.COUNT))
    }
}
