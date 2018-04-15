package com.github.chen0040.objdetect;

import com.github.chen0040.objdetect.models.Box;
import com.github.chen0040.objdetect.models.DetectedObj;
import com.github.chen0040.objdetect.utils.FileUtils;
import com.github.chen0040.objdetect.utils.LabelUtils;
import com.github.chen0040.objdetect.utils.TensorUtils;
import lombok.Getter;
import lombok.Setter;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Tensor;
import org.tensorflow.types.UInt8;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ObjectDetector implements AutoCloseable {

    private String modelParentDirPath = "/tmp";
    private static final String MODEL_FOLDER_NAME = "ssd_inception_v2_coco";
    private String[] labels;
    private SavedModelBundle model;

    private static final Logger logger = LoggerFactory.getLogger(ObjectDetector.class);

    public ObjectDetector() {

    }

    public List<DetectedObj> detectObjects(BufferedImage img) throws IOException {

        logger.info("begin detecting objects from image ...");
        List<DetectedObj> result = new ArrayList<>();

        List<Tensor<?>> outputs = null;
        try (Tensor<UInt8> input = TensorUtils.makeImageTensor(img)) {
            outputs =
                    model
                            .session()
                            .runner()
                            .feed("image_tensor", input)
                            .fetch("detection_scores")
                            .fetch("detection_classes")
                            .fetch("detection_boxes")
                            .run();
        }
        try (Tensor<Float> scoresT = outputs.get(0).expect(Float.class);
             Tensor<Float> classesT = outputs.get(1).expect(Float.class);
             Tensor<Float> boxesT = outputs.get(2).expect(Float.class)) {
            // All these tensors have:
            // - 1 as the first dimension
            // - maxObjects as the second dimension
            // While boxesT will have 4 as the third dimension (2 sets of (x, y) coordinates).
            // This can be verified by looking at scoresT.shape() etc.
            int maxObjects = (int) scoresT.shape()[1];
            float[] scores = scoresT.copyTo(new float[1][maxObjects])[0];
            float[] classes = classesT.copyTo(new float[1][maxObjects])[0];
            float[][] boxes = boxesT.copyTo(new float[1][maxObjects][4])[0];
            for (int i = 0; i < scores.length; ++i) {
                if (scores[i] < 0.5) {
                    continue;
                }
                String label = labels[(int) classes[i]];
                float score = scores[i];
                float[] box = boxes[i];

                DetectedObj detectedObj = new DetectedObj(label,score, box);
                result.add(detectedObj);
            }
        }

        logger.info("object detection completed on image");

        return result;
    }

    public void loadModel() throws Exception {
        exportModel();
        labels = LabelUtils.loadLabels();
        String modelDirPath = getModelDirPath();
        logger.info("loading model from {} ...", modelDirPath);
        this.model = SavedModelBundle.load(modelDirPath, "serve");
        logger.info("model loaded");
    }

    public String getModelDirPath() {
        File modelParentDir = new File(modelParentDirPath);
        return modelParentDir.getAbsolutePath() + "/" + MODEL_FOLDER_NAME;
    }



    private void exportModel() {
        File modelParentDir = new File(modelParentDirPath);
        if(!modelParentDir.exists()){
            modelParentDir.mkdir();
        }

        String modelDirPath = modelParentDir.getAbsolutePath() + "/" + MODEL_FOLDER_NAME;

        String modelPath = modelDirPath + "/saved_model.pb";

        File modelFile = new File(modelPath);

        if(modelFile.exists()){
            return;
        }

        File modelDir = new File(modelDirPath);
        if(!modelDir.exists()) {
            modelDir.mkdir();
        }


        String zipFileName = modelParentDir.getAbsolutePath() + "/saved_model.zip";
        try {

            InputStream inStream = FileUtils.getResource("tf_models/saved_model.zip");

            FileOutputStream outStream = new FileOutputStream(new File(zipFileName));

            byte[] buffer = new byte[1024];

            int length;
            while ((length = inStream.read(buffer)) > 0){
                outStream.write(buffer, 0, length);
            }

            inStream.close();
            outStream.close();

            ZipFile zipFile = new ZipFile(zipFileName);
            zipFile.extractAll(modelDirPath);
        }
        catch (IOException e) {
            logger.error("Failed to copy the saved_model.zip from resources to " + modelParentDirPath, e);
        }
        catch (ZipException e) {
            logger.error("Failed to unzip " + zipFileName, e);
        }
    }




    @Override
    public void close() throws Exception {
        if(model != null) {
            model.close();
            model = null;
        }
    }

    public BufferedImage drawDetectedObjects(BufferedImage img) {
        List<DetectedObj> objList;
        try {
            objList = detectObjects(img);
        } catch (IOException e) {
            logger.error("Failed to detect objects in image", e);
            objList = new ArrayList<>();
        }

        BufferedImage result = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());
        Graphics g = result.getGraphics();
        g.drawImage(img, 0, 0, null);

        g.setColor(Color.red);

        for(DetectedObj obj : objList){
            Box box = obj.getBox();
            int x = (int)(box.getLeft() * img.getWidth());
            int y = (int)(box.getTop() * img.getHeight());
            g.drawString(obj.getLabel(), x, y);
            int width = (int)(box.getWidth() * img.getWidth());
            int height = (int)(box.getHeight() * img.getHeight());
            g.drawRect(x, y, width, height);
        }

        return result;
    }
}
