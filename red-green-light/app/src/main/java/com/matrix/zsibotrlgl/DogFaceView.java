package com.matrix.zsibotrlgl;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

public class DogFaceView extends View {
    public enum Mood {
        IDLE,
        HAPPY,
        FOCUS,
        STOP
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private Mood mood = Mood.IDLE;
    private long moodSinceMs = System.currentTimeMillis();

    public DogFaceView(Context context) {
        super(context);
        init();
    }

    public DogFaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
    }

    public void setMood(Mood newMood) {
        if (mood != newMood) {
            mood = newMood;
            moodSinceMs = System.currentTimeMillis();
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long now = System.currentTimeMillis();
        float t = (now % 2300L) / 2300f;
        float age = (now - moodSinceMs) / 1000f;
        float w = getWidth();
        float h = getHeight();

        drawBackground(canvas, w, h);
        drawFace(canvas, w, h, t, age);
        postInvalidateDelayed(33);
    }

    private void drawBackground(Canvas canvas, float w, float h) {
        int top;
        int bottom;
        switch (mood) {
            case HAPPY:
                top = Color.rgb(223, 255, 184);
                bottom = Color.rgb(190, 238, 159);
                break;
            case FOCUS:
                top = Color.rgb(200, 244, 255);
                bottom = Color.rgb(155, 222, 241);
                break;
            case STOP:
                top = Color.rgb(255, 214, 208);
                bottom = Color.rgb(246, 160, 152);
                break;
            case IDLE:
            default:
                top = Color.rgb(255, 246, 132);
                bottom = Color.rgb(255, 229, 91);
                break;
        }
        paint.setShader(new LinearGradient(0, 0, 0, h, top, bottom, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, paint);
        paint.setShader(null);
    }

    private void drawFace(Canvas canvas, float w, float h, float t, float age) {
        float unit = Math.min(w, h);
        float cx = w * 0.5f;
        float eyeY = h * 0.37f;
        float eyeGap = unit * 0.25f;
        int ink = mood == Mood.STOP ? Color.rgb(96, 34, 38) : Color.rgb(34, 42, 50);
        float blink = (t > 0.91f && t < 0.98f) ? 0.16f : 1f;
        if (mood == Mood.FOCUS && age < 0.9f) {
            blink = 1.08f;
        }

        if (mood == Mood.HAPPY) {
            drawHappyEye(canvas, cx - eyeGap, eyeY, unit, ink, false);
            drawHappyEye(canvas, cx + eyeGap, eyeY, unit, ink, true);
        } else if (mood == Mood.STOP) {
            drawStopEye(canvas, cx - eyeGap, eyeY, unit, ink, -1);
            drawStopEye(canvas, cx + eyeGap, eyeY, unit, ink, 1);
        } else {
            drawPillEye(canvas, cx - eyeGap, eyeY, unit, blink, ink);
            drawPillEye(canvas, cx + eyeGap, eyeY, unit, blink, ink);
        }
        drawMouth(canvas, w, h, unit, ink);
    }

    private void drawPillEye(Canvas canvas, float cx, float cy, float unit, float blink, int ink) {
        paint.setColor(ink);
        float ew = unit * 0.072f;
        float eh = unit * 0.165f * blink;
        rect.set(cx - ew, cy - eh, cx + ew, cy + eh);
        canvas.drawRoundRect(rect, unit * 0.028f, unit * 0.028f, paint);
    }

    private void drawHappyEye(Canvas canvas, float cx, float cy, float unit, int ink, boolean mirror) {
        stroke.setColor(ink);
        stroke.setStrokeWidth(unit * 0.035f);
        Path path = new Path();
        float dir = mirror ? -1f : 1f;
        path.moveTo(cx - dir * unit * 0.09f, cy + unit * 0.03f);
        path.lineTo(cx, cy - unit * 0.075f);
        path.lineTo(cx + dir * unit * 0.09f, cy + unit * 0.03f);
        canvas.drawPath(path, stroke);
    }

    private void drawStopEye(Canvas canvas, float cx, float cy, float unit, int ink, int side) {
        stroke.setColor(ink);
        stroke.setStrokeWidth(unit * 0.038f);
        canvas.drawLine(cx - side * unit * 0.105f, cy - unit * 0.08f, cx + side * unit * 0.105f, cy - unit * 0.02f, stroke);
        paint.setColor(ink);
        rect.set(cx - unit * 0.055f, cy + unit * 0.018f, cx + unit * 0.055f, cy + unit * 0.105f);
        canvas.drawRoundRect(rect, unit * 0.02f, unit * 0.02f, paint);
    }

    private void drawMouth(Canvas canvas, float w, float h, float unit, int ink) {
        float cx = w * 0.5f;
        float y = h * 0.72f;
        stroke.setColor(ink);
        stroke.setStrokeWidth(unit * 0.022f);
        Path mouth = new Path();
        if (mood == Mood.STOP || mood == Mood.FOCUS) {
            canvas.drawLine(cx - unit * 0.09f, y, cx + unit * 0.09f, y, stroke);
            return;
        }
        if (mood == Mood.HAPPY) {
            mouth.moveTo(cx - unit * 0.105f, y - unit * 0.02f);
            mouth.quadTo(cx, y + unit * 0.105f, cx + unit * 0.105f, y - unit * 0.02f);
        } else {
            mouth.moveTo(cx - unit * 0.08f, y);
            mouth.quadTo(cx, y + unit * 0.05f, cx + unit * 0.08f, y);
        }
        canvas.drawPath(mouth, stroke);
    }
}
