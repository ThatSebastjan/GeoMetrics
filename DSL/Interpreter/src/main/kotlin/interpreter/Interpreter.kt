package interpreter

import io.github.dellisd.spatialk.turf.ExperimentalTurfApi
import kotlinx.serialization.json.*
import parser.Node



class InterpreterRuntimeException(message: String, val node: Node): Exception(message)


class Interpreter : Node.Visitor<Object> {

    private val scopes: MutableList<ProgramScope> = mutableListOf()

    private val lotIdRgx = "(\\d+/\\d+)|(\\d+)".toRegex()
    private val lotTypes = listOf("Road", "BuildingLot", "FarmLand", "Forest", "Meadow", "River", "Lake")

    private val riskTypes = listOf("Flood", "LandSlide", "Earthquake")

    private val cadastreIdRgx = "\\d+".toRegex()


    companion object NativeFunctions {

        //area(Polygon) -> Number
        @OptIn(ExperimentalTurfApi::class)
        fun area(params: List<Object>): Object {
            if(params.size != 1){
                throw IllegalArgumentException("area expects 1 argument but got ${params.size}")
            }

            val poly = params[0].expectType(ObjectType.POLYGON).value as PolygonObject
            val polyArea = io.github.dellisd.spatialk.turf.area(poly.toGeoJSONPolygon())

            return Object(polyArea, ObjectType.NUMBER)
        }


        val NATIVE_FN_LIST = listOf(::area)
    }



    //Entry
    fun interpret(root: Node) : Block {

        //Create global scope
        scopes.add(ProgramScope("GLOBAL"))

        //Declare global constants/identifiers
        lotTypes.forEach { declareConstant(it, Object(it, ObjectType.STRING)) }
        riskTypes.forEach { declareConstant(it, Object(it, ObjectType.STRING)) }

        //Declare inbuilt functions
        NATIVE_FN_LIST.forEach { declareConstant(it.name, Object(NativeFunction(it), ObjectType.NATIVE_FUNCTION)) }

        //Evaluate the root node to get the result (block of all features)
        val result = evaluate(root).expectType(ObjectType.BLOCK).value as Block

        return result
    }


    //Visit a given node and return the value
    private fun evaluate(node: Node) : Object {
        return node.accept(this)
    }


    //Check if any scope contains a definitions with name
    private fun findDefinition(name: String) : Object? {
        for(i in (scopes.lastIndex downTo 0)){

            val def = scopes[i].getConstant(name)

            if(def != null){
                return def
            }
        }

        return null
    }

    private fun definitionExists(name: String)  = findDefinition(name) != null


    private fun declareConstant(name: String, value: Object) = scopes.last().setConstant(name, value)



    override fun visitAdd(node: Node.Add): Object {
        val left = evaluate(node.left).expectType(ObjectType.NUMBER)
        val right = evaluate(node.right).expectType(ObjectType.NUMBER)

        return Object((left.value as Double) + (right.value as Double), ObjectType.NUMBER)
    }


    override fun visitSub(node: Node.Sub): Object {
        val left = evaluate(node.left).expectType(ObjectType.NUMBER)
        val right = evaluate(node.right).expectType(ObjectType.NUMBER)

        return Object((left.value as Double) - (right.value as Double), ObjectType.NUMBER)
    }


    override fun visitMul(node: Node.Mul): Object {
        val left = evaluate(node.left).expectType(ObjectType.NUMBER)
        val right = evaluate(node.right).expectType(ObjectType.NUMBER)

        return Object((left.value as Double) * (right.value as Double), ObjectType.NUMBER)
    }


    override fun visitDiv(node: Node.Div): Object {
        val left = evaluate(node.left).expectType(ObjectType.NUMBER)
        val right = evaluate(node.right).expectType(ObjectType.NUMBER)

        if((right.value as Double) == 0.0){
            throw InterpreterRuntimeException("Division by zero", node)
        }

        return Object((left.value as Double) / (right.value as Double), ObjectType.NUMBER)
    }


    override fun visitLessThan(node: Node.LessThan): Object {
        val left = evaluate(node.left).expectType(ObjectType.NUMBER)
        val right = evaluate(node.right).expectType(ObjectType.NUMBER)

        return Object((left.value as Double) < (right.value as Double), ObjectType.BOOLEAN)
    }


