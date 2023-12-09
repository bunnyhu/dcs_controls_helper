package dcscontrol;


import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime; // Import the LocalDateTime class
import java.time.format.DateTimeFormatter; // Import the DateTimeFormatter class
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;


//import org.jsoup.nodes.Document;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
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
import javafx.scene.layout.Pane;

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
 * @version 1.2
 */
public class MainDialogController implements Initializable {

    private static Object Jsoup;
	public static final Object OBJECT = Jsoup;
	/** DCS data table */
    private ObservableList<DCSTableModel> viewTableData = FXCollections.observableArrayList();
    /** Template PDF document */
    private PDDocument _pdfDocument = null;
    /** Template PDF file name */
    private String _pdfFilename = null;
    /** DCS controls folder for Add DCS HTML */
    private String _dcsFolder = null;
    /** Used PDF folder */
    private String _pdfFolder = null;
    
    private String _aircraft = null;

    
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
        String tesztFolder = System.getProperty("user.home") + "\\Saved Games\\DCS\\InputLayoutsTxt";
        File fajl = new File(tesztFolder);
        if (fajl.exists() && fajl.isDirectory()) {
            _dcsFolder = fajl.getPath();
            eventLog("DCS folder found: " + _dcsFolder);
        } else {
            fajl = new File(tesztFolder + "\\..");
            if (fajl.exists() && fajl.isDirectory()) {
                try {
                    _dcsFolder = fajl.getCanonicalPath();
                    eventLog("DCS folder found: " + _dcsFolder);
                } catch (IOException ex) {
                    eventLog(ex.getMessage());
                    JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                fajl = new File(tesztFolder + "\\.." + "\\..");
                if (fajl.exists() && fajl.isDirectory()) {
                    try {
                        _dcsFolder = fajl.getCanonicalPath();
                        eventLog("DCS folder is not found");
                    } catch (IOException ex) {
                        eventLog(ex.getMessage());
                        JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    eventLog("Saved Games folder is not found.");
                }
            }
        }
    }

    /**
     * Add DCS HTML button action
     */
    @FXML
    private void btnOpenAction(ActionEvent event) {
        File fajl = fileOpenSave("html files", "html", 'o', _dcsFolder);
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
                _dcsFolder = fajl.getPath();
                org.jsoup.nodes.Document doc = ((org.jsoup.Jsoup) Jsoup).parse(fajl, null);
                org.jsoup.select.Elements rows = doc.body().getElementsByTag("tr");
                String c1;
                String c_temp;
                String c2;
                String c3;
                String c4 = deviceName;
                for (org.jsoup.nodes.Element row : rows) {
                    org.jsoup.select.Elements cols = row.getElementsByTag("td");
                    if (cols.size() >= 3) {
                        c_temp = cols.get(0).text().replace("\"", "");
                        c2 = cols.get(1).text();
                        c3 = cols.get(2).text();
                        
                        /**
                        *buscamos si hay un guión, es que hay dos botones: JOY_BTN4 - JOY_BTN_POV1_D
                        *y substituimos todo lo que hay delante por MOD-: MOD-JOY_BTN_POV1_D
                        *así sabemos que esa acción lleva modificador.
                        *Y en la plantilla debe existir un campo con ese nombre.
                        */
                        
                        int index = c_temp.indexOf('-');
                        if (index > 0) {
                        	c_temp = "MOD-" + c_temp.substring(index + 2);
                        }                                              
                        c1 = prefix + c_temp;
                        
                        if (!c1.isEmpty() && (!c1.equals(prefix))) {
                            viewTableData.add(new DCSTableModel(c1, c2, c3, c4, ""));
                        }
                    }
                }
                
                //Añadimos la fecha y el modelo de avión
                DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");  
                String formattedDate = LocalDateTime.now().format(myFormatObj); 
                viewTableData.add(new DCSTableModel("PRINT_DATE",formattedDate, "", "", ""));

	            if (_aircraft == null) { //para que solo pregunte una vez
                	String carpeta = fajl.getParent().substring(fajl.getParent().lastIndexOf(File.separator) + 1);
	                _aircraft = (String)JOptionPane.showInputDialog(null,
	                        "Here you can type the name of the aircraft.\n",
	                        "Optional name for aircraft "  + carpeta, JOptionPane.QUESTION_MESSAGE, null, null, carpeta);
	                if (_aircraft == null) {
	                	_aircraft = carpeta; //para el nombre del archivo
	                    return;
	                }
	                else viewTableData.add(new DCSTableModel("AIRCRAFT_TYPE", _aircraft, "", "", ""));
	            }

                Collections.sort(viewTableData);
                eventLog( fajl.getName() + " HTML file add.");
            } catch (IOException ex) {
                eventLog("File open error: " + ex.getMessage());
                JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                eventLog("Error: " + ex.getMessage());
                JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
        } catch (IOException ex) {
            eventLog("IO Error: " + ex.getMessage());
            JOptionPane.showMessageDialog(null, ex.getMessage(), "IO Error", JOptionPane.ERROR_MESSAGE);
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
            File targetPdf = fileOpenSave("PDF file", "pdf", 's', _pdfFolder);
            if (targetPdf != null) {
                if (!targetPdf.getName().toLowerCase().endsWith(".pdf")) {
                    eventLog("Target file renamed from: " + targetPdf.getCanonicalFile());
                    targetPdf = new File(targetPdf.getCanonicalFile() + ".pdf");
                    eventLog("Target file renamed to: " + targetPdf.getCanonicalFile());                    
                }
                boolean doit = false;
                if (targetPdf.exists()) {
                    if (JOptionPane.showConfirmDialog(null, targetPdf.getName() + " is exist.\nOverwrite?", "File exist", JOptionPane.YES_NO_OPTION) == 0) {
                        doit = true;
                    }
                } else {
                    doit = true;
                }
                if (doit) {
                    if (_pdfDocument != null) {
                        fillTemplateFields();
                        _pdfDocument.save(targetPdf);
                        eventLog("Filled and saved PDF template into: " + targetPdf.getName());
                        // Saving image as well
                        PDFRenderer pdfRenderer = new PDFRenderer(_pdfDocument);

                        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("-(yyMMdd HH.mm)");  
                        String formattedDate = LocalDateTime.now().format(myFormatObj); 
                        
                        for (int page = 0; page < _pdfDocument.getNumberOfPages(); ++page)
                        { 
                        	String PngFilename = targetPdf.getCanonicalFile().toString().replace(".pdf", "") + formattedDate + "-" + (page+1) + ".png";
                        	// suffix in filename will be used as the file format
                        	BufferedImage bim = pdfRenderer.renderImageWithDPI(page, 200, ImageType.RGB);
                            //si existe la eliminamos antes                   	
                        	try { 
                                // Get the file 
                                File f = new File(PngFilename);  
                                // delete file
                                //System.out.println("ahora veremos si existe");
                                //if (f.exists()) {
                                //	System.out.println("Exists a previous image"); 
	                            //    if (f.delete()) {
	                            //        System.out.println("Previous file deleted"); 
	                                	//ImageIOUtil.writeImage(bim, PngFilename, 200);
	                                	//eventLog("Saved as PNG into: " + PngFilename );
	                            //    }
	                            //    else
	                            //        System.out.println("Previous file was not deleted"); 
                                //}
                                //else
                                	ImageIOUtil.writeImage(bim, PngFilename, 200);
                            		eventLog("Saved as PNG into: " + PngFilename );
                        	}
                            catch (Exception e) { 
                                System.err.println(e); 
                                eventLog("Unable to save as PNG into: " + PngFilename );
                            } 
            
                        }
                        
                    } else {
                        eventLog("Cannot write target pdf.");
                    }
                }
            }
        } catch (Exception ex) {
            eventLog(ex.getMessage());
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        repaint();
    }

    /**
     * Open PDF Template button action
     */
    @FXML
    private void btnOpenTemplateAction(ActionEvent event) {
        try {
            File originalPdf = fileOpenSave("PDF template file", "pdf", 'o', _pdfFolder);
            if (originalPdf != null) {
                if (originalPdf.canRead()) {
                    _pdfFilename = null;
                    if (_pdfDocument != null) {
                        _pdfDocument.close();
                    }
                    _pdfDocument = PDDocument.load(originalPdf);
                    _pdfFilename = originalPdf.getName();
                    _pdfFolder =  originalPdf.getPath();
                    eventLog("Template PDF file open: " + _pdfFilename);
                } else {
                    eventLog("The template file is not readable.");
                }
            }
        } catch (Exception ex) {
            eventLog(ex.getMessage());
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
    private File fileOpenSave(String filterDetail, String extension, char command, String directory) {
        try {
            JFileChooser chooser = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter(filterDetail, extension);
            chooser.setFileFilter(filter);
            if (directory != null && !directory.isEmpty()) {
                chooser.setCurrentDirectory(new File(directory));
            }
            int returnVal = JFileChooser.CANCEL_OPTION;
            switch ( Character.toLowerCase(command) ) {
                case 'o': 
                	returnVal = chooser.showOpenDialog(null);
                    break;
                case 's': 
                	DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("-(yyMMdd HH.mm)");  
                    String formattedDate = LocalDateTime.now().format(myFormatObj);
                	chooser.setSelectedFile(new File(_aircraft + formattedDate + ".pdf"));
                	returnVal = chooser.showSaveDialog(null);
                    break;
            }
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                return chooser.getSelectedFile().getAbsoluteFile();
            }
        } catch (Exception ex) {
            eventLog(ex.getMessage());
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }

    /**
     * Fill the PDF template form fields with data array
     * <p>
     * There is special virtual field in the template: <b>Modes</b>
     * <p>
     * This field (suggested a multiline text field) filled with all Modes function, that is
     * usually binded to the 1-9 numbers, like 1) Navigation Modes , 3) Close Air Combat
     * etc. You must read the <i>Keyboard.html</i>  file to fill this field!
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
                    if (field.getFullyQualifiedName().equals("Modes")) {
                        if (viewTableData.get(i).getGroup().equals(field.getFullyQualifiedName())) {
                            if (viewTableData.get(i).getAction().matches("\\(\\d\\).*")) {
                                field.setValue(field.getValueAsString() + viewTableData.get(i).getAction() + "\n");
                            }
                        }
                    } else if (viewTableData.get(i).getBindkey().equals(field.getFullyQualifiedName())) {
                        field.setValue(viewTableData.get(i).getAction());
                        break;
                    }
                }
            }
        } catch (IOException ex) {
            eventLog("I/O error: " + ex.getMessage());
            JOptionPane.showMessageDialog(null, ex.getMessage(), "IO Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            eventLog("Error: " + ex.getMessage());
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
            icoBtnTemplate.setText("✓");
            btnAddHtml.setDisable(false);;
        }
        if (!viewTableData.isEmpty()) {
            labelStep1Info.setText(labelStep1Info.getText() + " " + viewTableData.size() + " binded actions");
            icoBtnAddHtml.setText("✓");
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
