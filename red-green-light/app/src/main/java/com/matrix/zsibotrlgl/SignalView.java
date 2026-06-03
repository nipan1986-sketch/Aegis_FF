package com.matrix.zsibotrlgl;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class SignalView extends View {
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
    private Phase phase = Phase.HIDDEN;
    private Icon greenIcon = Icon.FORWARD;
    private String greenText = "往前走";

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
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
    }

    public void setPhase(Phase newPhase) {
        phase = newPhase;
        setVisibility(newPhase == Phase.HIDDEN ? GONE : VISIBLE);
        invalidate();
    }

    public void setGreenAction(String text, Icon icon) {
        greenText = text == null || text.trim().isEmpty() ? "往前走" : text;
        greenIcon = icon == null ? Icon.FORWARD : icon;
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
        } else {
            drawRed(canvas, w, h);
        }
    }

    private void drawGreen(Canvas canvas, float w, float h) {
        canvas.drawColor(Color.rgb(18, 178, 92));
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        paint.setTextSize(Math.min(w, h) * 0.13f);
        canvas.drawText("Green Light!", w * 0.5f, h * 0.24f, paint);
        paint.setTextSize(Math.min(w, h) * 0.1f);
        canvas.drawText(greenText, w * 0.5f, h * 0.82f, paint);
        paint.setFakeBoldText(false);

        float unit = Math.min(w, h);
        float cx = w * 0.5f;
        float cy = h * 0.52f;
        stroke.setColor(Color.WHITE);
        stroke.setStrokeWidth(unit * 0.042f);
        drawGreenIcon(canvas, cx, cy, unit);
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
        canvas.drawColor(Color.rgb(218, 32, 42));
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        paint.setTextSize(Math.min(w, h) * 0.16f);
        canvas.drawText("Red Light!", w * 0.5f, h * 0.26f, paint);
        paint.setTextSize(Math.min(w, h) * 0.1f);
        canvas.drawText("停下", w * 0.5f, h * 0.82f, paint);
        paint.setFakeBoldText(false);

        float unit = Math.min(w, h);
        float cx = w * 0.5f;
        float cy = h * 0.52f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        rect.set(cx - unit * 0.2f, cy - unit * 0.2f, cx + unit * 0.2f, cy + unit * 0.2f);
        canvas.drawRoundRect(rect, unit * 0.035f, unit * 0.035f, paint);
    }
}
