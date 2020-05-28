package com.example.drawpadtest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
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
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author QinYu
 * @description
 * @date 2020/5/27.
 */
public class DrawSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    static final int[] COLORS = {0xFFFF88CC, 0xFFFF0000, 0xFFFF8800, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF4444FF, 0xFF8800FF};
    static final int STROKE_WIDTH_PX = 6;
    static final int MSG_INVALIDATE = 1;

    private LinkedList<Path> pathList;
    private Path curPath;
    private HandlerThread drawThread;
    private DrawHandler drawHandler;
    private RectF dirtyRectF;
    private float startX, startY;
    private boolean isThreadStarted = false;

    public DrawSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        pathList = new LinkedList<>();
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
        drawHandler = new DrawHandler(drawThread.getLooper(), holder, pathList, dirtyRectF);
        //todo 代码添加path
//        drawHandler.sendEmptyMessage(DrawHandler.MSG_ADD_MANY);
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
                curPath = new Path();
                pathList.add(curPath);
                startX = event.getX();
                startY = event.getY();
                curPath.moveTo(startX, startY);
                dirtyRectF.set(startX, startY, startX, startY);
                Log.e("moveTo", pathList.indexOf(curPath) + "   x:" + event.getX() + "   y:" + event.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                //直接连线，但是可以加一个多边形预测，减少棱角
                curPath.lineTo(event.getX(), event.getY());
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
        private LinkedList<Path> pathList;
        private Paint paint;
        private RectF dirtyRectF;
        private Rect rect;
        private Bitmap drawCache;

        public DrawHandler(Looper looper, SurfaceHolder surfaceHolder, LinkedList<Path> pathList, RectF dirtyRect) {
            super(looper);
            this.surfaceHolder = surfaceHolder;
            this.pathList = pathList;
            this.dirtyRectF = dirtyRect;
            paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStrokeWidth(STROKE_WIDTH_PX);
            paint.setStyle(Paint.Style.STROKE);
            rect = new Rect();
            drawCache = Bitmap.createBitmap(surfaceHolder.getSurfaceFrame().width(), surfaceHolder.getSurfaceFrame().height(), Bitmap.Config.ARGB_8888);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MSG_INVALIDATE:
//                    加了bitmap缓存后不需要dirtyRect局部刷新了
//                    rect.left = (int) (dirtyRectF.left < dirtyRectF.right ? dirtyRectF.left : dirtyRectF.right) - STROKE_WIDTH_PX;
//                    rect.top = (int) (dirtyRectF.top < dirtyRectF.bottom ? dirtyRectF.top : dirtyRectF.bottom) - STROKE_WIDTH_PX;
//                    rect.right = (int) (dirtyRectF.left > dirtyRectF.right ? dirtyRectF.left : dirtyRectF.right) + STROKE_WIDTH_PX;
//                    rect.bottom = (int) (dirtyRectF.top > dirtyRectF.bottom ? dirtyRectF.top : dirtyRectF.bottom) + STROKE_WIDTH_PX;
//                    Log.e("dirtyRect", rect.toString() + "  thread:" + getLooper().getThread().getName());

                    Canvas canvas = surfaceHolder.lockCanvas();
                    if (pathList.size() > 500) {
                        Canvas canvasCache = new Canvas(drawCache);
                        for (int i = 0; i < pathList.size(); i++) {
                            Path path = pathList.get(i);
                            paint.setColor(COLORS[i % COLORS.length]);
                            canvasCache.drawPath(path, paint);
                        }
                        pathList.clear();
                    }
                    canvas.drawBitmap(drawCache, 0, 0, null);
                    for (int i = 0; i < pathList.size(); i++) {
                        Path path = pathList.get(i);
                        paint.setColor(COLORS[i % COLORS.length]);
                        canvas.drawPath(path, paint);
                    }
                    surfaceHolder.unlockCanvasAndPost(canvas);
                    break;
                case MSG_ADD_MANY:
                    addManyPath();
                    break;
                default:
                    break;
            }
        }

        /**
         * test only
         */
        public static final int MSG_ADD_MANY = 2;
        public int count = 0;
        private void addManyPath() {
            Log.e("addManyPath", "已经" + count + "条了");
            if (count < 100000) {
                for (int i = 0; i < 100; i++) {
                    Path path = new Path();
                    path.moveTo((int) (Math.random() * 800), (int) (Math.random() * 1200));
                    path.lineTo((int) (Math.random() * 800), (int) (Math.random() * 1200));
                    path.lineTo((int) (Math.random() * 800), (int) (Math.random() * 1200));
                    pathList.add(path);
                    this.sendEmptyMessage(MSG_INVALIDATE);
                }
                this.sendEmptyMessageDelayed(MSG_ADD_MANY, 1);
                count += 100;
            }
        }
    }

    private void addManyPath() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 100000; i++) {
                    Path path = new Path();
                    path.moveTo((int) (Math.random() * 800), (int) (Math.random() * 1200));
                    path.lineTo((int) (Math.random() * 800), (int) (Math.random() * 1200));
                    path.lineTo((int) (Math.random() * 800), (int) (Math.random() * 1200));
                    pathList.add(path);
                    if (i % 5000 == 0) {
                        Log.e("addManyPath", "已经" + i + "条了");
                        drawHandler.sendEmptyMessageDelayed(MSG_INVALIDATE, 10); //太短了会背压
                    } else {
                        drawHandler.sendEmptyMessage(MSG_INVALIDATE);
                    }
                }
            }
        }).start();
    }
}
