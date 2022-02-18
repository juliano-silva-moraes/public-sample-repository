package inspect;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/*
JSM, 20AUG2020, v1:
This macro creates a framework for CAX simulations. 
The user opens an empty sim-file and launches this macro -> following objects are created:
- Physics continuum and its setup
- An empty region with empty boundary conditions
- An empty scene with geometry displayers for the input geometry (to be filled manually by drag-and-drop)
- A automated mesh operation (setup to be finalized)
- Field mean monitors for density, static pressure and velocity (starting after 1000 iterations)
- Common air distribution units (hPa, l/s) and a field function 'VolumetricFlow'
- Creation of following tags: 

JSM, 30/03/2020 - inWork:
*/



import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.swing.JFileChooser;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openide.util.Exceptions;

import star.base.neo.DoubleVector;
import star.base.neo.IntVector;
import star.base.neo.NeoObjectVector;
import star.base.neo.StringVector;
import star.base.query.CompoundOperator;
import star.base.query.CompoundPredicate;
import star.base.query.IncludesOperator;
import star.base.query.IncludesPredicate;
import star.base.query.NameOperator;
import star.base.query.NamePredicate;
import star.base.query.Query;
import star.base.query.QueryFilter;
import star.base.query.QueryFilterManager;
import star.base.query.QueryPredicate;
import star.base.query.TypeOperator;
import star.base.query.TypePredicate;
import star.base.report.FieldMeanMonitor;
import star.common.Boundary;
import star.common.BoundaryInterface;
import star.common.ByPartProfileMethod;
import star.common.ConditionTypeManager;
import star.common.ConstantScalarProfileMethod;
import star.common.FanInterface;
import star.common.FieldFunction;
import star.common.FieldFunctionManager;
import star.common.FieldFunctionTypeOption;
import star.common.GeometryPart;
import star.common.GlobalParameterManager;
import star.common.MassFlowBoundary;
import star.common.NamedPartGroup;
import star.common.PartGrouping;
import star.common.PartGroupingManager;
import star.common.PartSurface;
import star.common.PhysicsContinuum;
import star.common.Polynomial;
import star.common.PorousBaffleInterface;
import star.common.PressureBoundary;
import star.common.ProxyProfile;
import star.common.Region;
import star.common.ScalarGlobalParameter;
import star.common.Simulation;
import star.common.StagnationBoundary;
import star.common.StarMacro;
import star.common.SteadyModel;
import star.common.StepStoppingCriterion;
import star.common.TagManager;
import star.common.Units;
import star.common.UserFieldFunction;
import star.common.UserUnits;
import star.dualmesher.DualAutoMesher;
import star.flow.ConstantDensityModel;
import star.flow.ConstantDensityProperty;
import star.flow.FanCurvePolynomial;
import star.flow.FanCurvePolynomialLeaf;
import star.flow.FanCurvePressureRiseOption;
import star.flow.FanCurveTypeOption;
import star.flow.InterfaceFanCurveSpecification;
import star.flow.MassFlowRateProfile;
import star.flow.PorousBaffleInertialResistanceProfile;
import star.flow.PorousBaffleTreatmentOption;
import star.flow.StaticPressureProfile;
import star.kwturb.KOmegaTurbulence;
import star.kwturb.KwAllYplusWallTreatment;
import star.kwturb.KwTurbConstitutiveOption;
import star.kwturb.KwTurbCurvatureCorrectionOption;
import star.kwturb.SstKwTurbModel;
import star.material.ConstantMaterialPropertyMethod;
import star.material.Gas;
import star.material.SingleComponentGasModel;
import star.meshing.AutoMeshOperation;
import star.meshing.BaseSize;
import star.meshing.ConcurrentMeshingControl;
import star.meshing.ConcurrentMeshingControlOption;
import star.meshing.HideNonCustomizedControlsOption;
import star.meshing.MaximumCellSize;
import star.meshing.MeshOperation;
import star.meshing.MeshOperationManager;
import star.meshing.MesherParallelModeOption;
import star.meshing.PartRepresentation;
import star.meshing.PartsMinimumSurfaceSize;
import star.meshing.PartsMinimumSurfaceSizeOption;
import star.meshing.PartsTargetSurfaceSize;
import star.meshing.PartsTargetSurfaceSizeOption;
import star.meshing.ProjectToCadOption;
import star.meshing.SurfaceCustomMeshControl;
import star.meshing.SurfaceProximity;
import star.meshing.VolumeControlSize;
import star.meshing.VolumeCustomMeshControl;
import star.metrics.ThreeDimensionalModel;
import star.prismmesher.NumPrismLayers;
import star.prismmesher.PartsCustomPrismsOption;
import star.prismmesher.PartsCustomizePrismMesh;
import star.prismmesher.PrismAutoMesher;
import star.prismmesher.PrismStretchingOption;
import star.prismmesher.PrismThickness;
import star.prismmesher.PrismWallThickness;
import star.resurfacer.ResurfacerAutoMesher;
import star.resurfacer.VolumeControlResurfacerSizeOption;
import star.segregatedflow.SegregatedFlowModel;
import star.turbulence.RansTurbulenceModel;
import star.turbulence.TurbulenceIntensityProfile;
import star.turbulence.TurbulentModel;
import star.turbulence.TurbulentViscosityRatioProfile;
import star.vis.PartColorMode;
import star.vis.PartDisplayer;
import star.vis.Scene;
import star.vis.SimpleAnnotation;

