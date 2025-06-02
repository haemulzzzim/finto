package com.haemulzzzim.fintobe.exception.business;

import com.haemulzzzim.fintobe.exception.BusinessException;
import com.haemulzzzim.fintobe.exception.ErrorCode;

public class EntityAlreadyExistException extends BusinessException {

	public EntityAlreadyExistException(String message, ErrorCode errorCode) {
		super(message, errorCode);
	}

}