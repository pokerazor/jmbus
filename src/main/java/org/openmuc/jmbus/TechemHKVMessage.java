/*
 * Copyright 2010-16 Fraunhofer ISE
 *
 * This file is part of jMBus.
 * For more information visit http://www.openmuc.org
 *
 * jMBus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * jMBus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with jMBus.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.openmuc.jmbus;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;

/**
 * 
 * Represents a Message of a Techem Heizkostenverteiler (heat cost allocator)
 *
 */
public class TechemHKVMessage extends WMBusMessage{

    private VariableDataStructure vdr;
    private final byte[] buffer;

	int ciField;
	String status="";
	Calendar lastDate=null;
	Calendar curDate=null;
	int lastVal=-1;
	int curVal=-1;
	float t1=-1;
	float t2=-1;
	byte[] historyBytes=new byte[27];
	String history="";
	
	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

	public TechemHKVMessage(WMBusMessage originalMessage){
		this(originalMessage.asBytes(),originalMessage.getRssi(),originalMessage.keyMap);
	}
    
    TechemHKVMessage(byte[] buffer, Integer signalStrengthInDBm, HashMap<String, byte[]> keyMap) {
        super(buffer,signalStrengthInDBm, keyMap);
    	this.buffer=buffer;
    }

    @Override
    public void decodeDeep() throws DecodingException {
        try {
        	super.decodeDeep();
        } catch (DecodingException e) {
        	int offset=10;
        	vdr=getVariableDataResponse();
        	
    		ciField=buffer[offset+0]  & 0xff;
    		
        	if (ciField ==  0xa0 && getSecondaryAddress().getManufacturerId().equals("TCH")	){
        		status=HexConverter.toShortHexString(buffer[offset+1]);
        		lastDate=parseLastDate(offset+2);
        		curDate=parseCurrentDate(offset+6);
        		lastVal=parseBigEndianInt(offset+4);
        		curVal=parseBigEndianInt(offset+8);
        		t1=parseTemp(offset+10);
        		t2=parseTemp(offset+12);
        		
        		System.arraycopy(buffer, 24, historyBytes, 0, 27);
        		history=HexConverter.toShortHexString(historyBytes);
        		
        	} else {
        		throw e;
        	}
        }   
    }
    
    int parseBigEndianInt(int i){
    	return (buffer[i] & 0xFF)+((buffer[i+1] & 0xFF)<<8);
    }
    
    float parseTemp(int i){    	
    	float tempint=parseBigEndianInt(i);
    	
    	return tempint/100;
    	//return String.format("%.2f", tempint / 100)+"�C";
    }
    
    private Calendar parseLastDate(int i){
    	int dateint=parseBigEndianInt(i);

        int day = (dateint >> 0) & 0x1F;
        int month = (dateint >> 5) & 0x0F;
        int year = (dateint >> 9) & 0x3F;
    	
//        return LocalDate.of(2000+year, month, day);
        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.YEAR, 2000+year);
        calendar.set(Calendar.MONTH, month-1);
        calendar.set(Calendar.DAY_OF_MONTH, day);

        return calendar;
    }
    
    private Calendar parseCurrentDate(int i){
    	int dateint=parseBigEndianInt(i);

        int day = (dateint >> 4) & 0x1F;
        int month = (dateint >> 9) & 0x0F;
//        int year = (dateint >> 13) & 0x07;
        Calendar calendar = new GregorianCalendar();
        calendar.set(calendar.get(Calendar.YEAR), month-1, day);
        return calendar;
//        return LocalDate.of( LocalDate.now().getYear(), month, day);
    }

    public String renderTechemFields() {
    	String s = "";
    	
		s+="Last Date: "+dateFormat.format(lastDate.getTime());
		s+=", Last Value: "+lastVal;
		
		s+=", Current Date: "+dateFormat.format(curDate.getTime());
		s+=", Current Value: "+curVal;

		s+=", T1: "+String.format("%.2f", t1)+"�C";
		s+=", T2: "+String.format("%.2f", t2)+"�C";

		s+=", History: "+ history;
    	return s;
    }
    
    @Override
    public String toString() {
    	
        StringBuilder builder = new StringBuilder();
        if (getVariableDataResponse()==null) {
            builder.append("Message has not been decoded. Bytes of this message: ");
            HexConverter.appendHexString(builder, buffer, 0, buffer.length);
            return builder.toString();
        } else {
            builder.append(new Date())
		            .append(";").append(getRssi())
		            .append(";").append(getControlField())
		            .append(";").append(getSecondaryAddress().getManufacturerId())
		            .append(";").append(getSecondaryAddress().getDeviceId())
		            .append(";").append(getSecondaryAddress().getVersion())
		            .append(";").append(getSecondaryAddress().getDeviceType())
		            .append(";").append(ciField)
		            .append(";").append(status)
		            .append(";").append(dateFormat.format(lastDate.getTime()))
		            .append(";").append(lastVal)
		            .append(";").append(dateFormat.format(curDate.getTime()))
		            .append(";").append(curVal)
		            .append(";").append(t1)
		            .append(";").append(t2)
		            .append(";").append(history);
            return builder.toString();
        }
    }
}