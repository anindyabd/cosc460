package simpledb;

import java.io.Serializable;
import java.util.*;

/**
 * TupleDesc describes the schema of a tuple.
 */
public class TupleDesc implements Serializable {

    /**
     * A help class to facilitate organizing the information of each field
     */
    public static class TDItem implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * The type of the field
         */
        public final Type fieldType;

        /**
         * The name of the field
         */
        public final String fieldName;

        public TDItem(Type t, String n) {
            this.fieldName = n;
            this.fieldType = t;
        }

        public String toString() {
            return fieldName + "(" + fieldType + ")";
        }
    }

    private static final long serialVersionUID = 1L;
    
    private TDItem[] tdlist;

    /**
     * Create a new TupleDesc with typeAr.length fields with fields of the
     * specified types, with associated named fields.
     *
     * @param typeAr  array specifying the number of and types of fields in this
     *                TupleDesc. It must contain at least one entry.
     * @param fieldAr array specifying the names of the fields. Note that names may
     *                be null.
     */
    public TupleDesc(Type[] typeAr, String[] fieldAr) {
    	this.tdlist = new TDItem[typeAr.length]; 
        for (int i = 0; i < tdlist.length; i++){
        	tdlist[i] = new TDItem(typeAr[i], fieldAr[i]);
        }
    }

    /**
     * Constructor. Create a new tuple desc with typeAr.length fields with
     * fields of the specified types, with anonymous (unnamed) fields.
     *
     * @param typeAr array specifying the number of and types of fields in this
     *               TupleDesc. It must contain at least one entry.
     */
    public TupleDesc(Type[] typeAr) {
        this.tdlist = new TDItem[typeAr.length];
        for (int i = 0; i < tdlist.length; i++){
        	tdlist[i] = new TDItem(typeAr[i], null);
        }
    }

    /**
     * @return the number of fields in this TupleDesc
     */
    public int numFields() {
        return tdlist.length;
    }

    /**
     * Gets the (possibly null) field name of the ith field of this TupleDesc.
     *
     * @param i index of the field name to return. It must be a valid index.
     * @return the name of the ith field
     * @throws NoSuchElementException if i is not a valid field reference.
     */
    public String getFieldName(int i) throws NoSuchElementException {
        if (i >= tdlist.length) {
        	throw new NoSuchElementException();
        }
        return tdlist[i].fieldName;
    }

    /**
     * Gets the type of the ith field of this TupleDesc.
     *
     * @param i The index of the field to get the type of. It must be a valid
     *          index.
     * @return the type of the ith field
     * @throws NoSuchElementException if i is not a valid field reference.
     */
    public Type getFieldType(int i) throws NoSuchElementException {
    	if (i >= tdlist.length) {
        	throw new NoSuchElementException();
        }
        if (tdlist[i].fieldType == null) {
        	throw new NoSuchElementException();
        }
        return tdlist[i].fieldType;
    }

    /**
     * Find the index of the field with a given name.
     *
     * @param name name of the field.
     * @return the index of the field that is first to have the given name.
     * @throws NoSuchElementException if no field with a matching name is found.
     */
    public int fieldNameToIndex(String name) throws NoSuchElementException {
    	int index = -1;
    	for (int i = 0; i < this.numFields(); i++){
        	if (this.getFieldName(i)!= null && this.getFieldName(i).equals(name)){
        		index = i;
        		break;
        	}
        }
    	if (index == -1){
    		throw new NoSuchElementException();
    	}
        return index;
    }

    /**
     * @return The size (in bytes) of tuples corresponding to this TupleDesc.
     * Note that tuples from a given TupleDesc are of a fixed size.
     */
    public int getSize() {
    	int size = 0;
        for (int i = 0; i < this.numFields(); i++){
        	size += this.getFieldType(i).getLen();
        }
        return size;
    }

    /**
     * Merge two TupleDescs into one, with td1.numFields + td2.numFields fields,
     * with the first td1.numFields coming from td1 and the remaining from td2.
     *
     * @param td1 The TupleDesc with the first fields of the new TupleDesc
     * @param td2 The TupleDesc with the last fields of the TupleDesc
     * @return the new TupleDesc
     */
    public static TupleDesc merge(TupleDesc td1, TupleDesc td2) {
    	int new_length = td1.numFields() + td2.numFields();
    	Type[] typeAr = new Type[new_length]; 
    	String[] fieldAr = new String[new_length];
    	for (int i = 0; i < td1.numFields(); i++){
    		typeAr[i] = td1.getFieldType(i);
    		fieldAr[i] = td1.getFieldName(i);
    	}
    	for (int i = td1.numFields(), j = 0; i < new_length; i++, j++){
    		typeAr[i] = td2.getFieldType(j);
    		fieldAr[i] = td2.getFieldName(j);
    	}
    	TupleDesc newtd = new TupleDesc(typeAr, fieldAr);
        return newtd;
    }

    /**
     * Compares the specified object with this TupleDesc for equality. Two
     * TupleDescs are considered equal if they are the same size and if the n-th
     * type in this TupleDesc is equal to the n-th type in td.
     *
     * @param o the Object to be compared for equality with this TupleDesc.
     * @return true if the object is equal to this TupleDesc.
     */
    public boolean equals(Object o) {
        if ((o instanceof TupleDesc) == false) {
        	return false;
        }
        else {
        	TupleDesc othertd = (TupleDesc)o;
        	if (othertd.getSize() != this.getSize()){
        		return false;
        	}
        	else {
        		for (int i = 0; i < this.numFields(); i++){
        			if (this.getFieldType(i) != (othertd.getFieldType(i))) {
        				return false;
        			}
        		}
        	}
        }
        return true;
    }

    public int hashCode() {
        // If you want to use TupleDesc as keys for HashMap, implement this so
        // that equal objects have equals hashCode() results
        throw new UnsupportedOperationException("unimplemented");
    }

    /**
     * Returns a String describing this descriptor. It should be of the form
     * "fieldName[0](fieldType[0]), ..., fieldName[M](fieldType[M])"
     *
     * @return String describing this descriptor.
     */
    public String toString() {
    	String outputstring = "";
        for (int i = 0; i < this.numFields(); i++){
        	outputstring += this.getFieldName(i) + "(" + this.getFieldType(i) + "), ";
        }
        if (outputstring.substring(outputstring.length()-2, outputstring.length()).equals(", ")){
        	outputstring = outputstring.substring(0, outputstring.length() - 2);
        }
        return outputstring;
    }

    /**
     * @return An iterator which iterates over all the field TDItems
     * that are included in this TupleDesc
     */
    public Iterator<TDItem> iterator() {
        // some code goes here
        return Arrays.asList(tdlist).iterator();
    }

}
