package playermodeling;

/**
 * Created by pavankantharaju on 3/1/18.
 */
public class Pair<T1,T2> {

    public T1 p1;
    public T2 p2;

    public Pair() {
        p1 = null;
        p2 = null;
    }

    public Pair(T1 x, T2 y) {
        p1 = x;
        p2 = y;
    }

    public void reset() {
        p1 = null;
        p2 = null;
    }

}
