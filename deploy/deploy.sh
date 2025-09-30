#!/bin/bash

# 작업 디렉토리 설정
cd /home/ubuntu

# ✅ .env 파일 로드
if [ -f "/home/ubuntu/.env" ]; then
  source /home/ubuntu/.env
else
  echo "⚠️ .env 파일을 찾을 수 없습니다. 스크립트를 종료합니다."
  exit 1
fi

# ✅ 현재 실행중인 App이 green인지 확인합니다.
IS_GREEN=$(sudo docker ps --format '{{.Names}}' | grep -w green)

# nginx 설정 파일 경로
GREEN_NGINX_CONF="/home/ubuntu/nginx/green-nginx.conf"
BLUE_NGINX_CONF="/home/ubuntu/nginx/blue-nginx.conf"
NGINX_CONF="/home/ubuntu/nginx/nginx.conf"

DOCKER_COMPOSE_FILE="/home/ubuntu/docker-compose.yaml"

MESSAGE_SUCCESS="✅ '일단!' 배포가 성공적으로 수행되었습니다!"
MESSAGE_FAILURE="🚨 '일단!' 배포 과정에서 오류가 발생했습니다. 빠른 확인바랍니다."

send_discord_message() {
  local message=$1
  curl -H "Content-Type: application/json" -d "{\"content\": \"$message\"}" $DISCORD_DEPLOY_RESULT_WEBHOOK_URL
}

#  Prometheus 타겟 업데이트 함수
update_prometheus_target() {
  local color="$1"      # blue | green
  local port="$2"       # 9001 | 9002
  local temp_local="/tmp/targets.json"

  local remote_file="${REMOTE_TARGETS_FILE_PATH}"
  local remote_home_tmp="~/targets.tmp.json"

  echo ">>> Prometheus 타겟을 ${color} (${PROMETHEUS_TARGET_SERVER}:${port}) 로 변경합니다."
  echo ">>> 대상 파일: ${MONITORING_SERVER_USER}@${MONITORING_SERVER}:${remote_file}"

  cat > "$temp_local" <<EOF
[
  {
    "targets": ["${PROMETHEUS_TARGET_SERVER}:${port}"],
    "labels": { "color": "${color}" }
  }
]
EOF

  scp -o StrictHostKeyChecking=yes -i "$SSH_KEY_PATH" \
    "$temp_local" \
    "${MONITORING_SERVER_USER}@${MONITORING_SERVER}:${remote_home_tmp}" || {
      echo "💥 타겟 임시 업로드 실패"
      rm -f "$temp_local"
      exit 1
    }

  ssh -o StrictHostKeyChecking=yes -i "$SSH_KEY_PATH" \
    "${MONITORING_SERVER_USER}@${MONITORING_SERVER_IP}" \
    "jq . ${remote_home_tmp} >/dev/null 2>&1 \
      && mkdir -p \"\$(dirname \"${remote_file}\")\" \
      && install -m 644 -T ${remote_home_tmp} ${remote_file} \
      && rm -f ${remote_home_tmp}" || {
        echo "💥 원격 JSON 검증/교체 실패(jq/권한/경로)"
        rm -f "$temp_local"
        exit 1
      }

  rm -f "$temp_local"
  echo "✅ Prometheus 타겟 업데이트 완료"
}

# 💚 blue가 실행중이라면 green을 up합니다.
if [ -z "$IS_GREEN" ]; then
  echo "### BLUE => GREEN ###"

  echo ">>> 1. green container를 up합니다."
  sudo docker compose -f "$DOCKER_COMPOSE_FILE" up --build -d green || {
    send_discord_message "$MESSAGE_FAILURE"
    exit 1
  }

  SECONDS=0
  while true; do
    echo ">>> 2. green health check 중..."
    sleep 3
    REQUEST=$(sudo docker exec illdan-green wget -qO- http://localhost:9001/actuator/health)
    if [[ "$REQUEST" == *"UP"* ]]; then
      echo "✅ health check success!!!"
      break
    fi
    if [ $SECONDS -ge 120 ]; then
      echo "💥 health check failed (timeout)!!!"
      send_discord_message "$MESSAGE_FAILURE"
      exit 1
    fi
  done

  # Prometheus 타겟 업데이트 (green, 9002 포트)
  update_prometheus_target "green" "9002"

  echo ">>> 3. nginx 라우팅 변경 및 reload"
  sudo cp "$GREEN_NGINX_CONF" "$NGINX_CONF"
  sudo docker exec illdan-nginx nginx -s reload || {
    send_discord_message "$MESSAGE_FAILURE"
    exit 1
  }

  echo ">>> 4. blue container를 종료합니다."
  sudo docker compose -f "$DOCKER_COMPOSE_FILE" stop blue || {
    send_discord_message "$MESSAGE_FAILURE"
    exit 1
  }

else
  echo "### GREEN => BLUE ###"

  echo ">>> 1. blue container를 up합니다."
  sudo docker compose -f "$DOCKER_COMPOSE_FILE" up --build -d blue || {
    send_discord_message "$MESSAGE_FAILURE"
    exit 1
  }

  SECONDS=0
  while true; do
    echo ">>> 2. blue health check 중..."
    sleep 3
    REQUEST=$(sudo docker exec illdan-blue wget -qO- http://localhost:9001/actuator/health)
    if [[ "$REQUEST" == *"UP"* ]]; then
      echo "✅ health check success!!!"
      break
    fi
    if [ $SECONDS -ge 120 ]; then
      echo "💥 health check failed (timeout)!!!"
      send_discord_message "$MESSAGE_FAILURE"
      exit 1
    fi
  done

  # Prometheus 타겟 업데이트 (blue, 9001 포트)
  update_prometheus_target "blue" "9001"

  echo ">>> 3. nginx 라우팅 변경 및 reload"
  sudo cp "$BLUE_NGINX_CONF" "$NGINX_CONF"
  sudo docker exec illdan-nginx nginx -s reload || {
    send_discord_message "$MESSAGE_FAILURE"
    exit 1
  }

  echo ">>> 4. green container를 종료합니다."
  sudo docker compose -f "$DOCKER_COMPOSE_FILE" stop green || {
    send_discord_message "$MESSAGE_FAILURE"
    exit 1
  }

fi

echo ">>> 5. Docker 이미지 정리"
sudo docker image prune -f
echo ">>> 6. Docker 빌드 캐시 정리"
sudo docker builder prune -f --filter "until=24h"

send_discord_message "$MESSAGE_SUCCESS"
