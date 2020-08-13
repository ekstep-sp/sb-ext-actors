/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.exception;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.sunbird.common.models.util.ProjectLogger;

@ControllerAdvice
public class ExceptionHandlerControllerAdvice {

	@ExceptionHandler(Throwable.class)
	@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
	public @ResponseBody ExceptionResponse handleException(final Exception exception,
			final HttpServletRequest request) {

		ExceptionResponse error = new ExceptionResponse();
		error.setErrorMessage("Internal Server Error");
		error.callerURL(request.getRequestURI());
		exception.printStackTrace();
		System.out.println("error caught here " + request.getRequestURI());
		ProjectLogger.log(request.getRequestURI(), exception);
		return error;
	}

	/**
	 * Handles all invalid data exceptions
	 * 
	 * @param exception
	 * @param request
	 * @return
	 */
	@ExceptionHandler({ InvalidDataInputException.class })
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public @ResponseBody ExceptionResponse HandleInvalidDataInputException(final InvalidDataInputException exception,
			final HttpServletRequest request) {
		ExceptionResponse error = new ExceptionResponse();
		String message = exception.getMessage();
		if (message == null || message.isEmpty())
			message = "invalid input data";
		ProjectLogger.log(request.getRequestURI(), exception);
		error.setErrorMessage(message);
		error.callerURL(request.getRequestURI());
		return error;
	}

	/**
	 * Handles all Validation exceptions
	 * 
	 * @param exception
	 * @param request
	 * @return
	 */
	@ExceptionHandler({ MethodArgumentNotValidException.class })
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public @ResponseBody ExceptionResponse HandleMethodArgumentNotValidException(
			final MethodArgumentNotValidException exception, final HttpServletRequest request) {
		ExceptionResponse error = new ExceptionResponse();
		String message = exception.getMessage();
		if (message == null || message.isEmpty())
			message = "invalid input data";
		ProjectLogger.log(request.getRequestURI(), exception);
		error.setErrorMessage(message);
		error.callerURL(request.getRequestURI());
		return error;
	}

	/**
	 * Handles all Validation exceptions
	 * 
	 * @param exception
	 * @param request
	 * @return
	 */
	@ExceptionHandler({ ConstraintViolationException.class })
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public @ResponseBody ExceptionResponse HandleConstraintViolationException(
			final ConstraintViolationException exception, final HttpServletRequest request) {
		ExceptionResponse error = new ExceptionResponse();
		String message = exception.getMessage();
		if (message == null || message.isEmpty())
			message = "invalid input data";
		ProjectLogger.log(request.getRequestURI(), exception);
		error.setErrorMessage(message);
		error.callerURL(request.getRequestURI());
		return error;
	}

	@ExceptionHandler({ HttpMessageNotReadableException.class })
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public @ResponseBody ExceptionResponse HandleHttpMessageNotReadableException(
			final ConstraintViolationException exception, final HttpServletRequest request) {
		ExceptionResponse error = new ExceptionResponse();
		String message = exception.getMessage();
		if (message == null || message.isEmpty())
			message = "invalid input data";
		ProjectLogger.log(request.getRequestURI(), exception);
		error.setErrorMessage(message);
		error.callerURL(request.getRequestURI());
		return error;
	}

	/**
	 * To handle parsing Exceptions
	 * 
	 * @param exception
	 * @param request
	 * @return
	 */
	@ExceptionHandler({ MethodArgumentTypeMismatchException.class })
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public @ResponseBody ExceptionResponse HandleNumberFormatException(
			final MethodArgumentTypeMismatchException exception, final HttpServletRequest request) {
		ExceptionResponse error = new ExceptionResponse();
		String message = "Bad Request";
		if (message == null || message.isEmpty())
			message = "invalid input data";
		error.setErrorMessage(message);
		error.callerURL(request.getRequestURI());
		ProjectLogger.log(request.getRequestURI(), exception);
		return error;
	}

	/**
	 * To handle invalid input characters
	 * 
	 * @param exception
	 * @param request
	 * @return
	 */
	@ExceptionHandler({ IllegalArgumentException.class })
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public @ResponseBody ExceptionResponse HandleIllegalArgumentException(final IllegalArgumentException exception,
			final HttpServletRequest request) {
		ExceptionResponse error = new ExceptionResponse();
		String message = "Bad Request";
		if (message == null || message.isEmpty())
			message = "invalid input data";
		error.setErrorMessage(message);
		error.callerURL(request.getRequestURI());
		ProjectLogger.log(request.getRequestURI(), exception);
		return error;
	}

	@ExceptionHandler({ MissingServletRequestParameterException.class })
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public @ResponseBody ExceptionResponse HandleMissingServletRequestParameterException(
			final MissingServletRequestParameterException exception, final HttpServletRequest request) {
		ExceptionResponse error = new ExceptionResponse();
		String message = "Bad Request";
		if (message == null || message.isEmpty())
			message = "invalid input data";
		error.setErrorMessage(message);
		error.callerURL(request.getRequestURI());
		ProjectLogger.log(request.getRequestURI(), exception);
		return error;
	}
}