/*
 *
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (C) Alkacon Software (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.ade.configuration;

import org.opencms.db.CmsPublishedResource;
import org.opencms.db.CmsResourceState;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsResource;
import org.opencms.file.CmsResourceFilter;
import org.opencms.file.types.I_CmsResourceType;
import org.opencms.main.CmsException;
import org.opencms.main.CmsLog;
import org.opencms.util.CmsStringUtil;
import org.opencms.util.CmsUUID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;

/**
 * This is the internal cache class used for storing configuration data. It is not public because it is only meant
 * for internal use.<p>
 * 
 * It stores an instance of {@link CmsADEConfigData} for each active configuration file in the sitemap,
 * and a single instance which represents the merged configuration from all the modules. When a sitemap configuration
 * file is updated, only the single instance for that configuration file is updated, whereas if a module configuration file
 * is changed, the configuration of all modules will be read again.<p>
 */
class CmsConfigurationCache {

    /** The log instance for this class. */
    private static final Log LOG = CmsLog.getLog(CmsConfigurationCache.class);

    /** The CMS context used for reading configuration data. */
    private CmsObject m_cms;

    /** The configurations from the sitemap / VFS. */
    private Map<String, CmsADEConfigData> m_siteConfigurations = new HashMap<String, CmsADEConfigData>();

    /** The merged configuration from all the modules. */
    private CmsADEConfigData m_moduleConfiguration;

    /** A cache which stores resources' paths by their structure IDs. */
    private Map<CmsUUID, String> m_pathCache = Collections.synchronizedMap(new HashMap<CmsUUID, String>());

    /** The path for sitemap configuration files relative from the base path. */
    protected static final String CONFIG_SUFFIX = "/.content/.config";

    /** The resource type for sitemap configurations. */
    protected I_CmsResourceType m_configType;

    /** The resource type for module configurations. */
    protected I_CmsResourceType m_moduleConfigType;

    /** The cached content types for folders. */
    private Map<String, String> m_folderTypes = new HashMap<String, String>();

    /** 
     * Creates a new cache instance.<p>
     * 
     * @param cms the CMS object used for reading the configuration data
     * @param configType the sitemap configuration file type 
     * @param moduleConfigType the module configuration file type 
     */
    public CmsConfigurationCache(CmsObject cms, I_CmsResourceType configType, I_CmsResourceType moduleConfigType) {

        m_cms = cms;
        m_configType = configType;
        m_moduleConfigType = moduleConfigType;
    }

    /**
     * Looks up the root path for a given structure id.<p>
     *
     * This is used for correcting the paths of cached resource objects.<p>
     * 
     * @param structureId the structure id 
     * @return the root path for the structure id
     * 
     * @throws CmsException if the resource with the given id was not found or another error occurred 
     */
    public String getPathForStructureId(CmsUUID structureId) throws CmsException {

        String rootPath = m_pathCache.get(structureId);
        if (rootPath != null) {
            return rootPath;
        }
        CmsResource res = m_cms.readResource(structureId);
        m_pathCache.put(structureId, res.getRootPath());
        return res.getRootPath();
    }

    /** 
     * Gets the base path for a given sitemap configuration file.<p>
     * 
     * @param siteConfigFile the root path of the sitemap configuration file
     *  
     * @return the base path for the sitemap configuration file 
     */
    protected String getBasePath(String siteConfigFile) {

        if (siteConfigFile.endsWith(CONFIG_SUFFIX)) {
            return CmsResource.getParentFolder(CmsResource.getParentFolder(siteConfigFile));
        }
        return siteConfigFile;
    }

    /**
     * Gets the merged module configuration.<p>
     * @return the merged module configuration instance
     */
    protected synchronized CmsADEConfigData getModuleConfiguration() {

        return m_moduleConfiguration;
    }

    /**
     * Helper method to retrieve the parent folder type.<p>
     * 
     * @param rootPath the path of a resource 
     * @return the parent folder content type 
     */
    protected synchronized String getParentFolderType(String rootPath) {

        String parent = CmsResource.getParentFolder(rootPath);
        if (parent == null) {
            return null;
        }
        String type = m_folderTypes.get(parent);
        if (type == null) {
            return null;
        }
        return type;
    }

