/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ejb;


import entity.UserGroups;
import entity.Users;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.TypedQuery;
/**
 *
 * @author shubhamv
 */
@Stateless
public class UserEJB {

    @PersistenceContext(unitName = "FlowServePU")
    private EntityManager em;

    public void persist(Object object) {
        em.persist(object);
    }
    
    public Users createUser(Users user) {
		try {
			user.setPassword(AuthenticationUtils.encodeSHA256(user.getPassword()));
		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
			e.printStackTrace();
		}
		UserGroups group = new UserGroups();
		group.setEmail(user.getEmail());
		group.setGroupname(UserGroups.USERS_GROUP);
		em.persist(user);
		em.persist(group);
		
		return user;
	}
	public Users findUserById(String id) {
		TypedQuery<Users> query = em.createNamedQuery("findUserById", User.class);
		query.setParameter("email", id);
		Users user = null;
		try {
			user = query.getSingleResult();
		} catch (Exception e) {
			// getSingleResult throws NoResultException in case there is no user in DB
			// ignore exception and return NULL for user instead
		}
		return user;
	}
}
