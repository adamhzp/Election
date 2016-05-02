import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.*;
import java.net.*;
import java.math.BigInteger;
import java.util.*;


public class CLA implements Runnable{

	private boolean isRunning = true;

	private int voter_port = 6868;
	private int ctf_port = 6869;
	private SSLServerSocketFactory voter_server_factory = null;
	private SSLServerSocket voter_server = null;
	private SSLServerSocket ctf = null;
	//a list of voters..
	private HashMap<voter, BigInteger> voters = null;
	private CTFconnection ctfCon = null;
	public boolean isConnectedToCTF = false;
	private cmdThread quitThread;


	public CLA()
	{
		System.setProperty("javax.net.ssl.keyStore","keystore.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "12345678");
      	System.setProperty("javax.net.ssl.trustStore","truststore.ts");
        System.setProperty("javax.net.ssl.trustStorePassword", "12345678");
		
		quitThread = new cmdThread();
        (new Thread(quitThread)).start();
		this.voter_port = voter_port;

		//read the voters and password from a txt now... need to be changed later...
		//maybe add encryptyion to voters...
		voters = new HashMap<voter, BigInteger>();
		loadVoters();

		try{
			voter_server_factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
			voter_server = (SSLServerSocket) voter_server_factory.createServerSocket(voter_port);
			String[] enabledCipherSuites = { "SSL_RSA_WITH_RC4_128_MD5" };
			voter_server.setEnabledCipherSuites(enabledCipherSuites);
			System.out.println("\nStarting CLA server at port "+this.voter_port+"\n IP: "+Inet4Address.getLocalHost().getHostAddress());

			SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
			ctf = (SSLServerSocket) ssf.createServerSocket(ctf_port);
			ctf.setEnabledCipherSuites(enabledCipherSuites);
			System.out.println("\nWaiting for CTF server on port: "+this.ctf_port);
			ctfCon = new CTFconnection(ctf, this);
			(new Thread(ctfCon)).start();
		}catch(Exception e){
			System.out.println("Failed to set up the CLA server");

			System.exit(0);
		}
	}

	public void loadVoters()
	{
		BufferedReader buf = null;
		String line = null;
		try{
			buf = new BufferedReader(new FileReader("voters.txt"));
			while((line = buf.readLine()) != null)
			{
				voter v = getVoter(line);
				if(v!=null)
				{
					voters.put(v, BigInteger.ZERO);
				}
			}
		}catch(Exception e)
		{
			System.out.println("Failed to load the voters from voters.txt\n");
			System.exit(0);
		}
	}

	public void run()
	{
		try{ 
			while(isRunning)
			{
				SSLSocket socket = (SSLSocket)voter_server.accept();
				(new Thread(new handleLogin(socket))).start();
			}

		}catch(Exception e)
		{
			return;
		}
	}


	private voter getVoter(String line)
	{
		line = line.trim();

		int index = line.indexOf(",");
		if(index != -1)
		{
			return new voter(line.substring(0,index).trim(), line.substring(index+1).trim());
		}
		
		return null;
	}

	class handleLogin implements Runnable
	{
		private SSLSocket socket = null;
		private BigInteger nounce = null;

		public handleLogin(SSLSocket socket)
		{
			this.socket = socket;
		}

		public void run()
		{
			try{
				
				ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
            	ObjectInputStream input = new ObjectInputStream(socket.getInputStream());

           		byte[] msg = (byte[])input.readObject();
           		ObjectInputStream stream = new ObjectInputStream(new ByteArrayInputStream(msg));

           		String request = stream.readUTF();
           		if(request.equals("LOGIN"))
           		{
           			this.nounce =(BigInteger) stream.readObject();
           			byte[] login = (byte[])input.readObject();
           			DataInputStream passtream = new DataInputStream(new ByteArrayInputStream(login));
           			String username = passtream.readUTF();
           			String password = passtream.readUTF();

           			ByteArrayOutputStream responseBytes = new ByteArrayOutputStream();
                	ObjectOutputStream responseStream = new ObjectOutputStream(responseBytes);
                	responseStream.writeObject(this.nounce);

                	if(isConnectedToCTF){
           				BigInteger b = null;
           				if( (b = login(username,password)) == null)
           				{	
							System.out.println("Username/Password mismatch");
							responseStream.writeUTF("Failed");				
           				}
           				else if(b.equals(BigInteger.ZERO))
           				{

           					b = new BigInteger(64, new Random());

							//send the validation number to the CTF server here
           					if(ctfCon.sendValidation(b,username)){
           						responseStream.writeUTF("Logged");
           						responseStream.writeObject(b);
								addValidation(username,password,b);
	           				}else{
           						responseStream.writeUTF("Failed");
           					}

           				}else{
           					responseStream.writeUTF("Logged");
           					responseStream.writeObject(b);
           				}
           			}else{
           				responseStream.writeUTF("NOCTF");
           			}
           				responseStream.writeObject(new Long(System.currentTimeMillis()));
           				output.writeObject(responseBytes.toByteArray());
           		}
           	}catch(Exception e)
           	{
           	}
		}

		public void addValidation(String username, String pass, BigInteger validation)
		{
			for(voter v : voters.keySet())
			{
				if(v.getUsername().equals(username) && v.getPassword().equals(pass))
				{
					voters.put(v, validation);
				}
			}	
		}


		public BigInteger login(String username, String pass)
		{
			for(voter v : voters.keySet())
			{
				if(v.getUsername().equals(username) && v.getPassword().equals(pass))
				{
					return voters.get(v);
				}
			}	
			return null;
		}
	}


	class CTFconnection implements Runnable{
		private SSLServerSocket ctf_server_socket = null;
		private SSLSocket socket = null;
		private ObjectOutputStream output = null;
    	private ObjectInputStream input = null;
    	private BigInteger nounce = null;
    	private CLA cla = null;
    	public boolean isRunning = true;

		public CTFconnection(SSLServerSocket ctf,CLA cla)
		{
			this.ctf_server_socket = ctf;
			this.cla = cla;
		}

		public void run()
		{
			try{ 
				connect();
            	while(isRunning)
            	{
            		Thread.sleep(500);
            		ByteArrayOutputStream b = new ByteArrayOutputStream();
            		ObjectOutputStream s = new ObjectOutputStream(b);
            		s.writeUTF("keepalive");
            		s.writeObject(this.nounce);
            		this.output.writeObject(b.toByteArray());
            	}
			}catch(Exception e)
			{
				System.out.println("CTF is disconnected..\nWaiting for CTF to reconnect..\nReset all the votes..\n");
				cla.voters = new HashMap<voter, BigInteger>();
				cla.loadVoters();
				cla.isConnectedToCTF = false;
				run();
				return;
			}
		}

		public void connect()
		{
			try{
				socket = (SSLSocket)ctf_server_socket.accept();
				output = new ObjectOutputStream(socket.getOutputStream());
            	input = new ObjectInputStream(socket.getInputStream());
				//test..
            	ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            	ObjectOutputStream stream = new ObjectOutputStream(bytes);
            	//generate a nounce here
            	nounce = new BigInteger(64, new Random());
            	stream.writeUTF("Connected");
            	stream.writeInt(voters.size());
            	stream.writeObject(nounce);
            	sendMessage(bytes.toByteArray());
            	bytes.close();
            	stream.close();

            	byte[] ack = (byte[]) input.readObject();
            	BigInteger check = (BigInteger)(new ObjectInputStream(new ByteArrayInputStream(ack))).readObject();
            	if(check.equals(nounce))
            	{
            		System.out.println("Connected with the CTF");
            		this.cla.isConnectedToCTF = true;
            	}else{
            		System.out.println("Potential replay attack");
            	}
			}catch(Exception e){
			}
		}

		public boolean sendValidation(BigInteger validation,String username)
		{
			try{
				ByteArrayOutputStream bytes = new ByteArrayOutputStream();
				ObjectOutputStream stream = new ObjectOutputStream(bytes);
				stream.writeUTF("ADD");
				stream.writeObject(nounce);
				stream.writeObject(validation);
				sendMessage(bytes.toByteArray());
				bytes.close();
				stream.close();	

				byte[] ack = (byte[])input.readObject();
				ObjectInputStream ackStream = new ObjectInputStream(new ByteArrayInputStream(ack));
				String s = ackStream.readUTF();
				BigInteger check = (BigInteger) ackStream.readObject();
				if(check.equals(nounce))
            	{
           			if(s.equalsIgnoreCase("Success"))
            		{
            			System.out.println("Register user "+username+" to CTF successfully.");
            			return true;
            		}else{
            			System.out.println("Failed to register user "+username+" to CTF.");	
            		}
            	}else{
            		System.out.println("Potential replay attack");
            	}
            	return false;
            }catch(Exception e)
            {
            	return false;
            }

		}

		public void sendMessage(byte[] msg)
		{
			try{
				this.output.writeObject(msg);
			}catch(Exception e)
			{
			}
		}

	}

}


