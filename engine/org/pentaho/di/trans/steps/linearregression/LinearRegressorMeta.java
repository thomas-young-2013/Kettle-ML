package org.pentaho.di.trans.steps.linearregression;

import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.injection.Injection;
import org.pentaho.di.core.injection.InjectionSupported;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.util.IntegerPluginProperty;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.*;
import org.pentaho.di.trans.step.errorhandling.Stream;
import org.pentaho.di.trans.step.errorhandling.StreamIcon;
import org.pentaho.di.trans.step.errorhandling.StreamInterface;
import org.pentaho.di.trans.steps.sort.SortRowsMeta;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Node;

import java.io.File;
import java.util.List;

/**
 * @author thomas young (liyang)
 * Created by thomasyngli on 2017/2/5.
 */
@InjectionSupported( localizationPrefix = "LinearRegressor.Injection.", groups = { "FIELDS" } )
public class LinearRegressorMeta  extends BaseStepMeta implements StepMetaInterface {
    private static Class<?> PKG = LinearRegressorMeta.class; // for i18n purposes, needed by Translator2!!

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

    /* the number of folds to validate model */
    @Injection( name = "ITETATION_NUM", group = "FIELDS")
    private int iterationNum;

    /** order by which fields? */
    @Injection( name = "NAME", group = "FIELDS" )
    private String[] fieldName;

    /** false : feature field, true=target field */
    @Injection( name = "IS_TARGET", group = "FIELDS" )
    private boolean[] isTarget;

    public LinearRegressorMeta() {
        super(); // allocate BaseStepMeta
    }

    public boolean[] getIsTarget() {
        return isTarget;
    }

    public void setIsTarget(boolean[] isTarget) {
        this.isTarget = isTarget;
    }

    public String[] getFieldName() {
        return fieldName;
    }

    public int getIterationNum() {
        return iterationNum;
    }

    public void setIterationNum(int iterationNum) {
        this.iterationNum = iterationNum;
    }

