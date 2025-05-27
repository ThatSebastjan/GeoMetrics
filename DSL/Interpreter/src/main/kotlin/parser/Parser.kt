package parser

import scanner.Scanner
import scanner.Token
import scanner.TokenType



class ParseException(message: String, val token: Token): Exception(message)


class Parser(private val scanner: Scanner) {
    private val tokens = mutableListOf<Token>()
    private var index = 0


    fun parse(): Node? {

        //Get all tokens
        while(true){
            val tkn = scanner.nextToken()
            tokens.add(tkn)

            if(tkn.isOfType(TokenType.EOF)){
                break
            }
            else if (tkn.isOfType(TokenType.LEXICAL_ERROR)){
                println("Lexical error at row: ${tkn.row}, column: ${tkn.column} (lexeme: ${tkn.getLexeme()})")
                return null
            }

        }

        //Parse and return AST, if successful
        try {
            val ast = start()

            if(!current().isOfType(TokenType.EOF)){
                println("Parse error: unexpected end of input! Not all tokens consumed");
                return null;
            }

            return ast
        }
        catch (e: ParseException){
            println("Parse exception: $e")
        }
        catch(e: Exception){
            println("Error while parsing: $e")
        }

        return null
    }


    private fun advance() : Token {
        if(index != tokens.lastIndex){
            index++
        }

        return previous()
    }


    private fun expect(type: TokenType) : Token {
        val token = advance()

        if(!token.isOfType(type)){
            throw ParseException("Expected token of type $type, but got $token at position ${token.row}:${token.column}", token)
        }

        return token
    }


    private fun expectTypes(vararg types: TokenType) : Token {
        val token = advance()

        for(t in types){
            if(token.isOfType(t)){
                return token
            }
        }

        throw ParseException("Expected token to be one of types (${types.joinToString { it.toString() }}), but got $token at position ${token.row}:${token.column}", token)
    }


    private fun previous() = tokens[index - 1]

    private fun current() = tokens[index]



    //Starting production
    private fun start() : Node {
        return programBlock()
    }


    private fun numericExpr() : Node {
        return addSub()
    }


    private fun addSub() : Node {
        val left = mulDiv()
        return addSubP(left)
    }


    private fun addSubP(left: Node) : Node {

        when(current().getType()){

            TokenType.PLUS -> {
                advance()

                val right = mulDiv()
                val value = Node.Add(left, right)
                return addSubP(value) //Self recursive; left associative
            }

            TokenType.MINUS -> {
                advance()

                val right = mulDiv()
                val value = Node.Sub(left, right)
                return addSubP(value)
            }

            else -> return left //Epsilon, return input value
        }
    }


    private fun mulDiv() : Node {
        val left = numericType()
        return mulDivP(left)
    }


    private fun mulDivP(left: Node) : Node {

        when(current().getType()){

            TokenType.TIMES -> {
                advance()

                val right = numericType()
                val value = Node.Mul(left, right)
                return mulDivP(value) //Self recursive
            }

            TokenType.DIVIDE -> {
                advance()

                val right = numericType()
                val value = Node.Div(left, right)
                return mulDivP(value) //Self recursive
            }

            else -> return left //Epsilon, return input value
        }
    }


    private fun numericType() : Node {

        when(current().getType()){

            TokenType.MINUS -> {
                advance()

                val num = expectTypes(TokenType.NUMBER, TokenType.INTEGER) //NOTE: in case of integer value, it is transformed into a double (Node.Number type)
                return Node.Number(-num.getLexeme().toDouble())
            }

            TokenType.NUMBER -> return Node.Number(advance().getLexeme().toDouble())

            TokenType.INTEGER -> return Node.Number(advance().getLexeme().toDouble())

            TokenType.LPAREN -> {
                advance()

                val value = addSub()
                expect(TokenType.RPAREN)

                return value
            }

            else -> return constNumericType()
        }
    }


    private fun constNumericType() : Node {
        val identifier = expect(TokenType.IDENTIFIER)
        return constNumericProperty(Node.Identifier(identifier.getLexeme()))
    }


    private fun constNumericProperty(identifier: Node.Identifier) : Node {

        when(current().getType()){

            TokenType.DOT -> {
                advance()

                val propertyNode = constNumericPropertyP()
                return Node.PropertyAccess(identifier, propertyNode)
            }

            TokenType.LPAREN -> return callArgs(identifier)

            else -> return identifier //Epsilon, return input constant identifier
        }
    }


