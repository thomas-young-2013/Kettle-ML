package org.pentaho.di.ui.trans.steps.linearregressionpredictor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.steps.linearregressionpredictor.LinearRegressionPredictorMeta;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.core.widget.ColumnInfo;
import org.pentaho.di.ui.core.widget.TableView;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.di.ui.trans.step.BaseStepDialog;
import org.pentaho.di.ui.trans.step.TableItemInsertListener;

import java.util.*;

/**
 * Created by thomasyngli on 2017/2/18.
 */
public class LinearRegressionPredictorDialog extends BaseStepDialog implements StepDialogInterface {
    private static Class<?> PKG = LinearRegressionPredictorMeta.class; // for i18n purposes, needed by Translator2!!

    private Label wlFilename;
    private Button wbFilename;
    private TextVar wFilename;
    private FormData fdlFilename, fdbFilename, fdFilename;

    private Label wlMainStep;
    private CCombo wMainStep;
    private FormData fdlMainStep, fdMainStep;

    private Label wlScoreDisplay;
    private Button wScoreDisplay;
    private FormData fdlScoreDisplay, fdScoreDisplay;

    private Label wlFields;
    private TableView wFields;
    private FormData fdlFields, fdFields;

    private LinearRegressionPredictorMeta input;
    private Map<String, Integer> inputFields;
    private ColumnInfo[] colinf;

    public LinearRegressionPredictorDialog(Shell parent, Object in, TransMeta transMeta, String sname ) {
        super( parent, (BaseStepMeta) in, transMeta, sname );
        input = (LinearRegressionPredictorMeta) in;
        inputFields = new HashMap<String, Integer>();
    }

