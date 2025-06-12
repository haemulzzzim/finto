# 📊 사용자 피드백 시스템

Agent Chat UI에 통합된 사용자 피드백 수집 시스템입니다.

## ✨ 주요 기능

### 1. 사용자 피드백 수집
- **Good/Bad 버튼**: AI 답변 옆에 👍/👎 버튼 표시
- **즉시 피드백**: 👍 클릭 시 즉시 긍정 피드백 기록
- **상세 피드백**: 👎 클릭 시 개선 사유 선택 및 의견 작성 가능

### 2. 개선 사유 분류
- **의도 오인식**: AI가 사용자의 의도를 잘못 이해한 경우
- **추천 미흡**: 제안이나 추천이 부족한 경우  
- **결과 부정확**: 답변이나 결과가 정확하지 않은 경우
- **기술적 오류**: 시스템 오류나 기술적 문제
- **기타**: 위에 해당하지 않는 기타 사유

### 3. 데이터 저장 및 분석
- **JSONL 형식**: `logs/user-feedback.jsonl` 파일에 저장
- **메타데이터**: IP, User-Agent, 타임스탬프 포함
- **실시간 통계**: 만족도, 개선 사유별 통계 제공

## 🖥️ 사용자 인터페이스

### AI 답변에 표시되는 피드백 버튼
```
[답변 내용]
📋 복사  🔄 새로고침  👍 👎  <-- 피드백 버튼들
```

### Bad 피드백 선택 시 모달
```
어떤 부분이 개선되면 좋을까요? 🤔

개선이 필요한 이유를 선택해주세요:
[드롭다운 메뉴: 의도 오인식 / 추천 미흡 / 결과 부정확 / 기술적 오류 / 기타]

추가 의견 (선택사항):
[텍스트 영역: 구체적인 개선사항이나 의견을 적어주세요...]

[취소] [피드백 제출]
```

### 피드백 완료 후 상태 표시
```
👍 도움됨  ❌  <-- Good 피드백 후
👎 개선 필요  ❌  <-- Bad 피드백 후
```

## 🔧 API 엔드포인트

### 피드백 제출
```http
POST /api/feedback
Content-Type: application/json

{
  "type": "good" | "bad",
  "messageId": "message-uuid",
  "timestamp": 1701234567890,
  "reason": "intent_misunderstanding", // bad 피드백의 경우만
  "comment": "구체적인 의견..." // 선택사항
}
```

### 피드백 로그 조회 (관리자용)
```http
GET /api/feedback/logs
Authorization: Bearer admin-secret-key

응답:
{
  "success": true,
  "stats": {
    "total": 10,
    "good": 7,
    "bad": 3,
    "satisfactionRate": "70.0",
    "badReasons": {
      "intent_misunderstanding": 2,
      "inaccurate_result": 1
    }
  },
  "entries": [...] // 최근 50개 피드백
}
```

## 📈 관리자 페이지

### 접속 방법
```
http://localhost:3000/admin/feedback
```

### 제공 정보
- **통계 대시보드**: 총 피드백, 긍정/부정 비율, 만족도
- **개선 사유 분석**: 부정 피드백의 사유별 분포
- **최근 피드백 목록**: 타임스탬프, 메시지 ID, 의견 포함

## 📁 파일 구조

```
src/
├── components/thread/messages/
│   ├── feedback.tsx          # 피드백 UI 컴포넌트
│   ├── shared.tsx           # CommandBar에 피드백 버튼 통합
│   └── ai.tsx              # AI 메시지에 messageId 전달
├── app/api/feedback/
│   ├── route.ts            # 피드백 제출 API
│   └── logs/route.ts       # 피드백 조회 API (관리자용)
├── app/admin/feedback/
│   └── page.tsx            # 관리자 대시보드
└── components/ui/
    ├── dialog.tsx          # 모달 컴포넌트
    └── select.tsx          # 드롭다운 컴포넌트

logs/
└── user-feedback.jsonl     # 피드백 데이터 저장 파일
```

## 🔒 보안 고려사항

### 현재 구현
- **IP 추적**: 기본적인 사용자 식별
- **간단한 인증**: 관리자 페이지용 베어러 토큰

### 권장 개선사항
- **사용자 세션 관리**: 보다 정교한 사용자 추적
- **강화된 인증**: JWT, OAuth 등 도입
- **데이터 암호화**: 민감한 피드백 데이터 암호화
- **Rate Limiting**: API 남용 방지

## 📊 데이터 분석 활용

### 로그 파일 예시
```jsonl
{"type":"good","messageId":"msg-123","timestamp":1701234567890,"userAgent":"Mozilla/5.0...","ip":"192.168.1.1"}
{"type":"bad","reason":"intent_misunderstanding","comment":"질문의 의도를 잘못 이해한 것 같아요","messageId":"msg-124","timestamp":1701234568000,"userAgent":"Mozilla/5.0...","ip":"192.168.1.2"}
```

### 분석 가능한 지표
- **만족도 트렌드**: 시간별 만족도 변화
- **문제 패턴**: 자주 발생하는 개선 사유
- **사용자 행동**: 피드백 제공 패턴
- **성능 지표**: 답변 품질 개선 효과

## 🚀 확장 가능성

### 추가 기능 아이디어
- **별점 평가**: 5점 척도 평가 시스템
- **카테고리별 분석**: 도구별, 주제별 만족도
- **A/B 테스트**: 다른 답변 방식의 만족도 비교
- **실시간 알림**: 부정 피드백 즉시 알림
- **자동 개선**: 피드백 기반 프롬프트 자동 조정

---

이 피드백 시스템을 통해 사용자 만족도를 지속적으로 모니터링하고 AI 답변 품질을 개선할 수 있습니다! 🎯 