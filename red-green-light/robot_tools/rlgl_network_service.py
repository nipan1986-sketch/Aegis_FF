#!/usr/bin/env python3
"""Red Light Green Light robot-side network service.

This service is intentionally separate from BrainBlocksUDPAegis. It lets the
Pad ask the robot to join an external Wi-Fi network while keeping the robot
hotspot available, then enables NAT so the Pad can reach the internet through
the robot hotspot.
"""

import argparse
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import os
import re
import shutil
import subprocess
import time
from urllib.parse import parse_qs, urlparse


DEFAULT_HOTSPOT_IP = "192.168.234.1"
DEFAULT_HOTSPOT_CIDR = "192.168.234.0/24"
DEFAULT_CONNECTION_NAME = "rlgl-external-wifi"


def run_cmd(cmd, timeout=20):
    proc = subprocess.run(
        cmd,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        timeout=timeout,
        check=False,
    )
    return proc.returncode, proc.stdout or ""


def shell_status():
    parts = []
    iptables_bin = shutil.which(os.environ.get("RLGL_IPTABLES", "")) or shutil.which("iptables-legacy") or shutil.which("iptables") or "iptables"
    for cmd in (
        ["hostname"],
        ["ip", "-br", "addr"],
        ["ip", "route"],
        ["nmcli", "-t", "-f", "DEVICE,TYPE,STATE,CONNECTION", "device", "status"],
        ["sysctl", "net.ipv4.ip_forward"],
        [iptables_bin, "-t", "nat", "-S", "POSTROUTING"],
        [iptables_bin, "-S", "FORWARD"],
    ):
        try:
            rc, out = run_cmd(cmd, timeout=5)
            parts.append("$ " + " ".join(cmd) + "\n" + out.strip() + "\nrc=" + str(rc))
        except Exception as exc:
            parts.append("$ " + " ".join(cmd) + "\nerror=" + str(exc))
    return "\n\n".join(parts)


def detect_ap_iface(hotspot_ip):
    rc, out = run_cmd(["ip", "-o", "-4", "addr", "show"], timeout=5)
    if rc != 0:
        return ""
    for line in out.splitlines():
        if hotspot_ip in line:
            parts = line.split()
            if len(parts) >= 2:
                return parts[1]
    return ""


def detect_default_iface():
    rc, out = run_cmd(["ip", "route", "show", "default"], timeout=5)
    if rc != 0:
        return ""
    parts = out.split()
    for idx, item in enumerate(parts):
        if item == "dev" and idx + 1 < len(parts):
            return parts[idx + 1]
    return ""


def detect_wifi_iface(ap_iface):
    rc, out = run_cmd(["nmcli", "-t", "-f", "DEVICE,TYPE", "device", "status"], timeout=8)
    if rc == 0:
        for line in out.splitlines():
            pieces = line.split(":")
            if len(pieces) >= 2 and pieces[1] == "wifi" and pieces[0] != ap_iface:
                return pieces[0]
    for name in os.listdir("/sys/class/net"):
        if name != ap_iface and os.path.isdir("/sys/class/net/%s/wireless" % name):
            return name
    return ""


def active_wifi_connection(wifi_iface):
    rc, out = run_cmd(["nmcli", "-t", "-f", "DEVICE,TYPE,STATE,CONNECTION", "device", "status"], timeout=8)
    if rc != 0:
        return ""
    for line in out.splitlines():
        pieces = line.split(":", 3)
        if len(pieces) >= 4 and pieces[0] == wifi_iface and pieces[1] == "wifi" and pieces[2] == "connected":
            return pieces[3]
    return ""


def connection_ssid(connection_name):
    if not connection_name:
        return ""
    rc, out = run_cmd(["nmcli", "-g", "802-11-wireless.ssid", "connection", "show", connection_name], timeout=8)
    if rc != 0:
        return ""
    return out.strip().splitlines()[0].strip() if out.strip() else ""


def find_connection_by_ssid(ssid):
    rc, out = run_cmd(["nmcli", "-t", "-f", "NAME,TYPE", "connection", "show"], timeout=10)
    if rc != 0:
        return ""
    for line in out.splitlines():
        pieces = line.split(":", 1)
        if len(pieces) != 2 or pieces[1] != "wifi":
            continue
        name = pieces[0]
        saved_ssid = connection_ssid(name)
        if saved_ssid == ssid or name == ssid:
            return name
    return ""


def wait_for_default_iface(timeout_s=12):
    deadline = time.time() + timeout_s
    while time.time() < deadline:
        iface = detect_default_iface()
        if iface:
            return iface
        time.sleep(0.5)
    return ""


def valid_iface(value):
    return bool(re.match(r"^[A-Za-z0-9_.:-]+$", value or ""))


