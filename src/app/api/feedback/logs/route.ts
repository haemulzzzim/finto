import { NextRequest, NextResponse } from "next/server";
import fs from "fs/promises";
import path from "path";

export async function GET(request: NextRequest) {
  try {
    // 간단한 인증 체크 (실제 환경에서는 더 강력한 인증 필요)
    const authHeader = request.headers.get("authorization");
    if (authHeader !== "Bearer admin-secret-key") {
      return NextResponse.json(
        { error: "Unauthorized" },
        { status: 401 }
      );
    }

    const logFile = path.join(process.cwd(), "logs", "user-feedback.jsonl");
    
    try {
      const logData = await fs.readFile(logFile, "utf8");
      const feedbackEntries = logData
        .trim()
        .split("\n")
        .filter(line => line.trim())
        .map(line => {
          try {
            return JSON.parse(line);
          } catch {
            return null;
          }
        })
        .filter(Boolean)
        .reverse(); // 최신 항목부터 표시

      // 통계 정보 계산
      const totalCount = feedbackEntries.length;
      const goodCount = feedbackEntries.filter(f => f.type === "good").length;
      const badCount = feedbackEntries.filter(f => f.type === "bad").length;
      
      const badReasons = feedbackEntries
        .filter(f => f.type === "bad" && f.reason)
        .reduce((acc, f) => {
          acc[f.reason] = (acc[f.reason] || 0) + 1;
          return acc;
        }, {});

      return NextResponse.json({
        success: true,
        stats: {
          total: totalCount,
          good: goodCount,
          bad: badCount,
          satisfactionRate: totalCount > 0 ? ((goodCount / totalCount) * 100).toFixed(1) : 0,
          badReasons
        },
        entries: feedbackEntries.slice(0, 50) // 최근 50개만 반환
      });

    } catch (error) {
      // 파일이 없는 경우
      return NextResponse.json({
        success: true,
        stats: {
          total: 0,
          good: 0,
          bad: 0,
          satisfactionRate: 0,
          badReasons: {}
        },
        entries: []
      });
    }

  } catch (error) {
    console.error("Error fetching feedback logs:", error);
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 }
    );
  }
} 