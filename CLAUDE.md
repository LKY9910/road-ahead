@AGENTS.md

## Claude 전용

- 이 저장소에서 Claude의 역할은 설계·문서·작업 분배·리뷰다. 구현은 원칙적으로 Codex에게 `docs/TASKS.md`로 맡긴다.
- 사용자가 직접 요청하면 작은 수정이나 긴급 수정은 Claude가 해도 된다. 그때도 TASKS와 HANDOFF에 남긴다.
- Codex 작업을 리뷰할 때는 코드뿐 아니라 HANDOFF의 검증 내용을 실제로 다시 확인한다. (빌드, 테스트 실행)
