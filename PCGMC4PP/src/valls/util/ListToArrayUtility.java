/*
 * The MIT License
 *
 * Copyright 2016 Josep Valls-Vargas <josep@valls.name>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package valls.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Josep Valls-Vargas <josep@valls.name>
 */
public class ListToArrayUtility {
    public static int[] toIntArray(List<Integer> lst){
        int[] o = new int[lst.size()];
        for(int i=0;i<lst.size();i++){
            o[i]=lst.get(i);
        }
        return o;
    }
    public static List<Integer> toIntegerList(int[] arr){
        List<Integer> lst = new ArrayList();
        for(int i=0;i<arr.length;i++){
            lst.add(arr[i]);
        }
        return lst;
    }
    
    public static boolean intInIntArray(int[] arr, int c){
        for(int i=0;i<arr.length;i++){
            if(arr[i]==c) return true;
        }
        return false;
    }
    
    public static boolean compareArrays(int[] array1, int[] array2) {
        if (array1 != null && array2 != null){
          if (array1.length != array2.length)
              return false;
          else
              for (int i = 0; i < array2.length; i++) {
                  if (array2[i] != array1[i]) {
                      return false;
                  }                 
            }
        }else if (!(array1 == null && array2 == null)){
          return false;
        }
        return true;
    }
    
    public static <K,V> Map<V,K> swapMapKeysValues(Map<K,V> map) {
    LinkedHashMap<V,K> rev = new LinkedHashMap<V, K>();
    for(Map.Entry<K,V> entry : map.entrySet())
        rev.put(entry.getValue(), entry.getKey());
    return rev;
}
    
}
