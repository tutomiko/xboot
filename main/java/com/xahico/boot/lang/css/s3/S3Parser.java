/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.xahico.boot.lang.css.s3;

import com.xahico.boot.io.Source;
import com.xahico.boot.util.StringUtilities;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * TBD.
 * 
 * @author Tuomas Kontiainen
**/
public final class S3Parser {
	private static final int DEFAULT_CHUNK_SIZE = 256;
	
	
	
	private int                       chunkSize = DEFAULT_CHUNK_SIZE;
	
	private final StringBuilder       buffer = new StringBuilder();
	private int                       cursor = 0;
	private final Map<String, String> namespace = new HashMap<>();
	private Source                    source = null;
	private InputStream               stream = null;
	
	
	
	public S3Parser (){
		super();
	}
	
	
	
	private void compact (){
		this.buffer.delete(0, this.cursor);
		
		this.cursor = 0;
	}
	
	private String evalString (final String string){
		final StringBuilder sb;
		
		sb = new StringBuilder();
		
		for (var i = 0; i < string.length(); i++) {
			char c;
			
			c = string.charAt(i);
			
			if (c == S3Syntax.SYM_VAR) {
				final StringBuilder kb;
				final String        value;
				
				kb = new StringBuilder();
				
				for (i++; i < string.length(); i++) {
					c = string.charAt(i);
					
					if (Character.isWhitespace(c) || (!Character.isLetter(c) && !Character.isDigit(c))) {
						break;
					} else {
						kb.append(c);
					}
				}
				
				value = this.getGlobal(kb.toString());

				if (null != value) {
					sb.append(value);
					sb.append(' ');
				} else {
					sb.append(S3Syntax.SYM_VAR);
					sb.append(kb);
					sb.append(c);
				}
				
				continue;
			}
			
			sb.append(c);
		}
		
		return sb.toString();
	}
	
	private String getGlobal (final String key){
		return this.namespace.get(key.toLowerCase());
	}
	
	private String grab (final boolean compact){
		try {
			if ((this.buffer.length() > 0) && (this.cursor > 0)) 
				return this.buffer.substring(0, (this.cursor - 1));
			else {
				return "--";
			}
		} finally {
			if (compact) {
				this.compact();
			}
		}
	}
	
	public S3Document parse () throws IOException, S3Exception {
		try {
			final S3Document document;
			
			this.source.open();
			
			this.stream = this.source.stream();
			
			
			document = new S3Document();
			document.setType(S3NodeType.DOCUMENT);
			
			this.parseNamespace(document, false);
			
			return document;
		} finally {
			this.source.close();
		}
	}
	
	private void parseLine (final S3Node node, final String line){
		final int    delimiter;
		final String key;
		final String value;
		
		delimiter = line.indexOf(S3Syntax.SYM_SET);
		
		if (delimiter == -1) 
			throw new Error("invalid line '%s' at [[%s]]".formatted(line, this.buffer));
		
		key = line.substring(0, delimiter).strip();
		value = line.substring(delimiter + 1).strip();
		
		if (key.startsWith(Character.toString(S3Syntax.SYM_VAR))) {
			this.registerGlobal(key.substring(1), value);
		} else {
			node.setContent(evalString(value));
			node.setName(key);
		}
	}
	
	private void parseNamespace (final S3Node parentNode, final boolean structure) throws IOException {
		for (;;) {
			final S3Node node;
			
			node = this.parseObject(parentNode, structure);
			
			this.compact();
			
			if (null == node) {
				break;
			}
			
			parentNode.addChild(node);
		}
	}
	
	private S3Node parseObject (final S3Node parentNode, final boolean structure) throws IOException {
		int          depthQuote1 = 0;
		int          depthQuote2 = 0;
		final S3Node node;
		boolean      withinComment = false;
		boolean      withinQuote1 = false;
		boolean      withinQuote2 = false;
		
		node = new S3Node(parentNode);
		
		for (;;) {
			final char c;
			boolean    enterQuote = false;
			boolean    withinQuote = false;
			
			if (this.cursor < this.buffer.length()) {
				c = this.buffer.charAt(this.cursor);
				
				this.cursor++;
			} else {
				if (! this.read()) {
					return null;
				} else {
					continue;
				}
			}
			
			// Detect if within comment
			if (! withinComment) {
				if ((c == '*') && (cursor >= 1) && (buffer.charAt(cursor - 1) == '/')) {
					withinComment = true;
					
					continue;
				}
			} else {
				if ((c == '/') && (buffer.charAt(cursor - 1) == '*')) {
					this.compact();
					
					withinComment = false;
				}
				
				continue;
			}
			
			// Detect if within QUOT1-Type String ("")
			if (c == S3Syntax.SYM_QUOTE1) {
				int depth;

				if (withinQuote2) {
					continue;
				}

				depth = 0;

				for (var i = (buffer.length() - 1); i > -1; i--) {
					if (buffer.charAt(i) == S3Syntax.SYM_QUOTE_ESC) 
						depth++;
					else {
						break;
					}
				}

				if (withinQuote1) {
					if (depth == depthQuote1) {
						withinQuote1 = false;
						depthQuote1 = 0;
					}
				} else {
					enterQuote = true;
					withinQuote1 = true;
					depthQuote1 = depth;
				}
			}

			// Detect if within QUOT2-Type String ('')
			if (c == S3Syntax.SYM_QUOTE2) {
				int depth;

				if (withinQuote1) {
					continue;
				}

				depth = 0;

				for (var i = (buffer.length() - 1); i > -1; i--) {
					if (buffer.charAt(i) == S3Syntax.SYM_QUOTE_ESC) 
						depth++;
					else {
						break;
					}
				}

				if (withinQuote2) {
					if (depth == depthQuote2) {
						withinQuote2 = false;
						depthQuote2 = 0;
					}
				} else {
					enterQuote = true;
					withinQuote2 = true;
					depthQuote2 = depth;
				}
			}
			
			if (! enterQuote) {
				withinQuote = (withinQuote1 || withinQuote2);
			}
			
			if (withinQuote || enterQuote) 
				continue;
			
			if (c == S3Syntax.SYM_LINE) {
				final String lineContents;
				
				node.setType(S3NodeType.COMPONENT);
				
				lineContents = this.grab(true);
				
				if (! lineContents.isBlank()) {
					this.parseLine(node, lineContents);
				}
				
				break;
			}
			
			if (c == S3Syntax.SYM_STRUCTURE_LEFT) {
				node.setSelector(S3Utilities.parseSelectors(this.grab(true)));
				node.setType(S3NodeType.STRUCTURE);
				
				this.parseNamespace(node, true);
				
				this.compact();
				
				break;
			}
			
			if (c == S3Syntax.SYM_STRUCTURE_RIGHT) {
				if (! structure) {
					throw new Error("invalid struct end at [[%s]]".formatted(this.buffer));
				}
				
				this.compact();
				
				return null;
			}
		}
		
		return node;
	}
	
	private boolean read () throws IOException {
		final byte[] bytes;
		
		bytes = this.stream.readNBytes(this.chunkSize);
		
		if (bytes.length == 0) {
			return false;
		}
		
		this.buffer.append(new String(bytes));
		
		return true;
	}
	
	private void registerGlobal (final String key, final String value){
		this.namespace.put(key.toLowerCase(), (StringUtilities.isQuoted(value) ? this.evalString(StringUtilities.unquote(value)) : this.evalString(value)));
	}
	
	public void setChunkSize (final int chunkSize){
		this.chunkSize = chunkSize;
	}
	
	public void setSource (final Source source){
		this.source = source;
	}
}