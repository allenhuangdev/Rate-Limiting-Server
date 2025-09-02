# Rate Limiting Service - Setup Guide

這是一個基於 Spring Boot 的限流服務實作指南，整合了 MySQL、Redis 和 RocketMQ。

## 🚀 快速啟動

### 1. 環境需求

- **Java**: JDK 21+
- **Maven**: 3.6+
- **Docker** 和 **Docker Compose**

### 2. 啟動基礎服務

使用 Docker Compose 啟動必要的服務：

```bash
docker-compose up -d
```

這將啟動以下服務：
- **MySQL**: port 3306 (taskuser/taskpass, database: taskdb)
- **Redis**: port 6379
- **RocketMQ NameServer**: port 9876
- **RocketMQ Broker**: port 10911
- **RocketMQ Console**: port 8088

### 3. 啟動應用程式

```bash
# 使用 Maven
./mvnw spring-boot:run

# 或者先編譯再執行
./mvnw clean package
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

應用程式將在 `http://localhost:8080` 啟動。

## 📋 API 文件

### 健康檢查

**GET** `/health` - 系統健康狀態  
**GET** `/ping` - 簡單存活檢查

### 限流管理 API

#### 1. 建立限流規則
```http
POST /api/v1/limits
Content-Type: application/json

{
  "apiKey": "my-api-key",
  "limit": 100,
  "windowSeconds": 60
}
```

**回應範例**:
```json
{
  "success": true,
  "message": "Rate limit created successfully",
  "data": {
    "id": 1,
    "apiKey": "my-api-key",
    "requestLimit": 100,
    "windowSeconds": 60,
    "createdAt": "2024-01-01T10:00:00",
    "updatedAt": "2024-01-01T10:00:00"
  },
  "timestamp": 1704096000000
}
```

#### 2. 檢查 API 存取
```http
GET /api/v1/check?apiKey=my-api-key
```

**成功回應**:
```json
{
  "success": true,
  "message": "Request allowed",
  "data": {
    "apiKey": "my-api-key",
    "allowed": true,
    "reason": "Request allowed",
    "currentUsage": 5,
    "remainingQuota": 95,
    "windowTtl": 55,
    "totalLimit": 100
  }
}
```

**超出限制回應** (HTTP 429):
```json
{
  "success": false,
  "error": "Rate limit exceeded",
  "timestamp": 1704096000000
}
```

#### 3. 查詢使用量
```http
GET /api/v1/usage?apiKey=my-api-key
```

**回應範例**:
```json
{
  "success": true,
  "message": "Usage retrieved successfully",
  "data": {
    "apiKey": "my-api-key",
    "currentUsage": 25,
    "remainingQuota": 75,
    "windowTtl": 35,
    "totalLimit": 100,
    "windowSeconds": 60
  }
}
```

#### 4. 刪除限流規則
```http
DELETE /api/v1/limits/my-api-key
```

#### 5. 列出所有限流規則
```http
GET /api/v1/limits?page=0&size=20&sort=createdAt,desc
```

#### 6. 取得特定限流規則
```http
GET /api/v1/limits/my-api-key
```

## 🧪 測試範例

### cURL 命令範例

```bash
# 建立限流規則
curl -X POST http://localhost:8080/api/v1/limits \
  -H "Content-Type: application/json" \
  -d '{"apiKey":"test-key-1","limit":10,"windowSeconds":60}'

# 測試 API 存取
curl "http://localhost:8080/api/v1/check?apiKey=test-key-1"

# 查詢使用量
curl "http://localhost:8080/api/v1/usage?apiKey=test-key-1"

# 快速測試多次請求（觸發限流）
for i in {1..15}; do
  curl "http://localhost:8080/api/v1/check?apiKey=test-key-1"
  echo ""
done

# 列出所有限制
curl "http://localhost:8080/api/v1/limits"

# 刪除限制
curl -X DELETE "http://localhost:8080/api/v1/limits/test-key-1"
```

### 使用提供的測試腳本

```bash
# 執行完整的 API 測試
./test-api.sh
```

## 🔧 配置說明

### application.yaml 設定

主要配置項目：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/taskdb
    username: taskuser
    password: taskpass
    
  data:
    redis:
      host: localhost
      port: 6379

