package valls.util;

public class SortablePair<T1 extends Comparable, T2 extends Comparable> implements Comparable {
  public T1 m_a;
    public T2 m_b;

    public SortablePair(T1 a,T2 b) {
        m_a = a;
        m_b = b;
    }
    
    public boolean equals(Object o) {
        if (!(o instanceof Pair)) return false;
        if (m_a==null && ((Pair)o).m_a!=null) return false;
        if (m_b==null && ((Pair)o).m_b!=null) return false;
        if (!m_a.equals(((Pair)o).m_a)) return false;
        if (!m_b.equals(((Pair)o).m_b)) return false;
        return true;
    }
    
    public String toString() {
        return "<" + m_a + "," + m_b + ">";
    }

    public int hashCode() {
        return 31 * m_a.hashCode() + m_b.hashCode();
        // http://stackoverflow.com/questions/299304/why-does-javas-hashcode-in-string-use-31-as-a-multiplier
    }

    @Override
    public int compareTo(Object o) {
        if(o instanceof SortablePair){
            SortablePair p = (SortablePair) o;
            if(this.m_a.equals(p.m_a)){
                return this.m_b.compareTo(p.m_b);
            } else {
                return this.m_a.compareTo(p.m_a);
            }
        } else {
            throw new UnsupportedOperationException("Cannot compare to Pair.");
        }
    }
}
