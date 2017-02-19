/**
 * Created by thomasyngli on 2017/2/5.
 */

package org.pentaho.di.trans.steps.linearregression;

import Jama.Matrix;
import org.pentaho.di.bascis.FileUtils;
import org.pentaho.di.computation.MatrixUtils;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaNumber;
import org.pentaho.di.core.row.value.ValueMetaString;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

import java.util.ArrayList;
import java.util.List;

/*
 * to do list:
 *   1. Do Type Checking
 *
 * */

public class LinearRegressor extends BaseStep implements StepInterface {
    private static Class<?> PKG = LinearRegressor.class; // for i18n

    private LinearRegressorMeta meta;
    private LinearRegressorData data;

    public LinearRegressor( StepMeta stepMeta, StepDataInterface stepDataInterface,
                     int copyNr, TransMeta transMeta, Trans trans ) {
        super( stepMeta, stepDataInterface, copyNr, transMeta, trans );
    }

    // run the linear regression algorithm.
    public void doLinearRegression() {

        // allocate matrix
        double [][] mat = new double[data.buffer.size()][data.fieldnrs];
        int rowcnt = 0;
        for (Object[] row: data.buffer) {
            int colcnt = 0;
            for (int i=0; i < data.fieldnrs; i++) {
                if (i != data.targetIndex) mat[rowcnt][colcnt++] = (double) row[i];
                else mat[rowcnt][data.fieldnrs-1] = (double) row[i];
            }
            rowcnt++;
        }

        // check the process.
        assert(data.buffer.size() == rowcnt);

        Matrix A = new Matrix(mat);

        int matrixRowDim = A.getRowDimension();
        int matrixColDim = A.getColumnDimension();

        // compute X matrix and do feature normalization.
        Matrix featureMat = A.getMatrix(0, matrixRowDim-1, 0, matrixColDim-2);
        boolean isRegu = false;
        Matrix tmpMat = featureMat;
        if (isRegu) tmpMat = MatrixUtils.featureNormalize(featureMat);

        // Matrix tmpMat = featureMat;
        Matrix X = new Matrix(matrixRowDim, matrixColDim, 1.0);
        X.setMatrix(0, matrixRowDim-1, 1, matrixColDim-1, tmpMat);

        Matrix y = A.getMatrix(0, matrixRowDim-1, new int[]{matrixColDim-1});

        Matrix theta = new Matrix(matrixColDim, 1);

        double alpha = meta.getLearningRate();
        int num_iters = meta.getIterationNum();
        // get display gap.
        boolean isDisplay = meta.getDisplay_iteration_gap() > 0;

        // gradient descent.
        for (int iter = 1; iter <= num_iters; iter++) {
            Matrix tmp = X.times(theta).minus(y);
            theta.minusEquals(X.transpose().times(tmp).times(alpha/matrixRowDim));

            // compute the cost in every iteration.
            double cost = tmp.transpose().times(tmp).get(0, 0)/(2*matrixRowDim);
            // System.out.println(cost);
            if (isDisplay && iter % meta.getDisplay_iteration_gap() == 0)
                logBasic( String.format("iteration %dth, the cost is %f", iter, cost ));
        }

        // do prediction.
        Matrix preMat = X.times(theta);
        // theta.print(1, 4);

        // compare the diff.
        if (meta.isCompared()) {
            double[][] arr1 = y.getArray();
            double[][] arr2 = preMat.getArray();
            for (int i = 0; i < matrixRowDim; i++) {
                // System.out.println(String.format("target:%f -> predict:%f", arr1[i][0], arr2[i][0]));
                logBasic( String.format("target:%f -> predict:%f", arr1[i][0], arr2[i][0]) );
            }
        }

        // save the weight value.
        double [][]thetaVal = theta.getArray();
        for (int i=0; i<data.fieldnrs; i++) data.weights[i] = thetaVal[i][0];

        if (meta.getWeightFileName() != null && meta.getWeightFileName() != "") {
            data.weightString = getWeightString();
            FileUtils.saveFile(meta.getWeightFileName(), data.weightString, false);
        }

    }

