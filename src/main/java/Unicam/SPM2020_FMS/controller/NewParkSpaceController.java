package Unicam.SPM2020_FMS.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import Unicam.SPM2020_FMS.model.User;
import Unicam.SPM2020_FMS.model.Login;
import Unicam.SPM2020_FMS.model.ParkingSpace;
import Unicam.SPM2020_FMS.service.ParkSpaceService;
import Unicam.SPM2020_FMS.service.ParkSpotService;
import Unicam.SPM2020_FMS.service.StorageService;

@Controller
public class NewParkSpaceController {

	@Autowired
	public ParkSpaceService parkService;

	@Autowired
	public ParkSpotService spotService;

	@Autowired
	public StorageService storageService;

	@RequestMapping(value = "/newParkArea", method = RequestMethod.GET)
	public ModelAndView newParkSpace(HttpServletRequest request, HttpServletResponse response, HttpSession session) {

		User user = (User) session.getAttribute("user");
		if (user != null) {
			if (user.getUserType().equals("Municipality")) {

				ModelAndView mav = new ModelAndView("newParkArea");

				// if coming from an error try to reload old information

				Object oldSpace = session.getAttribute("oldSpace");
				if (oldSpace != null) {
					mav.addObject("parkSpace", (ParkingSpace) oldSpace);
					session.removeAttribute("oldSpace");
				} else {
					mav.addObject("parkSpace", new ParkingSpace());
				}

				// and to show the error message

				Object message = session.getAttribute("message");
				if (message != null) {
					mav.addObject("message", (String) message);
					session.removeAttribute("message");
				}

				return mav;

			} else {
				session.removeAttribute("message");
				return new ModelAndView("welcome", "user", user);
			}
		} else {
			ModelAndView mav = new ModelAndView("login", "login", new Login());
			session.removeAttribute("message");
			mav.addObject("message", "Please login");
			return mav;
		}
	}

	@RequestMapping(value = "/addParkSpace", method = RequestMethod.POST)
	  public String addParkSpace(HttpServletRequest request, HttpServletResponse response, HttpSession session, 
			  @ModelAttribute("newParkSpace") ParkingSpace newParkSpace, BindingResult bindingResult) {
		  
		  
    	
		  String errMsg="";
		  Boolean fileNotUploaded=false;
		  
		  if (bindingResult.hasFieldErrors()){
			  
			  String field = bindingResult.getFieldError().getField();
			  if ( field.contains("Covered")||field.contains("Handicap")) {
				  errMsg = bindingResult.getFieldError().getDefaultMessage().split(":")[1];
			  } else {
				  errMsg = "Operation not completed: invalid information!";  
			  }
			  session.setAttribute("oldSpace", newParkSpace);
			  
		  } else {			  
			  
			  String filename = System.currentTimeMillis()+newParkSpace.getImageFile().getOriginalFilename();
			  newParkSpace.setImageName(filename);
			  int addResult=parkService.add(newParkSpace);			
			  String[] spaceMessages = {
					  "Creating Park Space has not been possible!",
					  "Position specified has been already used"
			  };

			  if (addResult<=0) {
				  
				  //deleteImageFile
				  addResult*=-1;
				  errMsg=spaceMessages[addResult];
				  session.setAttribute("oldSpace", newParkSpace);
				  
			  } else {
				  
				  newParkSpace.setIdParkingSpace(addResult);
				  
				  //try to store the uploaded file
				  try {
					  storageService.store(newParkSpace.getImageFile(),filename);
				  } catch (Exception e) {
					  fileNotUploaded=true;
				  }
				  
				  //try to generate spots
				  int genResult = spotService.generateSpots(newParkSpace.getSpots());
				  if (genResult<0) {
					  if (fileNotUploaded) {
						  errMsg = "Operation not completed: Park Area has been created without spots and without map";
					  }
					  errMsg = "Operation not completed: Park Area has been created without spots";
				  } else {
					  if (fileNotUploaded) {
						  errMsg = "Park Space correctly created without map";
					  }
					  errMsg = "Park Space correctly created with Parking spots and map"; 
				  }
				  
			  }
			
		  }
	    
		session.setAttribute("message", errMsg);
    	return "redirect:/newParkArea";
	}
}
