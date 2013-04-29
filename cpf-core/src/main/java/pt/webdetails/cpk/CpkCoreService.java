/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */
package pt.webdetails.cpk;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.dom4j.DocumentException;
import pt.webdetails.cpf.RestRequestHandler;
import pt.webdetails.cpf.Router;
import pt.webdetails.cpf.http.ICommonParameterProvider;
import pt.webdetails.cpf.utils.IPluginUtils;
import pt.webdetails.cpk.elements.IElement;
import pt.webdetails.cpk.plugins.PluginBuilder;
import pt.webdetails.cpk.security.AccessControl;


/**
 *
 * @author joao
 */
public class CpkCoreService {
    

    private static final long serialVersionUID = 1L;
    public static final String CDW_EXTENSION = ".cdw";
    public static final String PLUGIN_NAME = "cpk";
    private static final String ENCODING = "UTF-8";
    private CpkEngine cpkEngine;
    private final String PLUGIN_UTILS = "PluginUtils";
    private IPluginUtils pluginUtils;
    private static final Logger logger = Logger.getLogger(CpkCoreService.class.getName());

    public CpkCoreService(IPluginUtils pluginUtils){
        
        this.pluginUtils=pluginUtils;
    }
    
    
    public void createContent(Map<String,ICommonParameterProvider> parameterProviders) throws Exception {

        // Make sure we have the engine running
        cpkEngine = CpkEngine.getInstance();
        
        //PluginUtils pluginUtils = PluginUtils.getInstance();
        
        AccessControl accessControl = new AccessControl(pluginUtils);
        
        logger.log(Level.WARNING,"Creating content");//switched from debug("Creating content")

        // Get the path, remove leading slash
        
        String path = pluginUtils.getPathParameters(parameterProviders).getStringParameter("path", null);
        IElement element = null;

        if (path == null || path.equals("/")) {

            String url = cpkEngine.getDefaultElement().getId().toLowerCase();
            if (path == null) {
                // We need to put the http redirection on the right level
                url = pluginUtils.getPluginName() + "/" + url;
            }
            pluginUtils.redirect(parameterProviders, url);
        }

        element = cpkEngine.getElement(path.substring(1));
        if (element != null) {
            if (accessControl.isAllowed(element)) {
                element.processRequest(parameterProviders);
            } else {
                accessControl.throwAccessDenied(parameterProviders);
            }

        } else {
            super.createContent();//XXX have no super, change this maybe throw an exception
        }


    }

    //@Exposed(accessLevel = AccessLevel.PUBLIC)
    public void reload(OutputStream out,Map<String,ICommonParameterProvider> parameterProviders) throws DocumentException, IOException {

        // alias to refresh
        refresh(out,parameterProviders); //XXX should I store the parameter providers somewhere?
    }

    //@Exposed(accessLevel = AccessLevel.PUBLIC)
    public void refresh(OutputStream out, Map<String,ICommonParameterProvider> parameterProviders) throws DocumentException, IOException {
        AccessControl accessControl = new AccessControl(pluginUtils);
        if(accessControl.isAdmin()){
            logger.info("Refreshing CPK plugin " + getPluginName());
            cpkEngine.reload();
            status(out,parameterProviders); 
        }else{
            accessControl.throwAccessDenied(parameterProviders);
        }


    }

    //@Exposed(accessLevel = AccessLevel.PUBLIC)
    public void status(OutputStream out, Map<String,ICommonParameterProvider> parameterProviders) throws DocumentException, IOException {

        logger.info("Showing status for CPK plugin " + getPluginName());

        pluginUtils.getInstance().setResponseHeaders(parameterProviders, "text/plain");
        out.write(cpkEngine.getStatus().getBytes("UTF-8"));

    }

   // @Exposed(accessLevel = AccessLevel.PUBLIC)
    public void getSitemapJson(OutputStream out) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(out, cpkEngine.getSitemapJson());
    }
    
  //  @Exposed(accessLevel = AccessLevel.PUBLIC)
    public void pluginsList(OutputStream out){
        
        
        ObjectMapper mapper = new ObjectMapper();
        
        try {
            String json = mapper.writeValueAsString(cpkEngine.getPluginsList());
            writeMessage(out, json);
        } catch (IOException ex) {
            try {
                out.write("Error getting JSON".getBytes(ENCODING));
            } catch (IOException ex1) {
                Logger.getLogger(CpkCoreService.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }
        
    }
    
   // @Exposed(accessLevel = AccessLevel.PUBLIC)
    public void getElementsList(OutputStream out){
        try {
            out.write(cpkEngine.getElementsJson().getBytes(ENCODING));
        } catch (IOException ex) {
            Logger.getLogger(CpkCoreService.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
   // @Exposed(accessLevel = AccessLevel.PUBLIC)
    public void createPlugin(OutputStream out,Map<String,ICommonParameterProvider> parameterProviders){
        String json = parameterProviders.get("request").getStringParameter("plugin", null);
        
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode node = mapper.readTree(json);
            PluginBuilder pluginMaker = new PluginBuilder(node);
            pluginMaker.writeFiles(true);
            writeMessage(out, "Plugin created successfully!");
            
        } catch (Exception ex) {
            writeMessage(out, "There seems to have occurred an error during the plugin creation. Sorry!");
            Logger.getLogger(CpkCoreService.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }

   // @Override  
    public String getPluginName() {

        return pluginUtils.getInstance().getPluginName();
    }
    
    private void writeMessage(OutputStream out, String message){
        try {
            out.write(message.getBytes(ENCODING));
        } catch (IOException ex) {
            Logger.getLogger(CpkCoreService.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

   // @Override
    public RestRequestHandler getRequestHandler() {
        return Router.getBaseRouter();
    }
}
