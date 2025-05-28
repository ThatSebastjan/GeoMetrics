import interpreter.Interpreter
import parser.Parser
import scanner.Scanner
import scanner.ScannerInput
import java.io.File


fun main() {
    val inFile = object {}.javaClass.getResourceAsStream("test.txt")!!
    val inStream = ScannerInput(inFile)

    val scanner = Scanner(inStream)
    val parser = Parser(scanner)

    val astTree = parser.parse()

    if(astTree != null) {
        val interpreter = Interpreter()
        val geoJsonStr = interpreter.interpret(astTree)

        File("./outFeatures.json").writeText(geoJsonStr)
    }
}