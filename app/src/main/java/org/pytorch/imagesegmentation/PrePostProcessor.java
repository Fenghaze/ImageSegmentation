package org.pytorch.imagesegmentation;

import android.graphics.Bitmap;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import static java.lang.Math.abs;
import static java.lang.Math.atan;
import static org.opencv.imgproc.Imgproc.DIST_HUBER;

//后处理类
public class PrePostProcessor {
    static float[] NO_MEAN_RGB = new float[] {0.0f, 0.0f, 0.0f};
    static float[] NO_STD_RGB = new float[] {1.0f, 1.0f, 1.0f};
    static int mInputWidth = 473;
    static int mInputHeight = 473;

    // see http://host.robots.ox.ac.uk:8080/pascal/VOC/voc2007/segexamples/index.html for the list of classes with indexes
    private static final int CLASSNUM = 2;      //分割的分类数量
    private static final int BACKGROUND = 0;
    private static final int SHIP = 1;

    public static ArrayList<Result> detection(Bitmap srcBitmap,float[] scores, float ivScaleX, float ivScaleY, float startX, float startY){
        Bitmap mask_bitmap = gen_mask(srcBitmap, scores);
        Mat mask_canny_mat = canny_detection(mask_bitmap);
        Result result = detect_lines(srcBitmap, mask_canny_mat, ivScaleX, ivScaleY, startX, startY);
        ArrayList<Result> results = new ArrayList<>();
        results.add(result);
        return results;
    }

    //detect_lines()中HoughLinesP相关参数
    static private double rho = 2.0;
    static private double theta = 3.14/180;
    static private int threshold = 50;
    static private double min_line_len = 15;
    static private double max_line_gap = 30;

    //绘制mask
    private static Bitmap gen_mask(Bitmap srcBitmap,float[] scores) {
        int[] intValues = new int[mInputWidth * mInputHeight];
        for (int j = 0; j < mInputHeight; j++) {
            for (int k = 0; k < mInputWidth; k++) {
                int maxi = 0, maxj = 0, maxk = 0;
                double maxnum = -Double.MAX_VALUE;
                for (int i = 0; i < CLASSNUM; i++) {
                    float score = scores[i * (mInputWidth * mInputHeight) + j * mInputWidth + k];
                    if (score > maxnum) {
                        maxnum = score;
                        maxi = i; maxj = j; maxk = k;
                    }
                }
                if(maxi == SHIP)
                    intValues[maxj * mInputWidth + maxk] = 0xFF0000FF;
                else
                    intValues[maxj * mInputWidth + maxk] = 0xFF000000;
            }
        }
        /*后处理：根据像素值为mask上色*/
        Bitmap bmpSegmentation = Bitmap.createScaledBitmap(srcBitmap, mInputWidth, mInputHeight, true);
        Bitmap outputBitmap = bmpSegmentation.copy(bmpSegmentation.getConfig(), true);
        outputBitmap.setPixels(intValues, 0, outputBitmap.getWidth(), 0, 0, outputBitmap.getWidth(), outputBitmap.getHeight());
        return Bitmap.createScaledBitmap(outputBitmap, srcBitmap.getWidth(), srcBitmap.getHeight(), true);
    }

    //Canny边缘检测
    private static Mat canny_detection(Bitmap mask_bitmap){
        Mat mask_mat = new Mat();
        Mat mask_canny_mat = new Mat();
        Utils.bitmapToMat(mask_bitmap, mask_mat);
        Imgproc.GaussianBlur(mask_mat, mask_mat, new Size(3, 3), 0);
        Imgproc.Canny(mask_mat, mask_canny_mat,50, 150);
        return mask_canny_mat;
    }

