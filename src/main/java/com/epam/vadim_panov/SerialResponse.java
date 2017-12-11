package com.epam.vadim_panov;

import java.util.Optional;

/**
 * Created on 12/11/2017.
 */
public class SerialResponse {

	Long number;
	String error;

	public Optional<Long> getNumber() {
		return Optional.ofNullable(number);
	}

	public void setNumber(Long number) {
		this.number = number;
	}

	public String getError() {
		return error == null ? "Unknown error" : error;
	}

	public void setError(String error) {
		this.error = error;
	}


	public SerialResponse number(Long number) {
		this.number = number;
		return this;
	}

	public SerialResponse error(String error) {
		this.error = error;
		return this;
	}
}
