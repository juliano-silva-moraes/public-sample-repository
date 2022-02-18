package inspect;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openide.util.Exceptions;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.awt.GraphicsEnvironment;

import star.base.neo.IntVector;
import star.base.neo.NamedObject;
import star.base.report.AnalysisReport;
import star.base.report.AreaAverageReport;
import star.base.report.ElementCountReport;
import star.base.report.MaxReport;
import star.base.report.Monitor;
import star.base.report.PhysicalTimeMonitor;
import star.base.report.Report;
import star.base.report.ReportMonitor;
import star.base.report.ScalarReport;
import star.base.report.SumReport;
import star.base.report.VolumeAverageReport;
import star.common.*;
import star.energy.HeatTransferCoefficientProfile;
import star.energy.WallThermalOption;
import star.energy.WallThermalOption.Type;
import star.flow.MassFlowAverageReport;
import star.flow.MassFlowReport;
import star.flow.ReferencePressure;

@SuppressWarnings("unchecked")
public class JavaClassSample1 extends StarMacro {
    //--- Class variables
    String positiveAnswer= "yes";
    String negativAnswer= "no";
    Simulation sim;
    FvRepresentation fvRepresentation;
    Units C;
    Units m_s;
    Units l_s;
    Units hpa;
    final String FS = File.separator;
    private File excelFile;
    private String workingDirectory;
    private String fileName;
    private XSSFWorkbook workbook;
    FileInputStream inputStream;
	//--- Macro inputs
    String extractFieldMeanValues; // yes & no
 
    //--- cabin flow data extraction	
	class BoundaryData{
		String name;
		List<PartSurfacesSubgroup> partSurfacesSubgroups;
		
		BoundaryData(){
			this.name = "empty";
			this.partSurfacesSubgroups = new ArrayList<PartSurfacesSubgroup>();
		}
		
		//--- subgroup-based data extraction
		
		//getReportData without argument field function -> used to evaluate the mass flow for the input part surfaces
		HashMap<String, Double> getReportData(ScalarReport report, List<PartSurfacesSubgroup> partSurfacesSubgroups){
			HashMap<String, Double> reportData = new HashMap<String, Double>();
			List<PartSurface> partSurfaces;
			String subgroupName;
			
		    //get report data for each subgroup
			for(PartSurfacesSubgroup subgroup : partSurfacesSubgroups) {
				subgroupName = subgroup.subgroupName;
				partSurfaces = subgroup.partSurfaces;
				if(!partSurfaces.isEmpty()) {
					report.getParts().setObjects(partSurfaces);
					reportData.put(subgroupName, report.getValue());
				}
			}
			return reportData;
		}
	
		HashMap<String, Double> getReportData(ScalarReport report, FieldFunction fieldFunction, List<PartSurfacesSubgroup> partSurfacesSubgroups){
			HashMap<String, Double> reportData = new HashMap<String, Double>();
			List<PartSurface> partSurfaces;
			String subgroupName;
			
		    report.setFieldFunction(fieldFunction);
			
		    //get report data for each subgroup
			for(PartSurfacesSubgroup subgroup : partSurfacesSubgroups) {
				subgroupName = subgroup.subgroupName;
				partSurfaces = subgroup.partSurfaces;
				if(!partSurfaces.isEmpty()) {
					report.getParts().setObjects(partSurfaces);
					reportData.put(subgroupName, report.getValue());
				}
			}
			return reportData;
		}
	
		HashMap<String, Double> getReportData(ScalarReport report, FieldFunction fieldFunction, Units units, List<PartSurfacesSubgroup> partSurfacesSubgroups){
			HashMap<String, Double> reportData = new HashMap<String, Double>();
			List<PartSurface> partSurfaces;
			String subgroupName;
			
		    report.setFieldFunction(fieldFunction);
		    report.setUnits(units);
			
		    //get report data for each subgroup
			for(PartSurfacesSubgroup subgroup : partSurfacesSubgroups) {
				subgroupName = subgroup.subgroupName;
				partSurfaces = subgroup.partSurfaces;
				if(!partSurfaces.isEmpty()) {
					report.getParts().setObjects(partSurfaces);
					reportData.put(subgroupName, report.getValue());
				}
			}
			return reportData;
			
		}
	
		//--- surface-based data extraction
		HashMap<String, Double> getReportData(ScalarReport report, ArrayList<PartSurface> partSurfaces){
			HashMap<String, Double> reportData = new HashMap<String, Double>();
			
		    //get report data for each subgroup
			for(PartSurface partSurface : partSurfaces) {
				report.getParts().setObjects(partSurface);
				reportData.put(partSurface.getPresentationName(), report.getValue());
			}
			return reportData;
		}
	
		HashMap<String, Double> getReportData(ScalarReport report, FieldFunction fieldFunction, ArrayList<PartSurface> partSurfaces){
			HashMap<String, Double> reportData = new HashMap<String, Double>();
			
		    report.setFieldFunction(fieldFunction);
			
		    //get report data for each subgroup
			for(PartSurface partSurface : partSurfaces) {
				report.getParts().setObjects(partSurfaces);
				reportData.put(partSurface.getPresentationName(), report.getValue());
			}
			return reportData;
		}
	
