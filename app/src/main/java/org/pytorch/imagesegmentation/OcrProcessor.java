package org.pytorch.imagesegmentation;
import org.opencv.core.Core;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfFloat;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.pytorch.Tensor;
import android.graphics.Bitmap;
import android.util.Log;
import org.opencv.core.Point;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import static java.lang.Math.abs;
import static java.lang.Math.min;
import static org.opencv.core.CvType.CV_32FC1;
import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.core.CvType.CV_8UC3;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;


class OcrResults{
    public int [][]tmp_boxes;
    public float [][]tmp_scores;
}

class DetBox{
    public double [][]points;
    public double sside;
}

public class OcrProcessor {
    static float[] MEAN = new float[] {0.48f, 0.45f, 0.41f};
    static float[] STD = new float[] {0.23f, 0.22f, 0.23f};
    static double mInputWidth = 640;
    static double mInputHeight = 640;
    static int stride = 32;
    static int side_len = 640;
    static float[] scale = {0.0f, 0.0f};

    static float thresh = 0.5f;
    static float box_thresh =  0.6f;
    static int max_candidates = 1000;
    static int unclip_ratio = 2;
    static int min_size = 3;

    //重置输入size
    public static void getSize(int height, int width){
        if (height > width){
            mInputHeight = side_len;
            mInputWidth = Math.ceil((mInputHeight / height) * (width / stride) * stride);
        }else{
            mInputWidth = side_len;
            mInputHeight = Math.ceil((mInputWidth / width) * (height / stride) * stride);
            scale[0] = (float) (width / mInputWidth);
            scale[1] = (float) (height / mInputHeight);
        }
    }
    //一维数组转为三维数组
    public static float[][][] transfer1to3(float []scores, int height, int width){
        int h;
        int w;
        if (height > width){
            h = side_len;
            w = (int)scores.length/h;
        } else{
            w = side_len;
            h = (int)scores.length/w;
        }
        float[][][] pred = new float[1][h][w];
        int len = scores.length;
        int count = 0;
        for(int i=0; i<1; i++) {
            for(int j=0; j<h; j++){
                for(int k=0; k<w; k++){
                    pred[i][j][k] = scores[count];
                    count+=1;
                    if(count == len){
                        break;
                    }
                }
            }
        }
        OcrProcessor.genBoxes(pred, scale);
        return pred;
    }
    //生成bboxes
    public static void genBoxes(float [][][]pred, float []ratio_list) {
        int height = pred[0].length;
        int width = pred[0][0].length;
        int[][][] segmentation = new int[1][height][width];
        for (int i = 0; i < 1; i++) {
            for (int j = 0; j < height; j++) {
                for (int k = 0; k < width; k++) {
                    if (pred[i][j][k] > thresh) {
                        segmentation[i][j][k] = 1;
                    } else {
                        segmentation[i][j][k] = 0;
                    }
                }
            }
        }
        float boxes_batch[];
        float score_batch[];
        for (int batch_index = 0; batch_index < pred.length; batch_index++) {
            OcrResults res;
            boxes_from_bitmap(pred[batch_index], segmentation[batch_index], width, height);
//            float []boxes;
//            float []score;
//            for(int k=0; k<tmp_boxes.length; k++){
//                boxes[k] = tmp_boxes[k];
//            }
//            if(boxes.length > 0){
//                boxes = np.array(boxes);
//                ratio_w, ratio_h = ratio_list[batch_index];
//                boxes[:, :, 0] = boxes[:, :, 0] * ratio_w;
//                boxes[:, :, 1] = boxes[:, :, 1] * ratio_h;
//            }
//            boxes_batch.append(boxes);
//            score_batch.append(score);
//        }
//        return boxes_batch, score_batch;
        }
    }