    override fun visitGreaterThan(node: Node.GreaterThan): Object {
        val left = evaluate(node.left).expectType(ObjectType.NUMBER)
        val right = evaluate(node.right).expectType(ObjectType.NUMBER)

        return Object((left.value as Double) > (right.value as Double), ObjectType.BOOLEAN)
    }



    override fun visitPropertyAccess(node: Node.PropertyAccess): Object {
        val obj = evaluate(node.constant)

        when(obj.dataType){

            ObjectType.POINT -> {
                val pointObj = obj.value as PointObject

                return when(node.property){
                    "longitude" -> Object(pointObj.longitude, ObjectType.NUMBER)
                    "latitude" -> Object(pointObj.latitude, ObjectType.NUMBER)
                    else -> throw InterpreterRuntimeException("Attempted to access invalid property '${node.property}' on Point", node)
                }
            }

            ObjectType.POLYGON -> {
                return when(node.property){
                    "area" -> area(listOf(obj))
                    else -> throw InterpreterRuntimeException("Attempted to access invalid property '${node.property}' on Polygon", node)
                }
            }

            ObjectType.IMPORTED_FEATURE -> {
                val importObj = obj.value as ImportedFeature

                //Geometry property returns a PolygonObject
                if(node.property == "geometry"){
                    if(importObj.data.geometry == null){
                        throw InterpreterRuntimeException("Attempted to access imported feature geometry, but it is null", node)
                    }

                    val featurePoly = importObj.data.geometry as? io.github.dellisd.spatialk.geojson.Polygon
                        ?: throw InterpreterRuntimeException("Imported feature geometry is not a Polygon", node)

                    val polyObj = PolygonObject(featurePoly.coordinates[0].map { it -> PointObject(it.longitude, it.latitude) })
                    return Object(polyObj, ObjectType.POLYGON)
                }


                //Other properties
                val prop = importObj.data.properties[node.property]

                if(prop !is JsonPrimitive){ //We only allow primitives as there is no way to represent other types
                    throw InterpreterRuntimeException("Only primitive property access is supported on imported features: '${node.property}'", node)
                }

                if(prop.isString){
                    return Object(prop.content, ObjectType.STRING)
                }
                else if(prop.doubleOrNull != null){
                    return Object(prop.double, ObjectType.NUMBER)
                }
                else if(prop.floatOrNull != null){
                    return Object(prop.float.toDouble(), ObjectType.NUMBER) //Explicit conversion from float to double
                }
                else if(prop.intOrNull != null){
                    return Object(prop.int, ObjectType.INTEGER)
                }

                return Object.NullObject //We have no way of converting the primitive type, or it is null
            }

            else -> throw InterpreterRuntimeException("Invalid property access '${node.property}' on object of type ${obj.dataType}", node)
        }
    }



    override fun visitCall(node: Node.Call): Object {
        val callable = evaluate(node.identifier).expectOneOfTypes(ObjectType.LAMBDA, ObjectType.NATIVE_FUNCTION)
        val args = node.arguments.map { evaluate(it) }


        if(callable.dataType == ObjectType.NATIVE_FUNCTION){
            val fnObj = callable.value as NativeFunction
            return fnObj.function(args)
        }


        //Lambda call

        //Check for disallowed recursion (as there is no way to end the loop)
        if(scopes.last().tag == node.identifier.name){
            throw InterpreterRuntimeException("Recursion is not allowed", node)
        }

        //Begin scope and register parameters
        //NOTE: parameter types are not checked here as everything is allowed and a runtime error will occur on invalid operation
        scopes.add(ProgramScope(node.identifier.name))

        val lambdaObj = callable.value as Lambda

        if(lambdaObj.params.size != args.size){
            throw InterpreterRuntimeException("Invalid number of arguments for lambda call", node)
        }

        //NOTE: existing constant name collisions are not checked here as we allow shadowing!
        lambdaObj.params.forEachIndexed { index, paramName -> declareConstant(paramName, args[index]) }

        val result = evaluate(lambdaObj.body) //Type is checked in visitLambdaBody

        scopes.removeLast()
        return result
    }



    override fun visitNumber(node: Node.Number): Object {
        return Object(node.value, ObjectType.NUMBER)
    }

    override fun visitInteger(node: Node.Integer): Object {
        return Object(node.value, ObjectType.INTEGER)
    }

