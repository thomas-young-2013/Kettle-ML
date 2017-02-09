/**
 * Created by thomasyngli on 2017/2/5.
 */

package org.pentaho.di.trans.steps.linearregression;

import org.apache.commons.vfs2.FileObject;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.steps.sort.RowTempFile;

import java.io.DataInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class LinearRegressorData extends BaseStepData implements StepDataInterface {
    public List<FileObject> files;
    public List<Object[]> buffer;
    public int getBufferIndex;

    public List<InputStream> fis;
    public List<GZIPInputStream> gzis;
    public List<DataInputStream> dis;
    public List<Object[]> rowbuffer;
    public List<Integer> bufferSizes;

    // To store rows and file references
    public List<RowTempFile> tempRows;

    public int[] fieldnrs; // the corresponding field numbers;
    public FileObject fil;
    public RowMetaInterface outputRowMeta;

    public int freeCounter;

    /*
     * Group Fields Implementation heroic
     */
    public Object[] previous;
    public int[] groupnrs;
    public boolean newBatch;

    public LinearRegressorData() {
        super();

        files = new ArrayList<FileObject>();
        fis = new ArrayList<InputStream>();
        gzis = new ArrayList<GZIPInputStream>();
        dis = new ArrayList<DataInputStream>();
        bufferSizes = new ArrayList<Integer>();

        previous = null; // Heroic
    }

}
