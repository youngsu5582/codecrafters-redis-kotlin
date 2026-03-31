import java.net.ServerSocket

fun main(args: Array<String>) {
    // 레디스는 TCP 통신한다. - 클라이언트, 서버간 신뢰있는 데이터 교환
    val serverSocket = ServerSocket(6379)
    serverSocket.reuseAddress = true

    val socket = serverSocket.accept() // Wait for connection from client.
    println("accepted new connection")

    val inputStream = socket.getInputStream()
    val line = inputStream.bufferedReader().readLine()
    println(line)

    val outputStream = socket.getOutputStream()
    outputStream.write("+PONG\r\n".toByteArray())
}
