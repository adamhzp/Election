import java.util.Scanner;
import java.io.*;

public class cmdThread implements Runnable{


	public void run(){
		Scanner scannerObj = new Scanner(System.in);
		System.out.println("Type <quit> or <q> to quit the program");
		String input = scannerObj.nextLine();
		//waiting for input
		while((input==null)||((!input.equalsIgnoreCase("quit")) && (!input.equalsIgnoreCase("q")))){
			System.out.println("Type <quit> or <q> to quit the program");
			input = scannerObj.nextLine();
		}

		if(input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("q")){
				System.exit(0);
		}
	}



}