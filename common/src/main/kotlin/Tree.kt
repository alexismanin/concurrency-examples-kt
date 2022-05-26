package fr.amanin.concurrency.examples.common

data class Tree(val value: Int, val left: Tree? = null, val right: Tree? = null) {
    override fun toString(): String {
        val leftStr = if (left == null) "" else "$left <"
        val rightStr = if (right == null) "" else "> $right"
        return "[$leftStr $value $rightStr]"
    }
}

fun Tree.insert(newValue: Int) : Tree = when {
    value >= newValue -> copy(left  =  left?.insert(newValue) ?: Tree(newValue))
    else              -> copy(right = right?.insert(newValue) ?: Tree(newValue))
}

fun bbTree(values: IntProgression) = values.toBBTree()

fun IntProgression.toBBTree() : Tree {
    if (isEmpty()) throw IllegalArgumentException("Empty progression")
    if (step < 0) return reversed().toBBTree()
    val size = size()
    return when {
        size == 1 -> Tree(first, null, null)
        size == 2 -> Tree(last, Tree(first, null, null), null)
        else -> {
            val half = first + step * (size / 2)
            val left = bbTree(first until half step step)
            val right = bbTree((half + step)..last step step)
            Tree(half, left, right)
        }
    }
}

fun randomBTree(values: IntProgression) = values.toArray().apply { shuffle() }.bTree()

fun IntArray.bTree() : Tree {
    require(isNotEmpty()) { "Cannot create a tree over an empty set of values" }
    var tree = Tree(get(0))
    for (i in 1..lastIndex) tree = tree.insert(get(i))
    return tree
}