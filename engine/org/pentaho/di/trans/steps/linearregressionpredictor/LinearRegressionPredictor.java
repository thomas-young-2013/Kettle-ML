package org.pentaho.di.trans.steps.linearregressionpredictor;

import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.*;

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
        // wait for first for is available
        Object[] r = getRow();

        List<String> groupFields = null;

        if (first) {
            this.first = false;

            // do we have any row at start processing?
            if (r == null) {
                // seems that we don't
                this.setOutputDone();
                return false;
            }
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
        data.features = new ArrayList<Object[]>( 2000 );
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