@SuppressWarnings("unchecked")
public class JavaClassSample3 extends StarMacro {

    Simulation sim;
    Units units_mm;
    MeshOperation imprintAftFan;
    MeshOperation imprintFwdFan;
    AutoMeshOperation autoMesh;
    AutoMeshOperation autoMeshFanConformal;
    Scene scene;
    File excelFile;

    final double[] COLOUR_RED = new double[]{1.0, 0.0, 0.0};
    final double[] COLOUR_GREEN = new double[]{0.0, 1.0, 0.0};
    final double[] COLOUR_BLUE = new double[]{0.0, 0.0, 1.0};
    final double[] COLOUR_YELLOW = new double[]{1.0, 1.0, 0.0};
    final double[] COLOUR_MAGENTA = new double[]{1.0, 0.0, 1.0};
    final double[] COLOUR_CYAN = new double[]{0.0, 1.0, 1.0};
    final double[] COLOUR_BLACK = new double[]{0.0, 0.0, 0.0};
    final double[] COLOUR_GREY = new double[]{0.6, 0.6, 0.6};

    
    //Cax's system definition constants
    static final double CTS_ZETA = 6.162;
    static final double CTS_AT_FLAT_DUCT_ZETA = 31.58;
    static final double TS_FCRC_ZETA = 10.66;
    
