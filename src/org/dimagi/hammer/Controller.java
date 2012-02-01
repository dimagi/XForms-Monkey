package org.dimagi.hammer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.Vector;

import org.dimagi.hammer.http.ClientHttpRequest;
import org.dimagi.hammer.util.DummyIndexedStorageUtility;
import org.dimagi.hammer.util.RandomString;
import org.javarosa.core.model.Constants;
import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.data.BooleanData;
import org.javarosa.core.model.data.DateData;
import org.javarosa.core.model.data.DateTimeData;
import org.javarosa.core.model.data.DecimalData;
import org.javarosa.core.model.data.GeoPointData;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.IntegerData;
import org.javarosa.core.model.data.SelectMultiData;
import org.javarosa.core.model.data.SelectOneData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.core.model.data.TimeData;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.utils.IPreloadHandler;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.locale.Localizer;
import org.javarosa.core.services.properties.JavaRosaPropertyRules;
import org.javarosa.core.services.storage.IStorageFactory;
import org.javarosa.core.services.storage.IStorageUtility;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.core.services.transport.payload.ByteArrayPayload;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryPrompt;
import org.javarosa.model.xform.XFormSerializingVisitor;


public class Controller {
//	FormEntryPrompt fep = null;
	private FormParseInit fpi = null;
	public static final int SUCCESS=100;
	public static final int FAIL=200;
	private Random rand;
	
	private boolean PRINT_XML_TO_CONSOLE = false;
	private boolean VERBOSE_SERVER_RESPONSE = false;
	private static final String TEMP_INSTANCE_PATH = "temp.xml";
	private static final String TEMP_FOLDER = "temp";
	
	
	public Controller(){
//		fpi = new FormParseInit("testingform.xhtml");
//		fep = new FormEntryPrompt(fpi.getFormDef(), fpi.getFormEntryModel().getFormIndex());
		rand = new Random();
		StorageManager.setStorageFactory(new IStorageFactory() {

			public IStorageUtility newStorage(String name, Class type) {
				return new DummyIndexedStorageUtility();
			}
			
		});
		PropertyManager._().addRules(new JavaRosaPropertyRules());
		PropertyManager._().setProperty(JavaRosaPropertyRules.DEVICE_ID_PROPERTY, "someID");

	}
	
	
	public static void printUsage(){
		System.out.println("USAGE:\njava -jar instance_tool.jar generate [form_path] [number_of_instances]\n" +
							"\t ---> Generates number_of_instances from the form located at form_path (instances are filled with random data)\n\n" +
							"java -jar instance_tool.jar send [submissions_path] [server_post_url]\n" +
							"\t ---> Submits all the xml files in submission_path to server_post_url\n\n" +
							"java -jar instance_tool.jar answers [Xform_path] [csv_output_path]\n" + 
							"\t ---> Generates all the possible answers for each question in form Xform_path and outputs to a csv file located at csv_output_path\n\n");
		System.exit(0);
	}
	
	public static void main(String[] args){
		try{
			if(args.length == 0) printUsage();
			if(args.length == 3 && args[0].toLowerCase().equals("generate")){
				Controller c = new Controller();
				c.setPRINT_XML_TO_CONSOLE(false);
				c.setVERBOSE_SERVER_RESPONSE(true);
				c.generateInstances(args[1], Integer.parseInt(args[2]));
			}else if(args.length == 3 && args[0].toLowerCase().equals("send")){
				Controller cc = new Controller();
				cc.setPRINT_XML_TO_CONSOLE(false);
				cc.setVERBOSE_SERVER_RESPONSE(false);
				cc.sendInstancesFromFolder(args[1], args[2]);
			}else if(args.length == 3 && args[0].toLowerCase().equals("answers")){
				Controller cc = new Controller();
				cc.setPRINT_XML_TO_CONSOLE(false);
				cc.setVERBOSE_SERVER_RESPONSE(false);
				cc.generatePossibleAnswersFromForm(args[1], args[2]);
			}
			else{
				printUsage();
			}
			
			System.exit(0);
		}catch(Exception e){
			e.printStackTrace();
			printUsage();
		}
	}
	