    override fun visitStringValue(node: Node.StringValue): Object {
        return Object(node.value, ObjectType.STRING)
    }

    override fun visitNull(node: Node.Null): Object {
        return Object(null, ObjectType.NULL)
    }



    override fun visitIdentifier(node: Node.Identifier): Object {
        val value = findDefinition(node.name) ?: throw InterpreterRuntimeException("Undeclared identifier ${node.name}", node)
        return value
    }


    override fun visitPoint(node: Node.Point): Object {
        val longitude = evaluate(node.longitude).expectType(ObjectType.NUMBER)
        val latitude = evaluate(node.latitude).expectType(ObjectType.NUMBER)

        return Object(PointObject((longitude.value as Double), (latitude.value as Double)))
    }


    override fun visitPolygon(node: Node.Polygon): Object {
        val points = node.points.map { evaluate(it).value as PointObject }

        //Validate points
        if(points.size < 4){
            throw InterpreterRuntimeException("Polygon must have at least 4 points", node)
        }
        else if(points.first() != points.last()){
            throw InterpreterRuntimeException("First and last point of a polygon must match", node)
        }

        return Object(PolygonObject(points), ObjectType.POLYGON)
    }


    override fun visitCircle(node: Node.Circle): Object {
        val centerPoint = evaluate(node.center).value as PointObject
        val radius = evaluate(node.radius).expectType(ObjectType.NUMBER).value as Double

        return Object(PolygonObject.fromCircle(centerPoint, radius), ObjectType.POLYGON)
    }


    override fun visitBox(node: Node.Box): Object {
        val topLeft = evaluate(node.topLeft).value as PointObject
        val bottomRight = evaluate(node.bottomRight).value as PointObject

        return Object(PolygonObject.fromBox(topLeft, bottomRight), ObjectType.POLYGON)
    }


    override fun visitPolyLine(node: Node.PolyLine): Object {
        val width = evaluate(node.width).expectType(ObjectType.NUMBER).value as Double
        val points = node.points.map { evaluate(it).value as PointObject }

        if(points.size < 2){
            throw InterpreterRuntimeException("PolyLine must have at least 2 points", node)
        }

        return Object(PolygonObject.fromPolyLine(width, points), ObjectType.POLYGON)
    }



    override fun visitPropertyDefinition(node: Node.PropertyDefinition): Object {
        val value = evaluate(node.value).expectOneOfTypes(ObjectType.NUMBER, ObjectType.STRING, ObjectType.POLYGON, ObjectType.NULL)
        return Object(ObjectProperty(node.name, value), ObjectType.OBJECT_PROPERTY)
    }


    override fun visitConstantDefinition(node: Node.ConstantDefinition): Object {
        if(definitionExists(node.name)){
            throw InterpreterRuntimeException("Redefinition of constant '${node.name}'", node)
        }

        val value = evaluate(node.value).expectOneOfTypes(ObjectType.NUMBER, ObjectType.POINT, ObjectType.POLYGON,
            ObjectType.IMPORTED_FEATURE_COLLECTION, ObjectType.IMPORTED_FEATURE, ObjectType.LAMBDA)

        declareConstant(node.name, value)

        return Object.NullObject //No return value
    }


    override fun visitImport(node: Node.Import): Object {

        //Is it a FeatureCollection import?
        val fcImport = ImportedFeatureCollection.fromFile(node.file)

        if(fcImport != null){
            return Object(fcImport, ObjectType.IMPORTED_FEATURE_COLLECTION)
        }

        //Otherwise it must be a Feature import
        val feature = ImportedFeature.fromFile(node.file)

        if(feature != null){
            return Object(feature, ObjectType.IMPORTED_FEATURE)
        }

        throw InterpreterRuntimeException("Only FeatureCollection and Feature imports are allowed", node)
    }



    //This is visited on each lambda call in its own scope
    override fun visitLambdaBody(node: Node.LambdaBody): Object {

        //Evaluate const def statements
        node.constDefinitions.forEach { evaluate(it).expectType(ObjectType.NULL) }

        //Evaluate return statement
        return evaluate(node.returnValue).expectOneOfTypes(ObjectType.POINT, ObjectType.POLYGON, ObjectType.NUMBER)
    }


