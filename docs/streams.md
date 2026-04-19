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

