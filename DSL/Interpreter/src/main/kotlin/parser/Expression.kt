package parser


abstract class Node() {

    //Operations
    data class Add(val left: Node, val right: Node) : Node()

    data class Sub(val left: Node, val right: Node) : Node()

    data class Mul(val left: Node, val right: Node) : Node()

    data class Div(val left: Node, val right: Node) : Node()

    data class Negative(val value: NumericType) : Node()

    //Types
    data class NumericType(val value: Double) : Node()
}




