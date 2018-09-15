/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package async.beans;

import java.util.concurrent.Future;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.LocalBean;

/**
 *
 * @author user
 */
@Stateless
@LocalBean
public class MyAsyncBean 
{
    @Asynchronous
    public Future<String> getAsyncResult() throws InterruptedException 
    {
        System.out.println("Called async method, starting long running task...");
        for(int i=0;i<=10000;i++)
        {
            System.out.println(i+"\n");
        }
        System.out.println("Long running task completed. Now you can fetch the result.");
        String result =  "DONE! Result from my async EJB method invocation.";
        return new AsyncResult<>(result);
    }
}
