package com.matrix.zsibotrlgl;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends Activity {
    private static final String PREFS = "zsibot_red_light_green_light";
    private static final String DEFAULT_ROBOT_IP = "192.168.234.1";
    private static final int DEFAULT_UDP_PORT = 8081;
    private static final String DEFAULT_GATEWAY_URL = "";
    private static final String DEFAULT_NETWORK_SERVICE_URL = "http://192.168.234.1:8876";
    private static final int GREEN_PHASE_MS = 8000;
    private static final int RED_PHASE_MS = 3000;
    private static final int STOP_BEFORE_RED_MS = 2000;
    private static final int STAND_READY_MS = 2800;
    private static final int START_COUNTDOWN_MS = 3300;
    private static final int RED_READY_TIMEOUT_NORMAL_MS = 1300;
    private static final int RED_READY_TIMEOUT_SPECIAL_MS = 2600;

    private static final GameAction FORWARD = new GameAction(
            "往前走", "forward", "往前走", SignalView.Icon.FORWARD,
            0.0, -0.40, 0.0, 0.0,
            0.42, 0.0, 0.0
    );
    private static final GameAction BACKWARD = new GameAction(
            "往后退", "backward", "往后退", SignalView.Icon.BACKWARD,
            0.0, 0.40, 0.0, 0.0,
            -0.42, 0.0, 0.0
    );
    private static final GameAction LEFT = new GameAction(
            "向左移", "left", "向左移", SignalView.Icon.LEFT,
            -0.50, 0.0, 0.0, 0.0,
            0.0, -0.62, 0.0
    );
    private static final GameAction RIGHT = new GameAction(
            "向右移", "right", "向右移", SignalView.Icon.RIGHT,
            0.50, 0.0, 0.0, 0.0,
            0.0, 0.62, 0.0
    );
    private static final GameAction TURN_LEFT = new GameAction(
            "向左转", "turn left", "向左转", SignalView.Icon.TURN_LEFT,
            0.0, 0.0, 0.45, 0.0,
            0.0, 0.0, 0.72
    );
    private static final GameAction TURN_RIGHT = new GameAction(
            "向右转", "turn right", "向右转", SignalView.Icon.TURN_RIGHT,
            0.0, 0.0, -0.45, 0.0,
            0.0, 0.0, -0.72
    );
    private static final GameAction ACTOR_LEFT_03 = new GameAction(
            "向左移", "left", "向左移", SignalView.Icon.LEFT,
            -0.30, 0.0, 0.0, 0.0,
            0.0, -0.30, 0.0
    );
    private static final GameAction ACTOR_RIGHT_03 = new GameAction(
            "向右移", "right", "向右移", SignalView.Icon.RIGHT,
            0.30, 0.0, 0.0, 0.0,
            0.0, 0.30, 0.0
    );
    private static final GameAction PATROL_TURN_LEFT_03 = new GameAction(
            "向左转", "turn left", "向左转", SignalView.Icon.TURN_LEFT,
            0.0, 0.0, 0.30, 0.0,
            0.0, 0.0, 0.30
    );
    private static final GameAction PATROL_TURN_RIGHT_03 = new GameAction(
            "向右转", "turn right", "向右转", SignalView.Icon.TURN_RIGHT,
            0.0, 0.0, -0.30, 0.0,
            0.0, 0.0, -0.30
    );
    private static final GameAction ROBOT_FORWARD_03 = new GameAction(
            "往前走", "forward", "往前走", SignalView.Icon.FORWARD,
            0.0, -0.45, 0.0, 0.0,
            0.30, 0.0, 0.0
    );
    private static final GameAction ROBOT_BACKWARD_03 = new GameAction(
            "往后退", "backward", "往后退", SignalView.Icon.BACKWARD,
            0.0, 0.45, 0.0, 0.0,
            -0.30, 0.0, 0.0
    );
    private static final SpecialAction LIE_DOWN = new SpecialAction(
            "趴下", "lie", "趴下", SignalView.Icon.STAY,
            106, "lie", 2100, 122, "stand", 1700,
            false, false, false, false, -1, 0
    );
    private static final SpecialAction JUMP_UP = new SpecialAction(
            "往上跳", "jump up", "往上跳", SignalView.Icon.JUMP_UP,
            1, "jump", 2200, 155, "recover", 1200,
            true, true, false, false, -1, 0
    );
    private static final SpecialAction FRONT_JUMP = new SpecialAction(
            "向前跳", "front jump", "向前跳", SignalView.Icon.FORWARD,
            2, "frontjump", 2600, 155, "recover", 1600,
            true, true, false, false, -1, 0
    );
    private static final SpecialAction BACK_FLIP = new SpecialAction(
            "后空翻", "back flip", "后空翻", SignalView.Icon.JUMP_UP,
            3, "backflip", 3200, 155, "recover", 1400,
            true, true, false, false, -1, 0
    );
    private static final SpecialAction TWO_LEG_STAND = new SpecialAction(
            "双腿站立", "two leg stand", "双腿站立", SignalView.Icon.STAY,
            5, "two_leg_stand", 3000, 155, "recover", 1700,
            false, true, false, true, -1, 0
    );
    private static final SpecialAction WAVE = new SpecialAction(
            "挥手", "wave", "挥手", SignalView.Icon.STAY,
            4, "shakehand", 6500, 155, "recover", 1200,
            false, true, true, true, -1, 0
    );
    private static final LibraryAction FROG_SQUAT = new LibraryAction(
            "小青蛙", "小青蛙，蹲起", SignalView.Icon.STAY, null,
            new LibraryStep[]{
                    LibraryStep.cmd(106, "lie", 1000),
                    LibraryStep.cmd(122, "stand", 1400)
            },
            5200
    );
    private static final LibraryAction BUNNY_JUMP = new LibraryAction(
            "小兔子", "小兔子，原地跳", SignalView.Icon.JUMP_UP, JUMP_UP,
            new LibraryStep[]{
                    LibraryStep.cmd(1, "jump", 2200),
                    LibraryStep.cmd(155, "recover", 1000),
                    LibraryStep.cmd(122, "stand", 1700),
                    LibraryStep.cmd(175, "", 300)
            },
            6800
    );
    private static final LibraryAction KANGAROO_FRONT_JUMP = new LibraryAction(
            "小袋鼠", "小袋鼠，向前跳", SignalView.Icon.FORWARD, FRONT_JUMP,
            new LibraryStep[]{
                    LibraryStep.cmd(2, "frontjump", 2800),
                    LibraryStep.cmd(155, "recover", 1700),
                    LibraryStep.cmd(122, "stand", 2500)
            },
            7200
    );
    private static final LibraryAction ACTOR_SIDE_STEP = new LibraryAction(
            "小演员", "小演员，左移右移", SignalView.Icon.LEFT, null,
            new LibraryStep[]{
                    LibraryStep.move(ACTOR_LEFT_03, 2000),
                    LibraryStep.move(ACTOR_RIGHT_03, 2000)
            },
            6200
    );
    private static final LibraryAction DANCER_STAY_SWAY = new LibraryAction(
            "小舞者", "小舞者，身体摇摆", SignalView.Icon.STAY, null,
            new LibraryStep[]{
                    LibraryStep.cmd(122, "stand", 800),
                    LibraryStep.cmd(175, "", 500),
                    LibraryStep.pose(-0.78, 0.0, 0.0, 0.0, 680),
                    LibraryStep.pose(0.78, 0.0, 0.0, 0.0, 680),
                    LibraryStep.pose(0.0, 0.0, 0.0, -0.78, 680),
                    LibraryStep.pose(0.0, 0.0, 0.0, 0.78, 680),
                    LibraryStep.pose(0.0, 0.0, -0.62, 0.0, 680),
                    LibraryStep.pose(0.0, 0.0, 0.62, 0.0, 680),
                    LibraryStep.pose(0.0, 0.0, 0.0, 0.0, 300)
            },
            7200
    );
    private static final LibraryAction PATROL_TURNS = new LibraryAction(
            "小狗巡逻队", "小狗巡逻队，左转右转", SignalView.Icon.TURN_LEFT, null,
            new LibraryStep[]{
                    LibraryStep.move(PATROL_TURN_LEFT_03, 2000),
                    LibraryStep.move(PATROL_TURN_RIGHT_03, 2000)
            },
            5200
    );
    private static final LibraryAction ROBOT_FORWARD_BACK = new LibraryAction(
            "小机器人", "小机器人，前进后退", SignalView.Icon.FORWARD, null,
            new LibraryStep[]{
                    LibraryStep.move(ROBOT_FORWARD_03, 2000),
                    LibraryStep.move(ROBOT_BACKWARD_03, 2000)
            },
            5200
    );
    private static final LibraryAction GORILLA_TWO_LEG_STAND = new LibraryAction(
            "小猩猩", "小猩猩，后足站立", SignalView.Icon.STAY, TWO_LEG_STAND,
            new LibraryStep[]{
                    LibraryStep.cmd(5, "two_leg_stand", 5200),
                    LibraryStep.cmd(175, "", 300)
            },
            7000
    );
    private static final LibraryAction ATHLETE_BACK_FLIP = new LibraryAction(
            "小运动员", "小运动员，后空翻", SignalView.Icon.JUMP_UP, BACK_FLIP,
            new LibraryStep[]{
                    LibraryStep.cmd(3, "backflip", 3200),
                    LibraryStep.cmd(155, "recover", 1300),
                    LibraryStep.cmd(122, "stand", 2200),
                    LibraryStep.cmd(175, "", 300)
            },
            8000
    );
    private static final LibraryAction[] GAME_ACTIONS = {
            FROG_SQUAT,
            BUNNY_JUMP,
            KANGAROO_FRONT_JUMP,
            ACTOR_SIDE_STEP,
            PATROL_TURNS,
            ROBOT_FORWARD_BACK,
            GORILLA_TWO_LEG_STAND,
            DANCER_STAY_SWAY,
            ATHLETE_BACK_FLIP
    };
    private static final Object[] TEST_ACTIONS = {
            FROG_SQUAT,
            BUNNY_JUMP,
            KANGAROO_FRONT_JUMP,
            ACTOR_SIDE_STEP,
            PATROL_TURNS,
            ROBOT_FORWARD_BACK,
            GORILLA_TWO_LEG_STAND,
            DANCER_STAY_SWAY,
            ATHLETE_BACK_FLIP
    };

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService network = Executors.newCachedThreadPool();
    private final ExecutorService robotCommands = Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService heartbeat = Executors.newSingleThreadScheduledExecutor();
    private final Random random = new Random();
    private final AtomicBoolean moveStreaming = new AtomicBoolean(false);
    private final AtomicInteger udpSeq = new AtomicInteger(1);
    private final AtomicInteger motionToken = new AtomicInteger(1);

    private ImageView themeImage;
    private DogFaceView dogFace;
    private SignalView signalView;
    private TextView statusText;
    private Button startButton;
    private Button testButton;
    private Button stopButton;
    private TextToSpeech tts;
    private boolean gameRunning = false;
    private boolean testRunning = false;
    private boolean longPressTriggered = false;
    private int testIndex = 0;
    private String robotIp = DEFAULT_ROBOT_IP;
    private int udpPort = DEFAULT_UDP_PORT;
    private String gatewayUrl = DEFAULT_GATEWAY_URL;
    private String networkServiceUrl = DEFAULT_NETWORK_SERVICE_URL;

    private final Runnable openSettings = new Runnable() {
        @Override
        public void run() {
            longPressTriggered = true;
            showSettingsDialog();
        }
    };

    private final Runnable redPhase = new Runnable() {
        @Override
        public void run() {
            if (!gameRunning) {
                return;
            }
            enterRedPhase();
        }
    };

    private final Runnable nextGreen = new Runnable() {
        @Override
        public void run() {
            if (!gameRunning) {
                return;
            }
            enterGreenPhase();
        }
    };

    private final Runnable testStep = new Runnable() {
        @Override
        public void run() {
            runNextTestAction();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        enterImmersiveMode();
        loadSettings();
        buildUi();
        initTts();
        startHeartbeatLoop();
        showIdle();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            enterImmersiveMode();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopMoveCommands(false);
        mainHandler.removeCallbacksAndMessages(null);
        heartbeat.shutdownNow();
        robotCommands.shutdownNow();
        network.shutdownNow();
        if (tts != null) {
            tts.shutdown();
        }
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(255, 246, 132));

        themeImage = new ImageView(this);
        themeImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        themeImage.setImageResource(R.drawable.ui_welcome);
        root.addView(themeImage, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        dogFace = new DogFaceView(this);
        dogFace.setVisibility(View.GONE);
        root.addView(dogFace, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        signalView = new SignalView(this);
        signalView.setPhase(SignalView.Phase.HIDDEN);
        root.addView(signalView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        statusText = new TextView(this);
        statusText.setTextColor(Color.argb(170, 28, 36, 43));
        statusText.setTextSize(16);
        statusText.setGravity(Gravity.CENTER_VERTICAL);
        statusText.setPadding(dp(14), 0, dp(14), 0);
        statusText.setBackgroundColor(Color.argb(55, 255, 255, 255));
        statusText.setVisibility(View.GONE);
        FrameLayout.LayoutParams statusParams = new FrameLayout.LayoutParams(dp(410), dp(46));
        statusParams.gravity = Gravity.TOP | Gravity.LEFT;
        statusParams.setMargins(dp(18), dp(16), 0, 0);
        root.addView(statusText, statusParams);

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setPadding(dp(10), dp(10), dp(10), dp(10));
        controls.setBackgroundColor(Color.argb(0, 255, 255, 255));

        startButton = makeButton("Start", Color.rgb(245, 154, 22));
        startButton.setOnClickListener(v -> startGame());
        controls.addView(startButton, buttonParams());

        testButton = makeButton("测试", Color.rgb(31, 122, 140));
        testButton.setOnClickListener(v -> startActionTest());
        controls.addView(testButton, buttonParams());

        stopButton = makeButton("停止", Color.rgb(185, 45, 51));
        stopButton.setOnClickListener(v -> stopGame(true));
        controls.addView(stopButton, buttonParams());

        FrameLayout.LayoutParams controlsParams = new FrameLayout.LayoutParams(dp(410), dp(72));
        controlsParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        controlsParams.setMargins(0, 0, 0, dp(20));
        root.addView(controls, controlsParams);

        root.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                longPressTriggered = false;
                mainHandler.postDelayed(openSettings, 900);
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                mainHandler.removeCallbacks(openSettings);
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                mainHandler.removeCallbacks(openSettings);
                return true;
            }
            return false;
        });

        setContentView(root);
    }

    private Button makeButton(String label, int color) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(20);
        button.setTextColor(Color.WHITE);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(3), Color.WHITE);
        button.setBackground(bg);
        return button;
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(118), dp(56));
        params.setMargins(dp(8), 0, dp(8), 0);
        return params;
    }

    private void enterImmersiveMode() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    private void initTts() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.CHINA);
                if (result < 0) {
                    tts.setLanguage(Locale.getDefault());
                }
                tts.setSpeechRate(1.02f);
            }
        });
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        robotIp = prefs.getString("robotIp", DEFAULT_ROBOT_IP);
        udpPort = prefs.getInt("udpPort", DEFAULT_UDP_PORT);
        gatewayUrl = prefs.getString("gatewayUrl", DEFAULT_GATEWAY_URL);
        networkServiceUrl = prefs.getString("networkServiceUrl", DEFAULT_NETWORK_SERVICE_URL);
    }

    private void saveSettings(String ip, int port, String gateway, String networkUrl) {
        robotIp = ip.trim().isEmpty() ? DEFAULT_ROBOT_IP : ip.trim();
        udpPort = port > 0 ? port : DEFAULT_UDP_PORT;
        gatewayUrl = gateway.trim();
        networkServiceUrl = networkUrl.trim().isEmpty() ? defaultNetworkServiceUrl(robotIp) : networkUrl.trim();
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putString("robotIp", robotIp)
                .putInt("udpPort", udpPort)
                .putString("gatewayUrl", gatewayUrl)
                .putString("networkServiceUrl", networkServiceUrl)
                .apply();
        updateStatus("ZsiBot 红灯绿灯  " + robotIp + ":" + udpPort);
    }

    private void showIdle() {
        gameRunning = false;
        moveStreaming.set(false);
        signalView.setPhase(SignalView.Phase.HIDDEN);
        showThemeImage(R.drawable.ui_welcome);
        startButton.setEnabled(true);
        testButton.setEnabled(true);
        stopButton.setVisibility(View.GONE);
        stopButton.setEnabled(false);
        startButton.setVisibility(View.VISIBLE);
        testButton.setVisibility(View.VISIBLE);
        updateStatus("按开始按钮开始  " + robotIp + ":" + udpPort);
    }

    private void startGame() {
        if (gameRunning || testRunning) {
            return;
        }
        gameRunning = true;
        startButton.setEnabled(false);
        testButton.setEnabled(false);
        stopButton.setVisibility(View.VISIBLE);
        stopButton.setEnabled(true);
        signalView.setPhase(SignalView.Phase.HIDDEN);
        showCountdown();
        updateStatus("准备开始");
        sendStandCommands();
        mainHandler.postDelayed(nextGreen, START_COUNTDOWN_MS);
    }

    private void stopGame(boolean speak) {
        gameRunning = false;
        testRunning = false;
        mainHandler.removeCallbacks(redPhase);
        mainHandler.removeCallbacks(nextGreen);
        mainHandler.removeCallbacks(testStep);
        stopMoveCommands(true);
        robotCommands.execute(this::recoverAfterManualStop);
        signalView.setPhase(SignalView.Phase.HIDDEN);
        showThemeImage(R.drawable.ui_welcome);
        startButton.setEnabled(true);
        testButton.setEnabled(true);
        stopButton.setVisibility(View.GONE);
        stopButton.setEnabled(false);
        startButton.setVisibility(View.VISIBLE);
        testButton.setVisibility(View.VISIBLE);
        updateStatus("已停止");
        if (speak) {
            speak("游戏停止");
        }
    }

    private void enterGreenPhase() {
        LibraryAction action = GAME_ACTIONS[random.nextInt(GAME_ACTIONS.length)];
        runLibraryGameAction(action);
    }

    private void enterRedPhase() {
        stopMoveCommands(false);
        robotCommands.execute(() -> {
            sendStopBeforeRed();
            mainHandler.post(() -> {
                if (gameRunning) {
                    showRed(false);
                    mainHandler.postDelayed(nextGreen, RED_PHASE_MS);
                }
            });
            sendStopAfterRed();
        });
    }

    private void startActionTest() {
        if (gameRunning || testRunning) {
            return;
        }
        testRunning = true;
        testIndex = 0;
        startButton.setEnabled(false);
        testButton.setEnabled(false);
        stopButton.setVisibility(View.VISIBLE);
        stopButton.setEnabled(true);
        signalView.setPhase(SignalView.Phase.HIDDEN);
        showCountdown();
        updateStatus("动作测试准备");
        speak("动作测试开始");
        sendStandCommands();
        mainHandler.postDelayed(testStep, START_COUNTDOWN_MS);
    }

    private void runNextTestAction() {
        if (!testRunning) {
            return;
        }
        if (testIndex >= TEST_ACTIONS.length) {
            stopMoveCommands(false);
            testRunning = false;
            showIdle();
            updateStatus("动作测试完成");
            speak("动作测试完成");
            return;
        }
        Object action = TEST_ACTIONS[testIndex++];
        if (action instanceof GameAction) {
            GameAction gameAction = (GameAction) action;
            beginMoveCommands(gameAction, 2.3);
            showGreen(gameAction, true);
            mainHandler.postDelayed(() -> {
                if (!testRunning) {
                    return;
                }
                stopMoveCommands(false);
                network.execute(() -> {
                    sendStopBeforeRed();
                    mainHandler.post(() -> {
                        if (testRunning) {
                            showRed(true);
                            mainHandler.postDelayed(testStep, 1000);
                        }
                    });
                    sendStopAfterRed();
                });
            }, 1800);
        } else if (action instanceof SpecialAction) {
            SpecialAction special = (SpecialAction) action;
            runSpecialActionTest(special);
        } else if (action instanceof LibraryAction) {
            runLibraryActionTest((LibraryAction) action);
        }
    }

    private void showGreen(GameAction action, boolean testing) {
        hideThemeImage();
        signalView.setGreenAction(action.zh, action.icon);
        signalView.setPhase(SignalView.Phase.GREEN);
        updateStatus((testing ? "测试  " : "Green Light  ") + action.zh);
        speak("Green Light！" + action.speech + "！");
    }

    private void showGreen(SpecialAction action, boolean testing) {
        hideThemeImage();
        signalView.setGreenAction(action.zh, action.icon);
        signalView.setPhase(SignalView.Phase.GREEN);
        updateStatus((testing ? "测试  " : "Green Light  ") + action.zh);
        speak("Green Light！" + action.speech + "！");
    }

    private void showGreen(LibraryAction action, boolean testing) {
        hideThemeImage();
        signalView.setGreenAction(action.zh, action.icon);
        signalView.setPhase(SignalView.Phase.GREEN);
        updateStatus((testing ? "测试  " : "Green Light  ") + action.zh);
        speak("Green Light！" + action.speech + "！");
    }

    private void showRed(boolean testing) {
        hideThemeImage();
        signalView.setRedScene(random.nextInt(SignalView.RED_SCENE_COUNT));
        signalView.setPhase(SignalView.Phase.RED);
        updateStatus(testing ? "测试停下" : "Red Light  停下");
        speak("Red Light！");
    }

    private void showThemeImage(int imageRes) {
        themeImage.setImageResource(imageRes);
        themeImage.setVisibility(View.VISIBLE);
        dogFace.setVisibility(View.GONE);
    }

    private void hideThemeImage() {
        themeImage.setVisibility(View.GONE);
        dogFace.setVisibility(View.GONE);
    }

    private void showCountdown() {
        signalView.setPhase(SignalView.Phase.HIDDEN);
        showThemeImage(R.drawable.ui_ready);
        mainHandler.postDelayed(() -> showThemeImage(R.drawable.ui_count_3), 700);
        mainHandler.postDelayed(() -> showThemeImage(R.drawable.ui_count_2), 1400);
        mainHandler.postDelayed(() -> showThemeImage(R.drawable.ui_count_1), 2100);
        mainHandler.postDelayed(() -> showThemeImage(R.drawable.ui_go), 3000);
    }

    private int randomPhaseDelayMs() {
        return GREEN_PHASE_MS;
    }

    private void runSpecialActionTest(SpecialAction action) {
        motionToken.incrementAndGet();
        moveStreaming.set(false);
        showGreen(action, true);
        robotCommands.execute(() -> {
            sendSpecialAction(action);
            sendStopBeforeRed();
            mainHandler.post(() -> {
                if (testRunning) {
                    showRed(true);
                }
            });
            sendStopAfterRed();
            recoverAfterSpecial(action);
            mainHandler.post(() -> {
                if (testRunning) {
                    runNextTestAction();
                }
            });
        });
    }

    private void runSpecialGameAction(SpecialAction action) {
        motionToken.incrementAndGet();
        moveStreaming.set(false);
        robotCommands.execute(() -> {
            prepareSpecialAction(action);
            if (!gameRunning) {
                return;
            }
            triggerSpecialAction(action);
            mainHandler.post(() -> {
                if (gameRunning) {
                    showGreen(action, false);
                }
            });
            long redAtMs = System.currentTimeMillis() + Math.max(GREEN_PHASE_MS, action.actionMs);
            long stopAtMs = Math.max(System.currentTimeMillis(), redAtMs - STOP_BEFORE_RED_MS);
            sleepUntil(stopAtMs);
            if (!gameRunning) {
                return;
            }
            sendStopBeforeRed();
            sendStopAfterRed();
            if (hasGatewayFallback()) {
                sendGatewayCommand("halt_move", 0.12);
            }
            recoverAfterSpecial(action);
            waitForRobotReadyForRed(false, 0, RED_READY_TIMEOUT_SPECIAL_MS);
            mainHandler.post(() -> {
                if (gameRunning) {
                    showRed(false);
                    mainHandler.postDelayed(nextGreen, RED_PHASE_MS);
                }
            });
        });
    }

    private void scheduleRedAt(boolean testing, long redAtMs) {
        mainHandler.postDelayed(() -> {
            if (testing ? testRunning : gameRunning) {
                showRed(testing);
            }
        }, delayUntil(redAtMs));
    }

    private long delayUntil(long targetMs) {
        return Math.max(0, targetMs - System.currentTimeMillis());
    }

    private void scheduleNextGreenAfter(long redAtMs) {
        mainHandler.post(() -> {
            if (gameRunning) {
                mainHandler.postDelayed(nextGreen, delayUntil(redAtMs + RED_PHASE_MS));
            }
        });
    }

    private void scheduleNextTestStep() {
        mainHandler.post(() -> {
            if (testRunning) {
                mainHandler.postDelayed(testStep, 1000);
            }
        });
    }

    private void runLibraryGameAction(LibraryAction action) {
        int token = motionToken.incrementAndGet();
        moveStreaming.set(false);
        robotCommands.execute(() -> runLibraryAction(action, false, token, GREEN_PHASE_MS));
    }

    private void runLibraryActionTest(LibraryAction action) {
        int token = motionToken.incrementAndGet();
        moveStreaming.set(false);
        robotCommands.execute(() -> runLibraryAction(action, true, token, action.testDurationMs));
    }

    private void runLibraryAction(LibraryAction action, boolean testing, int token, int phaseMs) {
        if (action.warmupAction != null) {
            prepareBeforeSpecialLibraryAction();
            if (!isLibraryActionActive(testing, token)) {
                return;
            }
            prepareSpecialAction(action.warmupAction);
            if (!isLibraryActionActive(testing, token)) {
                return;
            }
        }

        long redAtMs = System.currentTimeMillis() + (testing ? Math.max(1, phaseMs) : GREEN_PHASE_MS);
        long actionEndMs = testing || action.warmupAction != null
                ? redAtMs
                : Math.max(System.currentTimeMillis(), redAtMs - STOP_BEFORE_RED_MS);
        int index = 0;
        boolean shown = false;
        while (isLibraryActionActive(testing, token) && System.currentTimeMillis() < actionEndMs) {
            LibraryStep step = action.steps[index % action.steps.length];
            if (shown && System.currentTimeMillis() + Math.max(0, step.durationMs) > actionEndMs) {
                break;
            }
            if (!shown && action.warmupAction != null && step.kind == LibraryStep.Kind.CMD) {
                sendLibraryCmdStep(step);
                mainHandler.post(() -> {
                    if (isLibraryActionActive(testing, token)) {
                        showGreen(action, testing);
                    }
                });
                shown = true;
                if (!sleepLibraryStep(step.durationMs, actionEndMs, testing, token)) {
                    break;
                }
            } else {
                if (!shown) {
                    mainHandler.post(() -> {
                        if (isLibraryActionActive(testing, token)) {
                            showGreen(action, testing);
                        }
                    });
                    shown = true;
                }
                if (!performLibraryStep(step, actionEndMs, testing, token)) {
                    break;
                }
            }
            index++;
        }

        if (!isLibraryActionActive(testing, token)) {
            return;
        }
        prepareRobotForRed(action, testing, token);
        if (testing) {
            mainHandler.post(() -> {
                if (isLibraryActionActive(true, token)) {
                    showRed(true);
                }
            });
        } else {
            mainHandler.post(() -> {
                if (isLibraryActionActive(false, token)) {
                    showRed(false);
                }
            });
        }
        if (testing && testRunning && motionToken.get() == token) {
            scheduleNextTestStep();
        } else if (!testing && gameRunning && motionToken.get() == token) {
            mainHandler.post(() -> {
                if (gameRunning) {
                    mainHandler.postDelayed(nextGreen, RED_PHASE_MS);
                }
            });
        }
    }

    private void prepareRobotForRed(LibraryAction action, boolean testing, int token) {
        sendStopBeforeRed();
        sendStopAfterRed();
        if (hasGatewayFallback()) {
            sendGatewayCommand("halt_move", 0.12);
        }
        if (action != null) {
            recoverAfterLibraryAction(action);
        }
        int timeoutMs = action != null && action.warmupAction != null
                ? RED_READY_TIMEOUT_SPECIAL_MS
                : RED_READY_TIMEOUT_NORMAL_MS;
        waitForRobotReadyForRed(testing, token, timeoutMs);
    }

    private void waitForRobotReadyForRed(boolean testing, int token, int timeoutMs) {
        long deadline = System.currentTimeMillis() + Math.max(1000, timeoutMs);
        int polls = 0;
        while (isActionStillActive(testing, token) && System.currentTimeMillis() < deadline) {
            if (isRobotReadyForRed()) {
                return;
            }
            if (polls % 4 == 0) {
                sendDirectStopBurst(4, 20);
            }
            polls++;
            sleepQuietly(250);
        }
        sendDirectStopBurst(10, 20);
        sleepQuietly(300);
    }

    private boolean isActionStillActive(boolean testing, int token) {
        if (token > 0) {
            return isLibraryActionActive(testing, token);
        }
        return testing ? testRunning : gameRunning;
    }

    private boolean isRobotReadyForRed() {
        try {
            JSONObject response = getJson(robotStatusUrl(), 350, 650);
            JSONObject robot = response.optJSONObject("robot");
            return robot != null && robot.optBoolean("ready_for_red", false);
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean performLibraryStep(LibraryStep step, long phaseEndMs, boolean testing, int token) {
        if (step.kind == LibraryStep.Kind.MOVE) {
            return performLibraryMoveStep(step.moveAction, step.durationMs, phaseEndMs, testing, token);
        }
        if (step.kind == LibraryStep.Kind.POSE) {
            return performLibraryPoseStep(step, phaseEndMs, testing, token);
        }
        sendLibraryCmdStep(step);
        return sleepLibraryStep(step.durationMs, phaseEndMs, testing, token);
    }

    private void sendLibraryCmdStep(LibraryStep step) {
        if (step.cmdCode == 5) {
            sendDirectCmd(176, 8, 35);
            sleepQuietly(300);
        }
        sendDirectCmd(step.cmdCode, 10);
        if (hasGatewayFallback() && step.gatewayCmd != null && !step.gatewayCmd.isEmpty()) {
            sendGatewayCommand(step.gatewayCmd, 0.0);
        }
    }

    private boolean performLibraryMoveStep(GameAction action, int durationMs, long phaseEndMs, boolean testing, int token) {
        try {
            InetAddress address = InetAddress.getByName(robotIp);
            DatagramSocket socket = new DatagramSocket();
            try {
                sendDirectCmd(socket, address, usesLowSpeed(action) ? 174 : 175, 6, 30);
                sleepQuietly(160);
                long stepEnd = Math.min(phaseEndMs, System.currentTimeMillis() + Math.max(120, durationMs));
                long lastGatewayMs = 0;
                while (isLibraryActionActive(testing, token) && System.currentTimeMillis() < stepEnd) {
                    sendDirectHeartbeat(socket, address);
                    sendDirectFrame(socket, address, action.lx, action.ly, action.rx, action.ry);
                    if (hasGatewayFallback() && System.currentTimeMillis() - lastGatewayMs > 170) {
                        sendGatewayMovePulse(action);
                        lastGatewayMs = System.currentTimeMillis();
                    }
                    Thread.sleep(35);
                }
            } finally {
                socket.close();
            }
        } catch (Exception ignored) {
            sleepQuietly(120);
        }
        return isLibraryActionActive(testing, token) && System.currentTimeMillis() < phaseEndMs;
    }

    private boolean performLibraryPoseStep(LibraryStep step, long phaseEndMs, boolean testing, int token) {
        try {
            InetAddress address = InetAddress.getByName(robotIp);
            DatagramSocket socket = new DatagramSocket();
            try {
                long stepEnd = Math.min(phaseEndMs, System.currentTimeMillis() + Math.max(120, step.durationMs));
                while (isLibraryActionActive(testing, token) && System.currentTimeMillis() < stepEnd) {
                    sendDirectHeartbeat(socket, address);
                    sendDirectFrame(socket, address, step.lx, step.ly, step.rx, step.ry);
                    Thread.sleep(35);
                }
                for (int i = 0; i < 4; i++) {
                    sendDirectFrame(socket, address, 0.0, 0.0, 0.0, 0.0);
                    Thread.sleep(25);
                }
            } finally {
                socket.close();
            }
        } catch (Exception ignored) {
            sleepQuietly(120);
        }
        return isLibraryActionActive(testing, token) && System.currentTimeMillis() < phaseEndMs;
    }

    private boolean sleepLibraryStep(int durationMs, long phaseEndMs, boolean testing, int token) {
        long end = Math.min(phaseEndMs, System.currentTimeMillis() + Math.max(0, durationMs));
        while (isLibraryActionActive(testing, token) && System.currentTimeMillis() < end) {
            sleepQuietly(Math.min(90, (int) Math.max(1, end - System.currentTimeMillis())));
        }
        return isLibraryActionActive(testing, token) && System.currentTimeMillis() < phaseEndMs;
    }

    private boolean isLibraryActionActive(boolean testing, int token) {
        return motionToken.get() == token && (testing ? testRunning : gameRunning);
    }

    private void recoverAfterLibraryAction(LibraryAction action) {
        if (action.warmupAction != null) {
            forceRecoverToReady();
        } else {
            lockWithoutRecover();
        }
    }

    private void prepareBeforeSpecialLibraryAction() {
        forceRecoverToReady();
    }

    private void lockAfterAction() {
        sendDirectCmd(175, 5);
        sleepQuietly(220);
    }

    private boolean isJumpStyleAction(SpecialAction action) {
        return action == JUMP_UP || action == FRONT_JUMP || action == BACK_FLIP;
    }

    private void beginMoveCommands(GameAction action, double maxSeconds) {
        int token = motionToken.incrementAndGet();
        moveStreaming.set(true);
        robotCommands.execute(() -> directUdpMoveLoop(action, maxSeconds, token));
        if (hasGatewayFallback()) {
            network.execute(() -> httpMovePulseLoop(action, maxSeconds, token));
        }
    }

    private void stopMoveCommands(boolean emergency) {
        motionToken.incrementAndGet();
        moveStreaming.set(false);
        robotCommands.execute(this::sendDirectStopBurst);
        if (hasGatewayFallback()) {
            network.execute(() -> {
                sendGatewayCommand("halt_move", 0.12);
                if (emergency) {
                    sendGatewayCommand("stop", 0.0);
                }
            });
        }
    }

    private void sendStandCommands() {
        robotCommands.execute(() -> {
            sendDirectCmd(122, 5);
            if (hasGatewayFallback()) {
                sendGatewayCommand("stand", 0.0);
            }
        });
    }

    private void sendSpecialAction(SpecialAction action) {
        prepareSpecialAction(action);
        triggerSpecialAction(action);
        sleepQuietly(action.actionMs);
    }

    private void prepareSpecialAction(SpecialAction action) {
        sendDirectStopBurst();
        sleepQuietly(120);
        if (action.recoverBefore) {
            sendDirectCmd(155, 5);
            sleepQuietly(1200);
        }
        if (action.prepStand) {
            sendDirectStopBurst();
            sleepQuietly(350);
            sendDirectCmd(122, 5);
            sleepQuietly(2800);
        }
        if (action.hybridPreamble) {
            sendRemoteHybridPreamble();
        }
        if (action.turboFirst) {
            sendDirectCmd(176, 5);
            sleepQuietly(450);
        }
        if (action.buttonIndex >= 0) {
            sendDirectButton(action.buttonIndex, action.buttonHoldMs);
            sleepQuietly(100);
        }
    }

    private void triggerSpecialAction(SpecialAction action) {
        sendDirectCmd(action.cmdCode, 10);
        if (hasGatewayFallback()) {
            sendGatewayCommand(action.gatewayCmd, 0.0);
        }
    }

    private void sendRecoverAction(SpecialAction action) {
        if (action.recoverCmdCode <= 0) {
            return;
        }
        sendDirectCmd(action.recoverCmdCode, 6);
        if (hasGatewayFallback()) {
            sendGatewayCommand(action.recoverGatewayCmd, 0.0);
        }
    }

    private void recoverAfterSpecial(SpecialAction action) {
        sendRecoverAction(action);
        sleepQuietly(action.recoverMs);
        if (!needsPostSpecialStand(action)) {
            return;
        }
        sendDirectDebugSpeed(-1, 5, 0);
        sleepQuietly(120);
        standAndLockAfterAction();
    }

    private boolean needsPostSpecialStand(SpecialAction action) {
        return action == WAVE || action == TWO_LEG_STAND || isJumpStyleAction(action);
    }

    private void recoverAfterManualStop() {
        forceRecoverToReady();
    }

    private void standAndLockAfterAction() {
        forceRecoverToReady();
    }

    private void forceRecoverToReady() {
        sendDirectStopBurst(14, 20);
        sleepQuietly(120);
        sendDirectCmd(155, 6, 30);
        sleepQuietly(900);
        sendDirectCmd(122, 8, 30);
        sleepQuietly(1800);
        sendDirectCmd(175, 6, 30);
        sleepQuietly(180);
        sendDirectStopBurst(4, 20);
    }

    private void lockWithoutRecover() {
        sendDirectStopBurst(12, 20);
        sleepQuietly(100);
        sendDirectCmd(175, 4, 30);
        sleepQuietly(120);
        sendDirectStopBurst(6, 20);
    }

    private void directUdpMoveLoop(GameAction action, double maxSeconds, int token) {
        try {
            InetAddress address = InetAddress.getByName(robotIp);
            DatagramSocket socket = new DatagramSocket();
            try {
                if (usesLowSpeed(action)) {
                    sendDirectCmd(socket, address, 174, 5, 30);
                } else {
                    sendDirectCmd(socket, address, 175, 5, 30);
                }
                sleepQuietly(180);
                long end = System.currentTimeMillis() + (long) (maxSeconds * 1000.0);
                while (moveStreaming.get() && motionToken.get() == token && System.currentTimeMillis() < end) {
                    try {
                        sendDirectHeartbeat(socket, address);
                        sendDirectFrame(socket, address, action.lx, action.ly, action.rx, action.ry);
                        Thread.sleep(35);
                    } catch (Exception ignored) {
                        sleepQuietly(120);
                    }
                }
                if (motionToken.get() == token) {
                    sendDirectStopBurst(socket, address);
                }
            } finally {
                socket.close();
            }
        } catch (Exception ignored) {
        }
    }

    private void httpMovePulseLoop(GameAction action, double maxSeconds, int token) {
        long end = System.currentTimeMillis() + (long) (maxSeconds * 1000.0);
        while (moveStreaming.get() && motionToken.get() == token && System.currentTimeMillis() < end) {
            sendGatewayMovePulse(action);
            sleepQuietly(150);
        }
    }

    private void sendDirectHeartbeat() throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("type", "heartbeat");
        sendUdpJson(payload);
    }

    private void startHeartbeatLoop() {
        heartbeat.scheduleWithFixedDelay(() -> {
            try {
                sendDirectHeartbeat();
            } catch (Exception ignored) {
            }
        }, 0, 250, TimeUnit.MILLISECONDS);
    }

    private void sendDirectHeartbeat(DatagramSocket socket, InetAddress address) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("type", "heartbeat");
        sendUdpJson(socket, address, payload);
    }

    private void sendRemoteHybridPreamble() {
        sendDirectHeartbeatStream(4400, 28.0);
        sendDirectDebugSpeed(-1, 5, 0);
        sendDirectHeartbeatStream(1400, 28.0);
        sendDirectNeutralBurst(10, 0);
        sendDirectHeartbeatStream(2300, 28.0);
        sendDirectCmd(193, 5, 0);
        sendDirectHeartbeatStream(450, 28.0);
        sendDirectDebugSpeed(-1, 5, 0);
        sendDirectHeartbeatStream(2800, 28.0);
    }

    private void sendDirectHeartbeatStream(int durationMs, double rate) {
        long end = System.currentTimeMillis() + Math.max(0, durationMs);
        long periodMs = Math.max(16L, Math.round(1000.0 / Math.max(1.0, rate)));
        while (System.currentTimeMillis() < end) {
            try {
                sendDirectHeartbeat();
                Thread.sleep(periodMs);
            } catch (Exception ignored) {
                sleepQuietly(periodMs);
            }
        }
    }

    private void sendDirectDebugSpeed(int speedValue, int repeat, int gapMs) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("speed_value", speedValue);
            payload.put("type", "debug_speed");
            for (int i = 0; i < Math.max(1, repeat); i++) {
                sendUdpJson(payload);
                if (gapMs > 0) {
                    Thread.sleep(gapMs);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void sendDirectNeutralBurst(int repeat, int gapMs) {
        try {
            for (int i = 0; i < Math.max(1, repeat); i++) {
                sendDirectFrame(0.0, 0.0, 0.0, 0.0);
                if (gapMs > 0) {
                    Thread.sleep(gapMs);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void sendDirectCmd(int cmd, int repeat) {
        sendDirectCmd(cmd, repeat, 30);
    }

    private void sendDirectCmd(int cmd, int repeat, int gapMs) {
        try {
            sendDirectHeartbeat();
            JSONObject payload = new JSONObject();
            payload.put("cmd", cmd);
            payload.put("type", "cmd");
            for (int i = 0; i < Math.max(1, repeat); i++) {
                sendUdpJson(payload);
                if (gapMs > 0) {
                    Thread.sleep(gapMs);
                }
            }
            sendDirectHeartbeat();
        } catch (Exception ignored) {
        }
    }

    private void sendDirectCmd(DatagramSocket socket, InetAddress address, int cmd, int repeat, int gapMs) throws Exception {
        sendDirectHeartbeat(socket, address);
        JSONObject payload = new JSONObject();
        payload.put("cmd", cmd);
        payload.put("type", "cmd");
        for (int i = 0; i < Math.max(1, repeat); i++) {
            sendUdpJson(socket, address, payload);
            if (gapMs > 0) {
                Thread.sleep(gapMs);
            }
        }
        sendDirectHeartbeat(socket, address);
    }

    private void sendStopBeforeRed() {
        sendDirectStopBurst(18, 25);
        sleepQuietly(120);
    }

    private void sendStopAfterRed() {
        sendDirectStopBurst(10, 25);
    }

    private void sendDirectStopBurst() {
        sendDirectStopBurst(10, 25);
    }

    private void sendDirectStopBurst(int repeat, int gapMs) {
        try {
            for (int i = 0; i < Math.max(1, repeat); i++) {
                sendDirectFrame(0.0, 0.0, 0.0, 0.0);
                if (gapMs > 0) {
                    Thread.sleep(gapMs);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void sendDirectStopBurst(DatagramSocket socket, InetAddress address) throws Exception {
        for (int i = 0; i < 10; i++) {
            sendDirectFrame(socket, address, 0.0, 0.0, 0.0, 0.0);
            Thread.sleep(25);
        }
    }

    private void sendDirectButton(int buttonIndex, int holdMs) {
        if (buttonIndex < 0 || buttonIndex >= 14) {
            return;
        }
        long end = System.currentTimeMillis() + Math.max(80, holdMs);
        try {
            while (System.currentTimeMillis() < end) {
                sendDirectFrame(0.0, 0.0, 0.0, 0.0, buttonIndex);
                Thread.sleep(25);
            }
            for (int i = 0; i < 8; i++) {
                sendDirectFrame(0.0, 0.0, 0.0, 0.0);
                Thread.sleep(25);
            }
        } catch (Exception ignored) {
        }
    }

    private void sendDirectFrame(double lx, double ly, double rx, double ry) throws Exception {
        sendDirectFrame(lx, ly, rx, ry, -1);
    }

    private void sendDirectFrame(double lx, double ly, double rx, double ry, int activeButton) throws Exception {
        InetAddress address = InetAddress.getByName(robotIp);
        DatagramSocket socket = new DatagramSocket();
        try {
            sendDirectFrame(socket, address, lx, ly, rx, ry, activeButton);
        } finally {
            socket.close();
        }
    }

    private void sendDirectFrame(DatagramSocket socket, InetAddress address, double lx, double ly, double rx, double ry) throws Exception {
        sendDirectFrame(socket, address, lx, ly, rx, ry, -1);
    }

    private void sendDirectFrame(
            DatagramSocket socket,
            InetAddress address,
            double lx,
            double ly,
            double rx,
            double ry,
            int activeButton
    ) throws Exception {
        JSONObject payload = new JSONObject();
        JSONArray buttons = new JSONArray();
        for (int i = 0; i < 14; i++) {
            buttons.put(i == activeButton ? 1 : 0);
        }
        JSONArray joystick = new JSONArray();
        joystick.put(lx);
        joystick.put(ly);
        joystick.put(rx);
        joystick.put(ry);
        payload.put("button", buttons);
        payload.put("joystick", joystick);
        payload.put("seq", udpSeq.incrementAndGet());
        payload.put("time", System.currentTimeMillis());
        payload.put("type", "remote");
        sendUdpJson(socket, address, payload);
    }

    private void sendUdpJson(JSONObject payload) throws Exception {
        InetAddress address = InetAddress.getByName(robotIp);
        DatagramSocket socket = new DatagramSocket();
        try {
            sendUdpJson(socket, address, payload);
        } finally {
            socket.close();
        }
    }

    private void sendUdpJson(DatagramSocket socket, InetAddress address, JSONObject payload) throws Exception {
        byte[] data = payload.toString().getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(data, data.length, address, udpPort);
        socket.send(packet);
    }

    private void sendGatewayMovePulse(GameAction action) {
        if (!hasGatewayFallback()) {
            return;
        }
        try {
            JSONObject payload = baseGatewayPayload("move");
            payload.put("vx", action.vx);
            payload.put("vy", action.vy);
            payload.put("yaw", action.yaw);
            payload.put("duration", 0.24);
            if (usesLowSpeed(action)) {
                payload.put("gear_cmd", 174);
                payload.put("gear_wait", 0.08);
            }
            payload.put("warmup", 0.0);
            payload.put("ramp", 0.04);
            payload.put("neutral_end", 0.0);
            payload.put("post_wait", 0.0);
            postJson(gatewayCmdUrl(), payload.toString(), 500, 700);
        } catch (Exception ignored) {
        }
    }

    private void sendGatewayCommand(String cmd, double duration) {
        if (!hasGatewayFallback()) {
            return;
        }
        try {
            JSONObject payload = baseGatewayPayload(cmd);
            if (duration > 0) {
                payload.put("duration", duration);
            }
            postJson(gatewayCmdUrl(), payload.toString(), 500, 900);
        } catch (Exception ignored) {
        }
    }

    private JSONObject baseGatewayPayload(String cmd) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("source", "android-red-light-green-light");
        payload.put("backend", "remote");
        payload.put("cmd", cmd);
        payload.put("lease_sec", 1.0);
        payload.put("force", true);
        return payload;
    }

    private String gatewayCmdUrl() {
        String base = gatewayUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/cmd";
    }

    private boolean hasGatewayFallback() {
        String value = gatewayUrl == null ? "" : gatewayUrl.trim();
        return value.startsWith("http://") || value.startsWith("https://");
    }

    private boolean usesLowSpeed(GameAction action) {
        return action == FORWARD
                || action == BACKWARD;
    }

    private String postJson(String target, String body, int connectTimeoutMs, int readTimeoutMs) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(target).openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(connectTimeoutMs);
        conn.setReadTimeout(readTimeoutMs);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        return readResponse(conn);
    }

    private JSONObject getJson(String target, int connectTimeoutMs, int readTimeoutMs) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(target).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(connectTimeoutMs);
        conn.setReadTimeout(readTimeoutMs);
        return new JSONObject(readResponse(conn));
    }

    private void configureRobotInternet(String ssid, String password, boolean openNetwork) {
        String trimmedSsid = ssid == null ? "" : ssid.trim();
        if (trimmedSsid.isEmpty()) {
            Toast.makeText(this, "请输入外部 Wi-Fi 名称", Toast.LENGTH_SHORT).show();
            return;
        }
        updateStatus("正在配置机器狗外网 Wi-Fi...");
        network.execute(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("source", "android-red-light-green-light");
                payload.put("ssid", trimmedSsid);
                payload.put("password", openNetwork ? "" : password);
                payload.put("open", openNetwork);
                payload.put("share_hotspot", true);
                String response = postJson(networkConfigureUrl(), payload.toString(), 5000, 65000);
                mainHandler.post(() -> {
                    updateStatus("外网配置已发送");
                    Toast.makeText(this, response, Toast.LENGTH_LONG).show();
                });
            } catch (Exception ex) {
                mainHandler.post(() -> {
                    updateStatus("外网配置失败");
                    Toast.makeText(this, "配置失败: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private String networkConfigureUrl() {
        return networkBaseUrl() + "/network/configure";
    }

    private String robotStatusUrl() {
        return networkBaseUrl() + "/robot/status";
    }

    private String networkBaseUrl() {
        String base = networkServiceUrl == null ? "" : networkServiceUrl.trim();
        if (base.isEmpty()) {
            base = defaultNetworkServiceUrl(robotIp);
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }

    private String defaultNetworkServiceUrl(String ip) {
        String host = ip == null || ip.trim().isEmpty() ? DEFAULT_ROBOT_IP : ip.trim();
        return "http://" + host + ":8876";
    }

    private String readResponse(HttpURLConnection conn) throws Exception {
        int code = conn.getResponseCode();
        InputStream in = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
        if (in == null) {
            return "HTTP " + code;
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private void showSettingsDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(18), dp(8), dp(18), 0);

        EditText ip = makeInput("Robot IP", robotIp);
        form.addView(ip);

        EditText port = makeInput("UDP Port", String.valueOf(udpPort));
        port.setInputType(InputType.TYPE_CLASS_NUMBER);
        form.addView(port);

        EditText gateway = makeInput("Gateway URL（可选，默认不用）", gatewayUrl);
        form.addView(gateway);

        EditText networkUrl = makeInput("Pad 上网服务 URL", networkServiceUrl);
        form.addView(networkUrl);

        TextView networkTitle = new TextView(this);
        networkTitle.setText("机器狗外部 Wi-Fi / Pad 上网");
        networkTitle.setTextSize(16);
        networkTitle.setPadding(0, dp(14), 0, 0);
        form.addView(networkTitle);

        EditText ssid = makeInput("外部 Wi-Fi 名称 SSID", "");
        form.addView(ssid);

        EditText password = makeInput("外部 Wi-Fi 密码", "");
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        form.addView(password);

        CheckBox openNetwork = new CheckBox(this);
        openNetwork.setText("开放 Wi-Fi（无密码）");
        form.addView(openNetwork);

        Button configureNetwork = makeButton("连接外部 Wi-Fi 并开启 Pad 上网", Color.rgb(92, 83, 168));
        form.addView(configureNetwork, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(50)
        ));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("ZsiBot 连接设置")
                .setView(form)
                .setPositiveButton("保存", (saveDialog, which) -> {
                    int parsedPort = DEFAULT_UDP_PORT;
                    try {
                        parsedPort = Integer.parseInt(port.getText().toString().trim());
                    } catch (Exception ignored) {
                    }
                    saveSettings(
                            ip.getText().toString(),
                            parsedPort,
                            gateway.getText().toString(),
                            networkUrl.getText().toString()
                    );
                })
                .setNegativeButton("取消", null)
                .show();

        configureNetwork.setOnClickListener(v -> {
            int parsedPort = DEFAULT_UDP_PORT;
            try {
                parsedPort = Integer.parseInt(port.getText().toString().trim());
            } catch (Exception ignored) {
            }
            saveSettings(
                    ip.getText().toString(),
                    parsedPort,
                    gateway.getText().toString(),
                    networkUrl.getText().toString()
            );
            configureRobotInternet(
                    ssid.getText().toString(),
                    password.getText().toString(),
                    openNetwork.isChecked()
            );
            dialog.dismiss();
        });
    }

    private EditText makeInput(String hint, String value) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setSingleLine(true);
        input.setText(value);
        input.setTextSize(16);
        return input;
    }

    private void speak(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "zsibot-rlgl");
        }
    }

    private void updateStatus(String text) {
        statusText.setText(text);
    }

    private void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private void sleepUntil(long targetMs) {
        while (System.currentTimeMillis() < targetMs) {
            sleepQuietly(Math.min(100, targetMs - System.currentTimeMillis()));
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static class LibraryAction {
        final String zh;
        final String speech;
        final SignalView.Icon icon;
        final SpecialAction warmupAction;
        final LibraryStep[] steps;
        final int testDurationMs;

        LibraryAction(
                String zh,
                String speech,
                SignalView.Icon icon,
                SpecialAction warmupAction,
                LibraryStep[] steps,
                int testDurationMs
        ) {
            this.zh = zh;
            this.speech = speech;
            this.icon = icon;
            this.warmupAction = warmupAction;
            this.steps = steps;
            this.testDurationMs = testDurationMs;
        }
    }

    private static class LibraryStep {
        enum Kind {
            MOVE,
            CMD,
            POSE
        }

        final Kind kind;
        final GameAction moveAction;
        final int cmdCode;
        final String gatewayCmd;
        final int durationMs;
        final double lx;
        final double ly;
        final double rx;
        final double ry;

        private LibraryStep(
                Kind kind,
                GameAction moveAction,
                int cmdCode,
                String gatewayCmd,
                int durationMs,
                double lx,
                double ly,
                double rx,
                double ry
        ) {
            this.kind = kind;
            this.moveAction = moveAction;
            this.cmdCode = cmdCode;
            this.gatewayCmd = gatewayCmd;
            this.durationMs = durationMs;
            this.lx = lx;
            this.ly = ly;
            this.rx = rx;
            this.ry = ry;
        }

        static LibraryStep move(GameAction action, int durationMs) {
            return new LibraryStep(Kind.MOVE, action, 0, "", durationMs, 0.0, 0.0, 0.0, 0.0);
        }

        static LibraryStep cmd(int cmdCode, String gatewayCmd, int durationMs) {
            return new LibraryStep(Kind.CMD, null, cmdCode, gatewayCmd, durationMs, 0.0, 0.0, 0.0, 0.0);
        }

        static LibraryStep pose(double lx, double ly, double rx, double ry, int durationMs) {
            return new LibraryStep(Kind.POSE, null, 0, "", durationMs, lx, ly, rx, ry);
        }
    }

    private static class GameAction {
        final String zh;
        final String english;
        final String speech;
        final SignalView.Icon icon;
        final double lx;
        final double ly;
        final double rx;
        final double ry;
        final double vx;
        final double vy;
        final double yaw;

        GameAction(
                String zh,
                String english,
                String speech,
                SignalView.Icon icon,
                double lx,
                double ly,
                double rx,
                double ry,
                double vx,
                double vy,
                double yaw
        ) {
            this.zh = zh;
            this.english = english;
            this.speech = speech;
            this.icon = icon;
            this.lx = lx;
            this.ly = ly;
            this.rx = rx;
            this.ry = ry;
            this.vx = vx;
            this.vy = vy;
            this.yaw = yaw;
        }
    }

    private static class SpecialAction {
        final String zh;
        final String english;
        final String speech;
        final SignalView.Icon icon;
        final int cmdCode;
        final String gatewayCmd;
        final int actionMs;
        final int recoverCmdCode;
        final String recoverGatewayCmd;
        final int recoverMs;
        final boolean hybridPreamble;
        final boolean prepStand;
        final boolean recoverBefore;
        final boolean turboFirst;
        final int buttonIndex;
        final int buttonHoldMs;

        SpecialAction(
                String zh,
                String english,
                String speech,
                SignalView.Icon icon,
                int cmdCode,
                String gatewayCmd,
                int actionMs,
                int recoverCmdCode,
                String recoverGatewayCmd,
                int recoverMs,
                boolean hybridPreamble,
                boolean prepStand,
                boolean recoverBefore,
                boolean turboFirst,
                int buttonIndex,
                int buttonHoldMs
        ) {
            this.zh = zh;
            this.english = english;
            this.speech = speech;
            this.icon = icon;
            this.cmdCode = cmdCode;
            this.gatewayCmd = gatewayCmd;
            this.actionMs = actionMs;
            this.recoverCmdCode = recoverCmdCode;
            this.recoverGatewayCmd = recoverGatewayCmd;
            this.recoverMs = recoverMs;
            this.hybridPreamble = hybridPreamble;
            this.prepStand = prepStand;
            this.recoverBefore = recoverBefore;
            this.turboFirst = turboFirst;
            this.buttonIndex = buttonIndex;
            this.buttonHoldMs = buttonHoldMs;
        }
    }
}
