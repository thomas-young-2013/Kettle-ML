package org.pentaho.di.trans.steps.linearregression;

import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.injection.Injection;
import org.pentaho.di.core.injection.InjectionSupported;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.*;
import org.pentaho.di.trans.step.errorhandling.Stream;
import org.pentaho.di.trans.step.errorhandling.StreamIcon;
import org.pentaho.di.trans.step.errorhandling.StreamInterface;
import org.pentaho.metastore.api.IMetaStore;

import java.util.List;

/**
 * @author thomas young (liyang)
 * Created by thomasyngli on 2017/2/5.
 */
@InjectionSupported( localizationPrefix = "LinearRegressor.Injection.", groups = { "FIELDS" } )
public class LinearRegressorMeta  extends BaseStepMeta implements StepMetaInterface {
    private static Class<?> PKG = LinearRegressorMeta.class; // for i18n purposes, needed by Translator2!!

    /** target field's name*/
    @Injection( name = "TARGET_FIELD", group = "FIELDS" )
    private String targetField;

    /* learning rate */
    @Injection( name = "LEARNING_RATE", group = "FIELDS")
    private double learningRate;

    /* the value of regulation */
    @Injection( name = "REGULATION_VALUE", group = "FIELDS")
    private double regulationValue;

    /* the percentage of training data */
    @Injection( name = "TRAIN_DATA_PERCENTAGE", group = "FIELDS")
    private double trainDataPercentage;

    /* the number of folds to validate model */
    @Injection( name = "FOLD_NUM", group = "FIELDS")
    private int foldNum;

    public LinearRegressorMeta() {
        super(); // allocate BaseStepMeta
    }

    @Override
    public void setDefault() {
        learningRate = 0.01;
        regulationValue = 0.1;
        trainDataPercentage = 0.8;
        foldNum = 5;
    }

    public String getTargetField() {
        return targetField;
    }

    public void setTargetField(String targetField) {
        this.targetField = targetField;
    }

    public double getLearningRate() {
        return learningRate;
    }

    public void setLearningRate(double learningRate) {
        this.learningRate = learningRate;
    }

    public double getRegulationValue() {
        return regulationValue;
    }

    public void setRegulationValue(double regulationValue) {
        this.regulationValue = regulationValue;
    }

    public double getTrainDataPercentage() {
        return trainDataPercentage;
    }

    public void setTrainDataPercentage(double trainDataPercentage) {
        this.trainDataPercentage = trainDataPercentage;
    }

    public int getFoldNum() {
        return foldNum;
    }

    public void setFoldNum(int foldNum) {
        this.foldNum = foldNum;
    }

    @Override
    public void readRep(Repository rep, IMetaStore metaStore, ObjectId id_step, List<DatabaseMeta> databases ) throws KettleException {
        try {
            int nrKeys = rep.countNrStepAttributes( id_step, "key_field" );
            int nrValues = rep.countNrStepAttributes( id_step, "value_field" );

            List<StreamInterface> infoStreams = getStepIOMeta().getInfoStreams();
            StreamInterface referenceStream = infoStreams.get( 0 );
            StreamInterface compareStream = infoStreams.get( 1 );

            referenceStream.setSubject( rep.getStepAttributeString( id_step, "reference" ) );
            compareStream.setSubject( rep.getStepAttributeString( id_step, "compare" ) );
        } catch ( Exception e ) {
            throw new KettleException( BaseMessages.getString(
                    PKG, "MergeRowsMeta.Exception.UnexpectedErrorReadingStepInfo" ), e );
        }
    }

    @Override
    public void searchInfoAndTargetSteps( List<StepMeta> steps ) {
        for ( StreamInterface stream : getStepIOMeta().getInfoStreams() ) {
            stream.setStepMeta( StepMeta.findStep( steps, (String) stream.getSubject() ) );
        }
    }

