package wde.inference;

import org.apache.log4j.Logger;

public abstract class AbstractInferencer<T extends ObservationResolver> implements Inferencer<T> {

    private final Logger logger = Logger.getLogger(this.getClass());

    public static final float FILL_VALUE = -9999.0f;

    public static final String DEFAULT_NAME = "Inferencer";

    private String name = DEFAULT_NAME;
//    private ObservationProcessor observationProcessor;
//    private HashSet<String> dependencies = new HashSet<>();

//    public AbstractInferencer(ObservationProcessor processor) {
//        this.observationProcessor = processor;
//    }

    public String getName() {
        return this.name;
    }

    protected Logger getLogger() {
        return this.logger;
    }

//    public String[] getObsTypeDependencies() {
//        return (String[])this.dependencies.toArray();
//    }

//    protected void addObsTypeDependency(String obsTypeName) {
//        this.dependencies.add(obsTypeName);
//    }

    public abstract InferenceResult doInference(T resolver);
}