rocketmq:
  name-server: localhost:9876
  producer:
    group: rate-limit-producer-group
```

## 📊 監控與觀察

### 1. 應用日誌

應用程式會輸出詳細的日誌，包括：
- 限流規則的建立/更新/刪除
- API 存取檢查結果
- Redis 操作記錄
- RocketMQ 事件發送

### 2. RocketMQ Console

訪問 `http://localhost:8088` 查看 RocketMQ 管理介面，監控訊息佇列狀態。

主題 `rate-limit-events` 包含以下標籤的事件：
- `LIMIT_CREATED`: 限流規則建立
- `LIMIT_UPDATED`: 限流規則更新
- `LIMIT_DELETED`: 限流規則刪除
- `LIMIT_EXCEEDED`: 超出限流

### 3. 資料庫查詢

直接連接 MySQL 查詢限流配置：

```sql
-- 連接資訊
Host: localhost:3306
User: taskuser
Password: taskpass
Database: taskdb

-- 查詢所有限流規則
SELECT * FROM rate_limits;

-- 查詢特定 API Key 的限制
SELECT * FROM rate_limits WHERE api_key = 'your-api-key';
```

### 4. Redis 監控

檢查 Redis 中的計數器：

```bash
# 連接到 Redis
redis-cli

# 查看所有限流相關的 key
KEYS rate_limit:*

# 查看特定 API key 的當前計數
GET rate_limit:your-api-key

# 查看 TTL
TTL rate_limit:your-api-key
```

## 🧪 測試執行

### 單元測試

```bash
# 執行所有測試
./mvnw test

# 執行特定測試類別
./mvnw test -Dtest=RateLimitServiceTest

# 產生測試覆蓋率報告
./mvnw test jacoco:report
```

### 整合測試

```bash
# 執行整合測試（需要 Docker 環境）
./mvnw integration-test
```

## 🔍 疑難排解

### 常見問題

1. **連接 MySQL 失敗**
   - 確認 Docker Compose 服務已啟動：`docker ps`
   - 檢查 `application.yaml` 中的連接配置

2. **Redis 連接錯誤**
   - 確認 Redis 服務運行中：`docker logs redis`
   - 檢查 Redis 連接埠是否被佔用：`lsof -i :6379`

3. **RocketMQ 啟動失敗**
   - 檢查 9876 和 10911 連接埠是否可用
   - 確認 `broker.conf` 配置正確
   - 檢查容器日誌：`docker logs rocketmq-broker`

4. **應用程式無法啟動**
   - 檢查 JDK 版本（需要 21+）：`java -version`
   - 確認 8080 連接埠未被佔用：`lsof -i :8080`
   - 查看應用程式日誌找出具體錯誤

### 日誌等級調整

修改 `application.yaml` 調整日誌等級：

```yaml
logging:
  level:
    com.example.demo: DEBUG  # 應用程式日誌
    org.springframework.web: INFO  # Spring Web 日誌
    org.springframework.data.redis: DEBUG  # Redis 日誌
```

## 📈 效能考量

### 限流演算法
- 使用 **固定視窗計數器** (Fixed Window Counter)
- Redis `INCR` 命令提供原子性操作
- 自動設定過期時間 (TTL)

### 擴展建議
- 多個應用實例可共享 Redis 計數器
- 可考慮實作 **滑動視窗** 或 **令牌桶** 演算法
- 生產環境建議使用 Redis Cluster

## 🛡️ 安全性建議

1. **生產環境配置**
   - 更改預設密碼
   - 啟用 Redis 認證
   - 限制網路存取

2. **API 安全**
   - 實作 API 金鑰驗證
   - 加入 HTTPS 支援
   - 設定適當的 CORS 策略

3. **監控告警**
   - 設定限流事件告警
   - 監控異常流量模式
   - 記錄安全相關事件

## 📝 Postman Collection

導入 `Rate_Limiting_Service.postman_collection.json` 到 Postman 中進行 API 測試。

Collection 包含：
- 所有 API 端點的完整測試
- 自動化測試腳本
- 錯誤情況的測試案例
- 變數配置方便測試

---

**問題回報**: 如遇到任何問題，請檢查上述疑難排解步驟，或查閱應用程式日誌以獲得更多詳細資訊。