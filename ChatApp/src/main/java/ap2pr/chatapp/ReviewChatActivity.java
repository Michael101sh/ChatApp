/* Michael Shachar
   Ido Ben El
   Michal Bar Ilan
   Betzalel Moshkovitz */

package ap2pr.chatapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import java.io.*;
import java.net.Socket;

public class ReviewChatActivity extends ChatActivity {
    private String messageFromServer = null;
    private String reviewMessage;
    private String ipAddress;
    private int port;
    String username = "bestapp";
    String password = "chatapp";
    private Thread t;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        chatText.setHint("Enter review here and wait for classification");
        Button buttonLeave = (Button) findViewById(R.id.leave_btn);
        buttonLeave.setVisibility(View.INVISIBLE);
    }

    @Override
    protected boolean sendChatMessage() {
        final String request = username + "," + password + "\n";
        ipAddress = getResources().getString(R.string.ip);
        port = getResources().getInteger(R.integer.port);
        reviewMessage = chatText.getText().toString();
        reviewMessage = reviewMessage.replaceAll("\n", "\\n");
        super.sendChatMessage();
        final ReviewChatActivity self = this;
        t = new Thread(new Runnable() {
            @Override
            public void run() {
                Socket clientSocket = null;
                DataInputStream inFromServer = null;
                DataOutputStream outToServer = null;
                try {
                    clientSocket = new Socket(ipAddress, port);
                    inFromServer = new DataInputStream(clientSocket.getInputStream());
                    outToServer = new DataOutputStream(clientSocket.getOutputStream());

                    //send user and password and get authentication response
                    outToServer.writeBytes(request);
                    String response = inFromServer.readLine();
                    //authentication succeeded
                    if (response.equals("true")) {
                        // check whether the review is in english
                        if (!StringUtils.isPureAscii(reviewMessage)) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    chatText.setText("Review should be in english only." +
                                            " Enter new review!");
                                    ReviewChatActivity.super.sendChatMessage();
                                }
                            });
                            return;
                        }
                        // check whether the review consist at least 100 characters
                        if (reviewMessage.split("[\\W]").length >= 100) {
                            outToServer.writeBytes(reviewMessage + '\n');
                            messageFromServer = inFromServer.readLine();
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    chatText.setText("Review should consist at least 100 words!");
                                    ReviewChatActivity.super.sendChatMessage();
                                }
                            });
                            return;
                        }
                    }
                    // if get to catch - connection failed
                } catch (IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            chatText.setText("There is no connection with the TCP server.");
                            ReviewChatActivity.super.sendChatMessage();
                        }
                    });
                    e.printStackTrace();
                } finally {
                    try {
                        if (inFromServer != null) {
                            inFromServer.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        if (outToServer != null) {
                            outToServer.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        if (clientSocket != null) {
                            clientSocket.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                // call callback
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        self.callback(messageFromServer);
                    }
                });
                }
        });
        t.start();
        return true;
    }

    public void callback(final String messageFromServer) {
        try {
            t.join();
        } catch (Exception e) {

        }
        if (messageFromServer != null) {
            chatText.setText(messageFromServer);
            ReviewChatActivity.super.sendChatMessage();
        }

    }
}