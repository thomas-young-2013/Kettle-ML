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
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

import java.util.List;

public class LinearRegressor extends BaseStep implements StepInterface {
    private static Class<?> PKG = LinearRegressor.class; // for i18n

    public LinearRegressor( StepMeta stepMeta, StepDataInterface stepDataInterface,
                     int copyNr, TransMeta transMeta, Trans trans ) {
        super( stepMeta, stepDataInterface, copyNr, transMeta, trans );
    }

    @Override
    public boolean processRow( StepMetaInterface smi, StepDataInterface sdi ) throws KettleException {
        // wait for first for is available
        Object[] r = getRow();
        List<String> groupFields = null;
        return false;
    }
}
