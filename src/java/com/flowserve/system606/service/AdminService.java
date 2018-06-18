/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowserve.system606.service;

import com.flowserve.system606.model.Country;
import com.flowserve.system606.model.InputType;
import com.flowserve.system606.model.User;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

/**
 *
 * @author shubhamv
 */
@Stateless
public class AdminService {

    @PersistenceContext(unitName = "FlowServePU")
    private EntityManager em;

    public List<User> searchUsers(String searchString) throws Exception {  // Need an application exception type defined.
        if (searchString == null || searchString.trim().length() < 2) {
            throw new Exception("Please supply a search string with at least 2 characters.");
        }
        System.out.println("Search" + searchString.toUpperCase());
        TypedQuery<User> query = em.createQuery("SELECT u FROM User u WHERE UPPER(u.name) LIKE :NAME OR UPPER(u.flsId) LIKE :NAME ORDER BY UPPER(u.name)", User.class);
        query.setParameter("NAME", "%" + searchString.toUpperCase() + "%");
        return (List<User>) query.getResultList();
    }

    public void updateUser(User u) throws Exception {
        em.merge(u);
    }

    public void persist(Object object) {
        em.persist(object);
    }

    public List<User> findUserByFlsId(String adname) {
        Query query = em.createQuery("SELECT u FROM User u WHERE UPPER(u.flsId) = :FLS_ID ORDER BY UPPER(u.id)");
        query.setParameter("FLS_ID", adname.toUpperCase());
        return (List<User>) query.getResultList();

    }

    public Country findCountryById(String id) {
        return em.find(Country.class, id);
    }

    public List<InputType> findInputTypeByName(String name) {
        Query query = em.createQuery("SELECT inputType FROM InputType inputType WHERE inputType.name = :NAME");
        query.setParameter("NAME", name);
        return (List<InputType>) query.getResultList();
    }

    public void persist(InputType inputType) throws Exception {
        em.persist(inputType);
    }

    public void persist(Country country) throws Exception {
        em.persist(country);
    }

    public void updater(User u) throws Exception {
        em.persist(u);
    }
}
