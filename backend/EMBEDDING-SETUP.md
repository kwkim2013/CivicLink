# 로컬 임베딩·유사 결정례 검색

## 준비

1. https://ollama.com/download 에서 Windows용 Ollama를 설치하고 실행합니다.
2. 터미널에서 `ollama pull bge-m3`를 실행합니다. 모델 다운로드에는 저장 공간과 시간이 필요합니다.
3. IntelliJ에서 Gradle `war` 작업을 실행하고 Tomcat을 다시 시작합니다.
4. `requests.http`에서 `임베딩 인덱스 생성` POST를 실행합니다.
5. 완료 후 `유사 결정례 검색` POST를 실행하고 `query`를 원하는 민원 문장으로 바꿉니다.

## 동작

- 저장된 결정례의 제목과 신청취지만 임베딩합니다. 신청취지가 없는 문서는 건너뛰고 번호를 반환합니다.
- 인덱스는 `D:/CivicLink/backend/data/search-index.json`에 저장합니다.
- `POST /api/decisions/index`는 전체 인덱스를 재생성합니다. 데이터 수집·수정 후 다시 실행하세요.
- `POST /api/decisions/search`는 `{ "query": "민원 내용", "topK": 3 }`를 받습니다.
- 응답의 `score`는 코사인 유사도이며 정확도나 확률이 아닙니다. 관련 문서가 없어도 상위 사례는 반환됩니다.
- 결정 내용, 분류, 각주는 검색된 결정례에 함께 반환됩니다. LLM 답변 생성은 아직 연결하지 않았습니다.
- 소규모 MVP용 파일 인덱스이며 전용 벡터 DB는 사용하지 않습니다.

## 선택 설정

`src/main/resources/application.properties`에 필요한 경우 추가합니다.

```properties
embedding.base-url=http://localhost:11434
embedding.model=bge-m3
embedding.index-file=D:/CivicLink/backend/data/search-index.json
```

모델을 바꾸면 인덱스를 다시 생성하세요. 모델 태그는 동일한 버전으로 유지하세요.
첫 모델 로딩과 인덱스 생성에는 시간이 걸릴 수 있습니다.
연결 또는 모델 오류는 HTTP 503으로 반환합니다.

공식 API: https://docs.ollama.com/api/embed
모델: https://ollama.com/library/bge-m3