	public void sendInstancesFromFolder(String subsFolder, String server_url){
		
		System.out.println("Attempting to send contents of folder: "+subsFolder+" to URL: "+server_url);
		File dir = new File(subsFolder);
		
		File[] instances = dir.listFiles();
		sendInstances(instances, server_url);
		
	}
	
	public void generateInstances(String formPath,int numInstances){
		System.out.println("Generating "+numInstances+" instances");
		File temp_folder = null;
		temp_folder = new File(TEMP_FOLDER);
		temp_folder.mkdir();
		
		System.out.println("GENERATED INSTANCES WILL BE PLACED IN: "+temp_folder.getAbsolutePath());
		fpi = new FormParseInit(formPath);
		fpi.getFormDef().getPreloader().addPreloadHandler(new IPreloadHandler() {
			
			@Override
			public String preloadHandled() {
				return "meta";
			}
			
			@Override
			public IAnswerData handlePreload(String preloadParams) {	
				if(preloadParams.toLowerCase().equals("UserID")){
					return new StringData("");
				}
				return new StringData("meta_"+preloadParams);
			}
			
			@Override
			public boolean handlePostProcess(TreeElement node, String params) {
				return false;
			}
		});
		File[] instanceArr = new File[numInstances];
		System.out.println("GENERATING INSTANCES...");
		for(int i=0;i<numInstances;i++){
			try{
				instanceArr[i] = File.createTempFile("tempInstance", ".xml", temp_folder);
//				instanceArr[i].deleteOnExit();
				fpi.getFormEntryController().jumpToIndex(FormIndex.createBeginningOfFormIndex());
				
				populateInstance();
				generateXML(instanceArr[i]);
				
			
			}catch(IOException ioe){
				ioe.printStackTrace();
				System.err.println("Could not create temporary xml file. Exiting...");
				System.exit(0);
			}
		}
		System.out.println("\nDONE GENERATING INSTANCES");
		System.out.println("================");
		
		
		//cleanup
//		for(int i=0;i<numInstances;i++){
//			
//			instanceArr[i].delete();
//			temp_folder.delete();
//		}
	}
	
