package inspect;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openide.util.Exceptions;

import star.base.report.ReportMonitor;
import star.common.*;

@SuppressWarnings("unchecked")
public class JavaClassSample2 extends StarMacro {

    Simulation sim;
    final String FS = File.separator;
    private File excelFile;
    private String workingDirectory;
    private String fileName;
    private XSSFWorkbook workbook;
    FileInputStream inputStream;
    
    class ParticlesDecayData{
		double particlesCountEOI;
		double airborneParcelsCountEOI;
		double airborneToOverallRatioEOI;
		double particlesDecayTime;
		double parcelsDecayTime;
	    	
		void extractCfdData(){

	        double[] physicalTime;
	        double[] overallParticlesMonitorData;
	        double[] airborneParticlesMonitorData;
	        double[] airborneParcelsMonitorData;
	        double[] airborneParticlesAfterEOI;
	        double[] airborneParcelsAfterEOI;

	        
	        //quantities of interest at the end of the particles injection
	        double particlesCountEOI;
	        double airborneParticlesCountEOI;
	        double airborneParcelsCountEOI;
	        double residualAirborneParticlesCount; // 1% of the count of airborne particles at EOI;
	        double residualAirborneParcelsCount; // 1% of the count of airborne parcels at EOI;

	        double particlesDecayTime;
	        double parcelsDecayTime;
			
			
			
			
			ReportMonitor overallParticlesMonitor = 
	        	      ((ReportMonitor) sim.getMonitorManager().getMonitor("Overall particles"));
	        ReportMonitor airborneParticlesMonitor = 
	      	      ((ReportMonitor) sim.getMonitorManager().getMonitor("Airborne particles"));
	        ReportMonitor airborneParcelsMonitor = 
	      	      ((ReportMonitor) sim.getMonitorManager().getMonitor("Airborne parcels"));
	      
	        physicalTime = overallParticlesMonitor.getAllSamples().getXValues();
	        overallParticlesMonitorData = overallParticlesMonitor.getAllSamples().getYValues();
	        airborneParticlesMonitorData = airborneParticlesMonitor.getAllSamples().getYValues();
	        airborneParcelsMonitorData = airborneParcelsMonitor.getAllSamples().getYValues();


	        //calculate decay time for particles
	        int endOfInjection = findTargetValue(physicalTime, 60.1);
	        particlesCountEOI = overallParticlesMonitorData[endOfInjection];
	        airborneParticlesCountEOI = airborneParticlesMonitorData[endOfInjection];
	        airborneParcelsCountEOI = airborneParcelsMonitorData[endOfInjection];
	        residualAirborneParticlesCount = airborneParticlesCountEOI/100;
	        residualAirborneParcelsCount = airborneParcelsCountEOI/100;

	        airborneParticlesAfterEOI = Arrays.copyOfRange(airborneParticlesMonitorData, endOfInjection, airborneParticlesMonitorData.length - 1);
	        airborneParcelsAfterEOI = Arrays.copyOfRange(airborneParcelsMonitorData, endOfInjection, airborneParcelsMonitorData.length - 1);

	        int particlesDecayIndex = endOfInjection + findTargetValue(airborneParticlesAfterEOI, residualAirborneParticlesCount, true);
	        particlesDecayTime = particlesDecayIndex <= (physicalTime.length - 1) ? physicalTime[particlesDecayIndex] : 0;
	        
	        int parcelsDecayIndex = endOfInjection + findTargetValue(airborneParcelsAfterEOI, residualAirborneParcelsCount, true);
	        parcelsDecayTime = physicalTime[parcelsDecayIndex];
	        
	        double airborneToOverallRatioEOI = airborneParticlesCountEOI / particlesCountEOI;
		
			this.particlesCountEOI = particlesCountEOI;
			this.airborneParcelsCountEOI = airborneParcelsCountEOI;
			this.airborneToOverallRatioEOI = airborneToOverallRatioEOI;
			this.particlesDecayTime = particlesDecayTime;
			this.parcelsDecayTime = parcelsDecayTime;

		}
    	
