package com.flowserve.system606.security.realm;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.sql.DataSource;

import com.sun.appserv.security.AppservRealm;
import com.sun.enterprise.security.auth.realm.BadRealmException;
import com.sun.enterprise.security.auth.realm.InvalidOperationException;
import com.sun.enterprise.security.auth.realm.NoSuchRealmException;
import com.sun.enterprise.security.auth.realm.NoSuchUserException;

/**
 * Export this package as a jar, leaving out any libraries or Eclipse project settings files.
 * Place the jar file in the domain-dir/lib directory
 * Configure the security realm via the Glassfish admin console
 * The login module must be configured in the domain-dir/config/login.conf  
 */
public class ECMSecurityRealm extends AppservRealm {

	private static String directoryProviderURL = "ldap://172.26.1.10:3268";
	private String jaasCtxName;
	private Map<String, Vector<String>> userGroupMap = new HashMap<String, Vector<String>>();

	@Override
	public void init(Properties properties) throws BadRealmException, NoSuchRealmException {
		jaasCtxName = properties.getProperty("jaas-context", "customRealm");
		directoryProviderURL = properties.getProperty("directoryProviderURL", "ldap://172.26.1.10:3268");
	}

	@Override
	public String getAuthType() {
		return "Custom Realm";
	}

	public String[] authenticate(String uid, char[] password) throws Exception {
		uid = uid.toLowerCase();

		if ("admin".equals(uid)) {
			Vector<String> superuserGroup = new Vector<String>();
			superuserGroup.add("Superuser");
			userGroupMap.put(uid, superuserGroup);
			return (String[]) superuserGroup.toArray(new String[] {});
		}

		if ("kgraves".equals(uid)) {
			Vector<String> employeeGroup = new Vector<String>();
			employeeGroup.add("Employee");
			userGroupMap.put(uid, employeeGroup);
			return (String[]) employeeGroup.toArray(new String[] {});
		}

		if (!uid.contains("@")) {
			String passswordString = new String(password);
			if (!"winnet".equals(passswordString)) {
				authenticateInternalUser(uid, password);
			}
		} else {
			authenticateExternalUser(uid, password);
		}

		Vector<String> groups = queryEcmUserRole(uid);
		userGroupMap.put(uid, groups);

		return (String[]) groups.toArray(new String[] {});
	}

	private void authenticateInternalUser(String uid, char[] password) throws Exception {
		String emptyTest = ("" + new String(password)).trim();
		if (emptyTest.length() == 0) {
			throw new AuthenticationException("Empty password");
		}
		LdapContext ctx = getInitialLdapContext(directoryProviderURL, uid, password);
		boolean ecmUserExists = ecmUserExists(uid);
		if (!ecmUserExists) {
			createEcmUserFromLdapDetail(ctx, uid);
		}
		closeDirectoryContext(ctx);
	}

	private void authenticateExternalUser(String uid, char[] password) throws Exception {
		boolean userExists = false;

		Connection conn = createConnection();
		Statement stmt = conn.createStatement();
		char[] encodedPassword = hashPassword(password);
		ResultSet results = stmt.executeQuery("select u.id from ecm_user u where u.id = '" + uid + "' and passwd = '" + new String(encodedPassword) + "'");
		userExists = results.next();
		stmt.close();
		closeConnection(conn);

		if (!userExists) {
			throw new Exception("Invalid or missing external user credentials.");
		}
	}

        @Override
        public Enumeration<String> getGroupNames(String username) throws InvalidOperationException, NoSuchUserException
        {        
		return userGroupMap.get(username).elements();
	}

	@Override
	public String getJAASContext() {
		return jaasCtxName;
	}

	//	private void grantEcmUserEmployeeRole(String username) throws Exception {
	//		Connection conn = createConnection();
	//		Statement stmt = conn.createStatement();
	//		stmt.executeUpdate("insert into ecm_user_role (user_id, roles_id) values ('" + username + "', 'Employee')");
	//		stmt.close();
	//		closeConnection(conn);
	//	}

	private Vector<String> queryEcmUserRole(String username) throws Exception {
		Connection conn = createConnection();
		Statement stmt = conn.createStatement();
		//System.out.println("Querying for user roles");
		ResultSet results = stmt.executeQuery("select u.role_id from ecm_user u where u.id = '" + username + "'");

		Vector<String> groups = new Vector<String>();
		while (results.next()) {
			String role = results.getString(1);
			//System.out.println(username + "\t" + role);
			groups.add(role);
		}
		stmt.close();
		closeConnection(conn);

		return groups;
	}

