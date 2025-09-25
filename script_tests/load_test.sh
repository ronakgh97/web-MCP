#!/bin/bash

set -euo pipefail

# Colors
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m'

# Default configuration
URL="http://localhost:3000"
CONCURRENT_REQUESTS=10
RESULTS=3
TIMEOUT=30
VERBOSE=false

readonly QUERY_POOL=(
  "spring boot microservices"
  "springAI documentation"
  "what is mcp server protocol?"
  "playwright automation"
  "ai-agents framework"
  "java spring security"
  "docker containerization"
  "kubernetes orchestration"
  "web scraping best practices"
  "RESTful API design"
)

# Function to print colored output
print_color() {
    local color=$1
    shift
    echo -e "${color}$*${NC}"
}

# Function to show usage
usage() {
    cat << EOF
Usage: $0 [OPTIONS]
Load testing script for Web Scraper API

OPTIONS:
    -n NUM      Number of concurrent requests (default: 10)
    -u URL      API endpoint URL (default: localhost:3000)
    -r NUM      Results per query (default: 3)
    -t SEC      Timeout in seconds (default: 30)
    -v          Verbose output
    -h          Show this help message

EXAMPLES:
    $0 -n 20 -v                    # 20 concurrent requests with verbose output
    $0 -n 50 -u http://prod-server:8080   # 50 requests to production server
    $0 -n 5 -r 1 -t 10             # Light test with 5 requests, 1 result each

EOF
    exit 1
}

# Parse CLI flags
while getopts "n:u:r:t:vh" opt; do
    case $opt in
        n) CONCURRENT_REQUESTS="$OPTARG" ;;
        u) URL="$OPTARG" ;;
        r) RESULTS="$OPTARG" ;;
        t) TIMEOUT="$OPTARG" ;;
        v) VERBOSE=true ;;
        h) usage ;;
        *) print_color "$RED" "Invalid option. Use -h for help." >&2; exit 1 ;;
    esac
done

# Validate inputs
if ! [[ "$CONCURRENT_REQUESTS" =~ ^[0-9]+$ ]] || [ "$CONCURRENT_REQUESTS" -lt 1 ]; then
    print_color "$RED" "Error: Number of requests must be a positive integer"
    exit 1
fi

if ! [[ "$RESULTS" =~ ^[0-9]+$ ]] || [ "$RESULTS" -lt 1 ]; then
    print_color "$RED" "Error: Results must be a positive integer"
    exit 1
fi

# Create results directory
RESULTS_DIR="load_test_results_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$RESULTS_DIR"

print_color "$BLUE" "Load Testing"
print_color "$BLUE" "=============================="
print_color "$YELLOW" "Target URL: $URL"
print_color "$YELLOW" "Concurrent Requests: $CONCURRENT_REQUESTS"
print_color "$YELLOW" "Results per Query: $RESULTS"
print_color "$YELLOW" "Timeout: ${TIMEOUT}s"
print_color "$YELLOW" "Results Directory: $RESULTS_DIR"
echo

# Pre-flight health check
print_color "$BLUE" "🔍 Pre-flight health check..."
if curl -s -m 5 "${URL%/search}/health" > /dev/null 2>&1; then
    print_color "$GREEN" "Service is healthy and reachable"
else
    print_color "$RED" "Service health check failed"
    print_color "$YELLOW" "Continuing anyway..."
fi

# Start timing
start_time=$(date +%s)

print_color "$BLUE" "Starting $CONCURRENT_REQUESTS concurrent requests..."
echo "--------------------------------------------------"

