package metro4j;

import org.json.JSONArray;
import org.json.JSONTokener;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import static java.lang.System.out;

/**
 * @author jschultz
 */
public class MetroSelfTest {

    public static double[] toDoubleArray(JSONArray array) {
        double result[] = new double[array.length()];

        for (int i = 0; i < array.length(); ++i) {
            result[i] = array.getDouble(i);
        }

        return result;
    }

    public static long[] toLongArray(JSONArray array) {
        long result[] = new long[array.length()];

        for (int i = 0; i < array.length(); ++i) {
            result[i] = array.getLong(i);
        }

        return result;
    }

    public static boolean[] toBooleanArray(JSONArray array) {
        boolean result[] = new boolean[array.length()];

        for (int i = 0; i < array.length(); ++i) {
            result[i] = array.getBoolean(i);
        }

        return result;
    }
    
    public static void main(String[] args) {
        try {

            InputStream istream = new BufferedInputStream(new FileInputStream("data.json"));

            JSONTokener tokenizer = new JSONTokener(istream);
            JSONArray a = new JSONArray(tokenizer);

            int size = a.length();
            out.println("Array size: " + size);

            Metro4J.Do_Metro(a.getBoolean(0), a.getDouble(1), a.getDouble(2), toDoubleArray(a.getJSONArray(3)), a.getLong(4), toLongArray(a.getJSONArray(5)), toDoubleArray(a.getJSONArray(6)), toDoubleArray(a.getJSONArray(7)), toDoubleArray(a.getJSONArray(8)), toDoubleArray(a.getJSONArray(9)), toDoubleArray(a.getJSONArray(10)), toDoubleArray(a.getJSONArray(11)), toDoubleArray(a.getJSONArray(12)), toDoubleArray(a.getJSONArray(13)), toDoubleArray(a.getJSONArray(14)), toDoubleArray(a.getJSONArray(15)), toDoubleArray(a.getJSONArray(16)), toDoubleArray(a.getJSONArray(17)), toDoubleArray(a.getJSONArray(18)), toDoubleArray(a.getJSONArray(19)), toLongArray(a.getJSONArray(20)), toBooleanArray(a.getJSONArray(21)), a.getDouble(22), a.getLong(23), a.getLong(24), a.getBoolean(25), a.getLong(26));


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
