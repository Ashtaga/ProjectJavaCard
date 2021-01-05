package aBus;

public class Debug {



	
	public static void main(String[] args) {
		DateByte lastTravelDate = new DateByte((byte)1,(byte)1,(short)2021);
		DateByte db = new DateByte((byte)6,(byte)2,(short)2021);
		short lastTravelTime = 0x00;
		short hour = (short)240;
		
		//Pour simplifier le calcul, on considère que les mois ont 31 jours.
    	int res ;
    	if(db.getYear() == lastTravelDate.getYear() && db.getMonth() == lastTravelDate.getMonth() && db.getDay() == lastTravelDate.getDay()) {
    		res = (short)(hour - lastTravelTime);
    	}else if(db.getYear() == lastTravelDate.getYear() && db.getMonth() == lastTravelDate.getMonth() && db.getDay() != lastTravelDate.getDay() ){
    		byte nbDay = (byte)(db.getDay() - lastTravelDate.getDay() - 1);
    		short nbMinute = (short)(nbDay * 24 * 60);
    		res = (short)( (1440 - lastTravelTime) + hour + nbMinute);
    	}else if(db.getYear() == lastTravelDate.getYear() && db.getMonth() != lastTravelDate.getMonth()){
    		byte nbMonth = (byte)(db.getMonth() - lastTravelDate.getMonth());
    		short nbDay = (short)((31 - lastTravelDate.getDay()) + db.getDay() + (nbMonth -1) * 31);
    		res = ( lastTravelTime + hour + nbDay * 24 * 60);
    	}else {
    		byte nbYear = (byte)(db.getYear() - lastTravelDate.getYear());
    		short nbDay = (short)(31 - lastTravelDate.getDay() + db.getDay() +  (12 - lastTravelDate.getMonth() + db.getMonth() - 1) * 31 + (nbYear - 1)* 12 * 31);
    		res = (lastTravelTime + hour + nbDay * 24 * 60);
    	}
    	
    	System.out.println("Res: "+res);
	}

}
