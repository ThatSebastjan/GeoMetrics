package parser



abstract class Node() {


    //Visitor interface
    interface Visitor<T> {
        fun visitAdd(node: Add) : T
        fun visitSub(node: Sub) : T
        fun visitMul(node: Mul) : T
        fun visitDiv(node: Div) : T
        fun visitLessThan(node: LessThan) : T
        fun visitGreaterThan(node: GreaterThan) : T

        fun visitPropertyAccess(node: PropertyAccess) : T

        fun visitCall(node: Call) : T

        fun visitNumber(node: Number) : T
        fun visitInteger(node: Integer) : T
        fun visitStringValue(node: StringValue) : T
        fun visitNull(node: Null) : T
        fun visitIdentifier(node: Identifier) : T
        fun visitPoint(node: Point) : T
        fun visitPolygon(node: Polygon) : T
        fun visitCircle(node: Circle) : T
        fun visitBox(node: Box) : T
        fun visitPolyLine(node: PolyLine) : T

        fun visitPropertyDefinition(node: PropertyDefinition) : T

        fun visitConstantDefinition(node: ConstantDefinition) : T

        fun visitImport(node: Import) : T

        fun visitLambdaBody(node: LambdaBody) : T
        fun visitLambda(node: Lambda) : T

        fun visitBlock(node: Block) : T
        fun visitCadastre(node: Cadastre) : T
        fun visitLot(node: Lot) : T
        fun visitRisk(node: Risk) : T

        fun visitForEach(node: ForEach) : T
        fun visitConditional(node: Conditional) : T
        fun visitBooleanExpression(node: BooleanExpression) : T
    }


    //Numeric operations
    data class Add(val left: Node, val right: Node) : Node() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitAdd(this)
        }
    }

    data class Sub(val left: Node, val right: Node) : Node() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitSub(this)
        }
    }

    data class Mul(val left: Node, val right: Node) : Node() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitMul(this)
        }
    }

    data class Div(val left: Node, val right: Node) : Node() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitDiv(this)
        }
    }

    data class LessThan(val left: Node, val right: Node) : Node() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitLessThan(this)
        }
    }

    data class GreaterThan(val left: Node, val right: Node) : Node() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitGreaterThan(this)
        }
    }


    //Property access
    data class PropertyAccess(val constant: Identifier, val property: String) : Node() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitPropertyAccess(this)
        }
    }


    //Function call
    data class Call(val identifier: Identifier, val arguments: List<Node>) : Node() { //Arguments must be of NumericType only!
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitCall(this)
        }
    }


    //Types
    data class Number(val value: Double) : Node() { //Double
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitNumber(this)
        }
    }

    data class Integer(val value: Int) : Node() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitInteger(this)
        }
    }

    data class StringValue(val value: String) : Node() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitStringValue(this)
        }
    }

    data class Null(val value: Any? = null) : Node() { //Why is this a thing?
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitNull(this)
        }
    }

    data class Identifier(val name: String) : Node() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitIdentifier(this)
        }
    }

    data class Point(val longitude: Node, val latitude: Node) : Node() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitPoint(this)
        }
    }

    data class Polygon(val points: List<Point>) : Node() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitPolygon(this)
        }
    }

    data class Circle(val center: Point, val radius: Node) : Node() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitCircle(this)
        }
    }

    data class Box(val topLeft: Point, val bottomRight: Point) : Node() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitBox(this)
        }
    }

    data class PolyLine(val width: Node, val points: List<Point>) : Node() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitPolyLine(this)
        }
    }


    //Property definition
    data class PropertyDefinition(val name: String, val value: Node) : Node() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitPropertyDefinition(this)
        }
    }


    //Constant definitions
    data class ConstantDefinition(val name: String, val value: Node) : Node() { //Value can be one of: numeric expression, Import, Lambda, any bounds type
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitConstantDefinition(this)
        }
    }

    data class Import(val file: String) : Node() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitImport(this)
        }
    }

    data class LambdaBody(val constDefinitions: List<ConstantDefinition>, val returnValue: Node) : Node() { //Return value is either a number or a bounds type
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitLambdaBody(this)
        }
    }

    data class Lambda(val params: List<String>, val body: LambdaBody) : Node() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitLambda(this)
        }
    }


    //Blocks
    data class Block(val nodes: List<Node>) : Node() { //Block representing a list of Nodes
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitBlock(this)
        }
    }

    data class Cadastre(val block: Block) : Node() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitCadastre(this)
        }
    }

    data class Lot(val properties: List<PropertyDefinition>) : Node() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitLot(this)
        }
    }

    data class Risk(val properties: List<PropertyDefinition>) : Node() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitRisk(this)
        }
    }


    //Control flow
    data class ForEach(val variableName: String, val arrayIdentifier: Identifier, val content: Block) : Node() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitForEach(this)
        }
    }

    data class Conditional(val condition: BooleanExpression, val ifBlock: Block, val elseBlock: Block?) : Node() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitConditional(this)
        }
    }

    data class BooleanExpression(val value: Node) : Node() {
        override fun <T> accept(visitor: Visitor<T>): T {
            return visitor.visitBooleanExpression(this)
        }
    }



    //Abstract method that must be implemented by every Node
    abstract fun <T> accept(visitor: Visitor<T>) : T;
}




