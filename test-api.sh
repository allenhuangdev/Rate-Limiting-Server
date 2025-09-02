#!/bin/bash

# Rate Limiting Service API Test Script
# This script demonstrates all API endpoints with curl commands

BASE_URL="http://localhost:8080"
TEST_API_KEY="test-key-demo"

echo "🚀 Rate Limiting Service API Test Script"
echo "========================================"
echo ""

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_step() {
    echo -e "${BLUE}📋 Step $1: $2${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Function to make HTTP request and show response
make_request() {
    local method=$1
    local url=$2
    local data=$3
    local description=$4
    
    echo -e "\n${YELLOW}🔗 $method $url${NC}"
    echo "💬 $description"
    echo ""
    
    if [ -n "$data" ]; then
        curl -X "$method" "$url" \
             -H "Content-Type: application/json" \
             -d "$data" \
             -w "\n📊 HTTP Status: %{http_code} | Time: %{time_total}s\n" \
             -s --show-error
    else
        curl -X "$method" "$url" \
             -w "\n📊 HTTP Status: %{http_code} | Time: %{time_total}s\n" \
             -s --show-error
    fi
    echo ""
    echo "────────────────────────────────────────"
}

# Check if service is running
print_step "0" "Checking if service is running"
if curl -s "$BASE_URL/ping" > /dev/null; then
    print_success "Service is running!"
else
    print_error "Service is not accessible at $BASE_URL"
    print_warning "Please ensure the service is started with: ./mvnw spring-boot:run"
    exit 1
fi

# 1. Health Check
print_step "1" "Health Check"
make_request "GET" "$BASE_URL/health" "" "Check system health status"

# 2. Create Rate Limit
print_step "2" "Create Rate Limit Rule"
make_request "POST" "$BASE_URL/api/v1/limits" '{
  "apiKey": "'$TEST_API_KEY'",
  "limit": 5,
  "windowSeconds": 60
}' "Create a rate limit of 5 requests per 60 seconds"

# 3. Get Specific Rate Limit
print_step "3" "Get Specific Rate Limit"
make_request "GET" "$BASE_URL/api/v1/limits/$TEST_API_KEY" "" "Retrieve the rate limit configuration we just created"

# 4. Check API Access (Multiple times to trigger rate limit)
print_step "4" "Test API Access (Multiple Requests)"
echo "Making 8 requests to trigger rate limiting..."
echo ""

for i in {1..8}; do
    echo -e "${YELLOW}Request #$i:${NC}"
    response=$(curl -s "$BASE_URL/api/v1/check?apiKey=$TEST_API_KEY" -w "|%{http_code}")
    http_code="${response##*|}"
    json_response="${response%|*}"
    
    if [ "$http_code" -eq 200 ]; then
        usage=$(echo "$json_response" | grep -o '"currentUsage":[0-9]*' | cut -d':' -f2)
        remaining=$(echo "$json_response" | grep -o '"remainingQuota":[0-9]*' | cut -d':' -f2)
        print_success "ALLOWED - Usage: $usage, Remaining: $remaining"
    elif [ "$http_code" -eq 429 ]; then
        print_warning "RATE LIMITED - Too many requests"
    else
        print_error "UNEXPECTED RESPONSE - HTTP $http_code"
    fi
    
    sleep 0.5
done

# 5. Check Usage Information
print_step "5" "Check Usage Information"
make_request "GET" "$BASE_URL/api/v1/usage?apiKey=$TEST_API_KEY" "" "Get current usage statistics"

# 6. List All Rate Limits
print_step "6" "List All Rate Limits"
make_request "GET" "$BASE_URL/api/v1/limits?page=0&size=10&sort=createdAt,desc" "" "Get paginated list of all rate limits"

# 7. Test Unknown API Key
print_step "7" "Test Unknown API Key"
make_request "GET" "$BASE_URL/api/v1/check?apiKey=unknown-key-12345" "" "Test with unknown API key (should be blocked)"

# 8. Test Invalid Request Data
print_step "8" "Test Validation (Invalid Data)"
make_request "POST" "$BASE_URL/api/v1/limits" '{
  "apiKey": "",
  "limit": -1,
  "windowSeconds": 0
}' "Test with invalid data (should return validation errors)"

# 9. Update Rate Limit
print_step "9" "Update Rate Limit Rule"
make_request "POST" "$BASE_URL/api/v1/limits" '{
  "apiKey": "'$TEST_API_KEY'",
  "limit": 10,
  "windowSeconds": 30
}' "Update the rate limit to 10 requests per 30 seconds"

# 10. Wait and Test After Update
print_step "10" "Test After Rate Limit Update"
echo "Waiting 5 seconds for window to potentially reset..."
sleep 5
make_request "GET" "$BASE_URL/api/v1/check?apiKey=$TEST_API_KEY" "" "Test API access after updating rate limit"

# 11. Clean Up - Delete Rate Limit
print_step "11" "Clean Up - Delete Rate Limit"
make_request "DELETE" "$BASE_URL/api/v1/limits/$TEST_API_KEY" "" "Delete the test rate limit rule"

# 12. Verify Deletion
print_step "12" "Verify Deletion"
make_request "GET" "$BASE_URL/api/v1/limits/$TEST_API_KEY" "" "Verify the rate limit was deleted (should return 404)"

echo ""
echo "🎉 API Test Complete!"
echo "========================================"
echo ""
echo "💡 Key Observations:"
echo "• Rate limiting activates after exceeding the configured limit"
echo "• HTTP 429 (Too Many Requests) is returned when blocked"
echo "• Usage counters reset after the time window expires"
echo "• Unknown API keys are rejected with descriptive messages"
echo "• Validation prevents invalid configurations"
echo ""
echo "📊 To monitor the system:"
echo "• Check MySQL: mysql -h localhost -u taskuser -p taskdb"
echo "• Check Redis: redis-cli -> KEYS rate_limit:*"
echo "• Check RocketMQ: http://localhost:8088"
echo ""
echo "🔄 To run this test again: bash test-api.sh"