def run_checked(cmd, timeout=20):
    rc, out = run_cmd(cmd, timeout=timeout)
    if rc != 0:
        raise RuntimeError("command failed: %s\n%s" % (" ".join(cmd), out.strip()))
    return out


def choose_iptables():
    candidates = []
    env_choice = os.environ.get("RLGL_IPTABLES", "").strip()
    if env_choice:
        candidates.append(env_choice)
    candidates.extend(["iptables-legacy", "iptables"])

    seen = set()
    for candidate in candidates:
        path = shutil.which(candidate)
        if not path or path in seen:
            continue
        seen.add(path)
        nat_ok = run_cmd([path, "-t", "nat", "-S", "POSTROUTING"], timeout=8)[0] == 0
        forward_ok = run_cmd([path, "-S", "FORWARD"], timeout=8)[0] == 0
        if nat_ok and forward_ok:
            return path
    raise RuntimeError("no usable iptables backend found; tried %s" % ", ".join(candidates))


def ensure_iptables_rule(iptables_bin, check_args, add_args):
    if run_cmd([iptables_bin] + check_args, timeout=8)[0] != 0:
        run_checked([iptables_bin] + add_args, timeout=8)


def ensure_nat(ap_iface, upstream_iface, hotspot_cidr):
    if not ap_iface:
        raise RuntimeError("hotspot interface not found")
    if not upstream_iface:
        raise RuntimeError("upstream interface not found")
    if not valid_iface(ap_iface) or not valid_iface(upstream_iface):
        raise RuntimeError("bad interface name")
    if ap_iface == upstream_iface:
        raise RuntimeError("hotspot and upstream are the same interface; AP+STA is not active")

    iptables_bin = choose_iptables()

    run_checked(["sysctl", "-w", "net.ipv4.ip_forward=1"], timeout=8)
    os.makedirs("/etc/sysctl.d", exist_ok=True)
    with open("/etc/sysctl.d/99-rlgl-hotspot-internet.conf", "w", encoding="utf-8") as f:
        f.write("net.ipv4.ip_forward=1\n")

    nat_rule = ["-s", hotspot_cidr, "-o", upstream_iface, "-j", "MASQUERADE"]
    ensure_iptables_rule(
        iptables_bin,
        ["-t", "nat", "-C", "POSTROUTING"] + nat_rule,
        ["-t", "nat", "-A", "POSTROUTING"] + nat_rule,
    )

    forward_out = ["-i", ap_iface, "-o", upstream_iface, "-j", "ACCEPT"]
    ensure_iptables_rule(
        iptables_bin,
        ["-C", "FORWARD"] + forward_out,
        ["-A", "FORWARD"] + forward_out,
    )

    forward_back = [
        "-i",
        upstream_iface,
        "-o",
        ap_iface,
        "-m",
        "conntrack",
        "--ctstate",
        "RELATED,ESTABLISHED",
        "-j",
        "ACCEPT",
    ]
    ensure_iptables_rule(
        iptables_bin,
        ["-C", "FORWARD"] + forward_back,
        ["-A", "FORWARD"] + forward_back,
    )
    return iptables_bin


