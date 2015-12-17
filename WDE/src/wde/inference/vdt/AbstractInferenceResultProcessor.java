package wde.inference.vdt;

import wde.inference.InferenceResult;
import wde.inference.InferenceResultProcessor;

public abstract class AbstractInferenceResultProcessor<T extends InferenceResult> implements InferenceResultProcessor<T> {

    public abstract void process(T result);
}
