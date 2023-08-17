package uk.ac.ebi.literature.textminingapi;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.ebi.literature.textminingapi.pojo.AnnotationsData;
import uk.ac.ebi.literature.textminingapi.pojo.SubmissionMessage;

@RestController
@RequestMapping("")
public class TextminingApiPublicController {
	
	private final TextminingApiPublicService textminingApiService;
    
	private static Logger logger = LoggerFactory.getLogger(TextminingApiPublicController.class);

    public TextminingApiPublicController(TextminingApiPublicService textminingApiService) {
        this.textminingApiService = textminingApiService;
    }

    @RequestMapping(value = "/delete/{ftId}", method = RequestMethod.DELETE)
    public ResponseEntity<List<String>> delete(@PathVariable(required=true, name="ftId") String ftId) {
        
        AtomicReference<List<String>> errors = new AtomicReference<List<String>>(new ArrayList<String>());

        AtomicReference<HttpStatus> retStatus = new AtomicReference<HttpStatus>();

        boolean success = textminingApiService.processDeletion(ftId, errors, retStatus);

        HttpStatus retStatusVal = success ? HttpStatus.OK : this.getStateFromServiceResponse(retStatus);
        ResponseEntity<List<String>> ret = new ResponseEntity<List<String>>(errors.get(), retStatusVal);
        
        return ret;
    }
    
    protected HttpStatus getStateFromServiceResponse(AtomicReference<HttpStatus> retStatus) {
    	return retStatus.get();
    }
    
    
    @RequestMapping(value = "/submit", method = RequestMethod.POST)
    public ResponseEntity<List<String>> submit(@RequestBody SubmissionMessage inputData) {
     
        AtomicReference<List<String>> errors = new AtomicReference<List<String>>(new ArrayList<String>());

        boolean success = textminingApiService.processSubmission(inputData, errors);

        HttpStatus retStatus = success ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        ResponseEntity<List<String>> ret = new ResponseEntity<List<String>>(errors.get(), retStatus);
        
        return ret;
    }
    
    @RequestMapping(value = "/getAnnotations/{ftId}/{filename}", method = RequestMethod.GET)
    public ResponseEntity<AnnotationsData> getAnnotations(@PathVariable(required=true, name="ftId") String ftId, @PathVariable(required=true, name="filename") String filename) {
        
    	AnnotationsData annotationsData = textminingApiService.getAnnotationsData(ftId, filename);

        HttpStatus retStatus = annotationsData!=null ? HttpStatus.OK : HttpStatus.NOT_FOUND;
        ResponseEntity<AnnotationsData> ret = new ResponseEntity<AnnotationsData>(annotationsData, retStatus);
        
        return ret;
    }
    
    @RequestMapping(value = "/getAnnotations/{ftId}", method = RequestMethod.GET)
    public ResponseEntity<List<AnnotationsData>> getAnnotations(@PathVariable(required=true, name="ftId") String ftId) {
        
    	List<AnnotationsData> annotationsDataList = textminingApiService.getAnnotationsData(ftId);

        HttpStatus retStatus = (annotationsDataList!=null && !annotationsDataList.isEmpty()) ? HttpStatus.OK : HttpStatus.NOT_FOUND;
        ResponseEntity<List<AnnotationsData>> ret = new ResponseEntity<List<AnnotationsData>>(annotationsDataList, retStatus);
        
        return ret;
    }
    
    @RequestMapping(value = "/getSubmissionStatus/{ftId}", method = RequestMethod.GET)
    public ResponseEntity<SubmissionMessage> getSubmissionStatus(@PathVariable(required=true, name="ftId") String ftId) {
        
    	SubmissionMessage submissionData = textminingApiService.getSubmissionStatus(ftId);

        HttpStatus retStatus = submissionData!=null ? HttpStatus.OK : HttpStatus.NOT_FOUND;
        ResponseEntity<SubmissionMessage> ret = new ResponseEntity<SubmissionMessage>(submissionData, retStatus);
        
        return ret;
    }
    
    @PostMapping("result")
	public ResponseEntity<?> result(@RequestBody SubmissionMessage submissionMessage) {
		logger.info("Request Arrived -> " + submissionMessage);
		return new ResponseEntity<>(HttpStatus.OK);
	}
}