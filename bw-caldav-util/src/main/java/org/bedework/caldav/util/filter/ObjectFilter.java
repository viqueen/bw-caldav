/* ********************************************************************
    Licensed to Jasig under one or more contributor license
    agreements. See the NOTICE file distributed with this work
    for additional information regarding copyright ownership.
    Jasig licenses this file to you under the Apache License,
    Version 2.0 (the "License"); you may not use this file
    except in compliance with the License. You may obtain a
    copy of the License at:

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on
    an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied. See the License for the
    specific language governing permissions and limitations
    under the License.
*/
package org.bedework.caldav.util.filter;

import org.bedework.caldav.util.TimeRange;

import org.bedework.util.calendar.PropertyIndex.PropertyInfoIndex;

/** A filter that selects events which match the single object value.
 *
 * <p>In CalDAV a property filter provides the following tests:<ul>
 * <li>presence or non-presence - will be the subclass PresenceFilter</li>
 * <li>time-range - will be the subclass TimeRangeFilter</li>
 * <li>text-match - entity type will be String</li>
 *
 * @author Mike Douglass douglm
 * @param <T> the type of entity
 */
public class ObjectFilter<T> extends PropertyFilter {
  private T entity;

  private boolean exact = true;

  private boolean caseless = true;

  /** Match on any of the entities.
   *
   * @param name - null one will be created
   * @param propertyIndex
   */
  public ObjectFilter(final String name, final PropertyInfoIndex propertyIndex) {
    super(name, propertyIndex);
  }

  /** Set the entity we're filtering on
   *
   *  @param val     entity
   */
  public void setEntity(final T val) {
    entity = val;
  }

  /** Get the entity we're filtering on
   *
   *  @return T     entity we're filtering on
   */
  public T getEntity() {
    return entity;
  }

  /** Set the exact flag
   *
   * @param val
   */
  public void setExact(final boolean val) {
    exact = val;
  }

  /** See if we do exact match
   *
   *  @return boolean true if exact
   */
  public boolean getExact() {
    return exact;
  }

  /** Set the caseless flag
   *
   * @param val
   */
  public void setCaseless(final boolean val) {
    caseless = val;
  }

  /** See if we do caseless match
   *
   *  @return boolean true if caseless
   */
  public boolean getCaseless() {
    return caseless;
  }

  /* * Create a filter for the given index and value
   *
   * @param name
   * @param propertyIndex
   * @param val
   * @return BwObjectFilter
   * /
  public static BwObjectFilter makeFilter(String name,
                                          PropertyInfoIndex propertyIndex,
                                          String val) {
    switch (propertyIndex) {
    case CLASS:
      break;

    case CREATED:
      break;

    case DESCRIPTION:
      BwIstringFilter descfilter = BwIstringFilter.makeDescriptionFilter(name);
      descfilter.setValue(null, val);
      return descfilter;

    case DTSTAMP:
      break;

    case DTSTART:
      break;

    case DURATION:
      break;

    case GEO:
      break;

    case LAST_MODIFIED:
      break;

    case LOCATION:
      BwLocationFilter locfilter = new BwLocationFilter(name);
      BwLocation loc = new BwLocation();
      loc.setAddress(new BwString(null, val));
      locfilter.setEntity(loc);
      break;

    case ORGANIZER:
      break;

    case PRIORITY:
      break;

    case RECURRENCE_ID:
      break;

    case SEQUENCE:
      break;

    case STATUS:
      break;

    case SUMMARY:
      BwIstringFilter sumfilter = BwIstringFilter.makeSummaryFilter(name);
      sumfilter.setValue(null, val);
      return sumfilter;

    case UID:
      break;

    case URL:
      break;

  / * Event only * /

    case DTEND:
      break;

    case TRANSP:
      break;

  / * Todo only * /

    case COMPLETED:
      break;

    case DUE:
      break;

    case PERCENT_COMPLETE:
      break;

  / * ---------------------------- Multi valued --------------- * /

  / * Event and Todo * /

    case ATTACH:
      break;

    case ATTENDEE :
      break;

    case CATEGORIES:
      BwCategoryFilter catfilter = new BwCategoryFilter(name);
      BwCategory cat = new BwCategory();
      cat.setWord(new BwString(null, val));
      catfilter.setEntity(cat);
      return catfilter;

    case COMMENT:
      break;

    case CONTACT:
      BwContactFilter ctctfilter = new BwContactFilter(name);
      BwContact ctct = new BwContact();
      ctct.setName(new BwString(null, val));
      ctctfilter.setEntity(ctct);
      return ctctfilter;

    case EXDATE:
      break;

    case EXRULE :
      break;

    case REQUEST_STATUS:
      break;

    case RELATED_TO:
      break;

    case RESOURCES:
      break;

    case RDATE:
      break;

    case RRULE :
      break;

  / * -------------- Other non-event: non-todo ---------------- * /

    case FREEBUSY:
      break;

    case TZID:
      break;

    case TZNAME:
      break;

    case TZOFFSETFROM:
      break;

    case TZOFFSETTO:
      break;

    case TZURL:
      break;

    case ACTION:
      break;

    case REPEAT:
      break;

    case TRIGGER:
      break;

    case CREATOR:
      BwCreatorFilter crefilter = new BwCreatorFilter(name);
      crefilter.setEntity(val);
      return crefilter;

    case OWNER:
      break;

    case ENTITY_TYPE:
      break;

    }

    BwObjectFilter<String> sfilter = new BwObjectFilter<String>(null, propertyIndex);
    sfilter.setEntity(val);

    return sfilter;
  }
  */

  /** Create a timerange filter for the given index and value
   *
   * @param name
   * @param propertyIndex
   * @param val     TimeRange
   * @return BwObjectFilter
   */
  public static ObjectFilter makeFilter(final String name,
                                          final PropertyInfoIndex propertyIndex,
                                          final TimeRange val) {
    TimeRangeFilter trf = new TimeRangeFilter(name, propertyIndex);

    trf.setEntity(val);

    return trf;
  }

  /* ====================================================================
   *                   Object methods
   * ==================================================================== */

  @Override
  public String toString() {
    if (getPropertyIndex()== PropertyInfoIndex.CATEGORIES) {
      StringBuilder sb = new StringBuilder("(categories=");
      sb.append(getEntity());
      sb.append(")");

      return sb.toString();
    }

    StringBuilder sb = new StringBuilder("BwObjectFilter{");

    super.toStringSegment(sb);
    sb.append("\nobj=");
    sb.append(getEntity());

    sb.append("}");

    return sb.toString();
  }
}