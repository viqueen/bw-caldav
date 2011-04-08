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
package org.bedework.caldav.util.filter.parse;

import org.bedework.caldav.util.TimeRange;
import org.bedework.caldav.util.filter.EntityTimeRangeFilter;
import org.bedework.caldav.util.filter.EntityTypeFilter;
import org.bedework.caldav.util.filter.FilterBase;
import org.bedework.caldav.util.filter.ObjectFilter;
import org.bedework.caldav.util.filter.PresenceFilter;
import org.bedework.caldav.util.filter.PropertyFilter;

import edu.rpi.cct.webdav.servlet.shared.WebdavBadRequest;
import edu.rpi.cct.webdav.servlet.shared.WebdavException;
import edu.rpi.cct.webdav.servlet.shared.WebdavForbidden;
import edu.rpi.cmt.calendar.IcalDefs;
import edu.rpi.cmt.calendar.PropertyIndex.PropertyInfoIndex;
import edu.rpi.sss.util.Util;
import edu.rpi.sss.util.xml.tagdefs.CaldavTags;

import net.fortuna.ical4j.model.DateTime;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import ietf.params.xml.ns.caldav.CompFilter;
import ietf.params.xml.ns.caldav.Filter;
import ietf.params.xml.ns.caldav.ParamFilter;
import ietf.params.xml.ns.caldav.PropFilter;
import ietf.params.xml.ns.caldav.TextMatch;
import ietf.params.xml.ns.caldav.UTCTimeRangeType;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/** Class to parse and process query filters.
 *
 *   @author Mike Douglass   douglm@rpi.edu
 */
public class Filters {
  /** Convenience method
   *
   * @param cf
   * @return boolean true if this element matches all of the
   *                  named component types.
   */
  public static boolean matchAll(final CompFilter cf) {
    return (cf.getTimeRange() == null)  &&
            Util.isEmpty(cf.getCompFilters()) &&
            Util.isEmpty(cf.getPropFilters());
  }

  /** Convenience method
   *
   * @param tm
   * @return boolean true if this element matches all of the
   *                  named component types.
   */
  public static boolean caseless(final TextMatch tm) {
    return tm.getCollation().equals("i;ascii-casemap");
  }

  /** Given a caldav like xml filter parse it
   *
   * @param xmlStr
   * @return Filter
   * @throws WebdavException
   */
  public static Filter parse(final String xmlStr) throws WebdavException {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);

      DocumentBuilder builder = factory.newDocumentBuilder();

      Document doc = builder.parse(new InputSource(new StringReader(xmlStr)));

