package nl.esciencecenter.esight.math;

import java.nio.FloatBuffer;

public abstract class VectorF extends Vector {
    protected float v[];
    private final FloatBuffer buf;

    protected VectorF(int size) {
        super(size, Type.FLOAT);
        v = new float[size];
        buf = FloatBuffer.wrap(v);
        buf.rewind();
    }

    /**
     * Retrieves the value of the vector at the given index.
     * 
     * @param i
     *            The index.
     * @return The value of the vector at index i.
     */
    public float get(int i) {
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
    public void set(int i, float u) {
        v[i] = u;
    }

    /**
     * Returns the flattened Array associated with this vector.
     * 
     * @return This matrix as a flat Array.
     */
    public float[] asArray() {
        return v;
    }

    /**
     * Returns the FloatBuffer associated with this vector.
     * 
     * @return This vector as a FloatBuffer.
     */
    public FloatBuffer asBuffer() {
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
            int val = Float.floatToIntBits(v[i]);
            int valHash = val ^ (val >>> 32);
            hashCode = 31 * hashCode + valHash;

        }
        return hashCode;
    }

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject)
            return true;
        if (!(thatObject instanceof VectorF))
            return false;

        // cast to native object is now safe
        VectorF that = (VectorF) thatObject;

        // now a proper field-by-field evaluation can be made
        boolean same = true;
        for (int i = 0; i < size; i++) {
            if (v[i] < that.v[i] - MatrixFMath.EPSILON
                    || v[i] > that.v[i] + MatrixFMath.EPSILON) {
                same = false;
            }
        }
        return same;
    }
}
