### PING

레디스는 클라이언트가 서버에 Command 를 보내고, 응답을 받는형태이다.

PING 은 레디스의 가장 간단한 명령어

```
❯ redis-cli                                                                                                                                                    21:49
127.0.0.1:6379> PING
PONG
```

- RESP : REdis Serialization Protocol, `클라이언트 - 서버` 간 통신에 사용하는 직렬화 프로토콜
  - 텍스트 기반, human readable
  - 파싱이 간단하고 빠름
  - \r\n (CRLF) 로 각 데이터 단위 구분
  - 첫 바이트로 데이터 타입을 구분한다.

- 데이터 타입

```
`+` : Simple String
`:` : Integer
`$` : Bulk String
`*` : Array
```

사실, 네트워크는 `+PONG\r\n` 로 반환한다.
`+` (Simple String) + `PONG` + `\r\n`