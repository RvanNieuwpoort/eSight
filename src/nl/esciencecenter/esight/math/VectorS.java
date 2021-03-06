package nl.esciencecenter.esight.math;

import java.nio.ShortBuffer;

public abstract class VectorS extends Vector {
    protected short v[];
    private final ShortBuffer buf;

    protected VectorS(int size) {
        super(size, Type.SHORT);
        v = new short[size];
        buf = ShortBuffer.wrap(v);
        buf.rewind();
    }

    /**
     * Retrieves the value of the vector at the given index.
     * 
     * @param i
     *            The index.
     * @return The value of the vector at index i.
     */
    public short get(int i) {
        return v[i];
    }

    /**
     * Sets the value of the vector at the given index.
     * 
     * @param i
     *            The index.
     * @param u
     *            The new value.
     */
    public void set(int i, short u) {
        v[i] = u;
    }

    /**
     * Returns the flattened Array associated with this vector.
     * 
     * @return This matrix as a flat Array.
     */
    public short[] asArray() {
        return v;
    }

    /**
     * Returns the ShortBuffer associated with this vector.
     * 
     * @return This vector as a ShortBuffer.
     */
    public ShortBuffer asBuffer() {
        buf.rewind();
        return buf;
    }

    @Override
    public String toString() {
        String result = "";
        for (int i = 0; i < v.length; i++) {
            result += (v[i] + " ");
        }

        return result;
    }

    @Override
    public int hashCode() {
        int cols = size;

        int hashCode = 1;
        for (int i = 0; i < cols; ++i) {
            int val = v[i];
            int valHash = val ^ (val >>> 32);
            hashCode = 31 * hashCode + valHash;

        }
        return hashCode;
    }

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject)
            return true;
        if (!(thatObject instanceof VectorS))
            return false;

        // cast to native object is now safe
        VectorS that = (VectorS) thatObject;

        // now a proper field-by-field evaluation can be made
        boolean same = true;
        for (int i = 0; i < size; i++) {
            if (v[i] != that.v[i]) {
                same = false;
            }
        }
        return same;
    }
}