		HashMap<String, Double> getReportData(ScalarReport report, FieldFunction fieldFunction, Units units, ArrayList<PartSurface> partSurfaces){
			HashMap<String, Double> reportData = new HashMap<String, Double>();
			
		    report.setFieldFunction(fieldFunction);
		    report.setUnits(units);
			
		    //get report data for each subgroup
			for(PartSurface partSurface : partSurfaces) {
				report.getParts().setObjects(partSurfaces);
				reportData.put(partSurface.getPresentationName(), report.getValue());
			}
			return reportData;
		}
	}

	//--- cabin flow data extraction	

	
	class PartSurfacesSubgroup{
		String subgroupName;
		List<PartSurface> partSurfaces; 
	}
	
	class Wall extends BoundaryData{
		Type thermalSpecification;
		HashMap<String, Double> surfaceArea = new HashMap<String, Double>();
		HashMap<String, Double> ambientTemperature = new HashMap<String, Double>();
		HashMap<String, Double> heatTransferCoefficient = new HashMap<String, Double>();
		HashMap<String, Double> surfaceAverageTemperature = new HashMap<String, Double>();
		HashMap<String, Double> totalHeatTransfer = new HashMap<String, Double>();
		HashMap<String, Double> conductionHeatTransfer = new HashMap<String, Double>();
		HashMap<String, Double> radiationHeatTransfer = new HashMap<String, Double>();
		HashMap<String, Double> totalExternalHeatTransfer = new HashMap<String, Double>();
		HashMap<String, Double> externalConductionHeatTransfer = new HashMap<String, Double>();
		HashMap<String, Double> externalRadiationHeatTransfer = new HashMap<String, Double>();
		HashMap<String, Double> surfaceAverageHeatFlux = new HashMap<String, Double>();
		HashMap<String, Double> surfaceAverageConductionHeatFlux = new HashMap<String, Double>();
		HashMap<String, Double> surfaceAverageRadiationHeatFlux = new HashMap<String, Double>();
		HashMap<String, Double> surfaceAverageExternalRadiationHeatFlux = new HashMap<String, Double>();
		
		void extractCfdData() {
			
		    //get field functions
		    VectorMagnitudeFieldFunction area = ((VectorMagnitudeFieldFunction) sim.getFieldFunctionManager().getFunction("Area").getMagnitudeFunction());
		    PrimitiveFieldFunction ambientTemperature = ((PrimitiveFieldFunction) sim.getFieldFunctionManager().getFunction("AmbientTemperature"));
		    PrimitiveFieldFunction temperature = ((PrimitiveFieldFunction) sim.getFieldFunctionManager().getFunction("Temperature"));
		    PrimitiveFieldFunction heatFlux = ((PrimitiveFieldFunction) sim.getFieldFunctionManager().getFunction("BoundaryHeatFlux"));
		    PrimitiveFieldFunction conductionHeatFlux = ((PrimitiveFieldFunction) sim.getFieldFunctionManager().getFunction("BoundaryConductionHeatFlux"));
		    PrimitiveFieldFunction radiationHeatFlux = ((PrimitiveFieldFunction) sim.getFieldFunctionManager().getFunction("BoundaryRadiationHeatFlux"));
		    PrimitiveFieldFunction externalRadiationHeatFlux = ((PrimitiveFieldFunction) sim.getFieldFunctionManager().getFunction("ExternalBoundaryRadiationHeatFlux"));
		    PrimitiveFieldFunction heatTransfer = ((PrimitiveFieldFunction) sim.getFieldFunctionManager().getFunction("BoundaryHeatTransfer"));
		    PrimitiveFieldFunction conductionHeatTransfer = ((PrimitiveFieldFunction) sim.getFieldFunctionManager().getFunction("BoundaryConductionHeatTransfer"));    
		    UserFieldFunction radiationHeatTransfer = ((UserFieldFunction) sim.getFieldFunctionManager().getFunction("RadiationHeatTransfer"));
		    UserFieldFunction externalRadiationHeatTransfer = ((UserFieldFunction) sim.getFieldFunctionManager().getFunction("ExternalRadiationHeatTransfer"));
		    
		    //get thermal specification
		    
		    //get reports
		    AreaAverageReport areaAverageReport = sim.getReportManager().createReport(AreaAverageReport.class);
		    areaAverageReport.setRepresentation(fvRepresentation);
		    SumReport sumReport = sim.getReportManager().createReport(SumReport.class);
		    sumReport.setRepresentation(fvRepresentation);

			//get the data for all sum reports
			HashMap<String, Double> surfaceArea = getReportData(sumReport, area, this.partSurfacesSubgroups);
			HashMap<String, Double> totalHeatTransfer = getReportData(sumReport, heatTransfer, this.partSurfacesSubgroups);
			HashMap<String, Double> totalExternalHeatTransfer = new HashMap<String, Double>();
			
			HashMap<String, Double> surfacesConductionHeatTransfer = getReportData(sumReport, conductionHeatTransfer, this.partSurfacesSubgroups);
			HashMap<String, Double> surfacesExternalConductionHeatTransfer = new HashMap<String, Double>();
			HashMap<String, Double> surfacesRadiationHeatTransfer = getReportData(sumReport, radiationHeatTransfer, this.partSurfacesSubgroups);
			HashMap<String, Double> surfacesExternalRadiationHeatTransfer = getReportData(sumReport, externalRadiationHeatTransfer, this.partSurfacesSubgroups);
			
			//get the data for all surface average reports
			HashMap<String, Double> surfaceAverageTemperature = getReportData(areaAverageReport, temperature, C, this.partSurfacesSubgroups);
			HashMap<String, Double> surfaceAverageAmbientTemperature = getReportData(areaAverageReport, ambientTemperature, C, this.partSurfacesSubgroups);
			HashMap<String, Double> surfaceAverageHeatFlux = getReportData(areaAverageReport, heatFlux, this.partSurfacesSubgroups);
			HashMap<String, Double> surfaceAverageConductionHeatFlux = getReportData(areaAverageReport, conductionHeatFlux, this.partSurfacesSubgroups);
			HashMap<String, Double> surfaceAverageRadiationHeatFlux = getReportData(areaAverageReport, radiationHeatFlux, this.partSurfacesSubgroups);
			HashMap<String, Double> surfaceAverageExternalRadiationHeatFlux = getReportData(areaAverageReport, externalRadiationHeatFlux, this.partSurfacesSubgroups);

			//calculate external heat transfer quantities
			for(Map.Entry<String, Double> entry : totalHeatTransfer.entrySet()) {
				totalExternalHeatTransfer.put(entry.getKey(), -entry.getValue());
			}
			
			double externalConductionHeatTransfer;
			for(String key : totalExternalHeatTransfer.keySet()) {
				externalConductionHeatTransfer = totalExternalHeatTransfer.get(key) - surfacesExternalRadiationHeatTransfer.get(key);
				surfacesExternalConductionHeatTransfer.put(key, externalConductionHeatTransfer);
			}
			
			this.surfaceArea = surfaceArea;
			this.totalHeatTransfer = totalHeatTransfer;
			this.totalExternalHeatTransfer = totalExternalHeatTransfer;
			this.conductionHeatTransfer = surfacesConductionHeatTransfer;
			this.externalConductionHeatTransfer = surfacesExternalConductionHeatTransfer;

			this.radiationHeatTransfer = surfacesRadiationHeatTransfer;
			this.externalRadiationHeatTransfer = surfacesExternalRadiationHeatTransfer;

			this.ambientTemperature = surfaceAverageAmbientTemperature;
			this.surfaceAverageTemperature = surfaceAverageTemperature;
			this.surfaceAverageHeatFlux = surfaceAverageHeatFlux;
			this.surfaceAverageConductionHeatFlux = surfaceAverageConductionHeatFlux;
			this.surfaceAverageRadiationHeatFlux = surfaceAverageRadiationHeatFlux;
			this.surfaceAverageExternalRadiationHeatFlux = surfaceAverageExternalRadiationHeatFlux;

			
			sim.getReportManager().removeObjects(areaAverageReport, sumReport);

		}

