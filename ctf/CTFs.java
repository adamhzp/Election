import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.*;
import java.net.*;
import java.math.BigInteger;
import java.util.*;

public class CTFs{
    private HashMap<BigInteger,BigInteger> voters = null;
    private HashMap<BigInteger, Integer> votes = null;

    private HashMap<Integer, Candidate>candidates = null;

    private static int cla_port = 6869;
    private SSLSocket cla_socket = null;
    private CLAconnection claCon  = null;
    private SSLServerSocket voter_server = null;
    public boolean isRunning = true;
    private int num_of_vote_left = 0;
    public boolean finish = false;
    private cmdThread quitThread;
    private String cla_IP = null;

	public CTFs(String cla){
		System.setProperty("javax.net.ssl.keyStore","CTFkeystore.jks");
      	System.setProperty("javax.net.ssl.keyStorePassword", "12345678");
      	System.setProperty("javax.net.ssl.trustStore","truststore.ts");
        System.setProperty("javax.net.ssl.trustStorePassword", "12345678");

        this.cla_IP = cla;

        quitThread = new cmdThread();
        (new Thread(quitThread)).start();

        //load the candidates
        candidates = new HashMap<Integer, Candidate>();
        BufferedReader buf = null;
        String line = null;
        try{
            buf = new BufferedReader(new FileReader("candidates.txt"));
            int i = 1;
            while((line = buf.readLine()) != null)
            {
                Candidate c = new Candidate(line.trim());
                if(c!=null)
                {
                    candidates.put(i, c);
                    i++;
                }
            }
        }catch(Exception e)
        {
            System.out.println("Failed to load the candidates from the candidates.txt");
            System.exit(0);
        }


      	try{
			String[] enabledCipherSuites = { "SSL_RSA_WITH_RC4_128_MD5" };

			SSLServerSocketFactory voter_server_factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
			voter_server = (SSLServerSocket) voter_server_factory.createServerSocket(7000);
			voter_server.setEnabledCipherSuites(enabledCipherSuites);
			System.out.println("\nStarting CTF server at port "+7000+"\n IP: "+Inet4Address.getLocalHost().getHostAddress());

            SSLSocketFactory sf = (SSLSocketFactory) SSLSocketFactory.getDefault();
            cla_socket = (SSLSocket)sf.createSocket();
            cla_socket.setEnabledCipherSuites(enabledCipherSuites);
            cla_socket.connect(new InetSocketAddress(cla_IP, 6869));
            claCon = new CLAconnection(cla_socket, this);
            (new Thread(claCon)).start();

            voters = new HashMap<BigInteger,BigInteger>();
            votes = new HashMap<BigInteger,Integer>();

		}catch(Exception e){
            System.out.println("CLA is not set up yet! Please set up the CLA server first.");
            System.exit(0);
		}

	}

	public void run(){
		try{
			while(true)
			{
				SSLSocket s = (SSLSocket)voter_server.accept();
                (new Thread(new handleVote(s,this))).start();
			}
		}catch(Exception e)
		{
		}
	}


    public void addValidation(BigInteger validation)
    {
        this.voters.put(validation, BigInteger.ZERO);
    }

    public boolean isValid(BigInteger check)
    {
        for(BigInteger v:voters.keySet())
        {
            if(check.equals(v))
            {
                return true;
            }
        }
        return false;
    }

    public boolean isvoted(BigInteger validation)
    {
        BigInteger id = voters.get(validation);
        if(id == null) return false;
        if(id.equals(BigInteger.ZERO))
        {
            return false;
        }
        
        return true;
    }

    public Boolean updateVote(BigInteger validation, BigInteger id, int vote)
    {
        if(finish) return false;
        this.voters.put(validation, id);
        this.votes.put(id, vote);
        this.candidates.get(vote).addVote();
        num_of_vote_left--;
        if(num_of_vote_left == 0)
        {
            finish = true;
        }
        return true;
    }


	class handleVote implements Runnable{

    	private SSLSocket socket = null;
    	private CTFs ctf = null;
    	private ObjectOutputStream output = null;
    	private ObjectInputStream input = null;

    	public handleVote(SSLSocket socket, CTFs ctf){
    		this.socket = socket;
    		this.ctf = ctf;
    		try{
    			output = new ObjectOutputStream(socket.getOutputStream());
            	input = new ObjectInputStream(socket.getInputStream());
    		}catch(Exception e)
    		{
    		}
    	}

