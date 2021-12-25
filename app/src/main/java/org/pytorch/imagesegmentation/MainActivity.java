package org.pytorch.imagesegmentation;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements Runnable {
    private ImageView mImageView;       //图片显示区域
    private Button mButtonSegment;      //预测按钮
    private ProgressBar mProgressBar;
    private ResultView mResultView;
    private Bitmap mBitmap = null;      //图片
    private Module mModule = null;      //waterline模型
    private Module detModule = null;      //det模型
    private Module recModule = null;      //rec模型
    private int mImageIndex = 0;
    private String[] mTestImages = {"1.jpg", "2.jpg", "3.jpg", "5.jpg", "ruler1.jpg", "test1.jpg"};
    private static final String TAG = "MainActivity";
    public static final int SELECT_PHOTH = 0;// 选择图片
    public static final int PHOTO_REQUEST_CAREMA = 1;// 拍照
    public static final int CROP_PHOTO = 2; //裁剪
    private Uri imageUri;
    public static File tempFile;

    //OpenCV库加载并初始化成功后的回调函数
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            // TODO Auto-generated method stub
            switch (status) {
                case BaseLoaderCallback.SUCCESS:
                    Log.i(TAG, "成功加载");
                    break;
                default:
                    super.onManagerConnected(status);
                    Log.i(TAG, "加载失败");
                    break;
            }
        }
    };
    //OpenCV库报错的回调函数
    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        }
        else{
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    //加载模型文件
    public static String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }

    //入口函数
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        try {
            //打开图片
            mBitmap = BitmapFactory.decodeStream(getAssets().open(mTestImages[mImageIndex]));
            //获取waterline模型
            mModule = Module.load(MainActivity.assetFilePath(getApplicationContext(), "best_model.pt"));
            Log.d("waterline model", "load success");
            //获取det模型
            detModule = Module.load(MainActivity.assetFilePath(getApplicationContext(), "det_model.pt"));
            Log.d("det model", "load success");
            //获取rec模型
            recModule = Module.load(MainActivity.assetFilePath(getApplicationContext(), "rec_model.pt"));
            Log.d("rec model", "load success");
        } catch (IOException e) {
            Log.e("ImageSegmentation", "Error reading assets", e);
            finish();
        }

        mImageView = findViewById(R.id.imageView);
        mImageView.setImageBitmap(mBitmap);
        mResultView = findViewById(R.id.resultView);
        mResultView.setVisibility(View.INVISIBLE);

        //切换图片按钮点击事件
        final Button buttonRestart = findViewById(R.id.restartButton);
        buttonRestart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mResultView.setVisibility(View.INVISIBLE);
                mImageIndex = (mImageIndex + 1) % mTestImages.length;
                try {
                    mBitmap = BitmapFactory.decodeStream(getAssets().open(mTestImages[mImageIndex]));
                    mImageView.setImageBitmap(mBitmap);
                } catch (IOException e) {
                    Log.e("ImageSegmentation", "Error reading assets", e);
                    finish();
                }
            }
        });

        //预测分割按钮点击事件
        mButtonSegment = findViewById(R.id.segmentButton);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mButtonSegment.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mResultView.setVisibility(View.INVISIBLE);
                mButtonSegment.setEnabled(false);
                mProgressBar.setVisibility(ProgressBar.VISIBLE);
                mButtonSegment.setText(getString(R.string.run_model));
                Thread thread = new Thread(MainActivity.this);
                thread.start();
            }
        });

        //上传图片
        final Button upload = findViewById(R.id.uploadButton);
        upload.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                mResultView.setVisibility(View.INVISIBLE);
                switch (view.getId()) {
                    case R.id.uploadButton:
                        Intent intent = new Intent(Intent.ACTION_PICK);
                        intent.setType("image/*");
                        startActivityForResult(intent, SELECT_PHOTH);
                        break;
                }
            }
        });

        //开启摄像头
        final Button camera = findViewById(R.id.cameraButton);
        camera.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.cameraButton:
                        openCamera(MainActivity.this);
                        break;
                }
            }
        });

        //实时检测
        final Button live = findViewById(R.id.liveButton);
        live.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                final Intent intent = new Intent(MainActivity.this, SegmentActivity.class);
                startActivity(intent);  //打开相机预览Activity
            }
        });

        String str = getStrFromJNI();
        Log.e("use c++", str);
    }

    public native String getStrFromJNI();
    static {
        System.loadLibrary("hello");
    }

    private void startCropImage(Uri uri, MainActivity activity) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        // 让裁剪框支持缩放
        intent.putExtra("scale", true);
        SimpleDateFormat timeStampFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        String filename = timeStampFormat.format(new Date());
        tempFile = new File(Environment.getExternalStorageDirectory(), filename + ".jpg");
        ContentValues contentValues = new ContentValues(1);
        contentValues.put(MediaStore.Images.Media.DATA, tempFile.getAbsolutePath());
        //检查是否有存储权限，以免崩溃
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            //申请WRITE_EXTERNAL_STORAGE权限
            Toast.makeText(this,"请开启存储权限",Toast.LENGTH_SHORT).show();
            return;
        }
        imageUri = activity.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        // 设置图片的输出格式
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        // return-data=true传递的为缩略图，小米手机默认传递大图，所以会导致onActivityResult调用失败
        intent.putExtra("return-data", false);
        startActivityForResult(intent, CROP_PHOTO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case SELECT_PHOTH:
                if (resultCode == RESULT_OK && data != null) {
                    imageUri = data.getData();
                    startCropImage(imageUri, MainActivity.this);
                }
                break;
            case PHOTO_REQUEST_CAREMA:
                if (resultCode == RESULT_OK) {
                    Intent intent = new Intent("com.android.camera.action.CROP");
                    intent.setDataAndType(imageUri, "image/*");
                    intent.putExtra("scale", true);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                    startActivityForResult(intent, CROP_PHOTO); // 启动裁剪程序
                }
                break;
            case CROP_PHOTO:
                if (resultCode == RESULT_OK) {
                    try {
                        mBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                        mImageView.setImageBitmap(mBitmap);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

    public static boolean hasSdcard() {
        return Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
    }

    private void openCamera(MainActivity activity) {
        //獲取系統版本
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        // 激活相机
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // 判断存储卡是否可以用，可用进行存储
        if (hasSdcard()) {
            SimpleDateFormat timeStampFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
            String filename = timeStampFormat.format(new Date());
            tempFile = new File(Environment.getExternalStorageDirectory(), filename + ".jpg");
            if (currentapiVersion < 24) {
                // 从文件中创建uri
                imageUri = Uri.fromFile(tempFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            } else {
                //兼容android7.0 使用共享文件的形式
                ContentValues contentValues = new ContentValues(1);
                contentValues.put(MediaStore.Images.Media.DATA, tempFile.getAbsolutePath());
                //检查是否有存储权限，以免崩溃
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    //申请WRITE_EXTERNAL_STORAGE权限
                    Toast.makeText(this,"请开启存储权限",Toast.LENGTH_SHORT).show();
                    return;
                }
                imageUri = activity.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            }
        }
        // 开启一个带有返回值的Activity，请求码为PHOTO_REQUEST_CAREMA
        activity.startActivityForResult(intent, PHOTO_REQUEST_CAREMA);
    }

    @Override
    public void run() {
        //图片重置大小
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(mBitmap, PrePostProcessor.mInputWidth, PrePostProcessor.mInputHeight, true);
        //输入tensor
        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(resizedBitmap, PrePostProcessor.NO_MEAN_RGB, PrePostProcessor.NO_STD_RGB);
        final long startTime = SystemClock.elapsedRealtime();
        //获得waterline模型的预测mask
        final Tensor outputTensor = mModule.forward(IValue.from(inputTensor)).toTensor();

        final long inferenceTime = SystemClock.elapsedRealtime() - startTime;
        Log.d("ImageSegmentation",  "inference time (ms): " + inferenceTime);
        //mask tensor
        final float[] scores = outputTensor.getDataAsFloatArray();

        //获取图像在ImageView中的位置信息
        Matrix matrix = mImageView.getImageMatrix();
        RectF rectF = new RectF();
        Drawable drawable = mImageView.getDrawable();
        if (drawable != null) {
            rectF.set(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            matrix.mapRect(rectF);
        }
        float left = rectF.left;       //左上角x
        float top = rectF.top;         //左上角y
        float right = rectF.right;     //右下角x
        float bottom = rectF.bottom;   //右下角y

        //手机展示图像与原图的比例、手机展示图像相对于View的坐标偏移量
        float ivScaleX = (float)(right-left) / mBitmap.getWidth();
        float ivScaleY = (float)(bottom-top) / mBitmap.getHeight();
        float startX = (float)(mResultView.getWidth() - (right-left))/2;
        float startY = top;

        //检测waterline
        ArrayList<Result> results = PrePostProcessor.detection(mBitmap, scores, ivScaleX, ivScaleY, startX, startY);

        //检测bboxes
        //图片重置大小
        OcrProcessor.getSize(mBitmap.getHeight(), mBitmap.getWidth());
        Bitmap resizedBitmap2 = Bitmap.createScaledBitmap(mBitmap, (int)OcrProcessor.mInputWidth, (int)OcrProcessor.mInputHeight, true);
        //输入tensor
        final Tensor inputTensor2 = TensorImageUtils.bitmapToFloat32Tensor(resizedBitmap2, OcrProcessor.MEAN, OcrProcessor.STD);
        //获得det model的输出结果
        final Tensor outputTensor2 = detModule.forward(IValue.from(inputTensor2)).toTensor(); //[1,1,x,640]
        //tensor转为float
        final float[] scores2 = outputTensor2.getDataAsFloatArray();
        final float[][][] pred = OcrProcessor.transfer1to3(scores2, mBitmap.getHeight(), mBitmap.getWidth());

        //识别bboxes
        //Bitmap image = OcrProcessor.toGrayscale(mBitmap);    //转为灰度图
        //图片重置大小
        Bitmap resizedBitmap3 = Bitmap.createScaledBitmap(mBitmap, 100, 32, true);
        //输入tensor
        final Tensor inputTensor3 = OcrProcessor.bitmapToFloat32Tensor(resizedBitmap3);
        float[] input = inputTensor3.getDataAsFloatArray();
        //获得det model的输出结果
        final Tensor outputTensor3 = recModule.forward(IValue.from(inputTensor3)).toTensor();
        Log.d("rec", "rec image");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //mImageView.setImageBitmap(bitmap);
                mResultView.setResults(results);
                mButtonSegment.setEnabled(true);
                mButtonSegment.setText(getString(R.string.segment));
                mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                mResultView.invalidate();
                mResultView.setVisibility(View.VISIBLE);
            }
        });
    }
}
