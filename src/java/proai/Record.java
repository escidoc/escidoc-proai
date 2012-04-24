package proai;


public interface Record {

    public String getItemID();

    public String getPrefix();

    /**
     * Get a string that can be used to construct the XML of the record.
     *
     * The format of this string is defined by the implementation.
     *
     * The string will typically contain some kind of identifier or locator 
     * (a file path or URL) and possibly, other attributes that may be used 
     * to construct a record's XML.
     */
    public String getSourceInfo();

}
