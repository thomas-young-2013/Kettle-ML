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
    public List<Object[]> buffer;
    public int getBufferIndex;

    public RowMetaInterface outputRowMeta;

    public int freeCounter;

    // the index of target field.
    public int targetIndex;

    // the number of field, including the target and features.
    public int fieldnrs;

    // store the weights(delta)
    public Object[] weights;

    public LinearRegressorData() {
        super();
    }

}
