package in.chaser.api;

import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.util.Map;

@RestControllerAdvice
public class Errors {
    @ExceptionHandler(IllegalArgumentException.class) ResponseEntity<?> invalid(IllegalArgumentException e) { return error(400,e.getMessage()); }
    @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> validation(MethodArgumentNotValidException e) { return error(400,"Check the required fields, dates and amount (two decimal places)."); }
    @ExceptionHandler(HttpMessageNotReadableException.class) ResponseEntity<?> json(HttpMessageNotReadableException e) { return error(400,"Invalid request format or date."); }
    @ExceptionHandler(MaxUploadSizeExceededException.class) ResponseEntity<?> size(Exception e) { return error(413,"File limit is 10 MB."); }
    @ExceptionHandler(ResponseStatusException.class) ResponseEntity<?> status(ResponseStatusException e) { return error(e.getStatusCode().value(),e.getReason()==null?"Request rejected.":e.getReason()); }
    @ExceptionHandler(Exception.class) ResponseEntity<?> internal(Exception e) {
        org.slf4j.LoggerFactory.getLogger(Errors.class).error("request_failed type={}",e.getClass().getSimpleName());
        return error(500,"Unable to complete this request. Please try again.");
    }
    private ResponseEntity<?> error(int status,String message) { return ResponseEntity.status(status).body(Map.of("message",message)); }
}
