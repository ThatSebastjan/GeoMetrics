package scanner


enum class PatternType {
    ONE_OR_MORE, //Regex equivalent of "+" modifier
    ZERO_OR_MORE, //Regex equivalent of "*" modifier
    EXACT
}

enum class ProgressionState {
    NO_PROGRESSION,
    ADVANCE,
    FINAL
}



class Pattern(private val pattern: String, val type: PatternType = PatternType.EXACT) {

    fun matches(char: Char) = pattern.contains(char)

    companion object {
        fun fromLiteral(str: String) = str.toList().map { Pattern(it.toString()) }
    }
}



class State(private val progression: List<Pattern>, private val token: TokenType) {
    private var index = 0 //Progression index
    private var prevIndex = -1 //Index used for ONE_OR_MORE matches
    private var reserved = false //Reserved keyword flag

    init {
        if(progression.isEmpty()){
            throw IllegalArgumentException("State progression list cannot be empty")
        }
    }

    //Advance progression, if possible
    fun next(char: Char) : ProgressionState {

        while(index < progression.size){
            val currentPattern = progression[index]

            when(currentPattern.type) {

                PatternType.EXACT -> {
                    index++

                    return when (currentPattern.matches(char)){
                        true -> { ProgressionState.ADVANCE }
                        false -> { ProgressionState.NO_PROGRESSION }
                    }
                }

                PatternType.ZERO_OR_MORE -> {
                    if(currentPattern.matches(char)){
                        return ProgressionState.ADVANCE; //Keep matching the same pattern as long as possible
                    }

                    index++ //Try next pattern
                }

                PatternType.ONE_OR_MORE -> {
                    if(currentPattern.matches(char)){
                        prevIndex = index
                        return ProgressionState.ADVANCE; //Keep matching the same pattern as long as possible
                    }

                    //If no match, match next if matched at least once
                    if(index == prevIndex){
                        index++
                    }
                    else {
                        return ProgressionState.NO_PROGRESSION //Failed to match at least once
                    }
                }

            }
        }

        return ProgressionState.FINAL
    }


    fun clone(): State {
        val tmp = State(progression, token)
        tmp.index = index
        tmp.prevIndex = prevIndex
        tmp.reserved = reserved
        return tmp
    }


    fun markReserved(): State {
        reserved = true
        return this
    }


    fun isReserved() = reserved

    fun getTokenType() = token

}