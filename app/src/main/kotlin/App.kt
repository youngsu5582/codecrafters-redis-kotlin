import DataType.INTEGERS
import java.io.BufferedReader
import java.io.EOFException
import java.net.ServerSocket

// 일단, 전역 캐시로 설정. 필요에 따라 리팩토링
val cache = Cache()

fun main(args: Array<String>) {
    // 레디스는 TCP 통신한다. - 클라이언트, 서버간 신뢰있는 데이터 교환
    val serverSocket = ServerSocket(6379)
    serverSocket.reuseAddress = true

    while (true) {
        val socket = serverSocket.accept() // Wait for connection from client.
        println("accepted new connection")

        val reader = socket.getInputStream().bufferedReader()
        val writer = socket.getOutputStream().bufferedWriter()
        Thread.startVirtualThread {
            try {
                while (true) {
                    // type + length
                    val respValue = readData(reader)
                    println(respValue)
                    if (respValue !is RespValue.Array) {
                        // 배열 형태가 아니면 빠른 반환
                        writer.write("unexpected type: $respValue")
                        return@startVirtualThread
                    }

                    val result = executeCommand(respValue)
                    println("result: $result")

                    writer.write(result)
                    writer.flush()
                }
            } catch (e: EOFException) {
                println("client disconnected")
            }
        }

    }
}

private fun executeCommand(value: RespValue.Array): String {
    println("executing command $value")
    val args = value.value.map { (it as RespValue.BulkString).value }
    val command = args[0].uppercase()
    if (command == "PING") {
        return convertData(RespData(DataType.SIMPLE_STRING, "PONG"))
    }
    if (command == "ECHO") {
        return convertData(RespData(DataType.BULK_STRING, args[1]))
    }

    if (command == "RPUSH") {
        val key = args[1]
        val value = args[2]
        val size = cache.rightPush(key, value)
        return convertData(RespData(DataType.INTEGERS, size.toString()))
    }

    if (command == "SET") {
        val key = args[1]
        val value = args[2]
        if (args.size <= 3) {
            cache.put(key, value)
            return convertData(RespData(DataType.SIMPLE_STRING, "OK"))
        }

        val option = TimeOption.from(args[3])
        val number = args[4].toLong()

        cache.put(key, value, option.toMills(number))
        return convertData(RespData(DataType.SIMPLE_STRING, "OK"))
    }
    if (command == "GET") {
        val key = args[1]
        val value = cache.get(key)
        if (value == null) {
            return convertData(RespData(DataType.BULK_STRING, null))
        }
        return convertData(RespData(DataType.BULK_STRING, value))
    }

    throw IllegalArgumentException("Unknown command $command")
}


private fun readData(reader: BufferedReader): RespValue {
    val line: String = reader.readLine() ?: throw EOFException("Connection closed")

    // type + value
    // 1차 파싱
    val data = parseData(line)
    println("data: $data")
    if (data.value == null) {
        throw EOFException("No data found")
    }
    when (data.type) {

        DataType.BULK_STRING -> {
            val length = data.value.toInt()
            val strings = reader.readLine()
            return RespValue.BulkString(strings)
        }

        INTEGERS -> {
            return RespValue.Integers(data.value.toInt())
        }

        DataType.SIMPLE_STRING -> {
            return RespValue.SimpleString(data.value)
        }

        DataType.ERRORS -> {
            return RespValue.Error(data.value)
        }

        DataType.ARRAY -> {
            val length = data.value.toInt()
            val list = mutableListOf<RespValue>()
            for (i in 0 until length) {
                val data = readData(reader)
                list.add(data)
            }
            return RespValue.Array(list.toList())
        }
    }
}

/**
 * 파싱된 데이터
 * "+OK\r\n" -> Simple String 과 OK
 * ":10\r\n" -> Integers 과 10
 * "-ERR unknown command 'foobar'\r\n" -> Errors 와 ERR unknown command 'foobar'
 * "$6\r\nfoobar\r\n" -> Bulk Strings 와 foobar
 * “*3\r\n:1\r\n+2\r\n$4\r\nbulk\r\n” -> Integer 1 + Simple String "2" + Bulk Strings "bulk"
 */
sealed class RespValue {
    data class SimpleString(val value: String) : RespValue()
    data class BulkString(val value: String) : RespValue()
    data class Error(val value: String) : RespValue()
    data class Integers(val value: Int) : RespValue()
    data class Array(val value: List<RespValue>) : RespValue()
}


/**
 * 파싱 전 데이터
 * "+OK\r\n" -> Simple String 과 OK
 * ":10\r\n" -> Integers 과 10
 * "-ERR unknown command 'foobar'\r\n" -> Errors 와 ERR unknown command 'foobar'
 * "$6\r\n" -> Bulk Strings 과 6
 * "*3\r\n" -> Arrays 과 3
 */
data class RespData(
    val type: DataType,
    // 차차, 구조를 생각하면 Sealed Class 로 바뀌는게 맞는거 같긴 한데
    // 당장 최소한의 변경으로 구현하기 위해 String -> String? nullable 로 변경
    val value: String?
)

enum class DataType(val value: Char) {
    SIMPLE_STRING('+'),
    ERRORS('-'),
    INTEGERS(':'),
    ARRAY('*'),
    BULK_STRING('$'),
    ;

    companion object {
        fun from(value: Char): DataType {
            return when (value) {
                ARRAY.value -> ARRAY
                BULK_STRING.value -> BULK_STRING
                ERRORS.value -> ERRORS
                INTEGERS.value -> INTEGERS
                SIMPLE_STRING.value -> SIMPLE_STRING
                else -> throw IllegalArgumentException("Unknown data type: $value")
            }
        }
    }
}

fun parseData(line: String): RespData {
    val type = DataType.from(line.first())
    return RespData(type, line.substring(1))
}

fun convertData(data: RespData): String {
    return when (data.type) {
        DataType.SIMPLE_STRING -> {
            "+" + data.value + Constant.LINE_SEPARATOR
        }

        DataType.ERRORS -> {
            "-" + data.value + Constant.LINE_SEPARATOR
        }

        INTEGERS -> {
            ":" + data.value + Constant.LINE_SEPARATOR
        }

        DataType.BULK_STRING -> {
            if (data.value == null) {
                "$" + -1 + Constant.LINE_SEPARATOR
            } else {
                "$" + data.value.length + Constant.LINE_SEPARATOR + data.value + Constant.LINE_SEPARATOR
            }
        }

        DataType.ARRAY -> {
            throw NotImplementedError("TODO Implemented!")
        }
    }
}

object Constant {
    const val LINE_SEPARATOR: String = "\r\n"
}