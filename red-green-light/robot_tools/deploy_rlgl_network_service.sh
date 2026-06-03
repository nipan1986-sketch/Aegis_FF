#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
HOST="${ROBOT_HOST:-192.168.234.1}"
USER_NAME="${ROBOT_USER:-firefly}"
PORT="${ROBOT_SSH_PORT:-22}"
REMOTE_DIR="${ROBOT_REMOTE_DIR:-/home/firefly/rlgl_tools}"
NETWORK_PORT="${RLGL_NETWORK_PORT:-8876}"

SSH_TARGET="${USER_NAME}@${HOST}"
SSH_OPTS=(-o StrictHostKeyChecking=no -p "${PORT}")
SCP_OPTS=(-o StrictHostKeyChecking=no -P "${PORT}")

echo "Deploying RLGL network service to ${SSH_TARGET}:${REMOTE_DIR}"
ssh "${SSH_OPTS[@]}" "${SSH_TARGET}" "mkdir -p '${REMOTE_DIR}'"
scp "${SCP_OPTS[@]}" \
  "${SCRIPT_DIR}/rlgl_network_service.py" \
  "${SCRIPT_DIR}/install_rlgl_network_service.sh" \
  "${SSH_TARGET}:${REMOTE_DIR}/"

echo "Installing service. If sudo asks, use the Firefly password."
ssh -t "${SSH_OPTS[@]}" "${SSH_TARGET}" \
  "chmod +x '${REMOTE_DIR}/install_rlgl_network_service.sh' '${REMOTE_DIR}/rlgl_network_service.py' && sudo TOOLS_ROOT='${REMOTE_DIR}' RLGL_NETWORK_PORT='${NETWORK_PORT}' bash '${REMOTE_DIR}/install_rlgl_network_service.sh'"

echo
echo "Checking service:"
if command -v curl >/dev/null 2>&1; then
  curl -fsS "http://${HOST}:${NETWORK_PORT}/status" || true
fi
