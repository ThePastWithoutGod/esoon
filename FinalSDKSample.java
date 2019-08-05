package com.genesys.codesamples.psdk;

import java.awt.*;
import java.awt.event.*;
import java.net.InetAddress;
import java.util.*;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JScrollPane;
import javax.swing.UIManager;

import net.miginfocom.swing.MigLayout;

import com.genesyslab.platform.applicationblocks.com.*;
import com.genesyslab.platform.applicationblocks.com.objects.*;
import com.genesyslab.platform.applicationblocks.com.queries.CfgApplicationQuery;
import com.genesyslab.platform.applicationblocks.commons.Predicate;
import com.genesyslab.platform.applicationblocks.commons.broker.Subscriber;
import com.genesyslab.platform.applicationblocks.warmstandby.*;
import com.genesyslab.platform.commons.collections.*;
import com.genesyslab.platform.commons.connection.configuration.ClientADDPOptions.*;
import com.genesyslab.platform.commons.connection.configuration.PropertyConfiguration;
import com.genesyslab.platform.commons.log.*;
import com.genesyslab.platform.commons.protocol.*;
import com.genesyslab.platform.configuration.protocol.ConfServerProtocol;
import com.genesyslab.platform.configuration.protocol.types.*;
import com.genesyslab.platform.management.protocol.messageserver.*;
import com.genesyslab.platform.reporting.protocol.statserver.events.EventInfo;
import com.genesyslab.platform.voice.protocol.tserver.*;
import com.genesyslab.platform.voice.protocol.tserver.events.*;
import com.genesyslab.platform.voice.protocol.tserver.requests.agent.*;
import com.genesyslab.platform.voice.protocol.tserver.requests.dn.*;
import com.genesyslab.platform.voice.protocol.tserver.requests.party.*;
import com.genesyslab.platform.voice.protocol.tserver.requests.userdata.*;

public class FinalSDKSample extends JFrame {

	/**
	 * Keep the compiler happy
	 */
	private static final long serialVersionUID = -1;

	private static final String APP_NAME = "FinalSDKSample";
    private static final int APP_TYPE = CfgAppType.CFGAgentDesktop.asInteger(); 
    
	private ILogger log;

    private CfgApplication cfgThisApplication;
    private ConfServerProtocol configProtocol;
    private IConfService confService;
    private Subscription confSubscription;

    private PersonPlace personPlace;
    private TServer tServer;
    private StatServer statServer;
    private MessageServer messageServer;
    private LCAServer lcaServer;
    private ConfigServer configServer;
    private Endpoint configEndpoint;
    private WarmStandbyService configWarmStandbyService;

    private ConfigServerChannelListenerHandler configServerChannelListenerHandler = new ConfigServerChannelListenerHandler();
    private ConfServerEventsHandler confServerEventsHandler;
	private StatServerChannelListenerHandler statServerChannelListenerHandler = new StatServerChannelListenerHandler();
	
	/*
	 * GUI Components
	 */
	private JTextArea textAreaLog; // Text Field to show all messages

	private JTextField textFieldHost;
	private JTextField textFieldApplication;
	private JTextField textFieldPlace;
	private JTextField textFieldPort;
	private JTextField textFieldPerson;
	private JTextField textFieldPassword;
	
	private JLabel labelAHTText;
	private JLabel labelAHT;
	
	private JButton buttonConnect;
	private JButton buttonDisconnect;
	private JButton buttonRegister;
	private JButton buttonUnregister;
	private JButton buttonLogin;
	private JButton buttonLogout;
	private JButton buttonNotReady;
	private JButton buttonReady;

	private JTextField textFieldNumberToDial;
	private JButton buttonDialCall;
	private JButton buttonAnswerCall;
	private JButton buttonReleaseCall;

	private JLabel lblNumberToTransfer;
	private JTextField textFieldNumberToTransferTo;
	private JButton btnSingleStepTransfer;
	private JButton btnInitiateTransfer;
	private JButton btnCompleteTransfer;

