public class testCTF{

	public static void main(String[] args)
	{
		if(args.length!=1)
		{
			System.out.println("USAGE: testCTF <CLA_IP>");
			return;
		}

		try{
			CTFs ctf = new CTFs(args[0]);
			ctf.run();
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}



}
