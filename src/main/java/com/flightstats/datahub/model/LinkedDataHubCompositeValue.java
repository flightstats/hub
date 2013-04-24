package com.flightstats.datahub.model;

import com.google.common.base.Optional;

public class LinkedDataHubCompositeValue {

	private final DataHubCompositeValue value;
	private final Optional<DataHubKey> previous;
	private final Optional<DataHubKey> next;

	public LinkedDataHubCompositeValue(DataHubCompositeValue value, Optional<DataHubKey> previous, Optional<DataHubKey> next) {
		this.value = value;
		this.previous = previous;
		this.next = next;
	}

	public DataHubCompositeValue getValue() {
		return value;
	}

	public String getContentType() {
		return value.getContentType();
	}

	public int getDataLength() {
		return value.getDataLength();
	}

	public int getContentTypeLength() {
		return value.getContentTypeLength();
	}

	public byte[] getData() {
		return value.getData();
	}

	public boolean hasPrevious() {
		return previous.isPresent();
	}

	public boolean hasNext() {
		return next.isPresent();
	}

	public Optional<DataHubKey> getPrevious() {
		return previous;
	}

	public Optional<DataHubKey> getNext() {
		return next;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		LinkedDataHubCompositeValue that = (LinkedDataHubCompositeValue) o;

		if (!next.equals(that.next)) {
			return false;
		}
		if (!previous.equals(that.previous)) {
			return false;
		}
		if (!value.equals(that.value)) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = value.hashCode();
		result = 31 * result + previous.hashCode();
		result = 31 * result + next.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "LinkedDataHubCompositeValue{" +
				"value=" + value +
				", previous=" + previous +
				", next=" + next +
				'}';
	}
}
