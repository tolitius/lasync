package lasync.limitq;

import java.util.concurrent.LinkedBlockingDeque;

public class LinkedLimitedQueue<E> extends LinkedBlockingDeque<E> {

    public LinkedLimitedQueue(int capacity) {
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
