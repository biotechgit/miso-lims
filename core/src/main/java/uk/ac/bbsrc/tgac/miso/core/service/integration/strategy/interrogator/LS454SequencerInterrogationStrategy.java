/*
 * Copyright (c) 2012. The Genome Analysis Centre, Norwich, UK
 * MISO project contacts: Robert Davey @ TGAC
 * *********************************************************************
 *
 * This file is part of MISO.
 *
 * MISO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MISO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MISO.  If not, see <http://www.gnu.org/licenses/>.
 *
 * *********************************************************************
 */

package uk.ac.bbsrc.tgac.miso.core.service.integration.strategy.interrogator;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.xml.XMLSerializer;
import net.sourceforge.fluxion.spi.ServiceProvider;

import uk.ac.bbsrc.tgac.miso.core.data.SequencerReference;
import uk.ac.bbsrc.tgac.miso.core.data.Status;
import uk.ac.bbsrc.tgac.miso.core.data.impl.StatusImpl;
import uk.ac.bbsrc.tgac.miso.core.data.type.HealthType;
import uk.ac.bbsrc.tgac.miso.core.data.type.PlatformType;
import uk.ac.bbsrc.tgac.miso.core.exception.InterrogationException;
import uk.ac.bbsrc.tgac.miso.core.service.integration.contract.InterrogationResult;
import uk.ac.bbsrc.tgac.miso.core.service.integration.contract.impl.MisoPerlDaemonQuery;
import uk.ac.bbsrc.tgac.miso.core.service.integration.mechanism.InterrogationMechanism;
import uk.ac.bbsrc.tgac.miso.core.service.integration.mechanism.interrogator.MisoPerlDaemonInterrogationMechanism;
import uk.ac.bbsrc.tgac.miso.core.service.integration.strategy.SequencerInterrogationStrategy;
import uk.ac.bbsrc.tgac.miso.core.util.SubmissionUtils;
import uk.ac.bbsrc.tgac.miso.core.util.UnicodeReader;

/**
 * A concrete implementation of a SequencerInterrogationStrategy that can make queries and parse results, supported by a
 * MisoPerlDaemonInterrogationMechanism, to a 454 sequencer.
 * <p/>
 * Methods in this class are not usually called explicitly, but via a {@link SequencerInterrogator} that has wrapped up this strategy to a
 * SequencerReference.
 * 
 * @author Rob Davey
 * @since 0.0.2
 */
@ServiceProvider
public class LS454SequencerInterrogationStrategy implements SequencerInterrogationStrategy {
  private static final Logger log = LoggerFactory.getLogger(LS454SequencerInterrogationStrategy.class);

  private static final MisoPerlDaemonQuery statusQuery = new MisoPerlDaemonQuery("454", "status");
  private static final MisoPerlDaemonQuery completeRunsQuery = new MisoPerlDaemonQuery("454", "complete");
  private static final MisoPerlDaemonQuery incompleteRunsQuery = new MisoPerlDaemonQuery("454", "running");

  @Override
  public boolean isStrategyFor(SequencerReference sr) {
    return (sr.getPlatform().getPlatformType().equals(PlatformType.LS454));
  }

