package interpreter

import kotlin.math.sqrt



class Vector2(var x: Double = 0.0, var y: Double = 0.0) {

    fun clone() : Vector2 {
        return Vector2(x, y)
    }

    fun add(other: Vector2) : Vector2 {
        x += other.x
        y += other.y
        return this
    }

    fun sub(other: Vector2) : Vector2 {
        x -= other.x
        y -= other.y
        return this
    }

    fun multiply(other: Vector2) : Vector2 {
        x *= other.x
        y *= other.y
        return this
    }

    fun multiplyScalar(scalar: Double) : Vector2 {
        x *= scalar
        y *= scalar
        return this
    }

    fun divide(other: Vector2) : Vector2 {
        x /= other.x
        y /= other.y
        return this
    }

    fun divideScalar(scalar: Double) : Vector2 {
        x /= scalar
        y /= scalar
        return this
    }

    fun dot(other: Vector2) : Double {
        return x * other.x + y * other.y
    }

    fun length() : Double {
        return sqrt(x * x + y * y)
    }

    fun normalize() : Vector2 {
        return divideScalar(length())
    }
}