    	public void run(){
    		try{

    			byte[] voteReq = (byte[]) input.readObject();
    			ObjectInputStream reqStream = new ObjectInputStream(new ByteArrayInputStream(voteReq));
    			String req = reqStream.readUTF();
    			BigInteger check = (BigInteger) reqStream.readObject();
    			if(ctf.isValid(check))
    			{
    				System.out.println("Valid validation: "+check);
                    if(finish){
                            output.writeObject(getResult(check));
                            return;
                    }

                    if(isvoted(check)){

                        ByteArrayOutputStream electionBytes = new ByteArrayOutputStream();
                        ObjectOutputStream electionStream = new ObjectOutputStream(electionBytes);
                        electionStream.writeUTF("voted");
                        electionStream.writeObject(check);
                        output.writeObject(electionBytes.toByteArray());
                        electionStream.close();
                        electionBytes.close();
                        return;
                    }else{

                        ByteArrayOutputStream electionBytes = new ByteArrayOutputStream();
                        ObjectOutputStream electionStream = new ObjectOutputStream(electionBytes);
                        electionStream.writeUTF("Success");
                        electionStream.writeUTF("Here's a list of candidates:\n1. Adam\n2.Bob\n3.Obama\n");
                        electionStream.writeInt(1);
                        electionStream.writeInt(3);
                        electionStream.writeObject(check);

                        output.writeObject(electionBytes.toByteArray());
                        electionStream.close();
                        electionBytes.close();
                    }

    			}else{
                    System.out.println("Invalid validation");

                }

                byte[] voteBytes = (byte[])input.readObject();

                ObjectInputStream voteStream = new ObjectInputStream(new ByteArrayInputStream(voteBytes));

                req = voteStream.readUTF();
                int vote = voteStream.readInt();
                check = (BigInteger) voteStream.readObject();
                BigInteger id = (BigInteger)voteStream.readObject();
                if(req.equalsIgnoreCase("choice") && isValid(check))
                {
                    if(this.ctf.updateVote(check,id,vote))
                    {
                        System.out.println("add vote successfully!");
                        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                        ObjectOutputStream str = new ObjectOutputStream(bytes);
                        str.writeUTF("VOTESUCCESS");
                        str.writeObject(id);
                        output.writeObject(bytes.toByteArray());
                    }else{
                        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                        ObjectOutputStream str = new ObjectOutputStream(bytes);
                        str.writeUTF("FAILED");
                        str.writeObject(id);
                        output.writeObject(bytes.toByteArray());
                    }
                }
    		}catch(Exception e)
    		{
    		}

    	}

        public byte[] getResult(BigInteger validation)
        {
            try{
            String resultString = null;
            int vote = 0;
            Candidate winner = null;
            if(this.ctf.finish)
            {
                resultString ="Here is the election result:\nWinner is:\n";
                for(Candidate c : this.ctf.candidates.values())
                {
                    if(vote<c.vote)
                    {
                        winner = c;
                        vote = c.vote;
                    }
                }
                if(winner!= null)
                {
                    resultString+=winner.name;
                    resultString+="!!!\n\n";
                }
                for(BigInteger v: this.ctf.votes.keySet())
                {
                    resultString+="Voter ";
                    resultString+= v;
                    resultString+=" voted ";
                    resultString+=candidates.get(votes.get(v)).name;
                    resultString+="\n\n";
                }
            }
            if(resultString == null) return null;
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ObjectOutputStream stream = new ObjectOutputStream(bytes);
            stream.writeUTF("result");
            stream.writeUTF(resultString);
            stream.writeObject(validation);
            return bytes.toByteArray();
        }catch(Exception e)
        {
            return null;
        }

        }


    }

    class CLAconnection implements Runnable{
        
        private SSLSocket socket = null;
        private ObjectOutputStream output = null;
        private ObjectInputStream input = null;
        private boolean isRunning = true;
        private BigInteger nounce = null;
        private CTFs ctf = null;

        public CLAconnection(SSLSocket socket,CTFs ctf)
        {
            this.socket = socket;
            this.ctf = ctf;
            try{
                output = new ObjectOutputStream(socket.getOutputStream());
                input = new ObjectInputStream(socket.getInputStream());
            }catch(Exception e)
            {
            }
        }

        public void run()
        {
            try{
                byte[] in = (byte[])input.readObject();
                ObjectInputStream stream = new ObjectInputStream(new ByteArrayInputStream(in));
                String request = stream.readUTF();

                if(request.equalsIgnoreCase("connected")){
                    System.out.println("Connected with CLA now.");
                    num_of_vote_left = stream.readInt();
                    this.nounce = (BigInteger) stream.readObject();
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    ObjectOutputStream s = new ObjectOutputStream(bytes);
                    s.writeObject(nounce);
                    output.writeObject(bytes.toByteArray());
                    bytes.close();
                    stream.close();
                }

                //waiting for incoming validation number..
                in = (byte[])input.readObject();
                while(in != null && isRunning)
                {
                    handleMsg(in);
                    in = (byte[]) input.readObject();
                }
            }catch(Exception e)
            {
                System.out.println("CLA is down.\nShutting down the CTF server now.\nPlease restart the CLA program to start the voting system.\n");
                System.exit(0);            
            }
        }

        private void handleMsg(byte[] msg)
        {
            try{
                ObjectInputStream stream = new ObjectInputStream(new ByteArrayInputStream(msg));
                String request = stream.readUTF();
                BigInteger check = (BigInteger) stream.readObject();
                if(check.equals(this.nounce)){
                    if(request.equals("ADD"))
                    {
                        System.out.println("Get add request from CLA..");
                        BigInteger validation = (BigInteger) stream.readObject();

                        ctf.addValidation(validation);
                        ByteArrayOutputStream ack  = new ByteArrayOutputStream();
                        ObjectOutputStream s = new ObjectOutputStream(ack);
                        s.writeUTF("Success");
                        s.writeObject(this.nounce);
                        output.writeObject(ack.toByteArray());
                        s.close();
                        ack.close();

                    }else if(request.equalsIgnoreCase("keepalive")){

                    }
                    else{
                        System.out.println(request);
                    }
                }else{
                    System.out.println("Potential replay attack");
                }
            }catch(Exception e)
            {

            }
        }

    }


    class Candidate{
        public String name;
        private int vote;

        public Candidate(String name)
        {
            this.vote =0;
            this.name = name;
        }
        public void addVote()
        {
            this.vote++;
        }

    }


}