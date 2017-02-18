package org.pentaho.di.trans.steps.linearregressionpredictor;

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
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.*;
import org.pentaho.di.trans.step.errorhandling.StreamInterface;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Node;

import java.util.List;

/**
 * Created by thomasyngli on 2017/2/16.
 */
@InjectionSupported( localizationPrefix = "LinearRegressionPredictor.Injection." )
public class LinearRegressionPredictorMeta  extends BaseStepMeta implements StepMetaInterface {
    private static Class<?> PKG = LinearRegressionPredictor.class; // for i18n purposes, needed by Translator2!!

    /** fields sent to the next step*/
    @Injection( name = "FIELD_NAME", group = "FIELDS" )
    private String[] fieldName;

    /** true if appended to the end in the file*/
    @Injection( name = "Is_APPENDED", group = "FIELDS" )
    private boolean isAppended;

    /** the file's name, which stored the score info*/
    @Injection( name = "PROCESS_LOG_FILE_NAME", group = "FIELDS" )
    private String processLogFileName;

    /** the number of fields*/
    @Injection( name = "FIELD_NUM", group = "FIELDS" )
    private int fieldNum;

    /** step name that trains the model*/
    @Injection( name = "TRAIN_STEP", group = "FIELDS" )
    private String trainStep;

    public String getTrainStep() {
        return trainStep;
    }

    public void setTrainStep(String trainStep) {
        this.trainStep = trainStep;
    }

    public String[] getFieldName() {
        return fieldName;
    }

    public void setFieldName(String[] fieldName) {
        this.fieldName = fieldName;
    }

    public boolean isAppended() {
        return isAppended;
    }

    public void setAppended(boolean appended) {
        isAppended = appended;
    }

    public String getProcessLogFileName() {
        return processLogFileName;
    }

    public void setProcessLogFileName(String processLogFileName) {
        this.processLogFileName = processLogFileName;
    }

    public int getFieldNum() {
        return fieldNum;
    }

    public void setFieldNum(int fieldNum) {
        this.fieldNum = fieldNum;
    }

    public LinearRegressionPredictorMeta() {
        super(); // allocate BaseStepMeta
    }

    public void loadXML(Node stepnode, List<DatabaseMeta> databases, IMetaStore metaStore ) throws KettleXMLException {
        readData( stepnode );
    }

    public void allocate( int num) {
        fieldName = new String[num];
    }

    public Object clone() {
        LinearRegressionPredictorMeta retval = (LinearRegressionPredictorMeta) super.clone();
        retval.allocate( fieldNum );
        System.arraycopy(fieldName, 0, retval.fieldName, 0, fieldNum);
        return retval;
    }

    private void readData( Node stepnode ) throws KettleXMLException {
        try {

            isAppended = "Y".equalsIgnoreCase(XMLHandler.getTagValue( stepnode, "is_appended" ));
            processLogFileName = XMLHandler.getTagValue(stepnode, "process_log_file_name");
            fieldNum = Integer.parseInt(XMLHandler.getTagValue(stepnode, "field_num"));
            trainStep = XMLHandler.getTagValue(stepnode, "train_step");

            Node fields = XMLHandler.getSubNode( stepnode, "fields" );
            int nrfields = XMLHandler.countNodes( fields, "field" );
            allocate( nrfields );

            for ( int i = 0; i < nrfields; i++ ) {
                Node fnode = XMLHandler.getSubNodeByNr( fields, "field", i );
                fieldName[i] = XMLHandler.getTagValue( fnode, "field_name" );
            }
        } catch ( Exception e ) {
            throw new KettleXMLException(
                    BaseMessages.getString( PKG, "MergeJoinMeta.Exception.UnableToLoadStepInfo" ), e );
        }
    }

    public void setDefault() {
        allocate( 0);
        processLogFileName = "";
        isAppended = true;
    }

    public String getXML() {
        StringBuilder retval = new StringBuilder();

        retval.append( XMLHandler.addTagValue( "is_appended", isAppended ) );
        retval.append( XMLHandler.addTagValue( "process_log_file_name", processLogFileName ) );
        retval.append( XMLHandler.addTagValue( "field_num", fieldNum ) );
        retval.append( XMLHandler.addTagValue("train_step", trainStep));

        retval.append( "    <fields>" ).append( Const.CR );
        for ( int i = 0; i < fieldName.length; i++ ) {
            retval.append( "      <field>" ).append( Const.CR );
            retval.append( "        " ).append( XMLHandler.addTagValue( "field_name", fieldName[i] ) );
            retval.append( "      </field>" ).append( Const.CR );
        }
        retval.append( "    </fields>" ).append( Const.CR );

        return retval.toString();
    }

    public void readRep(Repository rep, IMetaStore metaStore, ObjectId id_step, List<DatabaseMeta> databases ) throws KettleException {
        try {
            isAppended = "Y".equalsIgnoreCase(rep.getStepAttributeString( id_step, "is_appended" ));
            processLogFileName = rep.getStepAttributeString(id_step, "process_log_file_name");
            fieldNum = Integer.parseInt(rep.getStepAttributeString(id_step, "field_num"));
            trainStep = rep.getStepAttributeString(id_step, "train_step");

            int nrfields = rep.countNrStepAttributes( id_step, "field_name" );

            allocate( nrfields );

            for ( int i = 0; i < nrfields; i++ ) {
                fieldName[i] = rep.getStepAttributeString( id_step, i, "field_name" );
            }

        } catch ( Exception e ) {
            throw new KettleException( BaseMessages.getString(
                    PKG, "MergeJoinMeta.Exception.UnexpectedErrorReadingStepInfo" ), e );
        }
    }

    public void saveRep( Repository rep, IMetaStore metaStore, ObjectId id_transformation, ObjectId id_step ) throws KettleException {
        try {
            rep.saveStepAttribute( id_transformation, id_step, "field_num", fieldNum );
            rep.saveStepAttribute( id_transformation, id_step, "process_log_file_name", processLogFileName );
            rep.saveStepAttribute( id_transformation, id_step, "is_appended", isAppended );
            rep.saveStepAttribute( id_transformation, id_step, "train_step", trainStep);

            for ( int i = 0; i < fieldName.length; i++ ) {
                rep.saveStepAttribute( id_transformation, id_step, i, "field_name", fieldName[i] );
            }

        } catch ( Exception e ) {
            throw new KettleException( BaseMessages.getString( PKG, "MergeJoinMeta.Exception.UnableToSaveStepInfo" )
                    + id_step, e );
        }
    }

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

    public void check(List<CheckResultInterface> remarks, TransMeta transMeta, StepMeta stepMeta,
                      RowMetaInterface prev, String[] input, String[] output, RowMetaInterface info, VariableSpace space,
                      Repository repository, IMetaStore metaStore ) {
        CheckResult cr =
                new CheckResult( CheckResultInterface.TYPE_RESULT_WARNING, BaseMessages.getString(
                        PKG, "MergeJoinMeta.CheckResult.StepNotVerified" ), stepMeta );
        remarks.add( cr );
    }

    public StepInterface getStep( StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta tr,
                                  Trans trans ) {
        return new LinearRegressionPredictor( stepMeta, stepDataInterface, cnr, tr, trans );
    }

    public StepDataInterface getStepData() {
        return new LinearRegressionPredictorData();
    }

    public void resetStepIoMeta() {
        // Don't reset!
    }

    public TransMeta.TransformationType[] getSupportedTransformationTypes() {
        return new TransMeta.TransformationType[]{ TransMeta.TransformationType.Normal, };
    }
}