# Create request function
make_request() {
    local request_id=$1
    local query_index=$((RANDOM % ${#QUERY_POOL[@]}))
    local selected_query="${QUERY_POOL[$query_index]}"

    local json_payload=$(cat <<EOF
{
  "query": "$selected_query",
  "results": $RESULTS
}
EOF
)

    local output_file="$RESULTS_DIR/request_${request_id}.json"
    local timing_file="$RESULTS_DIR/request_${request_id}_timing.txt"

    if [ "$VERBOSE" = true ]; then
        print_color "$YELLOW" "Request #$request_id: '$selected_query'"
    fi

    # Capture timing and response
    local start_req=$(date +%s.%N)
    local http_code=$(curl -w "%{http_code}" -s -m "$TIMEOUT" \
        -X POST \
        -H "Content-Type: application/json" \
        -d "$json_payload" \
        "$URL/api/v1/service/search" \
        -o "$output_file" 2>/dev/null || echo "000")
    local end_req=$(date +%s.%N)

    local duration=$(echo "$end_req - $start_req" | bc -l)

    # Log timing information
    echo "request_id=$request_id,query='$selected_query',http_code=$http_code,duration=${duration}s" > "$timing_file"

    if [ "$http_code" = "200" ]; then
        if [ "$VERBOSE" = true ]; then
            print_color "$GREEN" "Request #$request_id completed (${duration}s)"
        fi
        echo "SUCCESS" > "$RESULTS_DIR/request_${request_id}_status.txt"
    else
        if [ "$VERBOSE" = true ]; then
            print_color "$RED" "Request #$request_id failed (HTTP: $http_code, ${duration}s)"
        fi
        echo "FAILED:$http_code" > "$RESULTS_DIR/request_${request_id}_status.txt"
    fi
}

# Launch concurrent requests
for i in $(seq 1 "$CONCURRENT_REQUESTS"); do
    make_request "$i" &
done

# Wait for all background jobs to finish
wait

# Stop timing
end_time=$(date +%s)
total_duration=$((end_time - start_time))

echo
print_color "$BLUE" "📊 Load Test Results"
print_color "$BLUE" "==================="

# Analyze results
successful_requests=0
failed_requests=0
total_response_time=0

for i in $(seq 1 "$CONCURRENT_REQUESTS"); do
    if [ -f "$RESULTS_DIR/request_${i}_status.txt" ]; then
        status=$(cat "$RESULTS_DIR/request_${i}_status.txt")
        if [[ "$status" == "SUCCESS" ]]; then
            ((successful_requests++))
        else
            ((failed_requests++))
        fi

        if [ -f "$RESULTS_DIR/request_${i}_timing.txt" ]; then
            duration=$(grep -o 'duration=[0-9.]*' "$RESULTS_DIR/request_${i}_timing.txt" | cut -d= -f2 | sed 's/s$//')
            total_response_time=$(echo "$total_response_time + $duration" | bc -l)
        fi
    fi
done

success_rate=$(echo "scale=2; $successful_requests * 100 / $CONCURRENT_REQUESTS" | bc -l)
avg_response_time=$(echo "scale=3; $total_response_time / $CONCURRENT_REQUESTS" | bc -l)
requests_per_second=$(echo "scale=2; $CONCURRENT_REQUESTS / $total_duration" | bc -l)

print_color "$GREEN" "Successful Requests: $successful_requests"
print_color "$RED" "Failed Requests: $failed_requests"
print_color "$YELLOW" "Success Rate: ${success_rate}%"
print_color "$YELLOW" "Average Response Time: ${avg_response_time}s"
print_color "$YELLOW" "Requests/Second: $requests_per_second"
print_color "$YELLOW" "Total Test Duration: ${total_duration}s"

# Generate summary report
cat > "$RESULTS_DIR/summary_report.txt" << EOF
Web Scraper Load Test Summary
=============================
Test Timestamp: $(date)
Target URL: $URL
Concurrent Requests: $CONCURRENT_REQUESTS
Results per Query: $RESULTS
Timeout: ${TIMEOUT}s

Results:
--------
Successful Requests: $successful_requests
Failed Requests: $failed_requests
Success Rate: ${success_rate}%
Average Response Time: ${avg_response_time}s
Requests/Second: $requests_per_second
Total Test Duration: ${total_duration}s

Individual Request Timings:
---------------------------
$(cat "$RESULTS_DIR"/request_*_timing.txt)
EOF

print_color "$BLUE" "Results saved to: $RESULTS_DIR/"
print_color "$BLUE" "Summary report: $RESULTS_DIR/summary_report.txt"

# Performance assessment
if (( $(echo "$success_rate >= 95" | bc -l) )); then
    print_color "$GREEN" "Success rate above 95%"
elif (( $(echo "$success_rate >= 80" | bc -l) )); then
    print_color "$YELLOW" "Success rate above 80%"
else
    print_color "$RED" "Success rate below 80%"
fi

if (( $(echo "$avg_response_time <= 5" | bc -l) )); then
    print_color "$GREEN" "Average response time under 5s"
elif (( $(echo "$avg_response_time <= 10" | bc -l) )); then
    print_color "$YELLOW" "Average response time 5-10s"
else
    print_color "$RED" "Average response time over 10s"
fi

echo
print_color "$BLUE" "Load test completed!"