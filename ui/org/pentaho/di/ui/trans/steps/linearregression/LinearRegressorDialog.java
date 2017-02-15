package org.pentaho.di.ui.trans.steps.linearregression;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaNumber;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.steps.linearregression.LinearRegressorMeta;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.core.widget.CheckBoxVar;
import org.pentaho.di.ui.core.widget.ColumnInfo;
import org.pentaho.di.ui.core.widget.TableView;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.di.ui.trans.step.BaseStepDialog;
import org.pentaho.di.ui.trans.step.TableItemInsertListener;

import java.util.*;

/**
 * Created by thomasyngli on 2017/2/11.
 */
public class LinearRegressorDialog extends BaseStepDialog implements StepDialogInterface {
    private static Class<?> PKG = LinearRegressorMeta.class; // for i18n purposes, needed by Translator2!!

    private Label wlPrefix;
    private Text wPrefix;
    private FormData fdlPrefix, fdPrefix;

    private Label wlSortSize;
    private TextVar wSortSize;
    private FormData fdlSortSize, fdSortSize;

    private Label wlIterationGap;
    private TextVar wIterationGap;
    private FormData fdlIterationGap, fdIterationGap;

    private Label wlFileIsCommand;
    private Button wFileIsCommand;
    private FormData fdlFileIsCommand, fdFileIsCommand;

    private Label wlFilename;
    private Button wbFilename;
    private TextVar wFilename;
    private FormData fdlFilename, fdbFilename, fdFilename;

    private Label wlFields;
    private TableView wFields;
    private FormData fdlFields, fdFields;

    private LinearRegressorMeta input;
    private Map<String, Integer> inputFields;
    private ColumnInfo[] colinf;

