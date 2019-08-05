package com.genesys.codesamples.psdk;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.EventObject;

import javax.swing.*;

import net.miginfocom.swing.MigLayout;

import com.genesyslab.platform.commons.collections.KeyValueCollection;
import com.genesyslab.platform.commons.connection.configuration.ClientADDPOptions.AddpTraceMode;
import com.genesyslab.platform.commons.connection.configuration.PropertyConfiguration;
import com.genesyslab.platform.commons.protocol.*;
import com.genesyslab.platform.contacts.protocol.UniversalContactServerProtocol;
import com.genesyslab.platform.contacts.protocol.contactserver.*;
import com.genesyslab.platform.contacts.protocol.contactserver.events.*;
import com.genesyslab.platform.contacts.protocol.contactserver.requests.*;
import com.genesyslab.platform.openmedia.protocol.InteractionServerProtocol;
import com.genesyslab.platform.openmedia.protocol.interactionserver.InteractionClient;
import com.genesyslab.platform.openmedia.protocol.interactionserver.requests.interactionmanagement.RequestSubmit;

public class ContactSample extends JFrame {

	/**
	 * Keep the compiler happy
	 */
	private static final long serialVersionUID = -1;

	private static final String HOST = "demosrv";
	private static final int PORT = 4400;
	private static final int IXN_SVR_PORT = 4420;
	
	private static final String CLIENT_NAME = "ContactExample";
	private static final String IXN_CLIENT_NAME = "ContactExample";
    private static final int TENANT_ID = 1;
    private static final String MEDIA_TYPE = "email";
    private static final String QUEUE = "Email In";
    private static final String INTERACTION_TYPE = "Inbound";
    private static final String INTERACTION_SUBTYPE = "InboundNew";
    private static final String FROM_ADDRESS = "thompson@demosrv.genesyslab.com";
    private static final String TO_ADDRESS = "info@premier.com";
    private static final String CONTACT_ID = "0008Va76MGEC000Q";
    
	/*
	 * GUI Components
	 */
	private JTextArea textAreaLog;
	private JButton buttonConnect;
	private JButton buttonDisconnect;
	private JButton buttonSearch;
	private JButton buttonSubmit;
	private JTextField textBoxMessage;
	private JLabel lblMessage;

	private UniversalContactServerProtocol protocol;
	private InteractionServerProtocol ixnServerProtocol;

	// Constructor
	public ContactSample(String title) {
		super(title);
		initializeComponent();
		initializePSDKProtocolAndAppBlocks();
	}

	private void initializePSDKProtocolAndAppBlocks() {
		PropertyConfiguration config = new PropertyConfiguration();
		config.setUseAddp(true);
		config.setAddpServerTimeout(20);
		config.setAddpClientTimeout(10);
		config.setAddpTraceMode(AddpTraceMode.Both);

		Endpoint endpoint = new Endpoint(HOST, PORT, config);

		protocol = new UniversalContactServerProtocol(endpoint);
		protocol.setClientName(CLIENT_NAME);

		// Define an anonymous channel listener class
		// Alternatively you can implement the ChannelListener interface
		// separately
		// and pass an instance of that class here
		protocol.addChannelListener(new ChannelListener() {

			public void onChannelClosed(ChannelClosedEvent arg0) {
				logMessage("Channel Closed: " + arg0.toString());
			}

			public void onChannelError(ChannelErrorEvent arg0) {
				logMessage("Channel Error: " + arg0.toString());
			}

			public void onChannelOpened(EventObject arg0) {
				logMessage("Channel Opened: " + arg0.toString());
			}
		});
		
		// Define an anonymous message handler class
		// Alternatively you can implement the MessageHandler interface
		// separately
		// and pass an instance of that class here
		protocol.setMessageHandler(new MessageHandler() {

			public void onMessage(Message message) {
				logMessage("Incoming Message: " + message);
				
				// send the ixn to the interaction server...
                // need to link the interaction id of the UCS record to the one in interaction server.
                if (message.messageId() == EventInsertInteraction.ID)
                {
                    EventInsertInteraction eii = (EventInsertInteraction)message;
                    SubmitToIxnServer(eii.getInteractionId());
                }
			}
		});
		
		Endpoint ixnSverEndpoint = new Endpoint(HOST, IXN_SVR_PORT, config);
        ixnServerProtocol = new InteractionServerProtocol(ixnSverEndpoint);
        ixnServerProtocol.setClientName(IXN_CLIENT_NAME);
        ixnServerProtocol.setClientType(InteractionClient.MediaServer);
        
        ixnServerProtocol.setMessageHandler(new MessageHandler() {
			public void onMessage(Message message) {
				logMessage("Incoming Message: " + message);
			}
		});
	}

