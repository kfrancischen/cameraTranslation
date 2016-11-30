package edu.stanford.ee368.cameratranslation;

import android.content.Context;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Pattern;
import android.content.Context;

/**
 * Created by francischen on 11/20/16.
 */

public class DatabaseFile {
    private  HashMap<String, String> vocabulary = new HashMap();

    Context context;

    public DatabaseFile(Context context){
        this.context = context;
        for(int i = 0; i < chineseVocabulary.size(); i++){
                vocabulary.put(englishVocabulary.get(i), chineseVocabulary.get(i));
        }
    }


    public List<String> getEnglishVocabulary(){
        return englishVocabulary;
    }

    public List<String> getChineseVocabulary(){
        return chineseVocabulary;
    }

    public String getEnglishByIndex(int index){
        if(index < englishVocabulary.size()){
            return englishVocabulary.get(index);
        }
        else{
            return null;
        }
    }

    public String getChineseByIndex(int index){
        if(index < chineseVocabulary.size()){
            return chineseVocabulary.get(index);
        }
        else{
            return null;
        }
    }

    // this computes the PCA matrices and the eigenvectors.
    public void computePCAMats(Mat PCAMat, Mat topVectors){
        Mat ensemble = new Mat();
        for(int i = 0; i < englishVocabulary.size(); i++){
            int resourceId = context.getResources().getIdentifier(englishVocabulary.get(i), "raw", context.getPackageName());
            try {
                Mat image = Utils.loadResource(this.context, resourceId, Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
                image.convertTo(image, CvType.CV_64FC1);
                Imgproc.resize(image, image, new Size(480, 270));
                //Core.normalize(image, image);
                ensemble.push_back(image.reshape(0, 1));
            }
            catch(Exception ex){
                return;
            }

        }
        ensemble = ensemble.t(); // size N * L
        Mat SKMat = new Mat();
        Core.gemm(ensemble.t(), ensemble, 1, new Mat(), 0, SKMat); // this computes image^T * image

        Mat eigValues = new Mat();
        Mat eigVectors = new Mat();
        Core.eigen(SKMat, eigValues, eigVectors);
        //topVectors = new Mat();
        //PCAMat = new Mat();
        Core.gemm(ensemble, eigVectors, 1, new Mat(), 0, topVectors);
        for(int i = 0; i < topVectors.width(); i++){
            Mat thisCol = topVectors.col(i);
            Core.normalize(thisCol, thisCol);
        }
        topVectors = topVectors.t(); // size L * N
        Core.gemm(topVectors, ensemble, 1, new Mat(), 0, PCAMat); // size L * L
    }

    public void computeDescriptor(List<Mat> databaseDescriptor){
        FeatureDetector detector = FeatureDetector.create(FeatureDetector.FAST);
        DescriptorExtractor descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        for(int i = 0; i < englishVocabulary.size(); i++){
            int resourceId = context.getResources().getIdentifier(englishVocabulary.get(i), "raw", context.getPackageName());
            try {
                Mat image = Utils.loadResource(this.context, resourceId, Imgcodecs.CV_LOAD_IMAGE_COLOR);
                MatOfKeyPoint objectKeyPoints = new MatOfKeyPoint();
                Mat objectDescriptor = new Mat();
                detector.detect(image, objectKeyPoints);
                descriptorExtractor.compute(image, objectKeyPoints, objectDescriptor);
                databaseDescriptor.add(objectDescriptor);
            }
            catch(Exception ex){
                return;
            }
        }
    }

    // TODO, data based needs to be implemented
    /** this is the dictionary for the English words
       each line should have the format: all lower letter
       any input with upper case will finally be converted to lower case
     **/
    private List<String> englishVocabulary = Arrays.asList(
            "exit",
            "entrance",
            "stop",
            "arrivals",
            "departures",
            "baggage",
            "checkin",
            "information",
            "lostfound",
            "moneyexchange",
            "restroom",
            "security",
            "custom");
    /** this is the dictionary for the corresponding Chinese words
       each line should have the Chinese translation for the corresponding lines of the englishVocabulary
     **/
    private List<String> chineseVocabulary = Arrays.asList(
            "出口",
            "入口",
            "停止",
            "到达层",
            "出发层",
            "行李",
            "登记",
            "信息咨询",
            "失物招领",
            "货币兑换",
            "卫生间",
            "安检",
            "海关");

}
