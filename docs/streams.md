## Redis Stream

Redis 5.0 부터 추가 된 자료구조

- append only 형태

Kafka 와 비슷하게 동작한다. (메시지 읽는 Consumer, 여러 Consumer 관리하는 Consumer Group)

- 메시지 전달 후 사라지지 않는다. - `fire & forget` X
- 여러 레벨 Control 제공

![image](https://d1apvpgu6ekv4q.cloudfront.net/bd8753d713984cc9f14177f333ba82ca.png)

### 동작

- `XADD` : Stream 에 데이터 추가

`XADD <stream-key> <message-id> <field> <name> ... <field> <name>`

- 메시지 id 는 `*` 입력시 자동 생성 (`<ms>-<sequenceNumber>`)

-> 시계열 데이터 저장소로 활용 가능

- `XRANGE` : start-id, end-id 를 통해 조회
  - ASC 순 조회
  - COUNT 옵션으로 개수 제한 두고 조회 가능

`XRANGE <stream-key> <start-id> <end-id>`

- `XREAD` : Range 기반 조회접근이 아닌, 구독 형식 조회

1. 데이터를 기다리는 여러 consumer, 새로운 데이터를 기다리는 모든 consumer 에 전달
2. 메시지가 Append 되는 형태
  - Pub/Sub 은 fire & forget 로 메시지 발행 후 저장되지 않는 형태
  - Blocking List 는 client 가 메시지 받으면 POP 되는 형태

-> 실시간 구독으로, 마지막 받은 ID 이후 새 메시지 listen

- `XDEL` : 특정 메시지를 삭제

> 여러모로, Kafka 의 Consumer Group 과 동일하게 돈다. `at-least-once`

### Entry ID

entry 들의 순서를 보장하는 요소 (단순, 식별자 값이 아님)

`<milliseconds>-<sequenceNumber>`

- 같은 stream 내에서 유일해야 함
- 항상 증가해야 한다
  - 새 entry 의 ID 는 이전 entry 보다 반드시 커야 함

3가지 방법으로 ID 지정이 가능하다.

- 명시적 : 시간, 시퀸스 모두 직접 지정 - `1526919030474-0`
- 시퀸스 자동 : 시간만 지정, 시퀀스 번호 자동 생성 - `1526919030474-*`
- 전체 자동 : 시간, 시퀸스 자동 생성 - `*`

검증 규칙

- 새 ID 는 마지막 Entry ID 보다 커야만 하다 (새 ID ms >= 마지막 ID ms)
- ms 가 같다면, 새 ID 의 sequenceNumber > 마지막 ID 의 sequenceNumber
- stream 이 빈 상태면, ID 는 `0-0` 보다 커야한다.(최소 `0-1`)