      return parse(doc.getDocumentElement());
    } catch (WebdavException cfe) {
      throw cfe;
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /** The given node must be the Filter element
   *
   * @param nd
   * @return Filter
   * @throws WebdavException
   */
  public static Filter parse(final Node nd) throws WebdavException {
    try {
      JAXBContext jc = JAXBContext.newInstance("ietf.params.xml.ns.caldav");
      Unmarshaller u = jc.createUnmarshaller();

      return (Filter)u.unmarshal(nd);
    } catch (Throwable t) {
      throw new WebdavException(t);
    }
  }

  /** Return an object encapsulating the filter query.
   *
   * @param f
   * @return EventQuery
   * @throws WebdavException
   */
  public static EventQuery getQuery(final Filter f) throws WebdavException {
    EventQuery eventq = new EventQuery();

    eventq.filter = getQueryFilter(f.getCompFilter(), eventq, 0);

    return eventq;
  }

  /** Returns a subtree of the filter used in querying
   *
   * @param cf
   * @param eq - so we can update time range
   * @param exprDepth - allows us to do validity checks
   * @return Filter - null for no filtering
   * @throws WebdavException
   */
  public static FilterBase getQueryFilter(final CompFilter cf,
                                          final EventQuery eq,
                                          final int exprDepth) throws WebdavException {
    FilterBase filter = null;
    int entityType = IcalDefs.entityTypeEvent;

    boolean isNotDefined = cf.getIsNotDefined() != null;
    String name = cf.getName().toUpperCase();

    if (exprDepth == 0) {
      if (!"VCALENDAR".equals(name)) {
        throw new WebdavBadRequest();
      }

//      if (Util.isEmpty(compFilters)) {
//        return null;
//      }
    } else if (exprDepth == 1) {
      // Calendar components only

      if ("VEVENT".equals(name)) {
        filter = EntityTypeFilter.eventFilter(null, isNotDefined);
        entityType = IcalDefs.entityTypeEvent;
      }

      if ("VTODO".equals(name)) {
        filter = EntityTypeFilter.todoFilter(null, isNotDefined);
        entityType = IcalDefs.entityTypeTodo;
      }

      if ("VJOURNAL".equals(name)) {
        filter = EntityTypeFilter.journalFilter(null, isNotDefined);
        entityType = IcalDefs.entityTypeJournal;
      }

      if ("VFREEBUSY".equals(name)) {
        filter = EntityTypeFilter.freebusyFilter(null, isNotDefined);
        entityType = IcalDefs.entityTypeFreeAndBusy;
      }

      if ("VAVAILABILITY".equals(name)) {
        filter = EntityTypeFilter.vavailabilityFilter(null, isNotDefined);
        entityType = IcalDefs.entityTypeVavailability;
      }

      if (filter == null) {
        throw new WebdavBadRequest();
      }
    } else if (exprDepth == 2) {
      // Sub-components only

      // XXX
      entityType = IcalDefs.entityTypeAlarm;

      filter = makeFilter(name, isNotDefined, matchAll(cf),
                          makeTimeRange(cf.getTimeRange()), null,
                          null);

      if (filter == null) {
        throw new WebdavBadRequest();
      }
    } else {
      throw new WebdavBadRequest("expr too deep");
    }

    if ((filter != null) && isNotDefined) {
      filter.setNot(true);
    }

    if (matchAll(cf)) {
      return filter;
    }

    if (exprDepth < 2) {
      /* XXX This is wrong - if filters handle time ranges OK we should remove
       * this merge which was here so post-processing could handle it.
       */
      if (cf.getTimeRange() != null) {
/*        if (eq.trange == null) {
          eq.trange = timeRange;
        } else {
          eq.trange.merge(timeRange);
        } */
        EntityTimeRangeFilter etrf = new EntityTimeRangeFilter(null,
                        makeTimeRange(cf.getTimeRange()));
        filter = FilterBase.addAndChild(filter, etrf);
      }
    }

    if (exprDepth > 0) {
      /* We are at a component level, event, todo etc.
       * If there are property filters turn this into an and of the current
       * filter with the or'd prop filters
       */
      filter = FilterBase.addAndChild(filter, processPropFilters(cf, eq, entityType));
    }

    if (!Util.isEmpty(cf.getCompFilters())) {
      FilterBase cfilters = null;
      for (CompFilter subcf: cf.getCompFilters()) {
        cfilters = FilterBase.addOrChild(cfilters, getQueryFilter(subcf,
                                                              eq, exprDepth + 1));
      }

      filter = FilterBase.addAndChild(filter, cfilters);
    }

    return filter;
  }

  private static TimeRange makeTimeRange(final UTCTimeRangeType utr) throws WebdavException {
    try {
      return new TimeRange(new DateTime(utr.getStart()),
                           new DateTime(utr.getEnd()));
    } catch (Throwable t) {
      throw new WebdavBadRequest(CaldavTags.validFilter, "Invalid time-range");
    }
  }

  private static FilterBase processPropFilters(final CompFilter cf,
                                    final EventQuery eq,
                                    final int entityType) throws WebdavException {
    if (Util.isEmpty(cf.getPropFilters())) {
      return null;
    }

    FilterBase pfilters = null;

    for (PropFilter pf: cf.getPropFilters()) {
      String pname = pf.getName();

      UTCTimeRangeType utr = pf.getTimeRange();
      TextMatch tm = pf.getTextMatch();
      boolean isNotDefined = pf.getIsNotDefined() != null;
      boolean testPresent = !isNotDefined && (utr == null) && (tm == null) &&
                            (Util.isEmpty(pf.getParamFilters()));
      TimeRange tr = null;
      if (utr != null) {
        tr = makeTimeRange(utr);
      }

      FilterBase filter = makeFilter(pname, isNotDefined, testPresent,
                                 tr,
                                 tm, pf.getParamFilters());

      if (filter != null) {
        pfilters = FilterBase.addAndChild(pfilters, filter);
      } else {
        eq.postFilter = true;

        // XXX This is wrong - if we postfilter we have to postfilter everything
        // XXX because it's an OR

        /** Add the propfilter to the post filter collection
         */
        if (entityType == IcalDefs.entityTypeEvent) {
          eq.eventFilters = addPropFilter(eq.eventFilters, pf);
        } else if (entityType == IcalDefs.entityTypeTodo) {
          eq.todoFilters = addPropFilter(eq.todoFilters, pf);
        } else if (entityType == IcalDefs.entityTypeJournal) {
          eq.journalFilters = addPropFilter(eq.journalFilters, pf);
        } else if (entityType == IcalDefs.entityTypeAlarm) {
          eq.alarmFilters = addPropFilter(eq.alarmFilters, pf);
        }
      }
    }

    return pfilters;
  }

  private static FilterBase makeFilter(final String pname,
                                       final boolean testNotDefined,
                                       final boolean testPresent,
                                       final TimeRange timeRange,
                                       final TextMatch match,
                            final Collection<ParamFilter> paramFilters) throws WebdavException {
    FilterBase filter = null;

    PropertyInfoIndex pi = PropertyInfoIndex.lookupPname(pname);

    if (pi == null) {
      // Unknown property
      throw new WebdavForbidden(CaldavTags.supportedFilter,
                                "Unknown property " + pname);
    }

    if (testNotDefined) {
      filter = new PresenceFilter(null, pi, false);
    } else if (testPresent) {
      // Presence check
      filter = new PresenceFilter(null, pi, true);
    } else if (timeRange != null) {
      filter = ObjectFilter.makeFilter(null, pi, timeRange);
    } else if (match != null) {
      ObjectFilter<String> f = new ObjectFilter<String>(null, pi);
      f.setEntity(match.getValue());
      f.setExact(false);

      boolean caseless = match.getCollation().equals("i;ascii-casemap");
      f.setCaseless(caseless);
      f.setNot(match.getNegateCondition().equals("yes"));
      filter = f;
    } else {
      // Must have param filters
      if (Util.isEmpty(paramFilters)) {
        throw new WebdavBadRequest();
      }
    }

    if (Util.isEmpty(paramFilters)) {
      return filter;
    }

    return FilterBase.addAndChild(filter, processParamFilters(pi,
                                                          paramFilters));
  }

  private static FilterBase processParamFilters(final PropertyInfoIndex parentIndex,
                                     final Collection<ParamFilter> paramFilters) throws WebdavException {
    FilterBase parfilters = null;

    for (ParamFilter pf: paramFilters) {
      TextMatch tm = pf.getTextMatch();
      boolean isNotDefined = pf.getIsNotDefined() != null;
      boolean testPresent = isNotDefined && (tm == null);

      PropertyFilter filter = (PropertyFilter)makeFilter(pf.getName(),
                                                         isNotDefined,
                                                         testPresent,
                                                         null, tm, null);

      if (filter != null) {
        filter.setParentPropertyIndex(parentIndex);
        parfilters = FilterBase.addOrChild(parfilters, filter);
      }
    }

    return parfilters;
  }

  private static List<PropFilter> addPropFilter(List<PropFilter> pfs,
                                         final PropFilter val) {
    if (pfs == null) {
      pfs = new ArrayList<PropFilter>();
    }

    pfs.add(val);

    return pfs;
  }
}

