package interpreter



//Program block environment - scope
class ProgramScope(val tag: String = "") {
    private val constants: MutableMap<String, Object> = mutableMapOf() //Constants declared in this scope.

    fun getConstant(name: String) = constants[name]

    fun setConstant(name: String, value: Object) {
        constants[name] = value
    }
}