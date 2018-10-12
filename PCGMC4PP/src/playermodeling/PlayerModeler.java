package playermodeling;

import weka.classifiers.Classifier;
import weka.core.*;

import java.io.File;
import java.util.*;

public abstract class PlayerModeler {

    public static final int DEBUG = 0;
    public static final String PLAYER_MODELING_DATA_DIR = "player_modeling_files/";

    /* Player-modeling specific variables */
    protected double interval;
    protected int skill_vector_update_technique_flag;

    /* Machine Learning-specific variables */
    protected Classifier classifier;
    protected Instances training_dataset;
    protected ArrayList<Attribute> attributes;
    protected ArrayList<String> annotation_values;

    public PlayerModeler() {}

    public PlayerModeler(Classifier cls, double interval_, int u_technique_flag) {
        interval = interval_;

        classifier = cls;
        skill_vector_update_technique_flag = u_technique_flag;

        /* Setup attributes */
        attributes = new ArrayList<Attribute>();
        for (int i = 0; i < FeatureExtraction.features.length; i++) {
            attributes.add(new Attribute(FeatureExtraction.features[i]));
        }

        annotation_values = new ArrayList<String>();
        annotation_values.add("A");
        annotation_values.add("B");
        annotation_values.add("C");
        attributes.add(new Attribute("annotations", annotation_values));
    }

    protected void saveTrainingModel() {
        try {
            String directory_name = PLAYER_MODELING_DATA_DIR + "saved_models/";
            new File(directory_name).mkdir();
            weka.core.SerializationHelper.write( directory_name + String.format("%s.%d.model", classifier.getClass().getName(), (int)interval), classifier);
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    protected void readTrainingModel(String filename) {
        try {
            classifier = (Classifier) weka.core.SerializationHelper.read(filename);
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    protected void trainModel() {
        try {
            classifier.buildClassifier(training_dataset);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected double classifyInstance(HashMap<String, Double> feature_vector) {
        if ( DEBUG > 0 ) {
            System.out.println("-------------------Create and classifying instance-------------------");
        }
        Instance new_instance = new DenseInstance(feature_vector.size() + 1);

        for (Attribute a : attributes) {
            if (!a.name().equals("annotations")) {
                new_instance.setValue(a, feature_vector.get(a.name()));
            }
        }
        new_instance.setDataset(training_dataset);

        try {
            double classification = classifier.classifyInstance(new_instance);
            return classification;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public abstract void execute();
}
