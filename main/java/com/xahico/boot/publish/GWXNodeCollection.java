/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.xahico.boot.publish;

import com.xahico.boot.lang.jsox.JSOXArray;
import com.xahico.boot.util.Filter;
import com.xahico.boot.util.OrderedConsumer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * TBD.
 * 
 * @param <T> 
 * TBD.
 * 
 * @author Tuomas Kontiainen
**/
public abstract class GWXNodeCollection <T extends GWXNode> extends GWXObject implements GWXSerializableCollection<T> {
	protected GWXNodeCollection (){
		super();
	}
	
	protected GWXNodeCollection (final GWXNode owner){
		super();
		
		this.link(owner);
	}
	
	
	
	public abstract void add (final T node);
	
	public GWXNodeCollection<T> begin (final int position){
		return new GWXNodeCollection<>((GWXNode)this.getParent()) {
			@Override
			public void add (final T node){
				GWXNodeCollection.this.add(node);
			}
			
			@Override
			public boolean contains (final T node){
				return GWXNodeCollection.this.contains(node);
			}
			
			@Override
			public T lookup (final Object key){
				return GWXNodeCollection.this.lookup(key);
			}
			
			@Override
			public void remove (final T node){
				GWXNodeCollection.this.remove(node);
			}
			
			@Override
			public int size (){
				return GWXNodeCollection.this.size();
			}
			
			@Override
			public void walk (final OrderedConsumer<T> consumer) {
				GWXNodeCollection.this.walk(new OrderedConsumer<>() {
					int cursor = -1;
					
					@Override
					public boolean accept (final T node){
						cursor++;
						
						if (cursor >= position) {
							return consumer.accept(node);
						} else {
							return true;
						}
					}
				});
			}
		};
	}
	
	protected final void bind (final T node){
		node.link(this);
	}
	
	@Override
	protected void cleanup (){
		this.walk((node) -> {
			node.cleanup();
		});
		
		super.cleanup();
	}
	
	public abstract boolean contains (final T node);
	
	public GWXNodeCollection<T> filtered (final Filter<T>... filters){
		return this.filtered(Arrays.asList(filters));
	}
	
	public GWXNodeCollection<T> filtered (final List<Filter<T>> filters){
		return new GWXNodeCollection<>((GWXNode)this.getParent()) {
			@Override
			public void add (final T node){
				for (final var filter : filters) {
					if (! filter.accept(node)) {
						return;
					}
				}
				
				GWXNodeCollection.this.add(node);
			}
			
			@Override
			public boolean contains (final T node){
				for (final var filter : filters) {
					if (! filter.accept(node)) {
						return false;
					}
				}
				
				return GWXNodeCollection.this.contains(node);
			}
			
			@Override
			public T lookup (final Object key){
				final T result;
				
				result = GWXNodeCollection.this.lookup(key);
				
				for (final var filter : filters) {
					if (! filter.accept(result)) {
						return null;
					}
				}
				
				return result;
			}
			
			@Override
			public void remove (final T node){
				for (final var filter : filters) {
					if (! filter.accept(node)) {
						return;
					}
				}
				
				GWXNodeCollection.this.remove(node);
			}
			
			@Override
			public int size (){
				return GWXNodeCollection.this.size();
			}
			
			@Override
			public void walk (final OrderedConsumer<T> consumer) {
				GWXNodeCollection.this.walk((node) -> {
					for (final var filter : filters) {
						if (! filter.accept(node)) {
							return;
						}
					}
					
					consumer.accept(node);
				});
			}
		};
	}
	
	public GWXNodeCollection<T> limited (final int max){
		return new GWXNodeCollection<>((GWXNode)this.getParent()) {
			@Override
			public void add (final T node){
				GWXNodeCollection.this.add(node);
			}
			
			@Override
			public boolean contains (final T node){
				return GWXNodeCollection.this.contains(node);
			}
			
			@Override
			public T lookup (final Object key){
				return GWXNodeCollection.this.lookup(key);
			}
			
			@Override
			public void remove (final T node){
				GWXNodeCollection.this.remove(node);
			}
			
			@Override
			public int size (){
				return GWXNodeCollection.this.size();
			}
			
			@Override
			public void walk (final OrderedConsumer<T> consumer) {
				GWXNodeCollection.this.walk(new OrderedConsumer<>() {
					int count = -1;
					
					@Override
					public boolean accept (final T node){
						count++;
						
						if (count < max) {
							return consumer.accept(node);
						} else {
							return false;
						}
					}
				});
			}
		};
	}
	