    //TODO("Maybe make this return separate Node types for latitude and longitude?!")
    private fun constNumericPropertyP() : Node.Identifier {
        val propertyToken = expectTypes(TokenType.LONGITUDE, TokenType.LATITUDE)
        return Node.Identifier(propertyToken.getLexeme())
    }


    private fun callArgs(identifier: Node.Identifier) : Node {
        expect(TokenType.LPAREN)

        val arg0 = numericExpr()
        val argsNode = callArgsP(mutableListOf(arg0))

        expect(TokenType.RPAREN)
        return Node.Call(identifier, argsNode)
    }


    private fun callArgsP(args: MutableList<Node>) : MutableList<Node> {
        if(current().isOfType(TokenType.COMMA)){
            advance()

            val newArg = numericExpr()
            args.add(newArg)
            return callArgsP(args)
        }

        return args //Epsilon, return list
    }



    private fun pointType() : Node.Point {
        expect(TokenType.LT)
        val longitude = numericExpr()
        expect(TokenType.COMMA)
        val latitude = numericExpr()
        expect(TokenType.GT)

        return Node.Point(longitude, latitude)
    }


    private fun polygonType() : Node.Polygon {
        expect(TokenType.POLYGON)
        expect(TokenType.LPAREN)

        val points = polygonPoints()
        expect(TokenType.RPAREN)

        return Node.Polygon(points)
    }


    private fun polygonPoints() : List<Node.Point> {
        val point0 = pointType()
        return polygonPointsP(mutableListOf(point0))
    }


    private fun polygonPointsP(points: MutableList<Node.Point>) : MutableList<Node.Point> {
        if(current().isOfType(TokenType.COMMA)){
            advance()

            val newPoint = pointType()
            points.add(newPoint)
            return polygonPointsP(points)
        }

        return points //Epsilon, return list
    }


    private fun circleType() : Node.Circle {
        expect(TokenType.CIRCLE)
        expect(TokenType.LPAREN)

        val centrePoint = pointType()
        expect(TokenType.COMMA)

        val radius = numericExpr()
        expect(TokenType.RPAREN)

        return Node.Circle(centrePoint, radius)
    }


    private fun boxType() : Node.Box {
        expect(TokenType.BOX)
        expect(TokenType.LPAREN)

        val topLeft = pointType()
        expect(TokenType.COMMA)

        val bottomRight = pointType()
        expect(TokenType.RPAREN)

        return Node.Box(topLeft, bottomRight)
    }


    private fun polyLineType() : Node.PolyLine {
        expect(TokenType.POLY_LINE)
        expect(TokenType.LPAREN)

        val width = numericExpr()
        expect(TokenType.COMMA)

        val points = polygonPoints()
        expect(TokenType.RPAREN)

        return Node.PolyLine(width, points)
    }


    private fun boundsType() : Node { //Either one of Polygon, Circle, Box, PolyLine or a function call, indexed imported data access or a constant

        when(current().getType()){
            TokenType.POLYGON -> return polygonType()
            TokenType.CIRCLE -> return circleType()
            TokenType.BOX -> return boxType()
            TokenType.POLY_LINE -> return polyLineType()

            else -> {
                val identifier = expect(TokenType.IDENTIFIER)
                return boundsTypeP(Node.Identifier(identifier.getLexeme()))
            }
        }
    }


    private fun boundsTypeP(identifier: Node.Identifier) : Node {
        if(current().isOfType(TokenType.LPAREN)){
             return callArgs(identifier)
        }

        return constArrayIndexed(identifier)
    }


    private fun constArrayIndexed(identifier: Node.Identifier) : Node {
        if(current().isOfType(TokenType.LSQUARE)){
            advance()

            val index = expect(TokenType.INTEGER)
            expect(TokenType.RSQUARE)

            return Node.IndexAccess(identifier, Node.Integer(index.getLexeme().toInt()))
        }

        return identifier //Epsilon, identifier is a constant of bounds type (type checked at runtime)
    }



    private fun constDef() : Node.ConstantDefinition {
        expect(TokenType.CONST)

        val identifier = expect(TokenType.IDENTIFIER)
        expect(TokenType.ASSIGN)

        val value = constValue()
        expect(TokenType.SEMI)

        return Node.ConstantDefinition(identifier.getLexeme(), value)
    }


