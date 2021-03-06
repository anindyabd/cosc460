package simpledb;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;

import simpledb.TupleDesc.TDItem;

/**
 * Tuple maintains information about the contents of a tuple. Tuples have a
 * specified schema specified by a TupleDesc object and contain Field objects
 * with the data for each field.
 */
public class Tuple implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private Field[] fieldlist;
    
    private TupleDesc td;    
    
    private RecordId rid;
    	
    
    /**
     * Create a new tuple with the specified schema (type).
     *
     * @param td the schema of this tuple. It must be a valid TupleDesc
     *           instance with at least one field.
     */
    
    public Tuple(TupleDesc td) {
    	this.td = td;
        this.fieldlist = new Field[td.numFields()];
        Iterator<TDItem> it = td.iterator();
        int i = 0;
        while (it.hasNext()){
        	if (td.getFieldType(i).equals(Type.INT_TYPE)){
        		fieldlist[i] = new IntField(-1);
        	}
        	else {
        		fieldlist[i] = new StringField(null, td.getFieldType(i).getLen());
        	}
        	i++;
        	it.next();
        }
    }

    /**
     * @return The TupleDesc representing the schema of this tuple.
     */
    public TupleDesc getTupleDesc() {
        return this.td;
    }

    /**
     * @return The RecordId representing the location of this tuple on disk. May
     * be null.
     */
    public RecordId getRecordId() {
        return rid;
    }

    /**
     * Set the RecordId information for this tuple.
     *
     * @param rid the new RecordId for this tuple.
     */
    public void setRecordId(RecordId rid) {
        this.rid = rid;
    }

    /**
     * Change the value of the ith field of this tuple.
     *
     * @param i index of the field to change. It must be a valid index.
     * @param f new value for the field.
     */
    public void setField(int i, Field f) {
    	if (f.getType().equals(Type.INT_TYPE) && fieldlist[i].getType().equals(Type.INT_TYPE)){
    		IntField myf = (IntField)f;
    		fieldlist[i] = new IntField(myf.getValue());
    	}
    	else if (f.getType().equals(Type.STRING_TYPE) && fieldlist[i].getType().equals(Type.STRING_TYPE)){
    		StringField myf = (StringField)f;
    		fieldlist[i] = new StringField(myf.getValue(), myf.getType().getLen());
    	}
    	else throw new RuntimeException();
    }

    /**
     * @param i field index to return. Must be a valid index.
     * @return the value of the ith field, or null if it has not been set.
     */
    public Field getField(int i) {
        return fieldlist[i];
    }

    /**
     * Returns the contents of this Tuple as a string. Note that to pass the
     * system tests, the format needs to be as follows:
     * <p/>
     * column1\tcolumn2\tcolumn3\t...\tcolumnN
     * <p/>
     * where \t is any whitespace, except newline
     */
    public String toString() {
       String toreturn ="";
    	for (int i = 0; i < fieldlist.length; i++){
    	   if (fieldlist[i].getType().equals(Type.INT_TYPE)){
    		IntField intfield = (IntField)fieldlist[i];
    		toreturn += intfield.getValue() + " ";
    	   }
    	   else {
    		   StringField stringfield = (StringField)fieldlist[i];
       		   toreturn += stringfield.getValue() + " ";
    	   }
       }
    	return toreturn;
    }

}