  @Override
  public List<Status> listAllStatus(SequencerReference sr) throws InterrogationException {
    List<Status> s = new ArrayList<>();
    JSONObject response = JSONObject.fromObject(doQuery(sr, new MisoPerlDaemonInterrogationMechanism(), statusQuery).parseResult());
    if (response != null && response.has("response")) {
      JSONArray a = response.getJSONArray("response");
      for (JSONObject j : (Iterable<JSONObject>) a) {
        if (j.has("file") && j.has("xml")) {
          try {
            StatusImpl status = new StatusImpl();
            if (j.has("complete") && j.getString("complete").equals("true")) {
              status.setHealth(HealthType.Completed);
            } else {
              status.setHealth(HealthType.Running);
            }

            Document statusDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            SubmissionUtils.transform(new UnicodeReader(j.getString("xml")), statusDoc);
            String runStarted = statusDoc.getElementsByTagName("date").item(0).getTextContent();
            status.setStartDate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(runStarted));
            status.setInstrumentName(statusDoc.getElementsByTagName("serialNumber").item(0).getTextContent());
            Node n = statusDoc.getElementsByTagName("run").item(0);
            for (int i = 0; i < n.getChildNodes().getLength(); i++) {
              Node child = n.getChildNodes().item(i);
              if (child instanceof Element && ((Element) child).getTagName().equals("id")) {
                status.setRunAlias(child.getTextContent());
              }
            }
            s.add(status);
          } catch (ParserConfigurationException e) {
            log.error("list all statuses", e);
            throw new InterrogationException(e.getMessage());
          } catch (ParseException e) {
            log.error("list all statuses", e);
            throw new InterrogationException(e.getMessage());
          } catch (TransformerException e) {
            log.error("list all statuses", e);
            throw new InterrogationException(e.getMessage());
          }
        }
      }
    }
    return s;
  }

  @Override
  public List<Status> listAllStatusBySequencerName(SequencerReference sr, String name) throws InterrogationException {
    return null;
  }

  @Override
  public List<String> listRunsByHealthType(SequencerReference sr, HealthType healthType) throws InterrogationException {
    String response = doQuery(sr, new MisoPerlDaemonInterrogationMechanism(),
        new MisoPerlDaemonQuery("454", healthType.getKey().toLowerCase())).parseResult();
    List<String> s = new ArrayList<>();
    if (response != null) {
      String[] ss = response.split(",");
      for (String sss : ss) {
        String regex = ".*/([\\d]+_[A-z0-9]+_[\\d]+_[A-z0-9_]*)/.*";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(sss);
        if (m.matches()) {
          s.add(m.group(1));
        }
      }
    }
    Collections.sort(s);
    return s;
  }

  @Override
  public List<String> listAllCompleteRuns(SequencerReference sr) throws InterrogationException {
    String response = doQuery(sr, new MisoPerlDaemonInterrogationMechanism(), completeRunsQuery).parseResult();
    List<String> s = new ArrayList<>();
    if (response != null) {
      String[] ss = response.split(",");
      for (String sss : ss) {
        String regex = ".*\\/(R_\\d{4}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_[A-z0-9]+_[A-z0-9]+_[A-z0-9_\\-]+)\\/.*";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(sss);
        if (m.matches()) {
          s.add(m.group(1));
        }
      }
    }
    Collections.sort(s);
    return s;
  }

  @Override
  public List<String> listAllIncompleteRuns(SequencerReference sr) throws InterrogationException {
    String response = doQuery(sr, new MisoPerlDaemonInterrogationMechanism(), incompleteRunsQuery).parseResult();
    List<String> s = new ArrayList<>();
    if (response != null) {
      String[] ss = response.split(",");
      for (String sss : ss) {
        String regex = ".*\\/(R_\\d{4}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_[A-z0-9]+_[A-z0-9]+_[A-z0-9_\\-]+)\\/.*";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(sss);
        if (m.matches()) {
          s.add(m.group(1));
        }
      }
    }
    Collections.sort(s);
    return s;
  }

  @Override
  public Status getRunStatus(SequencerReference sr, String runName) throws InterrogationException {
    MisoPerlDaemonQuery runStatusQuery = new MisoPerlDaemonQuery("454", runName, "status");

    try {
      JSONObject response = JSONObject.fromObject(doQuery(sr, new MisoPerlDaemonInterrogationMechanism(), runStatusQuery).parseResult());
      if (response != null && response.has("response")) {
        JSONArray a = response.getJSONArray("response");
        if (a.iterator().hasNext()) {
          JSONObject j = (JSONObject) a.iterator().next();
          if (j.has("file") && j.has("xml")) {
            StatusImpl status = new StatusImpl();
            if (j.has("complete") && j.getString("complete").equals("true")) {
              status.setHealth(HealthType.Completed);
            } else {
              status.setHealth(HealthType.Running);
            }
            Document statusDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            SubmissionUtils.transform(new UnicodeReader(j.getString("xml")), statusDoc);
            String runStarted = statusDoc.getElementsByTagName("date").item(0).getTextContent();
            status.setStartDate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(runStarted));
            status.setInstrumentName(statusDoc.getElementsByTagName("serialNumber").item(0).getTextContent());
            Node n = statusDoc.getElementsByTagName("run").item(0);
            for (int i = 0; i < n.getChildNodes().getLength(); i++) {
              Node child = n.getChildNodes().item(i);
              if (child instanceof Element && ((Element) child).getTagName().equals("id")) {
                status.setRunAlias(child.getTextContent());
              }
            }
            return status;
          }
        }
      }
    } catch (ParserConfigurationException e) {
      log.error("get run status", e);
      throw new InterrogationException(e.getMessage());
    } catch (ParseException e) {
      log.error("get run status", e);
      throw new InterrogationException(e.getMessage());
    } catch (TransformerException e) {
      log.error("get run status", e);
      throw new InterrogationException(e.getMessage());
    }
    return null;
  }

  @Override
  public JSONObject getRunInformation(SequencerReference sr, String runName) throws InterrogationException {
    MisoPerlDaemonQuery runInfoQuery = new MisoPerlDaemonQuery("454", runName, "status");

    JSONObject json = new JSONObject();
    try {
      JSONObject response = JSONObject.fromObject(doQuery(sr, new MisoPerlDaemonInterrogationMechanism(), runInfoQuery).parseResult());
      if (response != null && response.has("response")) {
        JSONArray a = response.getJSONArray("response");
        if (a.iterator().hasNext()) {
          JSONObject j = (JSONObject) a.iterator().next();
          if (j.has("file") && j.has("xml")) {
            XMLSerializer xmls = new XMLSerializer();
            JSON info = xmls.read(j.getString("xml"));
            if (!info.isEmpty()) {
              if (info.isArray()) {
                json.put("response", info);
              } else {
                return (JSONObject) info;
              }
            }
          }
        }
      }
    } catch (InterrogationException ie) {
      log.error("get run information", ie);
    }
    return json;
  }

  private InterrogationResult<String> doQuery(SequencerReference sr, InterrogationMechanism mechanism, MisoPerlDaemonQuery query)
      throws InterrogationException {
    log.info("Pushing query: " + query.generateQuery());
    InterrogationResult<String> result = mechanism.doQuery(sr, query);
    log.info("Consuming result: " + result.parseResult());
    return result;
  }
}
