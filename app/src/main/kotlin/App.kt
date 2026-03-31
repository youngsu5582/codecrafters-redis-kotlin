import java.net.ServerSocket

fun main(args: Array<String>) {
    // 레디스는 TCP 통신한다. - 클라이언트, 서버간 신뢰있는 데이터 교환
    val serverSocket = ServerSocket(6379)
    serverSocket.reuseAddress = true

    val socket = serverSocket.accept() // Wait for connection from client.
    println("accepted new connection")

    val reader = socket.getInputStream().bufferedReader()
    val outputStream = socket.getOutputStream()

    while (true) {
        // *1\r\n$4\r\nPING\r\n 형식으로 온다
        val line: String = reader.readLine() ?: break
        if (line.equals("PING", ignoreCase = true)) {
            outputStream.write(convertREsp("PONG"))
            outputStream.flush()
        }
    }
    println("Finish")
}


private fun convertREsp(line: String): ByteArray =
    "+$line\r\n".toByteArray()
