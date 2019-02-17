package playermodeling;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import weka.classifiers.Classifier;
import weka.core.*;

import java.io.File;
import java.util.*;

public abstract class AbstractPlayerModeler {

    public static final String PLAYER_MODELING_DATA_DIR = "pmfiles/"; /* TODO: This needs to be replaceable.  */
    private static final Logger logger = LogManager.getLogger(AbstractPlayerModeler.class);

    /* Player-modeling specific variables */
    protected double interval;
    protected int skillVectorUpdateTechniqueFlag;

    /* Machine Learning-specific variables */
    protected Classifier classifier;
    protected Instances trainingDataset;
    protected ArrayList<Attribute> attributes;
    protected ArrayList<String> annotationValues;

    public AbstractPlayerModeler() {}

    public AbstractPlayerModeler(Classifier cls, double interval_, int uTechniqueFlag) {
        interval = interval_;

        classifier = cls;
        skillVectorUpdateTechniqueFlag = uTechniqueFlag;

        /* Setup attributes */
        attributes = new ArrayList<Attribute>();
        for (int i = 0; i < FeatureExtractor.features.length; i++) {
            attributes.add(new Attribute(FeatureExtractor.features[i]));
        }
        annotationValues = new ArrayList<String>();
        annotationValues.add("A");
        annotationValues.add("B");
        annotationValues.add("C");
        attributes.add(new Attribute("annotations", annotationValues));
    }

    protected void saveTrainingModel(String playerModelingDirectory) {
        String directoryName = playerModelingDirectory + File.separator + "saved_models" + File.separator;
        String modelFileName = String.format("%s.%d.model", classifier.getClass().getName(), (int)interval);
        logger.info("Saving training model to: " + directoryName + modelFileName);
        try {
            new File(directoryName).mkdir();
            weka.core.SerializationHelper.write( directoryName + modelFileName, classifier);
            logger.info("Training model successfully saved to: " + directoryName + modelFileName);
        } catch ( Exception e ) {
            logger.error("Unable to save training model (either file format error or permission issue with saving)");
            logger.catching(e);
        }

    }

    protected void saveTrainingModel() {
        String directoryName = PLAYER_MODELING_DATA_DIR + "saved_models" + File.separator;
        String modelFileName = String.format("%s.%d.model", classifier.getClass().getName(), (int)interval);

        logger.info("Saving training model to: " + directoryName + modelFileName);
        try {
            new File(directoryName).mkdir();
            weka.core.SerializationHelper.write( directoryName + modelFileName, classifier);
            logger.info("Training model successfully saved to: " + directoryName + modelFileName);
        } catch (Exception e) {
            logger.error("Unable to save training model (either file format error or permission issue with saving)");
            logger.catching(e);
        }
    }

    protected void readTrainingModel(String playerModelingDirectory, String filename) {
        String modelFilepath = playerModelingDirectory + File.separator + filename;
        logger.info("Reading training model from: " + modelFilepath);

        try {
            classifier = (Classifier) weka.core.SerializationHelper.read(modelFilepath);
            logger.info("Training model successfully read from: " + modelFilepath);
        } catch ( Exception e ) {
            logger.fatal("Unable to read training model (either file format error or permission issue with reading)");
            logger.catching(Level.FATAL, e);
            System.exit(1);
        }
    }

    protected void readTrainingModel(String filename) {
        logger.info("Reading training model from: " + filename);
        try {
            classifier = (Classifier) weka.core.SerializationHelper.read(filename);
            logger.info("Training model successfully read from: " + filename);
        } catch ( Exception e ) {
            logger.fatal("Unable to read training model (either file format error or permission issue with reading)");
            logger.catching(Level.FATAL, e);
            System.exit(1);
        }
    }

    protected void trainModel() {
        logger.info("Training machine learning model");

        try {
            classifier.buildClassifier(trainingDataset);
            logger.info("Machine learning model successfully trained!");
        } catch (Exception e) {
            logger.fatal("Unable to train machine learning model.");
            logger.catching(Level.FATAL, e);
            System.exit(1);
        }
    }

    protected double classifyInstance(HashMap<String, Double> feature_vector) {
        logger.info("-------------------Create and classifying instance-------------------");

        Instance new_instance = new DenseInstance(FeatureExtractor.features.length + 1);
        for (Attribute a : attributes) {
            if (!a.name().equals("annotations")) {
                new_instance.setValue(a, feature_vector.get(a.name()));
            }
        }
        new_instance.setDataset(trainingDataset);
        logger.info("Classifying instance!");
        try {
            double classification = classifier.classifyInstance(new_instance);
            logger.info("Instance classified instance!");
            return classification;
        } catch (Exception e) {
            logger.fatal("Unable to classify instance due to error. Returning classification value of 0.0");
            logger.catching(Level.FATAL, e);
            System.exit(1);
            return 0.0;
        }
    }

    public abstract void execute();
}
