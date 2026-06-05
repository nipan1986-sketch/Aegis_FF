#!/usr/bin/env bash
set -euo pipefail

TOOLS_ROOT="${TOOLS_ROOT:-/home/firefly/rlgl_tools}"
PYTHON_BIN="${PYTHON_BIN:-/usr/bin/python3}"
PORT="${RLGL_NETWORK_PORT:-8876}"
SERVICE_PATH="/etc/systemd/system/rlgl-network.service"
SERVICE_SCRIPT="${TOOLS_ROOT}/rlgl_network_service.py"
ROS_SETUP="${ROS_SETUP:-/opt/ros/humble/setup.bash}"
ROS_DOMAIN_ID_VALUE="${ROS_DOMAIN_ID:-24}"
RMW_IMPLEMENTATION_VALUE="${RMW_IMPLEMENTATION:-rmw_zenoh_cpp}"

if [[ "${EUID}" -ne 0 ]]; then
  echo "Please run with sudo." >&2
  exit 1
fi

if [[ ! -f "${SERVICE_SCRIPT}" ]]; then
  echo "Missing ${SERVICE_SCRIPT}" >&2
  exit 1
fi

chmod +x "${SERVICE_SCRIPT}"

cat >"${SERVICE_PATH}" <<SERVICE
[Unit]
Description=Red Light Green Light hotspot internet service
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=${TOOLS_ROOT}
Environment=ROS_DOMAIN_ID=${ROS_DOMAIN_ID_VALUE}
Environment=RMW_IMPLEMENTATION=${RMW_IMPLEMENTATION_VALUE}
ExecStart=/bin/bash -lc 'source "${ROS_SETUP}" && exec ${PYTHON_BIN} "${SERVICE_SCRIPT}" --host 0.0.0.0 --port ${PORT}'
Restart=always
RestartSec=2

[Install]
WantedBy=multi-user.target
SERVICE

systemctl daemon-reload
systemctl enable rlgl-network.service
systemctl restart rlgl-network.service
systemctl --no-pager --full status rlgl-network.service