    private void submit() {
        try
        {
            // Set common interaction attributes
            InteractionAttributes attributes = new InteractionAttributes();
            attributes.setTenantId(TENANT_ID);
            attributes.setMediaTypeId(MEDIA_TYPE);
            attributes.setTypeId(INTERACTION_TYPE);
            attributes.setSubtypeId(INTERACTION_SUBTYPE);
            attributes.setStatus(Statuses.New);
            attributes.setContactId(CONTACT_ID);
            attributes.setSubject(textBoxMessage.getText());
            attributes.setEntityTypeId(EntityTypes.EmailIn);

            InteractionContent content = new InteractionContent();
            content.setText("Some email content goes here...");

            EmailInEntityAttributes emailAttributes = new EmailInEntityAttributes();
            emailAttributes.setFromAddress(FROM_ADDRESS);
            emailAttributes.setToAddresses(TO_ADDRESS);
            emailAttributes.setMailbox(TO_ADDRESS);
            
            RequestInsertInteraction request = RequestInsertInteraction.create();
            request.setEntityAttributes(emailAttributes);
            request.setInteractionAttributes(attributes);
            request.setInteractionContent(content);

            logRequest(request.toString());
            protocol.send(request);
        }
        catch (ProtocolException e)
        {
            logException(e);
        }
    }

    private void SubmitToIxnServer(String interactionId)
    {
        try
        {
            // Exercise: add attachment
            RequestAddDocument requestAD = new RequestAddDocument();
            requestAD.setInteractionId(interactionId);
            requestAD.setMimeType("text/plain");
            requestAD.setTheName("web.config");
        
            RandomAccessFile f = new RandomAccessFile("C:\\Users\\Administrator\\Documents\\ReleaseNotes\\web.config", "r");
            long longlength = f.length();
            int length = (int) longlength;
            byte[] bytes = new byte[length];
            f.readFully(bytes);
            f.close();

            requestAD.setContent(bytes);
            requestAD.setTheSize(bytes.length);
            protocol.request(requestAD);
            logRequest(requestAD.toString());

            // send to ixn server
            RequestSubmit request = RequestSubmit.create();
            request.setTenantId(TENANT_ID);
            request.setMediaType(MEDIA_TYPE);
            request.setQueue(QUEUE);
            request.setInteractionType(INTERACTION_TYPE);
            request.setInteractionSubtype(INTERACTION_SUBTYPE);
            request.setInteractionId(interactionId);

            // Prepare the message to send. It is inserted in the request as UserData
            KeyValueCollection userData = new KeyValueCollection();
            userData.addString("Subject", textBoxMessage.getText());
            userData.addString("Mailbox", TO_ADDRESS);
            userData.addString("FromAddress", FROM_ADDRESS);
            userData.addString("ContactId", CONTACT_ID);
            userData.addString("ServiceType", "WebSupport");
            request.setUserData(userData);

            logRequest(request.toString());
            ixnServerProtocol.send(request);
        }
        catch (ProtocolException | IOException e) {
            logException(e);
        }
    }
    
    private void search() {
    	// Exercise 1
        try
        {
            RequestGetContacts request = RequestGetContacts.create(); // can also use RequestContactListGet
            SearchCriteriaCollection searchCriteria = new SearchCriteriaCollection();
            SimpleSearchCriteria item = new SimpleSearchCriteria();
            item.setAttrName("FirstName");
            item.setAttrValue(textBoxMessage.getText());
            item.setOperator(Operators.Like);
            searchCriteria.add(item);
            request.setSearchCriteria(searchCriteria);
            request.setTenantId(TENANT_ID);

            logRequest(request.toString());
            protocol.send(request);
        }
        catch (ProtocolException e)
        {
            logException(e);
        }
    }
    
