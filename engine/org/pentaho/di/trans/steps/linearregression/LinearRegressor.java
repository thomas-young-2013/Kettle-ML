/**
 * Created by thomasyngli on 2017/2/5.
 */

package org.pentaho.di.trans.steps.linearregression;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleFileException;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaNumber;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.trans.steps.sort.RowTempFile;
import org.pentaho.di.trans.steps.sort.SortRows;
import org.pentaho.di.trans.steps.sort.SortRowsData;
import org.pentaho.di.trans.steps.sort.SortRowsMeta;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPInputStream;

/*
* things:
*   1. the features' type: all double?
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
    public void dolinearRegression() {
        // allocate matrix
        double [][] mat = new double[data.buffer.size()][data.featureFieldnrs];
        int rowcnt = 0;
        for (Object[] row: data.buffer) {
            int colcnt = 0;
            for (int i=0; i < data.featureFieldnrs; i++) {
                if (i != data.targetIndex) mat[rowcnt][colcnt++] = (double) row[i];
                else mat[rowcnt][data.featureFieldnrs-1] = (double) row[i];
            }
            rowcnt++;
        }
        // check the process.
        assert(data.buffer.size() == rowcnt);
        
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
            data.targetIndex = inputRowMeta.indexOfValue( meta.getTargetField() );
            int featureFieldNumber = inputRowMeta.size();
            data.featureFieldnrs = featureFieldNumber;
            data.weights = new double[featureFieldNumber];

            // Metadata
            /*data.outputRowMeta = inputRowMeta.clone();
            meta.getFields( data.outputRowMeta, getStepname(), null, null, this, repository, metaStore );*/

            RowMeta rowMeta = new RowMeta();
            for ( int i = 0; i < featureFieldNumber; i++) {
                rowMeta.addValueMeta(new ValueMetaNumber(inputRowMeta.getFieldNames()[i], 10, 6));
            }

            rowMeta.addValueMeta(new ValueMetaNumber("constantField", 10, 6));
            data.outputRowMeta = rowMeta;

        } // end if first

        // it is not first row and it is null
        if ( r == null ) {
            // add linear algorithm here.
            this.dolinearRegression();

            // flush result and set output done.
            this.passBuffer();
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

    /**
     * get sorted rows from available files in iterative manner.
     * that means call to this method will continue to return rows
     * till all temp files will not be read to the end.
     * */
    Object[] getBuffer() throws KettleValueException {
        Object[] retval;
        retval = null;
        return retval;
    }

    /**
     * This method passes all rows in the buffer to the next steps. Usually call to this method indicates that this
     * particular step finishing processing.
     *
     */
    void passBuffer() throws KettleException {
        // Now we can start the output!
        //
        Object[] r = getBuffer();
        Object[] previousRow = null;

        // log time spent for external merge (expected time consuming operation)
        if ( log.isDebug() && !data.files.isEmpty() ) {
            this.logDebug( BaseMessages.getString( PKG, "SortRows.Debug.ExternalMergeStarted" ) );
        }

        while ( r != null && !isStopped() ) {
            if ( log.isRowLevel() ) {
                logRowlevel( BaseMessages.getString( PKG, "SortRows.RowLevel.ReadRow", getInputRowMeta().getString( r ) ) );
            }

            r = getBuffer();
        }

        if ( log.isDebug() && !data.files.isEmpty() ) {
            this.logDebug( BaseMessages.getString( PKG, "SortRows.Debug.ExternalMergeFinished" ) );
        }

        // Clear out the buffer for the next batch
        //
        clearBuffers();
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

        // Buffer for reading from disk
        //
        data.rowbuffer = new ArrayList<Object[]>( 5000 );

        data.tempRows = new ArrayList<RowTempFile>();

        return true;
    }

    @Override
    public void dispose( StepMetaInterface smi, StepDataInterface sdi ) {
        clearBuffers();
        super.dispose( smi, sdi );
    }

    private void clearBuffers() {
        // Clean out the sort buffer
        data.buffer.clear();
        data.getBufferIndex = 0;
        data.rowbuffer.clear();

        // close any open DataInputStream objects
        if ( ( data.dis != null ) && ( data.dis.size() > 0 ) ) {
            for ( DataInputStream dis : data.dis ) {
                BaseStep.closeQuietly( dis );
            }
        }
        // close any open InputStream objects
        if ( ( data.fis != null ) && ( data.fis.size() > 0 ) ) {
            for ( InputStream is : data.fis ) {
                BaseStep.closeQuietly( is );
            }
        }
        // remove temp files
        for ( int f = 0; f < data.files.size(); f++ ) {
            FileObject fileToDelete = data.files.get( f );
            try {
                if ( fileToDelete != null && fileToDelete.exists() ) {
                    fileToDelete.delete();
                }
            } catch ( FileSystemException e ) {
                logError( e.getLocalizedMessage(), e );
            }
        }
    }

    /**
     * Calling this method will alert the step that we finished passing records to the step. Specifically for steps like
     * "Sort Rows" it means that the buffered rows can be sorted and passed on.
     */
    @Override
    public void batchComplete() throws KettleException {
        passBuffer();
        setOutputDone();
    }
}
