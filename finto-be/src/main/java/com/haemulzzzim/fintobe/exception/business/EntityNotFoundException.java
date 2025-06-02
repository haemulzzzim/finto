package com.haemulzzzim.fintobe.exception.business;

import com.haemulzzzim.fintobe.exception.BusinessException;
import com.haemulzzzim.fintobe.exception.ErrorCode;

public class EntityNotFoundException extends BusinessException {

	public EntityNotFoundException(String message, ErrorCode errorCode) {
		super(message, errorCode);
	}

}