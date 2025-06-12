-- =============================================
-- 피드백 시스템 MySQL 최적화 쿼리
-- =============================================

-- 📊 관리자 대시보드 통계 (한 번에 조회)
SELECT 
    COUNT(*) as total_count,
    SUM(CASE WHEN type = 'good' THEN 1 ELSE 0 END) as good_count,
    SUM(CASE WHEN type = 'bad' THEN 1 ELSE 0 END) as bad_count,
    ROUND(
        SUM(CASE WHEN type = 'good' THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 
        1
    ) as satisfaction_rate
FROM user_feedback;

-- 📈 개선 사유별 통계
SELECT 
    reason,
    COUNT(*) as count
FROM user_feedback 
WHERE type = 'bad' AND reason IS NOT NULL
GROUP BY reason
ORDER BY count DESC;

-- 📅 최근 피드백 목록 (페이징)
SELECT 
    id, type, message_id, timestamp_kr, reason, comment, ip
FROM user_feedback 
ORDER BY timestamp_iso DESC 
LIMIT 50 OFFSET 0;

-- 🔍 특정 메시지의 모든 피드백
SELECT 
    type, reason, comment, timestamp_kr, ip, user_agent
FROM user_feedback 
WHERE message_id = ?
ORDER BY timestamp_iso DESC;

-- 📊 일별 피드백 트렌드 (최근 30일)
SELECT 
    DATE(timestamp_iso) as date,
    COUNT(*) as total,
    SUM(CASE WHEN type = 'good' THEN 1 ELSE 0 END) as good,
    SUM(CASE WHEN type = 'bad' THEN 1 ELSE 0 END) as bad
FROM user_feedback 
WHERE timestamp_iso >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY DATE(timestamp_iso)
ORDER BY date DESC;

-- 🔥 성능 최적화를 위한 추가 인덱스 (필요시)
-- CREATE INDEX idx_timestamp_iso_date ON user_feedback (DATE(timestamp_iso));
-- CREATE INDEX idx_created_at ON user_feedback (created_at);

-- =============================================
-- 인덱스 사용 확인 쿼리
-- =============================================

-- 📋 인덱스 상태 확인
SHOW INDEXES FROM user_feedback;

-- 🔍 쿼리 실행 계획 확인 (EXPLAIN)
EXPLAIN SELECT * FROM user_feedback WHERE type = 'bad' ORDER BY timestamp_iso DESC LIMIT 10;

-- 📊 테이블 통계 확인
SHOW TABLE STATUS LIKE 'user_feedback';

-- =============================================
-- 테이블 유지보수
-- =============================================

-- 🗑️ 오래된 데이터 정리 (6개월 이전)
DELETE FROM user_feedback 
WHERE timestamp_iso < DATE_SUB(NOW(), INTERVAL 6 MONTH);

-- 🔧 테이블 최적화
OPTIMIZE TABLE user_feedback; 