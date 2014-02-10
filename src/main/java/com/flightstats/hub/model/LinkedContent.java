package com.flightstats.hub.model;

import com.google.common.base.Optional;

public class LinkedContent {

	private final Content value;
	private final Optional<ContentKey> previous;
	private final Optional<ContentKey> next;

    /**
     * todo - gfm - 2/10/14 -
     * @deprecated
     */
	public LinkedContent(Content value, Optional<ContentKey> previous, Optional<ContentKey> next) {
		this.value = value;
		this.previous = previous;
		this.next = next;
	}

    public LinkedContent(Content value, ContentKey previous, ContentKey next) {
        this.value = value;
        this.previous = Optional.fromNullable(previous);
        this.next = Optional.fromNullable(next);
    }

	public Content getValue() {
		return value;
	}

	public Optional<String> getContentType() {
		return value.getContentType();
	}

	public int getDataLength() {
		return value.getDataLength();
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

	public Optional<ContentKey> getPrevious() {
		return previous;
	}

	public Optional<ContentKey> getNext() {
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

		LinkedContent that = (LinkedContent) o;

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
		return "LinkedContent{" +
				"value=" + value +
				", previous=" + previous +
				", next=" + next +
				'}';
	}
}
