package uk.ac.ebi.literature.textminingapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import uk.ac.ebi.literature.textminingapi.pojo.AnnotationsData;
import uk.ac.ebi.literature.textminingapi.pojo.FileInfo;
import uk.ac.ebi.literature.textminingapi.pojo.MLTextObject;
import uk.ac.ebi.literature.textminingapi.pojo.Status;
import uk.ac.ebi.literature.textminingapi.pojo.SubmissionMessage;
import uk.ac.ebi.literature.textminingapi.service.MLQueueSenderService;
import uk.ac.ebi.literature.textminingapi.service.MongoService;
import uk.ac.ebi.literature.textminingapi.utility.Utility;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class TextminingApiPublicService {

	protected static final String INTERNAL_DELETION_ERRER="Internal errors prevented submission to be deleted successfully";
	protected static final String DB_ERROR_MSG="Network errors prevented messages to be stored successfully";
	protected static final String NETWORK_ERROR_MSG="Network errors prevented messages to be processed successfully";
	private static Logger logger = LoggerFactory.getLogger(TextminingApiPublicService.class);

	private final MLQueueSenderService queueSenderService;

	private final MongoService mongoService;

	@Value("${rabbitmq.tmExchange}")
	private String PUBLISH_EXCHANGE;

	@Value("${rabbitmq.submissionsQueue}")
	private String SUBMISSIONS_QUEUE;
    
	private final TextminingApiPublicValidator validator;
	
	@Value("${mongo.transaction}")
	private boolean transactionMongo;

	public TextminingApiPublicService(MLQueueSenderService queueSenderService, MongoService mongoService, TextminingApiPublicValidator validator) {
		this.queueSenderService = queueSenderService;
		this.mongoService = mongoService;
		this.validator = validator;
	}

	protected String getUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
	
	protected void populateData(SubmissionMessage inputData) {
		inputData.setUser(getUsername());
		inputData.setStatus(Status.PENDING.getLabel());
		if (inputData.getFiles()!=null) {
			for ( FileInfo file : inputData.getFiles()) {
				file.setStatus(Status.PENDING.getLabel());
			}
		}
	}
	
	public boolean processSubmission(SubmissionMessage inputData, AtomicReference<List<String>> errorsOutput) {
		// populate default fields before validating and to insert eventually in mongoDb
		populateData(inputData);
		logger.info("Received post submission: {}", inputData);
		List<String> errorMessages = new ArrayList<>();
		boolean success = true;
		AtomicReference<SubmissionMessage> previousSubmission = new AtomicReference<SubmissionMessage> ();
		boolean valid= validator.validateSubmission(inputData, errorsOutput, previousSubmission);
		if (!valid) {
			String errorMsg="Submission Message was rejected as invalid";
			if (!Utility.isEmpty(errorsOutput.get())) {
				errorMsg = errorMsg + ": "+ String.join("\n", errorsOutput.get());
			}
			logger.error(errorMsg);
		    success = false;
		    errorMessages.addAll(errorsOutput.get());      
		}else {
			
			//inserting into Mongo
			boolean storingMongo = this.insertSubmissionIntoMongo(inputData, previousSubmission.get());
			
			if (storingMongo) {
				//inserting messages in RabbitMq (one for each file)
				boolean sendingMessage = true;
				for (FileInfo file : inputData.getFiles()) {
					MLTextObject message = new MLTextObject();
					message.setUser(inputData.getUser());
					message.setFtId(inputData.getFtId());
					message.setStatus(file.getStatus());
					message.setFilename(file.getFilename());
					message.setUrl(file.getUrl());
					sendingMessage = queueSenderService.sendMessageToQueue(SUBMISSIONS_QUEUE, message, PUBLISH_EXCHANGE);
					 if (!sendingMessage) {
				        success = false;
				        errorMessages.add(NETWORK_ERROR_MSG);
				        logger.error("Error in storing message to submission queue {}", message);
				        break;
					}
				}
			}else {
				success = false;
		        errorMessages.add(DB_ERROR_MSG);
			}
			
		}
		
		if (success) {
        	logger.info("Processed post submission with success: {}", inputData);
        }else {
        	logger.error("Processed post submission with failure: {}", inputData);
        }
		
		errorsOutput.set(errorMessages);
		return success;
   }
   
   protected boolean insertSubmissionIntoMongo(SubmissionMessage inputData, SubmissionMessage existingData) {
       boolean ret = true;
       if (existingData != null) {
           // preserve the existing information
    	   inputData.set_id(existingData.get_id());
    	   inputData.setDateInserted(existingData.getDateInserted());
       }
       try {
    	   if (this.transactionMongo) {
    		   mongoService.storeSubmission(inputData);
    	   }else {
    		   mongoService.storeSubmissionNoTransactional(inputData);
    	   }
    	   
    	   logger.info((existingData != null ? "Updated" : "Inserted new") + " submission to DB: {}" , inputData.toString());
       }catch(Exception e) {
    	   logger.error("Problems in saving submission in mongoDb "+inputData.toString(), e);
    	   ret = false;
       }
       
       return ret;
       
   }
   
   public boolean processDeletion(String ftId, AtomicReference<List<String>> errorsOutput, AtomicReference<HttpStatus> httpStatus) {
		
	   	String user = this.getUsername();
	   	logger.info("Received delete submission: for ftId {} and user {}", ftId, user);
		List<String> errorMessages = new ArrayList<>();
		boolean success = true;
		boolean valid= validator.validateDeletion(ftId, user, errorsOutput);
		if (!valid) {
			String errorMsg="Deletion request was rejected as invalid";
			if (!Utility.isEmpty(errorsOutput.get())) {
				errorMsg = errorMsg + ": "+ String.join("\n", errorsOutput.get());
			}
			logger.error(errorMsg);
		    success = false;
		    errorMessages.addAll(errorsOutput.get());  
		    httpStatus.set(HttpStatus.BAD_REQUEST);
		}else {

			boolean deletingFromMongo = this.deleteSubmissionFromMongo(ftId, user);
			
			if (!deletingFromMongo) {
				success = false;
		        errorMessages.add(INTERNAL_DELETION_ERRER);
		        httpStatus.set(HttpStatus.INTERNAL_SERVER_ERROR);
			}
			
		}
		
		if (success) {
			httpStatus.set(HttpStatus.OK);
			logger.info("Submission deleted successfully for ftId "+ftId+ " and user "+user);
		}else {
			logger.error("Problems in deleting submission for ftId "+ftId+ " and user "+user);
		}
		errorsOutput.set(errorMessages);
		return success;
  }

	protected boolean deleteSubmissionFromMongo(String ftId, String user) {
		boolean ret = true;
	    
	    try {
	    	if (this.transactionMongo) {
	    		mongoService.deleteSubmission(ftId, user);
	    	}else {
	    		mongoService.deleteSubmissionNoTransactional(ftId, user);
	    	}
	 	   logger.info("Submission deleted successfully from mongoDB for ftId "+ftId+ " and user "+user);
	    }catch(Exception e) {
	 	   logger.error("Problems in deleting submission from mongoDb for ftId "+ftId+ " and user "+user, e);
	 	   ret = false;
	    }
	    
	    return ret;
	}

	public AnnotationsData getAnnotationsData(String ftId, String filename) {
		String user = this.getUsername();
	   	logger.info("Received getAnnotationsData request for ftId {} and user {} and filename {}", ftId, user, filename);
	   	AnnotationsData ret = this.mongoService.findAnnotations(ftId, user, filename);
	   	if (ret == null) {
	   		logger.error("No annotations data retrieved for request for ftId {} and user {} and filename {}", ftId, user, filename);
	   	}else {
	   		logger.info("Retrieved successfully annotations data for request for ftId {} and user {} and filename {}", ftId, user, filename);
	   	    //irrelevant information  not required in API response sat to null
	   		ret.setUser(null);
	   		ret.set_id(null);
	   		ret.setDateInserted(null);
	   		ret.setDateModified(null);
	   	}
	   	
	   	return ret;
	}
	
	public List<AnnotationsData> getAnnotationsData(String ftId) {
		String user = this.getUsername();
	   	logger.info("Received getAnnotationsData request for ftId {} and user {}", ftId, user);
	   	List<AnnotationsData> list = this.mongoService.findAnnotations(ftId, user);
	   	if (list == null || list.isEmpty()) {
	   		logger.error("No annotations data retrieved for request for ftId {} and user {}", ftId, user);
	   	}else {
	   		logger.info("Retrieved successfully annotations data for request for ftId {} and user {}", ftId, user);
	   		list.forEach(annotations->{
	   			annotations.setUser(null);
	   			annotations.set_id(null);
	   			annotations.setDateInserted(null);
	   			annotations.setDateModified(null);
	   		});
	   	}
	   	return list;
	}

	public SubmissionMessage getSubmissionStatus(String ftId) {
		String user = this.getUsername();
	   	logger.info("Received getSubmission request for ftId {} and user {}", ftId, user);
	   	SubmissionMessage ret = this.mongoService.findSubmission(ftId, user);
	   	if (ret == null) {
	   		logger.error("No submission data retrieved for request for ftId {} and user {}", ftId, user);
	   	}else {
	   		logger.info("Retrieved successfully submission data for request for ftId {} and user {}", ftId, user);
	   	    //irrelevant information  not required in API response sat to null
	   		ret.setUser(null);
	   		ret.set_id(null);
	   		ret.setDateInserted(null);
	   		ret.setDateModified(null);
	   		if (ret.getFiles()!=null && ret.getFiles().length>0) {
	   			for (FileInfo fileInfo : ret.getFiles()) {
	   				fileInfo.setErrorComponent(null);
	   			}
	   		}
	   	}
	   	
	   	return ret;
	}
}

