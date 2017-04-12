/**
 * 
 */
package edu.asu.PLPWebserver;


import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import edu.asu.plp.tool.backend.EventRegistry;
import edu.asu.plp.tool.backend.isa.*;
import edu.asu.plp.tool.backend.isa.events.SimulatorControlEvent;
import edu.asu.plp.tool.backend.isa.exceptions.AssemblerException;
import edu.asu.plp.tool.backend.plpisa.assembler2.*;

import org.json.JSONObject;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.Subscribe;

import edu.asu.SimulatorFiles.*;
import edu.asu.plp.tool.backend.plpisa.sim.*;
import edu.asu.plp.tool.prototype.ApplicationSettings;
import edu.asu.plp.tool.prototype.ProjectAssemblyDetails;
import edu.asu.plp.tool.prototype.Main.ApplicationEventBusEventHandler;
import edu.asu.plp.tool.prototype.model.Project;
import edu.asu.plp.tool.prototype.model.Theme;
import edu.asu.plp.tool.prototype.model.ThemeRequestCallback;
import edu.asu.plp.tool.prototype.view.ConsolePane;
import javafx.stage.Stage;


/**
 * @author ngoel2
 *
 */

@RestController
public class PLPWebController {
	
	String fileStoragePath = "files/";
	HttpSession session;
	private boolean isSimulationRunning;
	ASMImage image = null;
	private Thread simRunThread;
	
	private Stage stage;
	private ConsolePane console;
	private PLPSimulator activeSimulator = new PLPSimulator();
	
	

	@RequestMapping("/register")
	@CrossOrigin
	public String register(@RequestParam(value="un", defaultValue="guestUser") String un, HttpServletRequest request) {
		String response = "";
		String sessionKey;
		Map<String, String> responseMap = new HashMap<String, String> ();
//		sessionKey = PLPUserDB.getInstance().registerNewUser(un);
//		if(sessionKey < 0){
//			response += "\"status\":\"failed\",\"session_key\":-1";
//		} else {
//			response += "\"status\":\"success\",\"session_key\":"+sessionKey;
//		}
		
		session = request.getSession();
		sessionKey = session.getId();
		PLPUserDB.getInstance().registerNewUser(un, session, sessionKey	);
		responseMap.put("status", "success");
		responseMap.put("session_key", sessionKey);

		try {
			response = new ObjectMapper().writeValueAsString(responseMap);
		} catch (JsonProcessingException e) {
			System.out.println("JSON parsing Error.");
			e.printStackTrace();
		}
		//response += "\"status\":\"successs\",\"session_key\":\"" + sessionKey + "\"";
		System.out.println(response);
		return response;
	}

    @RequestMapping(value = "/uploadFile" , method = RequestMethod.POST)
    @CrossOrigin
    public String upload(HttpServletRequest request) {
    	
		String response = "";
    	System.out.println("in upload server side");
    	
        //org.springframework.web.multipart.MultipartHttpServletRequest
        MultipartHttpServletRequest mRequest;
        mRequest = (MultipartHttpServletRequest) request;

        java.util.Iterator<String> itr = mRequest.getFileNames();
        while (itr.hasNext()) {
            //org.springframework.web.multipart.MultipartFile
            MultipartFile mFile = mRequest.getFile(itr.next());
            String fileName = mFile.getOriginalFilename();
            System.out.println("**Saved at** "+fileStoragePath+ fileName);

            //To copy the file to a specific location in machine.
            File file = new File(fileStoragePath+fileName);
            try {
				FileCopyUtils.copy(mFile.getBytes(), file);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println(" server exception in file upload :(");
				e.printStackTrace();
				response += "\"status\":\"failed\"";
				return "{"+response+"}";
			} //This will copy the file to the specific location.
        }
        response += "\"status\":\"success\"";

		return "{"+response+"}";
    }
    
    
    @RequestMapping(value = "/assembleText" , method = RequestMethod.POST)
    @CrossOrigin
    public String assembleText(@RequestBody AssemblyInfo assembly, HttpServletRequest request, HttpSession session) throws AssemblerException, JsonProcessingException {
    	
    	System.out.println("in assemble");
    	String response = "";
    	Map<String, String> responseMap = new HashMap<String, String> ();
    	try {
    		ApplicationSettings.initialize();
    		ApplicationSettings.loadFromFile("settings/plp-tool.settings");
    		EventRegistry.getGlobalRegistry().register(new ApplicationEventBusEventHandler());
    	
	    	String sessKey = assembly.getSessionKey();
	    	System.out.println("Sess: " + assembly.getSessionKey());
	    	session = PLPUserDB.getInstance().getUser(sessKey).getUserSession();
	    	
	    	String code = assembly.getCode();
	    	
	    	WebASMFile asmFile = new WebASMFile(code, "main.asm");
	    	System.out.println("code: " + code);
	    	asmFile.setContent(code);
	    	List<ASMFile> listASM = new ArrayList<ASMFile>();
	    	listASM.add(asmFile);
	    	
	    	//Multiple asm
//	    	WebASMFile asmFile2 = new WebASMFile(code, "samm.asm");
//	    	asmFile2.setContent(code);
//	    	listASM.add(asmFile2);
//	    	
	    	
	    	Assembler assembler = new PLPAssembler();
	    	image = assembler.assemble(listASM);	    	
	    	//System.out.println("IMAGE:    " + image);	    	
	    	System.out.println("ID: " + session.getId());
	    	session.setAttribute("ASMImage", image);
	    	
	    	//System.out.println(image.getDisassemblyInfo());
	    	
	//    	ObjectMapper mapper = new ObjectMapper();
	//    	String jsonInString = mapper.writeValueAsString(image);
	//    	System.out.println("JSON IMG STRING: " + jsonInString);
	//    	
	    	
	//    	response = "\"status\":\"ok\"";
	//    	response += ",\"asmJson\":" + jsonInString;
	    	responseMap.put("status", "ok");
    	
    	}
    	catch (AssemblerException exception)
		{
    		//System.out.println(exception.getLocalizedMessage());
    		//response = "\"status\":\"error\"";
    		//String errorMessage = exception.getLocalizedMessage();
//    		for(int i =0;i<errorMessage.length();i++)
//    		{
//    			if(errorMessage.charAt(i) == ':')
//    			{
//    				//"dfsdfsfs":"sfsdfssfsd"
//    				//errorMessage.
//    			}
//    			else if(errorMessage == "\n")
//    			{
//    				
//    			}
//    		}
    		responseMap.put("status", "failed");
    		responseMap.put("message", exception.getLocalizedMessage());
    		
    		try {
    			response = new ObjectMapper().writeValueAsString(responseMap);
    		} catch (JsonProcessingException e) {
    			System.out.println("JSON parsing Error.");
    			e.printStackTrace();
    		}
    		//response += ",\"message\" : \""+exception.getLocalizedMessage()+"\"";
    		System.out.println(response);
    		
		}
    	
    	return response;
    }
    
	

