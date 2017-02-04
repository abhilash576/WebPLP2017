/**
 * 
 */
package edu.asu.PLPWebserver;


import java.io.File;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import edu.asu.SimulatorFiles.*;
import edu.asu.plp.tool.backend.plpisa.sim.*;


/**
 * @author ngoel2
 *
 */

@RestController
public class PLPWebController {
	
	String fileStoragePath = "E:/PLP/PLP-Simulator/";

	@RequestMapping("/register")
	@CrossOrigin
	public String register(@RequestParam(value="un", defaultValue="guestUser") String un) {
		int sessionKey;
		String response = "";
		sessionKey = PLPUserDB.getInstance().registerNewUser(un);
		if(sessionKey < 0){
			response += "\"status\":\"failed\",\"session_key\":-1";
		} else {
			response += "\"status\":\"success\",\"session_key\":"+sessionKey;
		}
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
    public String assembleText(@RequestBody String textval) {
    	System.out.println("in assemble");
    	String response = "";
    	
    	String code = textval;
    	System.out.println("code: " + code);
    	
    	response = "{\"status\":\"ok\"}";
    	return response;
    }
    
    @RequestMapping(value = "/Simulator" , method = RequestMethod.GET)
    @CrossOrigin
    public String Simulator(HttpServletRequest request) throws IOException {
    	System.out.println("in Simulate eclipse");
    	String response = "";
    	
    	ParseTxtFile ptf = new ParseTxtFile();
    	ptf.mainfunc();
    	

    	response = "{\"status\":\"ok\"}";
    	return response;
    }
}
