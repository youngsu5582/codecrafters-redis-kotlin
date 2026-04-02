## GET

- O(1)

key 에 해당하는 value 를 가져온다.

- key 에 해당하는 값이 없으면 `(nil)` 을 반환한다
- 키가 존재하지만, 문자열이 아니면 에러를 반환한다 (`(error)WRONGTYPE`)