    private fun constValue() : Node {

        when(current().getType()){
            TokenType.LT -> return pointType()
            TokenType.POLYGON -> return polygonType()
            TokenType.CIRCLE -> return circleType()
            TokenType.BOX -> return boxType()
            TokenType.POLY_LINE -> return polyLineType()

            TokenType.IMPORT -> {
                advance()

                val strToken = expect(TokenType.STRING)
                val strValue = strToken.getLexeme().drop(1).dropLast(1) //String without quotes
                return Node.Import(strValue)
            }

            TokenType.LAMBDA -> return lambdaExpr()

            else -> return numericExpr() //This has multiple elements in its first set so everything else is checked first
        }
    }


    private fun propertyDef() : Node.PropertyDefinition {
        expect(TokenType.DOT)

        val name = expect(TokenType.IDENTIFIER)
        expect(TokenType.COLON)

        val value = propertyValue()
        expect(TokenType.SEMI)

        return Node.PropertyDefinition(name.getLexeme(), value)
    }


    private fun propertyValue() : Node {

        when(current().getType()){
            TokenType.NUMBER -> return Node.Number(advance().getLexeme().toDouble())
            TokenType.INTEGER -> return Node.Integer(advance().getLexeme().toInt())
            TokenType.STRING -> return Node.StringValue(advance().getLexeme().drop(1).dropLast(1)) //Discard quotes

            TokenType.NULL -> {
                advance()
                return Node.Null()
            }

            else -> return boundsType() //Other types are checked first as they have a single element in First set
        }
    }


    private fun lambdaExpr() : Node.Lambda {
        expect(TokenType.LAMBDA)
        expect(TokenType.LPAREN)

        val params = lambdaParams()
        expect(TokenType.RPAREN)
        expect(TokenType.LCURLY)

        val body = lambdaBody()
        expect(TokenType.RCURLY)

        return Node.Lambda(params, body)
    }


    private fun lambdaParams() : List<Node.Identifier> {
        val paramToken = expect(TokenType.IDENTIFIER)
        val param = Node.Identifier(paramToken.getLexeme())

        return lambdaParamsP(mutableListOf(param))
    }


    private fun lambdaParamsP(params: MutableList<Node.Identifier>) : MutableList<Node.Identifier> {
        if(current().isOfType(TokenType.COMMA)){
            advance()

            val newParamToken = expect(TokenType.IDENTIFIER)
            val newParam = Node.Identifier(newParamToken.getLexeme())
            params.add(newParam)

            return lambdaParamsP(params)
        }

        return params
    }

    private fun lambdaBody() : Node.LambdaBody {
        val constDefs = constDefList()
        val retVal = returnValue()
        expect(TokenType.SEMI)

        return Node.LambdaBody(constDefs, retVal)
    }


    private fun constDefList() : List<Node.ConstantDefinition> {
        val list = mutableListOf<Node.ConstantDefinition>()

        while(current().isOfType(TokenType.CONST)){
            val def = constDef()
            list.add(def)
        }

        return list //Epsilon, return accumulated const definitions
    }


    private fun returnValue() : Node {
        expect(TokenType.RETURN)
        return returnType()
    }


    private fun returnType() : Node { //Lambdas can return a number or a bounds type (cannot return a point)
        if(current().isOfType(TokenType.NUMBER)){
            return Node.Number(advance().getLexeme().toDouble())
        }

        return boundsType()
    }



    private fun forEachLoop() : Node.ForEach {
        expect(TokenType.FOREACH)

        val varName = expect(TokenType.IDENTIFIER)
        expect(TokenType.IN)

        val arrayName = expect(TokenType.IDENTIFIER)
        expect(TokenType.LCURLY)

        val content = controlFlowBlockContent()
        expect(TokenType.RCURLY)

        return Node.ForEach(varName.getLexeme(), arrayName.getLexeme(), content)
    }


    private fun ifStatement() : Node.Conditional {
        expect(TokenType.IF)
        expect(TokenType.LPAREN)

        val condition = booleanExpr()
        expect(TokenType.RPAREN)
        expect(TokenType.LCURLY)

        val ifBlock = controlFlowBlockContent()
        expect(TokenType.RCURLY)

        val elseBlockData = elseBlock()
        return Node.Conditional(condition, ifBlock, elseBlockData)
    }


    private fun elseBlock() : Node.Block? { //Else is optional
        if(current().isOfType(TokenType.ELSE)) {
            advance()
            expect(TokenType.LCURLY)

            val blockData = controlFlowBlockContent()
            expect(TokenType.RCURLY)

            return blockData
        }

        return null
    }