    @Override
    public void saveRep( Repository rep, IMetaStore metaStore, ObjectId id_transformation, ObjectId id_step ) throws KettleException {
        try {
            List<StreamInterface> infoStreams = getStepIOMeta().getInfoStreams();
            StreamInterface referenceStream = infoStreams.get( 0 );
            StreamInterface compareStream = infoStreams.get( 1 );

            rep.saveStepAttribute( id_transformation, id_step, "reference", referenceStream.getStepname() );
            rep.saveStepAttribute( id_transformation, id_step, "compare", compareStream.getStepname() );
        } catch ( Exception e ) {
            throw new KettleException( BaseMessages.getString( PKG, "MergeRowsMeta.Exception.UnableToSaveStepInfo" )
                    + id_step, e );
        }
    }

    public boolean chosesTargetSteps() {
        return false;
    }

    public String[] getTargetSteps() {
        return null;
    }

    @Override
    public void getFields(RowMetaInterface r, String name, RowMetaInterface[] info, StepMeta nextStep,
                          VariableSpace space, Repository repository, IMetaStore metaStore ) throws KettleStepException {
        // We don't have any input fields here in "r" as they are all info fields.
        // So we just merge in the info fields.
        //
        if ( info != null ) {
            boolean found = false;
            for ( int i = 0; i < info.length && !found; i++ ) {
                if ( info[i] != null ) {
                    r.mergeRowMeta( info[i] );
                    found = true;
                }
            }
        }

    }

    @Override
    public void check(List<CheckResultInterface> remarks, TransMeta transMeta, StepMeta stepMeta,
                      RowMetaInterface prev, String[] input, String[] output, RowMetaInterface info, VariableSpace space,
                      Repository repository, IMetaStore metaStore ) {
        CheckResult cr;

        List<StreamInterface> infoStreams = getStepIOMeta().getInfoStreams();
        StreamInterface referenceStream = infoStreams.get( 0 );
        StreamInterface compareStream = infoStreams.get( 1 );

        if ( referenceStream.getStepname() != null && compareStream.getStepname() != null ) {
            cr =
                    new CheckResult( CheckResultInterface.TYPE_RESULT_OK, BaseMessages.getString(
                            PKG, "MergeRowsMeta.CheckResult.SourceStepsOK" ), stepMeta );
            remarks.add( cr );
        } else if ( referenceStream.getStepname() == null && compareStream.getStepname() == null ) {
            cr =
                    new CheckResult( CheckResultInterface.TYPE_RESULT_ERROR, BaseMessages.getString(
                            PKG, "MergeRowsMeta.CheckResult.SourceStepsMissing" ), stepMeta );
            remarks.add( cr );
        } else {
            cr =
                    new CheckResult( CheckResultInterface.TYPE_RESULT_OK, BaseMessages.getString(
                            PKG, "MergeRowsMeta.CheckResult.OneSourceStepMissing" ), stepMeta );
            remarks.add( cr );
        }
    }

    @Override
    public StepInterface getStep( StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta tr,
                                  Trans trans ) {
        return new LinearRegressor( stepMeta, stepDataInterface, cnr, tr, trans );
    }

    @Override
    public StepDataInterface getStepData() {
        return new LinearRegressorData();
    }

    /**
     * Returns the Input/Output metadata for this step.
     */
    @Override
    public StepIOMetaInterface getStepIOMeta() {
        if ( ioMeta == null ) {

            ioMeta = new StepIOMeta( true, true, false, false, false, false );

            ioMeta.addStream( new Stream( StreamInterface.StreamType.INFO, null, BaseMessages.getString(
                    PKG, "MergeRowsMeta.InfoStream.FirstStream.Description" ), StreamIcon.INFO, null ) );
            ioMeta.addStream( new Stream( StreamInterface.StreamType.INFO, null, BaseMessages.getString(
                    PKG, "MergeRowsMeta.InfoStream.SecondStream.Description" ), StreamIcon.INFO, null ) );
        }

        return ioMeta;
    }

    @Override
    public void resetStepIoMeta() {
    }

    @Override
    public TransMeta.TransformationType[] getSupportedTransformationTypes() {
        return new TransMeta.TransformationType[] { TransMeta.TransformationType.Normal, };
    }

}
