package org.pentaho.di.trans.steps.linearregressionpredictor;

import org.pentaho.di.core.RowSet;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaNumber;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.*;
import org.pentaho.di.trans.step.errorhandling.StreamInterface;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by thomasyngli on 2017/2/16.
 */
public class LinearRegressionPredictor extends BaseStep implements StepInterface {
    private static Class<?> PKG = LinearRegressionPredictor.class; // for i18n

    private LinearRegressionPredictorMeta meta;
    private LinearRegressionPredictorData data;

    public LinearRegressionPredictor(StepMeta stepMeta, StepDataInterface stepDataInterface,
                           int copyNr, TransMeta transMeta, Trans trans ) {
        super( stepMeta, stepDataInterface, copyNr, transMeta, trans );
    }

    @Override
    public boolean processRow(StepMetaInterface smi, StepDataInterface sdi ) throws KettleException {
        if (first) {
            this.first = false;
            /*
             * init the weight meta and features meta
             * init the rowsets
             */
            List<RowSet> rowsets = getInputRowSets();
            for (RowSet rowSet: rowsets) {
                if (rowSet.getOriginStepName().equalsIgnoreCase(meta.getTrainStep())) {
                    data.weightRowSet = rowSet;
                    data.weightMeta = rowSet.getRowMeta();
                    // set the target field.
                    data.targetField = data.weightMeta.getFieldNames()[data.weightMeta.getFieldNames().length-1];
                } else {
                    data.featureMetas.add( rowSet.getRowMeta() );
                    data.featureRowSets.add(rowSet);
                }
            }
            // set the output meta
            RowMeta rowMeta = new RowMeta();
            rowMeta.addValueMeta(new ValueMetaNumber("constant_field", 10, 6));

            for (String fieldName: meta.getFieldName()) {
                if (!fieldName.equals(data.targetField)) {
                    rowMeta.addValueMeta(new ValueMetaNumber(fieldName, 10, 6));
                }
            }
            rowMeta.addValueMeta(new ValueMetaNumber(data.targetField, 10, 6));
            data.outputRowMeta = rowMeta;
            meta.getFields( data.outputRowMeta, getStepname(), null, null, this, repository, metaStore );

        }
        if (data.weights == null) {
            data.weights = getRowFrom(data.weightRowSet);
        }
        boolean endedFlag = false;
        for (int i=0; i<data.featureRowSets.size(); i++) {
            Object [] row = getRowFrom(data.featureRowSets.get(i));
            if (row != null) {
                endedFlag = true;
                data.features.get(i).add(row);
            }
        }

        if (!endedFlag) {
            /* let's starting predicting the result.
             * predict algorithm here.
             * put the result to the next step: using putrow
             */

            clearBuffers();
            this.setOutputDone();
            return false;
        }

        return true;
    }

    @Override
    public boolean init( StepMetaInterface smi, StepDataInterface sdi ) {
        meta = (LinearRegressionPredictorMeta) smi;
        data = (LinearRegressionPredictorData) sdi;

        if ( !super.init( smi, sdi ) ) {
            return false;
        }

        // In memory buffer
        //
        data.features = new ArrayList<List<Object[]>>(2000);
        return true;
    }

    @Override
    public void dispose( StepMetaInterface smi, StepDataInterface sdi ) {
        clearBuffers();
        super.dispose( smi, sdi );
    }

    private void clearBuffers() {
        // Clean out the buffer\
        data.features.clear();
    }

    /**
     * Calling this method will alert the step that we finished passing records to the step. Specifically for steps like
     * "Sort Rows" it means that the buffered rows can be sorted and passed on.
     */
    @Override
    public void batchComplete() throws KettleException {
        setOutputDone();
    }
}
