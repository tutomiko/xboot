/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.xahico.boot.publish;

/**
 * TBD.
 * 
 * @author Tuomas Kontiainen
**/
final class GWXWebBridgeBuilder {
	private static final String FILE_CORE = "GWXWebCore.js";
	private static final String FILE_STUB = "GWXWebBridge.js";
	private static final String REF_GATEWAY = "gateway";
	private static final String REF_TIMEOUT = "timeout";
	private static final String REF_TOKEN = "token";
	public static final String  REF_SELF = "this";
	public static final String  VAR_SELF = "__GWXWebApplication__";
	
	
	
	private final String          baseURL;
	private final GWXAPIInterface iface;
	private final GWXNamespace    namespace = new GWXNamespace();
	private final GWXSession      session;
	
	
	
	GWXWebBridgeBuilder (final GWXSession session, final GWXAPIInterface iface, final String baseURL){
		super();
		
		this.session = session;
		this.iface = iface;
		this.baseURL = baseURL;
	}
	
	
	
	public String build (){
		final StringBuilder sb;
		
		this.namespace.set(REF_GATEWAY, baseURL);
		this.namespace.set(REF_TIMEOUT, ((this.session.timeout() == 0) ? 0 : (this.session.timeout() * 1000)));
		this.namespace.set(REF_TOKEN, GWXUtilities.buildTokenIdentity(this.session.getClass()));
		
		sb = new StringBuilder();
		sb.append(GWXResourceLoader.loadResource(FILE_CORE));
		sb.append(GWXImporter.importString(GWXResourceLoader.loadResource(FILE_STUB), this.namespace));
		
		return sb.toString();
	}
}