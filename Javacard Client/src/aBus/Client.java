package aBus;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;

import com.sun.javacard.apduio.Apdu;
import com.sun.javacard.apduio.CadT1Client;
import com.sun.javacard.apduio.CadTransportException;

public class Client {

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
    
    //Constantes de status word error for lifecycle
	public static final short SW_CARD_ALREADY_INITIALIZED = 0x6400;
	public static final short SW_CARD_NOT_INITIALIZED = 0x6401;
	public static final short SW_CARD_BLOCKED = 0x6402;
	public static final short SW_CARD_DEAD = 0x6403;
	public static final short SW_CARD_ALREADY_UNLOCK = 0x6404;
	
	//Arguments
	public static final short P1_VERIFY_PIN = (byte)0x01;
	public static final short P1_RELOAD = (byte)0x02;
	public static final short P1_VERIFY_PUK = (byte) 0x03;
	public final static byte EMPTY_DATA = (byte) 0x7777;
	public static void errorManager(Apdu apdu, CadT1Client cad) throws IOException, CadTransportException{
		switch (apdu.getStatus()) {
		case SW_TRAVEL_TIME_EXPIRED:
			apdu.command[Apdu.INS] = Client.INS_GET_TIME_EXPIRED;
			cad.exchangeApdu(apdu);
			System.out.println("Validité du voyage terminée !\nTicket expiré depuis " + (short)((((apdu.dataOut[1]) << 8) | (apdu.dataOut[0] & 0xFF)) - (short)(apdu.dataOut[2])) +" minute(s) !" );
		break;
		case SW_TRAVEL_ALREADY_VALIDATED:
			System.out.println("Vous avez déjà validé votre carte !");
		break;
		case SW_INSUFFICIENT_BALANCE_ERROR:
			System.out.println("Vous n'avez plus de voyage !\nMerci de recharger votre carte...");
		break;
		case SW_CARD_ALREADY_INITIALIZED:
			System.out.println("La carte a déjà été initialisée.");
		break;
		case SW_CARD_NOT_INITIALIZED:
			System.out.println("La carte n'a pas été initialisée.");
		break;
		case SW_CARD_BLOCKED:
			System.out.println("La carte est verouillée.");
		break;
		case SW_CARD_DEAD:
			System.out.println("La carte est inutilisable.");
		break;
		case SW_CARD_ALREADY_UNLOCK:
			System.out.println("La carte n'est pas verouillée.");
		break;
		case SW_INVALID_AMOUNT:
			System.out.println("Le montant doit être compris entre 0 et 5.");
		break;
		case SW_INVALID_PIN:
			System.out.println("Code PIN invalide.");
		break;
		case SW_INVALID_PUK:
			System.out.println("Code PUK invalide.");
		break;
		case SW_BLOCKED_PIN:
			System.out.println("Vous venez de bloquer la carte.");
		break;
		default:
			System.out.println("Erreur : status word different de 0x9000");
		}
	}
	
