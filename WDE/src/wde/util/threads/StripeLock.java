/**
 * StripeLock.java
 */
package wde.util.threads;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Allows mutually exclusive access to pre-allocated instances of type
 * {@code T} objects.
 * <p/>
 * <p>
 * To use {@code StripeLock} {@code T} must implement {@link ILockFactory},
 * and provide a definition for {@link ILockFactory#getLock()} that returns
 * a new instance of {@code T}.
 * </p>
 * <p/>
 * <p>
 * Use of this mutex follows that of most read/write lock patterns:
 * <pre> {@code readLock();} </pre>
 * <pre>    {@code // critical section} </pre>
 * <pre> {@code readUnlock();} </pre>
 * <pre> {@code writeLock();} </pre>
 * <pre>    {@code // critical section} </pre>
 * <pre> {@code writeUnlock();} </pre>
 * </p>
 *
 * @param <T> Template type. Instances of {@code StripeLock} must specify a
 *            concrete type at declaration time in place of type: T.
 * @see ReentrantLock
 */
public class StripeLock<T> {
    /**
     * Container of lockable objects.
     */
    private ArrayList<TypeLock<T>> m_oLocks;


    /**
     * Creates new instances of {@code StripeLock}
     */
    protected StripeLock() {
    }


    /**
     * <b> Constructor </b>
     * <p>
     * Initializes the container of locks to contain the provided number of
     * pre-allocated lockable objects.
     * </p>
     *
     * @param iLockFactory factory providing the objects to lock.
     * @param nLockCount   number of locks to provide this lock container.
     */
    public StripeLock(ILockFactory<T> iLockFactory, int nLockCount) {
        m_oLocks = new ArrayList<TypeLock<T>>(nLockCount);

        while (nLockCount-- > 0)
            m_oLocks.add(new TypeLock<T>(iLockFactory.getLock()));
    }


    /**
     * Calculates the index of the lock to use based off the id of the current
     * thread to help prevent two threads from attempting to obtain the same
     * lock.
     *
     * @return index of the lock
     */
    private int getIndex() {
        return (int) (Thread.currentThread().getId() % m_oLocks.size());
    }

    /**
     * If not held by another thread, this method obtains the lock assigned to
     * the calling thread on one of the pre-allocated objects contained in the
     * lock-list. Blocks other threads from accessing the critical section
     * following the call to this method.
     * <p>
     * The thread holds the lock until {@link StripeLock#readUnlock()} is
     * called.
     * </p>
     *
     * @return A pre-allocated instance of type {@code T}.
     * @see ReentrantLock#lock()
     */
    public T readLock() {
        TypeLock<T> oLock = m_oLocks.get(getIndex());
        oLock.lock();
        return oLock.getType();
    }

    public int acquireReadLock() {
        int index = getIndex();
        TypeLock<T> oLock = m_oLocks.get(index);
        oLock.lock();
        return index;
    }

    /**
     * Releases the lock assigned to the current thread.
     *
     * @see ReentrantLock#unlock()
     */
    public void readUnlock() {
        TypeLock<T> oLock = m_oLocks.get(getIndex());
        oLock.unlock();
    }


    /**
     * Obtains all locks to prevent all readers from from accessing the
     * critical section until {@link StripeLock#writeUnlock()} is called.
     *
     * @return A pre-allocated instance of type {@code T}.
     */
    public synchronized T writeLock() {
        // first lock the shared object used by the client
        int nCurrentIndex = getIndex();
        TypeLock<T> oLock = m_oLocks.get(nCurrentIndex);
        oLock.lock();

        // the write lock must acquire the remaining locks
        int nIndex = m_oLocks.size();
        while (nIndex-- > 0) {
            if (nIndex != nCurrentIndex)
                m_oLocks.get(nIndex).lock();
        }

        return oLock.getType();
    }


    /**
     * Releases all locks from the locks list.
     */
    public void writeUnlock() {
        // unlock all available locks
        int nIndex = m_oLocks.size();
        while (nIndex-- > 0)
            m_oLocks.get(nIndex).unlock();
    }

    /**
     * Extends {@link ReentrantLock} providing synchronization, and mutual
     * exclusion for the provided shared object type.
     */
    private class TypeLock<T> extends ReentrantLock {
        /**
         * "Pointer" to the object type being locked.
         */
        private T m_oType;


        /**
         * <b> Default Constructor </b>
         * <p>
         * Creates new instances of {@code TypeLock}
         * </p>
         */
        private TypeLock() {
        }


        /**
         * Sets the object type "pointer."
         *
         * @param oType object of the type to be locked.
         */
        private TypeLock(T oType) {
            m_oType = oType;
        }


        /**
         * <b> Accessor </b>
         *
         * @return The object type "pointer."
         */
        private T getType() {
            return m_oType;
        }
    }
}
