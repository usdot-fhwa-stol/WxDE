package wde.inference;

public class InferencerProcessorMapping<I extends Inferencer, P extends InferenceResultProcessor> {

    private final I inferencer;
    private final P resultProcessor;

    public InferencerProcessorMapping(I inferencer, P resultProcessor) {
        this.inferencer = inferencer;
        this.resultProcessor = resultProcessor;
    }

    public I getInferencer() {
        return inferencer;
    }

    public P getResultProcessor() {
        return resultProcessor;
    }
}