    //直线段检测
    private static Result detect_lines(Bitmap srcBitmap, Mat mask_canny_mat, float ivScaleX, float ivScaleY, float startX, float startY)
    {
        Mat src_mat = new Mat();
        Utils.bitmapToMat(srcBitmap, src_mat);
        Utils.bitmapToMat(srcBitmap, src_mat);
        Size srcSize = mask_canny_mat.size();
        double h = srcSize.height;
        double w = srcSize.width;
        Mat lines = new Mat();
        Imgproc.HoughLinesP(mask_canny_mat, lines, rho, theta, threshold, min_line_len, max_line_gap);

        double [][]lines_arr = get_mat_array(lines);
        Vector<double[]> filter_lines_arr = filter_lines(lines_arr, h, w);
        Vector<Point> points = collec_points(filter_lines_arr);

        //创建新的画布，绘制points代表的所有直线段
        Bitmap lines_bitmap = Bitmap.createBitmap((int)w, (int)h, Bitmap.Config.RGB_565);
        Mat lines_mat = Converters.vector_Point_to_Mat(points);
        //将lines_mat中的所有坐标点拟合为一条直线的参数
        Mat final_mat = new Mat();
        Imgproc.fitLine(lines_mat, final_mat, DIST_HUBER, 1, 0.001, 0.001);
        double [][]ds = get_mat_array(final_mat);
        //将直线参数转换为直线上的2个坐标点
        float x_0 = (float)(ds[2][0]-ds[0][0]*h);
        float y_0 = (float)(ds[3][0]-ds[1][0]*w);
        float x_1 = (float)(ds[2][0]+ds[0][0]*h);
        float y_1 = (float)(ds[3][0]+ds[1][0]*w);
        //根据坐标点在原图上绘制吃水线
//        Imgproc.line(src_mat, new Point(x_0, y_0), new Point(x_1, y_1), new Scalar(255, 0, 255), 2);
//        Utils.matToBitmap(src_mat, lines_bitmap);
//        return lines_bitmap;

        //坐标缩放
        x_0 = startX+ivScaleX*x_0;
        y_0 = startY+ivScaleY*y_0;
        x_1 = startX+ivScaleX*x_1;
        y_1 = startY+ivScaleY*y_1;

        //将结果封装在Result中返回
        return new Result(x_0, y_0, x_1, y_1);
    }

    //mat转为array
    private static double [][] get_mat_array(Mat mat){
        int row = mat.rows();
        int col = mat.cols();
        double[][] lines = new double[row][4];
        for (int i = 0; i<row; i++){
            for(int j=0; j<col; j++) {
                lines[i] = mat.get(i, j);
            }
        }
        return lines;
    }

    private static Vector<Point> collec_points(Vector<double[]>lines){
        Vector<Point> points = new Vector<Point>();
        int row = lines.size();
        for(int i=0; i<row; i++) {
            Point pt1 = new Point(lines.get(i)[0], lines.get(i)[1]);
            Point pt2 = new Point(lines.get(i)[2], lines.get(i)[3]);
            points.add(pt1);
            points.add(pt2);
        }
        return points;
    }

    private static Vector<double[]> filter_lines(double [][]lines, double h, double w){
        int row = lines.length;
        int col = 4;
        Vector<double[]> filter_lines = new Vector<>();
        double angle;
        for (int i=0; i<row; i++){
            double x1, y1, x2, y2;
            x1 = lines[i][0];
            y1 = lines[i][1];
            x2 = lines[i][2];
            y2 = lines[i][3];
            if (x2-x1 == 0) angle=90;
            else if(y2 - y1 == 0){
                if (abs(h - y1) >= h * 0.6) continue;
                angle = 0;
                filter_lines.add(lines[i]);
            }
            else if(abs(h - y1) >= h * 0.4) continue;
            else{
                double k = -(y2 - y1) / (x2 - x1);
                angle = atan(k) * 57.29577;
                if (0 < abs(angle) && abs(angle) <= 35) filter_lines.add(lines[i]);
            }
        }
        if (filter_lines.size() == 0) {
            double []tmp = new double[4];
            tmp[0] = 0;
            tmp[1] = h;
            tmp[2] = w;
            tmp[3] = h;
            filter_lines.add(tmp);
        }
        return filter_lines;
    }

};


