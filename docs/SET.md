## SET

- O(1)

key 에 value 를 저장한다.

- key 에 이미 값이 있으면, 덮어쓴다.
- SET 이 성공하면, 기존 설정된 TTL 은 삭제된다.

### Option

- NX : 키가 존재하지 않을 때만 설정, 있으면 무시
- XX : 키가 이미 존재할 때만 설정, 없으면 무시

```redis
SET test-key world GET
```

test-key 에 world 로 SET 하고, 이전에 설정된 값을 GET 해온다.
(값이 없다면 nil)

```
127.0.0.1:6379> SET temp hello GET
(nil)
127.0.0.1:6379> SET temp world GET
"hello"
```

SET 의 옵션들로 인해, `SETNX`, `SETEX`, `PSETEX`, `GETSET` 명령어들을 대체 가능하다.

- `OK` 라는 Simple String 을 반환한다.

### Expiry

- EX : second 지정
- PX : ms 지정

키가 만료되면, 접근 불가능하다. null Bulk String 을 반환한다.