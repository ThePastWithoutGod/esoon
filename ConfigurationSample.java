package com.genesys.codesamples.psdk;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import net.miginfocom.swing.MigLayout;

import com.genesyslab.platform.applicationblocks.com.*;
import com.genesyslab.platform.applicationblocks.com.objects.*;
import com.genesyslab.platform.applicationblocks.com.queries.*;
import com.genesyslab.platform.applicationblocks.commons.*;
import com.genesyslab.platform.applicationblocks.commons.broker.*;
import com.genesyslab.platform.applicationblocks.warmstandby.WarmStandbyConfiguration;
import com.genesyslab.platform.applicationblocks.warmstandby.WarmStandbyService;
import com.genesyslab.platform.commons.collections.*;
import com.genesyslab.platform.commons.connection.configuration.PropertyConfiguration;
import com.genesyslab.platform.commons.connection.configuration.ClientADDPOptions.AddpTraceMode;
import com.genesyslab.platform.commons.protocol.*;
import com.genesyslab.platform.configuration.protocol.ConfServerProtocol;
import com.genesyslab.platform.configuration.protocol.types.*;

public class ConfigurationSample extends JFrame {

	/**
	 * Keep the compiler happy 
	 */
	private static final long serialVersionUID = -1;
	
	/*
	 * Constants These constants must be set according to your environment and
	 * client.
	 */
	private static final String HOST = "demosrv";
	private static final int PORT = 2020;
	
	private static final String BACKUP_HOST = HOST;
	private static final int BACKUP_PORT = PORT;

	private static final String CLIENT_NAME = "AgentDesktop_Sample";
	private static final int TENANT_ID = 1;
	private static final String USER_NAME = "default";
	private static final String PASSWORD = "password";

	/*
	 * GUI Components
	 */
	private JTextArea textAreaLog; // Text Field to show all messages

	private JButton buttonConnect;
	private JButton buttonDisconnect;
	private JButton btnRegisterNotificationForObject;
	private JButton btnUnregisterNotificationForObject;
	private JButton btnRegisterNotification;
	private JButton btnUnregisterNotification;
	private JLabel lblObjectType;
	private JLabel lblObjectDbid;
	private JTextField textFieldObjectDBID;
	private JButton btnReadObjects;
	private JButton btnReadPerson;
	private JButton btnCreateSkill;
	private JButton btnAddSkillToPerson;
	private JLabel lblSkillName;
	private JTextField textFieldSkillName;
	private JButton btnDeleteSkill;
	private JTextField textFieldPersonName;
	private JLabel lblPersonName;
	private JComboBox<String> comboBoxObjectType;

	/*
	 * Private Members - For Genesys AppBlocks used (Protocol Manager and
	 * Message Broker)
	 */
	private ConfServerProtocol protocol;
	private WarmStandbyService warmStandbyService;
	private IConfService confService;

	private Subscription subscriptionForAll;
	private Subscription subscriptionForObject;

	/*
	 * Event Handler for all events in the system. Any class that implements
	 * 'Action' interface can server as a handler. The event handling mechanism
	 * while using COM App Block is different from all the other events
	 */
	class ConfServerEventsHandler implements Subscriber<ConfEvent> {
		
		public void handle(final Message message) {
			logMessage("Incoming Message: " + message);
		}

		public void handle(ConfEvent ev) {
			logMessage("Incoming Message: " + ev.toString());
			
			// look at changed application options
			if (ev.getCfgObject() instanceof CfgDeltaApplication) {
				CfgDeltaApplication cfgApplication = (CfgDeltaApplication)ev.getCfgObject();
				KeyValueCollection kvc = cfgApplication.getChangedOptions();
				
				if (kvc != null) {
					for (Object o : kvc) {
						KeyValuePair kvp = (KeyValuePair)o;
						logMessage("key " + kvp.getStringKey() + "=" + kvp.getValue());
					}
				}
			}
		}

		public Predicate<ConfEvent> getFilter() {
			return null;
		}
	}

