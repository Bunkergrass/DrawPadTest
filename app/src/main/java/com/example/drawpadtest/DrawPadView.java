package com.example.drawpadtest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.LinkedList;

/**
 * @author QinYu
 * @description
 * @date 2020/5/27.
 */
public class DrawPadView extends View {
    private static final int[] COLORS = {0xFF444444, 0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFF00, 0xFF00FFFF, 0xFFFF00FF};
    private Paint paint;
    private LinkedList<Path> pathList;
    private Path curPath;

    public DrawPadView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        pathList = new LinkedList<>();
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(6);
        paint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        for (int i = 0; i < pathList.size(); i++) {
            Path path = pathList.get(i);
            paint.setColor(COLORS[i % 7]);
            canvas.drawPath(path, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                curPath = new Path();
                pathList.add(curPath);
                curPath.moveTo(event.getX(), event.getY());
                Log.e("moveTo", pathList.indexOf(curPath) + "   x:" + event.getX() + "   y:" + event.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                curPath.lineTo(event.getX(), event.getY());
                Log.e("lineTo", pathList.indexOf(curPath) + "   x:" + event.getX() + "   y:" + event.getY());
                invalidate();
                break;
        }
        return true;
    }
}