    @Override
    public String open() {
        Shell parent = getParent();
        Display display = parent.getDisplay();

        shell = new Shell( parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN );
        props.setLook( shell );
        setShellImage( shell, input );

        ModifyListener lsMod = new ModifyListener() {
            @Override
            public void modifyText( ModifyEvent e ) {
                input.setChanged();
            }
        };
        changed = input.hasChanged();

        FormLayout formLayout = new FormLayout();
        formLayout.marginWidth = Const.FORM_MARGIN;
        formLayout.marginHeight = Const.FORM_MARGIN;

        shell.setLayout( formLayout );
        shell.setText( BaseMessages.getString( PKG, "LinearRegressionPredictorDialog.DialogTitle" ) );

        int middle = props.getMiddlePct();
        int margin = Const.MARGIN;

        // Stepname line
        wlStepname = new Label( shell, SWT.RIGHT );
        wlStepname.setText( BaseMessages.getString( PKG, "System.Label.StepName" ) );
        props.setLook( wlStepname );
        fdlStepname = new FormData();
        fdlStepname.left = new FormAttachment( 0, 0 );
        fdlStepname.right = new FormAttachment( middle, -margin );
        fdlStepname.top = new FormAttachment( 0, margin );
        wlStepname.setLayoutData( fdlStepname );
        wStepname = new Text( shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
        wStepname.setText( stepname );
        props.setLook( wStepname );
        wStepname.addModifyListener( lsMod );
        fdStepname = new FormData();
        fdStepname.left = new FormAttachment( middle, 0 );
        fdStepname.top = new FormAttachment( 0, margin );
        fdStepname.right = new FormAttachment( 100, 0 );
        wStepname.setLayoutData( fdStepname );

        wlMainStep = new Label( shell, SWT.RIGHT );
        wlMainStep.setText( BaseMessages.getString( PKG, "LinearRegressionPredictorDialog.WeightStep.Label" ) );
        props.setLook( wlMainStep );
        fdlMainStep = new FormData();
        fdlMainStep.left = new FormAttachment( 0, 0 );
        fdlMainStep.right = new FormAttachment( middle, -margin );
        fdlMainStep.top = new FormAttachment( wStepname, margin );
        wlMainStep.setLayoutData( fdlMainStep );
        wMainStep = new CCombo( shell, SWT.BORDER );
        props.setLook( wMainStep );

        java.util.List<StepMeta> prevSteps = transMeta.findPreviousSteps( transMeta.findStep( stepname ) );
        for ( StepMeta stepMeta : prevSteps ) {
            wMainStep.add( stepMeta.getName() );
        }

        wMainStep.addModifyListener( lsMod );
        fdMainStep = new FormData();
        fdMainStep.left = new FormAttachment( middle, 0 );
        fdMainStep.top = new FormAttachment( wStepname, margin );
        fdMainStep.right = new FormAttachment( 100, 0 );
        wMainStep.setLayoutData( fdMainStep );

        // training info appended to the file or not.
        wlScoreDisplay = new Label( shell, SWT.RIGHT );
        wlScoreDisplay.setText( BaseMessages.getString( PKG, "LinearRegressionPredictorDialog.isAppended.Label" ) );
        props.setLook( wlScoreDisplay );
        fdlScoreDisplay = new FormData();
        fdlScoreDisplay.left = new FormAttachment( 0, 0 );
        fdlScoreDisplay.top = new FormAttachment( wStepname, margin );
        fdlScoreDisplay.right = new FormAttachment( middle, -margin );
        wlScoreDisplay.setLayoutData( fdlScoreDisplay );
        wScoreDisplay = new Button( shell, SWT.CHECK );
        props.setLook( wScoreDisplay );
        fdScoreDisplay = new FormData();
        fdScoreDisplay.left = new FormAttachment( middle, 0 );
        fdScoreDisplay.top = new FormAttachment( wStepname, margin );
        fdScoreDisplay.right = new FormAttachment( 100, 0 );
        wScoreDisplay.setLayoutData( fdScoreDisplay );
        wScoreDisplay.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
                input.setChanged();
            }
        } );

        wlFilename = new Label( shell, SWT.RIGHT );
        wlFilename.setText( BaseMessages.getString( PKG, "LinearRegressionPredictorDialog.fileDirSaved.Label" ) );
        props.setLook( wlFilename );
        fdlFilename = new FormData();
        fdlFilename.left = new FormAttachment( 0, 0 );
        fdlFilename.top = new FormAttachment( wScoreDisplay, margin );
        fdlFilename.right = new FormAttachment( middle, -margin );
        wlFilename.setLayoutData( fdlFilename );

        wbFilename = new Button( shell, SWT.PUSH | SWT.CENTER );
        props.setLook( wbFilename );
        wbFilename.setText( BaseMessages.getString( PKG, "System.Button.Browse" ) );
        fdbFilename = new FormData();
        fdbFilename.right = new FormAttachment( 100, 0 );
        fdbFilename.top = new FormAttachment( wScoreDisplay, 0 );
        wbFilename.setLayoutData( fdbFilename );

        wFilename = new TextVar( transMeta, shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
        props.setLook( wFilename );
        wFilename.addModifyListener( lsMod );
        fdFilename = new FormData();
        fdFilename.left = new FormAttachment( middle, 0 );
        fdFilename.top = new FormAttachment( wScoreDisplay, margin );
        fdFilename.right = new FormAttachment( wbFilename, -margin );
        wFilename.setLayoutData( fdFilename );

        wOK = new Button( shell, SWT.PUSH );
        wOK.setText( BaseMessages.getString( PKG, "System.Button.OK" ) );
        wGet = new Button( shell, SWT.PUSH );
        wGet.setText( BaseMessages.getString( PKG, "System.Button.GetFields" ) );
        wCancel = new Button( shell, SWT.PUSH );
        wCancel.setText( BaseMessages.getString( PKG, "System.Button.Cancel" ) );

        setButtonPositions( new Button[] { wOK, wCancel, wGet }, margin, null );

        // Table definition.
        wlFields = new Label( shell, SWT.NONE );
        wlFields.setText( BaseMessages.getString( PKG, "LinearRegressionPredictorDialog.Fields.Label" ) );
        props.setLook( wlFields );
        fdlFields = new FormData();
        fdlFields.left = new FormAttachment( 0, 0 );
        fdlFields.top = new FormAttachment( wFilename, margin );
        wlFields.setLayoutData( fdlFields );

        final int FieldsRows = input.getFieldName().length;

        colinf =
                new ColumnInfo[] {
                        new ColumnInfo(
                                BaseMessages.getString( PKG, "LinearRegressionPredictorDialog.Fieldname.Column" ), ColumnInfo.COLUMN_TYPE_CCOMBO,
                                new String[] { "" }, false ) };

        wFields =
                new TableView(
                        transMeta, shell, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI, colinf, FieldsRows, lsMod, props );

        fdFields = new FormData();
        fdFields.left = new FormAttachment( 0, 0 );
        fdFields.top = new FormAttachment( wlFields, margin );
        fdFields.right = new FormAttachment( 100, 0 );
        fdFields.bottom = new FormAttachment( wOK, -2 * margin );
        wFields.setLayoutData( fdFields );

        //
        // Search the fields in the background

        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                StepMeta stepMeta = transMeta.findStep( stepname );
                if ( stepMeta != null ) {
                    try {
                        RowMetaInterface row = transMeta.getPrevStepFields( stepMeta );

                        // Remember these fields...
                        for ( int i = 0; i < row.size(); i++ ) {
                            inputFields.put( row.getValueMeta( i ).getName(), Integer.valueOf( i ) );
                        }
                        setComboBoxes();
                    } catch ( KettleException e ) {
                        logError( BaseMessages.getString( PKG, "System.Dialog.GetFieldsFailed.Message" ) );
                    }
                }
            }
        };
        new Thread( runnable ).start();

        // Add listeners
        lsOK = new Listener() {
            @Override
            public void handleEvent( Event e ) {
                ok();
            }
        };
        lsGet = new Listener() {
            @Override
            public void handleEvent( Event e ) {
                get();
            }
        };
        lsCancel = new Listener() {
            @Override
            public void handleEvent( Event e ) {
                cancel();
            }
        };

        wOK.addListener( SWT.Selection, lsOK );
        wGet.addListener( SWT.Selection, lsGet );
        wCancel.addListener( SWT.Selection, lsCancel );

        lsDef = new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected( SelectionEvent e ) {
                ok();
            }
        };

        // set change listener.
        wStepname.addSelectionListener( lsDef );
        wScoreDisplay.addSelectionListener(lsDef);

        wbFilename.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
                FileDialog dialog = new FileDialog( shell, SWT.SAVE );
                dialog.setFilterExtensions( new String[] { "*.txt", "*.csv", "*" } );
                if ( wFilename.getText() != null ) {
                    dialog.setFileName( transMeta.environmentSubstitute( wFilename.getText() ) );
                }
                dialog.setFilterNames( new String[] {
                        BaseMessages.getString( PKG, "System.FileType.TextFiles" ),
                        BaseMessages.getString( PKG, "System.FileType.CSVFiles" ),
                        BaseMessages.getString( PKG, "System.FileType.AllFiles" ) } );
                if ( dialog.open() != null ) {
                    if (dialog.getFileName() != null) {
                        // The extension is filled in and matches the end
                        // of the selected file => Strip off the extension.
                        String fileName = dialog.getFileName();
                        wFilename.setText( dialog.getFilterPath()
                                + System.getProperty( "file.separator" )
                                + fileName.substring( 0, fileName.length()) );
                        input.setChanged();
                    } else {
                        wFilename.setText( dialog.getFilterPath()
                                + System.getProperty( "file.separator" ) + dialog.getFileName() );
                    }
                }
            }
        } );

        // Detect X or ALT-F4 or something that kills this window...
        shell.addShellListener( new ShellAdapter() {
            @Override
            public void shellClosed( ShellEvent e ) {
                cancel();
            }
        } );

        lsResize = new Listener() {
            @Override
            public void handleEvent( Event event ) {
                Point size = shell.getSize();
                wFields.setSize( size.x - 10, size.y - 50 );
                wFields.table.setSize( size.x - 10, size.y - 50 );
                wFields.redraw();
            }
        };
        shell.addListener( SWT.Resize, lsResize );

        // Set the shell size, based upon previous time...
        setSize();

        getData();
        input.setChanged( changed );

        shell.open();
        while ( !shell.isDisposed() ) {
            if ( !display.readAndDispatch() ) {
                display.sleep();
            }
        }
        return stepname;
    }


    protected void setComboBoxes() {
        // Something was changed in the row.
        //
        final Map<String, Integer> fields = new HashMap<String, Integer>();

        // Add the currentMeta fields...
        fields.putAll( inputFields );

        Set<String> keySet = fields.keySet();
        java.util.List<String> entries = new ArrayList<String>( keySet );

        String[] fieldNames = entries.toArray( new String[entries.size()] );

        Const.sortStrings( fieldNames );
        colinf[0].setComboValues( fieldNames );
    }

    /**
     * Copy information from the meta-data input to the dialog fields.
     */
    public void getData() {

        wFilename.setText(input.getProcessLogFileName()==null?"":input.getProcessLogFileName());
        wScoreDisplay.setSelection(input.isAppended());
        if ( input.getTrainStep() != null ) {
            wMainStep.setText( input.getTrainStep() );
        }

        Table table = wFields.table;
        if ( input.getFieldName().length > 0 ) {
            table.removeAll();
        }
        for ( int i = 0; i < input.getFieldName().length; i++ ) {
            TableItem ti = new TableItem( table, SWT.NONE );
            ti.setText( 0, "" + ( i + 1 ) );
            ti.setText( 1, input.getFieldName()[i] );
        }

        wFields.setRowNums();
        wFields.optWidth( true );

        wStepname.selectAll();
        wStepname.setFocus();
    }

    private void cancel() {
        stepname = null;
        input.setChanged( changed );
        dispose();
    }

    private void ok() {
        if ( Utils.isEmpty( wStepname.getText() ) ) {
            return;
        }

        stepname = wStepname.getText(); // return value

        // copy info to LinearRegressionPredictorMeta class (input)
        input.setAppended(wScoreDisplay.getSelection());
        input.setProcessLogFileName(wFilename.getText());
        input.setTrainStep( transMeta.findStep( wMainStep.getText() ).getName() );

        // Table table = wFields.table;
        int nrfields = wFields.nrNonEmpty();
        input.allocate( nrfields );

        for ( int i = 0; i < nrfields; i++ ) {
            TableItem ti = wFields.getNonEmpty( i );
            input.getFieldName()[i] = ti.getText( 1 );
        }

        dispose();
    }

    private void get() {
        try {
            RowMetaInterface r = transMeta.getPrevStepFields( stepname );
            if ( r != null ) {
                TableItemInsertListener insertListener = new TableItemInsertListener() {
                    @Override
                    public boolean tableItemInserted( TableItem tableItem, ValueMetaInterface v ) {
                        tableItem.setText( 2, BaseMessages.getString( PKG, "System.Combo.No" ) );
                        return true;
                    }
                };
                BaseStepDialog
                        .getFieldsFromPrevious( r, wFields, 1, new int[] { 1 }, new int[] {}, -1, -1, insertListener );
            }
        } catch ( KettleException ke ) {
            new ErrorDialog( shell, BaseMessages.getString( PKG, "System.Dialog.GetFieldsFailed.Title" ), BaseMessages
                    .getString( PKG, "System.Dialog.GetFieldsFailed.Message" ), ke );
        }

    }
}
