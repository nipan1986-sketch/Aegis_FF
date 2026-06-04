# ZsiBot 红灯绿灯 Android Pad 游戏

第一版功能：

- 默认状态显示狗脸，参考 Navi Pad 应用的全屏狗脸风格。
- 只能点击“开始”按钮开始红灯绿灯循环；触摸空白区域不会再启动游戏。
- 绿灯阶段：Pad 同时触发 UDP/remote 命令、绿色屏幕、动作图标和语音。
- 红灯阶段：Pad 同时触发 UDP 停止帧、红色屏幕和 `Red Light！` 语音。
- 手动点击“停止”会额外执行 `155` recover -> `122` stand -> `174`，避免特技测试后机器狗停在不可响应状态。
- 游戏动作随机：往前走、往后退、向左移、向右移、向左转、向右转、往上跳、双腿站立、挥手。
- 绿灯到红灯、红灯到绿灯的间隔都在 2 到 10 秒之间随机变化。
- 点击“测试”会按顺序播放当前 app 支持的 UDP 动作：往前走、往后退、向左移、向右移、向左转、向右转、趴下、往上跳、双腿站立、挥手。
- 停止不再作为绿灯动作；红灯本身负责停止。
- 前进和后退会先发最低速档 `174`；joystick 幅度保持在可触发阈值以上，避免低到 dog_task 忽略。
- 基础移动都会先发低速/移动档 `174`；`175` 只用于 lock，不再放在移动摇杆帧前。
- 基础移动会在一个动作周期内复用同一个 UDP socket/source port，模拟 gateway/遥控器的连续摇杆流。
- 长按屏幕约 0.9 秒可修改 Robot IP、UDP Port、Gateway URL。
- 长按屏幕约 0.9 秒可设置机器狗外部 Wi-Fi，让 Pad 连接机器狗热点时也能通过机器狗上网。

默认连接：

- Robot IP: `192.168.234.1`
- Direct UDP: `192.168.234.1:8081`
- Gateway: 默认不使用。长按设置里填写 `http://192.168.234.1:8765` 后才会启用备用 HTTP `/cmd`。
- Pad 上网服务: `http://192.168.234.1:8876`，独立于 BrainBlocks gateway。

Pad 上网方案：

1. Pad 保持连接机器狗热点，仍使用 `192.168.234.1` 控制机器狗。
2. 机器狗自己连接外部 Wi-Fi。
3. 机器狗开启 NAT，把 `192.168.234.0/24` 热点网段转发到外部 Wi-Fi。
4. Android 设置页里填写外部 Wi-Fi SSID/密码，点击“连接外部 Wi-Fi 并开启 Pad 上网”。

机器狗侧独立服务：

```bash
cd /home/pan/matrix_ws/robot_edu_platform/edu_software/zsibot/red_light_green_light_android
bash robot_tools/deploy_rlgl_network_service.sh
```

默认安装到机器狗：

```text
/home/firefly/rlgl_tools
systemd service: rlgl-network.service
HTTP port: 8876
```

这个服务不修改 `BrainBlocksUDPAegis`，也不占用 BrainBlocks 的 `8765` 端口。

控制策略：

1. Pad 默认直接发送 dog_task remote UDP JSON 帧到 `192.168.234.1:8081`。
2. 绿灯和红灯现在都不再延迟显示；命令、屏幕、语音同一拍触发。
3. 红灯时停止 UDP streaming，并发 neutral 停止帧。
4. 如需兼容旧部署，可在长按设置里填写 Gateway URL，app 会额外发送短 HTTP `/cmd` 脉冲作为兜底。
5. 跳跃、双腿站立、挥手会先在后台发送预热/准备 UDP；预热完成后才显示 Green Light 和语音，同时发真正动作码，让机器狗看起来更快响应 Pad 提示。

直发 UDP 特殊动作码：

- `176`: Turbo / high speed 模式
- `106`: 趴下
- `1`: 往上跳 / 原地跳
- `2`: 前跳 / frontjump
- `3`: 后空翻 / backflip
- `5`: 双腿站立
- `4`: 挥手 / shakehand
- `122`: 站立
- `155`: 恢复

随机动作库：

1. 小青蛙：`106` 趴下 -> `122` 站立，循环近似蹲起。
2. 小兔子：站稳 + 跳跃预热 -> `1` 原地跳 -> `155` 恢复 -> `122` 站立 -> `175` lock，循环。
3. 小袋鼠：站稳 + 跳跃预热 -> `2` 前跳 -> `155` 恢复 -> `122` 站立 -> `175` lock，循环。
4. 小演员：`122` 站立 -> 大幅左移 -> 大幅右移，循环。
5. 小狗巡逻队：左转 -> 右转，循环。
6. 小机器人：低速前进 -> 低速后退，循环。
7. 小猩猩：`176` Turbo 准备 -> `5` 后足/双腿站立 -> `155` 恢复 -> `122` 站立，循环。
8. 小舞者：`122` 站立 -> `175` lock -> 大幅 roll/pitch/yaw 身体摇摆，循环。
9. 小运动员：站稳 + 后空翻预热 -> `3` 后空翻 -> `155` 恢复 -> `122` 站立 -> `175` lock，循环。

绿灯动作时长和红灯停止时长都随机为 3s 到 8s；红灯出现前，当前动作会一直循环。
每个特技动作开始前执行 stop -> `155` recover -> `122` stand -> `175` lock。
每个非特技动作结束后只发 `175` lock，不再额外 stand。

高风险/表演动作的直发顺序：

- 往上跳：复制 BrainBlocks gateway 的 hybrid preamble（heartbeat/debug_speed/neutral/lab/heartbeat）-> `1`
- 前跳：复制同一套 jump hybrid preamble -> `2`
- 后空翻：复制同一套 jump hybrid preamble -> `3`
- 双腿站立：`176` -> `5`
- 挥手：`176` -> `4`

调试结论：

- 之前失败主要不是动作码发送时间不够，而是特殊动作前置 UDP 序列不完整。
- 往上跳不能只发 `176 + button[2] + 1`；当前机器狗 gateway 对 jump 默认先跑 hybrid preamble。
- 双腿站立、挥手在 Pad 直发模式下增加了 neutral/stand 准备和 turbo `176`。
- 游戏动作线程使用 token 作废旧一轮运动，避免上一轮绿灯线程在下一轮继续发旧方向或停止帧。
- 基础移动不能每一帧都换 UDP 源端口；否则 `cmd` 类动作可能正常，但连续摇杆动作可能不被 `dog_task` 当作同一路遥控输入。
- 挥手、双腿站立后会执行更完整的恢复序列：recover -> debug_speed reset -> neutral -> stand -> 中速档。

构建 APK：

```bash
cd /home/pan/matrix_ws/robot_edu_platform/edu_software/zsibot/red_light_green_light_android
bash build_apk.sh
```

安装到已连接的 Pad：

```bash
bash build_apk.sh --install
```

APK 输出：

```text
manual-build/zsibot-red-light-green-light.apk
```
