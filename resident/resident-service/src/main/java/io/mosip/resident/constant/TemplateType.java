package io.mosip.resident.constant;

public enum TemplateType {
	
	REQUEST_RECEIVED("request-received"),
	SUCCESS("success"),
	FAILURE("failure");

	private String tempType;

	TemplateType(String tempType) {
		this.tempType = tempType;
	}
	
	public String getTempType() {
		return tempType;
	}

}
