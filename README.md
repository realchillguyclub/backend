## ✅ ILLDAN Backend

## 📄 API Documentation

[📝 REST Docs + Swagger]()

## 🔒 Rules

### Issue & PR

- `Code Review & Approve` 룰 설정
    - 상대방이 `Approve` 올린 사람이 `Merge`

### Branch

- 이슈 기반 브랜치 생성
    - `ex) feature/#4/login`
- `main branch`
    - 현재 서비스 중인 브랜치
    - release 브랜치에서 검수가 완료된 소스 코드 병합
    - PR이 병합된 경우 prod-cicd.yml 스크립트 실행
- `release branch`
    - 검수 중인 브랜치로 Staging 환경 역할
    - `release/v*` 형식의 브랜치 이름 사용
    - 검수 완료 후 main 브랜치로 병합, 이후 브랜치 제거
- `develop branch`
    - 개발 중인 기능 병합 브랜치
    - 배포 단위 기능 모두 병합 시 release 브랜치로 분기
    - PR 병합 또는 코드 푸시 시 test-cicd.yml 스크립트 실행
- `hotfix 브랜치`
    - 배포 후 긴급 수정 시 사용

### **Commit Message**

- 이슈 번호 붙여서 커밋 `Ex) #4 [feat] : 로그인 기능을 추가한다`
- Body는 추가 설명 필요하면 사용

| ***작업태그*** | ***내용*** |
| --- | --- |
| **feat** | 새로운 기능 추가 / 일부 코드 추가 / 일부 코드 수정 (리팩토링과 구분) / 디자인 요소 수정 |
| **fix** | 버그 수정 |
| **refactor** | 코드 리팩토링 |
| **style** | 코드 의미에 영향을 주지 않는 변경사항 (코드 포맷팅, 오타 수정, 변수명 변경, 에셋 추가) |
| **chore** | 빌드 부분 혹은 패키지 매니저 수정 사항 / 파일 이름 변경 및 위치 변경 / 파일 삭제 |
| **docs** | 문서 추가 및 수정 |
| **rename** | 패키지 혹은 폴더명, 클래스명 수정 (단독으로 시행하였을 시) |
| **remove** | 패키지 혹은 폴더, 클래스를 삭제하였을 때 (단독으로 시행하였을 시) |

### Naming

- **패키지명** : 한 단어 소문자 사용 `Ex) service`
- **클래스명** : 파스칼 케이스 사용 `Ex) JwtUtil`
- **메서드명** : 카멜 케이스 사용, 동사로 시작  `Ex) getUserScraps`
- **변수명** : 카멜 케이스 사용 `Ex) jwtToken`
- **상수명** : 대문자 사용 `Ex) EXPIRATION_TIME`
- **컬럼명** : 스네이크 케이스 사용 `Ex) user_id`


### API Response

```json
{
  "code": "GLOBAL-200",
  "message": "요청 응답에 성공했습니다.",
  "result": {
    "todoId": 1
  },
  "isSuccess": true
}
```

- `code` : 성공 코드 (커스텀)
- `message` : 성공 메세지 (커스텀)
- `result` : 데이터가 들어가는 곳
- `isSuccess` : 성공 여부
