public class testCTF{

	public static void main(String[] args)
	{
		if(args.length!=1)
		{
			System.out.println("USAGE: testCTF <CLA_IP>");
			return;
		}

		try{
			CTFs ctf = new CTFs("128.6.13.233");
			ctf.run();
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}



}