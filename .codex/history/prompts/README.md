# Agent Prompt History

이 디렉터리는 세션별 사용자 프롬프트 기록을 저장한다.

권장 규칙:

- 파일명은 `YYYY-MM-DD.md` 형식을 기본으로 사용한다.
- 하루에 여러 세션이 있으면 `YYYY-MM-DD-01.md`처럼 suffix를 붙인다.
- 원문 프롬프트와 핵심 결정 사항만 남기고 민감정보는 적지 않는다.

권장 템플릿:

```md
# Prompt Session

- Date:
- Session:
- Goal:

## User Prompts

1. ...

## Confirmed Decisions

- ...

## Notes

- ...
```