		void getWallThermalSpec(Boundary boundary) {
			List<PartSurfacesSubgroup> partSurfacesSubgroups = this.partSurfacesSubgroups;
			HashMap<String, Double> heatTransferCoefficient = new HashMap<String, Double>();

			//get thermal specification
			if(this.thermalSpecification == WallThermalOption.Type.CONVECTION || this.thermalSpecification == WallThermalOption.Type.ENVIRONMENT) {
				ProxyProfile proxyProfile_0;
				Double htc;
				
				HeatTransferCoefficientProfile heatTransferCoefficientProfile_0 = 
						boundary.getValues().get(HeatTransferCoefficientProfile.class);
				
				if(boundary.getAllowPerPartValues() == true) {
					for(PartSurfacesSubgroup subgroup: partSurfacesSubgroups) {

					    proxyProfile_0 = ((ProxyProfile) heatTransferCoefficientProfile_0.getMethod(ByPartProfileMethod.class).getProfileManager().getObject(subgroup.subgroupName));
					    htc = proxyProfile_0.getMethod(ConstantScalarProfileMethod.class).getQuantity().getValue();
					    heatTransferCoefficient.put(subgroup.subgroupName, htc);
					}
				}else {
				    htc = heatTransferCoefficientProfile_0.getMethod(ConstantScalarProfileMethod.class).getQuantity().getValue();
				    heatTransferCoefficient.put("no subgroups", htc);
				}
				
			}
			
			this.heatTransferCoefficient = heatTransferCoefficient;
		}
		
	    void printCfdData() {
				for(String key : this.surfaceArea.keySet()) {
					sim.println(this.name + ", " + key
										  + ", " + this.surfaceArea.get(key)
										  + ", " + this.surfaceAverageTemperature.get(key)
										  + ", " + this.heatTransferCoefficient.get(key)
										  + ", " + this.ambientTemperature.get(key)
										  + ", " + this.totalHeatTransfer.get(key)
										  + ", " + this.conductionHeatTransfer.get(key)
										  + ", " + this.surfaceAverageHeatFlux.get(key)
										  + ", " + this.surfaceAverageConductionHeatFlux.get(key)
							   );
				}
	    }
	    
	    List<List> createDataStructure() {
	    	List<List> dataStructure = new ArrayList<List>();
			
	    	Double ambientTemperature;
	    	for(String key : this.surfaceArea.keySet()) {
		    	List dataRow = new ArrayList<>();
				dataRow.add(name);
				dataRow.add(key);
				dataRow.add(this.surfaceArea.get(key));
				//filter out empty values for the ambient temperature;
				ambientTemperature = this.ambientTemperature.get(key).equals(-2.731500e+02) ? null : this.ambientTemperature.get(key);
				dataRow.add(ambientTemperature);
				dataRow.add(this.heatTransferCoefficient.get(key));
				dataRow.add(this.surfaceAverageTemperature.get(key));
				dataRow.add(this.totalHeatTransfer.get(key));
				dataRow.add(this.conductionHeatTransfer.get(key));
				dataRow.add(this.radiationHeatTransfer.get(key));
				dataRow.add(this.totalExternalHeatTransfer.get(key));
				dataRow.add(this.externalConductionHeatTransfer.get(key));
				dataRow.add(this.externalRadiationHeatTransfer.get(key));
				dataRow.add(this.surfaceAverageHeatFlux.get(key));
				dataRow.add(this.surfaceAverageConductionHeatFlux.get(key));
				dataRow.add(this.surfaceAverageRadiationHeatFlux.get(key));
				dataRow.add(this.surfaceAverageExternalRadiationHeatFlux.get(key));

				dataStructure.add(dataRow);

			}
			return dataStructure;
	    }
	}
	
