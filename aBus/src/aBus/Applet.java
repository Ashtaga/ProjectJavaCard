package aBus;

import javacard.framework.APDU;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.OwnerPIN;
//TODO : validation ticket : ajouter numéro de ligne et direction 
//dévérouillage code pin avec code puk
//journalisation des données
//garder historique en mémoire de la carte
//ownerPIN pour le code puk
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
    final static byte INS_GET_TIME_EXPIRED = (byte) 0x06;
    final static byte INS_INITIALISE_CARD = (byte) 0x07;
    
    // Constantes de status word error
    final static byte SW_TRAVEL_TIME_EXPIRED = (byte) 0x6301;
    final static byte SW_TRAVEL_ALREADY_VALIDATED = (byte) 0x6302;
    final static byte SW_INSUFFICIENT_BALANCE_ERROR = (byte) 0x6303;
    final static byte SW_INVALID_AMOUNT = (byte) 0x6304;
    final static byte SW_INVALID_PIN = (byte) 0x6305;
    final static byte SW_INVALID_PUK = (byte) 0x6306;
    final static byte SW_BLOCKED_PIN = (byte) 0x6307;
    
 // Constantes de status word error for lifecycle
	public static final short SW_CARD_ALREADY_INITIALIZED = 0x6400;
	public static final short SW_CARD_NOT_INITIALIZED = 0x6401;
	public static final short SW_CARD_BLOCKED = 0x6402;
	public static final short SW_CARD_DEAD = 0x6403;
	public static final short SW_CARD_ALREADY_UNLOCK = 0x6404;
	
	// For the lifeCycleState
 	public static final byte PRE_PERSO = (byte) 0x00;
 	public static final byte USE = (byte) 0x01;
 	public static final byte BLOCKED = (byte) 0x02;
 	public static final byte DEAD = (byte) 0x03;
    
 	//Arguments
	public static final short P1_VERIFY_PIN = (byte)0x01;
	public static final short P1_RELOAD = (byte)0x02;
	public static final short P1_VERIFY_PUK = (byte) 0x03;
 		
    // Autres constantes (constantes fonctionnelles)
    final static byte MAX_BALANCE = (byte) 0x1E;//30
    final static byte MAX_SIZE_RELOADING_AMOUNT = (byte) 0x05;//5
    final static byte TRAVEL_VALIDITY_TIME = (byte)0x3C;//60
    
    final static byte PIN_TRY_LIMIT = (byte) 0x03;
    final static byte MAX_PIN_SIZE = (byte) 0x04;
    
    final static byte PUK_TRY_LIMIT = (byte) 0x03;
    final static byte MAX_PUK_SIZE = (byte) 0x04;
    
    private byte lifeCycleState;
    private byte balance;
    private OwnerPIN PIN;
    private OwnerPIN PUK;
    private DateByte lastTravelDate;
    private short lastTravelTime;
    private short controlHour;
	private short validity;
	private DateByte controlDate;
    

    private Applet(byte[] bArray, short bOffset, byte bLength) {
        PIN = new OwnerPIN(PIN_TRY_LIMIT, MAX_PIN_SIZE);
        PUK = new OwnerPIN(PUK_TRY_LIMIT, MAX_PUK_SIZE);

    	lifeCycleState = PRE_PERSO;
        balance = 0x00;
        lastTravelDate = new DateByte((byte)0x01,(byte)0x01,(short)2021);
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
    		byte nbDay = (byte)(db.getDay() - lastTravelDate.getDay() - 1);
    		short nbMinute = (short)(nbDay * 24 * 60);
    		res = (short)( (1440 - lastTravelTime) + hour + nbMinute);
    	}else if(db.getYear() == lastTravelDate.getYear() && db.getMonth() != lastTravelDate.getMonth()){
    		byte nbMonth = (byte)(db.getMonth() - lastTravelDate.getMonth());
    		short nbDay = (short)((31 - lastTravelDate.getDay()) + db.getDay() + (nbMonth -1) * 31);
    		res = (short)( lastTravelTime + hour + nbDay * 24 * 60);
    	}else {
    		byte nbYear = (byte)(db.getYear() - lastTravelDate.getYear());
    		short nbDay = (short)(31 - lastTravelDate.getDay() + db.getDay() +  (12 - lastTravelDate.getMonth() + db.getMonth() - 1) * 31 + (nbYear - 1)* 12 * 31);
    		res = (short)(lastTravelTime + hour + nbDay * 24 * 60);
    	}
    	if(res < 0) {//Depassement du short
    		res = (short)32766;//Valeur max
    	}
    	return res;
    }
    
    public void process(APDU apdu) throws ISOException {
		byte[] buffer = apdu.getBuffer();
		if (buffer[ISO7816.OFFSET_CLA] == MON_CLA) {
			if(lifeCycleState != DEAD) {
				switch(buffer[ISO7816.OFFSET_INS]) {
			    	case INS_BUY_TRAVEL: 
			    		apdu.setIncomingAndReceive();
						byte a = buffer[ISO7816.OFFSET_CDATA];
						byte b = buffer[ISO7816.OFFSET_CDATA + 1];
						byte c = buffer[ISO7816.OFFSET_CDATA + 2];
						byte d = buffer[ISO7816.OFFSET_CDATA + 3];
						byte e = buffer[ISO7816.OFFSET_CDATA + 4];
						byte f = buffer[ISO7816.OFFSET_CDATA + 5];
						DateByte buyDate = new DateByte((byte)(a),(byte)(b),(short)(((d & 0xFF) << 8) | (c & 0xFF)));
						short buyHour = (short)(((f) << 8) | (e & 0xFF));
			    		if(lifeCycleState == PRE_PERSO) {
				    		ISOException.throwIt(SW_CARD_NOT_INITIALIZED);
				    	}else if(lifeCycleState == BLOCKED) {
				    		ISOException.throwIt(SW_CARD_BLOCKED);
				    	}else {
							if(getLastTravelDateInMinute(buyDate, buyHour) < TRAVEL_VALIDITY_TIME ) {
								ISOException.throwIt(SW_TRAVEL_ALREADY_VALIDATED);
								//TODO: Detecter si changement de ligne + envoyer message
							}else{
								//Voyage non valide
								if(balance > ((byte)0x00)) {
									--balance;
									lastTravelDate.update(buyDate);
									lastTravelTime = buyHour;
								}else{
									ISOException.throwIt(SW_INSUFFICIENT_BALANCE_ERROR);
								}
							}
				    	}
				    break;
				    case INS_RELOAD_CARD: 
				    	if(lifeCycleState == PRE_PERSO) {
				    		ISOException.throwIt(SW_CARD_NOT_INITIALIZED);
				    	}else if(lifeCycleState == BLOCKED) {
				    		ISOException.throwIt(SW_CARD_BLOCKED);
				    	}else {
				    		apdu.setIncomingAndReceive();
				    		switch (buffer[ISO7816.OFFSET_P1]) {
							case P1_VERIFY_PIN:
								if(PIN.check(buffer, ISO7816.OFFSET_CDATA, MAX_PIN_SIZE)) {
									apdu.setOutgoingAndSend((short) 0, (short) 0);
								}else {
									if(PIN.getTriesRemaining() == (byte)0) {
										lifeCycleState = BLOCKED;
										ISOException.throwIt(SW_BLOCKED_PIN);
									}else {
										ISOException.throwIt(SW_INVALID_PIN);
									}
								}
							break;
							case P1_RELOAD:
								if(PIN.isValidated()) {
									short amount = buffer[ISO7816.OFFSET_CDATA];
									PIN.reset();
						    		if(amount <= MAX_SIZE_RELOADING_AMOUNT && amount >= 0) {
						    			balance += (byte)(amount);
						    			apdu.setOutgoingAndSend((short) 0, (short) 0);
						    		}else {
						    			ISOException.throwIt(SW_INVALID_AMOUNT);
						    		}
								}
							break;
				    	}
				    	}
				    break;
				    case INS_CONSULT_CARD: 
				    	if(lifeCycleState == PRE_PERSO) {
				    		ISOException.throwIt(SW_CARD_NOT_INITIALIZED);
				    	}else if(lifeCycleState == BLOCKED) {
				    		ISOException.throwIt(SW_CARD_BLOCKED);
				    	}else {
				    		buffer[0] = balance;
							apdu.setOutgoingAndSend((short) 0, (short) 1);
				    	}
					break;
				    case INS_UNLOCK_PIN: 
				    	
				    	if(lifeCycleState == PRE_PERSO) {
				    		ISOException.throwIt(SW_CARD_NOT_INITIALIZED);
				    	}else if(lifeCycleState == USE) {
				    		ISOException.throwIt(SW_CARD_ALREADY_UNLOCK);
				    	}else {
				    		apdu.setIncomingAndReceive();
				    		if(buffer[ISO7816.OFFSET_P1] == P1_VERIFY_PUK)
				    		{
				    			if (this.lifeCycleState != BLOCKED) {
				                    ISOException.throwIt(ISO7816.SW_COMMAND_NOT_ALLOWED);
				    			}
				                	if(PUK.check(buffer, ISO7816.OFFSET_CDATA, MAX_PUK_SIZE)) {
				                		PIN.resetAndUnblock();
				                		PUK.reset();
				                		lifeCycleState = USE;
										apdu.setOutgoingAndSend((short) 0, (short) 0);
									}else {
										if(PUK.getTriesRemaining() == (byte)0) {
											lifeCycleState = DEAD;
											ISOException.throwIt(SW_CARD_DEAD);
										}else {
											ISOException.throwIt(SW_INVALID_PUK);
										}
									}
				    			}
				           }
					break;
				    case INS_CHECK_VALIDITY: 
				    	if(lifeCycleState == PRE_PERSO) {
				    		ISOException.throwIt(SW_CARD_NOT_INITIALIZED);
				    	}else if(lifeCycleState == BLOCKED) {
				    		ISOException.throwIt(SW_CARD_BLOCKED);
				    	}else {
				    		apdu.setIncomingAndReceive();
							a = buffer[ISO7816.OFFSET_CDATA];
							b = buffer[ISO7816.OFFSET_CDATA + 1];
							c = buffer[ISO7816.OFFSET_CDATA + 2];
							d = buffer[ISO7816.OFFSET_CDATA + 3];
							e = buffer[ISO7816.OFFSET_CDATA + 4];
							f = buffer[ISO7816.OFFSET_CDATA + 5];
							controlDate = new DateByte((byte)(a),(byte)(b),(short)(((d & 0xFF) << 8) | (c & 0xFF)));
							controlHour = (short)(((f) << 8) | (e & 0xFF));
							validity = (short)(getLastTravelDateInMinute(controlDate, controlHour));
							if(validity > TRAVEL_VALIDITY_TIME ) {
								ISOException.throwIt(SW_TRAVEL_TIME_EXPIRED);
							}
				    	}
					break;
				    case INS_CHECK_LOGS: 
				    	if(lifeCycleState == PRE_PERSO) {
				    		ISOException.throwIt(SW_CARD_NOT_INITIALIZED);
				    	}else if(lifeCycleState == BLOCKED) {
				    		ISOException.throwIt(SW_CARD_BLOCKED);
				    	}else {
				    		//TODO : Faire envoyer la journalisation
				    	}
					break;
				    case INS_GET_TIME_EXPIRED: 
				    	buffer[0] = (byte)(validity & 0xff);
						buffer[1] = (byte)((validity >> 8) & 0xff);
						buffer[2] = (byte)TRAVEL_VALIDITY_TIME;
						apdu.setOutgoingAndSend((short) 0, (short) 3);
					break;
				    case INS_INITIALISE_CARD: 
				    	if(lifeCycleState == PRE_PERSO) {
				    		apdu.setIncomingAndReceive();
				    		byte[] cpin = {
				    			buffer[ISO7816.OFFSET_CDATA],
				    			buffer[ISO7816.OFFSET_CDATA + 1],
				    			buffer[ISO7816.OFFSET_CDATA + 2],
				    			buffer[ISO7816.OFFSET_CDATA + 3]
				    		};
							PIN.update(cpin, (short)0, (byte)4);
							byte[] cpuk = {
				    			buffer[ISO7816.OFFSET_CDATA + 4],
				    			buffer[ISO7816.OFFSET_CDATA + 5],
				    			buffer[ISO7816.OFFSET_CDATA + 6],
				    			buffer[ISO7816.OFFSET_CDATA + 7]
				    		};
							PUK.update(cpuk, (short)0, (byte)4);

				    		lifeCycleState = USE;
				    		apdu.setOutgoingAndSend((short) 0, (short) 0);
				    	}else {
				    		ISOException.throwIt(SW_CARD_ALREADY_INITIALIZED);
				    	}
						break;
				    default:
				    	ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
			    }
			}else {
				ISOException.throwIt(SW_CARD_DEAD);
			}
		}else{
		    ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
		}
    }
 
}