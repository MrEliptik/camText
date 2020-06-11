import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

public class MyDrawView extends View {

    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Path mPath;
    private Paint mBitmapPaint;
    private Paint mPaint;
    private Paint mRectPaint;
    private ArrayList<Point> pointsList = new ArrayList<>();

    public MyDrawView(Context c) {
        super(c);

        mPath = new Path();
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(Color.parseColor("#00f1f5"));
        mPaint.setAlpha(56);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(40);

        mRectPaint = new Paint();
        mRectPaint.setAntiAlias(true);
        mRectPaint.setDither(true);
        mRectPaint.setColor(Color.parseColor("#00f1f5"));
        mRectPaint.setAlpha(56);
        mRectPaint.setStyle(Paint.Style.FILL);
        mRectPaint.setStrokeJoin(Paint.Join.ROUND);
        mRectPaint.setStrokeCap(Paint.Cap.ROUND);
        mRectPaint.setStrokeWidth(40);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);

        canvas.drawPath(mPath, mPaint);

        ArrayList<Double> points = calculateBoundingBox();

        if(points.size() > 0) {
            canvas.drawRect((float) Math.round(points.get(0)),
                    (float) Math.round(points.get(1)),
                    (float) Math.round(points.get(2)),
                    (float) Math.round(points.get(3)), mRectPaint);
        }
    }

    private float mX, mY;
    private static final float TOUCH_TOLERANCE = 4;

    private void touch_start(float x, float y) {
        mPath.reset();
        mPath.moveTo(x, y);
        mX = x;
        mY = y;
        pointsList.add(new Point((int)mX, (int)mY));
    }
    private void touch_move(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
            mX = x;
            mY = y;
        }
    }
    private void touch_up() {
        mPath.lineTo(mX, mY);
        // commit the path to our offscreen
        mCanvas.drawPath(mPath, mPaint);
        // save the path to calculate bounding rectangle
        pointsList.add(new Point((int)mX, (int)mY));
        for (Point p : pointsList){
            System.out.println(p);
        }

        // kill this so we don't double draw
        mPath.reset();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touch_start(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                touch_move(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touch_up();
                invalidate();
                break;
        }
        // Consume event
        return true;
    }

    public ArrayList<Double> calculateBoundingBox()
    {
        ArrayList<Double> rectanglePoints = new ArrayList<>();

        //Method to calculate the bounding box of this polyline
        double xmin = Double.POSITIVE_INFINITY;
        double xmax = Double.NEGATIVE_INFINITY;
        double ymin = xmin, ymax = xmax;

        for (Point p : pointsList){
            if ( p.x < xmin )
                xmin = p.x - (mPaint.getStrokeWidth()/2);

            if ( p.y < ymin )
                ymin = p.y - (mPaint.getStrokeWidth()/2);

            if ( p.x > xmax )
                xmax = p.x + (mPaint.getStrokeWidth()/2);

            if ( p.y > ymax )
                ymax = p.y + (mPaint.getStrokeWidth()/2);
        }
        if(xmin != Double.POSITIVE_INFINITY && xmax != Double.NEGATIVE_INFINITY &&
                ymin != Double.POSITIVE_INFINITY && ymax != Double.NEGATIVE_INFINITY &&
            xmin != xmax && ymin != ymax){
            rectanglePoints.add(xmin);
            rectanglePoints.add(ymin);
            rectanglePoints.add(xmax);
            rectanglePoints.add(ymax);
        }

        return rectanglePoints;
    }

    public void clear(){
        mBitmap.eraseColor(Color.TRANSPARENT);
        invalidate();
        System.gc();
    }}