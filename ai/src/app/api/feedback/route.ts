import { NextRequest, NextResponse } from "next/server";
import fs from "fs/promises";
import path from "path";

export interface FeedbackData {
  type: "good" | "bad";
  reason?: string;
  comment?: string;
  messageId: string;
  timestamp: number;
}

// 한국 시간(KST) 생성 함수
function getKoreanTime() {
  const now = new Date();
  // UTC 시간에 9시간 추가 (한국은 UTC+9)
  const koreaTime = new Date(now.getTime() + (9 * 60 * 60 * 1000));
  return {
    timestamp: koreaTime.getTime(),
    isoString: koreaTime.toISOString(),
    koreanString: koreaTime.toLocaleString('ko-KR', {
      timeZone: 'Asia/Seoul',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false
    })
  };
}

// 클라이언트 IP 추출 함수 (개선된 버전)
function getClientIP(request: NextRequest): string {
  // 다양한 헤더에서 IP 추출 시도
  const possibleHeaders = [
    'x-forwarded-for',
    'x-real-ip',
    'x-client-ip',
    'x-forwarded',
    'x-cluster-client-ip',
    'forwarded-for',
    'forwarded',
    'cf-connecting-ip', // Cloudflare
    'true-client-ip',   // Cloudflare Enterprise
    'x-original-forwarded-for'
  ];

  for (const header of possibleHeaders) {
    const value = request.headers.get(header);
    if (value) {
      // x-forwarded-for는 쉼표로 구분된 IP 목록일 수 있음
      const ip = value.split(',')[0].trim();
      if (ip && ip !== 'unknown') {
        return ip;
      }
    }
  }

  // 개발 환경에서는 localhost 표시
  return process.env.NODE_ENV === 'development' ? 'localhost' : 'unknown';
}

// 백엔드 전송 함수
async function sendToBackend(feedbackData: any) {
  try {
    const BACKEND_URL = process.env.FEEDBACK_BACKEND_URL;
    const BACKEND_API_KEY = process.env.FEEDBACK_BACKEND_API_KEY;
    
    if (!BACKEND_URL) {
      console.log("🔄 백엔드 URL이 설정되지 않음 - 로컬 저장만 수행");
      return;
    }

    const response = await fetch(`${BACKEND_URL}/api/feedback`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...(BACKEND_API_KEY && { "Authorization": `Bearer ${BACKEND_API_KEY}` }),
      },
      body: JSON.stringify(feedbackData),
    });

    if (response.ok) {
      console.log("✅ 백엔드로 피드백 전송 성공");
    } else {
      console.error("❌ 백엔드 전송 실패:", response.status, response.statusText);
    }
  } catch (error) {
    console.error("❌ 백엔드 전송 중 오류:", error);
  }
}

export async function POST(request: NextRequest) {
  try {
    const feedbackData: FeedbackData = await request.json();

    // 유효성 검사
    if (!feedbackData.messageId || !feedbackData.type) {
      return NextResponse.json(
        { error: "Missing required fields: messageId, type" },
        { status: 400 }
      );
    }

    if (feedbackData.type === "bad" && !feedbackData.reason) {
      return NextResponse.json(
        { error: "Reason is required for bad feedback" },
        { status: 400 }
      );
    }

    // 한국 시간 생성
    const koreaTime = getKoreanTime();
    
    // 클라이언트 IP 추출
    const clientIP = getClientIP(request);

    // 로그 데이터 준비
    const logEntry = {
      ...feedbackData,
      timestamp: koreaTime.timestamp,
      timestampISO: koreaTime.isoString,
      timestampKR: koreaTime.koreanString,
      userAgent: request.headers.get("user-agent") || "unknown",
      ip: clientIP,
    };

    // 🚀 백엔드로 실시간 전송 (비동기, 실패해도 로컬 저장은 계속)
    sendToBackend(logEntry).catch(console.error);

    // 📁 로컬 파일 저장 (기존 로직)
    const logDir = path.join(process.cwd(), "logs");
    const logFile = path.join(logDir, "user-feedback.jsonl");

    // 로그 디렉토리가 없으면 생성
    try {
      await fs.access(logDir);
    } catch {
      await fs.mkdir(logDir, { recursive: true });
    }

    // JSONL 형태로 파일에 추가 (각 라인이 하나의 JSON 객체)
    const logLine = JSON.stringify(logEntry) + "\n";
    
    try {
      await fs.appendFile(logFile, logLine, "utf8");
    } catch (error) {
      console.error("Failed to write feedback log:", error);
      return NextResponse.json(
        { error: "Failed to save feedback" },
        { status: 500 }
      );
    }

    // 콘솔에도 로그 출력 (개발 환경에서 확인용)
    console.log("💬 User Feedback Received:", {
      type: feedbackData.type,
      messageId: feedbackData.messageId,
      reason: feedbackData.reason,
      hasComment: !!feedbackData.comment,
      timestamp: koreaTime.koreanString,
      ip: clientIP,
    });

    return NextResponse.json({ 
      success: true, 
      message: "Feedback saved successfully" 
    });

  } catch (error) {
    console.error("Error processing feedback:", error);
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 }
    );
  }
} 