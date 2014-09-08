package org.bytesoft.bytejta.logger.store;

public class SimpleResourcePosition {
	private int index;
	private int position;
	// private int length;
	private boolean enabled;
	private transient String identifier;

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	// public int getLength() {
	// return length;
	// }
	//
	// public void setLength(int length) {
	// this.length = length;
	// }

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

}
