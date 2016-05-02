import java.io.*;

public class testvote{

	public static void main(String[] args)
	{
		if(args.length!=2)
		{
			System.out.println("USAGE: vote <CLA_IP> <CTF_IP>");
		}
		System.out.println("Welcome to the voting system!\nPlease enter your username:");

		String username = readString();
		System.out.println("Please enter your password:");
		String password = readString();
		votes v = new votes(username,password,null,args[0],args[1]);
		v.run();
	}

	public static String readString()
	{
		BufferedReader kb = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			try {
				return kb.readLine();
			} catch (IOException e) {
				// should never happen
			}
		}
	}

}