    public LinearRegressorDialog(Shell parent, Object in, TransMeta transMeta, String sname ) {
        super( parent, (BaseStepMeta) in, transMeta, sname );
        input = (LinearRegressorMeta) in;
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
        shell.setText( BaseMessages.getString( PKG, "LinearRegressorDialog.DialogTitle" ) );

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


        // learning rate
        wlPrefix = new Label( shell, SWT.RIGHT );
        wlPrefix.setText( BaseMessages.getString( PKG, "LinearRegressorDialog.LearningRate.Label" ) );
        props.setLook( wlPrefix );
        fdlPrefix = new FormData();
        fdlPrefix.left = new FormAttachment( 0, 0 );
        fdlPrefix.right = new FormAttachment( middle, -margin );
        fdlPrefix.top = new FormAttachment( wStepname, margin * 2 );
        wlPrefix.setLayoutData( fdlPrefix );
        wPrefix = new Text( shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
        props.setLook( wPrefix );
        wPrefix.addModifyListener( lsMod );
        fdPrefix = new FormData();
        fdPrefix.left = new FormAttachment( middle, 0 );
        fdPrefix.top = new FormAttachment( wStepname, margin * 2 );
        fdPrefix.right = new FormAttachment( 100, 0 );
        wPrefix.setLayoutData( fdPrefix );

        // iterations
        wlSortSize = new Label( shell, SWT.RIGHT );
        wlSortSize.setText( BaseMessages.getString( PKG, "LinearRegressorDialog.IterationNum.Label" ) );
        props.setLook( wlSortSize );
        fdlSortSize = new FormData();
        fdlSortSize.left = new FormAttachment( 0, 0 );
        fdlSortSize.right = new FormAttachment( middle, -margin );
        fdlSortSize.top = new FormAttachment( wPrefix, margin * 2 );
        wlSortSize.setLayoutData( fdlSortSize );
        wSortSize = new TextVar( transMeta, shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
        props.setLook( wSortSize );
        wSortSize.addModifyListener( lsMod );
        fdSortSize = new FormData();
        fdSortSize.left = new FormAttachment( middle, 0 );
        fdSortSize.top = new FormAttachment( wPrefix, margin * 2 );
        fdSortSize.right = new FormAttachment( 100, 0 );
        wSortSize.setLayoutData( fdSortSize );

        // iterations info output gap.
        wlIterationGap = new Label( shell, SWT.RIGHT );
        wlIterationGap.setText( BaseMessages.getString( PKG, "LinearRegressorDialog.iterationGap.Label" ) );
        props.setLook( wlIterationGap );
        fdlIterationGap = new FormData();
        fdlIterationGap.left = new FormAttachment( 0, 0 );
        fdlIterationGap.right = new FormAttachment( middle, -margin );
        fdlIterationGap.top = new FormAttachment( wSortSize, margin * 2 );
        wlIterationGap.setLayoutData( fdlIterationGap );
        wIterationGap = new TextVar( transMeta, shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
        props.setLook( wIterationGap );
        wIterationGap.addModifyListener( lsMod );
        fdIterationGap = new FormData();
        fdIterationGap.left = new FormAttachment( middle, 0 );
        fdIterationGap.top = new FormAttachment( wSortSize, margin * 2 );
        fdIterationGap.right = new FormAttachment( 100, 0 );
        wIterationGap.setLayoutData( fdIterationGap );

        // compare the predict with the target.
        wlFileIsCommand = new Label( shell, SWT.RIGHT );
        wlFileIsCommand.setText( BaseMessages.getString( PKG, "LinearRegressorDialog.isCompared.Label" ) );
        props.setLook( wlFileIsCommand );
        fdlFileIsCommand = new FormData();
        fdlFileIsCommand.left = new FormAttachment( 0, 0 );
        fdlFileIsCommand.top = new FormAttachment( wIterationGap, margin );
        fdlFileIsCommand.right = new FormAttachment( middle, -margin );
        wlFileIsCommand.setLayoutData( fdlFileIsCommand );
        wFileIsCommand = new Button( shell, SWT.CHECK );
        props.setLook( wFileIsCommand );
        fdFileIsCommand = new FormData();
        fdFileIsCommand.left = new FormAttachment( middle, 0 );
        fdFileIsCommand.top = new FormAttachment( wIterationGap, margin );
        fdFileIsCommand.right = new FormAttachment( 100, 0 );
        wFileIsCommand.setLayoutData( fdFileIsCommand );
        wFileIsCommand.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
                input.setChanged();
            }
        } );

        wlFilename = new Label( shell, SWT.RIGHT );
        wlFilename.setText( BaseMessages.getString( PKG, "LinearRegressorDialog.fileDirSaved.Label" ) );
        props.setLook( wlFilename );
        fdlFilename = new FormData();
        fdlFilename.left = new FormAttachment( 0, 0 );
        fdlFilename.top = new FormAttachment( wFileIsCommand, margin );
        fdlFilename.right = new FormAttachment( middle, -margin );
        wlFilename.setLayoutData( fdlFilename );

        wbFilename = new Button( shell, SWT.PUSH | SWT.CENTER );
        props.setLook( wbFilename );
        wbFilename.setText( BaseMessages.getString( PKG, "System.Button.Browse" ) );
        fdbFilename = new FormData();
        fdbFilename.right = new FormAttachment( 100, 0 );
        fdbFilename.top = new FormAttachment( wFileIsCommand, 0 );
        wbFilename.setLayoutData( fdbFilename );

        wFilename = new TextVar( transMeta, shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
        props.setLook( wFilename );
        wFilename.addModifyListener( lsMod );
        fdFilename = new FormData();
        fdFilename.left = new FormAttachment( middle, 0 );
        fdFilename.top = new FormAttachment( wFileIsCommand, margin );
        fdFilename.right = new FormAttachment( wbFilename, -margin );
        wFilename.setLayoutData( fdFilename );

        wOK = new Button( shell, SWT.PUSH );
        wOK.setText( BaseMessages.getString( PKG, "System.Button.OK" ) );
        wGet = new Button( shell, SWT.PUSH );
        wGet.setText( BaseMessages.getString( PKG, "System.Button.GetFields" ) );
        wCancel = new Button( shell, SWT.PUSH );
        wCancel.setText( BaseMessages.getString( PKG, "System.Button.Cancel" ) );

        setButtonPositions( new Button[] { wOK, wCancel, wGet }, margin, null );

        // Table with fields to sort and sort direction
        wlFields = new Label( shell, SWT.NONE );
        wlFields.setText( BaseMessages.getString( PKG, "LinearRegressorDialog.Fields.Label" ) );
        props.setLook( wlFields );
        fdlFields = new FormData();
        fdlFields.left = new FormAttachment( 0, 0 );
        fdlFields.top = new FormAttachment( wFilename, margin );
        wlFields.setLayoutData( fdlFields );

        final int FieldsRows = input.getFieldName().length;

        colinf =
                new ColumnInfo[] {
                        new ColumnInfo(
                                BaseMessages.getString( PKG, "LinearRegressorDialog.Fieldname.Column" ), ColumnInfo.COLUMN_TYPE_CCOMBO,
                                new String[] { "" }, false ),
                        new ColumnInfo(
                                BaseMessages.getString( PKG, "LinearRegressorDialog.isTarget.Column" ), ColumnInfo.COLUMN_TYPE_CCOMBO,
                                new String[] {
                                        BaseMessages.getString( PKG, "System.Combo.Yes" ),
                                        BaseMessages.getString( PKG, "System.Combo.No" ) } ) };

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
        wPrefix.addSelectionListener( lsDef );
        wSortSize.addSelectionListener( lsDef );
        wIterationGap.addSelectionListener(lsDef);

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
        wPrefix.setText(String.valueOf(input.getLearningRate()));
        wSortSize.setText(String.valueOf(input.getIterationNum()));
        wFilename.setText(input.getWeightFileName()==null?"":input.getWeightFileName());
        wIterationGap.setText(String.valueOf(input.getDisplay_iteration_gap()));
        wFileIsCommand.setSelection(input.isCompared());

        Table table = wFields.table;
        if ( input.getFieldName().length > 0 ) {
            table.removeAll();
        }
        for ( int i = 0; i < input.getFieldName().length; i++ ) {
            TableItem ti = new TableItem( table, SWT.NONE );
            ti.setText( 0, "" + ( i + 1 ) );
            ti.setText( 1, input.getFieldName()[i] );
            ti.setText( 2, input.getIsTarget()[i] ? BaseMessages.getString( PKG, "System.Combo.Yes" ) : BaseMessages
                    .getString( PKG, "System.Combo.No" ) );
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

        // copy info to LinearRegressorMeta class (input)
        input.setLearningRate( Double.parseDouble(wPrefix.getText()));
        input.setIterationNum(Integer.parseInt(wSortSize.getText()));

        input.setWeightFileName(wFilename.getText());
        input.setDisplay_iteration_gap(Integer.parseInt(wIterationGap.getText()));
        input.setCompared(wFileIsCommand.getSelection());

        // Table table = wFields.table;
        int nrfields = wFields.nrNonEmpty();
        input.allocate( nrfields );

        for ( int i = 0; i < nrfields; i++ ) {
            TableItem ti = wFields.getNonEmpty( i );
            input.getFieldName()[i] = ti.getText( 1 );
            input.getIsTarget()[i] = Utils.isEmpty( ti.getText( 2 ) ) || BaseMessages.getString( PKG, "System.Combo.Yes" ).equalsIgnoreCase( ti.getText( 2 ) );
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
