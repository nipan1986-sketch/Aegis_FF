package com.matrix.zsibotrlgl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

public class SignalView extends View {
    public static final int RED_SCENE_COUNT = 7;

    public enum Phase {
        HIDDEN,
        GREEN,
        RED
    }

    public enum Icon {
        FORWARD,
        BACKWARD,
        LEFT,
        RIGHT,
        TURN_LEFT,
        TURN_RIGHT,
        JUMP_UP,
        STAY
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final int[] redSceneResources = {
            R.drawable.ui_red_frog,
            R.drawable.ui_red_kangaroo,
            R.drawable.ui_red_blank_pink,
            R.drawable.ui_red_bunny,
            R.drawable.ui_red_robot,
            R.drawable.ui_red_dog,
            R.drawable.ui_red_bunny_scene
    };
    private Bitmap[] redScenes;
    private Phase phase = Phase.HIDDEN;
    private Icon greenIcon = Icon.FORWARD;
    private String greenText = "往前走";
    private int redSceneIndex = 0;
    private long animationStartMs = SystemClock.uptimeMillis();

    public SignalView(Context context) {
        super(context);
        init();
    }

    public SignalView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        redScenes = new Bitmap[redSceneResources.length];
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
    }

    public void setPhase(Phase newPhase) {
        if (phase != newPhase && newPhase == Phase.GREEN) {
            animationStartMs = SystemClock.uptimeMillis();
        }
        phase = newPhase;
        setVisibility(newPhase == Phase.HIDDEN ? GONE : VISIBLE);
        invalidate();
    }

    public void setGreenAction(String text, Icon icon) {
        greenText = text == null || text.trim().isEmpty() ? "往前走" : text;
        greenIcon = icon == null ? Icon.FORWARD : icon;
        invalidate();
    }

    public void setRedScene(int index) {
        redSceneIndex = Math.abs(index) % redSceneResources.length;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (phase == Phase.HIDDEN) {
            return;
        }
        float w = getWidth();
        float h = getHeight();
        if (phase == Phase.GREEN) {
            drawGreen(canvas, w, h);
            postInvalidateDelayed(33);
        } else {
            drawRed(canvas, w, h);
        }
    }

    private void drawGreen(Canvas canvas, float w, float h) {
        float unit = Math.min(w, h);
        drawPlaygroundBackground(canvas, w, h, unit);
        drawTrafficLight(canvas, w * 0.14f, h * 0.48f, unit, false);
        drawOutlinedText(canvas, "Green Light", w * 0.5f, h * 0.17f, unit * 0.115f, Color.rgb(83, 205, 73));

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(235, 255, 255, 255));
        rect.set(w * 0.31f, h * 0.25f, w * 0.86f, h * 0.78f);
        canvas.drawRoundRect(rect, unit * 0.035f, unit * 0.035f, paint);
        paint.setColor(Color.rgb(255, 211, 77));
        rect.set(w * 0.335f, h * 0.285f, w * 0.835f, h * 0.735f);
        canvas.drawRoundRect(rect, unit * 0.025f, unit * 0.025f, paint);
        paint.setColor(Color.rgb(255, 244, 177));
        rect.set(w * 0.355f, h * 0.31f, w * 0.815f, h * 0.71f);
        canvas.drawRoundRect(rect, unit * 0.022f, unit * 0.022f, paint);

        float cx = w * 0.585f;
        float cy = h * 0.50f;
        stroke.setColor(Color.WHITE);
        stroke.setStrokeWidth(unit * 0.052f);
        drawActionAnimation(canvas, cx, cy, unit);

