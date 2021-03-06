/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2007 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package ar.com.ergio.print.fiscal.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import java.util.logging.Level;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.compiere.apps.ADialog;
import org.compiere.apps.AEnv;
import org.compiere.apps.ConfirmPanel;
import org.compiere.apps.Preference;
import org.compiere.apps.PrintScreenPainter;
import org.compiere.apps.SwingWorker;
import org.compiere.model.MRole;
import org.compiere.plaf.CompiereColor;
import org.compiere.swing.CDialog;
import org.compiere.swing.CLabel;
import org.compiere.swing.CPanel;
import org.compiere.swing.CTextPane;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;

import ar.com.ergio.model.FiscalDocumentPrint;
import ar.com.ergio.model.FiscalDocumentPrint.Actions;
import ar.com.ergio.model.FiscalDocumentListener;
import ar.com.ergio.model.MFiscalPrinter;
import ar.com.ergio.print.fiscal.FiscalPacket;
import ar.com.ergio.print.fiscal.FiscalPrinterDevice;
import ar.com.ergio.print.fiscal.FiscalPrinterListener;
import ar.com.ergio.print.fiscal.msg.FiscalMessage;
import ar.com.ergio.print.fiscal.msg.FiscalMessages;

public class AInfoFiscalPrinter extends CDialog implements ActionListener, FiscalPrinterListener,
        FiscalDocumentListener
{

    private static final long serialVersionUID = 731761102221654153L;

    private static CLogger log = CLogger.getCLogger(AInfoFiscalPrinter.class);
    private static Icon i_inform = Env.getImageIcon("Inform32.gif");
    private static Icon i_warn = Env.getImageIcon("Warn32.gif");
    private static Icon i_question = Env.getImageIcon("Question32.gif");
    private static Icon i_error = Env.getImageIcon("Error32.gif");
    private static Icon i_busy = Env.getImageIcon("Busy.gif");
    private static Icon i_printing = Env.getImageIcon("Printing.gif");

    private static String NL = "<br>";

    // Mapeo entre los botones del ConfirmPanel y las acciones
    // NOTA: se asignan de esta forma por cuestiones de ordenamiento en la
    // interfaz gr??fica
    private static final String ACTION_REPRINT = ConfirmPanel.A_RESET;
    private static final String ACTION_VOID = ConfirmPanel.A_REFRESH;
    private static final String ACTION_EXPAND_INFO = ConfirmPanel.A_ZOOM;

    private ConfirmPanel confirmPanel = new ConfirmPanel(false, true, true, false, false, true,
            false);
    private CLabel iconLabel = new CLabel();
    private FlowLayout westLayout = new FlowLayout();
    private CTextPane infoDetail = new CTextPane();

    private BorderLayout infoLayout = new BorderLayout();
    private CPanel westPanel = new CPanel();
    private CPanel infoPanel = new CPanel();
    private StringBuffer detailText = new StringBuffer("");
    private CTextPane info = new CTextPane();

    // Menu
    private JMenuBar menuBar = new JMenuBar();
    private JMenu mFile = AEnv.getMenu("File");
    private JMenuItem mEMail = new JMenuItem();
    private JMenuItem mPrintScreen = new JMenuItem();
    private JMenuItem mScreenShot = new JMenuItem();
    private JMenuItem mEnd = new JMenuItem();
    private JMenuItem mPreference = new JMenuItem();

    private Properties ctx = Env.getCtx();
    private FiscalDocumentPrint fiscalDocumentPrint;
    private int windowNo;
    private boolean firstCommand = true;

    /**
     * Constructor de la clase ...
     */
    public AInfoFiscalPrinter(final DialogActionListener dialogActionListener, int windowNo,
                    final String title)
    {
        this((JFrame) Env.getWindow(windowNo), title, "", JOptionPane.INFORMATION_MESSAGE);
        this.windowNo = windowNo;
        setDialogActionListener(dialogActionListener);
        // this.parent = parent;
        log.info("AInfoFiscalPrinter instance created");
    } // AInfoFiscalPrinter

    /**
     * Constructor de la clase ...
     *
     * @param frame
     * @param title
     * @param message
     * @param messageType
     */

    public AInfoFiscalPrinter(Frame frame, String title, String message, int messageType)
    {
        super(frame, title, frame != null);
        setLayout(new BorderLayout());
        common(message, messageType);
        AEnv.positionCenterScreen(this);
        setResizable(false);
        setModal(true);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        infoDetail.setAutoscrolls(true);
    }

    /**
     * Descripci??n de M??todo
     *
     *
     * @param messageType
     */

    private void setInfoIcon(int messageType)
    {
        switch (messageType) {
        case JOptionPane.ERROR_MESSAGE:
            iconLabel.setIcon(i_error);

            break;
        case JOptionPane.INFORMATION_MESSAGE:
            iconLabel.setIcon(i_inform);

            break;
        case JOptionPane.QUESTION_MESSAGE:
            // confirmPanel.getCancelButton().setVisible( true );
            iconLabel.setIcon(i_question);

            break;
        case JOptionPane.WARNING_MESSAGE:
            iconLabel.setIcon(i_warn);

            break;
        case JOptionPane.PLAIN_MESSAGE:
        default:
            break;
        } // switch
    } // setInfo

    private void setInfoIcon(Icon icon)
    {
        iconLabel.setIcon(icon);
    }

    private void common(String message, int messageType)
    {

        CompiereColor.setBackground(this);

        try {
            addInfoMessage(message);
            jbInit();
            setInfoIcon(messageType);
        } catch (Exception ex) {
            log.log(Level.SEVERE, "AInfoFiscalPrinter.common - " + ex.getMessage());
        }

        // Default Button

        // this.getRootPane().setDefaultButton( confirmPanel.getOKButton());
    } // common

    /**
     * Descripci??n de M??todo
     *
     */

    private void jbInit() throws Exception
    {

        westPanel.setLayout(westLayout);
        westPanel.setName("westPanel");
        westPanel.setRequestFocusEnabled(false);
        infoPanel.setLayout(infoLayout);
        infoPanel.setName("infoPanel");
        infoPanel.setRequestFocusEnabled(false);
        CPanel confirmContentPanel = new CPanel();
        confirmContentPanel.setLayout(new BorderLayout());
        confirmContentPanel.add(confirmPanel);
        confirmContentPanel.setBorder(BorderFactory.createEmptyBorder(8, 9, 6, 0));
        this.getContentPane().add(confirmContentPanel, BorderLayout.SOUTH);
        this.getContentPane().add(westPanel, BorderLayout.WEST);
        westPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 0));
        westPanel.add(iconLabel);// ,new GridBagConstraints(
                                 // 0,0,1,1,0.0,0.0,GridBagConstraints.CENTER,GridBagConstraints.BOTH,new
                                 // Insets( 10,10,10,10 ),0,0 ));
        this.getContentPane().add(infoPanel, BorderLayout.CENTER);

        CPanel infoMPanel = new CPanel(new GridBagLayout());
        infoMPanel.add(info,
                new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
                        GridBagConstraints.BOTH, new Insets(10, 10, 0, 10), 0, 0));
        infoPanel.add(infoMPanel, BorderLayout.NORTH);

        CPanel infoDetailMPanel = new CPanel(new GridBagLayout());
        infoDetailMPanel
                .add(infoDetail, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(5, 10, 3, 10), 0, 0));
        infoPanel.add(infoDetailMPanel, BorderLayout.CENTER);

        // Detail text
        infoDetail.setPreferredSize(new Dimension(450, 90));
        infoDetail.setRequestFocusEnabled(false);
        infoDetail.setEnabled(false);
        infoDetail.setOpaque(true);
        infoDetail.setReadWrite(false);
        infoDetail.setAutoscrolls(true);
        infoDetail.setVisible(false);

        //
        // Confirm Panel
        //

        // OK --> Acci??n de Reimprimir
        getReprintButton().setVisible(false);
        getReprintButton().setText(Msg.getMsg(Env.getCtx(), "Reprint"));
        getReprintButton().setIcon(Env.getImageIcon("Print24.gif"));

        // Cancel --> Acci??n de anular factura
        getVoidButton().setVisible(false);
        getVoidButton().setText(Msg.getMsg(Env.getCtx(), "VoidInvoice"));
        getVoidButton().setIcon(Env.getImageIcon("Delete24.gif"));

        // Zoom --> Expandir/Contraer detalle de info
        getExpandInfoButton().setVisible(true);
        getExpandInfoButton().setIcon(Env.getImageIcon("Zoom24.gif"));

        getOkButton().setVisible(false);
        confirmPanel.getCancelButton().setVisible(false);
        // Menu

        mEMail.setIcon(Env.getImageIcon("EMailSupport16.gif"));
        mEMail.setText(Msg.getMsg(Env.getCtx(), "EMailSupport"));
        mEMail.addActionListener(this);
        mPrintScreen.setIcon(Env.getImageIcon("PrintScreen16.gif"));
        mPrintScreen.setText(Msg.getMsg(Env.getCtx(), "PrintScreen"));
        mPrintScreen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_PRINTSCREEN, 0));
        mPrintScreen.addActionListener(this);
        mScreenShot.setIcon(Env.getImageIcon("ScreenShot16.gif"));
        mScreenShot.setText(Msg.getMsg(Env.getCtx(), "ScreenShot"));
        mScreenShot.setAccelerator(KeyStroke
                .getKeyStroke(KeyEvent.VK_PRINTSCREEN, Event.SHIFT_MASK));
        mScreenShot.addActionListener(this);
        mPreference.setIcon(Env.getImageIcon("Preference16.gif"));
        mPreference.setText(Msg.getMsg(Env.getCtx(), "Preference"));
        mPreference.addActionListener(this);
        mEnd.setIcon(Env.getImageIcon("End16.gif"));
        mEnd.setText(Msg.getMsg(Env.getCtx(), "End"));
        mEnd.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, Event.ALT_MASK));
        mEnd.addActionListener(this);

        this.setJMenuBar(menuBar);

        menuBar.add(mFile);
        mFile.add(mPrintScreen);
        mFile.add(mScreenShot);
        mFile.addSeparator();
        mFile.add(mEMail);

        if (MRole.getDefault().isShowPreference()) {
            mFile.add(mPreference);
        }

        // mFile.addSeparator();
        // mFile.add( mEnd );

        //
        confirmPanel.addActionListener(this);
        setInfoMessage("");
    } // jbinit

    private void setInfoMsg(String message)
    {
        message = Msg.translate(ctx, message);
        StringBuffer sb = new StringBuffer(message.length() + 20);

        /*
         * if(message.indexOf("\n") == -1) { sb.append( NL ); }
         */

        sb.append("<b>");

        char[] chars = message.toCharArray();
        boolean first = true;
        int paras = 0;

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];

            if (c == '\n') {
                if (first) {
                    sb.append("</b>");
                    first = false;
                }

                if (paras > 1) {
                    sb.append(NL);
                } else {
                    sb.append(NL);
                }

                paras++;
            } else {
                sb.append(c);
            }
        }

        info.setText(sb.toString());

        Dimension size = info.getPreferredSize();

        size.width = 450;
        size.height = (Math.max(paras, message.length() / 60) + 1) * 30;
        size.height = Math.min(size.height + 40, 600);
        size.height = 70;

        info.setPreferredSize(size);

        // Log.print("Para=" + paras + " - " + info.getPreferredSize());

        info.setRequestFocusEnabled(false);
        info.setReadWrite(false);
        info.setOpaque(false);
        info.setBorder(null);

        //

        info.setCaretPosition(0);
    }

    public void addInfoMessage(FiscalMessage message)
    {
        addInfoMessage(message.getDescription(), message.isError());
    }

    public void addInfoMessage(String message)
    {
        addInfoMessage(message, false);
    }

    public void addInfoMessage(String message, boolean error)
    {
        String color = "black";
        // Se traduce el mensaje.
        message = Msg.translate(ctx, message);
        if (message.equals(""))
            return;

        StringBuffer sb = new StringBuffer();
        sb.append(detailText);

        if (error) {
            color = "red";
            // message = "<b>" + message + "</b>";
        } else {
            color = "navy";
        }
        sb.append("<font face='Tahoma' size=2 color=" + color + ">");
        sb.append(message);
        sb.append("</font>");

        if (!detailText.equals(""))
            sb.append(NL);

        detailText = sb;
        infoDetail.setText(sb.toString());

        // Se hace un scroll autom??tico hacia el final del panel.
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                JScrollBar vScroll = infoDetail.getVerticalScrollBar();
                vScroll.setValue(vScroll.getMaximum());
            }
        });
    }

    public void actionPerformed(ActionEvent e)
    {
        // log.finest( "ADialogDialog.actionPerformed - " + e);
        if (e.getActionCommand().equals(ACTION_REPRINT)) {
            reprintDocument();
        } else if (e.getActionCommand().equals(ACTION_VOID)) {
            fireVoidAction();
            dispose();
        } else if (e.getActionCommand().equals(ACTION_EXPAND_INFO)) {
            infoDetail.setVisible(!infoDetail.isVisible());
            pack();
            repaint();
        } else if (e.getActionCommand().equals(ConfirmPanel.A_OK)) {
            dispose();
        } else if (e.getActionCommand().equals(ConfirmPanel.A_CANCEL)) {
            getFiscalDocumentPrint().setCancelWaiting(true);
            dispose();
        } else if (e.getSource() == mPrintScreen) {
            printScreen();
        } else if (e.getSource() == mEMail) {
            String title = getTitle();
            String text = infoDetail.getText();

            dispose(); // otherwise locking
            ADialog.createSupportEMail(this, title, text);
        } else if (e.getSource() == mPreference) {
            if (MRole.getDefault().isShowPreference()) {
                Preference p = new Preference(null, 0);

                p.setVisible(true);
            }
        }
    }

    private void setErrorStatus()
    {
        setInfoIcon(JOptionPane.ERROR_MESSAGE);
        getReprintButton().setVisible(reprintButtonActive);
        getVoidButton().setVisible(voidButtonActive);
        getOkButton().setVisible(okButtonActive);
    }

    private void setBusyStatus()
    {
        iconLabel.setIcon(i_busy);
        confirmPanel.getCancelButton().setVisible(true);
        // confirmPanel.getOKButton().setEnabled(false);
        getReprintButton().setVisible(false);
        getVoidButton().setVisible(false);
        getOkButton().setVisible(false);
    }

    private void setPrintingStatus()
    {
        setInfoIcon(JOptionPane.INFORMATION_MESSAGE);
        getReprintButton().setVisible(false);
        getVoidButton().setVisible(false);
        getOkButton().setVisible(false);
    }

    public void commandExecuted(FiscalPrinterDevice source, final FiscalPacket command,
            FiscalPacket response)
    {
        Runnable doCommandExecuted = new Runnable()
        {
            public void run()
            {
                if (firstCommand) {
                    addInfoMessage(createActionMessage("ExecutingFiscalCommands"));
                    firstCommand = false;
                }
                addInfoMessage("[OK]: " + command.toString());
            }
        };
        invoke(doCommandExecuted, false);
    }

    public void printEnded(FiscalPrinterDevice source, FiscalMessages msgs)
    {
        Runnable doPrintEnded = new Runnable()
        {
            public void run()
            {
                addInfoMessage("FiscalPrintEndedOk");
                dispose();
            }
        };
        invoke(doPrintEnded, false);
    }

    public void statusChanged(FiscalPrinterDevice source, final FiscalPacket command,
            FiscalPacket response, final FiscalMessages msgs)
    {
        Runnable doStatusChange = new Runnable()
        {
            public void run()
            {
                // Hay estado de error
                if (msgs.hasErrors()) {
                    String msg = "[ERR]: " + command;
                    addInfoMessage(msg, true);
                    addInfoMessage("<b>" + translate("Errors") + "</b>", true);
                    addInfoMessage(msgs.getErrorsAsString(NL), true);
                    setErrorStatus();
                }
            }
        };
        invoke(doStatusChange, false);
    }

    public void statusReported(FiscalDocumentPrint fdp, MFiscalPrinter source, String status)
    {
        if (status.equals(MFiscalPrinter.STATUS_Error)) {
            invoke(doErrorStatusReported, true);
        } else if (status.equals(MFiscalPrinter.STATUS_IDLE)) {
            invoke(doBusyStatusReported, false);
        } else if (status.equals(MFiscalPrinter.STATUS_IDLE)) {
            invoke(doIdleStatusReported, false);
        }
    }

    public void actionStarted(FiscalDocumentPrint fdp, final int action)
    {
        Runnable doActionStarted = new Runnable()
        {
            public void run()
            {
                String msg = "";
                if (action == FiscalDocumentListener.AC_CHECK_STATUS) {
                    msg = createActionMessage("CheckingFiscalPrinterStatus");
                    setInfoIcon(i_busy);
                    addInfoMessage(createActionMessage("WaitingFiscalPrinterConnection"));
                } else if (action == FiscalDocumentListener.AC_CONNECT_PRINTER) {
                    msg = createActionMessage("ConnectingFiscalPrinter");
                    setInfoIcon(i_busy);
                } else if (action == FiscalDocumentListener.AC_PRINT_DOCUMENT) {
                    msg = createActionMessage("PrintingFiscalDocument");
                    setInfoIcon(i_printing);
                    addInfoMessage("FiscalPrinterConnected");
                } else if (action == FiscalDocumentListener.AC_PROCESS_DOCUMENT) {
                    msg = createActionMessage("CompletingDocument");
                    setInfoIcon(i_busy);
                } else if (action == FiscalDocumentListener.AC_EXECUTING_ACTION) {
                    msg = createActionMessage("ExecutingAction");
                    setInfoIcon(i_busy);
                }

                setInfoMessage(msg);
            }
        };
        invoke(doActionStarted, true);
    }

    public void errorOcurred(FiscalDocumentPrint source, final String errorTitle,
            final String errorDesc)
    {
        Runnable doErrorOcurred = new Runnable()
        {
            public void run()
            {
                setInfoMessage(errorTitle, errorDesc);
                setErrorStatus();
            }
        };
        invoke(doErrorOcurred, false);
    }

    public void documentPrintEndedOk(FiscalDocumentPrint source)
    {
        Runnable doDocumentPrintEndedOk = new Runnable()
        {
            public void run()
            {
                // getParent().refreshData();
                // confirmPanel.getOKButton().setEnabled(true);
                setInfoMessage("FiscalPrintEndedOk");
                setInfoIcon(i_inform);
            }
        };
        invoke(doDocumentPrintEndedOk, false);
    }

    public void actionEnded(boolean ok, Actions action)
    {
        Runnable doActionEndedOk = new Runnable()
        {
            public void run()
            {
                // getParent().refreshData();
                // confirmPanel.getOKButton().setEnabled(true);
                setInfoMessage("ActionEndedOk");
                setInfoIcon(i_inform);
            }
        };
        Runnable doActionEndedWrong = new Runnable()
        {
            public void run()
            {
                // getParent().refreshData();
                // confirmPanel.getOKButton().setEnabled(true);
                setInfoMessage("ActionEndedWrong");
                setInfoIcon(i_inform);
            }
        };
        if (ok) {
            invoke(doActionEndedOk, false);
        } else {
            invoke(doActionEndedWrong, false);
        }
    }

    /**
     * @return Returns the fiscalDocumentPrint.
     */
    public FiscalDocumentPrint getFiscalDocumentPrint()
    {
        return fiscalDocumentPrint;
    }

    /**
     * @param fiscalDocumentPrint
     *            The fiscalDocumentPrint to set.
     */
    public void setFiscalDocumentPrint(FiscalDocumentPrint fiscalDocumentPrint)
    {
        this.fiscalDocumentPrint = fiscalDocumentPrint;
    }

    /**
     * Invoca una acci??n representada por un Runnable en el hilo de eventos de
     * Swing.
     *
     * @param action
     *            Accion a invocar.
     * @param sync
     *            Indica si la invoicaci??n se debe hacer sincr??nica o
     *            asincr??nica. (<code>SwingUtilities.invoikeAndWait</code> o
     *            <code>SwingUtilities.invokeLater</code>).
     */
    private void invoke(Runnable action, boolean sync)
    {
        try {
            if (sync)
                SwingUtilities.invokeAndWait(action);
            else
                SwingUtilities.invokeLater(action);

        } catch (InterruptedException e) {
            log.log(Level.SEVERE, "Thread Interrupted: " + e.getMessage(), e);
        } catch (InvocationTargetException e) {
            log.log(Level.SEVERE, "Invocation error: " + e.getMessage(), e);
        }
    }

    private Runnable doErrorStatusReported = new Runnable()
    {
        public void run()
        {
            boolean rsp = ADialog.ask(getWindowNo(), AInfoFiscalPrinter.this,
                    "PrinterErrorConfirm", Msg.translate(Env.getCtx(), "PrinterErrorConfirmMsg"));
            confirmPanel.getCancelButton().setVisible(false);
            getFiscalDocumentPrint().setIgnoreErrorStatus(rsp);
            AInfoFiscalPrinter.this.setVisible(rsp);
            // @emmie - if user doesn't continue with fiscal printing, void actual invoice
            if (!rsp)
               fireVoidAction();
        }
    };

    private Runnable doBusyStatusReported = new Runnable()
    {
        public void run()
        {
            setInfoMessage("FiscalPrinterBusy", createActionMessage("PleaseWait"));
            setBusyStatus();
        }
    };

    private Runnable doIdleStatusReported = new Runnable()
    {
        public void run()
        {
            setInfoMessage("FiscalPrinterIdle");
            setPrintingStatus();
        }
    };

    /**
     * @return Returns the windowNo.
     */
    protected int getWindowNo()
    {
        return windowNo;
    }

    /**
     * @param windowNo
     *            The windowNo to set.
     */
    protected void setWindowNo(int windowNo)
    {
        this.windowNo = windowNo;
    }

    private void printScreen()
    {
        PrintScreenPainter.printScreen(this);
    }

    /**
     * Muestra un mensaje de informaci??n en la secci??n principal de la ventana
     * de di??logo.
     *
     * @param title
     *            T??tulo del mensaje a mostrar.
     * @param description
     *            Descripci??n del mensaje a mostrar.
     */
    public void setInfoMessage(String title, String description)
    {
        String msg = Msg.translate(ctx, title);
        if (description != null && !description.equals("")) {
            msg = msg + "\n" + Msg.translate(ctx, description);
        }
        setInfoMsg(msg);
    }

    /**
     * Muestra un mensaje de informaci??n en la secci??n principal de la ventana
     * de di??logo.
     *
     * @param title
     *            T??tulo del mensaje a mostrar.
     */
    public void setInfoMessage(String title)
    {
        setInfoMessage(title, null);
    }

    /**
     * Formatea un mensaje como una acci??n que se est?? ejecutando.
     *
     * @param msg
     *            Mensaje a formatear
     * @return Mensaje formateado.
     */
    protected String createActionMessage(String msg)
    {
        return Msg.translate(ctx, msg) + "...";
    }

    /**
     * @return Devuelve el mensaje traducido.
     */
    protected String translate(String msg)
    {
        return Msg.translate(ctx, msg);
    }

    public void clearDetail()
    {
        detailText = new StringBuffer("");
        if (infoDetail != null) {
            infoDetail.setText("");
        }
    }

    public JButton getReprintButton()
    {
        return confirmPanel.getResetButton();
    }

    public JButton getVoidButton()
    {
        return confirmPanel.getRefreshButton();
    }

    protected JButton getExpandInfoButton()
    {
        return confirmPanel.getZoomButton();
    }

    public JButton getOkButton()
    {
        return confirmPanel.getOKButton();
    }

    private boolean reprintButtonActive = false;
    private boolean voidButtonActive = false;
    private boolean okButtonActive = true;

    /**
     * @param reprintButtonActive
     *            el valor de reprintButtonActive a asignar
     */
    public void setReprintButtonActive(boolean reprintButtonActive)
    {
        this.reprintButtonActive = reprintButtonActive;
    }

    /**
     * @param voidButtonActive
     *            el valor de voidButtonActive a asignar
     */
    public void setVoidButtonActive(boolean voidButtonActive)
    {
        this.voidButtonActive = voidButtonActive;
    }

    /**
     * @param okButtonActive
     *            el valor de okButtonActive a asignar
     */
    public void setOkButtonActive(boolean okButtonActive)
    {
        this.okButtonActive = okButtonActive;
    }

    /** Listener de acciones del di??logo (Reimprimir, Anular, etc) */
    private DialogActionListener dialogActionListener = null;

    /**
     * @return el valor de dialogActionListener
     */
    public DialogActionListener getDialogActionListener()
    {
        return dialogActionListener;
    }

    /**
     * @param dialogActionListener
     *            el valor de dialogActionListener a asignar
     */
    public void setDialogActionListener(DialogActionListener dialogActionListener)
    {
        this.dialogActionListener = dialogActionListener;
    }

    private void fireVoidAction()
    {
        if (getDialogActionListener() != null) {
            getDialogActionListener().actionVoidPerformed();
        }
    }

    private void fireReprintFinishedAction()
    {
        if (getDialogActionListener() != null) {
            getDialogActionListener().actionReprintFinished();
        }
    }

    /**
     * Realiza el intento de reimpresi??n del documento invocando al
     * FiscalDocumentPrint bajo un nuevo hilo (SwingWorker)
     */
    private void reprintDocument()
    {
        SwingWorker worker = new SwingWorker()
        {
            @Override
            public Object construct()
            {
                return getFiscalDocumentPrint().reprintDocument();
            }

            @Override
            public void finished()
            {
                boolean success = (Boolean) getValue();
                if (success) {
                    fireReprintFinishedAction();
                }

            }
        };

        worker.start();
    }

    /**
     * Listener de acciones que puede disparar este di??logo.
     */
    public interface DialogActionListener
    {

        /**
         * Acci??n que indica que la reimpresi??n del documento se realiz??
         * correctamente.
         */
        public void actionReprintFinished();

        /**
         * Acci??n que indica que se presion?? el bot??n de anulaci??n de factura.
         * El Dialog solo invoca esta acci??n sin efectuar ninguna operaci??n
         * adicional
         */
        public void actionVoidPerformed();

    }

}