	class Inlet extends BoundaryData{
		HashMap<String, Double> surfaceArea = new HashMap<String, Double>();
		String inletType; //mass flow, velocity or stagnation inlet
		HashMap<String, Double> mfaTemperature = new HashMap<String, Double>();
		HashMap<String, Double> mfaDensity = new HashMap<String, Double>();
		HashMap<String, Double> mfaPressure = new HashMap<String, Double>();
		HashMap<String, Double> mfaTotalPressure = new HashMap<String, Double>();
		HashMap<String, Double> volumeFlow = new HashMap<String, Double>();
		HashMap<String, Double> massFlow = new HashMap<String, Double>();

		
		void extractCfdData() {
		    //get field functions
		    VectorMagnitudeFieldFunction area = ((VectorMagnitudeFieldFunction) sim.getFieldFunctionManager().getFunction("Area").getMagnitudeFunction());
		    PrimitiveFieldFunction temperature = ((PrimitiveFieldFunction) sim.getFieldFunctionManager().getFunction("Temperature"));
		    PrimitiveFieldFunction density = ((PrimitiveFieldFunction) sim.getFieldFunctionManager().getFunction("Density"));
		    UserFieldFunction volumetricFlow = ((UserFieldFunction) sim.getFieldFunctionManager().getFunction("VolumetricFlow"));
		    
		    PrimitiveFieldFunction pressure = ((PrimitiveFieldFunction) sim.getFieldFunctionManager().getFunction("Pressure"));
		    PrimitiveFieldFunction totalPressure = ((PrimitiveFieldFunction) sim.getFieldFunctionManager().getFunction("TotalPressure"));
		    
		    //get reports
		    MassFlowAverageReport massFlowAverageReport = sim.getReportManager().createReport(MassFlowAverageReport.class);
		    massFlowAverageReport.setRepresentation(fvRepresentation);
		    SumReport sumReport = sim.getReportManager().createReport(SumReport.class);
		    sumReport.setRepresentation(fvRepresentation);
		    MassFlowReport massFlowReport = sim.getReportManager().createReport(MassFlowReport.class);
		    massFlowReport.setRepresentation(fvRepresentation);
		    
			//get the data for all sum reports
		    HashMap<String, Double> surfaceArea = getReportData(sumReport, area, this.partSurfacesSubgroups);
		    HashMap<String, Double> volumeFlow = getReportData(sumReport, volumetricFlow, l_s, this.partSurfacesSubgroups);
			
		    //get the data for all mass flow average reports
		    HashMap<String, Double> mfaTemperature = getReportData(massFlowAverageReport, temperature, C, this.partSurfacesSubgroups);
		    HashMap<String, Double> mfaDensity = getReportData(massFlowAverageReport, density, this.partSurfacesSubgroups);
		    HashMap<String, Double> mfaPressure= getReportData(massFlowAverageReport, pressure, hpa, this.partSurfacesSubgroups);
		    HashMap<String, Double> mfaTotalPressure= getReportData(massFlowAverageReport, totalPressure, hpa, this.partSurfacesSubgroups);

		    HashMap<String, Double> massFlow= getReportData(massFlowReport, this.partSurfacesSubgroups);

		    
			this.surfaceArea = surfaceArea;
			this.mfaTemperature = mfaTemperature;
			this.mfaDensity = mfaDensity;
			this.mfaPressure = mfaPressure;
			this.mfaTotalPressure = mfaTotalPressure;
			this.volumeFlow = volumeFlow;
			this.massFlow = massFlow;

			
			sim.getReportManager().removeObjects(massFlowReport, massFlowAverageReport, sumReport);
		
		}
	
	    void printCfdData() {
			for(String key : this.surfaceArea.keySet()) {
				sim.println(this.name + ", " + key
									  + ", " + this.surfaceArea.get(key)
									  + ", " + this.mfaTemperature.get(key)
									  + ", " + this.massFlow.get(key)	  
									  + ", " + this.volumeFlow.get(key)
									  + ", " + this.mfaDensity.get(key)
									  + ", " + this.mfaPressure.get(key)
									  + ", " + this.mfaTotalPressure.get(key)
						   );
			}
	    }
	
	    List<List> createDataStructure() {
	    	
	    	List<List> dataStructure = new ArrayList<List>();
			
			for(String key : this.surfaceArea.keySet()) {
		    	List dataRow = new ArrayList<>();
				dataRow.add(name);
				dataRow.add(key);
				dataRow.add(this.surfaceArea.get(key));
				dataRow.add(this.mfaTemperature.get(key));
				dataRow.add(this.massFlow.get(key));
				dataRow.add(this.volumeFlow.get(key));
				dataRow.add(this.mfaDensity.get(key));
				dataRow.add(this.mfaPressure.get(key));
				dataRow.add(this.mfaTotalPressure.get(key));
				dataStructure.add(dataRow);
			}
			return dataStructure;
	    }
	}
	
