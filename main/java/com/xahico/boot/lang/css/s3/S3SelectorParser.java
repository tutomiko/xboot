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
final class S3SelectorParser {
	private String              backup = null;
	private final StringBuilder buffer = new StringBuilder();
	private int                 checkpoint = -1;
	private int                 cursor = -1;
	private int                 depth = 0;
	private String              source = null;
	
	
	
	S3SelectorParser (){
		super();
	}
	
	
	
	private void compact (){
		this.buffer.delete(0, this.cursor);
		
		this.cursor = 0;
	}
	
	private void discard (){
		this.buffer.delete(0, this.buffer.length());
	}
	
	private String grab (final boolean compact){
		try {
			if ((this.buffer.length() > 0) && (this.cursor > 0)) 
				return this.buffer.substring(0, this.cursor);
			else {
				return "";
			}
		} finally {
			if (compact) {
				this.compact();
			}
		}
	}
	
	private void mark (){
		this.checkpoint = this.cursor;
		this.backup = this.buffer.toString();
	}
	
	private S3SelectorType match (char c){
		final char firstChar;
		
		firstChar = c;
		
		this.mark();
		
		select:
		for (final var selectorType : S3SelectorType.values()) {
			c = firstChar;
			
			if (selectorType == S3SelectorType.UNKNOWN) 
				continue;

			if (!selectorType.supportsPrefix() || (c != selectorType.prefix())) 
				continue;

			if (selectorType.supportsAlias()) {
				int aliasMatched = 0;

				match_alias:
				while (this.read()) {
					c = source.charAt(this.cursor);
					
					if (Character.isWhitespace(c)) 
						continue;
					
					if (!(aliasMatched < selectorType.alias().length())) {
						this.rewind();
						
						continue select;
					}

					if (c != selectorType.alias().charAt(aliasMatched)) {
						this.rewind();
						
						continue select;
					}

					aliasMatched++;
					if (aliasMatched == selectorType.alias().length()) {
						break;
					}
				}

				if (aliasMatched != selectorType.alias().length()) {
					this.rewind();
					
					continue;
				}
			}

			if (selectorType.supportsSuffix()) {
				boolean suffixMatched = false;

				while (this.read()) {
					c = source.charAt(this.cursor);

					if (c == selectorType.suffix()) {
						suffixMatched = true;

						break;
					}
				}

				if (! suffixMatched) {
					this.rewind();

					continue;
				}
			}

			return selectorType;
		}
		
		return null;
	}
	
	public S3Selector parseString (final String source){
		S3Selector rootSelector = null;
		
		this.source = source;
		this.cursor = -1;
		
		while (this.read()) {
			char           c;
			S3SelectorType recognizedType = null;
			
			c = source.charAt(this.cursor);
			
			if (Character.isWhitespace(c)) 
				continue;
			
			if (c == S3Syntax.SYM_SELF) 
				recognizedType = S3SelectorType.SELF;
			
			if (null == recognizedType) {
				recognizedType = this.match(c);
			}
			
			if (null != recognizedType) {
				final String     content;
				final S3Selector selector;
				
				content = recognizedType.injector().inject(buffer.toString().strip());
				
				if (null != content) {
					selector = new S3Selector(recognizedType, content);

					if (null == rootSelector) {
						rootSelector = selector;
					} else {
						rootSelector.join(selector);
					}
				}
				
				this.discard();
			}
		}
		
		if (! this.buffer.isEmpty()) {
			return this.readSelectors(rootSelector);
		}
		
		return rootSelector;
	}
	
	private boolean read (){
		final char c;
		
		if (!(this.cursor + 1 < this.source.length())) {
			return false;
		}
		
		this.cursor++;
		
		c = this.source.charAt(this.cursor);
		
		this.buffer.append(c);
		
		if (c == S3Syntax.SYM_CLOSED_LEFT) {
			this.depth++;
			
			return this.read();
		}
		
		if (c == S3Syntax.SYM_CLOSED_RIGHT) {
			this.depth--;
			
			return this.read();
		}
		
		if (this.depth != 0) {
			return this.read();
		}
		
		return true;
	}
	
	private S3Selector readSelectors (S3Selector rootSelector){
		for (; this.cursor < this.buffer.length(); this.cursor++) {
			final char c;
			
			c = this.buffer.charAt(this.cursor);
			
			if (c == S3Syntax.SYM_CLOSED_LEFT) {
				this.depth++;
				
				continue;
			}

			if (c == S3Syntax.SYM_CLOSED_RIGHT) {
				this.depth--;
				
				continue;
			}

			if (this.depth != 0) {
				continue;
			}
			
			if (Character.isWhitespace(c)) {
				final String     data;
				final S3Selector selector;
				
				data = this.grab(true);
				
				if (data.isBlank()) 
					continue;
				
				selector = new S3Selector(S3SelectorType.UNKNOWN, data.strip());
				
				if (null == rootSelector) {
					rootSelector = selector;
				} else {
					rootSelector.join(selector);
				}
			}
		}
		
		if (! this.buffer.isEmpty()) {
			final String     data;
			final S3Selector selector;
			
			data = this.buffer.toString();
			
			if (! data.isBlank()) {
				selector = new S3Selector(S3SelectorType.UNKNOWN, data.strip());
				
				if (null == rootSelector) {
					rootSelector = selector;
				} else {
					rootSelector.join(selector);
				}
			}
		}
		
		return rootSelector;
	}
	
	private void rewind (){
		this.cursor = this.checkpoint;
		this.buffer.delete(0, this.buffer.length());
		this.buffer.append(this.backup);
	}
}