package com.flowserve.system606.view;

import com.flowserve.system606.model.User;
import com.flowserve.system606.service.AdminService;
import com.flowserve.system606.web.WebSession;
import com.onelogin.saml2.Auth;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//@ApplicationScoped
public class CustomAuthenticationMechanism implements HttpAuthenticationMechanism {

    @Inject
    private AdminService adminService;
    @Inject
    private WebSession webSession;

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request,
            HttpServletResponse response,
            HttpMessageContext httpMessageContext) {

        Logger.getLogger(CustomAuthenticationMechanism.class.getName()).log(Level.INFO, "CustomAuthenticationMechanism.....");

        try {
            Auth auth = new Auth(request, response);

            if (request.getMethod().equals("POST")) {
                auth.processResponse();
                if (auth.isAuthenticated()) {
                    User user = adminService.findUserByFlsIdType(auth.getNameId());
                    webSession.setUser(user);
                    httpMessageContext.notifyContainerAboutLogin(user, null);
                    //httpMessageContext.redirect("/rcs/dsahboard.xhtml");
                    return AuthenticationStatus.SUCCESS;
                    //return httpMessageContext.notifyContainerAboutLogin(user, null);
                }
            } else {
                if (webSession.getUser() == null) {
                    auth.login();
                }
            }
//            if (request.getSession().getAttribute("nameId") == null) {
//                Auth auth = new Auth(request, response);
//                if (request.getParameter("attrs") == null) {
//                    auth.login();
//                } else {
//                    String x = request.getPathInfo();
//                    auth.login(x);
//                }
//            } else {
//
//            }
        } catch (Exception e) {
            Logger.getLogger(CustomAuthenticationMechanism.class.getName()).log(Level.INFO, "Send to error page.", e);
        }

        return httpMessageContext.doNothing();
    }
}
