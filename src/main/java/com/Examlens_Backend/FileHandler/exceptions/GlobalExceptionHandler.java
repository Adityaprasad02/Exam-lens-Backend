package com.Examlens_Backend.FileHandler.exceptions;


import net.bytebuddy.jar.asm.Handle;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HandleException.class)
    public ResponseEntity<?> handleFileExceptions(HandleException ex){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("Error" , ex.getMessage())) ;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> MaxUpload(MaxUploadSizeExceededException exceededException){

        assert exceededException.getBody().getDetail() != null;
        Map<String,Object> map = Map.of("Error" , exceededException.getBody()) ;

        return new ResponseEntity<>(map,HttpStatus.BAD_REQUEST) ;
    }
}