	public void generatePossibleAnswersFromForm(String formPath,String outfile){
		System.out.println("Generating Possible Answers Keyfile for:"+formPath+" and saving to: "+outfile);
		fpi = new FormParseInit(formPath);
		fpi.getFormDef().getPreloader().addPreloadHandler(new IPreloadHandler() {
			
			@Override
			public String preloadHandled() {
				return "meta";
			}
			
			@Override
			public IAnswerData handlePreload(String preloadParams) {	
				if(preloadParams.toLowerCase().equals("UserID")){
					return new StringData("");
				}
				return new StringData("meta_"+preloadParams);
			}
			
			@Override
			public boolean handlePostProcess(TreeElement node, String params) {
				return false;
			}
		});
		System.out.println("Generating answers");
		fpi.getFormEntryController().jumpToIndex(FormIndex.createBeginningOfFormIndex());				
		ArrayList<String[]> ans = generateAnswers();

		try{
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outfile)));
			for(String[] s : ans){
				String outString ="";
				for(int i=0;i<s.length;i++){
					if(i<s.length-1){
						System.out.print(s[i]+", ");
						outString += (s[i]+", ");
					}else{
						System.out.print(s[i]);
						outString += (s[i]);
					}
				}
				System.out.print("\n");
				bw.write(outString+"\n");
				
			}
			bw.close();
			System.out.println("");
			System.out.println("\nDONE GENERATING ANSWERS");
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
	}
	
	private ArrayList<String[]> generateAnswers(){
		FormEntryController fec = fpi.getFormEntryController();
		EvaluationContext ec = new EvaluationContext();
		fpi.getFormDef().setEvaluationContext(ec); 
		fpi.getFormDef().initialize(true);
		Localizer l = fpi.getFormDef().getLocalizer();
		
		l.setDefaultLocale(l.getAvailableLocales()[0]);
		l.setLocale(l.getAvailableLocales()[0]);
		
		int event = fec.getModel().getEvent();
		
		ArrayList<String[]> ret = new ArrayList<String[]>();
		while(event != FormEntryController.EVENT_END_OF_FORM){
			if(event == FormEntryController.EVENT_QUESTION){
				FormEntryPrompt fep = fec.getModel().getQuestionPrompt();
				QuestionDef q = (QuestionDef)fep.getFormElement();
				if (q.getControlType() == Constants.CONTROL_SELECT_ONE || q.getControlType() == Constants.DATATYPE_CHOICE ){
					Vector<SelectChoice> choices = q.getChoices();
					String[] arr = new String[choices.size()+2];
					arr[0] = (q.getTextID().toLowerCase())+"_data";
					for(int i=0;i<choices.size();i++){
						arr[i+1] = choices.get(i).getValue().toLowerCase();
					}
					arr[arr.length-1] = "none";
					ret.add(arr);
				}else if(q.getControlType() == Constants.CONTROL_SELECT_MULTI || q.getControlType() == Constants.DATATYPE_CHOICE_LIST){
					Vector<SelectChoice> choices = q.getChoices();
					String id = q.getTextID().toLowerCase()+"_data_";
					for(int i=0;i<choices.size();i++){
						String[] arr = new String[4];
						arr[0] = id+choices.get(i).getValue().toLowerCase();
						arr[1] = "True".toLowerCase();
						arr[2] = "False".toLowerCase();
						arr[3] = "none";
						ret.add(arr);
					}
				
				}else{
				}
			}
			event = fec.stepToNextEvent();
		}
		
		return ret;
	}
	
	private void populateInstance(){
		FormEntryController fec = fpi.getFormEntryController();
		EvaluationContext ec = new EvaluationContext();
		fpi.getFormDef().setEvaluationContext(ec); 
		fpi.getFormDef().initialize(true);
		Localizer l = fpi.getFormDef().getLocalizer();
		if(l != null){
			l.setDefaultLocale(l.getAvailableLocales()[0]);
			l.setLocale(l.getAvailableLocales()[0]);
		}
		
		
		
		int event = fec.getModel().getEvent();
		while(event != FormEntryController.EVENT_END_OF_FORM){
			dealWithEvent(event);
			event = fec.stepToNextEvent();
		}
	}
	

	
	//Grabbed this code from ODK Collect, SaveToDiskTask.java
	private void generateXML(File file){
		FormEntryController fec = fpi.getFormEntryController();
        FormInstance datamodel = fec.getModel().getForm().getInstance();
        XFormSerializingVisitor serializer = new XFormSerializingVisitor();
        ByteArrayPayload payload = null;
        try{
        	payload = (ByteArrayPayload) serializer.createSerializedPayload(datamodel);
        	
        }catch(IOException ioe){
        	System.out.println(ioe.getMessage());
        	ioe.printStackTrace();
        }
        
        if(payload != null){
        	boolean success = exportXmlFile(payload,file, PRINT_XML_TO_CONSOLE);
        	if(!success){
        		System.err.println("Failed to export instance to XML file on disk");
        	}
        }
        
	}
	
    private boolean exportXmlFile(ByteArrayPayload payload, File file, boolean printToConsole) {

        // create data stream
        InputStream is = payload.getPayloadStream();
        int len = (int) payload.getLength();

        // read from data stream
        byte[] data = new byte[len];
        try {
            int read = is.read(data, 0, len);
            if (read > 0) {
                // write xml file
                try {
                    // String filename = path + "/" +
                    // path.substring(path.lastIndexOf('/') + 1) + ".xml";
                    BufferedWriter bw = new BufferedWriter(new FileWriter(file));
                    bw.write(new String(data, "UTF-8"));
                    bw.flush();
                    bw.close();
                    
                    if(printToConsole){
                    	System.out.println(new String(data, "UTF-8"));
                    }else{
                    	System.out.print("G");
                    }
                    
                    return true;

                } catch (IOException e) {
                    System.err.println("Error writing XML file");
                    e.printStackTrace();
                    return false;
                }
            }
        } catch (IOException e) {
        	System.err.println("Error reading from payload data stream");
            e.printStackTrace();
            return false;
        }

        return false;

    }
    
    private void sendInstances(File[] farray, String postURL){
		int numFiles = farray.length;
		for(int i=0;i<numFiles;i++){
			try{
				ClientHttpRequest pop = new ClientHttpRequest(postURL);
				pop.setParameter("xml_submission_file", farray[i]);
				
					BufferedReader in = new BufferedReader(new InputStreamReader(pop.post()));
					String line = null;
					while((line = in.readLine()) != null) {
						if(VERBOSE_SERVER_RESPONSE) System.out.println(line);
					}
					System.out.print("Sending:"+farray[i].getName()+", size:"+farray[i].length()+" ::: ");
					if(!VERBOSE_SERVER_RESPONSE){
						if(pop.getConnection().getHeaderField(0).toLowerCase().equals("HTTP/1.0 200 OK".toLowerCase())){
							System.out.println(pop.getConnection().getHeaderField(0));
						}else{
							System.out.println(pop.getConnection().getHeaderField(0));
						}
					}
					in.close();
			}catch(IOException ioe){
				ioe.printStackTrace();
			}

		}
		System.out.println("\nDONE SENDING");
    }
	
	/**
	 * Deals with form events (generate and save answer questions)
	 * @param event
	 */
	private void dealWithEvent(int event){
		FormEntryController fec = fpi.getFormEntryController();
		switch(event){
		case FormEntryController.EVENT_BEGINNING_OF_FORM: 
			break;
		case FormEntryController.EVENT_END_OF_FORM:
			//produce some signal to indicate that we're done, but probably this should be caught earlier.
			break;
		case FormEntryController.EVENT_GROUP:
			break;
		case FormEntryController.EVENT_PROMPT_NEW_REPEAT:
			if(rand.nextBoolean())   // 50/50 chance of generating another repeat... might not be best.
				fec.newRepeat();
			break;
		case FormEntryController.EVENT_QUESTION:
			FormEntryPrompt fep = new FormEntryPrompt(fpi.getFormDef(), fpi.getFormEntryModel().getFormIndex());
			int ev = generateAnswer(fec);
			if(ev == FAIL) {
				System.err.println("QUESTION NOT ANSWERED: Question Text = "+ fep.getQText());
			}
			break;
		case FormEntryController.EVENT_REPEAT:
			break;
		}
	}
	
	
	private int generateAnswer(FormEntryController fec){
		// user fep.getControlType() to determine current question type;
		FormEntryPrompt fep = new FormEntryPrompt(fpi.getFormDef(), fpi.getFormEntryModel().getFormIndex());
		IAnswerData ans = null;
		boolean success = false;
		int qtype = fep.getDataType();
		switch(qtype){
		case Constants.DATATYPE_BARCODE:
			ans = new StringData(RandomString.nextRandomString());
			System.err.println("Don't know how to handle barcode type questions!");
			break;
		case Constants.DATATYPE_BINARY:
			ans = null;
			System.err.println("Don't know how to handle binary type questions! Question Text: "+fep.getQText());
			break;
		case Constants.DATATYPE_BOOLEAN:
			ans = new BooleanData(rand.nextBoolean());
			break;
		case Constants.DATATYPE_CHOICE:
			int index = rand.nextInt(fep.getSelectChoices().size());
			ans = new SelectOneData(fep.getSelectChoices().elementAt(index).selection());
			break;
		case Constants.DATATYPE_CHOICE_LIST:
			Vector<SelectChoice> choices = fep.getSelectChoices();
			Vector<Selection> vs = new Vector<Selection>();
			for(int i=0;i<choices.size();i++){
				if(rand.nextBoolean())
					vs.add(choices.elementAt(i).selection());
			}
			ans = new SelectMultiData(vs);
			break;
		case Constants.DATATYPE_DATE:
			ans = (new DateData(new Date( (long)Math.random() * (new Date()).getTime() )));
			break;
		case Constants.DATATYPE_DATE_TIME:
			ans = new DateTimeData(new Date( (long)Math.random() * (new Date()).getTime() ));
			break;
		case Constants.DATATYPE_DECIMAL:
			ans = new DecimalData(rand.nextDouble()*rand.nextInt());
			break;
		case Constants.DATATYPE_GEOPOINT:
			double[] gp = {(double)rand.nextInt(128),(double)rand.nextInt(128),(double)rand.nextInt(128)};
			ans = new GeoPointData(gp);
			break;
		case Constants.DATATYPE_INTEGER:
			ans = new IntegerData(rand.nextInt());
			break;
		case Constants.DATATYPE_NULL:
			ans = null; // ??
			break;
		case Constants.DATATYPE_TEXT:
			ans = new StringData(RandomString.nextRandomString());
			break;
		case Constants.DATATYPE_TIME:
			ans = new TimeData(new Date( (long)Math.random() * (new Date()).getTime() ));
			break;
		case Constants.DATATYPE_UNSUPPORTED:
//			throw new RuntimeException("Question Datatype not supported : " + qtype + "!");
			System.err.println("Don't know how to handle this type of question. Type code: "+ qtype + ". Question text:" + fep.getQText());
			System.err.println("Please ensure your form is free of all *warnings* (according to the JR validator) if you are having problems");
			break;
		default:
			throw new RuntimeException("Question datatype unknown wtf"); 
		}

		qtype = fep.getControlType();
//		System.out.println("Generating answer for question:"+fep.getQText()+". Type:"+qtype);
		switch(qtype){
		case Constants.CONTROL_AUDIO_CAPTURE:
			ans = new StringData("");
			break;
		case Constants.CONTROL_IMAGE_CHOOSE:
			ans = new StringData("");
			break;
		case Constants.CONTROL_SELECT_MULTI:
			Vector<SelectChoice> choices = fep.getSelectChoices();
			Vector<Selection> vs = new Vector<Selection>();
			for(int i=0;i<choices.size();i++){
				if(rand.nextBoolean())
					vs.add(choices.elementAt(i).selection());
			}
			ans = new SelectMultiData(vs);
			break;
		case Constants.CONTROL_SELECT_ONE:
			int index = rand.nextInt(fep.getSelectChoices().size());
			ans = new SelectOneData(fep.getSelectChoices().elementAt(index).selection());
			break;
		case Constants.CONTROL_TRIGGER:
			ans = new StringData("TRIGGER_CONTROL");
		default:
			break;
		}
		success = fec.saveAnswer(ans);
		if(success) {
			return SUCCESS;
		}
		else {
			System.err.println("Question could not be answered. Question was: + " + fep.getIndex() + "Answer was: " + ans);
			return FAIL;
		}
		
		
	}




	public boolean isPRINT_XML_TO_CONSOLE() {
		return PRINT_XML_TO_CONSOLE;
	}




	public void setPRINT_XML_TO_CONSOLE(boolean pRINTXMLTOCONSOLE) {
		PRINT_XML_TO_CONSOLE = pRINTXMLTOCONSOLE;
	}




	public boolean isVERBOSE_SERVER_RESPONSE() {
		return VERBOSE_SERVER_RESPONSE;
	}




	public void setVERBOSE_SERVER_RESPONSE(boolean vERBOSESERVERRESPONSE) {
		VERBOSE_SERVER_RESPONSE = vERBOSESERVERRESPONSE;
	}
		
		

}
