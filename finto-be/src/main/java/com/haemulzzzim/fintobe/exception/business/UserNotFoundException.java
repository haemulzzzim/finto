package com.haemulzzzim.fintobe.exception.business;

import com.haemulzzzim.fintobe.exception.BusinessException;
import com.haemulzzzim.fintobe.exception.ErrorCode;

public class UserNotFoundException extends BusinessException {

	public UserNotFoundException(String message) {
		super(message, ErrorCode.USER_NOT_FOUND);
	}

	public UserNotFoundException(ErrorCode errorCode) {
		super(ErrorCode.USER_NOT_FOUND);
	}

}