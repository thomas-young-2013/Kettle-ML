package org.pentaho.di.bascis;

import java.io.BufferedWriter;
import java.io.FileWriter;

/**
 * Created by thomasyngli on 2017/2/19.
 */
public class FileUtils {
    private static BufferedWriter bufferedWriter;

    /* save string to file
    * @params: file full name, string content and is appended to the file tail
    * @return: void
    * */
    public static void saveFile(String filename, String content, boolean isAppended) {

        // save the weight value to file
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(filename, isAppended));
            bufferedWriter.write(content);
            bufferedWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.print("Problems aroused in file saving processs");
        }
    }
}
