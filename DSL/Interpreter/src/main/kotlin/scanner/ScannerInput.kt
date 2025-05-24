package scanner

import java.io.InputStream



class ScannerInput(private val input: InputStream) {
    private var cachedVal: Int = 0
    private var peekOffset = 0
    private var readOffset = 0


    fun peek() : Int {
        if(peekOffset > readOffset){
            return cachedVal
        }

        peekOffset++
        cachedVal = input.read()
        return cachedVal
    }


    fun read() : Int {
        if(peekOffset > readOffset){
            peekOffset = ++readOffset
            return cachedVal
        }

        peekOffset = ++readOffset
        return input.read()
    }
}