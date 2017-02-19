package org.pentaho.di.trans.steps.linearregressionpredictor;

import Jama.Matrix;
import org.pentaho.di.bascis.FileUtils;
import org.pentaho.di.computation.MetrixUtils;
import org.pentaho.di.core.RowSet;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.value.ValueMetaNumber;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.*;
import org.pentaho.di.trans.steps.linearregression.LinearRegressor;
import org.pentaho.di.trans.steps.linearregression.LinearRegressorMeta;

import java.util.ArrayList;
import java.util.List;

import static org.pentaho.di.bascis.BasicUtils.getCurrentTime;

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

    public void passResult(int count) {
        String []fields = meta.getFieldName();
        int []lookup = new int[fields.length];
        int targetIndex = -1;
        boolean checkFlag = true;
        for (int i = 0; i < fields.length; i++ ) {
            int index = data.weightMeta.indexOfValue(fields[i]);
            if (index >= 0 && index < data.weightMeta.size()-1) {
                lookup[i] = index;
            } else {
                if (!fields[i].equals(data.targetField)) {
                    checkFlag = false;
                    break;
                } else {
                    targetIndex = i;
                }
            }
        }
        if (!checkFlag) return;
        double [][]aArray = data.A.getArray();
        for ( int i = 0; i < data.A.getRowDimension(); i++ ) {
            Object [] row = new Object[fields.length];
            for (int j=0; j<fields.length; j++) {
                if (j == targetIndex) row[j] = data.p.get(i, 0);
                else row[j] = aArray[i][lookup[j]];
            }
            try {
                putRow(data.outputRowMeta, row);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void predcit() {
        // prepare the matrix.
        double [][] wt = new double[data.fieldNum][1];
        for (int j = 0; j<data.fieldNum; j++) {
            wt[j][0] = (Double) data.weights[j];
        }
        data.w = new Matrix(wt);

        String [] fields = data.weightMeta.getFieldNames();
        // first we need to adjust the order of field, same with the weight.
        for (int i = 0; i < data.featureRowSets.size(); i++) {
            double [][] mat = new double[data.features.get(i).size()][data.fieldNum];
            double [] t = new double[data.features.get(i).size()];

            String []fieldTmp = data.featureMetas.get(i).getFieldNames();

            // decide whether target field is in the fields.
            data.isTargetFieldIn = false;
            int targetIndex = -1;
            for (int j=0; j<fieldTmp.length; j++) {
                if (fieldTmp[j].equals(data.targetField)) {
                    data.isTargetFieldIn = true;
                    targetIndex = j;
                    break;
                }
            }

            int []lookup = new int[data.fieldNum];
            boolean fieldCheck = true;
            for (int j = 1; j < data.fieldNum; j++) {
                boolean flag = false;
                for (int k = 0; k < fieldTmp.length; k++) {
                    if (fields[j].equals(fieldTmp[k])) {
                        lookup[j] = k;
                        flag = true;
                    }
                }
                fieldCheck &= flag;
            }

            if (!fieldCheck) continue;
            // prepare the data.
            int rowCount = 0;
            for (Object []row: data.features.get(i)) {
                int count = 1;
                mat[rowCount][0] = 1.0;
                for (int j = 1; j < data.fieldNum; j++) {
                    mat[rowCount][count++] = (Double) row[lookup[j]];
                }
                if (data.isTargetFieldIn) {
                    t[rowCount] = (Double) row[targetIndex];
                }
                rowCount++;
            }

            data.A = new Matrix(mat);
            data.p = data.A.times(data.w);
            passResult(i);
            // compute the mse.
            if (data.isTargetFieldIn) {
                double[][] yT = new double[data.features.get(i).size()][1];
                for (int j = 0; j < data.features.get(i).size(); j++) {
                    yT[j][0] = t[j];
                }
                data.y = new Matrix(yT);
                data.mse = MetrixUtils.computeMES(data.p, data.y);

                data.outputString = getOutputString();

                logBasic( String.format("METRICS INFORMATION BELLOW:\n %s\n\n", data.outputString ));
                if (meta.getProcessLogFileName() != null && meta.getProcessLogFileName() != "") {
                    FileUtils.saveFile(meta.getProcessLogFileName(), data.outputString, true);
                }
            }
        }
    }

    /*
    * print time, iteration num, mse, train step name, weight
    * */
    public String getOutputString() {
        StringBuilder val = new StringBuilder(256);
        val.append("\n------------Kettle Working with Linear Regression-----------\n");
        val.append("Running time is: " + getCurrentTime() + "\n");

        // find linear regression step and get the metrics info.
        ArrayList<?> steps = (ArrayList<?>) getTrans().findBaseSteps(meta.getTrainStep());
        String weights = "";
        // decide whether the front step is the trainer or not
        if (steps.get(0) instanceof LinearRegressor) {
            LinearRegressor linearRegressor = (LinearRegressor) steps.get(0);
            LinearRegressorMeta linearRegressorMeta = (LinearRegressorMeta) linearRegressor.getLinearRegressorMeta();
            val.append(String.format("Learning rate is: %f\n", linearRegressorMeta.getLearningRate()));
            val.append(String.format("Iteration number is: %d\n", linearRegressorMeta.getIterationNum()));

            val.append(String.format("MSE is: %#.5f\n", data.mse));
            // weight information
            weights = linearRegressor.getWeightString();
        } else {
            val.append("Prediction according to the model in file");
        }
        val.append(weights + "\n\n");
        return val.toString();
    }

    @Override
    public boolean processRow(StepMetaInterface smi, StepDataInterface sdi ) throws KettleException {

        if (first) {
            this.first = false;

            while (true) {
                List<RowSet> rowsets = getInputRowSets();
                boolean flag = false;
                for (RowSet rowSet: rowsets) {
                    if (rowSet.getRowMeta() == null || rowSet.getRowMeta().getFieldNames().length == 0) flag = true;
                }
                if (!flag) break;
            }
            List<RowSet> rowsets = getInputRowSets();
            for (RowSet rowSet: rowsets) {
                if (rowSet.getOriginStepName().equalsIgnoreCase(meta.getTrainStep())) {
                    data.weightRowSet = rowSet;
                    data.weightMeta = rowSet.getRowMeta();
                    // set the target field.
                    data.targetField = data.weightMeta.getFieldNames()[data.weightMeta.getFieldNames().length-1];
                    data.fieldNum = data.weightMeta.getFieldNames().length - 1;
                } else {
                    data.featureMetas.add( rowSet.getRowMeta() );
                    data.featureRowSets.add(rowSet);
                }
            }

            // init the list, which stores the data.
            for (int i = 0; i < data.featureRowSets.size(); i++) {
                data.features.add(new ArrayList<Object []>());
            }

            // set the output meta
            RowMeta rowMeta = new RowMeta();
            for (String fieldName: meta.getFieldName()) {
                rowMeta.addValueMeta(new ValueMetaNumber(fieldName, 10, 6));
            }
            data.outputRowMeta = rowMeta;
            meta.getFields( data.outputRowMeta, getStepname(), null, null, this, repository, metaStore );
        }

        // read data from trainer step: weights.
        boolean endedFlag = false;
        if (data.weights == null) {
            data.weights = getRowFrom(data.weightRowSet);
            if (data.weights == null) endedFlag = true;
        }

        for ( int i = 0; i < data.featureRowSets.size(); i++ ) {
            Object [] row = getRowFrom(data.featureRowSets.get(i));
            if ( row != null ) {
                endedFlag |= true;
                data.features.get(i).add(row);
            }
        }

        if (!endedFlag) {
            /* let's starting predicting the result.
             * predict algorithm here.
             * put the result to the next step: using putrow
             */
            predcit();
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
        // Clean out the buffer
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