	private void finalizePSDKProtocolAndAppBlocks() {
		if (protocol.getState() == ChannelState.Opened) // Close only if the
														// protocol state is
														// opened
		{
			try {
				protocol.close();
			} catch (Exception e) {
				logException(e);
			}
		}
	}

	private void initializeComponent() {

		textAreaLog = new JTextArea();
		textAreaLog.setLineWrap(true);
		textAreaLog.setFont(new Font("Courier New", Font.PLAIN, 11));
		textAreaLog.setAutoscrolls(true);

		JScrollPane scrollPane = new JScrollPane(textAreaLog,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		MigLayout layoutButtons = new MigLayout("", "[25%]", "");
		JPanel panelButtons = new JPanel(layoutButtons);
		panelButtons.setBackground(Color.LIGHT_GRAY);

		// Button Connect
		buttonConnect = new JButton("Connect");
		buttonConnect.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				connect();
			}
		});
		panelButtons.add(buttonConnect, "cell 0 0, growx");

		// Button Disconnect
		buttonDisconnect = new JButton("Disconnect");
		buttonDisconnect.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				disconnect();
			}
		});
		panelButtons.add(buttonDisconnect, "cell 1 0, growx");
		
		// Button Submit
		buttonSubmit = new JButton("Submit");
		buttonSubmit.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				submit();
			}
		});
		panelButtons.add(buttonSubmit, "cell 2 0, growx");
		
		// Button Search
		buttonSearch = new JButton("Search");
		buttonSearch.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				search();
			}
		});
		panelButtons.add(buttonSearch, "cell 3 0, growx");

		lblMessage = new JLabel("Message/Search");
		panelButtons.add(lblMessage, "cell 0 2,alignx right");

		textBoxMessage = new JTextField("");
		panelButtons.add(textBoxMessage, "cell 1 2 2,growx");
		
		/*
		 * Attach Data
		 */

		// Base Layout
		getContentPane().add(panelButtons, BorderLayout.NORTH);
		getContentPane().add(scrollPane, BorderLayout.CENTER);

		setSize(590, 591);
	}

	private void connect() {
		// Open the connection - only when the connection is not already opened
		// Opening the connection can fail and raises an exception
		try {
			if (protocol.getState() == ChannelState.Closed)
				protocol.beginOpen();

			if (ixnServerProtocol.getState() == ChannelState.Closed)
				ixnServerProtocol.beginOpen();

		} catch (ProtocolException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}
	}

	private void disconnect() {
		try {
			// Close if protocol not already closed
			if (protocol.getState() == ChannelState.Opened)
				protocol.beginClose();

			if (ixnServerProtocol.getState() == ChannelState.Opened)
				ixnServerProtocol.beginClose();
				
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void dispose() {
		// close connections, stop threads and release resources
		finalizePSDKProtocolAndAppBlocks();
		super.dispose();
	}

	private void logMessage(final String message) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				textAreaLog.setText(textAreaLog.getText().concat(
						"Message Received: \n" + message
								+ "\n************************************\n"));
			}
		});
	}

	private void logRequest(final String toLog) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				textAreaLog.setText(textAreaLog.getText().concat(
						"Request: \n" + toLog
								+ "\n************************************\n"));
			}
		});
	}

	private void logException(final Exception e) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				textAreaLog.setText(textAreaLog.getText().concat(
						"Exception: \n" + e.getMessage()
								+ "\n************************************\n"));
			}
		});
	}

	public static void main(String[] args) {
		try {
			UIManager
					.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}

		EventQueue.invokeLater(new Runnable() {

			public void run() {
				final JFrame frame = new ContactSample(
						"Platform SDK Sample - Contact");

				frame.addWindowListener(new WindowAdapter() {

					public void windowClosing(WindowEvent we) {
						frame.dispose();
						System.exit(0);
					}
				});

				frame.setVisible(true);
			}
		});
	}
}
