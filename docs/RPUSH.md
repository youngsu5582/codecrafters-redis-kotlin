### RPUSH

- O(1) - N 개 요소 넣으면 O(N)
- 리스트 오른쪽(tail) 에 삽입
  - key 미존재 시, 자동 생성 후 삽입
  - 여러개의 요소 삽입시, 인자 순서(왼쪽->오른쪽) tail 추가

```
RPUSH mylist a b c
-> mylist: [a, b, c]
```
