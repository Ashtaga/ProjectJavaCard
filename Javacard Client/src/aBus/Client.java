package aBus;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Calendar;
import java.util.Date;

import com.sun.javacard.apduio.Apdu;
import com.sun.javacard.apduio.CadT1Client;
import com.sun.javacard.apduio.CadTransportException;

public class Client {

	final static byte INS_BUY_TRAVEL = (byte) 0x00;
    final static byte INS_RELOAD_CARD = (byte) 0x01;
    final static byte INS_CONSULT_CARD = (byte) 0x02;
    final static byte INS_UNLOCK_PIN = (byte) 0x03;
    final static byte INS_CHECK_VALIDITY = (byte) 0x04;
    final static byte INS_CHECK_LOGS = (byte) 0x05;
    final static byte INS_GET_TIME_EXPIRED = (byte) 0x06;
    
    final static byte SW_INSUFFICIENT_BALANCE_ERROR = (byte) 0x6300;
    final static byte SW_TRAVEL_TIME_EXPIRED = (byte) 0x6301;
    final static byte SW_TRAVEL_ALREADY_VALIDATED = (byte) 0x6302;
    
	public static void main(String[] args) throws IOException, CadTransportException {
		/* Connexion a la Javacard */
		CadT1Client cad;
		Socket sckCarte;
		try {
			sckCarte = new Socket("localhost", 9025);
			sckCarte.setTcpNoDelay(true);
			BufferedInputStream input = new BufferedInputStream(sckCarte.getInputStream());
			BufferedOutputStream output = new BufferedOutputStream(sckCarte.getOutputStream());
			cad = new CadT1Client(input, output);
			cad.powerUp();
		} catch (Exception e) {
			System.out.println("Erreur : impossible de se connecter a la Javacard");
			return;
		}
		
		
		 /* Sélection de l'applet */
		Apdu apdu = new Apdu();
		
		/* Menu principal */
		boolean fin = false;
		while (!fin) {
			System.out.println();
			System.out.println("Application aBus");
			System.out.println("----------------------------");
			System.out.println();
			System.out.println("1 - Valider son voyage");
			System.out.println("2 - Recharger la carte");
			System.out.println("3 - Consulter la carte");
			System.out.println("4 - Débloquer le code PIN");
			System.out.println("5 - Vérifier la validité");
			System.out.println("6 - Consulter l'historique");
			System.out.println("7 - Quitter");
			System.out.println();
			System.out.println("Votre choix ?");

			int choix = System.in.read();
			while (!(choix >= '1' && choix <= '7')) {
				choix = System.in.read();
			}
			
			apdu = new Apdu();
			apdu.command[Apdu.CLA] = 0x25;
			apdu.command[Apdu.P1] = 0x00;
			apdu.command[Apdu.P2] = 0x00;
			
			Date date = new Date();
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(date);
			short year = (short) (calendar.get(Calendar.YEAR));
			short time = (short) (calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE));
			byte[] dateData = {
				(byte)(calendar.get(Calendar.DAY_OF_MONTH)),
				(byte)(calendar.get(Calendar.MONTH) + 1),
				(byte)(year & 0xff),
				(byte)((year >> 8) & 0xff),
				(byte)(time & 0xff),
				(byte)((time >> 8) & 0xff)
			};
			switch (choix) {
				case '1':
					apdu.command[Apdu.INS] = Client.INS_BUY_TRAVEL;			
					apdu.setDataIn(dateData);
					cad.exchangeApdu(apdu);
					if (apdu.getStatus() == 0x9000) {
						System.out.println("Trajet validé !");
					}else if(apdu.getStatus() == SW_TRAVEL_ALREADY_VALIDATED) {
						System.out.println("Vous avez déjà validé votre carte il y a moins de 60 minutes !");
					}else {
						System.out.println("Erreur : status word different de 0x9000");
					}
				break;
				case '2':
					apdu.command[Apdu.INS] = Client.INS_RELOAD_CARD;
					cad.exchangeApdu(apdu);
					if (apdu.getStatus() != 0x9000) {
						System.out.println("Erreur : status word different de 0x9000");
					} else {
	
					}
				break;	
				case '3':
					apdu.command[Apdu.INS] = Client.INS_CONSULT_CARD;
					cad.exchangeApdu(apdu);
					if (apdu.getStatus() != 0x9000) {
						System.out.println("Erreur : status word different de 0x9000");
					} else {
						System.out.println("Balance : " + apdu.dataOut[0] + " voyage(s)");
					}
				break;
				case '4':
					apdu.command[Apdu.INS] = Client.INS_UNLOCK_PIN;
					cad.exchangeApdu(apdu);
					if (apdu.getStatus() != 0x9000) {
						System.out.println("Erreur : status word different de 0x9000");
					} else {
	
					}
				break;
				case '5':
					apdu.command[Apdu.INS] = Client.INS_CHECK_VALIDITY;
					apdu.setDataIn(dateData);
					cad.exchangeApdu(apdu);
					if (apdu.getStatus() == SW_TRAVEL_TIME_EXPIRED) {
						apdu.command[Apdu.INS] = Client.INS_GET_TIME_EXPIRED;
						cad.exchangeApdu(apdu);
						System.out.println("Validité du voyage terminée !\nTicket expiré depuis  " + (short)(((apdu.dataOut[1]) << 8) | (apdu.dataOut[0] & 0xFF)) +" minute(s) !" );
					} else if (apdu.getStatus() != 0x9000) {
						System.out.println("Erreur : status word different de 0x9000");
					}else {
						System.out.println("Carte validé !");
					}
					
				break;	
				case '6':
					apdu.command[Apdu.INS] = Client.INS_CHECK_LOGS;
					cad.exchangeApdu(apdu);
					if (apdu.getStatus() != 0x9000) {
						System.out.println("Erreur : status word different de 0x9000");
					} else {
						
					}
				break;	
				case '7':
					fin = true;
				break;
			}
		}
		
		/* Mise hors tension de la carte */
		try {
			cad.powerDown();
		} catch (Exception e) {
			System.out.println("Erreur lors de l'envoi de la commande Powerdown");
			return;
		}

	}

}