		void printCfdData() {
	        sim.println(
	        	"--- particles decay information:\n"
	        	+	"total number of particles injected:"+ particlesCountEOI + ";\n"
	        	+   "number of airborne parcels injected:" + airborneParcelsCountEOI + ";\n"
	        	+	"ratio airborne / injected @ 60.1 s:"+ airborneToOverallRatioEOI + ";\n"
	        	+	"time to reach 1% airborne left (particles):"+ particlesDecayTime + ";\n"
	        	+	"time to reach 1% airborne left (parcels):"+ parcelsDecayTime + ";\n"
	        	);
		}
		
	    List<List> createDataStructure() {
	    	List<List> dataStructure = new ArrayList<List>();
	    		
		    	List dataRow = new ArrayList<>();
				dataRow.add("total number of particles injected");
				dataRow.add(this.particlesCountEOI);
				dataStructure.add(dataRow);
				
		    	dataRow = new ArrayList<>();
				dataRow.add("number of airborne parcels injected");
				dataRow.add(this.airborneParcelsCountEOI);
				dataStructure.add(dataRow);
				
		    	dataRow = new ArrayList<>();
				dataRow.add("ratio airborne / injected @ 60.1 s");
				dataRow.add(this.airborneToOverallRatioEOI);
				dataStructure.add(dataRow);
				
				double decayTime = this.particlesDecayTime == 0 ? this.parcelsDecayTime : this.particlesDecayTime;
		    	dataRow = new ArrayList<>();
				dataRow.add("time to reach 1% airborne left");
				dataRow.add(decayTime);
				dataStructure.add(dataRow);
			return dataStructure;
	    }
	}
		
    
    
    
    @Override
    public void execute() {
        sim = getActiveSimulation();

        // start time for execution time calculation
        double startTime = (double) System.currentTimeMillis();
        
        try {
            process();
            
        } catch (IllegalArgumentException e) {
            sim.println(e.getMessage());
            
        } catch (Exception e) {
            sim.println("\nProblem during macro execution!");
            sim.println("Message: " + e.getMessage());
            sim.println("Class: " + e.getClass());
            sim.println("Stack trace:");
            for (StackTraceElement s : e.getStackTrace()) {
                sim.println(s.toString());
            }
        }
        
        // execution time calculation
        double stopTime = (double) System.currentTimeMillis();
        sim.println("\nExecution time: " + ((stopTime - startTime) / 1000) + " s\n");
    }

    private void process() {

        // code to be run...
      
        ParticlesDecayData particlesDecay = new ParticlesDecayData();
        particlesDecay.extractCfdData();
        particlesDecay.printCfdData();
        
        // write data to excel sheet
    	getWorkBook();
    	createBAK();
    	
    	//handling of sheets must be update
    	XSSFSheet dataSheet = createNewSheet();
        
    	List<List> dataStructure = particlesDecay.createDataStructure();
	    
    	Boolean writeData2Column = false;
        writeCfdData2Sheet (dataSheet, dataStructure, 0, 5, writeData2Column);
    	
        updateFormulas();
        writeWorkbook();

    }
    
    int findTargetValue(double[] array, double target, boolean reverseOrder){  
    	double[] auxArray = new double[array.length];
    	int index;
    	if(reverseOrder) {
    		for(int i = 0; i < array.length  - 1; i++) {
    			auxArray[i] = array[array.length - 1 -i];
    		}
    		index = array.length - findTargetValue(auxArray, target);
    		
    	}else {
    		index = findTargetValue(array, target);
    	}
        return index;  
    }  
    
