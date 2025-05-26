import parser.Parser
import scanner.Scanner
import scanner.ScannerInput



fun main() {
    val inFile = object {}.javaClass.getResourceAsStream("numericExprTest.txt")!!
    val inStream = ScannerInput(inFile)

    val scanner = Scanner(inStream)
    val parser = Parser(scanner)

    val res = parser.parse()
}