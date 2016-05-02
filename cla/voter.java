import java.math.BigInteger;



public class voter{

	private String userName = null;
	private String password = null;
	private BigInteger validation = null;

	public voter(String user, String password)
	{
		this.userName = user;
		this.password = password;
	}

	public String getUsername()
	{
		return this.userName;
	}

	public String getPassword()
	{
		return this.password;
	}

	public boolean hasValidation()
	{
		if(this.validation!=null)
		{
			return true;
		}
		return false;
	}
/*
	public ByteBuffer toBytebuffer()
	{

		byte[] u = userName.getBytes(Charset.forName("UTF-8"));
		byte[] p = password.getBytes(Charset.forName("UTF-8"));
		

	}	
	*/



}