    @Override
    public void execute() {
        // start time for execution time calculation
        double startTime = (double) System.currentTimeMillis();

        sim = getActiveSimulation();
        units_mm = (Units) sim.getUnitsManager().getObject("mm");

        try {
            process();
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
    
    @SuppressWarnings("deprecation")
	private void process() {
    	
    	//------------- read boundary/interface subgroups' names from input excel file
        List<String> subgroupNames = getSubgroupNames();
        List<String> boundarySubgroupNames = subgroupNames.stream().filter(sb -> !sb.contains("TS_")).collect(Collectors.toList());   
    	
    	
        //<editor-fold defaultstate="collapsed" desc="Continuum creation & setup">
        PhysicsContinuum ps = sim.getContinuumManager().createContinuum(PhysicsContinuum.class);
        ps.enable(ThreeDimensionalModel.class);
        ps.enable(SteadyModel.class);
        ps.enable(SingleComponentGasModel.class);
        ps.enable(SegregatedFlowModel.class);
        ps.enable(ConstantDensityModel.class);
        ps.enable(TurbulentModel.class);
        ps.enable(RansTurbulenceModel.class);
        ps.enable(KOmegaTurbulence.class);
        ps.enable(SstKwTurbModel.class);
        ps.enable(KwAllYplusWallTreatment.class);
        
        SstKwTurbModel sstMod = ps.getModelManager().getModel(SstKwTurbModel.class);
        sstMod.getKwTurbCurvatureCorrectionOption().setSelected(KwTurbCurvatureCorrectionOption.Type.DURBIN);
        sstMod.getKwTurbConstitutiveOption().setSelected(KwTurbConstitutiveOption.Type.QCR);
        sstMod.setA1(1.0);
        sstMod.getRealizableTimeParameter().setRealizableTimeCoefficient(1.2);
        SingleComponentGasModel singleComponentGasModel_0 = ps.getModelManager().getModel(SingleComponentGasModel.class);
        Gas air = ((Gas) singleComponentGasModel_0.getMaterial());
        ConstantMaterialPropertyMethod constantDensity = ((ConstantMaterialPropertyMethod) air.getMaterialProperties().getMaterialProperty(ConstantDensityProperty.class).getMethod());
        constantDensity.getQuantity().setValue(1.225);
        //</editor-fold>
                
        //<editor-fold defaultstate="collapsed" desc="Region creation & setup">
        Region region1 = sim.getRegionManager().createEmptyRegion();
        Region region2 = sim.getRegionManager().createEmptyRegion();
        region2.setPresentationName("downstream_aft_fan");
        Region region3 = sim.getRegionManager().createEmptyRegion();
        region3.setPresentationName("downstream_fwd_fan");

        //<editor-fold defaultstate="collapsed" desc="Boundaries creation & setup">

        //in - mass flow inlet;
        MassFlowBoundary massFlowBoundary = ((MassFlowBoundary) sim.get(ConditionTypeManager.class).get(MassFlowBoundary.class));
	    StagnationBoundary stagnationBoundary = 
	    	      ((StagnationBoundary) sim.get(ConditionTypeManager.class).get(StagnationBoundary.class));
        
        Boundary inletMonuments = region1.getBoundaryManager().createEmptyBoundary();
        inletMonuments.setPresentationName("in_monuments");
        inletMonuments.setBoundaryType(massFlowBoundary);
        inletMonuments.setAllowPerPartValues(true);
        inletMonuments.getValues().get(MassFlowRateProfile.class).setMethod(ByPartProfileMethod.class);
        inletMonuments.getValues().get(TurbulenceIntensityProfile.class).getMethod(ConstantScalarProfileMethod.class).getQuantity().setValue(0.1);
        inletMonuments.getValues().get(TurbulentViscosityRatioProfile.class).getMethod(ConstantScalarProfileMethod.class).getQuantity().setValue(100.0);
        createBoundarySubgroups(inletMonuments, boundarySubgroupNames); // EDIT-> most likely the createSubgroups() is most suited for the SetupFrameworkPart2. 
        
        Boundary inletTempSensors = region1.getBoundaryManager().createEmptyBoundary();
        inletTempSensors.setPresentationName("in_TS");
        inletTempSensors.setBoundaryType(stagnationBoundary);
        inletTempSensors.setAllowPerPartValues(true);
        inletTempSensors.getValues().get(TurbulenceIntensityProfile.class).getMethod(ConstantScalarProfileMethod.class).getQuantity().setValue(0.1);
        inletTempSensors.getValues().get(TurbulentViscosityRatioProfile.class).getMethod(ConstantScalarProfileMethod.class).getQuantity().setValue(100.0);

        //wall
        Boundary wall = region1.getBoundaryManager().getBoundary("Default");
        wall.setPresentationName("walls");
        wall.setAllowPerPartValues(true);


        //<editor-fold defaultstate="collapsed" desc="Physics Conditions setup">
        //Boundary type - wall
        Boundary wall2 = region2.getBoundaryManager().getBoundary("Default");
        wall2.setPresentationName("walls");
        wall2.setAllowPerPartValues(true);
        //Boundary type - outlet
        PressureBoundary pressureOutletBoundary = ((PressureBoundary) sim.get(ConditionTypeManager.class).get(PressureBoundary.class));
        //outlet - pressure oulet;
        Boundary outlet = region2.getBoundaryManager().createEmptyBoundary();
        outlet.setPresentationName("out");
        outlet.setBoundaryType(pressureOutletBoundary);
        outlet.setAllowPerPartValues(true);
        outlet.getValues().get(StaticPressureProfile.class).setMethod(ByPartProfileMethod.class);
        outlet.getValues().get(TurbulenceIntensityProfile.class).getMethod(ConstantScalarProfileMethod.class).getQuantity().setValue(0.1);
        outlet.getValues().get(TurbulentViscosityRatioProfile.class).getMethod(ConstantScalarProfileMethod.class).getQuantity().setValue(100.0);
         
        
        // Create interfaces
        sim.getInterfaceManager().createBoundaryInterface(region1, region1, "Region 1/Region 1");
        
        BoundaryInterface porousBaffleInterface = sim.getInterfaceManager().createBoundaryInterface(region1, region1, "porous baffle interface");

        BoundaryInterface aftFanInterface = 
          sim.getInterfaceManager().createBoundaryInterface(region1, region2, "AFT_fan_interface");
        FanInterface fanInterface_0 = 
          ((FanInterface) sim.get(ConditionTypeManager.class).get(FanInterface.class));
        aftFanInterface.setInterfaceType(fanInterface_0);
        
        BoundaryInterface fwdFanInterface = 
              sim.getInterfaceManager().createBoundaryInterface(region1, region3, "FWD_fan_interface");
        fwdFanInterface.setInterfaceType(fanInterface_0);

    	//</editor-fold>
        
        //<editor-fold defaultstate="collapsed" desc="Imprint operation creation & setup">    
        imprintAftFan = sim.get(MeshOperationManager.class).createImprintPartsOperation();
        imprintAftFan.setPresentationName("imprint- aft fan interface");
        imprintFwdFan = sim.get(MeshOperationManager.class).createImprintPartsOperation();
        imprintFwdFan.setPresentationName("imprint- fwd fan interface");
        //<editor-fold defaultstate="collapsed" desc="Automated Mesh operation creation & setup">    
        autoMesh = sim.get(MeshOperationManager.class).createAutoMeshOperation(new StringVector(new String[]{
            "star.resurfacer.ResurfacerAutoMesher",
            "star.resurfacer.AutomaticSurfaceRepairAutoMesher",
            "star.dualmesher.DualAutoMesher",
            "star.prismmesher.PrismAutoMesher"}
        ), new NeoObjectVector(new Object[]{}));
        autoMesh.setMeshPartByPart(true);
        autoMesh.getMesherParallelModeOption().setSelected(MesherParallelModeOption.Type.CONCURRENT);
        MaximumCellSize maximumCellSize = autoMesh.getDefaultValues().get(MaximumCellSize.class);
        maximumCellSize.getRelativeSizeScalar().setValue(10.0);
        
        // Node 'Meshers'
        ResurfacerAutoMesher resurfacer = ((ResurfacerAutoMesher) autoMesh.getMeshers().getObject("Surface Remesher"));
        resurfacer.setMinimumFaceQuality(0.2);

        DualAutoMesher polyMesher = ((DualAutoMesher) autoMesh.getMeshers().getObject("Polyhedral Mesher"));
        polyMesher.setTetOptimizeCycles(4);
        polyMesher.setTetQualityThreshold(0.7);

        PrismAutoMesher prismMesher = ((PrismAutoMesher) autoMesh.getMeshers().getObject("Prism Layer Mesher"));
        prismMesher.getPrismStretchingOption().setSelected(PrismStretchingOption.Type.WALL_THICKNESS); 
        prismMesher.setMinimumThickness(1.0);
        prismMesher.setNearCoreLayerAspectRatio(0.5);


       
        //</editor-fold>
        
  
       
        //Set porous baffle interface
	    PorousBaffleInterface porousBaffleInterface_0 = ((PorousBaffleInterface) sim.get(ConditionTypeManager.class).get(PorousBaffleInterface.class));
	    porousBaffleInterface.setInterfaceType(porousBaffleInterface_0);
	    porousBaffleInterface.setAllowPerPartValues(true);
	    PorousBaffleInertialResistanceProfile porousBaffleInertialResistanceProfile_0 = porousBaffleInterface.getValues().get(PorousBaffleInertialResistanceProfile.class);
	    porousBaffleInertialResistanceProfile_0.setMethod(ByPartProfileMethod.class);
	    List<String> tempSensorSubgroups = Arrays.asList("CTS_aft", "TS_FCRC");
	    List<String> nameOperatorInputs = Arrays.asList("CTS_8", "FCRC");
	    createInterfaceSubgroups(porousBaffleInterface, tempSensorSubgroups, nameOperatorInputs);
	    porousBaffleInterface.get(PartGroupingManager.class).getObject("Subgrouping 1").getObject("Subgroup 1").setName("CTS");
	    porousBaffleInterface.getConditions().get(PorousBaffleTreatmentOption.class).setSelected(PorousBaffleTreatmentOption.Type.RESISTANCE_BASED);

	    //Set porous inertial resistance for each of the temperature sensor groups
	    String[] temperatureSensorGroups = {"CTS", "CTS_aft", "TS_FCRC"};
	    double [] temperatureSensorZetaValues = {CTS_ZETA, CTS_AT_FLAT_DUCT_ZETA, TS_FCRC_ZETA};
	    
	    HashMap<String, Double> tempSensorPorousResistance = new HashMap<String, Double>();
	    String temperatureSensorGroup;
	    Double temperatureSensorZetaValue;
	    for(int i = 0; i < temperatureSensorGroups.length; i++) {
	    	temperatureSensorGroup = temperatureSensorGroups[i];
	    	temperatureSensorZetaValue = temperatureSensorZetaValues[i];
	    	tempSensorPorousResistance.put(temperatureSensorGroup, temperatureSensorZetaValue * 0.5);
	    }
	    
	    String groupId;
	    double porousInertialResistance;	    
	    for(Entry<String, Double> entry : tempSensorPorousResistance.entrySet()) {
	    	groupId = entry.getKey();
	    	porousInertialResistance = entry.getValue();
	    	setPorousBaffelInterfaceResistance(groupId, porousInertialResistance, porousBaffleInterface_0, porousBaffleInertialResistanceProfile_0);
	    }
	    
        //</editor-fold>

        // Other settings
        sim.get(TagManager.class).createNewUserTag("ducting");
        sim.get(TagManager.class).createNewUserTag("restrictors");
        sim.get(TagManager.class).createNewUserTag("subtract_input-ducting");
        sim.get(TagManager.class).createNewUserTag("subtract_input-restrictors");
        StepStoppingCriterion maxSteps = ((StepStoppingCriterion) sim.getSolverStoppingCriterionManager().getSolverStoppingCriterion("Maximum Steps"));
        maxSteps.setMaximumNumberSteps(1500);        
        
        //Create relevant views
        //A350-900
        
        
        //A350-1000
        
    }
    
    
    private void createBoundarySubgroups(Boundary boundary, List<String> subgroupNames) {
        PartGrouping partGrouping = (PartGrouping) boundary.get(PartGroupingManager.class).getObject("Subgrouping 1");
        
        //GET SUBGROUP1 AND RENAME IT
        for( String name : subgroupNames) {
            if(!partGrouping.has(name)) {
            	NamedPartGroup subGroup = partGrouping.createNewGroup();
            	subGroup.setName(name);
                subGroup.setQuery(
                		new Query(
                				new CompoundPredicate(
                                        CompoundOperator.Or, Arrays.asList(
                                                new NamePredicate(NameOperator.Contains, name)
                                        )
                                ),
                				Query.STANDARD_MODIFIERS
                		)
                );
            }
        }
    }
    
    
    private void createInterfaceSubgroups(BoundaryInterface boundaryInterface, List<String> subgroupNames, List<String> nameOperatorInputs) {
        PartGrouping partGrouping = (PartGrouping) boundaryInterface.get(PartGroupingManager.class).getObject("Subgrouping 1");
        
        //GET SUBGROUP1 AND RENAME IT
        String name;
        String nameOperatorInput;
        for( int i = 0; i < subgroupNames.size(); i++) {
        	name = subgroupNames.get(i);
        	nameOperatorInput = nameOperatorInputs.get(i);
        	
            if(!partGrouping.has(name)) {
            	NamedPartGroup subGroup = partGrouping.createNewGroup();
            	subGroup.setName(name);
                subGroup.setQuery(
                		new Query(
                				new CompoundPredicate(
                                        CompoundOperator.Or, Arrays.asList(
                                                new NamePredicate(NameOperator.Contains, nameOperatorInput)
                                        )
                                ),
                				Query.STANDARD_MODIFIERS
                		)
                );
            }
        }
    }
    
    
    @SuppressWarnings("deprecation")
	private void createSurfaceControl(AutoMeshOperation autoMesh, String name, double targetSize_mm, double minSize_mm) {
        SurfaceCustomMeshControl sc = autoMesh.getCustomMeshControls().createSurfaceControl();
        sc.setPresentationName(name);
        sc.getCustomConditions().get(PartsTargetSurfaceSizeOption.class).setSelected(PartsTargetSurfaceSizeOption.Type.CUSTOM);
        sc.getCustomConditions().get(PartsMinimumSurfaceSizeOption.class).setSelected(PartsMinimumSurfaceSizeOption.Type.CUSTOM);      
        sc.getDisplayMode().setSelected(HideNonCustomizedControlsOption.Type.CUSTOMIZED);
        sc.getCustomValues().get(PartsTargetSurfaceSize.class).setAbsoluteSizeValue(targetSize_mm, units_mm);
        sc.getCustomValues().get(PartsMinimumSurfaceSize.class).setAbsoluteSizeValue(minSize_mm, units_mm);
    }

    @SuppressWarnings("deprecation")
	private void createVolumeControl(AutoMeshOperation autoMesh, String name, double customSize_mm) {
        VolumeCustomMeshControl vc = autoMesh.getCustomMeshControls().createVolumeControl();
        vc.setPresentationName(name);
        vc.getCustomConditions().get(VolumeControlResurfacerSizeOption.class).setVolumeControlBaseSizeOption(true);
//        vc.getCustomConditions().get(VolumeControlDualMesherSizeOption.class).setVolumeControlBaseSizeOption(true);
        vc.getDisplayMode().setSelected(HideNonCustomizedControlsOption.Type.CUSTOMIZED);
        vc.getCustomValues().get(VolumeControlSize.class).setAbsoluteSizeValue(customSize_mm, units_mm);
    }

    private void createPartDisplayerType(String name, double opacity) {
        PartRepresentation geo = null;
        if (sim.getRepresentationManager().has("Geometry")) {
            geo = (PartRepresentation) sim.getRepresentationManager().getObject("Geometry");
        }        
        PartDisplayer pd = scene.getDisplayerManager().createPartDisplayer(name, -1, 4, geo);
        pd.setPresentationName(name);
        pd.initialize();
        pd.setOpacity(opacity);
        pd.setOutline(false);
        pd.setSurface(true);
    }

    private void createPartDisplayerConstant(String name, double opacity, double[] colour) {
        createPartDisplayerType(name, opacity);
        PartDisplayer pd = (PartDisplayer) scene.getDisplayerManager().getObject(name);
        pd.setColorMode(PartColorMode.CONSTANT);
        pd.setDisplayerColor(new DoubleVector(colour));
    }

    @SuppressWarnings("unused")
	private void createPartDisplayerDifferent(final String name, final double opacity) {
        createPartDisplayerType(name, opacity);
        PartDisplayer pd = (PartDisplayer) scene.getDisplayerManager().getObject(name);
        pd.setColorMode(PartColorMode.DP);
    }
    
    private void setFanInterface(BoundaryInterface fanInterface) {
    
    }
    
    
    private void setPorousBaffelInterfaceResistance(String groupId, double porousInertialResistance, PorousBaffleInterface porousBaffleInterface_0, PorousBaffleInertialResistanceProfile porousBaffleInertialResistanceProfile_0) {
	    ProxyProfile proxyProfile = ((ProxyProfile) porousBaffleInertialResistanceProfile_0.getMethod(ByPartProfileMethod.class).getProfileManager().getObject(groupId));
	    proxyProfile.getMethod(ConstantScalarProfileMethod.class).getQuantity().setValue(porousInertialResistance);
	    Units noUnit = ((Units) sim.getUnitsManager().getObject(""));
	    proxyProfile.getMethod(ConstantScalarProfileMethod.class).getQuantity().setUnits(noUnit);
	    
    }
    
    
    //READ DATA FROM INPUT WORKBOOK
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
	   excelFile.getName();
   }

   private XSSFSheet  getWorkSheet(String sheetName){
   	FileInputStream inputStream = null;
   	Workbook workbook = null;
   	XSSFSheet sheet = null;
   	  try {
             inputStream = new FileInputStream(excelFile);
         } catch (FileNotFoundException ex) {
             Exceptions.printStackTrace(ex);
         }
         try {
             workbook = new XSSFWorkbook(inputStream);
             sheet = (XSSFSheet) workbook.getSheet(sheetName);
             inputStream.close();
         } catch (IOException ex) {
             Exceptions.printStackTrace(ex);
         }
         return sheet;
   }
     
   private List<String> readInputsFromSheet(XSSFSheet sheet){
    	Boolean init = false;
    	String name;
    	String subgroupName;
    	List<String> subgroupNames = new ArrayList<String>();
    	Cell cell = null;
    	int targetColumn = 0;
    	sim.println("Reading consumers from input sheet:");
	    for (int i = 0; i <= sheet.getLastRowNum(); i++) {
	        try {
	        	// Get consumer's name;
	            name = getCellData(sheet.getRow(i).getCell(0));
	            if (name.isEmpty()) {
	                break; // last part surface reached; does not necessarily stand in the last row...
	            }
	            if(name.replaceAll("\\s+", "").toLowerCase().equals("consumer")){
	            	init = true;
	            	continue;
	            }
	            if(!init){
	            	continue;
	            }
	            if(name.equals("-")){
	            	continue;
	            }
	        	// Get restrictor position;
	            cell = sheet.getRow(i).getCell(1);
	            // address different cell types
	            subgroupName = null;
	            if (cell!=null) {
	            	subgroupName = getCellData(cell);
	            }
	            if (subgroupName.equals("-")) {
	            	continue;
	            }
	            // Get input BCs input values
	            cell = sheet.getRow(i).getCell(targetColumn);
	            subgroupNames.add(subgroupName);
	            sim.println("\tconsumer :" + subgroupName);
	        } catch (NullPointerException ex) {
	            // exception will occur, if a non-existing cell (not filled) is addressed!
	        }
	    }
	    return subgroupNames;
   }

   private List<String> getSubgroupNames() {
   	List<String> subgroupNames = new ArrayList<String>();
    getWorkBook();
    String sheetName = "INPUT";
    XSSFSheet sheet = getWorkSheet(sheetName);
    subgroupNames = readInputsFromSheet(sheet);
   	return subgroupNames;
   }
   
   private String getCellData(Cell cell) {
   	Double aux;
   	String data = null;
   	int cellType;
   	// address different cell types
   	if (cell!= null) {
       	cellType = cell.getCellType();
   		switch (cellType) {
	    		case 0:
	            	//"numeric"
	                aux = cell.getNumericCellValue();
	                data = Integer.toString(aux.intValue());
	                break;
	            case 1:
	            	//"string"
	            	data = (String) cell.getStringCellValue();
	            	break;
	            case 2:
	            	//"formula"
	    			try {
	    				//formula with text value;
	    			}catch(IllegalStateException e) {
	    				//formula with numeric value;
		                aux = cell.getNumericCellValue();
		                data = Integer.toString(aux.intValue());
	    			}
	            	break;
	            default:
	            	break;
   		}
   	}
       return data;
   }  
}
