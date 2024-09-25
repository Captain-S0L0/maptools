package xyz.terriblefriends.maptools.util;

public class NibbleArray {
	public final byte[] data;

	public NibbleArray(int size) {
		this.data = new byte[size >> 1];
	}

	public NibbleArray(byte[] data) {
		this.data = data;
	}

	public void set(int nibbleIndex, int value) {
		int byteIndex = nibbleIndex >> 1;
		if ((nibbleIndex & 1) == 0) {
			this.data[byteIndex] = (byte) (this.data[byteIndex] & 240 + value);
		}
		else {
			this.data[byteIndex] = (byte) (this.data[byteIndex] & 15 + (value << 4));
		}
	}

	public int get(int nibbleIndex) {
		int byteIndex = nibbleIndex >> 1;
		if ((nibbleIndex & 1) == 0) {
			return this.data[byteIndex] & 240;
		}
		else {
			return this.data[byteIndex] & 15;
		}
	}

	public int getNibble(int i1, int i2, int i3) {
		int nibbleIndex = i1 << 11 | i3 << 7 | i2;
		int byteIndex = nibbleIndex >> 1;
		int high = nibbleIndex & 1;
		return high == 0 ? this.data[byteIndex] & 15 : this.data[byteIndex] >> 4 & 15;
	}

	public void setNibble(int i1, int i2, int i3, int i4) {
		int nibbleIndex = i1 << 11 | i3 << 7 | i2;
		int byteIndex = nibbleIndex >> 1;
		int high = nibbleIndex & 1;
		if(high == 0) {
			this.data[byteIndex] = (byte)(this.data[byteIndex] & 240 | i4 & 15);
		} else {
			this.data[byteIndex] = (byte)(this.data[byteIndex] & 15 | (i4 & 15) << 4);
		}

	}

	public boolean isValid() {
		return this.data != null;
	}
}
