package scanner


enum class TokenType {
    EOF,
    LEXICAL_ERROR,
    IGNORED_TOKEN,

    PLUS,
    MINUS,
    TIMES,
    DIVIDE,
    LT,
    GT,
    ASSIGN,

    LPAREN,
    RPAREN,
    LCURLY,
    RCURLY,
    LSQUARE,
    RSQUARE,

    COMMA,
    DOT,
    SEMI,
    COLON,

    NUMBER,
    INTEGER,
    STRING,
    IDENTIFIER,

    //LONGITUDE,
    //LATITUDE,

    POLYGON,
    CIRCLE,
    BOX,
    POLY_LINE,
    NULL,

    CONST,
    IMPORT,
    LAMBDA,
    RETURN,

    FOREACH,
    IN,
    IF,
    ELSE,

    CADASTRE,
    LOT,
    ROAD,
    BUILDING_LOT,
    FARM_LAND,
    FOREST,
    MEADOW,
    RIVER,
    LAKE,

    RISK,
    FLOOD,
    LANDSLIDE,
    EARTHQUAKE


    //TODO("Other token types!")
}



class Token(private val lexeme: String, val column: Int, val row: Int, private val type: TokenType) {

    override fun toString(): String {
        return "$type($lexeme)"
    }

    fun isOfType(comparatorType: TokenType) = type == comparatorType

    fun getLexeme() = lexeme

    fun getType() = type
}