/**
 * 피드백 데이터베이스 연동 헬퍼 (개선된 MySQL 버전)
 * 
 * 지원하는 데이터베이스:
 * - MySQL (최적화된 인덱스 활용)
 * - PostgreSQL
 * - MongoDB
 * - Supabase
 */

// MySQL 연동 (인덱스 최적화)
export async function saveToMySQL(feedbackData: any) {
  try {
    const mysql = require('mysql2/promise'); // npm install mysql2
    
    const connection = await mysql.createConnection({
      host: process.env.MYSQL_HOST,
      user: process.env.MYSQL_USER,
      password: process.env.MYSQL_PASSWORD,
      database: process.env.MYSQL_DATABASE,
      port: process.env.MYSQL_PORT || 3306,
    });

    // 인덱스를 활용한 최적화된 INSERT
    const query = `
      INSERT INTO user_feedback (
        type, message_id, timestamp, timestamp_iso, timestamp_kr,
        reason, comment, user_agent, ip
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    `;

    const values = [
      feedbackData.type,
      feedbackData.messageId,
      feedbackData.timestamp,
      new Date(feedbackData.timestamp), // timestamp_iso
      feedbackData.timestampKR,
      feedbackData.reason || null,
      feedbackData.comment || null,
      feedbackData.userAgent,
      feedbackData.ip,
    ];

    await connection.execute(query, values);
    await connection.end();
    
    console.log('✅ MySQL에 피드백 저장 성공');
  } catch (error) {
    console.error('❌ MySQL 저장 실패:', error);
    throw error;
  }
}

// 관리자 페이지용 통계 조회 (인덱스 최적화)
export async function getFeedbackStats() {
  try {
    const mysql = require('mysql2/promise');
    
    const connection = await mysql.createConnection({
      host: process.env.MYSQL_HOST,
      user: process.env.MYSQL_USER,
      password: process.env.MYSQL_PASSWORD,
      database: process.env.MYSQL_DATABASE,
    });

    // 인덱스 활용한 통계 쿼리 (idx_type 사용)
    const [statsRows] = await connection.execute(`
      SELECT 
        COUNT(*) as total_count,
        SUM(CASE WHEN type = 'good' THEN 1 ELSE 0 END) as good_count,
        SUM(CASE WHEN type = 'bad' THEN 1 ELSE 0 END) as bad_count,
        ROUND(
          SUM(CASE WHEN type = 'good' THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 
          1
        ) as satisfaction_rate
      FROM user_feedback
    `);

    // 개선 사유별 통계 (idx_reason 사용)
    const [reasonRows] = await connection.execute(`
      SELECT reason, COUNT(*) as count
      FROM user_feedback 
      WHERE type = 'bad' AND reason IS NOT NULL
      GROUP BY reason
      ORDER BY count DESC
    `);

    // 최근 피드백 목록 (idx_timestamp 사용)
    const [recentRows] = await connection.execute(`
      SELECT id, type, message_id, timestamp_kr, reason, comment, ip
      FROM user_feedback 
      ORDER BY timestamp_iso DESC 
      LIMIT 50
    `);

    await connection.end();

    return {
      stats: statsRows[0],
      badReasons: reasonRows.reduce((acc: any, row: any) => {
        acc[row.reason] = row.count;
        return acc;
      }, {}),
      entries: recentRows
    };

  } catch (error) {
    console.error('❌ MySQL 통계 조회 실패:', error);
    throw error;
  }
}

