package parser

import javax.lang.model.type.NullType


abstract class Node() {

    //Numeric operations
    data class Add(val left: Node, val right: Node) : Node()

    data class Sub(val left: Node, val right: Node) : Node()

    data class Mul(val left: Node, val right: Node) : Node()

    data class Div(val left: Node, val right: Node) : Node()

    data class LessThan(val left: Node, val right: Node) : Node()

    data class GreaterThan(val left: Node, val right: Node) : Node()


    //Property access
    data class PropertyAccess(val constant: Identifier, val property: Identifier) : Node()

    data class IndexAccess(val constant: Identifier, val index: Integer) : Node() //constant must be an imported array


    //Function call
    data class Call(val identifier: Identifier, val arguments: List<Node>) : Node() //Arguments must be of NumericType only!


    //Types
    data class Number(val value: Double) : Node() //Double

    data class Integer(val value: Int) : Node()

    data class StringValue(val value: String) : Node()

    data class Null(val value: Any? = null) : Node() //Why is this a thing?

    data class Identifier(val name: String) : Node()

    data class Point(val longitude: Node, val latitude: Node) : Node()

    data class Polygon(val points: List<Point>) : Node()

    data class Circle(val centre: Point, val radius: Node) : Node()

    data class Box(val topLeft: Point, val bottomRight: Point) : Node()

    data class PolyLine(val width: Node, val points: List<Point>) : Node()


    //Property definition
    data class PropertyDefinition(val name: String, val value: Node) : Node()


    //Constant definitions
    data class ConstantDefinition(val name: String, val value: Node) : Node() //Value can be one of: numeric expression, Import, Lambda, any bounds type

    data class Import(val file: String) : Node()

    data class LambdaBody(val constDefinitions: List<ConstantDefinition>, val returnValue: Node) : Node() //Return value is either a number or a bounds type

    data class Lambda(val params: List<Identifier>, val body: LambdaBody) : Node()


    //Blocks
    data class Block(val nodes: List<Node>) : Node() //Block representing a list of Nodes

    data class Cadastre(val nodes: List<Node>) : Node()

    data class Lot(val properties: List<PropertyDefinition>) : Node()

    data class Risk(val properties: List<PropertyDefinition>) : Node()


    //Control flow
    data class ForEach(val variableName: String, val arrayName: String, val content: Block) : Node()

    data class Conditional(val condition: BooleanExpression, val ifBlock: Block, val elseBlock: Block?) : Node()

    data class BooleanExpression(val value: Node) : Node()
}