    private fun controlFlowBlockContent() : Node.Block {
        val nodes = mutableListOf<Node>()

        while(current().getType() in listOf(TokenType.CONST, TokenType.LOT, TokenType.RISK,
                TokenType.FOREACH, TokenType.IF, TokenType.ROAD,
                TokenType.BUILDING_LOT, TokenType.FARM_LAND, TokenType.FOREST,
                TokenType.MEADOW, TokenType.RIVER, TokenType.LAKE,
                TokenType.FLOOD, TokenType.LANDSLIDE, TokenType.EARTHQUAKE)){

            nodes.add(controlFlowBlockContentP())
        }

        return Node.Block(nodes) //Epsilon, return block data
    }


    private fun controlFlowBlockContentP() : Node {

        when(current().getType()){
            TokenType.CONST -> return constDef()

            TokenType.FOREACH, TokenType.IF -> return controlFlowDef()

            TokenType.RISK, TokenType.FLOOD, TokenType.LANDSLIDE, TokenType.EARTHQUAKE -> return riskBlock()

            else -> return lotBlock() //This one has the most elements in its First set
        }
    }


    private fun controlFlowDef() : Node {
        if(current().isOfType(TokenType.FOREACH)){
            return forEachLoop()
        }

        return ifStatement()
    }


    private fun booleanExpr() : Node.BooleanExpression {
        val left = numericExpr()
        val operator = expectTypes(TokenType.LT, TokenType.GT)
        val right = numericExpr()

        if(operator.isOfType(TokenType.LT)){
            return Node.BooleanExpression(Node.LessThan(left, right))
        }

        return Node.BooleanExpression(Node.GreaterThan(left, right))
    }



    private fun cadastreBlock() : Node.Cadastre {
        expect(TokenType.CADASTRE)
        expect(TokenType.LCURLY)

        val blockData = cadastreContent()
        expect(TokenType.RCURLY)
        return Node.Cadastre(blockData)
    }


    private fun cadastreContent() : List<Node> {
        val content = mutableListOf<Node>()

        while(current().getType() in listOf(TokenType.CONST, TokenType.DOT, TokenType.LOT,
                TokenType.FOREACH, TokenType.IF, TokenType.ROAD,
                TokenType.BUILDING_LOT, TokenType.FARM_LAND, TokenType.FOREST,
                TokenType.MEADOW, TokenType.RIVER, TokenType.LAKE)){

            content.add(cadastreContentP())
        }

        return content //Epsilon, return block elements
    }


    private fun cadastreContentP() : Node {

        when(current().getType()){
            TokenType.CONST -> return constDef()

            TokenType.DOT -> return propertyDef()

            TokenType.FOREACH, TokenType.IF -> return controlFlowDef()

            else -> return lotBlock() //This one has the most elements in First set so simpler ones are checked
        }
    }




    private fun propertyDefList() : List<Node.PropertyDefinition> {
        val list = mutableListOf<Node.PropertyDefinition>()

        while(current().isOfType(TokenType.DOT)){
            list.add(propertyDef())
        }

        return list //Epsilon, reached the end
    }


    private fun lotBlock() : Node.Lot {

        when(current().getType()){
            TokenType.ROAD -> return roadLot()
            TokenType.BUILDING_LOT -> return buildingLot()
            TokenType.FARM_LAND -> return farmLandLot()
            TokenType.FOREST -> return forestLot()
            TokenType.MEADOW -> return meadowLot()
            TokenType.RIVER -> return riverLot()
            TokenType.LAKE -> return lakeLot()

            else -> { //Normal lot definition
                expect(TokenType.LOT)
                expect(TokenType.LCURLY)

                val properties = propertyDefList()
                expect(TokenType.RCURLY)
                return Node.Lot(properties)
            }
        }
    }


    private fun lotSimplifiedArgs() : MutableList<Node.PropertyDefinition> { //Returns parsed properties
        expect(TokenType.LPAREN)

        val lotIdToken = expect(TokenType.STRING)
        val lotId = Node.StringValue(lotIdToken.getLexeme())
        expect(TokenType.COMMA)

        val lotBounds = boundsType()
        expect(TokenType.RPAREN)
        expect(TokenType.SEMI)

        return mutableListOf(
            Node.PropertyDefinition("id", lotId),
            Node.PropertyDefinition("bounds", lotBounds)
        )
    }


