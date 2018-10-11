/* Michael Shachar
   Ido Ben El
   Michal Bar Ilan
   Betzalel Moshkovitz */
   
package classifier;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;

public class Server {

	private static final int PORT = 23;
	private static final String FAILURE = "false\n";
	private static final String SUCCESS = "true\n";
	private static final String PASSWORD = "chatapp";
	private static final String USER = "bestapp";
	private static final String CLASSIFIER = "classifier";
	private static final String TRAINING_DATA = "train";
	private static final String NEGATIVE = "negative";
	private static final String POSITIVE = "positive";
	private static final String ERROR_ON_SERVER_THREAD = "error on server thread";

	static class ServerThread implements Runnable {

		private Socket client = null;
		private int reviewID = 0;
		public ServerThread(Socket c) {
			this.client = c;
		}

		// server thread to classify a review that was sent from an authorized
		// client
		public void run() {
			BufferedWriter writer = null;
			try (BufferedReader inFromClient = new BufferedReader(
					new InputStreamReader(client.getInputStream()));
					DataOutputStream outToClient = new DataOutputStream(
							client.getOutputStream());) {

				// get user and password from the client
				String[] clientDetails = inFromClient.readLine().split(",");

				// send authentication result to the client
				if (clientDetails[0].equals(USER)
						&& clientDetails[1].equals(PASSWORD)) {
					
					outToClient.writeBytes(SUCCESS);
				} else {
					outToClient.writeBytes(FAILURE);
					return;
				}

				// get the review from the client and write it into a text file
				FileSystem fs = FileSystems.getDefault();
				String rev = String.valueOf(reviewID);
				Path parentDir = fs.getPath(rev);
				if (!Files.exists(parentDir))
					Files.createDirectories(parentDir);
				Path dst = Paths.get(rev + "/" + rev + ".txt");
				String line;
				String[] lines;
				writer = Files.newBufferedWriter(dst,
						StandardCharsets.UTF_8);
				try  {
					while ((line = inFromClient.readLine()) == null);
					line = line.replace("\\n", "\n");
					lines = line.split("\n");
					for (int i = 0; i < lines.length; i++) {
						writer.write(lines[i]);
						writer.newLine();
					}
					if (writer != null) {
						writer.close();
					}
				} catch (Exception ex){
					ex.printStackTrace();
				}
				// run map reduce to classify the review
				if (Driver.runMapReduce(String.valueOf(reviewID++), CLASSIFIER)
						.startsWith(POSITIVE)) {
					// send positive classification to the client
					outToClient.writeBytes(POSITIVE);
				} else {
					// send negative classification to the client
					outToClient.writeBytes(NEGATIVE);
				}

			} catch (Exception e) {
				System.out.println(ERROR_ON_SERVER_THREAD
						+ e.getLocalizedMessage());} 
			finally {
				try {
					client.close();
				} catch (Exception ex) {
				}
			}
		}
	}

	//Main function
	public static void main(String args[]) throws IOException {

		@SuppressWarnings("resource")
		ServerSocket server = new ServerSocket(PORT);

		// create classifier from the training data
		Driver.createClassifier(TRAINING_DATA, CLASSIFIER);

		// classify review for each request
		try {
			while (true) {
				// wait for a client request
				Socket p = server.accept();
				System.out.println("Connected");
				// create a new server thread for the current client request
				new Thread(new ServerThread(p)).start();
			}
		} finally {
			// delete classifier folder
			FileUtils.deleteDirectory(new File(CLASSIFIER));
		}
	}
}