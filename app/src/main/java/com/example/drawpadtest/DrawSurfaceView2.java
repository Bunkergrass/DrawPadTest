package com.example.drawpadtest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import java.util.LinkedList;

/**
 * @author QinYu
 * @description
 * @date 2020/5/28.
 */
public class DrawSurfaceView2 extends SurfaceView implements SurfaceHolder.Callback {
    static final int[] COLORS = {0xFFFF88CC, 0xFFFF0000, 0xFFFF8800, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF4444FF, 0xFF8800FF};
    static final int STROKE_WIDTH_PX = 6;
    static final int MSG_INVALIDATE = 1;

    private Path path;
    private HandlerThread drawThread;
    private DrawHandler drawHandler;
    private RectF dirtyRectF;
    private float startX, startY;
    private boolean isThreadStarted = false;

    public DrawSurfaceView2(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        path = new Path();
        path.incReserve(100000);
        getHolder().addCallback(this);
        drawThread = new HandlerThread("drawThread");
        dirtyRectF = new RectF();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!isThreadStarted) {
            drawThread.start();
            isThreadStarted = true;
        }
        drawHandler = new DrawHandler(drawThread.getLooper(), holder, path, dirtyRectF);
        addManyPath();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        drawHandler.removeMessages(MSG_INVALIDATE);
        drawThread.quitSafely();
        isThreadStarted = false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getX();
                startY = event.getY();
                path.moveTo(startX, startY);
                dirtyRectF.set(startX, startY, startX, startY);
                Log.e("moveTo", " x:" + event.getX() + "   y:" + event.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                //todo 直接连线，但是可以加一个多边形预测，减少棱角
                path.lineTo(event.getX(), event.getY());
                dirtyRectF.set(startX, startY, event.getX(), event.getY());
                startX = event.getX();
                startY = event.getY();
//                Log.e("lineTo", pathList.indexOf(curPath) + "   x:" + event.getX() + "   y:" + event.getY());
                drawHandler.sendEmptyMessage(MSG_INVALIDATE);
                break;
        }
        return true;
    }

    private static class DrawHandler extends Handler {
        private SurfaceHolder surfaceHolder;
        private Path path;
        private Paint paint;
        private RectF dirtyRectF;
        private Rect rect;
        private Bitmap drawCache;

        public DrawHandler(Looper looper, SurfaceHolder surfaceHolder, Path path, RectF dirtyRect) {
            super(looper);
            this.surfaceHolder = surfaceHolder;
            this.path = path;
            this.dirtyRectF = dirtyRect;
            paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStrokeWidth(STROKE_WIDTH_PX);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
            rect = new Rect();
            drawCache = Bitmap.createBitmap(surfaceHolder.getSurfaceFrame().width(), surfaceHolder.getSurfaceFrame().height(), Bitmap.Config.ARGB_8888);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MSG_INVALIDATE:
                    //offset 笔触宽度
                    rect.left = (int) (dirtyRectF.left < dirtyRectF.right ? dirtyRectF.left : dirtyRectF.right) - STROKE_WIDTH_PX;
                    rect.top = (int) (dirtyRectF.top < dirtyRectF.bottom ? dirtyRectF.top : dirtyRectF.bottom) - STROKE_WIDTH_PX;
                    rect.right = (int) (dirtyRectF.left > dirtyRectF.right ? dirtyRectF.left : dirtyRectF.right) + STROKE_WIDTH_PX;
                    rect.bottom = (int) (dirtyRectF.top > dirtyRectF.bottom ? dirtyRectF.top : dirtyRectF.bottom) + STROKE_WIDTH_PX;
                    Log.e("dirtyRect", rect.toString() + "  thread:" + getLooper().getThread().getName());
                    Canvas canvas = surfaceHolder.lockCanvas(rect);
                    canvas.drawPath(path, paint);
                    surfaceHolder.unlockCanvasAndPost(canvas);
                    break;
                default:
                    break;
            }
        }
    }

    public void addManyPath() {
        for (int i = 0; i < 100000; i++) {
            path.moveTo((int) (Math.random() * 800), (int)(Math.random() * 1200));
            path.lineTo((int) (Math.random() * 800), (int)(Math.random() * 1200));
            path.lineTo((int) (Math.random() * 800), (int)(Math.random() * 1200));
            drawHandler.sendEmptyMessage(MSG_INVALIDATE);
        }
    }
}
