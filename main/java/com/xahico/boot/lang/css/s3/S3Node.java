/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.xahico.boot.lang.css.s3;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * TBD.
 * 
 * @author Tuomas Kontiainen
**/
public class S3Node {
	private final List<S3Node> childNodes = new ArrayList<>();
	private String             content = null;
	private String             name = null;
	private final S3Node       parentNode;
	private S3Selector         selector = null;
	private S3NodeType         type = S3NodeType.UNKNOWN;
	
	
	
	S3Node (final S3Node parentNode){
		super();
		
		this.parentNode = parentNode;
	}
	
	
	
	public void addChild (final S3Node node){
		this.childNodes.add(node);
	}
	
	public String compile (){
		final List<S3Path>  paths;
		final StringBuilder sb;
		
		sb = new StringBuilder();
		
		if (null != this.name) {
			sb.append(this.name);
			sb.append(S3Syntax.SYM_SET);
			sb.append(this.content);
			sb.append(S3Syntax.SYM_LINE);
		}
		
		if (null != this.selector) {
			paths = this.path();

			for (var i = 0; i < paths.size(); i++) {
				final S3Path path;

				path = paths.get(i);

				if (! path.nestable()) 
					continue;

				if (!sb.isEmpty() && !path.interconnect()) {
					sb.append(" ");
				}

				sb.append(path.path());
			}

			sb.append(" ");
			sb.append("{");
			
			for (var i = 0; i < this.childNodes.size(); i++) {
				final S3Node childNode;

				childNode = this.childNodes.get(i);

				if (childNode.type != S3NodeType.COMPONENT) 
					continue;

				sb.append(childNode.compile());
			}
			
			sb.append("}");

			for (final var path : paths) {
				final String capture;

				if (path.nestable()) 
					continue;

				capture = sb.toString();
				
				sb.delete(0, sb.length());
				
				if (capture.isBlank() || capture.strip().equals("{}")) 
					continue;
				
				sb.append(path.path());
				sb.append(" ");
				sb.append("{");
				sb.append(capture);
				sb.append("}");
			}
		}
		

		for (var i = 0; i < this.childNodes.size(); i++) {
			final S3Node childNode;
			
			childNode = this.childNodes.get(i);
			
			if (childNode.type == S3NodeType.COMPONENT) 
				continue;
			
			if (! childNode.selector.getType().collapsable()) 
				continue;
			
			sb.append(childNode.compile());
		}
		
		for (var i = 0; i < this.childNodes.size(); i++) {
			final S3Node childNode;
			
			childNode = this.childNodes.get(i);
			
			if (childNode.type == S3NodeType.COMPONENT) 
				continue;
			
			if (childNode.selector.getType().collapsable()) 
				continue;
			
			sb.append(childNode.toStyleString(0, false));
		}
		
		
		return sb.toString();
	}
	
	public List<S3Node> getChildren (){
		return this.childNodes;
	}
	
	public List<S3Path> path (){
		final LinkedList<S3Path> path;
		
		if (null != this.parentNode) {
			path = (LinkedList<S3Path>) this.parentNode.path();
		} else {
			path = new LinkedList<>();
		}
		
		if ((null != this.selector) && !this.selector.path().isEmpty()) {
			switch (this.selector.getType().requireWord()) {
				case HEAD:
					path.addFirst(new S3Path(this.selector.path(), this.selector.getType().nestable(), this.selector.getType().requireInterConnect()));
					
					break;
				case INHERIT:
					
				case TAIL:
					path.addLast(new S3Path(this.selector.path(), this.selector.getType().nestable(), this.selector.getType().requireInterConnect()));

					break;
				default: {
					throw new InternalError();
				}
			}
		}
		
		return path;
	}
	
	public void setContent (final String content){
		this.content = content;
	}
	
	public void setName (final String name){
		this.name = name;
	}
	
	public void setSelector (final S3Selector selector){
		this.selector = selector;
	}
	
	public void setType (final S3NodeType type){
		this.type = type;
	}
	
	@Override
	public String toString (){
		if (true) return this.compile();
		return this.toStyleString();
	}
	
	public String toStyleString (){
		if (true) return this.compile();
		return this.toStyleString(0, true);
	}
	
	public String toStyleString (final int depth, final boolean readable){
		final StringBuilder sb;
		
		sb = new StringBuilder();
		
		if (null != this.selector) {
			if (readable && (depth > 0)) {
				sb.append("\t".repeat(depth));
			}
			
			sb.append(this.selector.path());
			
			if (readable) {
				sb.append(" ");
			}
			
			sb.append("{");
			
			if (readable) {
				sb.append("\n");
			}
		}
		
		if (null != this.name) {
			if (readable && (depth > 0)) {
				sb.append("\t".repeat(depth));
			}
			
			sb.append(this.name);
			sb.append(S3Syntax.SYM_SET);
			
			if (readable) {
				sb.append(' ');
			}
			
			sb.append(this.content);
			sb.append(";");
		}
		
		for (var i = 0; i < this.childNodes.size(); i++) {
			final S3Node childNode;
			
			childNode = this.childNodes.get(i);
			
			sb.append(childNode.toStyleString((depth + 1), readable));
			
			if (readable && ((i + 1) < this.childNodes.size())) {
				sb.append("\n");
			}
		}
		
		if (null != this.selector) {
			if (readable) {
				sb.append("\n");
				
				if (depth > 0) {
					sb.append("\t".repeat(depth));
				}
			}
			
			sb.append("}");
		}
		
		return sb.toString();
	}
}