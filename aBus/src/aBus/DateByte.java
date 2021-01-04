package aBus;

public class DateByte{
    private byte day;
    private byte month;
    private short year;
    
    public DateByte() {
    	this.day = 0x00;
    	this.month = 0x00;
    	this.year = 0x00;
    }
    
    public DateByte(byte day, byte month, short year) {
    	this.day = day;
    	this.month = month;
    	this.year = year;
    }
    
    public byte getDay() {
    	return this.day;
    }
    
    public byte getMonth() {
    	return this.month;
    }
    
    public short getYear() {
    	return this.year;
    }
    
    public void update(byte day, byte month, short year) {
    	this.day = day;
    	this.month = month;
    	this.year = year;
    }
    
}
