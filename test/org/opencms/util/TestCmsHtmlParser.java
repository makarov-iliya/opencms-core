/*
 * File   : $Source: /alkacon/cvs/opencms/test/org/opencms/util/TestCmsHtmlParser.java,v $
 * Date   : $Date: 2005/11/09 14:59:59 $
 * Version: $Revision: 1.1.2.1 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (c) 2005 Alkacon Software GmbH (http://www.alkacon.com)
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
 * For further information about Alkacon Software GmbH, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.util;

import org.opencms.i18n.CmsEncoder;

import junit.framework.TestCase;

/** 
 * Test case for <code>{@link org.opencms.util.CmsHtmlParser}</code>.<p>
 * 
 * @author Alexander Kandzior 
 * 
 * @version $Revision: 1.1.2.1 $
 * 
 * @since 6.2.0
 */
public class TestCmsHtmlParser extends TestCase {

    /**
     * Default JUnit constructor.<p>
     * 
     * @param arg0 JUnit parameters
     */
    public TestCmsHtmlParser(String arg0) {

        super(arg0);
    }

    /**
     * Tests the HTML extractor.<p>
     * 
     * @throws Exception in case the test fails
     */
    public void testHtmlExtractor() throws Exception {

        CmsHtmlParser visitor1 = new CmsHtmlParser(true);
        String content1 = CmsFileUtil.readFile(
            "org/opencms/util/testHtml_01.html",
            CmsEncoder.ENCODING_ISO_8859_1);
        String result1 = CmsHtmlParser.process(content1, CmsEncoder.ENCODING_ISO_8859_1, visitor1);
        System.out.println(result1 + "\n\n");
        // assertEquals(content1, result1);

        CmsHtmlParser visitor2 = new CmsHtmlParser(true);
        String content2 = CmsFileUtil.readFile(
            "org/opencms/util/testHtml_02.html",
            CmsEncoder.ENCODING_ISO_8859_1);
        String result2 = CmsHtmlParser.process(content2, CmsEncoder.ENCODING_ISO_8859_1, visitor2);
        System.out.println(result2 + "\n\n");
        assertEquals(content2, result2);

        CmsHtmlParser visitor3 = new CmsHtmlParser(true);
        String content3 = CmsFileUtil.readFile(
            "org/opencms/util/testHtml_03.html",
            CmsEncoder.ENCODING_ISO_8859_1);
        String result3 = CmsHtmlParser.process(content3, CmsEncoder.ENCODING_ISO_8859_1, visitor3);
        System.out.println(result3 + "\n\n");
        assertEquals(content3, result3);
        
        // check with non-echo visitor, no output should be produced
        CmsHtmlParser visitor4 = new CmsHtmlParser();
        result3 = CmsHtmlParser.process(content3, CmsEncoder.ENCODING_ISO_8859_1, visitor4);
        System.out.println(result3 + "\n\n");
        assertEquals("", result3);
    }
}