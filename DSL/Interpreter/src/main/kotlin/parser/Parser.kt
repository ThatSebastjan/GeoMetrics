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
            return start()
        }
        catch (e: ParseException){
            println("Parse exception: $e")
            return null
        }
    }


    private fun advance() : Token {
        if(index != tokens.lastIndex){
            index++
        }

        return previous()
    }


    private fun peek() : Token {
        if(index < tokens.lastIndex){
            return tokens[index + 1]
        }

        return current()
    }


    private fun expect(type: TokenType) : Token {
        val token = advance()

        if(!token.isOfType(type)){
            throw ParseException("Expected token of type $type, but got ${token.getType()} at position ${token.row}:${token.column}", token)
        }

        return token
    }


    private fun isCurrent(vararg types: TokenType) : Boolean {
        for(t in types){
            if(checkType(t)){
                advance()
                return true
            }
        }

        return false
    }


    private fun isNext(vararg types: TokenType) : Boolean {
        if(index == tokens.lastIndex){
            return false
        }

        for(t in types){
            if(tokens[index + 1].isOfType(t)){
                return true
            }
        }

        return false
    }


    private fun checkType(type: TokenType) = tokens[index].isOfType(type)

    private fun previous() = tokens[index - 1]

    private fun current() = tokens[index]



    //Starting production
    private fun start() : Node {
       // return programBlock()

        return numericExpr() //TEMPORARY
    }


    /*
    private fun programBlock(outExpr: Expr = Expr.ProgramBlock()) : Expr {

        if(isNext(TokenType.CONST, TokenType.CADASTRE, TokenType.RISK, TokenType.FLOOD, TokenType.LANDSLIDE, TokenType.EARTHQUAKE)){
            val block = programBlock_()
            return programBlock(Expr.ProgramBlock(outExpr as Expr.ProgramBlock, block))
        }

        return outExpr
    }


    private fun programBlock_() : Expr {

    }
    */


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

                val num = expect(TokenType.NUMBER)
                val value = Node.NumericType(num.getLexeme().toDouble())
                return Node.Negative(value)
            }

            TokenType.NUMBER -> return Node.NumericType(advance().getLexeme().toDouble())

            TokenType.INTEGER -> return Node.NumericType(advance().getLexeme().toDouble())

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
        val ident = expect(TokenType.IDENTIFIER)

        //todo: FINISH THIS!

        return Node.NumericType(21212112.21211)
    }

}