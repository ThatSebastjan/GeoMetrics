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
import kotlin.math.*


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

        private fun toRad(deg: Double) = deg * (Math.PI / 180)
        private fun toDeg(rad: Double) = rad * (180 / Math.PI)

        fun fromCircle(center: PointObject, radius: Double) : PolygonObject {
            val c = radius / 6371000.0 //radius in meters to radians

            val cLon = toRad(center.longitude)
            val cLat = toRad(center.latitude)
            val points = mutableListOf<PointObject>()

            for(i in 0..360 step 10){
                val ang = toRad(i.toDouble());
                val latitude = asin(sin(cLat) * cos(c) + cos(cLat) * sin(c) * cos(ang))
                val longitude = cLon + atan2(sin(ang) * sin(c) * cos(cLat), cos(c) - sin(cLat) * sin(latitude))

                points.add(PointObject(toDeg(longitude), toDeg(latitude)))
            }

            points.add(points[0])
            points.reverse()

            return PolygonObject(points)
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


        //This mostly works (breaks at extreme angles). Implementation is on the questionable side
        fun fromPolyLine(width: Double, points: List<PointObject>) : PolygonObject {
            val thickness = (width / 6371000.0) * (180 / Math.PI) //radius in meters to degrees
            val startPoints = mutableListOf<Vector2>()
            val endPoints = mutableListOf<Vector2>()

            for(i in points.indices){
                val p = Vector2(points[i].longitude, points[i].latitude)

                //First point
                if(i == 0){
                    val next = Vector2(points[i + 1].longitude, points[i + 1].latitude)
                    val dir = next.clone().sub(p).normalize()
                    val normal = Vector2(dir.y, -dir.x).normalize()

                    startPoints.add(p.clone().add(normal.clone().multiplyScalar(thickness)))
                    endPoints.add(p.clone().add(normal.clone().multiplyScalar(-thickness)))
                    continue
                }

                //Last point
                else if(i == points.lastIndex){
                    val prev = Vector2(points[i - 1].longitude, points[i - 1].latitude)
                    val dir = p.clone().sub(prev).normalize()
                    val normal = Vector2(dir.y, -dir.x).normalize()

                    startPoints.add(p.clone().add(normal.clone().multiplyScalar(thickness)))
                    endPoints.add(p.clone().add(normal.clone().multiplyScalar(-thickness)))
                    continue
                }

                //Points in between
                val prev = Vector2(points[i - 1].longitude, points[i - 1].latitude)
                val next = Vector2(points[i + 1].longitude, points[i + 1].latitude)

                val dir1 = prev.clone().sub(p).normalize()
                val dir2 = next.clone().sub(p).normalize()

                val normal1 = Vector2(-dir1.y, dir1.x)
                val normal2 = Vector2(dir2.y, -dir2.x)
                val normal = normal1.clone().add(normal2).normalize()

                val angle = acos(dir1.dot(dir2))
                val len = thickness / sin(angle/2)

                startPoints.add(p.clone().add(normal.clone().multiplyScalar(len)))
                endPoints.add(p.clone().add(normal.clone().multiplyScalar(-len)))
            }

            endPoints.reverse()
            val pointList = (startPoints + endPoints).map { PointObject(it.x, it.y) }

            return PolygonObject(pointList + pointList[0])
        }
    }
}


data class ImportedFeatureCollection(val data: FeatureCollection) {

    companion object {

        fun fromFile(filePath: String) : ImportedFeatureCollection? {
            val inputStream: InputStream = File(filePath).inputStream()
            val inputString = inputStream.bufferedReader().use { it.readText() }

            val maybeData = FeatureCollection.fromJsonOrNull(inputString) ?: return null
            return ImportedFeatureCollection(maybeData)
        }
    }
}


data class ImportedFeature(val data: Feature) {

    companion object {

        fun fromFile(filePath: String) : ImportedFeature? {
            val inputStream: InputStream = File(filePath).inputStream()
            val inputString = inputStream.bufferedReader().use { it.readText() }

            val maybeData = Feature.fromJsonOrNull(inputString) ?: return null
            return ImportedFeature(maybeData)
        }
    }
}



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


        //Color
        props["stroke-width"] = JsonPrimitive(2.0)
        props["stroke-opacity"] = JsonPrimitive(1.0)
        props["fill-opacity"] = JsonPrimitive(0.5)

        val color = when(type){
            "Road" -> Pair("#AFB5C7", "#A5A5A5")
            "BuildingLot" -> Pair("#F4F0EF", "#CDD1DE")
            "FarmLand" -> Pair("#E3CD84", "#388659")
            "Forest" -> Pair("#137547", "#2A9134")
            "Meadow" -> Pair("#5BBA6F", "#3FA34D")
            "River" -> Pair("#98DCFE", "#49C6E5")
            "Lake" -> Pair("#0077B6", "#0070AC")

            else -> throw IllegalArgumentException("Illegal land lot type: $type")
        }

        props["fill"] = JsonPrimitive(color.first)
        props["stroke"] = JsonPrimitive(color.second)

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

        props["stroke-width"] = JsonPrimitive(5.0)
        props["stroke-opacity"] = JsonPrimitive(1.0)
        props["fill-opacity"] = JsonPrimitive(0.0)
        props["stroke"] = JsonPrimitive("#FF0000")

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

        //Color
        props["stroke-width"] = JsonPrimitive(2.0)
        props["stroke-opacity"] = JsonPrimitive(1.0)
        props["fill-opacity"] = JsonPrimitive(0.5)

        val color = when(type){
            "Flood" -> Pair("#6495ED", "#BB0099")
            "LandSlide" -> Pair("#ED8B64", "#E05858")
            "Earthquake" -> Pair("#ED6464", "#E44949")

            else -> throw IllegalArgumentException("Illegal risk type: $type")
        }

        props["fill"] = JsonPrimitive(color.first)
        props["stroke"] = JsonPrimitive(color.second)

        return listOf(Feature(bounds.toGeoJSONPolygon(), props))
    }
}


class Block(val data: List<Object>) : GeoJSONExport {


    override fun toString(): String {
        var str = "Block {\n"

        data.forEach {

            when(it.dataType){
                ObjectType.CADASTRE -> {
                    val obj = it.value as Cadastre
                    str += "${obj.toString().split("\n").joinToString("\n") { "\t\t$it" }},\n"
                }

                ObjectType.RISK -> {
                    val obj = it.value as Risk
                    str += "${obj.toString().split("\n").joinToString("\n") { "\t\t$it" }},\n"
                }

                else -> throw InvalidTypeException("Invalid object type in block at time of export")
            }
        }
        str += "}"

        return str
    }


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