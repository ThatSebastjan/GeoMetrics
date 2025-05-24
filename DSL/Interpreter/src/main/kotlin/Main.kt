import scanner.Scanner
import scanner.ScannerInput
import scanner.TokenType
import java.io.File


fun main() {
    val inFile = File("C:\\Users\\Rok\\Desktop\\school work\\2\\projekt\\GeoAssess\\DSL\\Interpreter\\src\\main\\resources\\example.txt").inputStream()
    val inStream = ScannerInput(inFile)

    val scanner = Scanner(inStream)

    while(true){
        val t = scanner.nextToken()
        println(t)

        if(t.isOfType(TokenType.EOF)) {
            break
        }

        if(t.isOfType(TokenType.LEXICAL_ERROR)){
            println("Lexical error at col: ${t.column}, row: ${t.row}")
            break
        }
    }
}