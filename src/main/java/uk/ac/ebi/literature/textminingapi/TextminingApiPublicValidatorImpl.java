package uk.ac.ebi.literature.textminingapi;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.ac.ebi.literature.textminingapi.pojo.FileInfo;
import uk.ac.ebi.literature.textminingapi.pojo.Status;
import uk.ac.ebi.literature.textminingapi.pojo.SubmissionMessage;
import uk.ac.ebi.literature.textminingapi.service.MongoService;
import uk.ac.ebi.literature.textminingapi.utility.Utility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class TextminingApiPublicValidatorImpl implements TextminingApiPublicValidator {
	
	private static final Logger logger = LoggerFactory.getLogger(TextminingApiPublicValidatorImpl.class);
	private final MongoService mongoService;

	public TextminingApiPublicValidatorImpl(MongoService mongoService) {
		this.mongoService = mongoService;
	}

	@Override
	public boolean validateSubmission(SubmissionMessage obj, AtomicReference<List<String>> errorMessage, AtomicReference<SubmissionMessage> previousSubmission) {
		
		boolean valid = true;
		
		List<String> errors = new ArrayList<>();
		
        if (obj==null) {
        	errors.add(NULL_ERROR);
        	valid = false;
        }
        
        if ((obj!= null) && (Utility.isEmpty(obj.getFtId()))) {
        	errors.add(FT_ID_EMPTY_ERROR);
        	valid = false;
        }
        
        if ((obj!= null) && (!Utility.isEmpty(obj.getFtId())) ) {
        	if (!checkMongoSubmission(obj, previousSubmission)) {
        		errors.add(SUBMISSION_ALREADY_EXISTING_ERROR);
        		valid = false;
        	}
        }
        
        if ((obj!= null) && (Utility.isEmpty(obj.getCallback()))) {
        	errors.add(CALLBACK_EMPTY_ERROR);
        	valid = false;
        }
                
        if ((obj!= null) && (!Utility.isEmpty(obj.getCallback())) && (this.isNotValidUrl(obj.getCallback())) ) {
        	errors.add(CALLBACK_INVALID_URL_ERROR);
        	valid = false;
        }

        if ((obj!=null) && ((obj.getFiles()==null) || (obj.getFiles().length==0))) {
        	errors.add(NO_FILE_ERROR);
        	valid = false;
        }
        
        if ((obj!=null) && ((obj.getFiles()!=null) && (obj.getFiles().length>0))) {
        	int index=1;
        	AtomicReference<List<String>> errorMessageFile;
        	for (FileInfo file : obj.getFiles()) {
        		errorMessageFile = new AtomicReference<>();
        		if (!this.isValidFile(file, errorMessageFile, index)) {
        			errors.addAll(errorMessageFile.get());
                	valid = false;
        		}
        		index++;
        	}
        }
        
        Optional<String> duplicateFile = checkDuplicateFileInSubmission(obj);
        if((obj!= null) && duplicateFile.isPresent()) {
        	errors.add(String.format(DUPLICATE_FILENAME_ERROR, duplicateFile.get()));
        	valid = false;
        }
        
        errorMessage.set(errors);
		return valid;
	}

	private Optional<String> checkDuplicateFileInSubmission(SubmissionMessage obj) {
		if(obj!=null && obj.getFiles()!=null && obj.getFiles().length>0) {
			Map<String, List<FileInfo>> grouping = Stream.of(obj.getFiles()).filter(file->(file!=null && StringUtils.isNotBlank(file.getFilename()))).collect(Collectors.groupingBy(FileInfo::getFilename));
			for(String fileName : grouping.keySet()) {
				if(grouping.get(fileName).size()>1) return Optional.of(fileName);
			}
		}
		return Optional.empty();	
	}
	
	private boolean isValidFile(FileInfo file , AtomicReference<List<String>> errorMessage, int index) {
		List<String> errors = new ArrayList<>();
		boolean valid=true;
		if (Utility.isEmpty(file.getFilename())) {
			errors.add(String.format(FILE_NAME_EMPTY_ERROR, index));
        	valid = false;
		}
		
		if (Utility.isEmpty(file.getUrl())) {
			errors.add(String.format(FILE_URL_EMPTY_ERROR, index));
        	valid = false;
		}
		
		if ((!Utility.isEmpty(file.getUrl())) && (this.isNotValidUrl(file.getUrl()))) {
			errors.add(String.format(FILE_URL_NOT_VALID_ERROR, index));
        	valid = false;
		}
		
		errorMessage.set(errors);
		
		return valid;
	}
	
	private boolean isNotValidUrl(String url) {
		UrlValidator urlValidator = new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS);
		return !urlValidator.isValid(url);
	}
	
	private boolean checkMongoSubmission(SubmissionMessage obj, AtomicReference<SubmissionMessage>previousSubmission) {
		SubmissionMessage existingSubmission = mongoService.findSubmission(obj.getFtId(), obj.getUser());
    	if (existingSubmission!=null) {
    		previousSubmission.set(existingSubmission);
    		return Status.getStatusByLabel(existingSubmission.getStatus()) != Status.PENDING;
    	}
    	return true;
	}
	
	private SubmissionMessage getMongoSubmission(String ftId, String user) {
		return mongoService.findSubmission(ftId, user);
	}

	@Override
	public boolean validateDeletion(String ftId, String user, AtomicReference<List<String>> errorMessages) {
		boolean valid = true;
		
		List<String> errors = new ArrayList<>();
        
        if ((Utility.isEmpty(ftId))) {
        	errors.add(FT_ID_EMPTY_URL_PATH_DELETE_ERROR);
        	valid = false;
        }
        
        if ((!Utility.isEmpty(ftId))) {
        	SubmissionMessage existingSubmission = this.getMongoSubmission(ftId, user);
        	
        	if (existingSubmission==null) {
        		valid = false;
        		errors.add(String.format(SUBMISSION_NOT_FOUND_ERROR, ftId));
        	}else {
        		if (Status.getStatusByLabel(existingSubmission.getStatus()) == Status.PENDING) {
        			valid = false;
        			errors.add(String.format(SUBMISSION_PENDING_ERROR_DELETION, ftId));	
        		}
        	}
        }
        
        errorMessages.set(errors);
        return valid;
	}
}
