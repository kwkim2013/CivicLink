# CivicLink 백엔드 실행 및 API

## 적용 순서

1. 로컬 Ollama에서 `ollama pull qwen3:4b`를 실행합니다. 임베딩은 기존 `bge-m3`를 사용합니다.
2. IntelliJ Gradle 새로고침 → `war` 빌드 → Tomcat 재시작.
3. 첫 기동 시 기존 `data/decisions/*.json`을 H2로 가져옵니다. JSON은 삭제하지 않고 DB에 없는 번호만 가져옵니다.
4. `requests.http`의 CSRF → 회원가입 → 로그인 요청을 순서대로 실행합니다. 예시 비밀번호는 로컬 테스트용이므로 바꾸세요.
5. RAG 답변, 분류, 민원 등록·조회·수정을 실행합니다.

DB 파일: `D:/CivicLink/backend/data/civiclink.mv.db`. DB 비밀번호는 선택적으로 `database.password`에 설정할 수 있습니다.
기존 메모리 민원은 이전에 재시작하면 사라지던 데이터이므로 자동 이관되지 않습니다.
결정례의 신규 수집·수정은 DB에 저장됩니다. 벡터 인덱스는 기존 파일 방식을 유지합니다.
기존 벡터 인덱스는 텍스트가 같으면 계속 사용하며 데이터 추가·변경 후에는 다시 생성하세요.

## 권한 및 세션

- USER: 본인 민원 CRUD, 결정례 조회·검색, AI 답변·분류.
- ADMIN: 전체 민원 CRUD, 공공 API 호출·수집·인덱스 생성 추가 허용.
- 공개 회원가입은 항상 USER입니다. 요청에 role을 넣어도 승격되지 않습니다.
- 관리자 최초 생성: IntelliJ Tomcat 실행 구성의 환경변수에 `CIVICLINK_ADMIN_USERNAME`과 `CIVICLINK_ADMIN_PASSWORD`를 지정하고 재시작하세요. 아이디는 영문·숫자·_.- 3~50자, 비밀번호는 12~128자입니다. 기존 계정의 비밀번호/권한은 자동 변경하지 않습니다. 일반 회원과 다른 관리자 아이디를 사용하세요.
- 로그인 세션은 30분 비활성 시 만료됩니다. 비밀번호는 무작위 salt와 PBKDF2-HMAC-SHA256으로 저장합니다.
- GET `/api/auth/csrf`로 세션 쿠키와 토큰을 받고, 변경 요청(POST/PUT/DELETE)은 `X-CSRF-Token` 헤더를 보냅니다. 로그인 후 토큰이 갱신됩니다.
- 이전에 인증 없이 쓰던 API도 이제 로그인이 필요합니다. GET 주소창 접근만으로 로그인 상태가 생성되지는 않습니다.

## API 목록

| 메서드 | 주소 | 기능 |
|---|---|---|
| GET | /api/auth/csrf | CSRF 토큰 |
| POST | /api/auth/register | username, password로 가입 |
| POST | /api/auth/login | 로그인, 새 csrfToken 반환 |
| GET | /api/auth/me | 현재 사용자 |
| POST | /api/auth/logout | 로그아웃 |
| POST | /api/ai/answer | content로 RAG 참고 답변 생성 |
| POST | /api/ai/classify | content로 분류 추천 |
| GET/POST | /api/complaints | 민원 목록(최대 100건)/등록 |
| GET/PUT/DELETE | /api/complaints/{id} | 민원 조회/수정/삭제 |
| GET | /api/decisions/{id} | 결정례 조회 |
| GET | /api/decisions | 결정례 목록 요약 |
| POST | /api/decisions/search | query, topK로 검색 |
| POST | /api/decisions/collect | 관리자 일괄 수집 |
| POST | /api/decisions/{id}/collect | 관리자 개별 수집 |
| POST | /api/decisions/index | 관리자 인덱스 재생성 |

AI 요청 예: `{ "content": "횡단보도 경계석이 높아 휠체어로 이동하기 어렵습니다." }`.
분류 결과는 자동 저장하지 않습니다. 사용자가 확인한 category를 민원 등록/수정 API로 전달합니다.
답변의 references는 실제 검색 결과에 포함된 문서번호만 허용합니다. needsReview는 항상 true이며 담당자 참고용입니다.
`rag.min-score` 기본 0.55는 실험용 기준값이지 검증된 정확도/확률이 아닙니다. 관련 사례 부족은 검토필요로 응답합니다.
기존 저장 데이터가 적어 분류 범위가 제한됩니다. 분류 평가와 답변 근거의 사실성 검증은 추가로 필요합니다.

## 프론트 연결

개발 서버에서 `/api`를 Tomcat으로 프록시해 같은 출처로 요청하세요. 임의 CORS 전체 허용은 설정하지 않았습니다.
브라우저 fetch는 `credentials: 'same-origin'`을 사용하고 CSRF 토큰은 메모리로 보관하세요.

```javascript
let csrfToken = (await (await fetch('/api/auth/csrf')).json()).csrfToken;
const response = await fetch('/api/auth/login', {
  method: 'POST', credentials: 'same-origin',
  headers: {'Content-Type': 'application/json', 'X-CSRF-Token': csrfToken},
  body: JSON.stringify({username, password})
});
const data = await response.json();
if (!response.ok) throw new Error(data.message);
csrfToken = data.csrfToken;
```

오류 형식: `{status, message, errorId}`. 400 입력 오류, 401 로그인 필요, 403 CSRF/권한 오류, 409 중복/준비 필요, 503 Ollama 실행 실패.
민원 상세 조회·삭제의 빈 404/204 응답도 처리하세요. 원문에 HTML이 포함될 수 있으니 프론트에서 textContent로 표시하세요.
LLM 답변 생성과 수집은 동기 처리이며 모델 로딩 시 수 분이 걸릴 수 있습니다. 여러 사용자 운영용 큐·페이지네이션·감사로그·비밀번호 변경/복구·배포 보안 강화는 아직 포함하지 않았습니다.
