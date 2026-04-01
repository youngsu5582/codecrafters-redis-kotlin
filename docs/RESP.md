## RESP

- 참고 링크 : https://sgc109.github.io/2020/07/22/redis-resp-protocol/

Redis 클라이언트가 Redis 서버와 통신할 때 사용하는 프로토콜
REdis Serialization Protocol

- Redis 클라이언트가 구현해야만 하는 표준 프로토콜
- Client - Server 간 통신에서 사용 (Cluster 에서 노드간 통신은 다른 걸 사용)

3가지 요소를 중점으로 여기고, 고안되었다고 한다...?

- 쉬운 구현
- 빠른 파싱
- 사람이 읽을 수 있다. human readable

- 데이터 타입

첫 바이트로 데이터 타입을 구분한다.

```
`+` : Simple String
`:` : Integer
`$` : Bulk String
`-` : Error
`*` : Array
```

클라이언트는 Bulk String 의 Array 타입으로 명령어를 서버에 전송
-> 서버는 클라이언트가 보낸 명령어에 맞는 타입으로 응답

- Simple Strings

binary-safe 하지 않은 일반 문자열 전송할 때 사용하는 타입
EX) `“+OK\r\n”`

- Errors

에러 정보에 대한 타입
관습적으로 에러 이름 먼저 쓰고, 그 다음 상세 정보 적음

EX) `"-ERR unknown command 'foobar'\r\n"`

- Integers

정수 형태 데이터
숫자 크기는 signed 64 bit 범위 내로 지정
`INCR`, `LLEN`, `LASTSAVE` 같은 명령어에 대한 응답
`EXISTS` 는 true/false 로 Integers 타입 1/0 을 사용

EX) `:1\r\n`

- Bulk Strings

binary-safe 한 문자열
`$` 에 이어 문자열 길이 주어지고, `"\r\n"` 이후, 실제 문자열 등장

EX) `"$6\r\nfoobar\r\n"`
빈 문자열: `"$0\r\n\r\n`

- Arrays

배열 뒤 10진수 배열 크기와 `"r\n"`
배열 내 원소는 각각 특정한 타입을 가진다.(그 타입은 모두 달라도 괜찮음)

`"*3\r\n:1\r\n+2\r\n$4\r\nbulk\r\n`

-> 배열 크기는 3
- 첫번째 요소는 Integer 1
- 두번째 요소는 Simple String "2"
- 세번째 요소는 Bulk String "bulk"

