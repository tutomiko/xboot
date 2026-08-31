/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.xahico.boot.lang.css.s3;

/**
 * TBD.
 * 
 * @author Tuomas Kontiainen
**/
public enum S3SelectorType {
	ATTRIBUTE(null, S3Syntax.SYM_ATTRIB_BEGIN, S3Syntax.SYM_ATTRIB_END, true, S3WordPosition.TAIL, true, true, true),
	CALL(null, S3Syntax.SYM_CALL, S3SelectorType.FIX_NONE, true, S3WordPosition.TAIL, true, true, true),
	CLASS(null, S3Syntax.SYM_CLASS, S3SelectorType.FIX_NONE, true, S3WordPosition.TAIL, true, true, false),
	DIRECT_CHILD(null, S3Syntax.SYM_DIRECT_CHILD, S3SelectorType.FIX_NONE, true, S3WordPosition.TAIL, true, false, false),
	ID(null, S3Syntax.SYM_REF, S3SelectorType.FIX_NONE, true, S3WordPosition.TAIL, true, true, false),
	KEYFRAMES("keyframes", S3Syntax.SYM_WILD, S3SelectorType.FIX_NONE, false, S3WordPosition.HEAD, false, false, false),
	KEYFRAMES_WEBKIT("-webkit-keyframes", S3Syntax.SYM_WILD, S3SelectorType.FIX_NONE, false, S3WordPosition.HEAD, false, false, false),
	MEDIA("media", S3Syntax.SYM_WILD, S3SelectorType.FIX_NONE, false, S3WordPosition.HEAD, true, false, false),
	SELF(null, S3SelectorType.FIX_NONE, S3SelectorType.FIX_NONE, true, S3WordPosition.TAIL, true, true, false, (string) -> null),
	UNKNOWN(null, S3SelectorType.FIX_NONE, S3SelectorType.FIX_NONE, true, S3WordPosition.INHERIT, true, false, false);
	
	
	
	private static final char FIX_NONE = '\0';
	
	
	
	private final String         alias;
	private final boolean        collapsable;
	private final S3Injector     injector;
	private final boolean        nestable;
	private final char           prefix;
	private final boolean        requireConnect;
	private final boolean        requireInterConnect;
	private final S3WordPosition requireWord;
	private final char           suffix;
	
	
	
	S3SelectorType (final String alias, final char prefix, final char suffix, final boolean nestable, final S3WordPosition requireWord, final boolean collapsable, final boolean requireConnect, final boolean requireInterConnect){
		this.alias = alias;
		this.prefix = prefix;
		this.suffix = suffix;
		this.nestable = nestable;
		this.requireWord = requireWord;
		this.collapsable = collapsable;
		this.requireConnect = requireConnect;
		this.requireInterConnect = requireInterConnect;
		this.injector = S3Injector.EMPTY;
	}
	
	S3SelectorType (final String alias, final char prefix, final char suffix, final boolean nestable, final S3WordPosition requireWord, final boolean collapsable, final boolean requireConnect, final boolean requireInterConnect, final S3Injector injector){
		this.alias = alias;
		this.prefix = prefix;
		this.suffix = suffix;
		this.nestable = nestable;
		this.requireWord = requireWord;
		this.collapsable = collapsable;
		this.requireConnect = requireConnect;
		this.requireInterConnect = requireInterConnect;
		this.injector = injector;
	}
	
	
	
	public String alias (){
		return this.alias;
	}
	
	public boolean collapsable (){
		return this.collapsable;
	}
	
	public S3Injector injector (){
		return this.injector;
	}
	
	public boolean nestable (){
		return this.nestable;
	}
	
	public char prefix (){
		return this.prefix;
	}
	
	public boolean requireConnect (){
		return this.requireConnect;
	}
	
	public boolean requireInterConnect (){
		return this.requireInterConnect;
	}
	
	public S3WordPosition requireWord (){
		return this.requireWord;
	}
	
	public char suffix (){
		return this.suffix;
	}
	
	public boolean supportsAlias (){
		return (null != this.alias);
	}
	
	public boolean supportsPrefix (){
		return (this.prefix != FIX_NONE);
	}
	
	public boolean supportsSuffix (){
		return (this.suffix != FIX_NONE);
	}
}