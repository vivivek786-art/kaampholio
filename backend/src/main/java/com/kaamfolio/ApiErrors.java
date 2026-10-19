package com.kaamfolio;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice
public class ApiErrors {
	@ExceptionHandler(ResponseStatusException.class)
	ResponseEntity<?> status(ResponseStatusException e) {
		return ResponseEntity.status(e.getStatusCode())
				.body(Map.of("message", e.getReason() == null ? "Request failed" : e.getReason()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<?> validation(MethodArgumentNotValidException e) {
		var first = e.getBindingResult().getFieldErrors().get(0);
		return ResponseEntity.badRequest().body(Map.of("message", first.getField() + ": " + first.getDefaultMessage()));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<?> malformed() {
		return ResponseEntity.badRequest().body(Map.of("message", "Invalid JSON request"));
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	ResponseEntity<?> size() {
		return ResponseEntity.status(413).body(Map.of("message", "File must be no larger than 25 MB"));
	}
}