	class Outlet extends Inlet{	
	}
	
	class SimData{ //general information about the simulation setup;
		String geoId;
		String meshId;
		String bcId;
		String caseId;
		String simName;
		String date;
		MesherSettings mesherSettings;
		
		
		SimData(){
			String baseName = sim.getPresentationName();
			if(baseName.contains("@")) {
				baseName = baseName.split("@")[0];
			}
			this.simName = baseName;

			String[] identifiers = this.simName.split("_");
			String identifier;
			for(int i = 0; i < identifiers.length; i++) {
				identifier = identifiers[i];
				if(identifier.startsWith("g")){
					this.geoId = identifier;
				}else if(identifier.startsWith("m")){
					this.meshId = identifier;
				}else if(identifier.startsWith("b")){
					this.bcId = identifier;
				}else if(identifier.startsWith("c")){
					this.caseId = identifier;
				}
			}
		}
		
		void printCfdData(){
			sim.println("--- Simulation File Summary");
			sim.println("sim file's presentation name: " + this.simName);
			sim.println("geometry ID: " + this.geoId);
			sim.println("mesh ID: " + this.meshId);
			sim.println("boundary condition ID: " + this.bcId);
			sim.println("case ID: " + this.caseId);
			sim.print("\n");
		}
		
		
	}
	
	class MesherSettings{
		String mesher;
		double baseSize;
		double targetSurfaceSize;
		double minimumSize;
		int numPrismLayers;
		double PrismLayersThickness;
		
	}	
	
	
	class RegionData{//general information about the regions in the model
		double cellCount;
		double currentIteration;
		double physicalTime;
		double volumeAverageTemperature;
		double referencePressure;
		String simulationName;

		void extractCfdData(){
			sim.println(" ---extracting Region (global) quantities---");
			//report-based data
		    PrimitiveFieldFunction temperature = ((PrimitiveFieldFunction) sim.getFieldFunctionManager().getFunction("Temperature"));
	
		    ElementCountReport elementCountReport = sim.getReportManager().createReport(ElementCountReport.class);
		    elementCountReport.setRepresentation(fvRepresentation);
		    VolumeAverageReport volumeAverageReport = sim.getReportManager().createReport(VolumeAverageReport.class);
		    volumeAverageReport.setRepresentation(fvRepresentation);
		    
		    elementCountReport.getParts().setQuery(null);
		    Collection<Region> regions =  sim.getRegionManager().getRegions();
		    elementCountReport.getParts().setObjects(regions);
		    cellCount = elementCountReport.getReportMonitorValue();
		    
		    volumeAverageReport.getParts().setObjects(regions);
		    volumeAverageReport.setFieldFunction(temperature);
		    volumeAverageReport.setUnits(C);
		    volumeAverageTemperature = volumeAverageReport.getReportMonitorValue();
		    
		    sim.getReportManager().removeObjects(elementCountReport, volumeAverageReport);
	
		    //general data
		    currentIteration = sim.getSimulationIterator().getCurrentIteration();
		    physicalTime = sim.getSolution().getPhysicalTime();
		    PhysicsContinuum fluid = null;
		    for( Continuum continuum :  sim.getContinuumManager().getObjects()){
		    	if(continuum.getClass() == PhysicsContinuum.class) {
				    sim.println("\t the continuum " + continuum.getPresentationName() + " is assumed to be the main fluid of interest");
				    fluid = (PhysicsContinuum) continuum;
				    break;
		    	}
		    }
		    referencePressure = fluid.getReferenceValues().get(ReferencePressure.class).getValue();
		    
		}
		void printCfdData() {
			
		}
		
		List<List> createDataStructure() {
	    	List<List> dataStructure = new ArrayList<List>();
	    	List<String> header = Arrays.asList("sim file","cell count","current iteration","physical time (s)", "volume average temperature (" + C.toString() +")", "reference pressure (Pa)");
	    	dataStructure.add(header);
	    	
	    	List dataRow = new ArrayList<>();
	    	dataRow.add(sim.getPresentationName());
	    	dataRow.add(this.cellCount);
	    	dataRow.add(this.currentIteration);
	    	dataRow.add(this.physicalTime);
	    	dataRow.add(this.volumeAverageTemperature);
	    	dataRow.add(this.referencePressure);
	    	dataRow.add(this.simulationName);
	    	dataStructure.add(dataRow);
	
	    	return dataStructure;
		}
	}

	//--- end of cabin flow data extraction	
	class ProbeData{
	
		HashMap<String,HashMap<String, Double>> probeTemperature;
		HashMap<String,HashMap<String, Double>> probeVelocity;
		PrimitiveFieldFunction temperature;
		VectorMagnitudeFieldFunction velocity;
		
