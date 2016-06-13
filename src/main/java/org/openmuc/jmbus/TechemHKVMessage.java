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

import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;

/**
 * 
 * Represents a Techem Heizkostenverteiler Message
 * 
 *
 */
public class TechemHKVMessage extends WMBusMessage{

    private VariableDataStructure vdr;
    private final byte[] buffer;

	int ciField;
	String status="";
	LocalDate lastDate=null;
	LocalDate curDate=null;
	int lastVal=-1;
	int curVal=-1;
	float t1=-1;
	float t2=-1;
	byte[] historyBytes=new byte[27];
	String history="";

	
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
    	//return String.format("%.2f", tempint / 100)+"°C";
    }
    
    private LocalDate parseLastDate(int i){
    	int dateint=parseBigEndianInt(i);

        int day = (dateint >> 0) & 0x1F;
        int month = (dateint >> 5) & 0x0F;
        int year = (dateint >> 9) & 0x3F;
    	
        return LocalDate.of(2000+year, month, day);
    }
    
    private LocalDate parseCurrentDate(int i){
    	int dateint=parseBigEndianInt(i);

        int day = (dateint >> 4) & 0x1F;
        int month = (dateint >> 9) & 0x0F;
//        int year = (dateint >> 13) & 0x07;
        return LocalDate.of( LocalDate.now().getYear(), month, day);
    }

    public String renderTechemFields() {
    	String s = "";
    	
		s+="Last Date: "+lastDate;
		s+=", Last Value: "+lastVal;
		
		s+=", Current Date: "+curDate;
		s+=", Current Value: "+curVal;

		s+=", T1: "+String.format("%.2f", t1)+"°C";
		s+=", T2: "+String.format("%.2f", t2)+"°C";

		s+=", History: "+ history;
    	return s;
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (getRssi() != null) {
       //     builder.append("Message was received with signal strength: ").append(getRssi()).append("dBm\n");
        }
        if (getVariableDataResponse()==null) {
            builder.append("Message has not been decoded. Bytes of this message:\n");
            HexConverter.appendHexString(builder, buffer, 0, buffer.length);
            return builder.toString();
        } else {
      //      builder.append("control field: ");
            HexConverter.appendHexString(getControlField(), builder);
     //       builder.append("\nSecondary Address -> ")
       //             .append(getSecondaryAddress())
                    //.append("\nVariable Data Response:\n")
                    //.append(vdr)
        //            .append("\nMessage is Techem Heizkörperverteiler Message\n")
           //         .append(renderTechemFields());

//            return builder.toString();
            return renderCSV();

        }
    }
    
    String renderCSV(){
        StringBuilder builder = new StringBuilder();
        builder.append(new Date())
               .append(";").append(getRssi())
               .append(";").append(getControlField())
               .append(";").append(getSecondaryAddress().getManufacturerId())
               .append(";").append(getSecondaryAddress().getDeviceId())
               .append(";").append(getSecondaryAddress().getVersion())
               .append(";").append(getSecondaryAddress().getDeviceType())
               .append(";").append(lastDate)
               .append(";").append(lastVal)
               .append(";").append(curDate)
               .append(";").append(curVal)
               .append(";").append(t1)
               .append(";").append(t2)
               .append(";").append(history);

        return builder.toString();
    }

}
