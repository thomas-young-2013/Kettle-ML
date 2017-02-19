package org.pentaho.di.bascis;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by thomasyngli on 2017/2/19.
 */
public class BasicUtils {

    BasicUtils() {}

    /*
    * @params: none
    * @return: current time in string yyyy-MM-dd HH:mm:ss
    * */
    public static String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String val = "";
        try {
            val = sdf.format(new Date());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return val;
    }
}