		void extractCfdData(){

			if(positiveAnswer.equals(extractFieldMeanValues)) {
				temperature = ((PrimitiveFieldFunction) sim.getFieldFunctionManager().getFunction("fmTemperatureMonitor"));
			}else {
	    	    temperature = ((PrimitiveFieldFunction) sim.getFieldFunctionManager().getFunction("Temperature"));
			}
		    
			velocity = ((VectorMagnitudeFieldFunction) sim.getFieldFunctionManager().getFunction("Velocity").getMagnitudeFunction());
		    
		    HashMap<String,HashMap<String, Double>> probeTemperature = getProbeData(temperature, C);
		    HashMap<String,HashMap<String, Double>> probeVelocity = getProbeData(velocity, m_s);
		    this.probeTemperature = probeTemperature;
		    this.probeVelocity = probeVelocity;
		}
		
		
		HashMap<String,HashMap<String, Double>> getProbeData(FieldFunction fieldFunction, Units unit){
			HashMap<String,HashMap<String, Double>> probeData = new HashMap<String,HashMap<String, Double>>();
	    	HashMap<String, Double> footProbeData = new HashMap<String, Double>();
	    	HashMap<String, Double> kneeProbeData = new HashMap<String, Double>();
	    	HashMap<String, Double> headProbeData = new HashMap<String, Double>();
	    	HashMap<String, Double> overHeadProbeData = new HashMap<String, Double>();
	    	
			List<Part> probes = new ArrayList<Part>();	
			sim.getPartManager().getObjects().stream()
											.filter(part -> part.getPresentationName().contains("pax_"))
											.forEach(part -> probes.add(part));
	    	String probePosition;
	    	String row;
	    	String seatLabel;
	    	String paxId;
	    	double probeValue;
	    	
	    	//create max report
	        MaxReport maxReport = sim.getReportManager().createReport(MaxReport.class);
	        maxReport.setRepresentation(fvRepresentation);
	    	maxReport.setFieldFunction(fieldFunction);
	    	maxReport.setUnits(unit);	
		    
		    sim.getReportManager().remove(maxReport);
		    
			probeData.put("foot", footProbeData);
			probeData.put("knee", kneeProbeData);
			probeData.put("head", headProbeData);
			probeData.put("overHead", overHeadProbeData);
	
			return probeData;
		}
	    
		void printCfdData() {
		    sim.println("position, pax-id, temperature (" + C + "), velocity, (" + m_s + ")");
	
		    for(String probeLevel : this.probeTemperature.keySet()) {
			    HashMap<String, Double> localProbeTemperature;
			    HashMap<String, Double> localProbeVelocity;
		    	localProbeTemperature = probeTemperature.get(probeLevel);
		    	localProbeVelocity = probeVelocity.get(probeLevel);
			    localProbeTemperature.keySet().stream().forEach( key -> sim.println(probeLevel + ", "
			    																	+ key + ", " 
			    																	+ localProbeTemperature.get(key) +", "
			    																	+ localProbeVelocity.get(key)
			    																	));
		    }
			
		}
		
	    List<List> createDataStructure() {
	    	List<List> dataStructure = new ArrayList<List>();
			String row;
			String seat;
			Double temperature;
	    	List<String> header = Arrays.asList("position", "row", "seat", this.temperature.getPresentationName() +" (C)", this.velocity.getPresentationName() + " (m/s)");
	    	dataStructure.add(header);
		    for(String probeLevel : this.probeTemperature.keySet()) {
			    HashMap<String, Double> localProbeTemperature = new HashMap<String, Double>();
			    HashMap<String, Double> localProbeVelocity = new HashMap<String, Double>();
		    	localProbeTemperature = probeTemperature.get(probeLevel);
		    	localProbeVelocity = probeVelocity.get(probeLevel);
	
		    	for(String key : localProbeTemperature.keySet()) {
			    	List dataRow = new ArrayList<>();
			    	row = key.split("-")[0];
			    	seat = key.split("-")[1];
			    	dataRow.add(probeLevel);
			    	dataRow.add(row);
			    	dataRow.add(seat);
			    	// filter out meaningless temperature values
			    	temperature = localProbeTemperature.get(key) > 0 ? localProbeTemperature.get(key) : null;
			    	dataRow.add(temperature); 
			    	dataRow.add(localProbeVelocity.get(key));
			    	dataStructure.add(dataRow);
			    	
		    	}
		    }
			    																
			return dataStructure;
	    }
		
	}

    @Override
    public void execute() {
        sim = getActiveSimulation();
		//get volume mesh representation
	    fvRepresentation = ((FvRepresentation) sim.getRepresentationManager().getObject("Volume Mesh"));
    	
	    //create custom units and field functions
        createCustomFeatures();

    	//get units
	    C = ((Units) sim.getUnitsManager().getObject("C"));
	    m_s = ((Units) sim.getUnitsManager().getObject("m/s"));
	    hpa = ((Units) sim.getUnitsManager().getObject("hPa"));
	    l_s = ((Units) sim.getUnitsManager().getObject("l/s"));
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
    	Boolean batchExecution = GraphicsEnvironment.isHeadless(); 
    	
    	if(batchExecution) {
    		sim.println("Execution mode is BATCH");
    		sim.println("Physics quantities will be extracted from last iteration");
    		extractFieldMeanValues = "no";
    	}else {
        	extractFieldMeanValues = selectFieldFunctions();
    	}
    	 	   	
    	//--- extract general information from Simulation file 
    	SimData simData = new SimData();
    	simData.printCfdData();
    	
    	RegionData regionData = new RegionData();
    	regionData.extractCfdData();
    	List<List> regionDataStructure = regionData.createDataStructure();
    	//--- extract cfd data
    	
    	//get data from boundaries
    	sim.println(" ---extracting data from boundaries---");
    	List<List> boundaryDataStructure = processBoundaries();

    	if(batchExecution) {
    		List<List> dataStructure = new ArrayList<List>();

    		//merge data structures
    		dataStructure.addAll(regionDataStructure);
    		List<String> emptyLine = new ArrayList<>();
    		emptyLine.add("\n");
    		dataStructure.add(emptyLine);

    		dataStructure.addAll(boundaryDataStructure);
            Boolean structureWithHeader = false;
            String origBaseName = sim.getPresentationName();
            String outputFolder = sim.getSessionDir();
            sim.print(outputFolder);
            String outputFileName = "CFD_extract_" + origBaseName;
            CfdDataWriter.writeDataStructure2csv(outputFolder, outputFileName, dataStructure, structureWithHeader);      
    	}else {
          	//--- write data to excel sheet
        	//--- get user inputs
        	getWorkBook();
        	createBAK();
        	XSSFSheet boundaryDataSheet = createNewSheet();
        	Boolean writeData2Column = false;
            writeCfdData2Sheet (boundaryDataSheet, regionDataStructure, writeData2Column);
            writeCfdData2Sheet (boundaryDataSheet, boundaryDataStructure, 4, 0, writeData2Column);             
            updateFormulas();
            writeWorkbook();
    	}
    	
    }
    