// 테이블 초기화 함수 (인덱스 포함)
export async function initializeMySQLTable() {
  try {
    const mysql = require('mysql2/promise');
    
    const connection = await mysql.createConnection({
      host: process.env.MYSQL_HOST,
      user: process.env.MYSQL_USER,
      password: process.env.MYSQL_PASSWORD,
      database: process.env.MYSQL_DATABASE,
    });

    // 테이블 생성 (인덱스 포함)
    await connection.execute(`
      CREATE TABLE IF NOT EXISTS user_feedback (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        type ENUM('good', 'bad') NOT NULL,
        message_id VARCHAR(255) NOT NULL,
        timestamp BIGINT NOT NULL,
        timestamp_iso DATETIME NOT NULL,
        timestamp_kr VARCHAR(50) NOT NULL,
        reason ENUM('intent_misunderstanding', 'insufficient_recommendation', 'inaccurate_result', 'technical_error', 'other') NULL,
        comment TEXT NULL,
        user_agent TEXT NULL,
        ip VARCHAR(45) NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        
        -- 성능 최적화 인덱스
        INDEX idx_type (type),
        INDEX idx_timestamp (timestamp_iso),
        INDEX idx_message_id (message_id),
        INDEX idx_type_timestamp (type, timestamp_iso),
        INDEX idx_reason (reason)
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    `);

    await connection.end();
    console.log('✅ MySQL 테이블 초기화 완료 (인덱스 포함)');
  } catch (error) {
    console.error('❌ MySQL 테이블 초기화 실패:', error);
    throw error;
  }
}

// PostgreSQL/MySQL 예시 (기존 유지)
export async function saveToPostgreSQL(feedbackData: any) {
  try {
    const { Pool } = require('pg'); // npm install pg
    
    const pool = new Pool({
      connectionString: process.env.DATABASE_URL,
    });

    const query = `
      INSERT INTO user_feedback (
        type, message_id, timestamp, reason, comment, user_agent, ip
      ) VALUES ($1, $2, $3, $4, $5, $6, $7)
    `;

    const values = [
      feedbackData.type,
      feedbackData.messageId,
      new Date(feedbackData.timestamp),
      feedbackData.reason || null,
      feedbackData.comment || null,
      feedbackData.userAgent,
      feedbackData.ip,
    ];

    await pool.query(query, values);
    console.log('✅ PostgreSQL에 피드백 저장 성공');
  } catch (error) {
    console.error('❌ PostgreSQL 저장 실패:', error);
    throw error;
  }
}

// MongoDB 예시
export async function saveToMongoDB(feedbackData: any) {
  try {
    const { MongoClient } = require('mongodb'); // npm install mongodb
    
    const client = new MongoClient(process.env.MONGODB_URL!);
    await client.connect();
    
    const db = client.db('agent_chat');
    const collection = db.collection('user_feedback');
    
    await collection.insertOne({
      ...feedbackData,
      createdAt: new Date(),
    });
    
    await client.close();
    console.log('✅ MongoDB에 피드백 저장 성공');
  } catch (error) {
    console.error('❌ MongoDB 저장 실패:', error);
    throw error;
  }
}

// Supabase 예시
export async function saveToSupabase(feedbackData: any) {
  try {
    const { createClient } = require('@supabase/supabase-js'); // npm install @supabase/supabase-js
    
    const supabase = createClient(
      process.env.SUPABASE_URL!,
      process.env.SUPABASE_ANON_KEY!
    );

    const { error } = await supabase
      .from('user_feedback')
      .insert([{
        type: feedbackData.type,
        message_id: feedbackData.messageId,
        timestamp: new Date(feedbackData.timestamp).toISOString(),
        reason: feedbackData.reason || null,
        comment: feedbackData.comment || null,
        user_agent: feedbackData.userAgent,
        ip: feedbackData.ip,
      }]);

    if (error) throw error;
    console.log('✅ Supabase에 피드백 저장 성공');
  } catch (error) {
    console.error('❌ Supabase 저장 실패:', error);
    throw error;
  }
}

// Firebase Firestore 예시
export async function saveToFirestore(feedbackData: any) {
  try {
    const admin = require('firebase-admin'); // npm install firebase-admin
    
    if (!admin.apps.length) {
      admin.initializeApp({
        credential: admin.credential.cert({
          projectId: process.env.FIREBASE_PROJECT_ID,
          clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
          privateKey: process.env.FIREBASE_PRIVATE_KEY?.replace(/\\n/g, '\n'),
        }),
      });
    }

    const db = admin.firestore();
    
    await db.collection('user_feedback').add({
      ...feedbackData,
      timestamp: admin.firestore.Timestamp.fromMillis(feedbackData.timestamp),
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });
    
    console.log('✅ Firestore에 피드백 저장 성공');
  } catch (error) {
    console.error('❌ Firestore 저장 실패:', error);
    throw error;
  }
} 