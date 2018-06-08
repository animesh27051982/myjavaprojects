package com.flowserve.system606.service;

import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

@Stateless
public class AdminService {

    private static final Logger LOG = Logger.getLogger(AdminService.class.getName());

//    	@PersistenceContext(unitName = "revrecPU")
//	private EntityManager em;
    @Resource
    private SessionContext sessionContext;

    @PostConstruct
    public void init() {
    }

//    public List<User> searchUsers(String searchString) throws Exception {  // Need an application exception type defined.
//        if (searchString == null || searchString.trim().length() < 2) {
//            throw new Exception("Please supply a search string with at least 2 characters.");
//        }
//
//        //TypedQuery<User> query = em.createQuery("SELECT u FROM User u WHERE UPPER(u.name) LIKE :NAME OR UPPER(u.flsId) LIKE :NAME ORDER BY UPPER(u.name)", User.class);
//        query.setParameter("NAME", "%" + searchString.toUpperCase() + "%");
//
//        return query.getResultList();
//    }
//	public List<User> findAllUsers() throws Exception {
//		Query query = em.createQuery("SELECT u FROM User u ORDER BY u.name");
//		return (List<User>) query.getResultList();
//	}
}
