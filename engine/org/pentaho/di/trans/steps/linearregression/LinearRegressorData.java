/**
 * Created by thomasyngli on 2017/2/5.
 */

package org.pentaho.di.trans.steps.linearregression;

import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

import java.io.BufferedWriter;
import java.util.List;

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

    BufferedWriter bufferedWriter;

    public String weightString;
    public String targetField;

    public LinearRegressorData() {
        super();
    }

}
