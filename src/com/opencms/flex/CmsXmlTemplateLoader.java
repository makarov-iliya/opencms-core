/*
 * File   : $Source: /alkacon/cvs/opencms/src/com/opencms/flex/Attic/CmsXmlTemplateLoader.java,v $
 * Date   : $Date: 2002/09/04 15:46:47 $
 * Version: $Revision: 1.4 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (C) 2002  The OpenCms Group
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about OpenCms, please see the
 * OpenCms Website: http://www.opencms.org
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * First created on 14. April 2002, 19:10
 */


package com.opencms.flex;

import com.opencms.core.*;
import com.opencms.file.*;

import com.opencms.flex.cache.*;

import java.io.IOException;

import javax.servlet.ServletException;


/**
 * Description of the class CmsDumpLoader here.
 *
 * @author  Alexander Kandzior (a.kandzior@alkacon.com)
 * @version $Revision: 1.4 $
 */
public class CmsXmlTemplateLoader extends com.opencms.launcher.CmsXmlLauncher implements I_CmsResourceLoader {
    
    private static CmsFlexCache m_cache;    
    
    private static int DEBUG = 0;

    private A_OpenCms m_openCms = null;
    
    // ---------------------------- Implementation of interface com.opencms.launcher.I_CmsLauncher          
    
    /** Destroy this ResourceLoder  */
    public void destroy() {
        // NOOP
    }
    
    /** Return a String describing the ResourceLoader  */
    public String getResourceLoaderInfo() {
        return "A XmlTemplate loader that extends from com.opencms.launcher.CmsXmlLauncher";
    }
    
    /** Initialize the ResourceLoader  */
    public void init(A_OpenCms openCms) {
        // This must be saved for call to this.generateOutput();
        m_openCms = openCms;
        m_cache = (CmsFlexCache)openCms.getRuntimeProperty(this.C_LOADER_CACHENAME);
        if (m_cache == null) {
            m_cache = new CmsFlexCache( openCms );
            m_openCms.setRuntimeProperty(this.C_LOADER_CACHENAME, m_cache);
        }        
        log(this.getClass().getName() + " initialized!");     
    }
    
    /** Basic processing method with CmsFile  */
    public void load(com.opencms.file.CmsObject cms, com.opencms.file.CmsFile file, javax.servlet.http.HttpServletRequest req, javax.servlet.http.HttpServletResponse res) 
    throws ServletException, IOException {    
        CmsFlexRequest w_req; 
        CmsFlexResponse w_res;
        if (req instanceof CmsFlexRequest) {
            w_req = (CmsFlexRequest)req; 
        } else {
            w_req = new CmsFlexRequest(req, file, m_cache, cms); 
        }        
        if (res instanceof CmsFlexResponse) {
            w_res = (CmsFlexResponse)res;              
        } else {
            w_res = new CmsFlexResponse(res, false);
        }                
        service(cms, file, w_req, w_res);
    }
    
    // --------------------------- Overloaded methods from launcher interface

    public void setOpenCms(A_OpenCms openCms) {
        init(openCms);
    }
    
    public void service(CmsObject cms, CmsResource file, CmsFlexRequest req, CmsFlexResponse res)
    throws ServletException, IOException {
        long timer1 = 0;
        if (DEBUG > 0) {
            timer1 = System.currentTimeMillis();        
            System.err.println("========== XmlTemplateLoader loading: " + file.getAbsolutePath());            
        }
        try {                        
            // get the CmsRequest
            I_CmsRequest cms_req = cms.getRequestContext().getRequest();
            byte[] result = null;
            
            com.opencms.file.CmsFile fx = req.getCmsObject().readFile(file.getAbsolutePath());            
            
            cms.getRequestContext().setUri(fx.getAbsolutePath());            
            result = generateOutput(cms, fx, fx.getLauncherClassname(), cms_req, m_openCms);            
            cms.getRequestContext().setUri(null);

            if(result != null) {
                res.getOutputStream().write(result);
            }        
        }  catch (Exception e) {
            System.err.println("Error in CmsXmlTemplateLoader: " + e.toString());
            if (DEBUG > 0) System.err.println(com.opencms.util.Utils.getStackTrace(e));
            throw new ServletException("Error in CmsXmlTemplateLoader processing", e);       
        }
        if (DEBUG > 0) {
            long timer2 = System.currentTimeMillis() - timer1;        
            System.err.println("========== Time delivering XmlTemplate for " + file.getAbsolutePath() + ": " + timer2 + "ms");            
        }
    }
    
    private void log(String message) {
        if (com.opencms.boot.I_CmsLogChannels.C_PREPROCESSOR_IS_LOGGING) {
            com.opencms.boot.CmsBase.log(com.opencms.boot.CmsBase.C_FLEX_LOADER, "[CmsXmlTemplateLoader] " + message);
        }
    }    
}
