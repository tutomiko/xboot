/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.xahico.boot.publish;

import com.xahico.boot.lang.jsox.JSOXVariant;

/**
 * TBD.
 * 
 * @author Tuomas Kontiainen
**/
public class GWXNode extends GWXObject implements GWXSerializable {
	private final GWXProperties properties;
	
	
	
	protected GWXNode (){
		super();
		
		this.properties = GWXProperties.getProperties(this.getClass());
	}
	
	GWXNode (final GWXProperties properties){
		super();
		
		this.properties = properties;
	}
	
	
	
	final boolean checkKey (final Object key){
		final Object keyOwn;
		
		if (null == key) 
			return false;
		
		keyOwn = this.getId();
		
		if (key == keyOwn) 
			return true;
		
		if (keyOwn instanceof String) 
			return key.toString().equals(keyOwn);
		
		return key.equals(keyOwn);
	}
	
	@Override
	protected void cleanup (){
		for (final var propertyKey : this.properties) {
			final Object property;
			
			property = this.properties.get(this, propertyKey);
			
			if (null == property) 
				continue;
			
			if (property instanceof GWXObject propertyObject) {
				propertyObject.cleanup();
			}
		}
		
		super.cleanup();
	}
	
	final Object getId (){
		final Object id;
		
		id = this.properties.id(this);
		
		if (null != id) {
			return id;
		} else {
			return super.name();
		}
	}
	
	final GWXProperties getProperties (){
		return this.properties;
	}
	
	public final Object getProperty (final String key){
		return this.properties.get(this, key);
	}
	
	final String getPropertyKey (final Object object){
		for (final var propertyKey : this.properties) {
			final Object property;
			
			property = this.properties.get(this, propertyKey);
			
			if (property == object) {
				return propertyKey;
			}
		}
		
		return null;
	}
	
	@Override
	final String name (){
		final Object id;
		
		id = this.getId();
		
		if (null == id) 
			return "";
		
		return id.toString();
	}
	
	@Override
	final void select (final String path, final String from, final GWXSelector consumer){
		final int     delimiter;
		final String  key;
		final boolean last;
		final String  next;
		
		delimiter = path.indexOf('/');
		
		if (delimiter == -1) {
			key = path;
			next = null;
			last = true;
		} else {
			key = path.substring(0, delimiter);
			next = path.substring(delimiter + 1);
			last = next.isEmpty();
		}
		
		if (key.equals("**") && last) {
			consumer.call(from, this);
			
			for (final var propertyKey : this.properties) {
				final Object property;
				
				property = this.properties.get(this, propertyKey);
				
				if (property instanceof GWXObject propertyObject) {
					propertyObject.select(key, (from + "/" + propertyKey), consumer);
				}
			}
		} else if (key.equals("*")) {
			if (last) {
				for (final var propertyKey : this.properties) {
					final Object property;
					
					property = this.properties.get(this, propertyKey);
					
					if (property instanceof GWXObject) {
						if (! consumer.call((from + "/" + propertyKey), (GWXObject)property)) {
							break;
						}
					}
				}
			} else {
				for (final var propertyKey : this.properties) {
					final Object property;
					
					property = this.properties.get(this, propertyKey);
					
					if (property instanceof GWXObject propertyObject) {
						propertyObject.select(next, (from + "/" + propertyKey), consumer);
					}
				}
			}
		} else {
			final Object property;
			
			property = this.properties.get(this, key);
			
			if ((null != property) && (property instanceof GWXObject propertyObject)) {
				if (last) {
					consumer.call((from + "/" + key), propertyObject);
				} else {
					propertyObject.select(next, (from + "/" + key), consumer);
				}
			}
		}
	}
	
	@Override
	public JSOXVariant serialize (final boolean internal){
		final JSOXVariant serialized;
		
		serialized = new JSOXVariant();
		
		for (final var propertyKey : this.properties) {
			final Object propertyObject;
			final Object propertySerialized;
			
			propertyObject = this.properties.get(this, propertyKey);
			
			if (propertyObject instanceof GWXNode propertyNode) {
				propertySerialized = propertyNode.serialize(internal);
			} else if (propertyObject instanceof GWXNodeCollection propertyNodes) {
				propertySerialized = propertyNodes.serialize(internal);
			} else {
				propertySerialized = propertyObject;
			}
			
			serialized.put(propertyKey, propertySerialized);
		}
		
		return serialized;
	}
	
	public final JSOXVariant snapshot (){
		return this.serialize(false);
	}
}