package uk.ac.ebi.literature.textminingapi;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import uk.ac.ebi.literature.textminingapi.pojo.SubmissionMessage;

public interface TextminingApiPublicValidator {
	
	public final static String NULL_ERROR="Submission message can not be empty";
    public final static String FT_ID_EMPTY_ERROR="ft_id field can not be empty";
    public final static String FT_ID_EMPTY_URL_PATH_DELETE_ERROR="ft_id parameter mandatory in the URL path (i.e. /delete/PMC1234567)";
    public final static String FT_ID_NOT_EXISTING_IN_EPMC_ERROR="ft_id not existing in Europe PMC";
    public final static String SUBMISSION_ALREADY_EXISTING_ERROR="It exists already a submission for this user and ft_id in pending state";
    public final static String CALLBACK_EMPTY_ERROR="callback field can not be empty";
    public final static String CALLBACK_INVALID_URL_ERROR="callback field must be a valid URL";
    public final static String NO_FILE_ERROR="The submission should contain at least one file";
    public final static String FILE_URL_NOT_VALID_ERROR="The url is not valid for file number %d";
    public final static String FILE_URL_EMPTY_ERROR="The url is not populated for file number %d";
    public final static String FILE_NAME_EMPTY_ERROR= "The filename is not populated for file number %d";
    public final static String SUBMISSION_NOT_FOUND_ERROR= "Can not be found a submission with ft_id %s";
    public final static String SUBMISSION_PENDING_ERROR_DELETION= "Submission with ft_id %s still to be fully processed. It can be deleted only afterwards the processing has been fully completed";
    public final static String DUPLICATE_FILENAME_ERROR= "File %s appears more than once in the request body";
    
    boolean validateSubmission(SubmissionMessage obj, AtomicReference<List<String>> errorMessages, AtomicReference<SubmissionMessage> previousSubmission);
    
    boolean validateDeletion(String ftId, String user, AtomicReference<List<String>> errorMessages);
}