    int findTargetValue(double[] array, double target){  
        int start = 0, end = array.length - 1;  
        int index = -1;  
        while (start <= end) {  
            int mid = (start + end) / 2;  
            
            if(array[mid] == target) {
            	return index; 
            }
            // Move to right side if target is greater.
            else if (array[mid] < target) {  
                start = mid + 1;  
            } 
            // Move left side.  
            else {  
                index = mid;  
                end = mid - 1;  
            }  
        }  
        return index;  
    }  


    private File openExcelFile() {
        File xls = null;
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(sim.getSessionDir()));
        int returnValue = chooser.showOpenDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            xls = chooser.getSelectedFile();
        }
        return xls;
    }
    
    private void getWorkBook(){
	   excelFile = openExcelFile();
	   workingDirectory = excelFile.getParent();
       fileName = excelFile.getName();
       try {
       	FileInputStream inputStream = null;
           inputStream = new FileInputStream(excelFile);
           workbook = new XSSFWorkbook(inputStream);
           //NOTE: I want to close the stream here
           //inputStream.close();
           
       } catch (IOException ex) {
           Exceptions.printStackTrace(ex);
       }
   }   		
    
    private void createBAK() {
	   // create Backup file
		String extension = ".xls" + fileName.split(".xls")[1];   // solution to handle *.xlsx, *.xlsm and *.xlsb
       try { 
		   // create backup file
		   File bakFile = new File(workingDirectory + FS + fileName.replace(extension, "_BAK" + extension));

			try {
				Files.copy(excelFile.toPath(), bakFile.toPath(), REPLACE_EXISTING);
			} catch (IOException e) {
				throw new IOException("Back-up file cannot be written! Check, if it is opened!");
			}				
       } 
       catch (Exception e) { 
           e.printStackTrace(); 
       }
    }

    private XSSFSheet createNewSheet() {
    	XSSFSheet cfdSheet;
        // CFD result sheet 
        String cfdShName = "config_xx";
        while (true) {   // the input dialog will be kept open until a valid input is available or the input is cancelled!
            Object userInput = JOptionPane.showInputDialog(
                    null,
                    "Specify sheet name:",
                    "User input required",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    null,
                    cfdShName);

            if (userInput == null) {
                throw new IllegalArgumentException("User aborted specification of sheet name!");

            } else {
                cfdShName = (String) "CFD_data_" + userInput;
                if (cfdShName.isEmpty()) {
                    JOptionPane.showMessageDialog(
                            null,
                            "Sheet name must not be empty!",
                            "Warning",
                            JOptionPane.WARNING_MESSAGE);
                    cfdShName = "CFD_data_config_xx"; // just to fill empty input text field...

                } else if (((String) userInput).length() > 30) {
                    JOptionPane.showMessageDialog(
                            null,
                            "Sheet name must not contain more than 31 characters!",
                            "Warning",
                            JOptionPane.WARNING_MESSAGE);
                    // here, we keep the original input to allow the user a change...

                } else if (workbook.getSheet(cfdShName) != null) {
                    int answer = JOptionPane.showConfirmDialog(
                            null,
                            "Sheet '" + cfdShName + "' already existing!\n"
                            + "Do you want to overwrite it?",
                            "User input required",
                            JOptionPane.YES_NO_OPTION);
                    if (answer == JOptionPane.YES_OPTION) {
                        int index = workbook.getSheetIndex(cfdShName);
                        workbook.removeSheetAt(index);
                        cfdSheet = workbook.createSheet(cfdShName);
                        break;
                    }

                } else {   // default case: new sheet created with user input
                    cfdSheet = workbook.createSheet(cfdShName);
                    break;
                }
            }
        }
        return cfdSheet;
    }
    
    @SuppressWarnings("rawtypes")
	

    private void writeCfdData2Sheet(XSSFSheet sheet, @SuppressWarnings("rawtypes") List<List> dataStructure, Boolean write2Cols) {
    	writeCfdData2Sheet(sheet, dataStructure, 0, 0, write2Cols);
    }
    
    private void writeCfdData2Sheet(XSSFSheet sheet, @SuppressWarnings("rawtypes") List<List> dataStructure, int firstRow, int firstColumn, Boolean write2Cols) {
    	//method's variables  
    	XSSFRow row;
    	XSSFCell cell;
    	Object field;
       	
    	if(sheet == null) {
    		sim.println(" sheet is null");
    		return;
    	}
    	
    	int numOfColumns;
    	int numOfRows = dataStructure.size();

    	if (write2Cols) {
	    	// quantities in the rows
	    	for(int j = 0; j < numOfRows; j++) {
	    		// initialize row (this is the major different between writing data 2 columns or to rows)
	            row = sheet.getRow(firstRow + j);
	            if(row == null) {
	            	row = sheet.createRow(firstRow + j);
	            }
	    		// names in the columns
	        	numOfColumns = dataStructure.get(j).size();

	    		for(int i = 0; i < numOfColumns; i++) {
	    			field = dataStructure.get(i).get(j);
	    			// set cell value to string
	    			cell = row.createCell(firstColumn + i);
	    			
	                if (field instanceof String) {
	                    cell.setCellValue((String) field);
	        			cell.setCellType(XSSFCell.CELL_TYPE_STRING);
	                } else if (field instanceof Integer) {
	                    cell.setCellValue((Integer) field);
	                    cell.setCellType(XSSFCell.CELL_TYPE_NUMERIC);
	                } else if (field instanceof Double) {
	                	cell.setCellValue((Double) field);
	                	cell.setCellType(XSSFCell.CELL_TYPE_NUMERIC);
	                }
	    		}
	    	}
    	}else {
	    	// quantities in the rows
	    	for(int j = 0; j < numOfRows; j++) {
	    		// initialize row (this is the major different between writing data 2 columns or to rows)
	            row = sheet.getRow(firstRow + j);
	            if(row == null) {
	            	row = sheet.createRow(firstRow + j);
	            }
	    		// names in the columns
	        	numOfColumns = dataStructure.get(j).size();
	    		for(int i = 0; i < numOfColumns; i++) {
	    			field = dataStructure.get(j).get(i);
	    			// set cell value
	    			cell = row.createCell(firstColumn + i);
	                if (field instanceof String) {
	                    cell.setCellValue((String) field);
	                } else if (field instanceof Integer) {
	                    cell.setCellValue((Integer) field);
	                } else if (field instanceof Double) {
	                	cell.setCellValue((Double) field);
	                }
	    		}
	    	}
    	}
    }

    private void writeWorkbook() {
	     // Write new excel file with results
	        try { 
	            FileOutputStream out = new FileOutputStream(new File(workingDirectory +File.separator + fileName)); 
	            workbook.write(out); 
	            out.close(); 
				sim.println( "\n------------------------------\n" + fileName + " written successfully on disk." + "\n------------------------------\n");
	        } catch (FileNotFoundException e) {
	            e.printStackTrace();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
  }
    
    private void updateFormulas() {
        // see: https://poi.apache.org/components/spreadsheet/eval.html

        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        List<String> allErrors = new ArrayList<>();

        for (Sheet sheet : workbook) {
            StringJoiner errors = new StringJoiner(", ");
            for (Row r : sheet) {
                for (Cell c : r) {
                    if (c.getCellType() == Cell.CELL_TYPE_FORMULA) {
                        try {
                            evaluator.evaluateFormulaCell(c);
                        } catch (Exception e) {
                            errors.add(c.getAddress().formatAsString());
                        }
                    }
                }
            }
            if (errors.length() > 0) {
                allErrors.add("- Sheet '" + sheet.getSheetName() + "': " + errors.toString());
            }
        }

        if (allErrors.size() > 0) {
            sim.println("\nFollowing formulas in Excel could not be updated by the macro:");
            allErrors.forEach(s -> sim.println(s));
            sim.println("Usually, Excel will recalculate them be itself!");
        }

    }
    

}
