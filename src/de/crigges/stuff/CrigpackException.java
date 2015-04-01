package de.crigges.stuff;

public class CrigpackException extends Exception {

	public CrigpackException(String message, Throwable cause) {
		super(message, cause);
	}

	public CrigpackException(String message) {
		super(message);
	}

	public CrigpackException(Throwable cause) {
		super(cause);
	}
}