	void createCustomFeatures() {
        //<editor-fold defaultstate="collapsed" desc="Creation of air distribution units: hPa, l/s">
        if (!sim.getUnitsManager().has("hPa")) {
            UserUnits hPa = sim.getUnitsManager().createUnits("hPa", false);
            hPa.setConversion(100.0);
            hPa.setDimensionsVector(new IntVector(new int[]{
                0, 0, 0, // mass, length, time
                0, 0, 0, // temperature, current, luminosity
                0, 0, 0, // quantity, angle, temperature difference
                0, 0, 0, // solid angle, digital information, force
                0, 0, 1, // energy, power, pressure
                0, 0, 0, // velocity, angular velocity, volume
                0, 0, 0, // moment, stress, electric charge
                0, 0, 0, 0 // electric potential, electric conductance, electric capacitance, electric resistance
            }));
        }

        if (!sim.getUnitsManager().has("l/s")) {
            UserUnits lps = sim.getUnitsManager().createUnits("l/s", false);
            lps.setConversion(0.001);
            lps.setDimensionsVector(new IntVector(new int[]{
                0, 0, -1, // mass, length, time
                0, 0, 0, // temperature, current, luminosity
                0, 0, 0, // quantity, angle, temperature difference
                0, 0, 0, // solid angle, digital information, force
                0, 0, 0, // energy, power, pressure
                0, 0, 1, // velocity, angular velocity, volume
                0, 0, 0, // moment, stress, electric charge
                0, 0, 0, 0 // electric potential, electric conductance, electric capacitance, electric resistance
            }));
        }
        //</editor-fold>
        
        //<editor-fold defaultstate="collapsed" desc="Creation of additional field functions: 'VolumetricFlow', 'MeanVelocity'">
        if (!sim.getFieldFunctionManager().has("VolumetricFlow")) {
            UserFieldFunction volFlow = sim.getFieldFunctionManager().createFieldFunction();
            volFlow.getTypeOption().setSelected(FieldFunctionTypeOption.Type.SCALAR);
            volFlow.setPresentationName("VolumetricFlow");
            volFlow.setFunctionName("VolumetricFlow");
            volFlow.setDimensionsVector(new IntVector(new int[]{
                0, 0, -1, // mass, length, time
                0, 0, 0, // temperature, current, luminosity
                0, 0, 0, // quantity, angle, temperature difference
                0, 0, 0, // solid angle, digital information, force
                0, 0, 0, // energy, power, pressure
                0, 0, 1, // velocity, angular velocity, volume
                0, 0, 0, // moment, stress, electric charge
                0, 0, 0, 0 // electric potential, electric conductance, electric capacitance, electric resistance
            }));
            volFlow.setDefinition("dot($${Velocity},$${Area})");
        }
        if (!sim.getFieldFunctionManager().has("RadiationHeatTransfer")) {
            UserFieldFunction volFlow = sim.getFieldFunctionManager().createFieldFunction();
            volFlow.getTypeOption().setSelected(FieldFunctionTypeOption.Type.SCALAR);
            volFlow.setPresentationName("RadiationHeatTransfer");
            volFlow.setFunctionName("RadiationHeatTransfer");
            volFlow.setDimensionsVector(new IntVector(new int[]{
                0, 0, 0, // mass, length, time
                0, 0, 0, // temperature, current, luminosity
                0, 0, 0, // quantity, angle, temperature difference
                0, 0, 0, // solid angle, digital information, force
                0, 1, 0, // energy, power, pressure
                0, 0, 0, // velocity, angular velocity, volume
                0, 0, 0, // moment, stress, electric charge
                0, 0, 0, 0 // electric potential, electric conductance, electric capacitance, electric resistance
            }));
            volFlow.setDefinition("${BoundaryRadiationHeatFlux}*mag($${Area})");
        }
        
        
        
        if (!sim.getFieldFunctionManager().has("ExternalRadiationHeatTransfer")) {
            UserFieldFunction volFlow = sim.getFieldFunctionManager().createFieldFunction();
            volFlow.getTypeOption().setSelected(FieldFunctionTypeOption.Type.SCALAR);
            volFlow.setPresentationName("ExternalRadiationHeatTransfer");
            volFlow.setFunctionName("ExternalRadiationHeatTransfer");
            volFlow.setDimensionsVector(new IntVector(new int[]{
                0, 0, 0, // mass, length, time
                0, 0, 0, // temperature, current, luminosity
                0, 0, 0, // quantity, angle, temperature difference
                0, 0, 0, // solid angle, digital information, force
                0, 1, 0, // energy, power, pressure
                0, 0, 0, // velocity, angular velocity, volume
                0, 0, 0, // moment, stress, electric charge
                0, 0, 0, 0 // electric potential, electric conductance, electric capacitance, electric resistance
            }));
            volFlow.setDefinition("${ExternalBoundaryRadiationHeatFlux}*mag($${Area})");
        }
        
        
	}
    