	public static byte[] saisieDate() {
		byte[] data = new byte[8];
		
		/*Date date = new Date();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		short year = (short) (calendar.get(Calendar.YEAR));
		short time = (short) (calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE));
		data[0] =(byte)(calendar.get(Calendar.DAY_OF_MONTH));
		data[1] =(byte)(calendar.get(Calendar.MONTH) + 1);
		data[2] =(byte)(year & 0xff);
		data[3] =(byte)((year >> 8) & 0xff);
		data[4] =(byte)(time & 0xff);
		data[5] =(byte)((time >> 8) & 0xff);*/

			
		Scanner clavier = new Scanner(System.in);
		System.out.println("Saisir le jour :");
		byte saisie = clavier.nextByte();
		data[0] = saisie;
		System.out.println("Saisir le mois :");
		saisie = clavier.nextByte();
		data[1] = saisie;
		System.out.println("Saisir l'année :");
		short saisie2 = clavier.nextShort();
		data[2] = (byte)(saisie2 & 0xff);
		data[3] = (byte)((saisie2 >> 8) & 0xff);
		System.out.println("Saisir l'heure (format minute) :");
		saisie2 = clavier.nextShort();
		data[4] = (byte)(saisie2 & 0xff);
		data[5] = (byte)((saisie2 >> 8) & 0xff);
		return data;
	}
	
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
		Scanner clavier = new Scanner(System.in);
		/* Menu principal */
		boolean fin = false;
		while (!fin) {
			System.out.println();
			System.out.println("Application aBus");
			System.out.println("----------------------------");
			System.out.println();
			System.out.println("0 - Initialisation");
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
			while (!(choix >= '0' && choix <= '7')) {
				choix = System.in.read();
			}
			
			apdu = new Apdu();
			apdu.command[Apdu.CLA] = 0x25;
			apdu.command[Apdu.P1] = 0x00;
			apdu.command[Apdu.P2] = 0x00;

			byte[] data;

			switch (choix) {
				case '0':
					apdu.command[Apdu.INS] = Client.INS_INITIALISE_CARD;			
					int pin1 = (int)(Math.random() * 10);
					int pin2 = (int)(Math.random() * 10);
					int pin3 = (int)(Math.random() * 10);
					int pin4 = (int)(Math.random() * 10);
					int puk1 = (int)(Math.random() * 10);
					int puk2 = (int)(Math.random() * 10);
					int puk3 = (int)(Math.random() * 10);
					int puk4 = (int)(Math.random() * 10);
					byte[] dataInit = {
						(byte)pin1,(byte)pin2,(byte)pin3,(byte)pin4, //Code PIN
						(byte)puk1,(byte)puk2,(byte)puk3,(byte)puk4 //Code PUK
					};
					apdu.setDataIn(dataInit);
					cad.exchangeApdu(apdu);
					if (apdu.getStatus() == 0x9000) {
						System.out.println("Votre code PIN est : " +pin1 +""+pin2+""+pin3+""+pin4);
						System.out.println("Votre code PUK est : " +puk1 +""+puk2+""+puk3+""+puk4);
						System.out.println("Carte intialisé !");
					}else {
						errorManager(apdu, cad);
					}
				break;
				case '1':
					apdu.command[Apdu.INS] = Client.INS_BUY_TRAVEL;	
					data = saisieDate();
					System.out.println("Veuillez entrer le numéro de ligne :");
					byte p1 = clavier.nextByte();
					data[6] = p1;
					System.out.println("Veuillez entrer le sens (0 ou 1):");
					p1 = clavier.nextByte();
					data[7] = p1;
					apdu.setDataIn(data);
					cad.exchangeApdu(apdu);
					if (apdu.getStatus() == 0x9000) {
						System.out.println("Trajet validé !");
					}else {
						errorManager(apdu, cad);
					}
				break;
				case '2':
					apdu.command[Apdu.INS] = INS_RELOAD_CARD;
					apdu.command[Apdu.P1] = P1_VERIFY_PIN;
					cad.exchangeApdu(apdu);
					
					System.out.println("Veuillez saisir votre code PIN caractère par caractère :");
					p1 = clavier.nextByte();
					byte p2 = clavier.nextByte();
					byte p3 = clavier.nextByte();
					byte p4 = clavier.nextByte();
					byte[] cPin = {p1,p2,p3,p4};
					apdu.setDataIn(cPin);
					cad.exchangeApdu(apdu);
					if (apdu.getStatus() != 0x9000) {
						errorManager(apdu, cad);
					}else {
						apdu.command[Apdu.P1] = P1_RELOAD;
						System.out.println("Saisir un montant :");
						byte amount = clavier.nextByte();
						data = saisieDate();
						try {
							data[6] = amount;
							apdu.setDataIn(data);
							cad.exchangeApdu(apdu);
						}catch (Exception e) {
							System.out.println("Erreur: Impossible d'executer la commande");
							return;
						}
						if (apdu.getStatus() != 0x9000) {
							errorManager(apdu, cad);
						} else {
							System.out.println("Votre carte a bien été rechargée !");
						}
					}
					
				break;	
				case '3':
					apdu.command[Apdu.INS] = Client.INS_CONSULT_CARD;
					cad.exchangeApdu(apdu);
					if (apdu.getStatus() != 0x9000) {
						errorManager(apdu, cad);
					} else {
						System.out.println("Balance : " + apdu.dataOut[0] + " voyage(s)");
					}
				break;
				case '4':
					apdu.command[Apdu.INS] = Client.INS_UNLOCK_PIN;
					apdu.command[Apdu.P1] = P1_VERIFY_PUK;
					cad.exchangeApdu(apdu);
					System.out.println("Veuillez saisir le code PUK afin de dévérouiller le code PIN");
					byte pu1 = clavier.nextByte();
					byte pu2 = clavier.nextByte();
					byte pu3 = clavier.nextByte();
					byte pu4 = clavier.nextByte();
					byte[] cPuk = {pu1,pu2,pu3,pu4};
					apdu.setDataIn(cPuk);
					cad.exchangeApdu(apdu);
					if (apdu.getStatus() != 0x9000) {
						errorManager(apdu, cad);
					}else{
						try{
						apdu.command[Apdu.P1] = P1_VERIFY_PUK;
						apdu.setDataIn(cPuk);
						}catch(Exception e){
							System.out.println("Erreur: Impossible d'executer la commande");
							return;
						}
						if (apdu.getStatus() != 0x9000) {
							errorManager(apdu, cad);
						}else{
							cad.exchangeApdu(apdu);
							System.out.println("Carte devérouillée");
						}
					}
				break;
				case '5':
					apdu.command[Apdu.INS] = Client.INS_CHECK_VALIDITY;
					data = saisieDate();
					apdu.setDataIn(data);
					cad.exchangeApdu(apdu);
					if (apdu.getStatus() != 0x9000) {
						errorManager(apdu, cad);
					}else {
						System.out.println("Carte validée !");
					}
					
				break;	
				case '6':
					apdu.command[Apdu.INS] = Client.INS_CHECK_LOGS;
					cad.exchangeApdu(apdu);
					if(apdu.getStatus() != 0x9000){
						errorManager(apdu, cad);
					}else{
						cad.exchangeApdu(apdu);
						byte[] tabJ = new byte[90];
						for(int i = 0; i<90; i++){
							tabJ[i] = apdu.dataOut[i];
						}
						System.out.println("Historique de vos transactions :");
						int cpt = 0;
						for(int j = 0; j< 10; j++){
							short year1 =(short)((tabJ[(j*9)+3] & 0xFF) << 8 | (tabJ[(j*9)+4]& 0xFF));
							short month =(short)(tabJ[(j*9)+2]);
							short day =(short)(tabJ[(j*9)+1]);
							short h1 = (short)(tabJ[(j*9)+5] << 8 | (tabJ[(j*9)+6] & 0xFF));
							short min = (short)(h1%60);
							short h = (short)(h1/60);
							short lignecout =(short) tabJ[(j*9)+7];
							short sens =(short)tabJ[(j*9)+8];
							switch(tabJ[j*9]){
							case 0x01 : 
								System.out.println("[Achat] Ligne "+lignecout+", sens "+sens+" acheté le "+day+"/"+month+"/"+ year1+" à "+h+"h"+min);
							break;
							case 0x02 : 
								System.out.println("[Rechargement] pour "+lignecout+" trajets achetés le "+day+"/"+month+"/"+ year1+" à "+h+"h"+min);
							break;
							case 0x03 : 
								System.out.println("[Correspondance] "+lignecout+", sens "+sens+" acheté le "+day+"/"+month+"/"+ year1+" à "+h+"h"+min);
							break;
							}
								
						}
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
