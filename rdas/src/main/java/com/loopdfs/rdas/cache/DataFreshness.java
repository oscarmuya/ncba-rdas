package com.loopdfs.rdas.cache;

public enum DataFreshness {
	FRESH("fresh"),
	STALE("stale"),
	UNAVAILABLE("unavailable");

	private final String headerValue;

	DataFreshness(String headerValue) {
		this.headerValue = headerValue;
	}

	public String headerValue() {
		return headerValue;
	}
}