    private fun roadLot() : Node.Lot {
        expect(TokenType.ROAD)

        val props = lotSimplifiedArgs()
        props.add(Node.PropertyDefinition("type", Node.Identifier("Road")))
        return Node.Lot(props)
    }


    private fun buildingLot() : Node.Lot {
        expect(TokenType.BUILDING_LOT)

        val props = lotSimplifiedArgs()
        props.add(Node.PropertyDefinition("type", Node.Identifier("BuildingLot")))
        return Node.Lot(props)
    }


    private fun farmLandLot() : Node.Lot {
        expect(TokenType.FARM_LAND)

        val props = lotSimplifiedArgs()
        props.add(Node.PropertyDefinition("type", Node.Identifier("FarmLand")))
        return Node.Lot(props)
    }


    private fun forestLot() : Node.Lot {
        expect(TokenType.FOREST)

        val props = lotSimplifiedArgs()
        props.add(Node.PropertyDefinition("type", Node.Identifier("Forest")))
        return Node.Lot(props)
    }


    private fun meadowLot() : Node.Lot {
        expect(TokenType.MEADOW)

        val props = lotSimplifiedArgs()
        props.add(Node.PropertyDefinition("type", Node.Identifier("Meadow")))
        return Node.Lot(props)
    }


    private fun riverLot() : Node.Lot {
        expect(TokenType.RIVER)

        val props = lotSimplifiedArgs()
        props.add(Node.PropertyDefinition("type", Node.Identifier("River")))
        return Node.Lot(props)
    }


    private fun lakeLot() : Node.Lot {
        expect(TokenType.LAKE)

        val props = lotSimplifiedArgs()
        props.add(Node.PropertyDefinition("type", Node.Identifier("Lake")))
        return Node.Lot(props)
    }


    private fun riskBlock() : Node.Risk {

        when(current().getType()){
            TokenType.FLOOD -> return floodRisk()
            TokenType.LANDSLIDE -> return landSlideRisk()
            TokenType.EARTHQUAKE -> return earthquakeRisk()

            else -> { //Normal risk block definition
                expect(TokenType.RISK)
                expect(TokenType.LCURLY)

                val props = propertyDefList()
                expect(TokenType.RCURLY)
                return Node.Risk(props)
            }
        }
    }


    private fun riskSimplifiedArgs() : MutableList<Node.PropertyDefinition> {
        expect(TokenType.LPAREN)

        val bounds = boundsType()
        expect(TokenType.COMMA)

        val probability = numericExpr()
        expect(TokenType.COMMA)

        val frequency = numericExpr()
        expect(TokenType.RPAREN)
        expect(TokenType.SEMI)

        return mutableListOf(
            Node.PropertyDefinition("bounds", bounds),
            Node.PropertyDefinition("probability", probability),
            Node.PropertyDefinition("frequency", frequency)
        )
    }


    private fun floodRisk() : Node.Risk {
        expect(TokenType.FLOOD)

        val props = riskSimplifiedArgs()
        props.add(Node.PropertyDefinition("type", Node.Identifier("Flood")))
        return Node.Risk(props)
    }


    private fun landSlideRisk() : Node.Risk {
        expect(TokenType.LANDSLIDE)

        val props = riskSimplifiedArgs()
        props.add(Node.PropertyDefinition("type", Node.Identifier("LandSlide")))
        return Node.Risk(props)
    }


    private fun earthquakeRisk() : Node.Risk {
        expect(TokenType.EARTHQUAKE)

        val props = riskSimplifiedArgs()
        props.add(Node.PropertyDefinition("type", Node.Identifier("Earthquake")))
        return Node.Risk(props)
    }



    private fun programBlock() : Node.Block {
        val list = mutableListOf<Node>()

        //const, cadastre, risk, flood, landSlide, earthQuake, foreach, if
        while(current().getType() in listOf(TokenType.CONST, TokenType.CADASTRE, TokenType.RISK,
                TokenType.FLOOD, TokenType.LANDSLIDE, TokenType.EARTHQUAKE,
                TokenType.FOREACH, TokenType.IF)){

            list.add(programBlockP())
        }

        return Node.Block(list)
    }


    private fun programBlockP() : Node {

        when(current().getType()){
            TokenType.CONST -> return constDef()
            TokenType.CADASTRE -> return cadastreBlock()
            TokenType.FOREACH, TokenType.IF -> return controlFlowDef()

            else -> return riskBlock() //Others are checked as this one has more elements in First set
        }
    }

}