    override fun visitLambda(node: Node.Lambda): Object {
        return Object(Lambda(node.params, node.body), ObjectType.LAMBDA) //Lambda object is a light wrapper around lambda node and is evaluated on each call
    }



    //Block is a generic wrapper for a list of constructs
    //NOTE: scope changes should be handled by the caller
    override fun visitBlock(node: Node.Block): Object {
        val result = mutableListOf<Object>()

        for(n in node.nodes){

            when(n){
                is Node.ConstantDefinition -> evaluate(n).expectType(ObjectType.NULL) //Constant definitions return null on evaluation

                //Add nested data if evaluated
                is Node.ForEach -> {
                    val obj = evaluate(n).expectType(ObjectType.BLOCK).value as Block //Evaluated block might be empty if only const definitions are present
                    result.addAll(obj.data)
                }

                //Add nested conditional data if it evaluates to something
                is Node.Conditional -> {
                    val obj = evaluate(n).expectOneOfTypes(ObjectType.BLOCK, ObjectType.NULL) //Conditional can evaluate to nothing (null)

                    if(obj.dataType == ObjectType.BLOCK){
                        val nestedBlock = obj.value as Block;
                        result.addAll(nestedBlock.data)
                    }
                }

                is Node.Lot -> result.add(evaluate(n))

                is Node.Risk -> result.add(evaluate(n))

                is Node.Cadastre -> result.add(evaluate(n))

                is Node.PropertyDefinition -> result.add(evaluate(n))

                else -> throw InterpreterRuntimeException("Invalid node found in block: $n", node)
            }
        }

        return Object(Block(result), ObjectType.BLOCK)
    }



    override fun visitCadastre(node: Node.Cadastre): Object {
        val properties = mutableListOf<ObjectProperty>() //Cadastre has required properties: name, id, bounds
        val lots = mutableListOf<LandLot>()


        //Cadastre is a scope block
        scopes.add(ProgramScope("CADASTRE"))


        //Evaluate block and check for valid constructs
        val blockData = evaluate(node.block).value as Block

        for(n in blockData.data){

            when(n.dataType){
                ObjectType.OBJECT_PROPERTY -> properties.add(n.value as ObjectProperty)

                ObjectType.LAND_LOT -> lots.add(n.value as LandLot)

                else -> throw InterpreterRuntimeException("Invalid object type in cadastre block: $n", node)
            }
        }

        //Pop scope
        scopes.removeLast()


        //Check required properties
        val cadastreNameProp = properties.find { it.name == "name" }?.value ?: throw InterpreterRuntimeException("Cadastre must have a name property", node)
        cadastreNameProp.expectType(ObjectType.STRING)

        val cadastreIdProp = properties.find { it.name == "id" }?.value ?: throw InterpreterRuntimeException("Cadastre must have an id property", node)
        cadastreIdProp.expectType(ObjectType.STRING)

        if(!cadastreIdRgx.matches(cadastreIdProp.value as String)){
            throw InterpreterRuntimeException("Invalid cadastre id property format", node)
        }

        val cadastreBoundsProp = properties.find { it.name == "bounds" }?.value ?: throw InterpreterRuntimeException("Cadastre must have a bounds property", node)
        cadastreBoundsProp.expectType(ObjectType.POLYGON)
        val cadastreBounds = cadastreBoundsProp.value as PolygonObject

        val otherProps = properties.filter { it.name !in listOf("name", "id", "bounds") }


        //Check if lots are in cadastre
        lots.forEach {
            if(!it.bounds.isContainedWithin(cadastreBounds)){
                throw InterpreterRuntimeException("Lot must be in cadastre bounds", node)
            }
        }

        return Object(Cadastre(cadastreNameProp.value as String, cadastreIdProp.value as String, cadastreBounds, otherProps, lots), ObjectType.CADASTRE)
    }


