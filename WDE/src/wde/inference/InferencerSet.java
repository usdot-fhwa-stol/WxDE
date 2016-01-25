package wde.inference;

import java.util.HashSet;

public class InferencerSet extends HashSet<Inferencer> {

    public InferencerSet() {

    }

    public InferencerSet(Inferencer[] inferencers) {
        for (Inferencer inferencer : inferencers) {
            if (inferencer == null)
                throw new NullPointerException("inferencers");

            this.add(inferencer);
        }
    }
}
