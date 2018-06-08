package com.flowserve.system606.view;

import com.flowserve.system606.model.User;
import com.flowserve.system606.service.AdminService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

@ManagedBean(name = "userSearch")
@ViewScoped
public class UserSearch implements Serializable {

    private static final long serialVersionUID = -1438027991420003830L;

    @EJB
    private AdminService adminService;

    public List<User> getUsers() throws Exception {
        List<User> users = new ArrayList<User>();
//        List<User> users = adminService.findAllUsers();
//        Collections.sort(users);

        return users;
    }
}
