package wde.inference.vdt;

public class ValueHolder<T> {

    private T value;

    public ValueHolder(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public static <T> ValueHolder<T> from(T value) {
        return new ValueHolder<>(value);
    }
}