	private JButton buttonHoldCall;
	private JButton buttonRetrieveCall;

	private JTextField textFieldKey;
	private JTextField textFieldValue;
	private JButton buttonAttachData;

	/*
	 * Private Members to store dynamic information
	 */
	private StatServerInfoEvent statServerInfoEvent = new StatServerInfoEvent(this) {

		private static final long serialVersionUID = 1L;

		@Override
		public void onStatInfoEvent(EventInfo eventInfo) {
			labelAHT.setText(new Double(Double.parseDouble(eventInfo.getStringValue())).toString());
		}
    };
    
    private class StatServerChannelListenerHandler implements ChannelListener {

		@Override
		public void onChannelClosed(ChannelClosedEvent arg0) {
			labelAHT.setText("Not available");
		}

		@Override
		public void onChannelError(ChannelErrorEvent arg0) {
		}

		@Override
		public void onChannelOpened(EventObject arg0) {
		}
    }
    
    private TServerInfoEvent tServerInfoEvent = new TServerInfoEvent(this) {

		private static final long serialVersionUID = 1L;

		@Override
		public void onTServerEvent(TServerEvent tserverEvent) {
			 if (tserverEvent instanceof EventEstablished) {
		            EventEstablished eventEstablished = (EventEstablished)tserverEvent;

		            if (eventEstablished.getCallType() != CallType.Consult) {
		                if (messageServer != null)
		                    messageServer.SendMessage(5050, LogLevel.Info, LogCategory.Audit, "Call made with connid " + tServer.getConnID().toString());
		            }
			 }
		}
    };
    
    private final FinalSDKSample finalSDKSample = this;
    private LCAShutdownEvent lCAShutdownEvent = new LCAShutdownEvent(this) {
    	
    	private static final long serialVersionUID = 1L;

		@Override
		public void onShutdown() {
			log.info("Shutting down due to LCA");
			finalSDKSample.dispose();
		}
    };
    
	/*
	 * Event Handler for all events in the system. Any class that implements
	 * 'Action' interface can server as a handler. The event handling mechanism
	 * while using COM App Block is different from all the other events
	 */
	private class ConfServerEventsHandler implements Subscriber<ConfEvent> {
		
		public void handle(ConfEvent ev) {
			// look at changed application options
	        if (ev.getCfgObject() instanceof CfgDeltaApplication) {
	            CfgDeltaApplication cfgApplication = (CfgDeltaApplication)ev.getCfgObject();
	            ChangeLogging(cfgApplication.getChangedOptions());
	        }
		}

		public Predicate<ConfEvent> getFilter() {
			return null;
		}
	}
	
	// Constructor
	public FinalSDKSample(String title) {
		super(title);
		initializeComponent();
		
		// set up log4j2 which uses log4j2.xml
		Log.setLoggerFactory(new Log4J2LoggerFactoryImpl());
		log = Log.getLogger(FinalSDKSample.class);
		log.info(APP_NAME + " started");
		
		configEndpoint = new Endpoint(textFieldHost.getText(), Integer.parseInt(textFieldPort.getText()));
		configProtocol = new ConfServerProtocol(configEndpoint);
	}

