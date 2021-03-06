package simpledb;

import java.util.*;
import simpledb.TupleDesc.TDItem;

/**
 * SeqScan is an implementation of a sequential scan access method that reads
 * each tuple of a table in no particular order (e.g., as they are laid out on
 * disk).
 */
public class SeqScan implements DbIterator {

	
    private static final long serialVersionUID = 1L;
    
    private int tableid;
    private TransactionId tid;
    private String tableAlias;
    private DbFileIterator fileiterator;
    private DbFile file;
    /**
     * Creates a sequential scan over the specified table as a part of the
     * specified transaction.
     *
     * @param tid        The transaction this scan is running as a part of.
     * @param tableid    the table to scan.
     * @param tableAlias the alias of this table (needed by the parser); the returned
     *                   tupleDesc should have fields with name tableAlias.fieldName
     *                   (note: this class is not responsible for handling a case where
     *                   tableAlias or fieldName are null. It shouldn't crash if they
     *                   are, but the resulting name can be null.fieldName,
     *                   tableAlias.null, or null.null).
     */
    public SeqScan(TransactionId tid, int tableid, String tableAlias) {
        this.tid = tid;
        this.tableid = tableid;
        this.tableAlias = tableAlias;
        this.file = (DbFile) Database.getCatalog().getDatabaseFile(tableid);
    }

    /**
     * @return return the table name of the table the operator scans. This should
     * be the actual name of the table in the catalog of the database
     */
    public String getTableName() {
        Catalog catalog = Database.getCatalog();
        return catalog.getTableName(tableid);
    }

    /**
     * @return Return the alias of the table this operator scans.
     */
    public String getAlias() {
        return tableAlias;
    }

    public SeqScan(TransactionId tid, int tableid) {
        this(tid, tableid, Database.getCatalog().getTableName(tableid));
    }

    public void open() throws DbException, TransactionAbortedException {
    	fileiterator = file.iterator(tid);
    	fileiterator.open();
    }

    /**
     * Returns the TupleDesc with field names from the underlying HeapFile,
     * prefixed with the tableAlias string from the constructor. This prefix
     * becomes useful when joining tables containing a field(s) with the same
     * name.
     *
     * @return the TupleDesc with field names from the underlying HeapFile,
     * prefixed with the tableAlias string from the constructor.
     */
    public TupleDesc getTupleDesc() {
    	Catalog catalog = Database.getCatalog();
        TupleDesc td = catalog.getTupleDesc(tableid);
        Type[] types = new Type[td.numFields()];
        String[] names = new String[td.numFields()];
        int i = 0;
        Iterator<TDItem> iterator = td.iterator();
        while (iterator.hasNext()){
        	iterator.next();
        	types[i] = td.getFieldType(i);
        	names[i] = td.getFieldName(i);
        	i++;
        }
        String[] newnames = new String[names.length];
        for (int j = 0; j < names.length; j++){
        	newnames[j] = tableAlias + "." + names[j]; 
        }
        TupleDesc newtd = new TupleDesc(types, newnames);
        return newtd;
    }

    public boolean hasNext() throws TransactionAbortedException, DbException {
        return fileiterator.hasNext();
    }

    public Tuple next() throws NoSuchElementException,
            TransactionAbortedException, DbException {
        return fileiterator.next();
    }

    public void close() {
        fileiterator.close();
    }

    public void rewind() throws DbException, NoSuchElementException,
            TransactionAbortedException {
        fileiterator.rewind();
    }
}
