package dcscontrol;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Main Controller Class, button actions, repaint.
 * <p>
 * This project is made for process the DCS (Digital Combat Simulator) joystick / Keyboard binds. 
 * Mostly used for my home made DIY controller box called DCS Buddy.
 * <p>
 * I am using a kneeboard sheet to remember the buttons functions, but sometimes 
 * I am redefined them inside DCS. It is painful to update my page because so many 
 * functions and hard to remember or found what is changed.
 * <p>
 * The project able to read the exported DCS controls HTML file and connect them 
 * to PDF form text fields. You can use multiple controllers and separate the 
 * same commands like joystick buttons with prefix code.
 * <p>
 * https://github.com/bunnyhu/dcs_buddy_helper
 * 
 * @author Bunny
 * @version 1.0
 */
public class MainDialogController implements Initializable {

    /** DCS data table */
    private ObservableList<DCSTableModel> viewTableData = FXCollections.observableArrayList();
    /** Template PDF document */
    private PDDocument _pdfDocument = null;
    /** Template PDF file name */
    private String _pdfFilename = null;

    @FXML
    private Pane paneStep2;
    @FXML
    private Label labelStep1Info;
    @FXML
    private Label labelStep2Info;
    @FXML
    private Button btnAddHtml;
    @FXML
    private Label icoBtnAddHtml;
    @FXML
    private Label icoBtnTemplate;
    @FXML
    private TextArea textEventLog;
    @FXML
    private TableView table;
    @FXML
    private TableColumn colFormField;
    @FXML
    private TableColumn colBindkey;
    @FXML
    private TableColumn colAction;
    @FXML
    private TableColumn colDevice;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initViewTable();
        repaint();
    }
    
    /**
     * Add DCS HTML button action
     */
    @FXML
    private void btnOpenAction(ActionEvent event) {
        File fajl = fileOpenSave("html files", "html", 'o');
        if (fajl != null) {
            try {
                String deviceName = DCSTableModel.getDeviceFromFilename(fajl.getName());
                String prefix = JOptionPane.showInputDialog(null,
                        "To separate two or more device, use prefix for key/button form field in PDF.\n"
                        +"For example: device without prefix: JOY_BTN1\n"
                        +"device with TM_ prefix: TM_JOY_BTN1\n\n"
                        +"If there is prefix for "+ deviceName + ", set here or leave empty:",
                        "Optional prefix for "+deviceName, JOptionPane.QUESTION_MESSAGE);
                if (prefix == null) {
                    return;
                }
                Document doc = Jsoup.parse(fajl, null);
                Elements rows = doc.body().getElementsByTag("tr");
                String c1;
                String c2;
                String c3;
                String c4 = deviceName;
                for (Element row : rows) {
                    Elements cols = row.getElementsByTag("td");
                    if (cols.size() == 3) {
                        c1 = prefix + cols.get(0).text().replace("\"", "");
                        c2 = cols.get(1).text();
                        c3 = cols.get(2).text();
                        if (!c1.isEmpty() && (!c1.equals(prefix))) {
                            viewTableData.add(new DCSTableModel(c1, c2, c3, c4, ""));
                        }
                    }
                }
                Collections.sort(viewTableData);
                eventLog( fajl.getName() + " HTML file add.");
            } catch (IOException ex) {
                eventLog("File open error: " + ex.getMessage());
            } catch (Exception ex) {
                eventLog("Error: " + ex.getMessage());
            }
        }
        tableDataRecalculate();
        repaint();
    }

    /**
     * Exit (from the application) button action
     */
    @FXML
    private void btnExitAction(ActionEvent event) {
        System.exit(0);
    }

    /**
     * Clear list button action
     */
    @FXML
    private void btnClearAction(ActionEvent event) {
        viewTableData.clear();
        textEventLog.clear();
        try {
            if (_pdfDocument != null) {
                _pdfDocument.close();
                _pdfDocument = null;
            }
        } catch (IOException e) {
            eventLog("IO Error: " + e.getMessage());
        }
        _pdfFilename = "";
        repaint();
        eventLog("Area clear, all file closed.");
    }

    /**
     * Fill & save as PDF template button action
     */
    @FXML
    private void btnFillSavePdfAction(ActionEvent event) {
        try {
            File targetPdf = fileOpenSave("PDF template file", "pdf", 's');
//            File targetPdf = new File(new URI("file:/C:/Users/Bunny/Documents/java/dcs/dcs_buddy_final.pdf"));
            if (_pdfDocument != null) {
                fillTemplateFields();
                _pdfDocument.save(targetPdf);
                eventLog("Fill and save PDF template into: " + targetPdf.getName());
            } else {
                eventLog("Cannot write target pdf.");
            }
        } catch (Exception ex) {
            eventLog(ex.getMessage());
        }
        repaint();
    }

    /**
     * Open PDF Template button action
     */
    @FXML
    private void btnOpenTemplateAction(ActionEvent event) {
        try {
            File originalPdf = fileOpenSave("PDF template file", "pdf", 'o');
//            File originalPdf = new File(new URI("file:/C:/Users/Bunny/Documents/java/dcs/dcs_buddy_template.pdf"));
            if (originalPdf != null) {
                if (originalPdf.canRead()) {
                    _pdfFilename = null;
                    if (_pdfDocument != null) {
                        _pdfDocument.close();
                    }
                    _pdfDocument = PDDocument.load(originalPdf);
                    _pdfFilename = originalPdf.getName();
                    eventLog("Template PDF file open: " + _pdfFilename);
                } else {
                    eventLog("The template file is not readable.");
                }
            }
        } catch (Exception ex) {
            eventLog(ex.getMessage());
        }
        tableDataRecalculate();
        repaint();
    }

    /**
     * Open a file
     *
     * @param filterDetail detail text for extension ex: "PDF Files"
     * @param extension file extension ex: "pdf"
     * @return HTML file or null if error or no select
     */
    private File fileOpenSave(String filterDetail, String extension, char command) {
        try {
            JFileChooser chooser = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter(filterDetail, extension);
            chooser.setFileFilter(filter);
            chooser.setCurrentDirectory(new File("c:\\Users\\Bunny\\Documents\\java\\dcs"));
            int returnVal = JFileChooser.CANCEL_OPTION;
            switch ( Character.toLowerCase(command) ) {
                case 'o': returnVal = chooser.showOpenDialog(null);
                    break;
                case 's': returnVal = chooser.showSaveDialog(null);
                    break;
            }
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                return chooser.getSelectedFile().getAbsoluteFile();
            }
        } catch (Exception ex) {
            eventLog(ex.getMessage());
        }
        return null;
    }

    /**
     * Fill the PDF template form fields with data array
     */
    @SuppressWarnings("rawtypes")
    private void fillTemplateFields() {
        try {
            PDDocumentCatalog docCatalog = _pdfDocument.getDocumentCatalog();
            PDAcroForm acroForm = docCatalog.getAcroForm();
            List fields = acroForm.getFields();
            Iterator fieldsIter = fields.iterator();
            while (fieldsIter.hasNext()) {
                PDField field = (PDField) fieldsIter.next();
                for (int i = 0; i < viewTableData.size(); i++) {
                    if (viewTableData.get(i).getBindkey().equals(field.getFullyQualifiedName())) {
                        field.setValue(viewTableData.get(i).getAction());
                        break;
                    }
                }
            }            
        } catch (IOException e) {
            eventLog("I/O error: " + e.getMessage());
        } catch (Exception e) {
            eventLog("Error: " + e.getMessage());            
        }
    }  
    
    /**
     * Repaint the main window
     * <p>
     * It is used after every modify in the app inputs
     */
    private void repaint() {
        labelStep1Info.setText("");
        icoBtnAddHtml.setText("?");
        icoBtnTemplate.setText("?");
        
        paneStep2.setDisable(true);
        labelStep2Info.setText("");
        btnAddHtml.setDisable(true);
        if ((_pdfDocument != null) && (_pdfDocument.getNumberOfPages()>0)) {
            labelStep1Info.setText(_pdfFilename);
            icoBtnTemplate.setText("✔");
            btnAddHtml.setDisable(false);;
        }
        if (!viewTableData.isEmpty()) {
            labelStep1Info.setText(labelStep1Info.getText() + " " + viewTableData.size() + " binded actions");
            icoBtnAddHtml.setText("✔");
        }
        if (!viewTableData.isEmpty() && (_pdfDocument != null)) {
            paneStep2.setDisable(false);
        }
    }

    /**
     * Log app event inside the log text area
     * @param event Logged event text (+ auto newline)
     */
    private void eventLog(String event) {
        textEventLog.appendText(event + "\n");
    }

    /**
     * Initialize the view table cells
     */
    private void initViewTable() {
        table.getColumns();
        colFormField.setCellValueFactory(
            new PropertyValueFactory<DCSTableModel,String>("formkey")
        );
        colFormField.setSortable(false);
        colBindkey.setCellValueFactory(
            new PropertyValueFactory<DCSTableModel,String>("bindkey")
        );
        colBindkey.setSortable(false);
        colAction.setCellValueFactory(
            new PropertyValueFactory<DCSTableModel,String>("action")
        );
        colAction.setSortable(false);
        colDevice.setCellValueFactory(
            new PropertyValueFactory<DCSTableModel,String>("device")
        );
        colDevice.setSortable(false);
        table.setItems(viewTableData);        
    }
    
    /**
     * Recalculate the view table and join the form field commands to the 
     * HTML imported commands. It is used after read a PDF template or a HTML
     * controller info file
     */
    private void tableDataRecalculate() {
        if (_pdfDocument != null) {
            PDDocumentCatalog docCatalog = _pdfDocument.getDocumentCatalog();
            PDAcroForm acroForm = docCatalog.getAcroForm();
            List fields = acroForm.getFields();
            Iterator fieldsIter = fields.iterator();            
            while (fieldsIter.hasNext()) {
                PDField field = (PDField) fieldsIter.next();
                for (int i = 0; i < viewTableData.size(); i++) {                    
                    if (viewTableData.get(i).getBindkey().equals(field.getFullyQualifiedName())) {
                        viewTableData.get(i).setFormkey(field.getFullyQualifiedName());
                        break;
                    }
                }
            }            
        }
    }
}