        drawOutlinedText(canvas, greenText, w * 0.585f, h * 0.83f, unit * 0.085f, Color.rgb(255, 235, 84));
    }

    private void drawActionAnimation(Canvas canvas, float cx, float cy, float unit) {
        float t = (SystemClock.uptimeMillis() - animationStartMs) / 1000f;
        String text = greenText == null ? "" : greenText;
        if (text.contains("青蛙")) {
            drawFrog(canvas, cx, cy, unit, t);
        } else if (text.contains("兔子")) {
            drawRabbit(canvas, cx, cy, unit, t, false);
        } else if (text.contains("袋鼠")) {
            drawKangaroo(canvas, cx, cy, unit, t);
        } else if (text.contains("演员")) {
            drawRunner(canvas, cx + (float) Math.sin(t * 3.0f) * unit * 0.16f, cy, unit, t, false);
        } else if (text.contains("巡逻")) {
            canvas.save();
            canvas.rotate((float) Math.sin(t * 3.2f) * 23f, cx, cy + unit * 0.04f);
            drawDog(canvas, cx, cy, unit, t);
            canvas.restore();
        } else if (text.contains("机器人")) {
            drawRobot(canvas, cx + (float) Math.sin(t * 3.0f) * unit * 0.14f, cy, unit, t);
        } else if (text.contains("猩猩")) {
            drawGorilla(canvas, cx, cy, unit, t);
        } else if (text.contains("舞者")) {
            canvas.save();
            canvas.rotate((float) Math.sin(t * 4.0f) * 15f, cx, cy - unit * 0.06f);
            drawRunner(canvas, cx, cy, unit, t, true);
            canvas.restore();
        } else if (text.contains("运动员") || text.contains("空翻")) {
            canvas.save();
            canvas.rotate((t * 360f) % 360f, cx, cy);
            drawRunner(canvas, cx, cy, unit, t, true);
            canvas.restore();
        } else if (greenIcon == Icon.JUMP_UP) {
            drawRabbit(canvas, cx, cy, unit, t, true);
        } else if (greenIcon == Icon.STAY) {
            drawRunner(canvas, cx, cy, unit, t, true);
        } else {
            float dir = (greenIcon == Icon.LEFT || greenIcon == Icon.BACKWARD) ? -1f : 1f;
            if (greenIcon == Icon.TURN_LEFT || greenIcon == Icon.TURN_RIGHT) {
                canvas.save();
                canvas.rotate((greenIcon == Icon.TURN_LEFT ? -1f : 1f) * ((t * 220f) % 360f), cx, cy);
                drawRunner(canvas, cx, cy, unit, t, false);
                canvas.restore();
            } else {
                drawRunner(canvas, cx + (float) Math.sin(t * 4.0f) * unit * 0.12f * dir, cy, unit, t, false);
            }
        }
    }

    private void drawRunner(Canvas canvas, float cx, float cy, float unit, float t, boolean dance) {
        float bob = (float) Math.sin(t * 7f) * unit * 0.018f;
        float swing = (float) Math.sin(t * 7f) * unit * 0.09f;
        float headR = unit * 0.07f;
        float bodyW = unit * 0.12f;
        float bodyH = unit * 0.18f;
        float bodyTop = cy - unit * 0.05f + bob;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(255, 190, 128));
        canvas.drawCircle(cx, bodyTop - unit * 0.095f, headR, paint);
        paint.setColor(Color.rgb(92, 54, 32));
        rect.set(cx - headR * 0.9f, bodyTop - unit * 0.16f, cx + headR * 0.9f, bodyTop - unit * 0.08f);
        canvas.drawArc(rect, 180, 180, true, paint);
        paint.setColor(dance ? Color.rgb(255, 107, 153) : Color.rgb(73, 171, 92));
        rect.set(cx - bodyW * 0.5f, bodyTop, cx + bodyW * 0.5f, bodyTop + bodyH);
        canvas.drawRoundRect(rect, unit * 0.035f, unit * 0.035f, paint);
        stroke.setColor(Color.rgb(70, 55, 55));
        stroke.setStrokeWidth(unit * 0.022f);
        float shoulderY = bodyTop + unit * 0.04f;
        float hipY = bodyTop + bodyH;
        canvas.drawLine(cx - bodyW * 0.42f, shoulderY, cx - bodyW * 0.72f, shoulderY + swing, stroke);
        canvas.drawLine(cx + bodyW * 0.42f, shoulderY, cx + bodyW * 0.72f, shoulderY - swing, stroke);
        canvas.drawLine(cx - bodyW * 0.25f, hipY, cx - bodyW * 0.45f, hipY + unit * 0.18f - swing, stroke);
        canvas.drawLine(cx + bodyW * 0.25f, hipY, cx + bodyW * 0.45f, hipY + unit * 0.18f + swing, stroke);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(cx - unit * 0.025f, bodyTop - unit * 0.10f, unit * 0.012f, paint);
        canvas.drawCircle(cx + unit * 0.025f, bodyTop - unit * 0.10f, unit * 0.012f, paint);
    }

    private void drawFrog(Canvas canvas, float cx, float cy, float unit, float t) {
        float squat = (1f + (float) Math.sin(t * 5f)) * 0.5f;
        float bodyH = unit * (0.18f - squat * 0.055f);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(92, 211, 66));
        canvas.drawCircle(cx, cy - unit * 0.03f + squat * unit * 0.045f, unit * 0.14f, paint);
        paint.setColor(Color.rgb(186, 246, 105));
        rect.set(cx - unit * 0.08f, cy - unit * 0.02f, cx + unit * 0.08f, cy + bodyH);
        canvas.drawOval(rect, paint);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(cx - unit * 0.07f, cy - unit * 0.17f, unit * 0.045f, paint);
        canvas.drawCircle(cx + unit * 0.07f, cy - unit * 0.17f, unit * 0.045f, paint);
        paint.setColor(Color.rgb(25, 36, 25));
        canvas.drawCircle(cx - unit * 0.07f, cy - unit * 0.17f, unit * 0.022f, paint);
        canvas.drawCircle(cx + unit * 0.07f, cy - unit * 0.17f, unit * 0.022f, paint);
        stroke.setColor(Color.rgb(43, 135, 43));
        stroke.setStrokeWidth(unit * 0.025f);
        canvas.drawLine(cx - unit * 0.08f, cy + unit * 0.09f, cx - unit * 0.21f, cy + unit * 0.19f + squat * unit * 0.06f, stroke);
        canvas.drawLine(cx + unit * 0.08f, cy + unit * 0.09f, cx + unit * 0.21f, cy + unit * 0.19f + squat * unit * 0.06f, stroke);
    }

    private void drawRabbit(Canvas canvas, float cx, float cy, float unit, float t, boolean jumpHigh) {
        float hop = Math.max(0f, (float) Math.sin(t * 5.5f)) * unit * (jumpHigh ? 0.18f : 0.12f);
        cy -= hop;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        rect.set(cx - unit * 0.04f, cy - unit * 0.26f, cx + unit * 0.005f, cy - unit * 0.08f);
        canvas.drawOval(rect, paint);
        rect.set(cx + unit * 0.005f, cy - unit * 0.26f, cx + unit * 0.05f, cy - unit * 0.08f);
        canvas.drawOval(rect, paint);
        canvas.drawCircle(cx, cy - unit * 0.08f, unit * 0.095f, paint);
        canvas.drawOval(cx - unit * 0.11f, cy, cx + unit * 0.11f, cy + unit * 0.20f, paint);
        paint.setColor(Color.rgb(255, 139, 174));
        canvas.drawCircle(cx + unit * 0.035f, cy - unit * 0.075f, unit * 0.012f, paint);
        stroke.setColor(Color.rgb(90, 90, 90));
        stroke.setStrokeWidth(unit * 0.02f);
        canvas.drawLine(cx - unit * 0.05f, cy + unit * 0.16f, cx - unit * 0.14f, cy + unit * 0.26f, stroke);
        canvas.drawLine(cx + unit * 0.05f, cy + unit * 0.16f, cx + unit * 0.14f, cy + unit * 0.26f, stroke);
    }

    private void drawKangaroo(Canvas canvas, float cx, float cy, float unit, float t) {
        float leap = (float) Math.sin(t * 4.5f) * unit * 0.14f;
        cx += leap;
        cy -= Math.max(0f, (float) Math.sin(t * 4.5f)) * unit * 0.10f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(216, 142, 70));
        canvas.drawOval(cx - unit * 0.10f, cy - unit * 0.05f, cx + unit * 0.11f, cy + unit * 0.20f, paint);
        canvas.drawCircle(cx + unit * 0.06f, cy - unit * 0.12f, unit * 0.07f, paint);
        stroke.setColor(Color.rgb(145, 82, 39));
        stroke.setStrokeWidth(unit * 0.026f);
        canvas.drawLine(cx - unit * 0.09f, cy + unit * 0.11f, cx - unit * 0.24f, cy + unit * 0.20f, stroke);
        canvas.drawLine(cx + unit * 0.05f, cy + unit * 0.15f, cx + unit * 0.19f, cy + unit * 0.25f, stroke);
        canvas.drawLine(cx - unit * 0.10f, cy + unit * 0.02f, cx - unit * 0.28f, cy - unit * 0.07f, stroke);
    }

    private void drawDog(Canvas canvas, float cx, float cy, float unit, float t) {
        float wag = (float) Math.sin(t * 9f) * unit * 0.04f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(135, 146, 156));
        canvas.drawRoundRect(cx - unit * 0.16f, cy - unit * 0.02f, cx + unit * 0.16f, cy + unit * 0.12f, unit * 0.05f, unit * 0.05f, paint);
        canvas.drawCircle(cx + unit * 0.20f, cy - unit * 0.03f, unit * 0.07f, paint);
        stroke.setColor(Color.rgb(80, 88, 98));
        stroke.setStrokeWidth(unit * 0.025f);
        canvas.drawLine(cx - unit * 0.14f, cy + unit * 0.10f, cx - unit * 0.16f, cy + unit * 0.24f, stroke);
        canvas.drawLine(cx - unit * 0.03f, cy + unit * 0.11f, cx - unit * 0.01f, cy + unit * 0.24f, stroke);
        canvas.drawLine(cx + unit * 0.08f, cy + unit * 0.11f, cx + unit * 0.10f, cy + unit * 0.24f, stroke);
        canvas.drawLine(cx + unit * 0.15f, cy + unit * 0.11f, cx + unit * 0.17f, cy + unit * 0.24f, stroke);
        canvas.drawLine(cx - unit * 0.17f, cy, cx - unit * 0.28f, cy - unit * 0.08f + wag, stroke);
    }

    private void drawRobot(Canvas canvas, float cx, float cy, float unit, float t) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(82, 181, 220));
        rect.set(cx - unit * 0.11f, cy - unit * 0.18f, cx + unit * 0.11f, cy + unit * 0.03f);
        canvas.drawRoundRect(rect, unit * 0.025f, unit * 0.025f, paint);
        paint.setColor(Color.rgb(39, 100, 140));
        rect.set(cx - unit * 0.13f, cy + unit * 0.03f, cx + unit * 0.13f, cy + unit * 0.22f);
        canvas.drawRoundRect(rect, unit * 0.025f, unit * 0.025f, paint);
        paint.setColor(Color.rgb(255, 235, 84));
        canvas.drawCircle(cx - unit * 0.045f, cy - unit * 0.09f, unit * 0.018f, paint);
        canvas.drawCircle(cx + unit * 0.045f, cy - unit * 0.09f, unit * 0.018f, paint);
        stroke.setColor(Color.rgb(39, 100, 140));
        stroke.setStrokeWidth(unit * 0.024f);
        float step = (float) Math.sin(t * 6f) * unit * 0.06f;
        canvas.drawLine(cx - unit * 0.06f, cy + unit * 0.22f, cx - unit * 0.10f, cy + unit * 0.32f + step, stroke);
        canvas.drawLine(cx + unit * 0.06f, cy + unit * 0.22f, cx + unit * 0.10f, cy + unit * 0.32f - step, stroke);
    }

    private void drawGorilla(Canvas canvas, float cx, float cy, float unit, float t) {
        float sway = (float) Math.sin(t * 4f) * unit * 0.025f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(93, 62, 45));
        canvas.drawCircle(cx, cy - unit * 0.13f + sway, unit * 0.08f, paint);
        canvas.drawOval(cx - unit * 0.12f, cy - unit * 0.07f + sway, cx + unit * 0.12f, cy + unit * 0.20f + sway, paint);
        stroke.setColor(Color.rgb(70, 47, 35));
        stroke.setStrokeWidth(unit * 0.035f);
        canvas.drawLine(cx - unit * 0.10f, cy + unit * 0.02f, cx - unit * 0.23f, cy + unit * 0.16f, stroke);
        canvas.drawLine(cx + unit * 0.10f, cy + unit * 0.02f, cx + unit * 0.23f, cy + unit * 0.16f, stroke);
        canvas.drawLine(cx - unit * 0.05f, cy + unit * 0.18f, cx - unit * 0.10f, cy + unit * 0.31f, stroke);
        canvas.drawLine(cx + unit * 0.05f, cy + unit * 0.18f, cx + unit * 0.10f, cy + unit * 0.31f, stroke);
    }

    private void drawGreenIcon(Canvas canvas, float cx, float cy, float unit) {
        switch (greenIcon) {
            case BACKWARD:
                drawArrow(canvas, cx, cy, 0f, unit * 0.28f, unit);
                break;
            case LEFT:
                drawArrow(canvas, cx, cy, -unit * 0.3f, 0f, unit);
                break;
            case RIGHT:
                drawArrow(canvas, cx, cy, unit * 0.3f, 0f, unit);
                break;
            case TURN_LEFT:
                drawTurn(canvas, cx, cy, unit, true);
                break;
            case TURN_RIGHT:
                drawTurn(canvas, cx, cy, unit, false);
                break;
            case JUMP_UP:
                drawJumpUp(canvas, cx, cy, unit);
                break;
            case STAY:
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.WHITE);
                rect.set(cx - unit * 0.19f, cy - unit * 0.19f, cx + unit * 0.19f, cy + unit * 0.19f);
                canvas.drawRoundRect(rect, unit * 0.035f, unit * 0.035f, paint);
                break;
            case FORWARD:
            default:
                drawArrow(canvas, cx, cy, 0f, -unit * 0.3f, unit);
                break;
        }
    }

    private void drawJumpUp(Canvas canvas, float cx, float cy, float unit) {
        drawArrow(canvas, cx, cy + unit * 0.09f, 0f, -unit * 0.36f, unit);
        stroke.setStrokeWidth(unit * 0.026f);
        canvas.drawLine(cx - unit * 0.24f, cy + unit * 0.27f, cx + unit * 0.24f, cy + unit * 0.27f, stroke);
        stroke.setStrokeWidth(unit * 0.042f);
    }

    private void drawArrow(Canvas canvas, float cx, float cy, float dx, float dy, float unit) {
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1f) {
            return;
        }
        float ux = dx / len;
        float uy = dy / len;
        float sx = cx - dx * 0.58f;
        float sy = cy - dy * 0.58f;
        float ex = cx + dx * 0.58f;
        float ey = cy + dy * 0.58f;
        float px = -uy;
        float py = ux;
        float head = unit * 0.15f;

        Path arrow = new Path();
        arrow.moveTo(sx, sy);
        arrow.lineTo(ex, ey);
        arrow.moveTo(ex - ux * head + px * head * 0.62f, ey - uy * head + py * head * 0.62f);
        arrow.lineTo(ex, ey);
        arrow.lineTo(ex - ux * head - px * head * 0.62f, ey - uy * head - py * head * 0.62f);
        canvas.drawPath(arrow, stroke);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(230, 255, 255, 255));
        rect.set(sx - unit * 0.055f, sy + unit * 0.17f, sx + unit * 0.055f, sy + unit * 0.29f);
        canvas.drawOval(rect, paint);
        rect.set(cx - unit * 0.055f, cy + unit * 0.22f, cx + unit * 0.055f, cy + unit * 0.34f);
        canvas.drawOval(rect, paint);
        rect.set(ex - unit * 0.055f, ey + unit * 0.17f, ex + unit * 0.055f, ey + unit * 0.29f);
        canvas.drawOval(rect, paint);
    }

    private void drawTurn(Canvas canvas, float cx, float cy, float unit, boolean left) {
        rect.set(cx - unit * 0.24f, cy - unit * 0.24f, cx + unit * 0.24f, cy + unit * 0.24f);
        canvas.drawArc(rect, left ? -35f : 215f, left ? -250f : 250f, false, stroke);
        Path head = new Path();
        if (left) {
            head.moveTo(cx - unit * 0.23f, cy - unit * 0.08f);
            head.lineTo(cx - unit * 0.34f, cy - unit * 0.18f);
            head.lineTo(cx - unit * 0.19f, cy - unit * 0.25f);
        } else {
            head.moveTo(cx + unit * 0.23f, cy - unit * 0.08f);
            head.lineTo(cx + unit * 0.34f, cy - unit * 0.18f);
            head.lineTo(cx + unit * 0.19f, cy - unit * 0.25f);
        }
        canvas.drawPath(head, stroke);
    }

    private void drawRed(Canvas canvas, float w, float h) {
        Bitmap scene = redScenes[redSceneIndex];
        if (scene == null) {
            scene = BitmapFactory.decodeResource(getResources(), redSceneResources[redSceneIndex]);
            redScenes[redSceneIndex] = scene;
        }
        if (scene != null) {
            drawCoverBitmap(canvas, scene, w, h);
        } else {
            canvas.drawColor(Color.rgb(218, 32, 42));
            drawOutlinedText(canvas, "Red Light", w * 0.5f, h * 0.26f, Math.min(w, h) * 0.16f, Color.rgb(255, 54, 54));
        }
    }

    private void drawCoverBitmap(Canvas canvas, Bitmap bitmap, float w, float h) {
        float scale = Math.max(w / bitmap.getWidth(), h / bitmap.getHeight());
        float bw = bitmap.getWidth() * scale;
        float bh = bitmap.getHeight() * scale;
        rect.set((w - bw) * 0.5f, (h - bh) * 0.5f, (w + bw) * 0.5f, (h + bh) * 0.5f);
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(null);
        canvas.drawBitmap(bitmap, null, rect, paint);
    }

    private void drawPlaygroundBackground(Canvas canvas, float w, float h, float unit) {
        paint.setShader(new LinearGradient(0, 0, 0, h, Color.rgb(105, 198, 242), Color.rgb(207, 239, 255), Shader.TileMode.CLAMP));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(0, 0, w, h, paint);
        paint.setShader(null);

        paint.setColor(Color.WHITE);
        drawCloud(canvas, w * 0.08f, h * 0.15f, unit * 0.09f);
        drawCloud(canvas, w * 0.88f, h * 0.14f, unit * 0.075f);

        paint.setColor(Color.rgb(110, 204, 88));
        rect.set(-w * 0.08f, h * 0.62f, w * 1.08f, h * 1.18f);
        canvas.drawOval(rect, paint);
        paint.setColor(Color.rgb(255, 229, 149));
        rect.set(w * 0.18f, h * 0.72f, w * 0.82f, h * 1.08f);
        canvas.drawOval(rect, paint);
        paint.setColor(Color.rgb(78, 184, 234));
        rect.set(-w * 0.12f, h * 0.82f, w * 0.24f, h * 1.10f);
        canvas.drawOval(rect, paint);
        rect.set(w * 0.78f, h * 0.82f, w * 1.12f, h * 1.10f);
        canvas.drawOval(rect, paint);
    }

    private void drawCloud(Canvas canvas, float cx, float cy, float r) {
        canvas.drawCircle(cx - r * 0.8f, cy + r * 0.25f, r * 0.62f, paint);
        canvas.drawCircle(cx, cy, r, paint);
        canvas.drawCircle(cx + r * 0.95f, cy + r * 0.32f, r * 0.72f, paint);
        rect.set(cx - r * 1.65f, cy + r * 0.25f, cx + r * 1.75f, cy + r * 1.0f);
        canvas.drawRoundRect(rect, r * 0.4f, r * 0.4f, paint);
    }

    private void drawTrafficLight(Canvas canvas, float cx, float cy, float unit, boolean red) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(66, 144, 61));
        rect.set(cx - unit * 0.075f, cy + unit * 0.18f, cx + unit * 0.075f, cy + unit * 0.30f);
        canvas.drawRoundRect(rect, unit * 0.02f, unit * 0.02f, paint);
        rect.set(cx - unit * 0.11f, cy + unit * 0.29f, cx + unit * 0.11f, cy + unit * 0.34f);
        canvas.drawRoundRect(rect, unit * 0.02f, unit * 0.02f, paint);
        paint.setColor(Color.rgb(50, 136, 66));
        rect.set(cx - unit * 0.11f, cy - unit * 0.27f, cx + unit * 0.11f, cy + unit * 0.21f);
        canvas.drawRoundRect(rect, unit * 0.075f, unit * 0.075f, paint);
        paint.setColor(Color.rgb(123, 212, 93));
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(unit * 0.018f);
        stroke.setColor(Color.rgb(123, 212, 93));
        canvas.drawRoundRect(rect, unit * 0.075f, unit * 0.075f, stroke);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(red ? Color.rgb(244, 63, 46) : Color.rgb(118, 245, 74));
        canvas.drawCircle(cx, cy - unit * 0.13f, unit * 0.071f, paint);
        paint.setColor(red ? Color.rgb(34, 91, 56) : Color.rgb(12, 97, 43));
        canvas.drawCircle(cx, cy + unit * 0.09f, unit * 0.071f, paint);
    }

    private void drawOutlinedText(Canvas canvas, String text, float x, float y, float size, int fillColor) {
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        paint.setTextSize(size);
        paint.setColor(Color.rgb(24, 33, 42));
        paint.setStrokeWidth(size * 0.12f);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawText(text, x, y, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(fillColor);
        canvas.drawText(text, x, y, paint);
        paint.setFakeBoldText(false);
    }
}
