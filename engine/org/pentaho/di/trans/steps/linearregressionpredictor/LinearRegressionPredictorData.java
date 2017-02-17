package org.pentaho.di.trans.steps.linearregressionpredictor;

import org.pentaho.di.core.RowSet;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by thomasyngli on 2017/2/16.
 */
public class LinearRegressionPredictorData extends BaseStepData implements StepDataInterface {
    public Object[] weights;
    public RowMetaInterface weightMeta;
    public List<RowMetaInterface> featureMetas;

    public RowMetaInterface outputRowMeta;
    public List<List<Object[]>> features;

    public List<RowSet> featureRowSets;
    public RowSet weightRowSet;

    public String targetField;

    /**
     * Default initializer
     */
    public LinearRegressionPredictorData() {
        super();
        weights = null;
        featureMetas = new ArrayList<RowMetaInterface>();
        features = new ArrayList<List<Object[]>>();
        featureRowSets = new ArrayList<RowSet>();
    }
}