    @RequestMapping(value = "/Simulator" , method = RequestMethod.GET)
    @CrossOrigin
    public String Simulator(HttpServletRequest request, HttpSession session) throws IOException {
    	System.out.println("in Simulate eclipse");
    	String response = "";
    	
    	ParseTxtFile ptf = new ParseTxtFile();
    	//ptf.mainfunc();
    	
    	session = this.session;
    	System.out.println("ID in simulate: " + session.getId());
    	
    	//System.out.println("ASM OBJ is Sim :"  +  session.getAttribute("ASMImage"));
    	image = (ASMImage) session.getAttribute("ASMImage");
    	System.out.println("ASM OBJ is Sim :" + image);

    	activeSimulator.startListening();
    	
    	EventRegistry.getGlobalRegistry().post(
				new SimulatorControlEvent("load", image));
    	
    	response = "{\"status\":\"ok\"}";
    	return response;
    }
    
    // Run Simulation -- abhilash
    
    @RequestMapping(value = "/Run" , method = RequestMethod.GET)
    @CrossOrigin
    public String Run(HttpServletRequest request, HttpSession session) throws IOException {
    	System.out.println("in Run method");
    	String response = "";
    	
    	
			EventRegistry.getGlobalRegistry().post(
					new SimulatorControlEvent("load", image));
			
			isSimulationRunning = true;
			simRunThread = new Thread(new Runnable(){
				public void run()
				{
					while (isSimulationRunning) {
						EventRegistry.getGlobalRegistry().post(new SimulatorControlEvent("step", null));
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
						}
					}
				}
			});
			simRunThread.start();
			
		
    	
    	
    	response = "{\"status\":\"ok\"}";
    	return response;
    }
    
    // new class copied form main.java
    
    public class ApplicationEventBusEventHandler
	{
		private ApplicationEventBusEventHandler()
		{
			
		}
		
		@Subscribe
		public void applicationThemeRequestCallback(ThemeRequestCallback event)
		{
			if (event.requestedTheme().isPresent())
			{
				Theme applicationTheme = event.requestedTheme().get();
				try
				{
					stage.getScene().getStylesheets().clear();
					stage.getScene().getStylesheets().add(applicationTheme.getPath());
					return;
				}
				catch (MalformedURLException e)
				{
					console.warning("Unable to load application theme "
							+ applicationTheme.getName());
					return;
				}
			}
			
			console.warning("Unable to load application theme.");
		}
		
		@Subscribe
		public void deadEvent(DeadEvent event)
		{
			System.out.println("Dead Event");
			System.out.println("Dead Event");
			System.out.println(event.getEvent());
			System.out.println(event.getSource());
			System.out.println(event.getClass());
		}
	}
}