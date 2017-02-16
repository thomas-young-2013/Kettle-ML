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
import org.pentaho.di.trans.step.errorhandling.Stream;
import org.pentaho.di.trans.step.errorhandling.StreamIcon;
import org.pentaho.di.trans.step.errorhandling.StreamInterface;
import org.pentaho.di.trans.steps.mergejoin.MergeJoin;
import org.pentaho.di.trans.steps.mergejoin.MergeJoinData;
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

    /** true if display the scoring info, false otherwise*/
    @Injection( name = "Is_SCORE", group = "FIELDS" )
    private boolean isScored;

    /** the number of fields*/
    @Injection( name = "FIELD_NUM", group = "FIELDS" )
    private int fieldNum;

    public static final String[] join_types = { "INNER", "LEFT OUTER", "RIGHT OUTER", "FULL OUTER" };
    public static final boolean[] one_optionals = { false, false, true, true };
    public static final boolean[] two_optionals = { false, true, false, true };
    @Injection( name = "JOIN_TYPE" )
    private String joinType;
    @Injection( name = "KEY_FIELD1" )
    private String[] keyFields1;
    @Injection( name = "KEY_FIELD2" )
    private String[] keyFields2;


    public String[] getFieldName() {
        return fieldName;
    }

    public void setFieldName(String[] fieldName) {
        this.fieldName = fieldName;
    }

    public boolean isScored() {
        return isScored;
    }

    public void setScored(boolean scored) {
        isScored = scored;
    }

    /**
     * The supported join types are INNER, LEFT OUTER, RIGHT OUTER and FULL OUTER
     *
     * @return The type of join
     */
    public String getJoinType() {
        return joinType;
    }

    /**
     * Sets the type of join
     *
     * @param joinType The type of join, e.g. INNER/FULL OUTER
     */
    public void setJoinType( String joinType ) {
        this.joinType = joinType;
    }

    /**
     * @return Returns the keyFields1.
     */
    public String[] getKeyFields1() {
        return keyFields1;
    }

    /**
     * @param keyFields1 The keyFields1 to set.
     */
    public void setKeyFields1( String[] keyFields1 ) {
        this.keyFields1 = keyFields1;
    }

    /**
     * @return Returns the keyFields2.
     */
    public String[] getKeyFields2() {
        return keyFields2;
    }

    /**
     * @param keyFields2 The keyFields2 to set.
     */
    public void setKeyFields2( String[] keyFields2 ) {
        this.keyFields2 = keyFields2;
    }

    public boolean excludeFromRowLayoutVerification() {
        return true;
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

        StepIOMetaInterface stepIOMeta = new StepIOMeta( true, true, false, false, false, false );
        List<StreamInterface> infoStreams = getStepIOMeta().getInfoStreams();

        for ( StreamInterface infoStream : infoStreams ) {
            stepIOMeta.addStream( new Stream( infoStream ) );
        }
        retval.ioMeta = stepIOMeta;

        return retval;
    }

    public String getXML() {
        StringBuilder retval = new StringBuilder();

        List<StreamInterface> infoStreams = getStepIOMeta().getInfoStreams();

        retval.append( XMLHandler.addTagValue( "join_type", getJoinType() ) );
        retval.append( XMLHandler.addTagValue( "step1", infoStreams.get( 0 ).getStepname() ) );
        retval.append( XMLHandler.addTagValue( "step2", infoStreams.get( 1 ).getStepname() ) );

        retval.append( "    <keys_1>" + Const.CR );
        for ( int i = 0; i < fieldName.length; i++ ) {
            retval.append( "      " + XMLHandler.addTagValue( "key", keyFields1[i] ) );
        }
        retval.append( "    </keys_1>" + Const.CR );

        retval.append( "    <keys_2>" + Const.CR );
        for ( int i = 0; i < keyFields2.length; i++ ) {
            retval.append( "      " + XMLHandler.addTagValue( "key", keyFields2[i] ) );
        }
        retval.append( "    </keys_2>" + Const.CR );

        return retval.toString();
    }

    private void readData( Node stepnode ) throws KettleXMLException {
        try {

            Node keysNode1 = XMLHandler.getSubNode( stepnode, "keys_1" );
            Node keysNode2 = XMLHandler.getSubNode( stepnode, "keys_2" );

            int nrKeys1 = XMLHandler.countNodes( keysNode1, "key" );
            int nrKeys2 = XMLHandler.countNodes( keysNode2, "key" );

            // allocate( nrKeys1, nrKeys2 );

            for ( int i = 0; i < nrKeys1; i++ ) {
                Node keynode = XMLHandler.getSubNodeByNr( keysNode1, "key", i );
                keyFields1[i] = XMLHandler.getNodeValue( keynode );
            }

            for ( int i = 0; i < nrKeys2; i++ ) {
                Node keynode = XMLHandler.getSubNodeByNr( keysNode2, "key", i );
                keyFields2[i] = XMLHandler.getNodeValue( keynode );
            }

            List<StreamInterface> infoStreams = getStepIOMeta().getInfoStreams();
            infoStreams.get( 0 ).setSubject( XMLHandler.getTagValue( stepnode, "step1" ) );
            infoStreams.get( 1 ).setSubject( XMLHandler.getTagValue( stepnode, "step2" ) );
            joinType = XMLHandler.getTagValue( stepnode, "join_type" );
        } catch ( Exception e ) {
            throw new KettleXMLException(
                    BaseMessages.getString( PKG, "MergeJoinMeta.Exception.UnableToLoadStepInfo" ), e );
        }
    }

    public void setDefault() {
        allocate( 0);
    }

    public void readRep(Repository rep, IMetaStore metaStore, ObjectId id_step, List<DatabaseMeta> databases ) throws KettleException {
        try {
            int nrKeys1 = rep.countNrStepAttributes( id_step, "keys_1" );
            int nrKeys2 = rep.countNrStepAttributes( id_step, "keys_2" );

            // allocate( nrKeys1, nrKeys2 );

            for ( int i = 0; i < nrKeys1; i++ ) {
                keyFields1[i] = rep.getStepAttributeString( id_step, i, "keys_1" );
            }
            for ( int i = 0; i < nrKeys2; i++ ) {
                keyFields2[i] = rep.getStepAttributeString( id_step, i, "keys_2" );
            }

            List<StreamInterface> infoStreams = getStepIOMeta().getInfoStreams();
            infoStreams.get( 0 ).setSubject( rep.getStepAttributeString( id_step, "step1" ) );
            infoStreams.get( 1 ).setSubject( rep.getStepAttributeString( id_step, "step2" ) );
            joinType = rep.getStepAttributeString( id_step, "join_type" );
        } catch ( Exception e ) {
            throw new KettleException( BaseMessages.getString(
                    PKG, "MergeJoinMeta.Exception.UnexpectedErrorReadingStepInfo" ), e );
        }
    }

    @Override
    public void searchInfoAndTargetSteps( List<StepMeta> steps ) {
        for ( StreamInterface stream : getStepIOMeta().getInfoStreams() ) {
            stream.setStepMeta( StepMeta.findStep( steps, (String) stream.getSubject() ) );
        }
    }

    public void saveRep( Repository rep, IMetaStore metaStore, ObjectId id_transformation, ObjectId id_step ) throws KettleException {
        try {
            for ( int i = 0; i < keyFields1.length; i++ ) {
                rep.saveStepAttribute( id_transformation, id_step, i, "keys_1", keyFields1[i] );
            }

            for ( int i = 0; i < keyFields2.length; i++ ) {
                rep.saveStepAttribute( id_transformation, id_step, i, "keys_2", keyFields2[i] );
            }

            List<StreamInterface> infoStreams = getStepIOMeta().getInfoStreams();

            rep.saveStepAttribute( id_transformation, id_step, "step1", infoStreams.get( 0 ).getStepname() );
            rep.saveStepAttribute( id_transformation, id_step, "step2", infoStreams.get( 1 ).getStepname() );
            rep.saveStepAttribute( id_transformation, id_step, "join_type", getJoinType() );
        } catch ( Exception e ) {
            throw new KettleException( BaseMessages.getString( PKG, "MergeJoinMeta.Exception.UnableToSaveStepInfo" )
                    + id_step, e );
        }
    }

    public void check(List<CheckResultInterface> remarks, TransMeta transMeta, StepMeta stepMeta,
                      RowMetaInterface prev, String[] input, String[] output, RowMetaInterface info, VariableSpace space,
                      Repository repository, IMetaStore metaStore ) {
    /*
     * @todo Need to check for the following: 1) Join type must be one of INNER / LEFT OUTER / RIGHT OUTER / FULL OUTER
     * 2) Number of input streams must be two (for now at least) 3) The field names of input streams must be unique
     */
        CheckResult cr =
                new CheckResult( CheckResultInterface.TYPE_RESULT_WARNING, BaseMessages.getString(
                        PKG, "MergeJoinMeta.CheckResult.StepNotVerified" ), stepMeta );
        remarks.add( cr );
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

    public StepInterface getStep( StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta tr,
                                  Trans trans ) {
        return new MergeJoin( stepMeta, stepDataInterface, cnr, tr, trans );
    }

    public StepDataInterface getStepData() {
        return new MergeJoinData();
    }

    /**
     * Returns the Input/Output metadata for this step. The generator step only produces output, does not accept input!
     */
    public StepIOMetaInterface getStepIOMeta() {
        if ( ioMeta == null ) {

            ioMeta = new StepIOMeta( true, true, false, false, false, false );

            ioMeta.addStream( new Stream( StreamInterface.StreamType.INFO, null, BaseMessages.getString(
                    PKG, "MergeJoinMeta.InfoStream.FirstStream.Description" ), StreamIcon.INFO, null ) );
            ioMeta.addStream( new Stream( StreamInterface.StreamType.INFO, null, BaseMessages.getString(
                    PKG, "MergeJoinMeta.InfoStream.SecondStream.Description" ), StreamIcon.INFO, null ) );
        }

        return ioMeta;
    }

    public void resetStepIoMeta() {
        // Don't reset!
    }

    public TransMeta.TransformationType[] getSupportedTransformationTypes() {
        return new TransMeta.TransformationType[]{ TransMeta.TransformationType.Normal, };
    }
}