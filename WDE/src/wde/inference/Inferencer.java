package wde.inference;

public interface Inferencer<T extends ObservationResolver> {

    String getName();

    InferenceResult doInference(T resolver);
}
