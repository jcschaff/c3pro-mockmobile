package edu.uconn.c3pro.mockmobile;

public class Registration {
	private boolean sandbox;
	private String receipt_data;

	public Registration(boolean sandbox, String receipt_data) {
		this.sandbox = sandbox;
	}

	public boolean getSandbox() {
		return sandbox;
	}

	public void setId(boolean sandbox) {
		this.sandbox = sandbox;
	}

	public String getContent() {
		return receipt_data;
	}

	public void setContent(String receipt_data) {
		this.receipt_data = receipt_data;
	}

}
