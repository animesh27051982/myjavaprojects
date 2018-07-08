package com.flowserve.system606.security.realm;

import java.security.Principal;
import java.util.Set;

import org.glassfish.security.common.PrincipalImpl;

import com.sun.appserv.security.AppservPasswordLoginModule;
import com.sun.enterprise.security.auth.login.common.LoginException;

public class ECMLoginModule extends AppservPasswordLoginModule {

	@Override
	protected void authenticateUser() throws LoginException {

		_logger.info("ECMSecurityRealm : Authenticate User: " + _username);
		final ECMSecurityRealm realm = (ECMSecurityRealm) _currentRealm;

		if ((_username == null) || (_username.length() == 0))
			throw new LoginException("Invalid credentials");

		try {
			_username = _username.toLowerCase();
			String[] grpList = realm.authenticate(_username, getPasswordChar());
			Set<Principal> principals = _subject.getPrincipals();
			for (Principal principal : principals) {
				_logger.severe("Principal: " + principal.getName());
				System.out.println("Principal: " + principal.getName());
			}
			principals.add(new PrincipalImpl(_username));
			this.commitUserAuthentication(grpList);
		} catch (Exception e) {
			_logger.severe("Error upon login: " + e.getMessage());
			System.out.println("Error upon login: " + e.getMessage());
			throw new LoginException(e.getMessage());
		}
	}
}
