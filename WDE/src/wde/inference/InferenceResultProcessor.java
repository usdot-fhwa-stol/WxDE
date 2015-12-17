package wde.inference;

public interface InferenceResultProcessor<T extends InferenceResult> {
    void process(T result);
}
