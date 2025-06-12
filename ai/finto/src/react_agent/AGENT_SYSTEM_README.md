# Agent 시스템 구조

## 개요
이 시스템은 6개의 독립적인 AI Agent를 제공하며, 각 Agent는 고유한 디렉토리, 프롬프트, MCP 설정을 가집니다.

## Agent 목록

### 0. 기본 대화 챗봇 (`default_chat`)
- **설명**: 일반적인 대화 및 질문 답변
- **MCP 도구**: 없음 (기본 대화 모드)
- **아이콘**: 💬 MessageCircle
- **색상**: 회색

### 1. 일정 관리 Agent (`schedule_management`)
- **설명**: 구글 캘린더 일정 등록 및 회의실 예약
- **MCP 도구**: Google Calendar, Playwright
- **아이콘**: 📅 Calendar
- **색상**: 파란색
- **주요 기능**:
  - 구글 캘린더 일정 생성
  - 브라우저 자동화로 회의실 예약 페이지 열기
  - 자동 워크플로우 (일정 등록 → 브라우저 열기 → 회의실 예약 문의)

### 2. 회의자료 준비 Agent (`meeting_materials`)
- **설명**: 법률 문서 검색 및 회의자료 준비
- **MCP 도구**: Legal Qdrant, Word Document Server
- **아이콘**: 📄 FileText
- **색상**: 초록색
- **주요 기능**:
  - 법률 문서 벡터 검색
  - 경제 데이터 분석
  - 금융 상품 설계
  - Word 문서 생성

### 3. 회의보고서 작성 Agent (`meeting_report`)
- **설명**: 법률 문서 기반 회의보고서 작성
- **MCP 도구**: Legal Qdrant, Word Document Server
- **아이콘**: ✏️ Edit3
- **색상**: 보라색

### 4. 보고서 전달 Agent (`report_delivery`)
- **설명**: 회의보고서를 업로드 및 전달
- **MCP 도구**: Notion API, Word Document Server
- **아이콘**: 📤 Send
- **색상**: 주황색

### 5. 마케팅 전송 Agent (`marketing_sender`)
- **설명**: 메일/슬랙 전송
- **MCP 도구**: Slack, Gmail, Word Document Server
- **아이콘**: 📧 Mail
- **색상**: 빨간색

## 디렉토리 구조

```
src/react_agent/agents/
├── default_chat/
│   ├── config.json          # 빈 MCP 설정
│   └── prompts.py           # 기본 대화 프롬프트
├── schedule_management/
│   ├── config.json          # Google Calendar + Playwright
│   └── prompts.py           # 일정 관리 전용 프롬프트
├── meeting_materials/
│   ├── config.json          # Legal Qdrant + Word Document
│   └── prompts.py           # 회의자료 준비 프롬프트
├── meeting_report/
│   ├── config.json          # Legal Qdrant + Word Document
│   └── prompts.py           # 회의보고서 작성 프롬프트
├── report_delivery/
│   ├── config.json          # Notion API + Word Document
│   └── prompts.py           # 보고서 전달 프롬프트
└── marketing_sender/
    ├── config.json          # Slack + Gmail + Word Document
    └── prompts.py           # 마케팅 전송 프롬프트
```

## 시스템 작동 방식

### 1. UI에서 Agent 선택
- 사용자가 UI 토글에서 Agent를 선택 (0-5)
- `AgentSelector` 컴포넌트가 선택된 Agent ID를 관리

### 2. 백엔드로 Agent ID 전달
- `configurable.agent_id`로 선택된 Agent ID가 전달됨
- LangGraph 서버가 이 값을 받아서 해당 Agent 실행

### 3. Agent별 설정 로드
- `load_agent_prompt(agent_id)`: 해당 Agent의 프롬프트 로드
- `load_agent_config(agent_id)`: 해당 Agent의 MCP 설정 로드
- 동적으로 모듈을 import하여 각 Agent의 고유 설정 적용

### 4. MCP 도구 초기화
- 로드된 설정에 따라 필요한 MCP 서버들만 초기화
- 기본 대화 챗봇(Agent 0)의 경우 MCP 도구 없이 실행

## 새로운 Agent 추가 방법

1. **디렉토리 생성**:
   ```bash
   mkdir src/react_agent/agents/new_agent
   ```

2. **설정 파일 생성** (`config.json`):
   ```json
   {
     "mcpServers": {
       "server-name": {
         "command": "node",
         "args": ["path/to/server.js"],
         "transport": "stdio"
       }
     }
   }
   ```

3. **프롬프트 파일 생성** (`prompts.py`):
   ```python
   SYSTEM_PROMPT = """새로운 Agent의 프롬프트
   
   System time: {system_time}"""
   ```

4. **매핑 추가**:
   - `graph.py`의 `AGENT_DIRECTORIES`에 추가
   - `agent-selector.tsx`의 `AGENTS` 배열에 추가
   - `Agent.tsx`의 매핑에 추가

## 주요 특징

- **독립성**: 각 Agent는 완전히 독립적인 설정과 프롬프트를 가짐
- **확장성**: 새로운 Agent를 쉽게 추가할 수 있는 구조
- **동적 로딩**: 선택된 Agent에 따라 필요한 설정만 로드
- **타입 안전성**: TypeScript로 UI 컴포넌트의 타입 안전성 보장
- **실행 가능성**: 모든 파일이 문법적으로 검증됨

## 테스트

시스템이 올바르게 작동하는지 확인하려면:

```bash
cd finto
python -c "
import asyncio
from src.react_agent.graph import load_agent_prompt, load_agent_config
async def test():
    for i in range(6):
        prompt = load_agent_prompt(i)
        config = await load_agent_config(i)
        print(f'Agent {i}: {len(config)} MCP servers')
asyncio.run(test())
"
``` 