def configure_wifi(data):
    hotspot_ip = str(data.get("hotspot_ip") or DEFAULT_HOTSPOT_IP).strip()
    hotspot_cidr = str(data.get("hotspot_cidr") or DEFAULT_HOTSPOT_CIDR).strip()
    ap_iface = str(data.get("ap_iface") or "").strip() or detect_ap_iface(hotspot_ip)
    wifi_iface = str(data.get("wifi_iface") or "").strip() or detect_wifi_iface(ap_iface)
    ssid = str(data.get("ssid") or "").strip()
    password = str(data.get("password") or "")
    open_network = bool(data.get("open", False))
    hidden = bool(data.get("hidden", False))
    connection_name = str(data.get("connection_name") or DEFAULT_CONNECTION_NAME).strip()

    if not ssid:
        raise RuntimeError("missing ssid")
    if not wifi_iface:
        raise RuntimeError("station Wi-Fi interface not found")
    if not valid_iface(wifi_iface):
        raise RuntimeError("bad station Wi-Fi interface")

    logs = []
    logs.append("ap_iface=%s wifi_iface=%s ssid=%s" % (ap_iface, wifi_iface, ssid))
    run_cmd(["nmcli", "radio", "wifi", "on"], timeout=10)

    active_connection = active_wifi_connection(wifi_iface)
    active_ssid = connection_ssid(active_connection) or active_connection
    if active_connection and active_ssid == ssid:
        logs.append("already connected using profile %s" % active_connection)
    else:
        saved_connection = find_connection_by_ssid(ssid)
        if saved_connection:
            rc, out = run_cmd(["nmcli", "connection", "up", saved_connection], timeout=45)
            logs.append("using saved profile %s\n%s" % (saved_connection, out))
            if rc != 0:
                raise RuntimeError("saved Wi-Fi profile failed: " + out.strip())
        else:
            if not open_network and not password:
                raise RuntimeError("missing password and no saved Wi-Fi profile for %s" % ssid)
            run_cmd(["nmcli", "device", "wifi", "rescan", "ifname", wifi_iface], timeout=15)
            if run_cmd(["nmcli", "-t", "-f", "NAME", "connection", "show"], timeout=10)[1].splitlines().count(connection_name):
                run_cmd(["nmcli", "connection", "delete", connection_name], timeout=15)

            args = ["nmcli", "device", "wifi", "connect", ssid, "ifname", wifi_iface, "name", connection_name]
            if not open_network:
                args += ["password", password]
            if hidden:
                args += ["hidden", "yes"]

            rc, out = run_cmd(args, timeout=45)
            logs.append(out)
            if rc != 0:
                logs.append("direct connect failed, trying explicit profile")
                run_cmd(["nmcli", "connection", "delete", connection_name], timeout=15)
                rc, out = run_cmd(
                    ["nmcli", "connection", "add", "type", "wifi", "ifname", wifi_iface, "con-name", connection_name, "ssid", ssid],
                    timeout=15,
                )
                logs.append(out)
                run_cmd(["nmcli", "connection", "modify", connection_name, "connection.autoconnect", "yes"], timeout=10)
                if hidden:
                    run_cmd(["nmcli", "connection", "modify", connection_name, "802-11-wireless.hidden", "yes"], timeout=10)
                if not open_network:
                    run_cmd(
                        ["nmcli", "connection", "modify", connection_name, "wifi-sec.key-mgmt", "wpa-psk", "wifi-sec.psk", password],
                        timeout=10,
                    )
                rc, out = run_cmd(["nmcli", "connection", "up", connection_name], timeout=45)
                logs.append(out)
                if rc != 0:
                    raise RuntimeError("nmcli connection failed: " + out.strip())

    upstream_iface = str(data.get("upstream_iface") or "").strip() or wait_for_default_iface()
    iptables_bin = ensure_nat(ap_iface, upstream_iface, hotspot_cidr)
    ping_ok = run_cmd(["ping", "-c", "1", "-W", "2", "8.8.8.8"], timeout=5)[0] == 0
    return {
        "ap_iface": ap_iface,
        "wifi_iface": wifi_iface,
        "upstream_iface": upstream_iface,
        "iptables": iptables_bin,
        "hotspot_ip": hotspot_ip,
        "hotspot_cidr": hotspot_cidr,
        "internet_ping_ok": ping_ok,
        "log": "\n".join(logs)[-4000:],
    }


class Handler(BaseHTTPRequestHandler):
    def _send_json(self, code, payload):
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.send_header("Access-Control-Allow-Methods", "GET,POST,OPTIONS")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_OPTIONS(self):
        self.send_response(204)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.send_header("Access-Control-Allow-Methods", "GET,POST,OPTIONS")
        self.end_headers()

    def do_GET(self):
        path = urlparse(self.path).path
        if path in ("/status", "/status/", "/network/status", "/network/status/"):
            self._send_json(200, {"ok": True, "service": "rlgl-network-service", "status": shell_status()[-8000:]})
            return
        self._send_json(404, {"ok": False, "error": "not found"})

    def do_POST(self):
        path = urlparse(self.path).path
        if path not in ("/network/configure", "/network/configure/"):
            self._send_json(404, {"ok": False, "error": "not found"})
            return
        try:
            length = int(self.headers.get("Content-Length") or "0")
            raw = self.rfile.read(length).decode("utf-8")
            data = json.loads(raw or "{}")
        except Exception as exc:
            self._send_json(400, {"ok": False, "error": "bad json: %s" % exc})
            return
        try:
            result = configure_wifi(data)
            self._send_json(200, {"ok": True, "error": None, **result})
        except Exception as exc:
            self._send_json(200, {"ok": False, "error": str(exc), "status": shell_status()[-8000:]})

    def log_message(self, fmt, *args):
        print("[rlgl-network]", fmt % args, flush=True)


def main():
    parser = argparse.ArgumentParser(description="RLGL robot network service")
    parser.add_argument("--host", default="0.0.0.0")
    parser.add_argument("--port", type=int, default=int(os.environ.get("RLGL_NETWORK_PORT", "8876")))
    args = parser.parse_args()
    server = ThreadingHTTPServer((args.host, args.port), Handler)
    print("RLGL network service listening on %s:%s" % (args.host, args.port), flush=True)
    server.serve_forever()


if __name__ == "__main__":
    main()