    override fun visitLot(node: Node.Lot): Object {

        //Lot has required properties: id, type, bounds
        val idProp = node.properties.find { it.name == "id" } ?: throw InterpreterRuntimeException("Lot must have an id property!", node)
        val id = evaluate(idProp.value).expectType(ObjectType.STRING)

        if(!lotIdRgx.matches(id.value as String)){
            throw InterpreterRuntimeException("Invalid lot id property format", node)
        }

        val typeProp = node.properties.find { it.name == "type" } ?: throw InterpreterRuntimeException("Lot must have a type property!", node)
        val type = evaluate(typeProp.value).expectType(ObjectType.STRING) //Type is a interpreter global pre-declared identifier

        if(!lotTypes.contains(type.value as String)){
            throw InterpreterRuntimeException("Invalid lot type property value: '${type.value}'", node)
        }

        val boundsProp = node.properties.find { it.name == "bounds" } ?: throw InterpreterRuntimeException("Lot must have a bounds property!", node)
        val bounds = evaluate(boundsProp.value).expectType(ObjectType.POLYGON)

        val otherProps = node.properties.filter { it.name !in listOf("id", "type", "bounds") }.map { evaluate(it).value as ObjectProperty }

        return Object(LandLot(id.value as String, type.value as String, bounds.value as PolygonObject, otherProps), ObjectType.LAND_LOT)
    }


    override fun visitRisk(node: Node.Risk): Object {

        //Risk has required properties: type, bounds, probability, frequency
        val typeProp = node.properties.find { it.name == "type" } ?: throw InterpreterRuntimeException("Risk must have a type property!", node)
        val type = evaluate(typeProp.value).expectType(ObjectType.STRING) //Type is a interpreter global pre-declared identifier

        if(!riskTypes.contains(type.value as String)){
            throw InterpreterRuntimeException("Invalid risk type property value: '${type.value}'", node)
        }

        val boundsProp = node.properties.find { it.name == "bounds" } ?: throw InterpreterRuntimeException("Risk must have a bounds property!", node)
        val bounds = evaluate(boundsProp.value).expectType(ObjectType.POLYGON)

        val probabilityProp = node.properties.find { it.name == "probability" } ?: throw InterpreterRuntimeException("Risk must have a probability property!", node)
        val probability = evaluate(probabilityProp.value).expectType(ObjectType.NUMBER).value as Double

        if((probability < 0.0) || (probability > 1.0)){
            throw InterpreterRuntimeException("Risk probability property must be between 0 and 1. Found: $probability", node)
        }

        val frequencyProp = node.properties.find { it.name == "frequency" } ?: throw InterpreterRuntimeException("Risk must have a frequency property!", node)
        val frequency = evaluate(frequencyProp.value).expectType(ObjectType.NUMBER).value as Double

        if((frequency < 0.0) || (frequency > 1.0)){
            throw InterpreterRuntimeException("Risk frequency property must be between 0 and 1. Found: $frequency", node)
        }

        val otherProps = node.properties.filter { it.name !in listOf("type", "bounds", "probability", "frequency") }.map { evaluate(it).value as ObjectProperty }

        return Object(Risk(type.value as String, bounds.value as PolygonObject, probability, frequency, otherProps), ObjectType.RISK)
    }



    override fun visitForEach(node: Node.ForEach): Object {
        val featureListObj = evaluate(node.arrayIdentifier).expectType(ObjectType.IMPORTED_FEATURE_COLLECTION)
        val featureList = featureListObj.value as ImportedFeatureCollection
        val result = mutableListOf<Object>()


        featureList.data.forEachIndexed { index, it ->

            //Begin scope
            scopes.add(ProgramScope("FOR_EACH"))

            //Declare variable prop without checking for name conflicts as we allow shadowing here
            declareConstant(node.variableName, Object(ImportedFeature(it), ObjectType.IMPORTED_FEATURE))
            declareConstant("index", Object(index.toDouble(), ObjectType.NUMBER)) //Declare loop index constant

            //Evaluate body for each iteration, accumulating results
            val block = evaluate(node.content).value as Block
            result.addAll(block.data)

            //End scope
            scopes.removeLast()
        }

        return Object(Block(result), ObjectType.BLOCK)
    }


    override fun visitConditional(node: Node.Conditional): Object {
        val condition = evaluate(node.condition).expectType(ObjectType.BOOLEAN)

        //Begin new scope
        scopes.add(ProgramScope("CONDITIONAL"))

        val result = if(condition.value as Boolean){
            evaluate(node.ifBlock)
        }
        else if(node.elseBlock != null){
            evaluate(node.elseBlock)
        }
        else {
            Object.NullObject
        }

        //End scope
        scopes.removeLast()

        return result
    }


    override fun visitBooleanExpression(node: Node.BooleanExpression): Object {
        val result = evaluate(node.value).expectType(ObjectType.BOOLEAN)
        return result
    }

}