    private static void boxes_from_bitmap(float [][]pred, int[][] _bitmap, int dest_width, int dest_height){
//        _bitmap: single map with shape (1, H, W), whose values are binarized as {0, 1}
        int [][] bitmap = _bitmap;
        int height = bitmap.length;
        int width = bitmap[0].length;
        //二维数组转为mat
        Mat src_mat = new Mat(height, width, CV_8UC1);
        for(int i=0; i<height; i++){
            for(int j=0; j<width; j++){
                bitmap[i][j] *= 255;
                src_mat.put(i, j, bitmap[i][j]);
            }
        }
        List<MatOfPoint> outs = new ArrayList<>();
        Mat hierarchy = new Mat();
        Bitmap bitmap1 = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Utils.matToBitmap(src_mat, bitmap1);
        Imgproc.findContours(src_mat, outs, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
        //hierarchy.release();

        //提取轮廓坐标 Mat=>Point
        List<List<Point>> contours = new ArrayList<>();
        for (int i=0; i<outs.size(); i++){
            contours.add(outs.get(i).toList());
        }

        Log.d("findcountors", "finish");
        int num_contours = min(contours.size(), max_candidates);
        int [][][]boxes = new int[num_contours][4][2];
        float [][]scores = new float[num_contours][];
        for(int index=0; index<num_contours; index++){
            DetBox detbox = get_mini_boxes(new MatOfPoint2f(outs.get(index).toArray()));
            Log.d("findcountors", "finish");

            if (detbox.sside < min_size){
                continue;
            }
            float score = box_score_fast(pred, detbox.points);
            if (box_thresh > score){
                continue;
            }
//            box = unclip(detbox.points, unclip_ratio).reshape(-1, 1, 2);
//            box, sside = get_mini_boxes(box);
//            if (sside < min_size + 2){
//                continue;
//            }
//            box = np.array(box);
//            if not isinstance(dest_width, int){
//                dest_width = dest_width.item();
//                dest_height = dest_height.item();
//            }
//            box[:, 0] = np.clip(np.round(box[:, 0] / width * dest_width), 0, dest_width);
//            box[:, 1] = np.clip(np.round(box[:, 1] / height * dest_height), 0, dest_height);
//            boxes[index, :, :] = box.astype(np.int16);
//            scores[index] = score;
        }
//        return boxes, scores;
    }

//    private static void unclip(double [][] box, int unclip_ratio){
//        poly = Polygon(box)
//        distance = poly.area * unclip_ratio / poly.length
//        offset = pyclipper.PyclipperOffset()
//        offset.AddPath(box, pyclipper.JT_ROUND, pyclipper.ET_CLOSEDPOLYGON)
//        expanded = np.array(offset.Execute(distance))
//        return expanded
//    }

    private static float box_score_fast(float [][]bitmap, double [][] _box){
        int height = bitmap.length;
        int width = bitmap[0].length;
        double[][] box = _box.clone();
        //找到横纵坐标最大最小值
        sort(_box, new int[] {0,1});  //按第一列升序排序
        int xmin = (int)_box[0][0];
        int xmax = (int)_box[_box[0].length][0];
        sort(_box, new int[] {1, 0});  //按第二列升序排序
        int ymin = (int)_box[0][1];
        int ymax = (int)_box[_box[0].length][1];
        //第一列-xmin
        for (int i=0; i<box.length; i++){
            box[i][0] -= xmin;
        }
        //第二列-ymin
        for (int i=0; i<box.length; i++){
            box[i][1] -= ymin;
        }
        Mat mask = Mat.zeros(ymax-ymin+1,xmax-xmin+1, CV_8UC1);
        List<Point> points = new ArrayList<>();
        for(int i=0; i<box.length; i++){
            points.add(new Point(box[i][0], box[i][1]));
        }
        MatOfPoint matPt = new MatOfPoint();
        matPt.fromList(points);
        List<MatOfPoint> pts = new ArrayList<MatOfPoint>();
        pts.add(matPt);
        Imgproc.fillPoly(mask,pts,new Scalar( 1, 1, 1 ),0,0,new Point(0,0) );

        //double类型二维数组转为mat
        Mat mat = new Mat(mask.height(), mask.width(), CV_32FC1);
        for (int i=0,row=ymin; i<mask.height()&&row<ymax+1; i++,row++) {
            for(int j=0,col=xmin; j<mask.width()&&col<xmax+1; j++,col++) {
                mat.put(i, j, bitmap[row][col]);
            }
        }
        Scalar score = Core.mean(mat, mask);
        return (float) score.val[0];
    }

    //mat转为数组
    private static double [][] mat2array(Mat mat){
        int row = mat.rows();
        int col = mat.cols();
        double[][] lines = new double[row][col];
        for (int i = 0; i<row; i++){
            for(int j=0; j<col; j++) {
                lines[i] = mat.get(i, j);
            }
        }
        return lines;
    }

    //获取单个det bbox
    private static DetBox get_mini_boxes(MatOfPoint2f contour){
        RotatedRect bounding_box;
        bounding_box = Imgproc.minAreaRect(contour);
        Mat mat = new Mat();
        Imgproc.boxPoints(bounding_box, mat);
        double [][]points = get_mat_array(mat);
        sort(points, new int[] {0,1});  //按第一列升序排序
        Log.d("findcountors", "finish");

        int index_1 = 0;
        int index_2 = 1;
        int index_3 = 2;
        int index_4 = 3;

        if (points[1][1] > points[0][1]){
            index_1 = 0;
            index_4 = 1;
        }else{
            index_1 = 1;
            index_4 = 0;
        }
        if (points[3][1] > points[2][1]){
            index_2 = 2;
            index_3 = 3;
        }else{
            index_2 = 3;
            index_3 = 2;
        }
        double[][] box = {points[index_1], points[index_2], points[index_3], points[index_4]};
        DetBox detbox = new DetBox();
        detbox.points = box;
        detbox.sside = Math.min(bounding_box.size.height, bounding_box.size.width);
        return detbox;
    }

    //获取mat的像素数据
    private static double [][] get_mat_array(Mat mat){
        int row = mat.rows();
        int col = mat.cols();
        double[][] points = new double[row][col];
        for (int i = 0; i<row; i++){
            for(int j=0; j<col; j++) {
                double []x = mat.get(i, j);
                points[i][j] = x[0];
            }
        }
        return points;
    }

    //二维数组按列排序
    public static void sort(double[][] ob, final int[] order) {
        Arrays.sort(ob, new Comparator<Object>() {
            public int compare(Object o1, Object o2) {
                double[] one = (double[]) o1;
                double[] two = (double[]) o2;
                for (int i = 0; i < order.length; i++) {
                    int k = order[i];
                    if (one[k] > two[k]) {
                        return 1;
                    } else if (one[k] < two[k]) {
                        return -1;
                    } else {
                        continue;  //如果按一条件比较结果相等，就使用第二个条件进行比较。
                    }
                }
                return 0;
            }
        });
    }

    //bitmap转为灰度图
    public static Bitmap toGrayscale(Bitmap mBitmap) {
        //bitmap转为mat
        Mat mat = new Mat();
        Utils.bitmapToMat(mBitmap, mat);
        //mat转为灰度图mat
        Mat mat2 = new Mat();
        Imgproc.cvtColor(mat, mat2, COLOR_BGR2GRAY);
        //灰度图转为bitmap
        Bitmap bitmap = Bitmap.createBitmap(mBitmap.getWidth(), mBitmap.getHeight(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(mat2, bitmap);
        return bitmap;
    }

    /*
    * TensorImageUtils.bitmapToFloat32Tensor 返回Tensor为单通道的代码
    * */

    public static Tensor bitmapToFloat32Tensor(final Bitmap bitmap) {
        return bitmapToFloat32Tensor(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight());
    }

    public static Tensor bitmapToFloat32Tensor(
            final Bitmap bitmap,
            int x,
            int y,
            int width,
            int height) {

        final FloatBuffer floatBuffer = Tensor.allocateFloatBuffer(width * height);
        bitmapToFloatBuffer(bitmap, x, y, width, height, floatBuffer, 0);
        return Tensor.fromBlob(floatBuffer, new long[] {1, 1, height, width});
    }

    public static void bitmapToFloatBuffer(final Bitmap bitmap,
                                           final int x,
                                           final int y,
                                           final int width,
                                           final int height,
                                           final FloatBuffer outBuffer,
                                           final int outBufferOffset) {
        checkOutBufferCapacityNoRgb(outBuffer, outBufferOffset, width, height);

        final int pixelsCount = height * width;
        final int[] pixels = new int[pixelsCount];
        bitmap.getPixels(pixels, 0, width, x, y, width, height);
        for (int i = 0; i < pixelsCount; i++) {
            final int c = pixels[i];
            outBuffer.put(((c) & 0xff) / 255.0f);
        }
    }

    private static void checkOutBufferCapacityNoRgb(
            FloatBuffer outBuffer, int outBufferOffset, int tensorWidth, int tensorHeight) {
        if (outBufferOffset + tensorWidth * tensorHeight > outBuffer.capacity()) {
            throw new IllegalStateException("Buffer underflow");
        }
    }

}