package cympl.compiler.jvm

import org.objectweb.asm.Label

internal class LoopContext {

    private val labelPairs: MutableList<LoopLabelPair> = ArrayDeque()

    fun mark(continueLabel: Label, breakLabel: Label) {
        labelPairs.add(LoopLabelPair(continueLabel, breakLabel))
    }

    fun unmark() {
        labelPairs.removeLast()
    }

    val nearestContinueLabel get() = labelPairs.lastOrNull()?.continueLabel
    val nearestBreakLabel get() = labelPairs.lastOrNull()?.breakLabel

    private class LoopLabelPair(val continueLabel: Label, val breakLabel: Label)
}