    public String getWeightString() {
        StringBuilder val = new StringBuilder(256);
        val.append("constant_field");
        RowMetaInterface inputRowMeta = getInputRowMeta();
        for ( int i = 0; i < inputRowMeta.getFieldNames().length; i++) {
            if( i != data.targetIndex )
                val.append("," + inputRowMeta.getFieldNames()[i]);
        }
        val.append(String.format(",%s\n", data.targetField));

        val.append(data.weights[0]);
        for (int i=1; i<data.fieldnrs; i++) {
            val.append(",");
            val.append(data.weights[i]);
        }
        val.append(",0\n");

        return val.toString();
    }

    @Override
    public boolean processRow( StepMetaInterface smi, StepDataInterface sdi ) throws KettleException {
        // wait for first for is available
        Object[] r = getRow();

        List<String> groupFields = null;

        if ( first ) {
            this.first = false;

            // do we have any row at start processing?
            if ( r == null ) {
                // seems that we don't
                this.setOutputDone();
                return false;
            }

            // set the target field's index.
            RowMetaInterface inputRowMeta = getInputRowMeta();
            // int featureFieldNumber = inputRowMeta.size();
            int featureFieldNumber = inputRowMeta.size();
            String targetFieldName = "";
            for (int i=0; i<featureFieldNumber; i++) {
                if (meta.getIsTarget()[i]) targetFieldName = meta.getFieldName()[i];
            }
            data.targetField = targetFieldName;

            // find target field index.
            for (int i = 0; i < inputRowMeta.getFieldNames().length; i++) {
                if (targetFieldName.equalsIgnoreCase(inputRowMeta.getFieldNames()[i])) data.targetIndex = i;
            }

            data.fieldnrs = featureFieldNumber;
            data.weights = new Object[ featureFieldNumber + 1 ];

            // Metadata
            /*data.outputRowMeta = inputRowMeta.clone();
            meta.getFields( data.outputRowMeta, getStepname(), null, null, this, repository, metaStore );*/

            RowMeta rowMeta = new RowMeta();
            rowMeta.addValueMeta(new ValueMetaNumber("constant_field", 10, 6));

            for ( int i = 0; i < featureFieldNumber; i++) {
                if( i != data.targetIndex )
                    rowMeta.addValueMeta(new ValueMetaNumber(inputRowMeta.getFieldNames()[i], 10, 6));
            }
            // target field's name
            rowMeta.addValueMeta(new ValueMetaString(data.targetField));

            data.outputRowMeta = rowMeta;
            meta.getFields( data.outputRowMeta, getStepname(), null, null, this, repository, metaStore );

        } // end if first

        // it is not first row and it is null
        if ( r == null ) {
            // add linear algorithm here & save the weight if necessary.
            this.doLinearRegression();
            // pass the weight to next step
            data.weights[data.weights.length - 1] = 0;
            putRow( data.outputRowMeta, data.weights );

            // flush result and set output done.
            clearBuffers();
            this.setOutputDone();
            return false;
        }

        this.addBuffer( getInputRowMeta(), r );

        if ( checkFeedback( getLinesRead() ) ) {
            if ( log.isBasic() ) {
                logBasic( "Linenr " + getLinesRead() );
            }
        }
        return true;
    }

    void addBuffer( RowMetaInterface rowMeta, Object[] r ) throws KettleException {
        // Save row
        data.buffer.add( r );
        // Check the free memory every 1000 rows...
        //
        data.freeCounter++;
    }

    @Override
    public boolean init( StepMetaInterface smi, StepDataInterface sdi ) {
        meta = (LinearRegressorMeta) smi;
        data = (LinearRegressorData) sdi;

        if ( !super.init( smi, sdi ) ) {
            return false;
        }

        // In memory buffer
        //
        data.buffer = new ArrayList<Object[]>( 5000 );

        return true;
    }

    @Override
    public void dispose( StepMetaInterface smi, StepDataInterface sdi ) {
        clearBuffers();
        super.dispose( smi, sdi );
    }

    private void clearBuffers() {
        // Clean out the buffer
        data.buffer.clear();
        data.getBufferIndex = 0;
    }

    /**
     * Calling this method will alert the step that we finished passing records to the step. Specifically for steps like
     * "Sort Rows" it means that the buffered rows can be sorted and passed on.
     */
    @Override
    public void batchComplete() throws KettleException {
        setOutputDone();
    }
    public LinearRegressorMeta getLinearRegressorMeta() {
        return meta;
    }
}