	List<List> processBoundaries() {
    	List<List> dataStructure = new ArrayList<List>();
		List<Inlet> inlets = new ArrayList<Inlet>();
		List<Outlet> outlets = new ArrayList<Outlet>();
		List<Wall> walls = new ArrayList<Wall>();
		BoundaryData boundaryData;
		BoundaryType boundaryType;
    	
  		//get all boundaries
  		Collection<Boundary> boundaries = new ArrayList<Boundary>();
  		sim.getRegionManager().getRegions().stream()
  								.forEach(b -> boundaries.addAll(b.getBoundaryManager().getBoundaries()));
  		//handle boundary types 				
  		InletBoundary inletBoundary = ((InletBoundary) sim.get(ConditionTypeManager.class).get(InletBoundary.class));
  		MassFlowBoundary massFlowBoundary = ((MassFlowBoundary) sim.get(ConditionTypeManager.class).get(MassFlowBoundary.class));
  		PressureBoundary pressureOutletBoundary = ((PressureBoundary) sim.get(ConditionTypeManager.class).get(PressureBoundary.class));
  		WallBoundary wallBoundary = ((WallBoundary) sim.get(ConditionTypeManager.class).get(WallBoundary.class));

  		
  		return dataStructure;
    }
    
    BoundaryData getBoundaryData(Boundary boundary){
    	BoundaryData boundaryData = new BoundaryData();
    	PartGrouping partGrouping;
    	List<NamedPartGroup> subgroups;
		
		return boundaryData;
    }
    

    String selectFieldFunctions() {
        int option = 0;
        String extractFieldMeanValues;
        Object[] options = {"Values from last iteration", "Values from field mean monitors"};
        option = JOptionPane.showOptionDialog(
                null,
                "Which field functions have to be used?",
                "User input required",
                JOptionPane.CLOSED_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, //do not use a custom Icon
                options, //the titles of buttons
                options[0]); //default button title
        switch(option) {
	        case 0:
	        	extractFieldMeanValues = "no";
	            break;
	        case 1:
	        	extractFieldMeanValues = "yes";
	            break;
	        default:
	            throw new IllegalArgumentException("User aborted input!");
        }

        return extractFieldMeanValues;
    }
    
    
    
  
    private File openExcelFile() {
        File xls = null;
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(sim.getSessionDir()));
        javax.swing.filechooser.FileFilter txtFilter = new FileNameExtensionFilter("Excel Workbook", "xlsx", "xlsm", "xlsb");
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.addChoosableFileFilter(txtFilter);
        chooser.setDialogTitle("Please select file");
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
    
    static class CfdDataWriter {
        public static void writeDataStructure2csv(String outputFolder, String outputFileName, List<List> dataStructure, Boolean structureWithHeader) {
            // File input path
    		String outputFile = outputFolder + File.separator + outputFileName + ".csv";
    		try {
    			FileWriter csvWriter = new FileWriter(outputFile);
    	    	int firstRow = 0;
    	    	String csvLine;
    	    	if(structureWithHeader) {
    	        	List<String> header = dataStructure.get(0);
    	        	csvLine = createCsvLineFromList(header);
    			    csvWriter.append(csvLine);
    			    csvWriter.append("\n");
    	        	firstRow++;
    	    	}
    			
    	    	int numOfRows = dataStructure.size();
    	    	for(int i = firstRow; i < numOfRows; i++) {
    	    		List<String> rowData = dataStructure.get(i);
    	        	csvLine = createCsvLineFromList(rowData);
    			    csvWriter.append(csvLine);
    			    csvWriter.append("\n");
    	    	}
    			csvWriter.flush();
    			csvWriter.close();
    		}catch(IOException e){
    			
    		}
        }
        
       public static String createCsvLineFromList(List rowData) {
        	String csvLine = "";
        	if(rowData.size() == 1) {
        		return rowData.get(0) == null ? "" : (String) rowData.get(0);
        	}
        	
        	for(int i = 0; i < rowData.size() -1 ; i++) {
        		if(rowData.get(i) == null ) {
            		csvLine = csvLine + "" + ",";
        		}else {
            		csvLine = csvLine + rowData.get(i) + ",";
        		}
        	}
        	
    		if(rowData.get(rowData.size() - 1) == null ) {
        		csvLine = csvLine + "";
    		}else {
        		csvLine = csvLine + rowData.get(rowData.size() - 1);
    		}
    		return csvLine;
    	}
    }
   
    
    
    
    public static boolean isInteger(String s) {
        return isInteger(s,10);
    }

    public static boolean isInteger(String s, int radix) {
        if(s.isEmpty()) return false;
        for(int i = 0; i < s.length(); i++) {
            if(i == 0 && s.charAt(i) == '-') {
                if(s.length() == 1) return false;
                else continue;
            }
            if(Character.digit(s.charAt(i),radix) < 0) return false;
        }
        return true;
    }
	 
    
}