    /**
     * Helper method for getting the best matching sitemap configuration object for a given root path, ignoring the module 
     * configuration.<p>
     * 
     * For example, if there are configurations available for the paths /a, /a/b/c, /a/b/x and /a/b/c/d/e, then 
     * the method will return the configuration object for /a/b/c when passed the path /a/b/c/d.
     * 
     * If no configuration data is found for the path, null will be returned.<p> 
     * 
     * @param path a root path  
     * @return the configuration data for the given path, or null if none was found 
     */
    protected synchronized CmsADEConfigData getSiteConfigData(String path) {

        if (path == null) {
            return null;
        }
        String normalizedPath = CmsStringUtil.joinPaths("/", path, "/");
        List<String> prefixes = new ArrayList<String>();
        for (String key : m_siteConfigurations.keySet()) {
            if (normalizedPath.startsWith(CmsStringUtil.joinPaths("/", key, "/"))) {
                prefixes.add(key);
            }
        }
        if (prefixes.size() == 0) {
            return null;
        }
        Collections.sort(prefixes);
        // for any two prefixes of a string, one is a prefix of the other. so the alphabetically last
        // prefix is the longest prefix of all.
        return m_siteConfigurations.get(prefixes.get(prefixes.size() - 1));
    }

    /**
     * Initializes the cache by reading in all the configuration files.<p>
     */
    protected synchronized void initialize() {

        m_siteConfigurations.clear();
        try {
            List<CmsResource> configFileCandidates = m_cms.readResources(
                "/",
                CmsResourceFilter.DEFAULT.addRequireType(m_configType.getTypeId()));
            for (CmsResource candidate : configFileCandidates) {
                if (isSitemapConfiguration(candidate.getRootPath(), candidate.getTypeId())) {
                    update(candidate);
                }
            }
        } catch (Exception e) {
            LOG.error(e.getLocalizedMessage(), e);
        }
        refreshModuleConfiguration();
        try {
            initializeFolderTypes();
        } catch (Exception e) {
            LOG.error(e.getLocalizedMessage(), e);
        }

    }

    /**
     * Initializes the cached folder types.<p>
     * 
     * @throws CmsException if something goes wrong 
     */
    protected synchronized void initializeFolderTypes() throws CmsException {

        m_folderTypes.clear();
        for (CmsADEConfigData configData : m_siteConfigurations.values()) {
            Map<String, String> folderTypes = configData.getFolderTypes();
            m_folderTypes.putAll(folderTypes);
        }
        if (m_moduleConfiguration != null) {
            Map<String, String> folderTypes = m_moduleConfiguration.getFolderTypes();
            m_folderTypes.putAll(folderTypes);
        }
    }

    /**
     * Checks whether the given path/type combination belongs to a module configuration file.<p>
     * 
     * @param rootPath the root path of the resource 
     * @param type the type id of the resource 
     * 
     * @return true if the path/type combination belongs to a module configuration 
     */
    protected boolean isModuleConfiguration(String rootPath, int type) {

        return type == m_moduleConfigType.getTypeId();
    }

    /** 
     * Returns true if this an online configuration cache.<p>
     * 
     * @return true if this is an online cache, false if it is an offline cache 
     */
    protected boolean isOnline() {

        return m_cms.getRequestContext().getCurrentProject().isOnlineProject();
    }

    /**
     * Checks whether the given path/type combination belongs to a sitemap configuration.<p> 
     * 
     * @param rootPath the root path 
     * @param type the resource type id 
     * 
     * @return true if the path/type belong to an active sitemap configuration 
     */
    protected boolean isSitemapConfiguration(String rootPath, int type) {

        return rootPath.endsWith("/.content/.config") && (type == m_configType.getTypeId());
    }

    /**
     * Reloads the module configuration.<p>
     */
    protected synchronized void refreshModuleConfiguration() {

        CmsConfigurationReader reader = new CmsConfigurationReader(m_cms);
        m_moduleConfiguration = reader.readModuleConfigurations();
        m_moduleConfiguration.initialize(m_cms);
    }

