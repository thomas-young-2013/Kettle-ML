package org.pentaho.di.trans.steps.linearregressionpredictor;

import org.pentaho.di.core.RowSet;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

import java.util.List;

/**
 * Created by thomasyngli on 2017/2/16.
 */
public class LinearRegressionPredictorData extends BaseStepData implements StepDataInterface {
    public Object[] one, two;
    public RowMetaInterface oneMeta, twoMeta;

    public RowMetaInterface outputRowMeta;
    public List<Object[]> ones, twos;

    public int[] keyNrs1;
    public int[] keyNrs2;

    public RowSet oneRowSet;
    public RowSet twoRowSet;

    /**
     * Default initializer
     */
    public LinearRegressionPredictorData() {
        super();
        ones = null;
        twos = null;

        keyNrs1 = null;
        keyNrs2 = null;
    }
}