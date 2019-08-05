package com.genesys.codesamples.psdk;

import java.util.Collection;

import com.genesyslab.platform.applicationblocks.com.*;
import com.genesyslab.platform.applicationblocks.com.objects.*;
import com.genesyslab.platform.applicationblocks.com.queries.*;
import com.genesyslab.platform.commons.log.ILogger;

public class PersonPlace {
	private final ILogger log;
    private final IConfService confService;

    private String employeeID;
    private String switchLoginID;
    private String voiceDN;

    public PersonPlace(ILogger log_, IConfService confService) {
        log = log_.createChildLogger("PersonPlace");
        this.confService = confService;
    }

    public String getEmployeeID() {
    	return employeeID;
    }
    
    public String getSwitchLoginID() {
    	return switchLoginID;
    }
    
    public String getVoiceDN() {
    	return voiceDN;
    }
    
    /**
     * Get the person details from config server
     * @param username
     * @return
     */
    public boolean GetPerson(String username) {
        log.debug("GetPerson: " + username);
        CfgPersonQuery query = new CfgPersonQuery(confService);
        query.setUserName(username);
        CfgPerson cfgPerson;
        
		try {
			cfgPerson = confService.retrieveObject(query);
		} catch (ConfigException e) {
			log.error("GetPerson", e);
			return false;
		}
		
        if (cfgPerson != null && cfgPerson.getAgentInfo() != null) {
            // get the first agent id that we find (should really match the connected switch with the login id)
            Collection<CfgAgentLoginInfo> loginInfos = cfgPerson.getAgentInfo().getAgentLogins();
            for (CfgAgentLoginInfo loginInfo : loginInfos) {
                CfgAgentLogin agentLogin = loginInfo.getAgentLogin();
                employeeID = cfgPerson.getEmployeeID();
                switchLoginID = agentLogin.getLoginCode();
                log.debug("GetPerson: successful");
                return true;
            }
        }

        log.debug("GetPerson: NOT successful");
        return false;
    }

    /**
     * Get the place details from config server
     * @param place
     * @return
     */
    public boolean GetPlace(String place) {
        log.debug("GetPlace: " + place);
        CfgPlaceQuery placeQuery = new CfgPlaceQuery(confService);
        placeQuery.setName(place);
        CfgPlace cfgPlace;
		try {
			cfgPlace = confService.retrieveObject(placeQuery);
		} catch (ConfigException e) {
			log.error("GetPlace", e);
			return false;
		}

        if (place != null) {
            // just get the first DN that we find (should really match the connected switch with the DN)
            for (CfgDN cfgDN : cfgPlace.getDNs()) {
                voiceDN = cfgDN.getNumber();
                log.debug("GetPlace: successful");
                return true;
            }
        }

        log.debug("GetPlace: NOT successful");
        return false;
    }
}
