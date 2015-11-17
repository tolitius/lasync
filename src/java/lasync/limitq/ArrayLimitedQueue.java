package lasync.limitq;

import java.util.concurrent.ArrayBlockingQueue;

public class ArrayLimitedQueue<E> extends ArrayBlockingQueue<E> {

    public ArrayLimitedQueue(int capacity) {
        super(capacity);
    }

    @Override
    public boolean offer(E e) {
        try {
            put(e);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
        return true;
    }
}
