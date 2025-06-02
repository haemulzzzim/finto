package com.haemulzzzim.fintobe.exception.business;

import com.haemulzzzim.fintobe.exception.BusinessException;
import com.haemulzzzim.fintobe.exception.ErrorCode;

public class InvalidValueException extends BusinessException {

	public InvalidValueException(String value) {
		super(value, ErrorCode.INVALID_INPUT_VALUE);
	}

	public InvalidValueException(String value, ErrorCode errorCode) {
		super(value, errorCode);
	}

}