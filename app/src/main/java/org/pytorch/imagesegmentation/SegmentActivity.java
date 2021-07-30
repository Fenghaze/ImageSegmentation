package org.pytorch.imagesegmentation;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.os.SystemClock;
import android.util.Log;
import android.view.TextureView;
import android.view.ViewStub;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.camera.core.ImageProxy;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

class Result {
    double score;   //拟合程度
    float x0, y0, x1, y1; //吃水线坐标
    public Result(float x0, float y0, float x1, float y1) {
        this.score = 94.4;
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
    }
};

public class SegmentActivity extends AbstractCameraXActivity<SegmentActivity.AnalysisResult>{

    static class AnalysisResult {
        private final ArrayList<Result> mResults;

        public AnalysisResult(ArrayList<Result> results) {
            mResults = results;
        }
    }

    private Module mModule = null;
    private ResultView mResultView;

    @Override
    protected int getContentViewLayoutId() {
        return R.layout.result_view;
    }

    @Override
    protected TextureView getCameraPreviewTextureView() {
        mResultView = findViewById(R.id.resultView);
        return ((ViewStub) findViewById(R.id.texture_view_stub))
                .inflate()
                .findViewById(R.id.video_view);
    }

    @Override
    protected void applyToUiAnalyzeImageResult(AnalysisResult result) {
        mResultView.setResults(result.mResults);
        mResultView.invalidate();
    }

    private Bitmap imgToBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);

        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    @Nullable
    @WorkerThread
    @Override
    //分析帧数据
    protected AnalysisResult analyzeImage(ImageProxy image, int rotationDegrees) {
        try {
            if (mModule == null) {
                mModule = Module.load(MainActivity.assetFilePath(getApplicationContext(), "best_model.pt"));
            }
        } catch (IOException e) {
            Log.e("Segmentation", "Error reading assets", e);
            return null;
        }

        Bitmap bitmap = imgToBitmap(image.getImage());
        Matrix matrix = new Matrix();
        matrix.postRotate(90.0f);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        //图片重置大小
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, PrePostProcessor.mInputWidth, PrePostProcessor.mInputHeight, true);
        //输入tensor
        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(resizedBitmap, PrePostProcessor.NO_MEAN_RGB, PrePostProcessor.NO_STD_RGB);
        //获得预测mask
        final Tensor outputTensor = mModule.forward(IValue.from(inputTensor)).toTensor();
        //mask tensor
        final float[] scores = outputTensor.getDataAsFloatArray();


        float left =  mResultView.getLeft();
        float top = mResultView.getTop();
        float right = mResultView.getRight();
        float bottom = mResultView.getBottom();
        //手机展示图像与原图的比例、手机展示图像相对于View的坐标偏移量
        float ivScaleX = (float)(right-left) / bitmap.getWidth();
        float ivScaleY = (float)(bottom-top) / bitmap.getHeight();
        float startX = (float)(mResultView.getWidth() - (right-left))/2;
        float startY = top;


//        float ivScaleX = (float) mResultView.getWidth() / bitmap.getWidth();
//        float ivScaleY = (float) mResultView.getHeight() / bitmap.getHeight();
//        float startX = (float) (mResultView.getWidth() - bitmap.getWidth()) /2;
//        float startY = (float) (mResultView.getHeight() - bitmap.getHeight()) /2;

        //检测直线段
        ArrayList<Result> results = PrePostProcessor.detection(bitmap, scores, ivScaleX, ivScaleY, startX, startY);
        return new AnalysisResult(results);
    }
}