    public void setFieldName(String[] fieldName) {
        this.fieldName = fieldName;
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
    public void loadXML(Node stepnode, List<DatabaseMeta> databases, IMetaStore metaStore ) throws KettleXMLException {
        readData( stepnode );
    }

    public void allocate( int nrfields ) {
        fieldName = new String[nrfields]; // order by
        isTarget = new boolean[nrfields];
    }

    @Override
    public Object clone() {
        LinearRegressorMeta retval = (LinearRegressorMeta) super.clone();

        int nrfields = fieldName.length;

        retval.allocate( nrfields );
        System.arraycopy( fieldName, 0, retval.fieldName, 0, nrfields );
        System.arraycopy( isTarget, 0, retval.isTarget, 0, nrfields );

        return retval;
    }

    private void readData( Node stepnode ) throws KettleXMLException {
        try {
            learningRate = Double.parseDouble(XMLHandler.getTagValue( stepnode, "learning_rate" ));
            iterationNum = Integer.parseInt(XMLHandler.getTagValue( stepnode, "iteration_num" ));

            Node fields = XMLHandler.getSubNode( stepnode, "fields" );
            int nrfields = XMLHandler.countNodes( fields, "field" );

            allocate( nrfields );

            for ( int i = 0; i < nrfields; i++ ) {
                Node fnode = XMLHandler.getSubNodeByNr( fields, "field", i );

                fieldName[i] = XMLHandler.getTagValue( fnode, "field_name" );
                isTarget[i] = "Y".equalsIgnoreCase(XMLHandler.getTagValue(fnode, "is_target"));
            }
        } catch ( Exception e ) {
            throw new KettleXMLException( "Unable to load step info from XML", e );
        }
    }

    @Override
    public void setDefault() {
        learningRate = 0.01;
        regulationValue = 0.1;
        trainDataPercentage = 0.8;
        foldNum = 5;
        iterationNum = 500;
        int nrfields = 0;
        allocate( nrfields );
    }

    @Override
    public String getXML() {
        StringBuilder retval = new StringBuilder( 256 );

        retval.append( "      " ).append( XMLHandler.addTagValue( "learning_rate", learningRate ) );
        retval.append( "      " ).append( XMLHandler.addTagValue( "iteration_num", iterationNum ) );

        retval.append( "    <fields>" ).append( Const.CR );
        for ( int i = 0; i < fieldName.length; i++ ) {
            retval.append( "      <field>" ).append( Const.CR );
            retval.append( "        " ).append( XMLHandler.addTagValue( "field_name", fieldName[i] ) );
            retval.append( "        " ).append( XMLHandler.addTagValue( "is_target", isTarget[i] ) );
            retval.append( "      </field>" ).append( Const.CR );
        }
        retval.append( "    </fields>" ).append( Const.CR );

        return retval.toString();
    }

    @Override
    public void readRep( Repository rep, IMetaStore metaStore, ObjectId id_step, List<DatabaseMeta> databases ) throws KettleException {
        try {
            learningRate = Double.parseDouble(rep.getStepAttributeString( id_step, "learning_rate" ));
            iterationNum = Integer.parseInt(rep.getStepAttributeString( id_step, "iteration_num" ));

            int nrfields = rep.countNrStepAttributes( id_step, "field_name" );

            allocate( nrfields );

            for ( int i = 0; i < nrfields; i++ ) {
                fieldName[i] = rep.getStepAttributeString( id_step, i, "field_name" );
                isTarget[i] = rep.getStepAttributeBoolean( id_step, i, "is_target" );
            }
        } catch ( Exception e ) {
            throw new KettleException( "Unexpected error reading step information from the repository", e );
        }
    }

    @Override
    public void saveRep( Repository rep, IMetaStore metaStore, ObjectId id_transformation, ObjectId id_step ) throws KettleException {
        try {
            rep.saveStepAttribute( id_transformation, id_step, "learning_rate", learningRate );
            rep.saveStepAttribute( id_transformation, id_step, "iteration_num", iterationNum );

            for ( int i = 0; i < fieldName.length; i++ ) {
                rep.saveStepAttribute( id_transformation, id_step, i, "field_name", fieldName[i] );
                rep.saveStepAttribute( id_transformation, id_step, i, "is_target", isTarget[i] );
            }
        } catch ( Exception e ) {
            throw new KettleException( "Unable to save step information to the repository for id_step=" + id_step, e );
        }
    }

    @Override
    public void getFields( RowMetaInterface r, String name, RowMetaInterface[] info, StepMeta nextStep,
                           VariableSpace space, Repository repository, IMetaStore metaStore ) throws KettleStepException {
        // We don't have any input fields here in "r" as they are all info fields.
        // So we just merge in the info fields.
        //
        if ( info != null ) {
            for ( int i = 0; i < info.length; i++ ) {
                if ( info[i] != null ) {
                    r.mergeRowMeta( info[i], name );
                }
            }
        }

        for ( int i = 0; i < r.size(); i++ ) {
            ValueMetaInterface vmi = r.getValueMeta( i );
            if ( vmi != null && Utils.isEmpty( vmi.getName() ) ) {
                vmi.setOrigin( name );
            }
        }
        return;
    }

    @Override
    public void searchInfoAndTargetSteps( List<StepMeta> steps ) {
        for ( StreamInterface stream : getStepIOMeta().getInfoStreams() ) {
            stream.setStepMeta( StepMeta.findStep( steps, (String) stream.getSubject() ) );
        }
    }

    public boolean chosesTargetSteps() {
        return false;
    }

    public String[] getTargetSteps() {
        return null;
    }


    @Override
    public void check( List<CheckResultInterface> remarks, TransMeta transMeta, StepMeta stepMeta,
                       RowMetaInterface prev, String[] input, String[] output, RowMetaInterface info, VariableSpace space,
                       Repository repository, IMetaStore metaStore ) {
        CheckResult cr;

        if ( prev != null && prev.size() > 0 ) {
            cr =
                    new CheckResult( CheckResultInterface.TYPE_RESULT_OK, BaseMessages.getString(
                            PKG, "LinearRegressorMeta.CheckResult.FieldsReceived", "" + prev.size() ), stepMeta );
            remarks.add( cr );

            String error_message = "";
            boolean error_found = false;

            // Starting from selected fields in ...
            for ( int i = 0; i < fieldName.length; i++ ) {
                int idx = prev.indexOfValue( fieldName[i] );
                if ( idx < 0 ) {
                    error_message += "\t\t" + fieldName[i] + Const.CR;
                    error_found = true;
                }
            }
            if ( error_found ) {
                error_message = BaseMessages.getString( PKG, "LinearRegressorMeta.CheckResult.SortKeysNotFound", error_message );

                cr = new CheckResult( CheckResultInterface.TYPE_RESULT_ERROR, error_message, stepMeta );
                remarks.add( cr );
            } else {
                if ( fieldName.length > 0 ) {
                    cr =
                            new CheckResult( CheckResultInterface.TYPE_RESULT_OK, BaseMessages.getString(
                                    PKG, "LinearRegressorMeta.CheckResult.AllSortKeysFound" ), stepMeta );
                    remarks.add( cr );
                } else {
                    cr =
                            new CheckResult( CheckResultInterface.TYPE_RESULT_ERROR, BaseMessages.getString(
                                    PKG, "LinearRegressorMeta.CheckResult.NoSortKeysEntered" ), stepMeta );
                    remarks.add( cr );
                }
            }

        } else {
            cr =
                    new CheckResult( CheckResultInterface.TYPE_RESULT_ERROR, BaseMessages.getString(
                            PKG, "LinearRegressorMeta.CheckResult.NoFields" ), stepMeta );
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

}
