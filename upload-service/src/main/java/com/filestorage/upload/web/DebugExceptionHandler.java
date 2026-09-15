package com.filestorage.upload.web;


import com.filestorage.upload.client.*;
import com.filestorage.upload.controller.*;
import com.filestorage.upload.domain.*;
import com.filestorage.upload.dto.*;
import com.filestorage.upload.job.*;
import com.filestorage.upload.messaging.*;
import com.filestorage.upload.repository.*;
import com.filestorage.upload.service.*;
import com.filestorage.upload.storage.*;
import com.filestorage.upload.web.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class DebugExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handle(ResponseStatusException ex) {

        System.out.println(">>> [DEBUG UPLOAD EXCEPTION]");
        System.out.println("Status  = " + ex.getStatusCode());
        System.out.println("Reason  = " + ex.getReason());
        System.out.println("Cause   = " + ex.getCause());

  if(ex.getCause() != null) {
            ex.getCause().printStackTrace();
        }

        return ResponseEntity
                .status(ex.getStatusCode())
                .body(ex.getReason());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleOther(Exception ex) {

        System.out.println(">>> [DEBUG UPLOAD UNKNOWN EXCEPTION]");
        ex.printStackTrace();

        return ResponseEntity
                .internalServerError()
                .body(ex.getMessage());
    }
}