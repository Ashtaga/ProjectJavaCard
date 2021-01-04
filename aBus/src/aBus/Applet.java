package aBus;

import javacard.framework.APDU;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.OwnerPIN;

public class Applet extends javacard.framework.Applet {
	// On d�finit des constantes pour faciliter l'�criture des programmes

    // Constante pour le CLA
    final static byte MON_CLA = (byte) 0x25;

    // Une constante par INS, c'est � dire par commande
    final static byte INS_BUY_TRAVEL = (byte) 0x00;
    final static byte INS_RELOAD_CARD = (byte) 0x01;
    final static byte INS_CONSULT_CARD = (byte) 0x02;
    final static byte INS_UNLOCK_PIN = (byte) 0x03;
    final static byte INS_CHECK_VALIDITY = (byte) 0x04;
    final static byte INS_CHECK_LOGS = (byte) 0x05;

    // Constantes de status word error
    final static byte SW_INSUFFICIENT_BALANCE_ERROR = (byte) 0x6300;

    // Autres constantes (constantes fonctionnelles)
    final static byte MAX_BALANCE = (byte) 0x1E;//30
    final static byte MAX_SIZE_RELOADING_AMOUNT = (byte) 0x05;//5
    final static byte TRAVEL_VALIDITY_TIME = (byte) 0x3C;//60
    
    final static byte PIN_TRY_LIMIT = (byte) 0x03;
    final static byte MAX_PIN_SIZE = (byte) 0x06;
    
    final static byte PUK_TRY_LIMIT = (byte) 0x03;
    final static byte MAX_PUK_SIZE = (byte) 0x06;
    
    private byte balance;
    private OwnerPIN PIN;
    private OwnerPIN PUK;
    private DateByte lastTravelDate;
    private short lastTravelTime;
    

    private Applet(byte[] bArray, short bOffset, byte bLength) {
        /*PIN = new OwnerPIN(PIN_TRY_LIMIT, MAX_PIN_SIZE);
        PUK = new OwnerPIN(PUK_TRY_LIMIT, MAX_PUK_SIZE);
        //TODO: Generation code PIN + PUK
        byte iLen = bArray[bOffset];
        bOffset = (short) (bOffset + iLen + 1);
        byte cLen = bArray[bOffset];
        bOffset = (short) (bOffset + cLen + 1);
        byte aLen = bArray[bOffset];
        PIN.update(bArray, (short) (bOffset + 1), aLen);
        
        iLen = bArray[bOffset];
        bOffset = (short) (bOffset + iLen + 1);
        cLen = bArray[bOffset];
        bOffset = (short) (bOffset + cLen + 1);
        aLen = bArray[bOffset];
        PUK.update(bArray, (short) (bOffset + 1), aLen);*/
        
        balance = 0x00;
        lastTravelDate = new DateByte();
        lastTravelTime = 0x00;
        register();
    }

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new Applet(bArray, bOffset, bLength);
    }

    public boolean select() {
    	return true;
    }

    private short getLastTravelDateInMinute(DateByte db, short hour)  {
    	//Pour simplifier le calcul, on considère que les mois ont 31 jours.
    	short res = (short)0;
    	if(db.getYear() == lastTravelDate.getYear() && db.getMonth() == lastTravelDate.getMonth() && db.getDay() == lastTravelDate.getDay()) {
    		res = (short)(hour - lastTravelTime);
    	}else if(db.getYear() == lastTravelDate.getYear() && db.getMonth() == lastTravelDate.getMonth() && db.getDay() != lastTravelDate.getDay() ){
    		byte nbDay = (byte)(db.getDay() - lastTravelDate.getDay());
    		short nbMinute = (short)(nbDay * 24 * 60);
    		res = (short)(1440 - lastTravelTime + hour + nbMinute);
    	}else if(db.getYear() == lastTravelDate.getYear() && db.getMonth() != lastTravelDate.getMonth()){
    		byte nbMonth = (byte)(db.getMonth() - lastTravelDate.getMonth());
    		short nbDay = (short)(31 - lastTravelDate.getDay() + db.getDay() + (nbMonth - 1) * 31);
    		res = (short)(1440 - lastTravelTime + hour + nbDay * 24 * 60);
    	}else {
    		byte nbYear = (byte)(db.getYear() - lastTravelDate.getYear());
    		short nbDay = (short)(31 - lastTravelDate.getDay() + db.getDay() + (12 - lastTravelDate.getMonth() + db.getMonth()) * 31 + (nbYear - 1)* 12 * 31);
    		res = (short)(1440 - lastTravelTime + hour + nbDay * 24 * 60);
    	}
    	return res;
    }
    
    public void process(APDU apdu) throws ISOException {
		byte[] buffer = apdu.getBuffer();
		if (buffer[ISO7816.OFFSET_CLA] == MON_CLA) {
			switch(buffer[ISO7816.OFFSET_INS]) {
		    	case INS_BUY_TRAVEL: 
		    		//TODO: Get Last Travel Time and date
					if(getLastTravelDateInMinute(new DateByte((byte)1,(byte)1,(short)2021),(short)628)<60) {
						//Voyage valide
						//TODO: Detecter si changement de ligne + envoyer message
					}else{
						//Voyage non valide
						if(balance > ((byte)0x00)) {
							--balance;
							//TODO: Update Date and Time
							lastTravelDate.update((byte)0x00,(byte)0x00,(short)0x00);
							lastTravelTime = 0x628;
						}else{
							ISOException.throwIt(SW_INSUFFICIENT_BALANCE_ERROR);
						}
					}
			    break;
			    case INS_RELOAD_CARD: 
			    break;
			    case INS_CONSULT_CARD: 
			    	buffer[0] = balance;
					apdu.setOutgoingAndSend((short) 0, (short) 1);
				break;
			    case INS_UNLOCK_PIN: 
				break;
			    case INS_CHECK_VALIDITY: 
				break;
			    case INS_CHECK_LOGS: 
				break;
			    default:
			    	ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		    }
		}else{
		    ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
		}
    }
 
}