	public abstract T lookup (final Object key);
	
	@Override
	final String name (){
		if ((null == super.name()) && (this.getParent() instanceof GWXNode parentNode)) {
			this.name(parentNode.getPropertyKey(this));
		}
		
		return super.name();
	}
	
	public GWXNodeCollection<T> ordered (final Comparator<T> comparator){
		return new GWXNodeCollection<>((GWXNode)this.getParent()) {
			@Override
			public void add (final T node){
				GWXNodeCollection.this.add(node);
			}
			
			@Override
			public boolean contains (final T node){
				return GWXNodeCollection.this.contains(node);
			}
			
			@Override
			public T lookup (final Object key){
				return GWXNodeCollection.this.lookup(key);
			}
			
			@Override
			public void remove (final T node){
				GWXNodeCollection.this.remove(node);
			}
			
			@Override
			public int size (){
				return GWXNodeCollection.this.size();
			}
			
			@Override
			public void walk (final OrderedConsumer<T> consumer){
				final List<T> nodes;
				
				nodes = new ArrayList<>();
				
				GWXNodeCollection.this.walk((node) -> {
					nodes.add(node);
					
					return true;
				});

				nodes.sort(comparator);
				
				for (final var node : nodes) {
					if (! consumer.accept(node)) {
						break;
					}
				}
			}
		};
	}
	
	public abstract void remove (final T node);
	
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
			
			this.walk(new Consumer<>() {
				boolean acceptNext = true;

				@Override
				public void accept (final T object){
					if (acceptNext) {
						acceptNext = consumer.call((from + "/" + object.getId()), object);
					}
				}
			});
		} else if (key.equals("*")) {
			if (last) {
				this.walk((object) -> {
					return consumer.call((from + "/" + object.getId()), object);
				});
			} else {
				this.walk((object) -> {
					object.select(next, (from + "/" + object.getId()), consumer);
				});
			}
		} else {
			final GWXObject object;
			
			object = this.lookup(key);
			
			if (null != object) {
				if (last) {
					consumer.call((from + "/" + key), object);
				} else {
					object.select(next, (from + "/" + key), consumer);
				}
			}
		}
	}
	
	@Override
	public JSOXArray serialize (final boolean internal){
		final JSOXArray serialized;
		
		serialized = new JSOXArray();
		
		this.walk((object) -> {
			serialized.append(object.serialize(internal));
			
			return true;
		});
		
		return serialized;
	}
	
	public abstract int size ();
	
	@Override
	public final JSOXArray snapshot (){
		return JSOXArray.wrap(this.toArray());
	}
	
	public Object[] toArray (){
		final Object[] array;
		
		array = new Object[this.size()];
		
		return this.toArray(array);
	}
	
	public <E> E[] toArray (final E[] array){
		this.walk(new Consumer<>() {
			int index = 0;
			
			@Override
			public void accept (final T object){
				if (index < array.length) {
					array[index] = ((E)object);
					
					index++;
				}
			}
		});
		
		return array;
	}
	
	protected final void unbind (final T node){
		node.unlink();
	}
	
	public final void walk (final Consumer<T> consumer){
		this.walk((object) -> {
			consumer.accept(object);
			
			return true;
		});
	}
	
	public final void walk (final Consumer<T> consumer, final Runnable callback){
		try {
			this.walk(consumer);
		} finally {
			callback.run();
		}
	}
	
	public abstract void walk (final OrderedConsumer<T> consumer);
	
	public final void walk (final OrderedConsumer<T> consumer, final Runnable callback){
		try {
			this.walk(consumer);
		} finally {
			callback.run();
		}
	}
}