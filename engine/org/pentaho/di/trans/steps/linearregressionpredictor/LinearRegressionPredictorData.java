package org.pentaho.di.trans.steps.linearregressionpredictor;

import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

import java.util.List;

/**
 * Created by thomasyngli on 2017/2/16.
 */
public class LinearRegressionPredictorData extends BaseStepData implements StepDataInterface {
    public Object[] weights;
    public RowMetaInterface weightMeta, featureMeta;

    public RowMetaInterface outputRowMeta;
    public List<Object[]> features;

    /**
     * Default initializer
     */
    public LinearRegressionPredictorData() {
        super();
    }
}