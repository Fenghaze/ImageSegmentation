package org.pytorch.imagesegmentation;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;

public class ResultView extends View {
    private ArrayList<Result> mResults;

    private Paint mPaintLine;
    private Paint mPaintText;

    public ResultView(Context context) {
        super(context);
    }

    public ResultView(Context context, AttributeSet attrs){
        super(context, attrs);
        mPaintLine = new Paint();
        mPaintText = new Paint();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mResults == null) return;
        for (Result result : mResults) {
            Path mPath = new Path();
            canvas.drawPath(mPath, mPaintText);
            mPaintLine.setColor(Color.RED);
            mPaintLine.setStrokeWidth(3);
            canvas.drawLine(result.x0, result.y0, result.x1, result.y1, mPaintLine);
            mPaintText.setColor(Color.MAGENTA);
            mPaintText.setColor(Color.RED);
            mPaintText.setStrokeWidth(0);
            mPaintText.setStyle(Paint.Style.FILL);
            mPaintText.setTextSize(48);

            canvas.drawText(String.format("(%.1f, %.1f), (%.1f, %.1f)", result.x0, result.y0, result.x1, result.y1),200, 400, mPaintText);
        }
    }

    public void setResults(ArrayList<Result> results) {
        mResults = results;
    }
}