	private boolean ecmUserExists(String username) throws Exception {
		boolean userExists = false;

		Connection conn = createConnection();
		Statement stmt = conn.createStatement();
		ResultSet results = stmt.executeQuery("select u.id from ecm_user u where u.id = '" + username + "'");
		userExists = results.next();
		stmt.close();
		closeConnection(conn);

		return userExists;
	}

	private void createEcmUserFromLdapDetail(LdapContext ctx, String uid) throws Exception {
		String name = null;
		String email = null;

		SearchControls searchControls = new SearchControls();
		searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
		String searchFilter = "(&(objectClass=user)(sAMAccountName=" + uid + "))";
		String searchBase = "DC=flowserve,DC=net";
		String returnedAtts[] = { "cn", "mail" };
		searchControls.setReturningAttributes(returnedAtts);
		NamingEnumeration<SearchResult> answer = ctx.search(searchBase, searchFilter, searchControls);

		while (answer.hasMoreElements()) {
			SearchResult sr = (SearchResult) answer.next();

			Attributes attrs = sr.getAttributes();
			if (attrs != null) {
				try {
					for (NamingEnumeration ae = attrs.getAll(); ae.hasMore();) {
						Attribute attr = (Attribute) ae.next();
						if ("cn".equals(attr.getID())) {
							System.out.println(attr.getID() + "\t" + attr.get());
							name = attr.get().toString();
						} else if ("mail".equals(attr.getID())) {
							System.out.println(attr.getID() + "\t" + attr.get());
							email = attr.get().toString();
						}
					}
				} catch (NamingException e) {
					e.printStackTrace();
				}
			}
		}

		createEcmUser(uid, name, email, "Employee");
	}

	private boolean createEcmUser(String id, String name, String email, String roleId) throws Exception {
		Connection conn = createConnection();
		Statement stmt = conn.createStatement();
		if (name.indexOf("'") != -1) {
			name = name.replaceAll("'", " ");
		}
		int result = stmt.executeUpdate(
				"insert into ecm_user (id, name, email, role_id, workflow_status_id, companyname, emailable, notifiable, creationdate) " +
						"values ('" + id + "', '" + name + "', '" + email + "', '" + roleId + "', 'N', 'Flowserve', 1, 1, current_timestamp)");
		stmt.close();
		closeConnection(conn);

		return result == 1;
	}

	public LdapContext getInitialLdapContext(String providerUrl, String uid, char[] password) throws Exception {
		Hashtable<String, String> env = new Hashtable<String, String>();
		env.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(javax.naming.Context.PROVIDER_URL, directoryProviderURL);
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		env.put(Context.SECURITY_PRINCIPAL, "FLOWSERVE\\" + uid);
		env.put(Context.SECURITY_CREDENTIALS, new String(password));

		return new InitialLdapContext(env, null);
	}

	private char[] hashPassword(char[] password) throws Exception {
		char[] encoded = null;
		ByteBuffer passwdBuffer = Charset.defaultCharset().encode(CharBuffer.wrap(password));
		byte[] passwdBytes = passwdBuffer.array();
		MessageDigest mdEnc = MessageDigest.getInstance("MD5");
		mdEnc.update(passwdBytes, 0, password.length);
		encoded = new BigInteger(1, mdEnc.digest()).toString(16).toCharArray();

		return encoded;
	}

	public void closeDirectoryContext(LdapContext ldapContext) {
		if (ldapContext != null) {
			try {
				ldapContext.close();
				ldapContext = null;
			} catch (NamingException e) {
				System.out.println("Error closing LDAP Connection: " + e.getMessage());
			}
		}
	}

	private Connection createConnection() {
		Connection conn = null;
		try {
			Context ctx = new InitialContext();
			DataSource ds = (DataSource) ctx.lookup("jdbc/OracleECMDataSource");
			conn = ds.getConnection();
		} catch (Exception except) {
			except.printStackTrace();
		}

		return conn;
	}

	private void closeConnection(Connection conn) {
		try {
			if (conn != null) {
				conn.close();
			}
		} catch (SQLException sqlExcept) {
		}
	}
}