	// Constructor
	public ConfigurationSample(String title) {
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
		Endpoint backupEndpoint = new Endpoint(BACKUP_HOST, BACKUP_PORT, config);

		protocol = new ConfServerProtocol(endpoint);
		protocol.setClientName(CLIENT_NAME);
		protocol.setUserName(USER_NAME);
		protocol.setUserPassword(PASSWORD);
		protocol.setClientApplicationType(CfgAppType.CFGAgentDesktop.ordinal());

		// Setup Warm-Standby
		WarmStandbyConfiguration warmStandbyConfig = new WarmStandbyConfiguration(
				endpoint, backupEndpoint);
		warmStandbyConfig.setTimeout(5000);
		warmStandbyConfig.setAttempts((short) 2);

		warmStandbyService = new WarmStandbyService(protocol);
		warmStandbyService.applyConfiguration(warmStandbyConfig);
		warmStandbyService.start();

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

			public void onMessage(Message arg0) {
				logMessage("Incoming Message: " + arg0);
			}
		});

		confService = ConfServiceFactory.createConfService(protocol);
		confService.register(new ConfServerEventsHandler());
	}

	private void finalizePSDKProtocolAndAppBlocks() {
		// Stop WarmStandby application block
		warmStandbyService.stop();

		if (protocol.getState() == ChannelState.Opened) {
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
		MigLayout layoutButtons = new MigLayout("", "[112.00][99.00][][]",
				"[][][][][6.00][][][6.00][][][][6.00][][]");
		JPanel panelButtons = new JPanel(layoutButtons);
		panelButtons.setBackground(Color.LIGHT_GRAY);

		// Button Connect
		buttonConnect = new JButton("Connect");
		buttonConnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				connect();
			}
		});
		panelButtons.add(buttonConnect, "cell 2 0,growx");

		// Button Disconnect
		buttonDisconnect = new JButton("Disconnect");
		buttonDisconnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				disconnect();
			}
		});
		panelButtons.add(buttonDisconnect, "cell 3 0,growx");

		btnRegisterNotification = new JButton("Register Notification (All)");
		btnRegisterNotification.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				registerNotification();
			}
		});
		panelButtons.add(btnRegisterNotification, "cell 2 1,growx");

		btnUnregisterNotification = new JButton("Unregister Notification (All)");
		btnUnregisterNotification.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				unregisterNotification();
			}
		});
		panelButtons.add(btnUnregisterNotification, "cell 3 1,growx");

		lblObjectType = new JLabel("Type");
		panelButtons.add(lblObjectType, "flowx,cell 0 2");
		comboBoxObjectType = new JComboBox<String>();
		comboBoxObjectType.setModel(new DefaultComboBoxModel<String>(new String[] {
				"CFGNoObject", "CFGSwitch", "CFGDN", "CFGPerson", "CFGPlace",
				"CFGAgentGroup", "CFGPlaceGroup", "CFGTenant", "CFGService",
				"CFGApplication", "CFGHost", "CFGPhysicalSwitch", "CFGScript",
				"CFGSkill", "CFGActionCode", "CFGAgentLogin", "CFGTransaction",
				"CFGDNGroup", "CFGStatDay", "CFGStatTable", "CFGAppPrototype",
				"CFGAccessGroup", "CFGFolder", "CFGField", "CFGFormat",
				"CFGTableAccess", "CFGCallingList", "CFGCampaign",
				"CFGTreatment", "CFGFilter", "CFGTimeZone", "CFGVoicePrompt",
				"CFGIVRPort", "CFGIVR", "CFGAlarmCondition", "CFGEnumerator",
				"CFGEnumeratorValue", "CFGObjectiveTable", "CFGCampaignGroup",
				"CFGGVPReseller", "CFGGVPCustomer", "CFGGVPIVRProfile",
				"CFGScheduledTask", "CFGRole", "CFGPersonLastLogin ",
				"CFGMaxObjectType" }));
		comboBoxObjectType.setSelectedIndex(3);
		panelButtons.add(comboBoxObjectType, "cell 0 2,growx,aligny center");

		lblObjectDbid = new JLabel("DBID");
		panelButtons.add(lblObjectDbid, "flowx,cell 1 2,growx");
		textFieldObjectDBID = new JTextField();
		textFieldObjectDBID.setText("119");
		panelButtons.add(textFieldObjectDBID, "cell 1 2");
		textFieldObjectDBID.setColumns(10);

		btnRegisterNotificationForObject = new JButton("Register Notification");
		btnRegisterNotificationForObject
				.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						registerNotificationForObject();
					}
				});
		panelButtons.add(btnRegisterNotificationForObject, "cell 2 2,growx");

		btnUnregisterNotificationForObject = new JButton(
				"Unregister Notification");
		btnUnregisterNotificationForObject
				.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						unregisterNotificationForObject();
					}
				});
		panelButtons.add(btnUnregisterNotificationForObject, "cell 3 2,growx");

		btnReadPerson = new JButton("Read Person");
		btnReadPerson.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				readPerson();
			}
		});

		btnReadObjects = new JButton("Read Object(s)");
		btnReadObjects.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				readObjects();
			}
		});
		panelButtons.add(btnReadObjects, "cell 2 3,growx");

		lblPersonName = new JLabel("Person Name");
		panelButtons.add(lblPersonName, "cell 0 8,alignx trailing");

		textFieldPersonName = new JTextField("kmilburn");
		panelButtons.add(textFieldPersonName, "cell 1 8,growx");
		textFieldPersonName.setColumns(10);
		panelButtons.add(btnReadPerson, "cell 2 8,growx");

		lblSkillName = new JLabel("Skill Name");
		panelButtons.add(lblSkillName, "cell 0 9,alignx trailing");

		textFieldSkillName = new JTextField();
		textFieldSkillName.setText("French");
		panelButtons.add(textFieldSkillName, "cell 1 9,growx");
		textFieldSkillName.setColumns(10);

		btnCreateSkill = new JButton("Create Skill");
		btnCreateSkill.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				createSkill();
			}
		});
		panelButtons.add(btnCreateSkill, "cell 2 9,growx");

		btnDeleteSkill = new JButton("Delete Skill");
		btnDeleteSkill.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				deleteSkill();
			}
		});
		panelButtons.add(btnDeleteSkill, "cell 3 9,growx");

		btnAddSkillToPerson = new JButton("Update Skill to Person");
		btnAddSkillToPerson.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addSkillToPerson();
			}
		});
		panelButtons.add(btnAddSkillToPerson, "cell 2 10,growx");

		// Base Layout
		getContentPane().add(panelButtons, BorderLayout.NORTH);
		getContentPane().add(scrollPane, BorderLayout.CENTER);

		this.setMinimumSize(new Dimension(655, 550));
	}

	private void connect() {
		// Open the connection - only when the connection is not already opened
		// Opening the connection can fail and raises an exception
		try {
			if (protocol.getState() == ChannelState.Closed)
				protocol.beginOpen(); 	// attempt to open the channel
										// asynchronously
		} catch (ProtocolException e) {
			logException(e);
		} catch (IllegalStateException e) {
			logException(e);
		}
	}

	private void disconnect() {
		try {
			// Close if protocol not already closed
			if (protocol.getState() == ChannelState.Opened) {
				// Close the connection asynchronously
				protocol.beginClose();
			}
		} catch (Exception e) {
			logException(e);
		}
	}

	private void readObjects() {
		// Need to iterate all object types
		try {
			Collection<CfgObjectType> objectTypeCollection = CfgObjectType.values();

			for (CfgObjectType objectType : objectTypeCollection) {
				if ((objectType != CfgObjectType.CFGNoObject)
						&& (objectType != CfgObjectType.CFGMaxObjectType)) {
					CfgFilterBasedQuery<?> query = new CfgFilterBasedQuery<>(
							objectType);
					Collection<ICfgObject> objects = confService
							.retrieveMultipleObjects(ICfgObject.class, query);

					if (objects != null) {
						logResponse("Objects Read: Type [" + objectType.name()
								+ "], Count [" + objects.size() + "]");
					} else {
						logResponse("Objects Read: Type [" + objectType.name()
								+ "], Count [0]");
					}
				}
			}
		} catch (Exception e) {
			logException(e);
		}
	}

	private void readPerson() {
		try {
			// TODO Configuration Platform SDK - Exercise 1
			CfgPersonQuery query = new CfgPersonQuery(confService);
			query.setUserName(textFieldPersonName.getText());
			CfgPerson person = confService.retrieveObject(query);
			logResponse(person.toString());
		} catch (Exception e) {
			logException(e);
		}
	}

	private void createSkill() {
		try {
			// TODO Configuration Platform SDK - Exercise 2
			CfgSkill skill = new CfgSkill(confService);
			skill.setTenantDBID(TENANT_ID); // default Tenant
			skill.setName(textFieldSkillName.getText());
			skill.save();
			logResponse("Skill: " + textFieldSkillName.getText() + " created");
		} catch (Exception e) {
			logException(e);
		}
	}

	private void addSkillToPerson() {
		// Add Skill To Person
		try {
			// retrieve the skill and person given through their DBIDs.
			CfgPersonQuery personQuery = new CfgPersonQuery();
			personQuery.setUserName(textFieldPersonName.getText());

			CfgPerson person = confService.retrieveObject(personQuery);

			// retrieve the skill and person given through their DBIDs.
			CfgSkillQuery skillQuery = new CfgSkillQuery();
			skillQuery.setName(textFieldSkillName.getText());

			CfgSkill skillFromQuery = confService.retrieveObject(skillQuery);
			int skillLevel = new Random().nextInt(10);
            boolean found = false;

            // update?
            for (CfgSkillLevel csl : person.getAgentInfo().getSkillLevels()) {
                if (csl.getSkill().getName() == skillQuery.getName()) {
                    csl.setLevel(skillLevel);
                    found = true;
                    break;
                }
            }

            // or add?
            if (!found) {
                CfgSkillLevel cfgSkillLevel = new CfgSkillLevel(confService, person);
                cfgSkillLevel.setSkill(skillFromQuery);
                cfgSkillLevel.setLevel(skillLevel);
                person.getAgentInfo().getSkillLevels().add(cfgSkillLevel);
            }
            
			person.save();
			logResponse("Skill: " + + skillFromQuery.getDBID() + ", " + textFieldSkillName.getText()
					+ " added to Person: " + textFieldPersonName.getText());
		} catch (Exception e) {
			logException(e);
		}
	}

	private void deleteSkill() {
		try {
			// TODO Configuration Platform SDK - Exercise 5
			CfgSkillQuery query = new CfgSkillQuery();
			query.setName(textFieldSkillName.getText());
			ICfgObject objectToDelete = confService.retrieveObject(
					ICfgObject.class, query);
			objectToDelete.delete();
			logResponse("Skill: " + textFieldSkillName.getText() + " deleted");
		} catch (Exception e) {
			logException(e);
		}
	}

	private void registerNotification() {
		try {
			NotificationQuery query = new NotificationQuery();
			query.setObjectDbid(0);
			query.setObjectType(CfgObjectType.CFGNoObject);
			query.setTenantDbid(0);
			subscriptionForAll = confService.subscribe(query);
			logResponse("registerNotification: " + subscriptionForAll);
		} catch (Exception e) {
			logException(e);
		}
	}

	private void registerNotificationForObject() {
		try {
			NotificationQuery query = new NotificationQuery();

			query.setObjectDbid(Integer.parseInt(textFieldObjectDBID.getText()));
			query.setObjectType(ConvertNameToObjectType(comboBoxObjectType
					.getSelectedItem().toString()));
			query.setTenantDbid(0);  // Objects for all tenants

			subscriptionForObject = confService.subscribe(query);
			logResponse("registerNotificationForObject: " + subscriptionForObject);
		} catch (Exception e) {
			logException(e);
		}
	}

	private void unregisterNotification() {
		try {
			confService.unsubscribe(subscriptionForAll);
		} catch (Exception e) {
			logException(e);
		}
	}

	private void unregisterNotificationForObject() {
		try {
			confService.unsubscribe(subscriptionForObject);
		} catch (Exception e) {
			logException(e);
		}
	}

	private CfgObjectType ConvertNameToObjectType(String objectTypeName) {

		CfgObjectType objectType = (CfgObjectType) CfgObjectType.getValue(
				CfgObjectType.class, objectTypeName);

		if (objectType != null) {
			return objectType;
		} else {
			return CfgObjectType.CFGNoObject;
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
						">> INCOMING \n\n" + message
								+ "\n************************************\n"));
			}
		});
	}

	private void logResponse(final String toLog) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				textAreaLog.setText(textAreaLog.getText().concat(
						">> INCOMING \n\n" + toLog
								+ "\n************************************\n"));
			}
		});
	}

	private void logException(final Exception e) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				textAreaLog.setText(textAreaLog.getText().concat(
						"Exception: \n\n" + e.getMessage()
								+ "\n************************************\n"));
			}
		});
	}

	public static void main(String[] args) {
		try {
			UIManager
					.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (Exception e) {
			e.printStackTrace();
		}

		EventQueue.invokeLater(new Runnable() {

			public void run() {
				final JFrame frame = new ConfigurationSample(
						"Platform SDK Sample - Configuration Sample");

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
