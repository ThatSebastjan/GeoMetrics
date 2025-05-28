package interpreter

import com.sun.jdi.InvalidTypeException
import java.io.InvalidObjectException



enum class ObjectType {
    NUMBER,
    INTEGER,
    STRING,
    BOOLEAN,

    POINT,
    POLYGON,

    IMPORTED_FEATURE_COLLECTION,
    IMPORTED_FEATURE,

    OBJECT_PROPERTY,

    LAMBDA,
    NATIVE_FUNCTION,

    CADASTRE,
    LAND_LOT,
    RISK,

    BLOCK,

    NULL,
}


//Base type for everything
class Object(val value: Any?, type: ObjectType? = null) {

    var dataType = type
        private set


    init {
        val valType = getValueType()

        if(type == null) {
            dataType = valType
        }
        else {
            expectType(valType) //DEBUG TYPE CHECK - Check if explicitly provided type matches our internal value type representation
        }
    }


    private fun getValueType() : ObjectType {
        return when(value){
            is Double -> ObjectType.NUMBER
            is Int -> ObjectType.INTEGER
            is String -> ObjectType.STRING
            is Boolean -> ObjectType.BOOLEAN

            is PointObject -> ObjectType.POINT
            is PolygonObject -> ObjectType.POLYGON

            is ImportedFeatureCollection -> ObjectType.IMPORTED_FEATURE_COLLECTION
            is ImportedFeature -> ObjectType.IMPORTED_FEATURE

            is ObjectProperty -> ObjectType.OBJECT_PROPERTY

            is Lambda -> ObjectType.LAMBDA
            is NativeFunction -> ObjectType.NATIVE_FUNCTION

            is Cadastre -> ObjectType.CADASTRE
            is LandLot -> ObjectType.LAND_LOT
            is Risk -> ObjectType.RISK

            is Block -> ObjectType.BLOCK

            null -> ObjectType.NULL

            else -> throw InvalidObjectException("Invalid value provided as Object value")
        }
    }


    override fun toString(): String {
        return "Object<$dataType>($value)"
    }


    //Type checking
    fun expectType(expectedType: ObjectType) : Object {
        if(dataType != expectedType){
            throw InvalidTypeException("Expected Object of type $expectedType, but found ${dataType}")
        }

        return this
    }


    fun expectOneOfTypes(vararg expectedTypes: ObjectType) : Object {
        for(eType in expectedTypes){
            if(dataType == eType){
                return this
            }
        }

        throw InvalidTypeException("Expected Object to be one of types (${expectedTypes.joinToString(", ")}), but found ${dataType}")
    }


    companion object {
        val NullObject = Object(null, ObjectType.NULL)
    }
}