"use client";

import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";

interface FeedbackStats {
  total: number;
  good: number;
  bad: number;
  satisfactionRate: string;
  badReasons: Record<string, number>;
}

interface FeedbackEntry {
  type: "good" | "bad";
  reason?: string;
  comment?: string;
  messageId: string;
  timestamp: number;
  userAgent: string;
  ip: string;
}

interface FeedbackResponse {
  success: boolean;
  stats: FeedbackStats;
  entries: FeedbackEntry[];
}

export default function FeedbackAdminPage() {
  const [data, setData] = useState<FeedbackResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchData = async () => {
    setLoading(true);
    setError(null);
    
    try {
      const response = await fetch("/api/feedback/logs", {
        headers: {
          "Authorization": "Bearer admin-secret-key"
        }
      });

      if (!response.ok) {
        throw new Error("Failed to fetch data");
      }

      const result = await response.json();
      setData(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unknown error");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const formatDate = (timestamp: number) => {
    // 한국 시간으로 표시
    const date = new Date(timestamp);
    return date.toLocaleString("ko-KR", {
      timeZone: "Asia/Seoul",
      year: "numeric",
      month: "2-digit", 
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
      hour12: false
    });
  };

  const reasonLabels: Record<string, string> = {
    intent_misunderstanding: "의도 오인식",
    insufficient_recommendation: "추천 미흡", 
    inaccurate_result: "결과 부정확",
    technical_error: "기술적 오류",
    other: "기타"
  };

  if (loading) {
    return (
      <div className="container mx-auto p-8">
        <h1 className="text-2xl font-bold mb-6">사용자 피드백 관리</h1>
        <p>로딩 중...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="container mx-auto p-8">
        <h1 className="text-2xl font-bold mb-6">사용자 피드백 관리</h1>
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
          에러: {error}
        </div>
        <Button onClick={fetchData} className="mt-4">
          다시 시도
        </Button>
      </div>
    );
  }

  if (!data) return null;

  return (
    <div className="container mx-auto p-8">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">사용자 피드백 관리 📊</h1>
        <Button onClick={fetchData}>새로고침</Button>
      </div>

      {/* 통계 카드들 */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
        <div className="bg-white p-6 rounded-lg shadow border">
          <h3 className="text-sm font-medium text-gray-500">총 피드백</h3>
          <p className="text-2xl font-bold text-gray-900">{data.stats.total}</p>
        </div>
        <div className="bg-green-50 p-6 rounded-lg shadow border border-green-200">
          <h3 className="text-sm font-medium text-green-600">긍정적 피드백</h3>
          <p className="text-2xl font-bold text-green-700">{data.stats.good}</p>
        </div>
        <div className="bg-red-50 p-6 rounded-lg shadow border border-red-200">
          <h3 className="text-sm font-medium text-red-600">개선 필요</h3>
          <p className="text-2xl font-bold text-red-700">{data.stats.bad}</p>
        </div>
        <div className="bg-blue-50 p-6 rounded-lg shadow border border-blue-200">
          <h3 className="text-sm font-medium text-blue-600">만족도</h3>
          <p className="text-2xl font-bold text-blue-700">{data.stats.satisfactionRate}%</p>
        </div>
      </div>

      {/* 개선 사유 분석 */}
      {Object.keys(data.stats.badReasons).length > 0 && (
        <div className="bg-white p-6 rounded-lg shadow border mb-8">
          <h2 className="text-lg font-semibold mb-4">개선 필요 사유 분석</h2>
          <div className="space-y-2">
            {Object.entries(data.stats.badReasons).map(([reason, count]) => (
              <div key={reason} className="flex justify-between items-center">
                <span>{reasonLabels[reason] || reason}</span>
                <span className="bg-red-100 text-red-800 px-2 py-1 rounded text-sm">
                  {count}건
                </span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* 최근 피드백 목록 */}
      <div className="bg-white rounded-lg shadow border">
        <div className="p-6 border-b">
          <h2 className="text-lg font-semibold">최근 피드백 ({data.entries.length}개)</h2>
        </div>
        <div className="divide-y">
          {data.entries.length === 0 ? (
            <div className="p-6 text-center text-gray-500">
              아직 피드백이 없습니다.
            </div>
          ) : (
            data.entries.map((entry, index) => (
              <div key={index} className="p-6">
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-2">
                      <span className={`px-2 py-1 rounded text-xs font-medium ${
                        entry.type === "good" 
                          ? "bg-green-100 text-green-800" 
                          : "bg-red-100 text-red-800"
                      }`}>
                        {entry.type === "good" ? "👍 도움됨" : "👎 개선 필요"}
                      </span>
                      {entry.reason && (
                        <span className="text-xs text-gray-500">
                          {reasonLabels[entry.reason] || entry.reason}
                        </span>
                      )}
                    </div>
                    {entry.comment && (
                      <p className="text-gray-700 text-sm mb-2">"{entry.comment}"</p>
                    )}
                    <div className="text-xs text-gray-500">
                      메시지 ID: {entry.messageId} | {formatDate(entry.timestamp)}
                    </div>
                  </div>
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
} 