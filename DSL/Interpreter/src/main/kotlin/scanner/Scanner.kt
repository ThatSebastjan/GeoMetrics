package scanner



class Scanner(private val input: ScannerInput) {
    private var col = 0
    private var row = 0

    private var currentToken: Token? = null
    private var states = listOf<State>()

    init {
        initStates()
    }


    private fun initStates() {

        states = listOf(

            //Ignored characters
            State(listOf(
                Pattern(" \n\r\t", PatternType.ONE_OR_MORE)
            ), TokenType.IGNORED_TOKEN),


            //Inline comments
            State(listOf(
                Pattern("/"), Pattern("/"),
                Pattern((' ' .. '~').joinToString(""), PatternType.ZERO_OR_MORE) //All writable chars
            ), TokenType.IGNORED_TOKEN),


            //Operators
            State(listOf(Pattern("+")), TokenType.PLUS),
            State(listOf(Pattern("-")), TokenType.MINUS),
            State(listOf(Pattern("*")), TokenType.TIMES),
            State(listOf(Pattern("/")), TokenType.DIVIDE),
            State(listOf(Pattern("<")), TokenType.LT),
            State(listOf(Pattern(">")), TokenType.GT),
            State(listOf(Pattern("=")), TokenType.ASSIGN),


            //Parenthesis
            State(listOf(Pattern("(")), TokenType.LPAREN),
            State(listOf(Pattern(")")), TokenType.RPAREN),
            State(listOf(Pattern("{")), TokenType.LCURLY),
            State(listOf(Pattern("}")), TokenType.RCURLY),
            State(listOf(Pattern("[")), TokenType.LSQUARE),
            State(listOf(Pattern("]")), TokenType.RSQUARE),


            //Delimiters
            State(listOf(Pattern(",")), TokenType.COMMA),
            State(listOf(Pattern(".")), TokenType.DOT),
            State(listOf(Pattern(";")), TokenType.SEMI),
            State(listOf(Pattern(":")), TokenType.COLON),


            //<number> -> \d+\.\d+
            State(listOf(
                Pattern("0123456789", PatternType.ONE_OR_MORE),
                Pattern("."),
                Pattern("0123456789", PatternType.ONE_OR_MORE),
            ), TokenType.NUMBER),


            //<integer> -> \d+
            State(listOf(
                Pattern("0123456789", PatternType.ONE_OR_MORE),
            ), TokenType.INTEGER),


            //<string> -> "([^"]*)"
            State(listOf(
                Pattern("\""),
                Pattern((' ' .. '~').joinToString("").replace("\"", ""), PatternType.ZERO_OR_MORE), //Everything except double quote (")
                Pattern("\""),
            ), TokenType.STRING),


            //<identifier> -> [a-zA-Z_$][a-zA-Z_$0-9]*
            State(listOf(
                Pattern("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_$"),
                Pattern("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_$0123456789", PatternType.ZERO_OR_MORE),
            ), TokenType.IDENTIFIER),


            //Reserved keywords
            State(Pattern.fromLiteral("longitude"), TokenType.LONGITUDE).markReserved(),
            State(Pattern.fromLiteral("latitude"), TokenType.LATITUDE).markReserved(),

            State(Pattern.fromLiteral("Polygon"), TokenType.POLYGON).markReserved(),
            State(Pattern.fromLiteral("Circle"), TokenType.CIRCLE).markReserved(),
            State(Pattern.fromLiteral("Box"), TokenType.BOX).markReserved(),
            State(Pattern.fromLiteral("PolyLine"), TokenType.POLY_LINE).markReserved(),
            State(Pattern.fromLiteral("null"), TokenType.NULL).markReserved(),

            State(Pattern.fromLiteral("const"), TokenType.CONST).markReserved(),
            State(Pattern.fromLiteral("import"), TokenType.IMPORT).markReserved(),
            State(Pattern.fromLiteral("lambda"), TokenType.LAMBDA).markReserved(),
            State(Pattern.fromLiteral("return"), TokenType.RETURN).markReserved(),

            State(Pattern.fromLiteral("foreach"), TokenType.FOREACH).markReserved(),
            State(Pattern.fromLiteral("in"), TokenType.IN).markReserved(),
            State(Pattern.fromLiteral("if"), TokenType.IF).markReserved(),
            State(Pattern.fromLiteral("else"), TokenType.ELSE).markReserved(),

            State(Pattern.fromLiteral("cadastre"), TokenType.CADASTRE).markReserved(),
            State(Pattern.fromLiteral("lot"), TokenType.LOT).markReserved(),
            State(Pattern.fromLiteral("road"), TokenType.ROAD).markReserved(),
            State(Pattern.fromLiteral("buildingLot"), TokenType.BUILDING_LOT).markReserved(),
            State(Pattern.fromLiteral("farmLand"), TokenType.FARM_LAND).markReserved(),
            State(Pattern.fromLiteral("forest"), TokenType.FOREST).markReserved(),
            State(Pattern.fromLiteral("meadow"), TokenType.MEADOW).markReserved(),
            State(Pattern.fromLiteral("river"), TokenType.RIVER).markReserved(),
            State(Pattern.fromLiteral("lake"), TokenType.LAKE).markReserved(),

            State(Pattern.fromLiteral("risk"), TokenType.RISK).markReserved(),
            State(Pattern.fromLiteral("flood"), TokenType.FLOOD).markReserved(),
            State(Pattern.fromLiteral("landSlide"), TokenType.LANDSLIDE).markReserved(),
            State(Pattern.fromLiteral("earthQuake"), TokenType.EARTHQUAKE).markReserved(),
        )
    }


    private fun read() : Char {
        val value = input.read().toChar()
        col++

        if(value == '\n'){
            row++
            col = 1
        }

        return value
    }


    private fun nextTokenInternal() : Token {
        var lexeme = ""
        var states = this.states.map { it.clone() } //Possible states (starting with all)

        val startCol = col
        val startRow = row


        if(input.peek() == -1){
            return Token("", startCol, startRow, TokenType.EOF)
        }


        while(true){
            val nextChar = input.peek().toChar()
            val nextProgressions = states.map { it.next(nextChar) }
            val nextStates = states.filterIndexed { index, _ -> nextProgressions[index] == ProgressionState.ADVANCE }
            var finalStates = states.filterIndexed { index, _ -> nextProgressions[index] == ProgressionState.FINAL }

            //No possible next states, check for final state
            if(nextStates.isEmpty()){

                //Multiple final states, probably overlap between reserved keywords and identifiers
                if(finalStates.size > 1){
                    val reservedFinalStates = finalStates.filter { it.isReserved() }

                    if(reservedFinalStates.isEmpty()){
                        throw Exception("Multiple ambiguous final states")
                    }
                    else if(reservedFinalStates.size > 1){
                        throw Exception("Multiple ambiguous reserved final states. Keyword overlap!")
                    }

                    finalStates = reservedFinalStates //Only keep reserved final state (discard overlap between variables and reserved keywords)
                }
                else if(finalStates.isEmpty()){
                    return Token(lexeme, startCol, startRow, TokenType.LEXICAL_ERROR)
                }

                //Reached one final state
                val finalState = finalStates[0]

                if(finalState.getTokenType() == TokenType.IGNORED_TOKEN){
                    return nextTokenInternal() //Skip ignored tokens
                }

                return Token(lexeme, startCol, startRow, finalState.getTokenType())
            }

            lexeme += read()
            states = nextStates
        }
    }


    fun nextToken() : Token {
        currentToken = nextTokenInternal()
        return currentToken!!
    }

}