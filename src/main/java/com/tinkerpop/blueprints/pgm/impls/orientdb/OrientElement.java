package com.tinkerpop.blueprints.pgm.impls.orientdb;

import java.util.Set;

import com.orientechnologies.orient.core.db.graph.OGraphElement;
import com.orientechnologies.orient.core.id.ORID;
import com.tinkerpop.blueprints.pgm.Element;

/**
 * @author Luca Garulli (http://www.orientechnologies.com)
 */
public abstract class OrientElement implements Element {

  protected static final String LABEL = "label";
  protected final OrientGraph   graph;
  protected final OGraphElement raw;

  protected OrientElement(final OrientGraph iGraph, final OGraphElement iElement) {
    this.graph = iGraph;
    this.raw = iElement;

    iGraph.putElementInCache(iElement.getDocument().getIdentity(), this);
  }

  public void setProperty(final String iKey, final Object value) {
    final Object oldValue = raw.get(iKey);

    graph.beginTransaction();

    try {
      this.raw.set(iKey, value);
      this.save();

      if (oldValue != null)
        // REMOVE OLD INDEXED PROPERTY
        graph.getIndex().remove(iKey, oldValue, this);
      graph.getIndex().put(iKey, value, this);
      graph.commitTransaction();

    } catch (RuntimeException e) {

      graph.rollbackTransaction();
      throw e;
    }
  }

  public Object removeProperty(final String iKey) {
    graph.beginTransaction();

    try {
      final Object old = this.raw.remove(iKey);
      this.save();
      graph.getIndex().remove(iKey, old, this);
      graph.commitTransaction();

      return old;
    } catch (RuntimeException e) {

      graph.rollbackTransaction();
      throw e;
    }
  }

  public Object getProperty(final String key) {
    return this.raw.get(key);
  }

  public Set<String> getPropertyKeys() {
    final Set<String> set = this.raw.propertyNames();
    set.remove(LABEL);
    return set;
  }

  /**
   * Returns the Element Id assuring to save it if it's transient yet.
   */
  public Object getId() {
    ORID rid = this.raw.getId();
    this.save();
    return rid;
  }

  protected void delete() {
    graph.beginTransaction();

    try {
      graph.removeElementFromCache(this.raw.getId());
      this.raw.delete();
      graph.commitTransaction();

    } catch (RuntimeException e) {

      graph.rollbackTransaction();
      throw e;
    }
  }

  protected void save() {
    graph.beginTransaction();

    try {
      this.raw.save();
      graph.commitTransaction();

    } catch (RuntimeException e) {

      graph.rollbackTransaction();
      throw e;
    }
  }

  public OGraphElement getRaw() {
    return raw;
  }

  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((this.raw == null) ? 0 : this.raw.hashCode());
    return result;
  }

  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    final OrientElement other = (OrientElement) obj;
    if (this.raw == null) {
      if (other.raw != null)
        return false;
    } else if (!this.raw.equals(other.raw))
      return false;
    return true;
  }
}
