/**
 * @file ILockFactory.java
 */
package util.threads;

/**
 * Implementations of this class can be "plugged in" to instances of
 * {@link StripeLock} to allow mutually exclusive access to critical sections
 * of code pertaining to objects of type {@code T}.
 * <p>
 * Interface class. Implementations must fully define {@code ILockFactory}
 * methods.
 * </p>
 * @param <T> Template type. Implementations must specify a
 * concrete type at declaration time in place of type: T.
 */
public interface ILockFactory<T>
{
	/**
     * All implementations of {@code ILockFactory} must define this method.
     * <p>
     * This is used to add a container of lockable objects of type {@code T}
     * to the {@link StripeLock} Mutex.
     * </p>
     *
     * @return A new instance of {@code T}
     *
     * @see StripeLock
     */
	public T getLock();
}