	/*
	 * Private methods
	 */
	private void ChangeLogging(KeyValueCollection changedOptions) {
        log.debug("ChangeLogging: " + changedOptions);

        try {
            if (changedOptions == null ||
                    (!changedOptions.containsKey("Log") && !changedOptions.containsKey("log")))
                return;

            KeyValueCollection logKVPs = changedOptions.getList("log");
            if (logKVPs == null)
                logKVPs = changedOptions.getList("Log");

//            if (logKVPs.Contains("verbose"))
//            {
//                log4net.ILog nativeLog = LogManager.GetLogger(typeof(FinalSDKSample));
//                log4net.Repository.Hierarchy.Logger logger = (log4net.Repository.Hierarchy.Logger)nativeLog.Logger;
//
//                string verbosity = logKVPs["verbose"] as string;
//                log.Debug("Requested level: " + verbosity);
//
//                switch (verbosity.ToLower())
//                {
//                    case "standard":
//                        logger.Level = log4net.Core.Level.Info;
//                        break;
//
//                    case "none":
//                        logger.Level = log4net.Core.Level.Off;
//                        break;
//
//                    default: // "all" will fall thru
//                        logger.Level = log4net.Core.Level.Debug;
//                        break;
//                }
//            }
//
//            String expire = null;
//            String segment = null;
//            String allDirectory = null;
//
//            if (logKVPs.Contains("expire"))
//                expire = logKVPs["expire"] as string;
//
//            if (logKVPs.Contains("segment")) {
//                segment = logKVPs["segment"] as string;
//                if (!segment.EndsWith("MB") && !segment.EndsWith("KB"))
//                    segment += "KB";
//            }
//
//            if (logKVPs.Contains("all"))
//                allDirectory = logKVPs["all"] as string;
//
//            if (string.IsNullOrEmpty(expire) && string.IsNullOrEmpty(segment) && string.IsNullOrEmpty(allDirectory))
//                return;
//
//            log4net.Repository.Hierarchy.Hierarchy root = log4net.LogManager.GetRepository() as log4net.Repository.Hierarchy.Hierarchy;
//            RollingFileAppender rfa = (RollingFileAppender)root.Root.GetAppender("RollingFile");
//            if (rfa == null)
//                return;
//        
//            if (!string.IsNullOrEmpty(expire)) {
//                log.debug("Old MaxAge: " + rfa.MaxAge);
//                log.debug("Old MaxSizeRollBackups: " + rfa.MaxSizeRollBackups);
//
//                if (expire == "false") {
//                    rfa.MaxAge = 0;
//                    rfa.MaxSizeRollBackups = -1;
//                }
//                else if (expire.EndsWith("day")) {
//                    String t = expire.Substring(expire.Length - 3);
//                    rfa.MaxAge = Integer.parseInt(t);
//                    rfa.MaxSizeRollBackups = -1;
//
//                    if (rfa.MaxAge < 1 || rfa.MaxAge > 100) {
//                        rfa.MaxAge = 0;
//                        rfa.MaxSizeRollBackups = 10;
//                    }
//                }
//                else {
//                    rfa.MaxAge = -1;
//                    rfa.MaxSizeRollBackups = Integer.parseInt(expire);
//
//                    if (rfa.MaxSizeRollBackups < 1 || rfa.MaxSizeRollBackups > 1000)
//                    {
//                        rfa.MaxAge = 0;
//                        rfa.MaxSizeRollBackups = 10;
//                    }
//                }
//
//                log.Debug("New MaxAge: " + rfa.MaxAge);
//                log.Debug("New MaxSizeRollBackups: " + rfa.MaxSizeRollBackups);
//            }
//
//            if (!string.IsNullOrEmpty(segment)) {
//                log.Debug("Old MaximumFileSize: " + rfa.MaximumFileSize);
//                rfa.MaximumFileSize = segment;
//                log.Debug("New MaximumFileSize: " + rfa.MaximumFileSize);
//            }
//
//            if (!string.IsNullOrEmpty(allDirectory)) {
//                log.Debug("Old File: " + rfa.File);
//                rfa.File = allDirectory;
//                log.Debug("New File: " + rfa.File);
//            }
//
//            rfa.ActivateOptions();
        }
        catch (Exception e) {
            log.error("ChangeLogging", e);
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
		MigLayout layoutButtons = new MigLayout("",
				"[150.00,grow][134.00,grow][139.00][103.00]",
				"[][][][][][][][][14.00][]");
		JPanel panelButtons = new JPanel(layoutButtons);
		panelButtons.setBackground(Color.LIGHT_GRAY);

		textFieldHost = new JTextField("demosrv");
		panelButtons.add(textFieldHost, "cell 0 0,growx");
		
		textFieldApplication = new JTextField("AgentDesktop_Sample");
		panelButtons.add(textFieldApplication, "cell 0 1,growx");
		
		textFieldPlace = new JTextField("SIP_Server_Place_7039");
		panelButtons.add(textFieldPlace, "cell 0 2,growx");
		
		textFieldPort = new JTextField("2020");
		panelButtons.add(textFieldPort, "cell 1 0,growx");
		
		textFieldPerson = new JTextField("Monique");
		panelButtons.add(textFieldPerson, "cell 1 1,growx");
		
		textFieldPassword = new JTextField("");
		panelButtons.add(textFieldPassword, "cell 1 2,growx");
		
		labelAHTText = new JLabel("AHT");
		panelButtons.add(labelAHTText, "cell 0 3,growx");
		
		labelAHT = new JLabel("Not available");
		panelButtons.add(labelAHT, "cell 1 3,growx");
		
		/*
		 * Connection/Disconnect
		 */
		// Button Connect
		buttonConnect = new JButton("Connnect");
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

		/*
		 * Register/Unregister
		 */
		// Button Register
		buttonRegister = new JButton("Register");
		buttonRegister.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				register();
			}
		});
		panelButtons.add(buttonRegister, "cell 2 1,growx");

		// Button Unregister
		buttonUnregister = new JButton("Unregister");
		buttonUnregister.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				unregister();
			}
		});
		panelButtons.add(buttonUnregister, "cell 3 1,growx");

		/*
		 * Login/Logout
		 */
		// Button Login
		buttonLogin = new JButton("Login");
		buttonLogin.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				login();
			}
		});
		panelButtons.add(buttonLogin, "cell 2 2,growx");

		// Button Logout
		buttonLogout = new JButton("Logout");
		buttonLogout.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				logout();
			}
		});
		panelButtons.add(buttonLogout, "cell 3 2,growx");

		// Button NotReady
		buttonNotReady = new JButton("Not Ready");
		buttonNotReady.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				notReady();
			}
		});
		panelButtons.add(buttonNotReady, "cell 2 4,growx");

		// Button Ready
		buttonReady = new JButton("Ready");
		buttonReady.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ready();
			}
		});
		panelButtons.add(buttonReady, "cell 3 4,growx");
				
		/*
		 * Dial/Answer/Release
		 */
		// TextBox Number
		JLabel lblNumberToDial = new JLabel("Number to Dial:", JLabel.RIGHT);
		panelButtons.add(lblNumberToDial, "cell 0 4");
		textFieldNumberToDial = new JTextField("7007");
		panelButtons.add(textFieldNumberToDial, "cell 0 5,growx");

		// Button Dial Call
		buttonDialCall = new JButton("Dial Call");
		buttonDialCall.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialCall();
			}
		});
		panelButtons.add(buttonDialCall, "cell 1 5,growx");

		// Button Answer Call
		buttonAnswerCall = new JButton("Answer Call");
		buttonAnswerCall.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				answerCall();
			}
		});
		panelButtons.add(buttonAnswerCall, "cell 2 5,growx");

		// Button Release Call
		buttonReleaseCall = new JButton("Release Call");
		buttonReleaseCall.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				releaseCall();
			}
		});
		panelButtons.add(buttonReleaseCall, "cell 3 5,growx");

		/*
		 * Hold/Retrieve
		 */
		// Button Hold Call
		buttonHoldCall = new JButton("Hold Call");
		buttonHoldCall.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				holdCall();
			}
		});
		panelButtons.add(buttonHoldCall, "cell 2 6,growx");

		// Button Retrieve Call
		buttonRetrieveCall = new JButton("Retrieve Call");
		buttonRetrieveCall.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				retreiveCall();
			}
		});
		panelButtons.add(buttonRetrieveCall, "cell 3 6,growx");

		/*
		 * MuteTransfer/InitTransfer/CompleteTransfer
		 */
		lblNumberToTransfer = new JLabel("Number to Transfer to:");
		panelButtons.add(lblNumberToTransfer, "cell 0 7");

		textFieldNumberToTransferTo = new JTextField("7008");
		panelButtons.add(textFieldNumberToTransferTo, "cell 0 8,growx");
		textFieldNumberToTransferTo.setColumns(10);

		btnSingleStepTransfer = new JButton("Single-Step Transfer");
		btnSingleStepTransfer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				singleStepTransfer();
			}
		});
		panelButtons.add(btnSingleStepTransfer, "cell 1 8,growx");

		btnInitiateTransfer = new JButton("Initiate Transfer  ");
		btnInitiateTransfer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				initiateTransfer();
			}
		});
		panelButtons.add(btnInitiateTransfer, "cell 2 8,growx");

		btnCompleteTransfer = new JButton("Complete Transfer");
		btnCompleteTransfer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				completeTransfer();
			}
		});
		panelButtons.add(btnCompleteTransfer, "cell 3 8,growx");

		/*
		 * Attach Data
		 */
		JLabel lblKey = new JLabel("  Key", JLabel.RIGHT);
		panelButtons.add(lblKey, "flowx,cell 0 9,alignx left");

		// TextField AttachData Key
		textFieldKey = new JTextField();
		panelButtons.add(textFieldKey, "cell 0 9,growx");

		JLabel lblValue = new JLabel("  Value", JLabel.RIGHT);
		panelButtons.add(lblValue, "flowx,cell 1 9");

		// TextField AttachData Value
		textFieldValue = new JTextField();
		panelButtons.add(textFieldValue, "cell 1 9 2 1,growx");

		// Base Layout
		getContentPane().add(panelButtons, BorderLayout.NORTH);

		// Button Attach Data
		buttonAttachData = new JButton("Attach Data");
		buttonAttachData.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				attachData();
			}
		});
		panelButtons.add(buttonAttachData, "cell 3 9,growx");
		getContentPane().add(scrollPane, BorderLayout.CENTER);

		setSize(590, 590);
	}

	/*
	 * Methods that use SDK APIs
	 */
	private void connect() {

		log.info("connect- entry");
		
		// Open the connection - only when the connection is not already opened
		// Opening the connection can fail and raise exceptions
		try {
			// attempt to automatically reconnect if disconnected
	        PropertyConfiguration config = new PropertyConfiguration();
	        config.setUseAddp(true);
	        config.setAddpServerTimeout(20);
	        config.setAddpClientTimeout(10);
	        config.setAddpTraceMode(AddpTraceMode.None);

	        configEndpoint = new Endpoint(textFieldHost.getText(), Integer.parseInt(textFieldPort.getText()), config);
	        configProtocol.setEndpoint(configEndpoint);
	        configProtocol.setClientName(textFieldApplication.getText());
	        configProtocol.setClientApplicationType(textFieldApplication.getText().equals("Speechminer") ? CfgAppType.CFGGenericServer.asInteger() : APP_TYPE);
	        configProtocol.setInvoker(new SwingInvoker());
	        
	        configProtocol.setUserName(textFieldPerson.getText());
	        configProtocol.setUserPassword(textFieldPassword.getText());
	        
	        if (confService == null)
	        	confService = ConfServiceFactory.createConfService(configProtocol);

	        WarmStandbyConfiguration warmStandbyConfig = new WarmStandbyConfiguration(configEndpoint, configEndpoint);
	        warmStandbyConfig.setTimeout(5000);
	        warmStandbyConfig.setAttempts((short)2);
	        configWarmStandbyService = new WarmStandbyService(configProtocol);
	        configWarmStandbyService.applyConfiguration(warmStandbyConfig);
	        configWarmStandbyService.start();

	        if (configProtocol.getState() == ChannelState.Closed) {
	            configProtocol.addChannelListener(configServerChannelListenerHandler);
	            configProtocol.beginOpen();
//
//	            ThreadPool.QueueUserWorkItem((obj) =>
//	                {
//	                    Thread.Sleep(5000);
//	                    if (configProtocol.State != ChannelState.Opened)
//	                    {
//	                        log.Error("Could NOT connect to Config Server at host " + textBoxHost.Text + ", port " +
//	                                    textBoxPort.Text + ", app " + textBoxApplication.Text);
//	                        configProtocol.Opened -= OnConfigServerOpened;
//	                        configProtocol.Closed -= OnConfigServerClosed;
//	                    }
//	                });
	        }

		} catch (IllegalStateException | ProtocolException e) {
			logException(e);
		}
		
		log.debug("connect - exit");
	}

	class ConfigServerChannelListenerHandler implements ChannelListener {

		@Override
		public void onChannelClosed(ChannelClosedEvent arg0) {
			log.info("onChannelClosed");
		}

		@Override
		public void onChannelError(ChannelErrorEvent arg0) {
			log.info("onChannelError");
		}

		@Override
		public void onChannelOpened(EventObject arg0) {
			try {
				log.info("Config Server connection is successful");
	            log.info("OnConfigServerOpened - entry");
	            personPlace = new PersonPlace(log, confService);
	
	            if (personPlace.GetPerson(textFieldPerson.getText()) && personPlace.GetPlace(textFieldPlace.getText())) {
	                CfgApplicationQuery applicationQuery = new CfgApplicationQuery(confService);
	                applicationQuery.setName(textFieldApplication.getText());
	                cfgThisApplication = confService.retrieveObject(applicationQuery);
	
	                if (cfgThisApplication == null) {
	                    log.error("Could not find application " + textFieldApplication.getText());
	                    return;
	                }
	
	                applicationQuery.setName(null);
	
	                ChangeLogging(cfgThisApplication.getOptions());
	                RegisterForAppChanges();
	
	                // get connections
	                for (CfgConnInfo connInfo : cfgThisApplication.getAppServers()) {
	                	Integer dbId = connInfo.getAppServerDBID();
	
	                    if (dbId != null) {
	                        applicationQuery.setDbid((int)dbId);
	                        CfgApplication connectedApplication = confService.retrieveObject(applicationQuery);
	
	                        if (connectedApplication == null) // permissions issue?
	                            continue;
	
	                        log.info("connect to " + connectedApplication.getName());
	                        
	                        if (connectedApplication.getAppPrototype().getType() == CfgAppType.CFGStatServer) {
	                            statServer = new StatServer(log, connInfo, connectedApplication, textFieldApplication.getText(), personPlace.getEmployeeID());
	                            statServer.addListener(statServerInfoEvent);
	                            statServer.protocol.addChannelListener(statServerChannelListenerHandler);
	                            statServer.Start();
	                        }
	                        else if (connectedApplication.getAppPrototype().getType() == CfgAppType.CFGMessageServer) {
	                            messageServer = new MessageServer(log, connInfo, connectedApplication, textFieldApplication.getText(), InetAddress.getLocalHost().getHostName(),
	                                cfgThisApplication.getType(), cfgThisApplication.getDBID());
	                            messageServer.Start();
	                        }
	                        else if (connectedApplication.getAppPrototype().getType() == CfgAppType.CFGTServer) {
	                            tServer = new TServer(log, connInfo, connectedApplication, textFieldApplication.getText());
	                            tServer.addListener(tServerInfoEvent);
	                            tServer.Start();
	                        }
	                        else if (connectedApplication.getAppPrototype().getType() == CfgAppType.CFGConfigServer) {
	                            configServer = new ConfigServer(log, connInfo, connectedApplication, configProtocol, configEndpoint);
	                        }
	                    }
	                }
	            }
	
	            lcaServer = new LCAServer(log, confService, textFieldApplication.getText());
	            lcaServer.addListener(lCAShutdownEvent);
	            lcaServer.Start();
			}
			catch (Exception e) {
				log.error("OnConfigServerOpened", e);
			}
			
            log.info("OnConfigServerOpened - exit");
		}
	}
	
	private void disconnect() {
		log.debug("disconnect - entry");

        try
        {
            UnregisterForAppChanges();

            if (tServer != null) {
            	tServer.removeListener(tServerInfoEvent);
                tServer.Stop();
            }

            if (statServer != null) {
            	statServer.removeListener(statServerInfoEvent);
            	statServer.protocol.removeChannelListener(statServerChannelListenerHandler);
                statServer.Stop();
            }

            if (messageServer != null)
                messageServer.Stop();

            if (lcaServer != null) {
            	lcaServer.removeListener(lCAShutdownEvent);
                lcaServer.Stop();
            }

            if (configServer != null)
                configServer.Stop();
            else {
                if (configWarmStandbyService != null && configWarmStandbyService.getState() != WarmStandbyState.OFF)
                    configWarmStandbyService.stop();

                configProtocol.removeChannelListener(configServerChannelListenerHandler);
                configProtocol.beginClose();
            }
        }
        catch (Exception ex) {
            log.error("disconnect", ex);
        }

        log.debug("disconnect - exit");
	}

	private void RegisterForAppChanges() {
        log.debug("RegisterForAppChanges: entry");

        try
        {
            // will cause an exception on a config server reconnect (no obvious way to clear it when disconnected)
        	if (confServerEventsHandler != null) {
        		confServerEventsHandler = new ConfServerEventsHandler();
        		confService.register(confServerEventsHandler);
        	}
        } catch (Exception e) {
            log.info("confService.Register exception: " + e.getMessage());
        }

        try {
            NotificationQuery query = new NotificationQuery();
            query.setObjectDbid(cfgThisApplication.getDBID());
            query.setObjectType(CfgObjectType.CFGApplication);
            query.setTenantDbid(0); // Objects for all tenants
            confSubscription = confService.subscribe(query);
        }
        catch (Exception e) {
            log.error("RegisterForAppChanges", e);
        }

        log.debug("RegisterForAppChanges: exit");
    }
	
	private void UnregisterForAppChanges() {
        log.debug("UnregisterForAppChanges: entry");

        try {
            // issues if this is done while not connected
            if (confService != null && confSubscription != null && confService.getProtocol().getState() == ChannelState.Opened) {
                confService.unsubscribe(confSubscription);
                
                if (confServerEventsHandler != null) {
                	confService.unregister(confServerEventsHandler);
                	confServerEventsHandler = null;
                }
            }
        }
        catch (Exception e) {
            log.error("UnregisterNotificationForObject", e);
        }

        log.debug("UnregisterForAppChanges: exit");
    }
	
	private void register() {
		try {
			RequestRegisterAddress request = RequestRegisterAddress.create(personPlace.getVoiceDN(),
					RegisterMode.ModeShare,
					ControlMode.RegisterDefault,
					AddressType.DN);

			logRequest(request.toString());
			tServer.protocol.send(request);
		} catch (Exception e) {
			logException(e);
		}
	}

	private void unregister() {
		try {
			RequestUnregisterAddress request = RequestUnregisterAddress.create(
					personPlace.getVoiceDN(),
					ControlMode.RegisterDefault);

			logRequest(request.toString());
			tServer.protocol.send(request);
		} catch (Exception e) {
			logException(e);
		}
	}

	private void login() {
		try {
			RequestAgentLogin request = RequestAgentLogin.create(personPlace.getVoiceDN(),
					AgentWorkMode.ManualIn); // SIP Server - Not Ready after login  

			request.setAgentID(personPlace.getSwitchLoginID());

			logRequest(request.toString());
			tServer.protocol.send(request);
		} catch (Exception e) {
			logException(e);
		}
	}

	private void logout() {
		try {
			RequestAgentLogout request = RequestAgentLogout.create(personPlace.getVoiceDN());
			logRequest(request.toString());
			tServer.protocol.send(request);
		} catch (Exception e) {
			logException(e);
		}
	}
	
	private void notReady() {
		try {
			RequestAgentNotReady request = RequestAgentNotReady.create(personPlace.getVoiceDN(), AgentWorkMode.Unknown);
			logRequest(request.toString());
			tServer.protocol.send(request);
		} catch (Exception e) {
			logException(e);
		}
	}

	private void ready() {
		try {
			RequestAgentReady request = RequestAgentReady.create(personPlace.getVoiceDN(), AgentWorkMode.Unknown);
			logRequest(request.toString());
			tServer.protocol.send(request);
		} catch (Exception e) {
			logException(e);
		}
	}

	private void answerCall() {
		try {
			RequestAnswerCall request = RequestAnswerCall.create(personPlace.getVoiceDN(), tServer.getConnID());
			logRequest(request.toString());
			tServer.protocol.send(request);
		} catch (Exception e) {
			logException(e);
		}
	}

	private void releaseCall() {
		try {
			RequestReleaseCall request = RequestReleaseCall.create(personPlace.getVoiceDN(), tServer.getConnID());
			logRequest(request.toString());
			tServer.protocol.send(request);
		} catch (Exception e) {
			logException(e);
		}
	}

	private void dialCall() {
		try {
			RequestMakeCall request = RequestMakeCall.create(personPlace.getVoiceDN(),
					textFieldNumberToDial.getText(),
					MakeCallType.Regular);

			logRequest(request.toString());
			tServer.protocol.send(request);
		} catch (Exception e) {
			logException(e);
		}
	}

	private void holdCall() {
		try {
			RequestHoldCall request = RequestHoldCall.create(personPlace.getVoiceDN(), tServer.getConnID());
			logRequest(request.toString());
			tServer.protocol.send(request);
		} catch (Exception e) {
			logException(e);
		}
	}

	private void retreiveCall() {
		try {
			RequestRetrieveCall request = RequestRetrieveCall.create(personPlace.getVoiceDN(), tServer.getConnID());
			logRequest(request.toString());
			tServer.protocol.send(request);
		} catch (Exception e) {
			logException(e);
		}
	}

	private void singleStepTransfer() {
		try {
			RequestSingleStepTransfer request = RequestSingleStepTransfer.create(
					personPlace.getVoiceDN(),
					tServer.getConnID(),
                    textFieldNumberToTransferTo.getText());

			logRequest(request.toString());
			tServer.protocol.send(request);
		} catch (Exception e) {
			logException(e);
		}
	}

	private void initiateTransfer() {
		try {
			
			RequestInitiateTransfer request = RequestInitiateTransfer.create(
					personPlace.getVoiceDN(),
					tServer.getConnID(),
                    textFieldNumberToTransferTo.getText());

			logRequest(request.toString());
			tServer.protocol.send(request);
		} catch (Exception e) {
			logException(e);
		}
	}

	private void completeTransfer() {
		try {
			RequestCompleteTransfer request = RequestCompleteTransfer.create(
					personPlace.getVoiceDN(),
					tServer.getConnID(),
					tServer.getConsultConnID());

			logRequest(request.toString());
			tServer.protocol.send(request);
		} catch (Exception e) {
			logException(e);
		}
	}

	private void attachData() {
		try {
			KeyValueCollection attachData = new KeyValueCollection();
			attachData.addString(textFieldKey.getText(),
					textFieldValue.getText());
			attachData.addString("first_name", "Malcolm");
            attachData.addString("last_name", "X");
            attachData.addInt("age", 40);

			RequestUpdateUserData request = RequestUpdateUserData.create(
					personPlace.getVoiceDN(),
					tServer.getConnID(),
					attachData);

			logRequest(request.toString());
			tServer.protocol.send(request);
		} catch (Exception e) {
			logException(e);
		}
	}

	public void dispose() {
		disconnect();
		super.dispose();
	}

	private void logRequest(final String toLog) {
		textAreaLog.setText(textAreaLog.getText().concat(
				"<< OUTGOING \n\n" + toLog
						+ "\n************************************\n"));
	}

	private void logException(final Exception e) {
		textAreaLog.setText(textAreaLog.getText().concat(
				"Exception: \n" + e.getMessage()
						+ "\n************************************\n"));
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
				final JFrame frame = new FinalSDKSample(
						"Platform SDK Sample - Final");

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
