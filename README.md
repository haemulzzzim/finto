# ✨ Finto (Finance + Auto)

금융권 직원들의 생산성을 높여주는 AI 에이전트 플랫폼

## 📋 개요

Finto는 금융권 직원들의 반복 업무를 자동화하고 생산성을 향상시키기 위해 개발된 AI 에이전트 플랫폼입니다. 다양한 전문 Agent들이 각각의 업무 영역을 담당하여 효율적인 업무 처리를 지원합니다.

## 🏗️ 프로젝트 구조

```
finto/
├── ai/                    # AI Agent 프론트엔드 (Next.js)
│   ├── src/
│   │   ├── app/          # Next.js 앱 라우터
│   │   ├── components/   # UI 컴포넌트
│   │   └── providers/    # 상태 관리 및 Agent 컨텍스트
│   └── finto/           # Python Agent 백엔드
│       └── src/react_agent/
│           ├── agents/   # 각 Agent별 설정 및 프롬프트
│           ├── graph.py  # LangGraph 워크플로우
│           └── tools.py  # MCP 도구 정의
├── finto-be/             # 백엔드 API (Spring Boot)
└── logs/                 # 로그 파일
```

## 🤖 AI Agent 시스템

### Agent 종류

Finto는 6개의 전문 Agent로 구성되어 있습니다:

#### 1. **기본 대화 Agent** (Agent 0)
- **기능**: 일반적인 대화 및 질문 응답
- **도구**: 없음 (기본 챗봇)
- **설정**: `mcp_config0.json`

#### 2. **일정 관리 Agent** (Agent 1)
- **기능**: 구글 캘린더 일정 등록 및 회의실 예약
- **도구**: 
  - Google Calendar API
  - Playwright (브라우저 자동화)
- **설정**: `mcp_config1.json`
- **워크플로우**:
  1. 일정 정보 수집
  2. 구글 캘린더에 일정 등록
  3. 브라우저로 캘린더 확인
  4. 회의실 예약 페이지 연동

#### 3. **회의자료 준비 Agent** (Agent 2)
- **기능**: 은행 상품 검색 및 워드 문서 작성
- **도구**:
  - Qdrant Vector DB (은행 상품 데이터)
  - Word Document Server
- **설정**: `mcp_config2.json`
- **워크플로우**:
  1. 은행 상품 정보 검색
  2. 상품 비교 분석
  3. 워드 문서 자동 생성

#### 4. **회의보고서 작성 Agent** (Agent 3)
- **기능**: 상품설명서 작성 및 법령 검토
- **도구**:
  - Qdrant Vector DB (법령 데이터)
  - Word Document Server
- **설정**: `mcp_config3.json`
- **워크플로우**:
  1. 상품설명서 초안 작성
  2. 법령과 비교 검토
  3. 오류 부분 취소선 표시
  4. 법적 근거 각주 추가

#### 5. **보고서 전달 Agent** (Agent 4)
- **기능**: Notion 문서 작성 및 관리
- **도구**:
  - Notion API
  - Word Document Server
- **설정**: `mcp_config4.json`

#### 6. **마케팅 전송 Agent** (Agent 5)
- **기능**: Slack 메시지 및 이메일 전송
- **도구**:
  - Slack API
  - Gmail API
  - Word Document Server
- **설정**: `mcp_config5.json`

## 🛠️ 기술 스택

### 프론트엔드
- **Next.js 14**: React 기반 풀스택 프레임워크
- **TypeScript**: 타입 안전성 보장
- **Tailwind CSS**: 스타일링
- **LangGraph**: AI 워크플로우 관리

### 백엔드
- **Python**: AI Agent 백엔드
- **LangGraph**: AI 워크플로우 엔진
- **LangChain**: LLM 통합
- **MCP (Model Context Protocol)**: 도구 연동

### 데이터베이스
- **Qdrant**: 벡터 데이터베이스
- **Redis**: 캐싱 및 세션 관리
- **PostgreSQL**: 관계형 데이터베이스

### 외부 서비스
- **Google Calendar API**: 일정 관리
- **Notion API**: 문서 관리
- **Slack API**: 메시지 전송
- **Gmail API**: 이메일 전송
- **Playwright**: 브라우저 자동화

## 🚀 설치 및 실행

### 1. 저장소 클론
```bash
git clone https://github.com/haemulzzzim/finto.git
cd finto
```

### 2. AI Agent 실행
```bash
cd ai
npm install
npm run dev
```

### 3. Python Agent 백엔드 실행
```bash
cd finto
pip install -r requirements.txt
python -m finto.src.react_agent
```

### 4. 백엔드 API 실행
```bash
cd finto-be
./gradlew bootRun
```

## 📊 성능 최적화

### 회의실 예약 시스템 최적화
- **최적화 전**: 평균 응답 시간 450ms
- **최적화 후**: 평균 응답 시간 120ms (73% 개선)
- **처리량**: 초당 2.2건 → 8.3건 (277% 향상)

### 최적화 방법
1. **역할 분리**: 조회용과 예약 처리용 메서드 분리
2. **Redis 활용**: 빠른 조회를 위한 캐싱
3. **병렬 처리**: `parallelStream`을 통한 동시 처리
4. **데이터 일관성**: Redis-DB 동기화 메커니즘

## 🔧 환경 설정

### 환경 변수
```bash
# LangGraph 서버
NEXT_PUBLIC_API_URL=http://localhost:2024
NEXT_PUBLIC_ASSISTANT_ID=agent

# Qdrant Vector DB
QDRANT_URL=http://100.66.119.25:6333/

# 외부 API 키
SLACK_BOT_TOKEN=your_slack_token
NOTION_API_KEY=your_notion_key
```

## 📝 사용 예시

### 일정 관리 Agent 사용
```
사용자: 내일 오후 2시에 제품 기획 회의 잡아줘
Agent: 제품 기획 회의를 내일 오후 2시에 등록해 드리겠습니다. 
       회의 종료 시간은 언제로 할까요?
사용자: 4시까지
Agent: 네, 추가로 참석자가 있을까요?
...
[자동으로 구글 캘린더에 일정 등록]
[브라우저로 캘린더 확인]
Agent: 회의실도 예약하실건가요?
```

### 회의자료 준비 Agent 사용
```
사용자: 외화예금 상품 비교 분석서 작성해줘
Agent: 외화예금 상품 정보를 검색하여 비교 분석서를 작성하겠습니다.
[Qdrant에서 은행 상품 정보 검색]
[워드 문서 자동 생성]
Agent: 외화예금_비교분석.docx 파일이 생성되었습니다.
```




**Finto** - 금융권 직원들의 생산성을 높여주는 AI 에이전트 ✨
