package com.otisbean.keyring;

public class KeyringException extends Exception {

	private static final long serialVersionUID = 1L;

	public KeyringException() {
		super();
	}

	public KeyringException(String message) {
		super(message);
	}

	public KeyringException(Throwable cause) {
		super(cause);
	}

	public KeyringException(String message, Throwable cause) {
		super(message, cause);
	}

}
