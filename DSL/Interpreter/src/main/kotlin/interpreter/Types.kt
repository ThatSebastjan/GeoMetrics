package interpreter

import com.sun.jdi.InvalidTypeException
import io.github.dellisd.spatialk.geojson.*
import io.github.dellisd.spatialk.turf.booleanPointInPolygon
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import parser.Node
import java.io.File
import java.io.InputStream


interface GeoJSONExport {
    fun toGeoJSON() : List<Feature>
}


//Internal interpreter types

class PointObject(val longitude: Double, val latitude: Double) {

    override fun toString(): String {
        return "Point($longitude, $latitude)"
    }


    override fun equals(other: Any?): Boolean {
        if(other is PointObject){
            return (longitude == other.longitude) && (latitude == other.latitude)
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}


class PolygonObject(val points: List<PointObject>) {

    override fun toString() : String {
        return "Polygon(${points.joinToString(", ")})"
    }


    fun toGeoJSONPolygon() : Polygon {
        val pointList = points.map { Position(doubleArrayOf(it.longitude, it.latitude)) }
        return Polygon(pointList)
    }


    fun isContainedWithin(other: PolygonObject) : Boolean {
        val otherPoly = other.toGeoJSONPolygon()
        val selfPoly = toGeoJSONPolygon()

        return selfPoly.coordinates[0].all { booleanPointInPolygon(Point(it), otherPoly)  }
    }


    companion object {

        fun fromCircle(center: PointObject, radius: Double) : PolygonObject {
            TODO("Implement me!")
            //return PolygonObject(listOf())
        }

        fun fromBox(topLeft: PointObject, bottomRight: PointObject) : PolygonObject {
            return PolygonObject(
                listOf(
                    topLeft,
                    PointObject(topLeft.longitude, bottomRight.latitude),
                    bottomRight,
                    PointObject(bottomRight.longitude, topLeft.latitude),
                    topLeft
                )
            )
        }

        fun fromPolyLine(width: Double, points: List<PointObject>) : PolygonObject {
            TODO("Implement me!")
            //return PolygonObject(listOf())
        }
    }
}


class ImportedFeatureCollection(filePath: String) {
    val data: FeatureCollection

    init {
        val inputStream: InputStream = File(filePath).inputStream()
        val inputString = inputStream.bufferedReader().use { it.readText() }
        data = FeatureCollection.fromJson(inputString)
    }
}


data class ImportedFeature(val data: Feature)



data class ObjectProperty(val name: String, val value: Object);


data class Lambda(val params: List<String>, val body: Node.LambdaBody)



class LandLot(val id: String, val type: String, val bounds: PolygonObject, val additionalProps: List<ObjectProperty>) : GeoJSONExport {

    override fun toString(): String {
        var str = "Lot {\n\t.id: $id;\n\t.type: $type;\n\t.bounds: $bounds;\n"

        additionalProps.forEach {
            str += "\t.${it.name}: ${it.value};\n"
        }
        str += "}"

        return str
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun toGeoJSON(): List<Feature> {
        val props = mutableMapOf<String, JsonElement> ()

        props["id"] = JsonPrimitive(id)
        props["type"] = JsonPrimitive(type)

        additionalProps.forEach {
            val value = when(it.value.dataType){
                ObjectType.NUMBER -> JsonPrimitive(it.value.value as Double)
                ObjectType.STRING -> JsonPrimitive(it.value.value as String)
                ObjectType.NULL -> JsonPrimitive(null)

                else -> throw InvalidTypeException("Cannot convert property of type ${it.value} to JSON property")
            }

            props[it.name] = value
        }

        return listOf(Feature(bounds.toGeoJSONPolygon(), props))
    }
}


class Cadastre(val name: String, val id: String, val bounds: PolygonObject, val additionalProps: List<ObjectProperty>, val lots: List<LandLot>) : GeoJSONExport {

    override fun toString(): String {
        var str = "Cadastre {\n\t.name: $name;\n\t.id: $id;\n\t.bounds: $bounds;\n"

        additionalProps.forEach {
            str += "\t.${it.name}: ${it.value};\n"
        }
        str += "\n"

        str += "\tlots: [\n"
        lots.forEach {
            str += "${it.toString().split("\n").joinToString("\n") { "\t\t$it" }},\n"
        }

        str += "\t]"
        str += "}"
        return str
    }


    @OptIn(ExperimentalSerializationApi::class)
    override fun toGeoJSON(): List<Feature> {
        val props = mutableMapOf<String, JsonElement> ()

        props["name"] = JsonPrimitive(name)
        props["id"] = JsonPrimitive(id)

        additionalProps.forEach {
            val value = when(it.value.dataType){
                ObjectType.NUMBER -> JsonPrimitive(it.value.value as Double)
                ObjectType.STRING -> JsonPrimitive(it.value.value as String)
                ObjectType.NULL -> JsonPrimitive(null)

                else -> throw InvalidTypeException("Cannot convert property of type ${it.value} to JSON property")
            }

            props[it.name] = value
        }

        val cadastreFeature = Feature(bounds.toGeoJSONPolygon(), props)
        val lotFeatures = lots.map { it.toGeoJSON()[0] }
        return listOf(cadastreFeature) + lotFeatures
    }

}


class Risk(val type: String, val bounds: PolygonObject, val probability: Double, val frequency: Double, val additionalProps: List<ObjectProperty>) : GeoJSONExport {

    override fun toString(): String {
        var str = "Risk {\n\t.type: $type;\n\t.bounds: $bounds;\n\t.probability: $probability;\n\t.frequency: $frequency;\n"

        additionalProps.forEach {
            str += "\t.${it.name}: ${it.value};\n"
        }
        str += "}"

        return str
    }


    @OptIn(ExperimentalSerializationApi::class)
    override fun toGeoJSON(): List<Feature> {
        val props = mutableMapOf<String, JsonElement> ()

        props["type"] = JsonPrimitive(type)
        props["probability"] = JsonPrimitive(probability)
        props["frequency"] = JsonPrimitive(frequency)

        additionalProps.forEach {
            val value = when(it.value.dataType){
                ObjectType.NUMBER -> JsonPrimitive(it.value.value as Double)
                ObjectType.STRING -> JsonPrimitive(it.value.value as String)
                ObjectType.NULL -> JsonPrimitive(null)

                else -> throw InvalidTypeException("Cannot convert property of type ${it.value} to JSON property")
            }

            props[it.name] = value
        }

        return listOf(Feature(bounds.toGeoJSONPolygon(), props))
    }
}


class Block(val data: List<Object>) : GeoJSONExport {

    override fun toGeoJSON() : List<Feature> {
        val features = mutableListOf<Feature>()

        data.forEach {

            when(it.dataType){
                ObjectType.CADASTRE -> {
                    val obj = it.value as Cadastre
                    features.addAll(obj.toGeoJSON())
                }

                ObjectType.RISK -> {
                    val obj = it.value as Risk
                    features.addAll(obj.toGeoJSON())
                }

                else -> throw InvalidTypeException("Invalid object type in block at time of export")
            }
        }

        return features
    }

    fun toFeatureCollection() : FeatureCollection {
        return FeatureCollection(toGeoJSON())
    }
}


data class NativeFunction(val function: (List<Object>) -> Object)