# Agent Tool-Use History

이 디렉터리는 세션별 tool-use 작업 기록을 저장한다.

권장 규칙:

- 파일명은 대응하는 프롬프트 기록과 같은 날짜/세션 키를 사용한다.
- 읽은 파일, 수정한 파일, 실행한 명령, 테스트 결과를 순서대로 남긴다.
- OAuth 식별자, 토큰, 비밀번호, 세션 값 같은 민감정보는 기록하지 않는다.

권장 템플릿:

```md
# Tool-Use Session

- Date:
- Session:
- Prompt Reference:

## Files Read

- ...

## Files Changed

- ...

## Commands And Tests

- `...`

## Failures Or Retries

- ...

## Outcome

- ...
```
