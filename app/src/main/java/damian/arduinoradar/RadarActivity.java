package damian.arduinoradar;


import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RadarActivity extends Activity {

    private int width, height, scale;
    private Paint arcPaint = new Paint();
    private Paint lineBlue = new Paint();
    private Paint lineRed = new Paint();
    private Paint textPaint = new Paint();
    private Handler mHandler;
    private myView myView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        DisplayMetrics metrics = this.getResources().getDisplayMetrics();
        width = metrics.widthPixels;
        height = metrics.heightPixels;
        scale = MainActivity.scale;

        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(3);
        textPaint.setTextSize(40);
        lineBlue.setStyle(Paint.Style.STROKE);
        lineBlue.setStrokeWidth(2);
        lineBlue.setColor(Color.BLUE);
        lineRed.setStyle(Paint.Style.STROKE);
        lineRed.setStrokeWidth(5);
        lineRed.setColor(Color.RED);

        myView = new myView(this);
        setContentView(myView);

        mHandler = new Handler();
        mHandler.postDelayed(mRunnable, 30);
    }

    private Runnable mRunnable = new Runnable() {

        @Override
        public void run() {
            myView.move();
            mHandler.postDelayed(this, 15);
        }
    };

    class myView extends View {

        private Point center = new Point(width/2,height-(width/2));
        private List<RadarPoint> points = new ArrayList<>();

        public myView(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas){
            super.onDraw(canvas);

            int ang = MainActivity.angle;
            int dist = MainActivity.distance;

            Iterator<RadarPoint> j = points.iterator();
            while (j.hasNext()){
                RadarPoint p = j.next();
                RadarPoint point2 = getPoint(p.angle,width/2);
                canvas.drawLine(p.x,p.y,point2.x,point2.y, lineRed);
                if (p.angle == ang) {
                    j.remove();
                }
            }

            canvas.drawArc (0, height-width, width, height, 210, 120, true, arcPaint);
            canvas.drawArc ((width*0.125f), (height-width)+(width*0.125f), width-(width*0.125f), height-(width*0.125f), 210, 120, false, arcPaint);
            canvas.drawArc ((width*0.25f), (height-width)+(width*0.25f), width-(width*0.25f), height-(width*0.25f), 210, 120, false, arcPaint);
            canvas.drawArc ((width*0.375f), (height-width)+(width*0.375f), width-(width*0.375f), height-(width*0.375f), 210, 120, false, arcPaint);
            canvas.drawText ("0",center.x,center.y+50,textPaint);
            canvas.drawText (String.valueOf(scale/4),center.x+90,center.y-20,textPaint);
            canvas.drawText (String.valueOf(scale/2),center.x+210,center.y-80,textPaint);
            canvas.drawText (String.valueOf((int)(scale*0.75)),center.x+320,center.y-150,textPaint);
            canvas.drawText (String.valueOf(scale),center.x+450,center.y-220,textPaint);
            RadarPoint point = getPoint(ang,width/2);
            canvas.drawLine(center.x,center.y,point.x,point.y, lineBlue);

            if (ang > 0 && dist > 0){
                int pixelDist = (int)((width/2) * (dist * (1f/scale)));
                RadarPoint point2 = getPoint(ang,pixelDist);
                points.add(point2);
            }
        }

        public void move() {
            invalidate();
        }

        private class RadarPoint {
            private int x, y, angle;

            private RadarPoint (int x, int y, int angle){
                this.x = x;
                this.y = y;
                this.angle = angle;
            }
        }

        protected RadarPoint getPoint (int angle, int dist) {
            double xOffset = dist * Math.cos(Math.toRadians(angle));
            double yOffset = dist * Math.sin(Math.toRadians(angle));
            int x = center.x + (int)xOffset;
            int y = center.y - (int)yOffset;
            RadarPoint point = new RadarPoint(x, y, angle);
            return point;
        }
    }
}
