/**
 * 
 */
package edu.asu.PLPWebserver;


import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

import java.io.File;
import java.io.IOException;
import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import edu.asu.plp.tool.backend.isa.*;
import edu.asu.plp.tool.backend.isa.exceptions.AssemblerException;
import edu.asu.plp.tool.backend.plpisa.assembler2.*;


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

import edu.asu.SimulatorFiles.*;
import edu.asu.plp.tool.backend.plpisa.sim.*;
import edu.asu.plp.tool.prototype.model.Project;


/**
 * @author ngoel2
 *
 */

@RestController
public class PLPWebController {
	
	String fileStoragePath = "C:/Users/sjjai/Desktop/PLP/";
	HttpSession session;

	@RequestMapping("/register")
	@CrossOrigin
	public String register(@RequestParam(value="un", defaultValue="guestUser") String un, HttpServletRequest request) {
		String response = "";
		String sessionKey;
//		sessionKey = PLPUserDB.getInstance().registerNewUser(un);
//		if(sessionKey < 0){
//			response += "\"status\":\"failed\",\"session_key\":-1";
//		} else {
//			response += "\"status\":\"success\",\"session_key\":"+sessionKey;
//		}
		
		session = request.getSession();
		sessionKey = session.getId();
		PLPUserDB.getInstance().registerNewUser(un, session, sessionKey	);
		if(session.isNew())
			response += "\"status\":\"successs\",\"session_key\":\"" + sessionKey + "\"";
		System.out.println(response);
		return "{"+response+"}";
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
    	String sessKey = assembly.getSessionKey();
    	System.out.println("Sess: " + assembly.getSessionKey());
    	session = PLPUserDB.getInstance().getUser(sessKey).getUserSession();
    	
    	String response = "";
    	
    	String code = assembly.getCode();
    	
    	WebASMFile asmFile = new WebASMFile(code, "main.asm");
    	System.out.println("code: " + code);
    	asmFile.setContent(code);
    	List<ASMFile> listASM = new ArrayList<ASMFile>();
    	listASM.add(asmFile);
    	
    	Assembler assembler = new PLPAssembler();
    	
    	ASMImage image = assembler.assemble(listASM);
    	
    	System.out.println("IMAGE:    " + image);
    	
    	
    	System.out.println("ID: " + session.getId());
    	
    	session.setAttribute("ASMImage", image);
    	
    	//System.out.println(image.getDisassemblyInfo());
    	
//    	ObjectMapper mapper = new ObjectMapper();
//    	String jsonInString = mapper.writeValueAsString(image);
//    	System.out.println("JSON IMG STRING: " + jsonInString);
//    	
    	
//    	response = "\"status\":\"ok\"";
//    	response += ",\"asmJson\":" + jsonInString;
    	
    	
    	
    	return "{"+response+"}";
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
    	ASMImage image = (ASMImage) session.getAttribute("ASMImage");
    	System.out.println("ASM OBJ is Sim :" + image);

    	response = "{\"status\":\"ok\"}";
    	return response;
    }
}
