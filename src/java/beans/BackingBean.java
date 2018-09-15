/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package beans;

import async.beans.MyAsyncBean;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.annotation.PostConstruct;
/**
 *
 * @author user
 */
@Named(value = "backingBean")
@SessionScoped
public class BackingBean implements Serializable 
{

    @EJB
    MyAsyncBean asyncBean;
    private String result;
    private Future<String> asyncResult;
    
    @PostConstruct
    public void init() 
    {
        try 
        {
            asyncResult = asyncBean.getAsyncResult();
        } 
        catch (InterruptedException ex)
        {
            Logger.getLogger(BackingBean.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public String getResult() 
    {
        return result;
    }
 
    public void setResult(String result) 
    {
        this.result = result;
    }
 
    public BackingBean() 
    {
    }
 
    //After we call EJB bean's async method
    //we get reference to Future object that we
    //can use to get the result or check is our
    //task complete. Here we also immediately check
    //if our task is done. 
    public String getAsyncResult() {
        try {
            //asyncResult = asyncBean.getAsyncResult();
            if (asyncResult.isDone()) {
                this.setResult(asyncResult.get());
            } else {
                this.setResult("Processing...");
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(BackingBean.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(BackingBean.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "result";
    }
 
    //Refresh the view. Again, check if our task is completed,
    //and it is, get the result. 
    public String refresh() 
    {
        try {
            if (asyncResult.isDone()) {
                this.setResult(asyncResult.get());
            } else {
                this.setResult("Still processing...");
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(BackingBean.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(BackingBean.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "";
    }
    
}