    /**
     * Removes a published resource from the cache.<p>
     * 
     * @param res the published resource 
     */
    protected void remove(CmsPublishedResource res) {

        remove(res.getStructureId(), res.getRootPath(), res.getType());
    }

    /**
     * Removes a resource from the cache.<p>
     * 
     * @param res the resource to remove 
     */
    protected void remove(CmsResource res) {

        remove(res.getStructureId(), res.getRootPath(), res.getTypeId());
    }

    /**
     * Removes the cache entry for the given resource data.<p>
     * 
     * @param structureId the resource structure id 
     * @param rootPath the resource root path 
     * @param type the resource type 
     */
    protected void remove(CmsUUID structureId, String rootPath, int type) {

        if (CmsResource.isTemporaryFileName(rootPath)) {
            return;
        }
        try {
            updateFolderTypes(rootPath);
        } catch (CmsException e) {
            LOG.error(e.getLocalizedMessage(), e);
        }
        m_pathCache.remove(structureId);
        if (isSitemapConfiguration(rootPath, type)) {
            synchronized (this) {
                m_siteConfigurations.remove(getBasePath(rootPath));
                LOG.info("Removing config file from cache: " + rootPath);
            }
        } else if (isModuleConfiguration(rootPath, type)) {
            refreshModuleConfiguration();
            try {
                initializeFolderTypes();
            } catch (CmsException e) {
                LOG.error(e.getLocalizedMessage(), e);
            }

        }

    }

    /**
     * Updates the cache entry for the given published resource.<p>
     * 
     * @param res a published resource
     */
    protected void update(CmsPublishedResource res) {

        try {
            update(res.getStructureId(), res.getRootPath(), res.getType(), res.getState());
        } catch (CmsException e) {
            LOG.error(e.getLocalizedMessage(), e);
        }
    }

    /** 
     * Updates the cache entry for the given resource.<p>
     * 
     * @param res the resource for which the cache entry should be updated
     */
    protected void update(CmsResource res) {

        try {
            update(res.getStructureId(), res.getRootPath(), res.getTypeId(), res.getState());
        } catch (CmsException e) {
            LOG.error(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Updates the cache entry for the given resource data.<p>
     * 
     * @param structureId the structure id of the resource  
     * @param rootPath the root path of the resource 
     * @param type the type id of the resource 
     * @param state the state of the resource 
     * @throws CmsException if something goes wrong 
     */
    protected void update(CmsUUID structureId, String rootPath, int type, CmsResourceState state) throws CmsException {

        if (CmsResource.isTemporaryFileName(rootPath)) {
            return;
        }

        try {
            updateFolderTypes(rootPath);
        } catch (CmsException e) {
            LOG.error(e.getLocalizedMessage(), e);
        }
        synchronized (m_pathCache) {
            m_pathCache.remove(structureId);
            m_pathCache.put(structureId, rootPath);
        }
        if (isSitemapConfiguration(rootPath, type)) {
            synchronized (this) {
                CmsConfigurationReader configReader = new CmsConfigurationReader(m_cms);
                String basePath = getBasePath(rootPath);
                CmsResource configRes = m_cms.readResource(rootPath);
                CmsADEConfigData configData = configReader.parseSitemapConfiguration(getBasePath(rootPath), configRes);
                configData.initialize(m_cms);
                m_siteConfigurations.put(basePath, configData);
                LOG.info("Updating configuration file " + rootPath);
                initializeFolderTypes();
            }
        } else if (isModuleConfiguration(rootPath, type)) {
            refreshModuleConfiguration();
            initializeFolderTypes();
        }
    }

    /**
     * Updates the cached folder types.<p>
     * 
     * @param rootPath the folder root path 
     * @throws CmsException if something goes wrong 
     */
    protected synchronized void updateFolderTypes(String rootPath) throws CmsException {

        if (m_folderTypes.containsKey(rootPath)) {
            synchronized (this) {
                initializeFolderTypes();
            }
        }
    }

}
