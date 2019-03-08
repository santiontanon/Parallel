package valls.util;

import java.util.List;

public class MathUtils {
    public static int ipow(int base, int exp) {
        int result = 1;
        while (exp > 0) {
            if ((exp & 1) != 0) {
                result *= base;
            }
            exp >>= 1;
            base *= base;
        }

        return result;
    }
    public static Double average(List<Integer> lst){
          if(lst.isEmpty()){
            return 0.0;
        }
        int sum = 0;
        for(int i:lst){
            sum+=i;
        }
        return (double) sum / (double) lst.size();
    }
}
