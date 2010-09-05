/**
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.diffable.tags;

import java.io.File;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import com.google.diffable.Constants;
import com.google.diffable.data.DiffableContext;

/**
 * This class defines the diffable resource tag.
 * 
 * @author joshua Harrison
 * @author ibrahim Chaehoi
 */
public class DiffableResourceTag extends TagSupport {

	/** The serial version UID */
	private static final long serialVersionUID = 6375497229840702819L;
	private String resource;
	private String type;
	
	public void setResource(String resource) {
		this.resource = resource;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	private String hashPath(File resource) 
	throws Exception {
		byte[] filePathBytes = resource.getAbsolutePath().getBytes();
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		md5.update(filePathBytes, 0, filePathBytes.length);
		return new BigInteger(1, md5.digest()).toString(16);
	}
	
	@Override
	public int doStartTag() throws JspException {
		
		DiffableContext ctx = (DiffableContext) pageContext.getServletContext().getAttribute(Constants.DIFFABLE_CONTEXT);
		if(ctx == null){
			throw new JspException("No diffable context defined!");
		}
		try {
			if (ctx.getServletPrefix() == null) {
				throw new JspException(
					"No servlet prefix defined!");
			}
			// Set the response of the page containing this resource to be
			// uncacheable.
			((HttpServletResponse)pageContext.getResponse())
				.setHeader("Cache-Control", "private, max-age=0");
		    ((HttpServletResponse)pageContext.getResponse())
				.setHeader("Expires", "-1");
		
			File found = null;
			if (!(new File(resource).exists())) {
				for (File folder : ctx.getResourceFolders()) {
					File test =
						new File(folder.getAbsolutePath() +
								 File.separator + resource);
					if (test.exists()) {
						found = test;
						break;
					}
				}
			} else {
				found = new File(resource);
			}
			if (found == null) {
				throw new JspException("Cannot find resource " +
				    resource + " referenced in DiffableTag");
			} else {
				Map<File, String> currentVersions = ctx.getCurrentVersions();
				if(!currentVersions.containsKey(found)) {
					throw new JspException(
						"Cannot find current version of resource '" +
						found.getAbsolutePath() + ".'");
				} else {
					String resourceHash = hashPath(found);
					pageContext.getOut().println("<script type='text/javascript'>");
					pageContext.getOut().println(
						"if(!window['deltajs']) { window['deltajs'] = {}; }");
					pageContext.getOut().println(
						"window['deltajs']['" + resourceHash + "']={};");
					pageContext.getOut().println(
						"window['deltajs']['" + resourceHash + "']" +
						"['cv'] = '" +
						currentVersions.get(found) + "';");
					pageContext.getOut().println("</script>");
					pageContext.getOut().println(
					    "<script type='text/javascript' src='" +
						ctx.getServletPrefix() + "/" + resourceHash + "'></script>");
				}
			}
		} catch (Exception exc) {
			exc.printStackTrace();
		}
		return SKIP_BODY;
	}
}