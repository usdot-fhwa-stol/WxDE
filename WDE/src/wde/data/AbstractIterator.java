package wde.data;

import java.util.Iterator;

public abstract class AbstractIterator<T> implements Iterator<T> {
    @Override
    public void remove() {
        throw new UnsupportedOperationException("remove");
    }
}
