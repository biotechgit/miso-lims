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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MISO. If not, see <http://www.gnu.org/licenses/>.
 *
 * *********************************************************************
 */

package uk.ac.bbsrc.tgac.miso.spring.ajax;

import static uk.ac.bbsrc.tgac.miso.core.util.LimsUtils.isStringEmptyOrNull;
import static uk.ac.bbsrc.tgac.miso.spring.ControllerHelperServiceUtils.getBarcodeFileLocation;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.binary.Base64;
import org.krysalis.barcode4j.BarcodeDimension;
import org.krysalis.barcode4j.BarcodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

import com.eaglegenomics.simlims.core.Note;
import com.eaglegenomics.simlims.core.User;
import com.eaglegenomics.simlims.core.manager.SecurityManager;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sourceforge.fluxion.ajax.Ajaxified;
import net.sourceforge.fluxion.ajax.util.JSONUtils;

import uk.ac.bbsrc.tgac.miso.core.data.Boxable;
import uk.ac.bbsrc.tgac.miso.core.data.Project;
import uk.ac.bbsrc.tgac.miso.core.data.Sample;
import uk.ac.bbsrc.tgac.miso.core.data.SampleQC;
import uk.ac.bbsrc.tgac.miso.core.data.impl.ProjectOverview;
import uk.ac.bbsrc.tgac.miso.core.data.impl.SampleQCImpl;
import uk.ac.bbsrc.tgac.miso.core.data.type.QcType;
import uk.ac.bbsrc.tgac.miso.core.factory.barcode.BarcodeFactory;
import uk.ac.bbsrc.tgac.miso.core.manager.MisoFilesManager;
import uk.ac.bbsrc.tgac.miso.core.manager.RequestManager;
import uk.ac.bbsrc.tgac.miso.core.service.naming.NamingScheme;
import uk.ac.bbsrc.tgac.miso.core.service.naming.validation.ValidationResult;
import uk.ac.bbsrc.tgac.miso.core.util.LimsUtils;
import uk.ac.bbsrc.tgac.miso.core.util.TaxonomyUtils;
import uk.ac.bbsrc.tgac.miso.service.PrinterService;
import uk.ac.bbsrc.tgac.miso.service.SampleService;
import uk.ac.bbsrc.tgac.miso.spring.ControllerHelperServiceUtils;
import uk.ac.bbsrc.tgac.miso.spring.ControllerHelperServiceUtils.BarcodePrintAssister;

/**
 * uk.ac.bbsrc.tgac.miso.spring.ajax
 * <p/>
 * Info
 *
 * @author Rob Davey
 * @since 0.0.2
 */
@Ajaxified
public class SampleControllerHelperService {
  public static final class SampleBarcodeAssister implements BarcodePrintAssister<Sample> {
    private final RequestManager requestManager;
    private final SampleService sampleService;

    public SampleBarcodeAssister(RequestManager requestManager, SampleService sampleService) {
      this.requestManager = requestManager;
      this.sampleService = sampleService;
    }

    @Override
    public Iterable<Sample> fetchAll(long projectId) throws IOException {
      return requestManager.listAllSamplesByProjectId(projectId);
    }

    @Override
    public Sample fetch(long id) throws IOException {
      return sampleService.get(id);
    }

    @Override
    public void store(Sample item) throws IOException {
      sampleService.update(item);
    }

    @Override
    public String getGroupName() {
      return "samples";
    }

    @Override
    public String getIdName() {
      return "sampleId";
    }
  }

  protected static final Logger log = LoggerFactory.getLogger(SampleControllerHelperService.class);
  @Autowired
  private SecurityManager securityManager;
  @Autowired
  private RequestManager requestManager;
  @Autowired
  private SampleService sampleService;
  @Autowired
  private MisoFilesManager misoFileManager;
  @Autowired
  private PrinterService printerService;
  @Autowired
  private NamingScheme namingScheme;

  public JSONObject validateSampleAlias(HttpSession session, JSONObject json) {
    try {
      if (isStringEmptyOrNull(json.getString("alias")) && namingScheme.hasSampleAliasGenerator()) {
        // alias will be generated by DAO during save
        return JSONUtils.SimpleJSONResponse("OK");
      } else if (!json.has("alias")) {
        return JSONUtils.SimpleJSONError("No alias specified");
      } else {
        String alias = json.getString("alias");
        ValidationResult aliasValidation = namingScheme.validateSampleAlias(alias);
        if (aliasValidation.isValid()) {
          log.info("Sample alias OK!");
          return JSONUtils.SimpleJSONResponse("OK");
        } else {
          log.error("Sample alias not valid: " + alias);
          return JSONUtils.SimpleJSONError(aliasValidation.getMessage());
        }
      }
    } catch (Exception e) {
      log.error("Exception in validateSampleAlias", e);
      throw e;
    }
  }

  public JSONObject getSampleQCUsers(HttpSession session, JSONObject json) {
    try {
      Collection<String> users = new HashSet<>();
      User user = securityManager.getUserByLoginName(SecurityContextHolder.getContext().getAuthentication().getName());
      users.add(user.getFullName());

      if (json.has("sampleId") && !isStringEmptyOrNull(json.getString("sampleId"))) {
        Long sampleId = Long.parseLong(json.getString("sampleId"));
        Sample sample = sampleService.get(sampleId);

        Project p = sample.getProject();
        if (p.userCanRead(user)) {
          for (ProjectOverview po : p.getOverviews()) {
            if (po.getSampleGroup() != null) {
              if (po.getSamples().contains(sample)) {
                users.add(po.getPrincipalInvestigator());
              }
            }
          }
        }
      }
      StringBuilder sb = new StringBuilder();
      for (String name : users) {
        sb.append("<option value='" + name + "'>" + name + "</option>");
      }
      Map<String, Object> map = new HashMap<>();
      map.put("qcUserOptions", sb.toString());
      map.put("sampleId", json.getString("sampleId"));
      return JSONUtils.JSONObjectResponse(map);
    } catch (IOException e) {
      log.error("Failed to get available users for this Sample QC: ", e);
      return JSONUtils.SimpleJSONError("Failed to get available users for this Sample QC: " + e.getMessage());
    }
  }

  public JSONObject getSampleQcTypes(HttpSession session, JSONObject json) {
    try {
      StringBuilder sb = new StringBuilder();
      Collection<QcType> types = sampleService.listSampleQcTypes();
      for (QcType s : types) {
        sb.append("<option units='" + s.getUnits() + "' value='" + s.getQcTypeId() + "'>" + s.getName() + "</option>");
      }
      Map<String, Object> map = new HashMap<>();
      map.put("types", sb.toString());
      return JSONUtils.JSONObjectResponse(map);
    } catch (IOException e) {
      log.error("get sample qc type", e);
    }
    return JSONUtils.SimpleJSONError("Cannot list all Sample QC Types");
  }

  public JSONObject addSampleQC(HttpSession session, JSONObject json) {
    try {
      for (Object k : json.keySet()) {
        String key = (String) k;
        if (json.get(key) == null || isStringEmptyOrNull(json.getString(key))) {
          return JSONUtils.SimpleJSONError("Please enter a value for '" + key + "'");
        }
      }
      if (json.has("sampleId") && !isStringEmptyOrNull(json.getString("sampleId"))) {
        Long sampleId = Long.parseLong(json.getString("sampleId"));
        Sample sample = sampleService.get(sampleId);
        if (json.get("qcPassed") != null) {
          if ("true".equals(json.getString("qcPassed"))) {
            sample.setQcPassed(true);
          } else if ("false".equals(json.getString("qcPassed"))) {
            sample.setQcPassed(false);
          }
        }

        SampleQC newQc = new SampleQCImpl();
        newQc.setQcCreator(json.getString("qcCreator"));
        newQc.setQcDate(new SimpleDateFormat("dd/MM/yyyy").parse(json.getString("qcDate")));
        newQc.setQcType(requestManager.getSampleQcTypeById(json.getLong("qcType")));
        newQc.setResults(Double.parseDouble(json.getString("results")));
        sample.addQc(newQc);
        requestManager.saveSampleQC(newQc);

        StringBuilder sb = new StringBuilder();
        sb.append("<tr><th>QCed By</th><th>QC Date</th><th>Method</th><th>Results</th></tr>");
        for (SampleQC qc : sample.getSampleQCs()) {
          sb.append("<tr>");
          sb.append("<td>" + qc.getQcCreator() + "</td>");
          sb.append("<td>" + qc.getQcDate() + "</td>");
          sb.append("<td>" + qc.getQcType().getName() + "</td>");
          sb.append("<td>" + LimsUtils.round(qc.getResults(), 2) + " " + qc.getQcType().getUnits() + "</td>");
          sb.append("</tr>");
        }
        return JSONUtils.SimpleJSONResponse(sb.toString());
      }
    } catch (Exception e) {
      log.error("Failed to add Sample QC to this sample: ", e);
      return JSONUtils.SimpleJSONError("Failed to add Sample QC to this sample: " + e.getMessage());
    }
    return JSONUtils.SimpleJSONError("Cannot add SampleQC");
  }

  public JSONObject changeSampleQCRow(HttpSession session, JSONObject json) {
    try {
      JSONObject response = new JSONObject();
      Long qcId = Long.parseLong(json.getString("qcId"));
      SampleQC sampleQc = requestManager.getSampleQCById(qcId);
      response.put("results", "<input type='text' id='" + qcId + "' value='" + sampleQc.getResults() + "'/>");
      response.put("edit", "<a href='javascript:void(0);' onclick='Sample.qc.editSampleQC(\"" + qcId + "\");'>Save</a>");
      return response;
    } catch (Exception e) {
      log.error("Failed to display Sample QC of this sample: ", e);
      return JSONUtils.SimpleJSONError("Failed to display Sample QC of this sample: " + e.getMessage());
    }
  }

  public JSONObject editSampleQC(HttpSession session, JSONObject json) {
    try {
      if (json.has("qcId") && !isStringEmptyOrNull(json.getString("qcId"))) {
        Long qcId = Long.parseLong(json.getString("qcId"));
        SampleQC sampleQc = requestManager.getSampleQCById(qcId);
        sampleQc.setResults(Double.parseDouble(json.getString("result")));
        requestManager.saveSampleQC(sampleQc);
        return JSONUtils.SimpleJSONResponse("OK");
      }
    } catch (Exception e) {
      log.error("Failed to add Sample QC to this sample: ", e);
      return JSONUtils.SimpleJSONError("Failed to add Sample QC to this sample: " + e.getMessage());
    }
    return JSONUtils.SimpleJSONError("Cannot add SampleQC");
  }

  public JSONObject bulkAddSampleQCs(HttpSession session, JSONObject json) {
    try {
      JSONArray qcs = JSONArray.fromObject(json.getString("qcs"));
      // validate
      boolean ok = true;
      for (JSONObject qc : (Iterable<JSONObject>) qcs) {
        String qcType = qc.getString("qcType");
        String results = qc.getString("results");
        String qcCreator = qc.getString("qcCreator");
        String qcDate = qc.getString("qcDate");

        if (isStringEmptyOrNull(qcType) || isStringEmptyOrNull(results) || isStringEmptyOrNull(qcCreator) || isStringEmptyOrNull(qcDate)) {
          ok = false;
        }
      }

      // persist
      if (ok) {
        Map<String, Object> map = new HashMap<>();
        JSONArray a = new JSONArray();
        JSONArray errors = new JSONArray();
        for (JSONObject qc : (Iterable<JSONObject>) qcs) {
          JSONObject j = addSampleQC(session, qc);
          j.put("sampleId", qc.getString("sampleId"));
          if (j.has("error")) {
            errors.add(j);
          } else {
            a.add(j);
          }
        }
        map.put("saved", a);
        if (!errors.isEmpty()) {
          map.put("errors", errors);
        }
        return JSONUtils.JSONObjectResponse(map);
      } else {
        log.error("Failed to add Sample QC to this Library: one of the required fields of the selected QCs is missing or invalid");
        return JSONUtils.SimpleJSONError(
            "Failed to add Sample QC to this Library: one of the required fields of the selected QCs is missing or invalid");
      }
    } catch (Exception e) {
      log.error("Failed to add Sample QC to this sample: ", e);
      return JSONUtils.SimpleJSONError("Failed to add Sample QC to this sample: " + e.getMessage());
    }
  }

  public JSONObject addSampleNote(HttpSession session, JSONObject json) {
    Long sampleId = json.getLong("sampleId");
    String internalOnly = json.getString("internalOnly");
    String text = json.getString("text");

    try {
      Sample sample = sampleService.get(sampleId);
      Note note = new Note();
      internalOnly = internalOnly.equals("on") ? "true" : "false";
      note.setInternalOnly(Boolean.parseBoolean(internalOnly));
      note.setText(text);
      sampleService.addNote(sample, note);
    } catch (IOException e) {
      log.error("add sample note", e);
      return JSONUtils.SimpleJSONError(e.getMessage());
    }

    return JSONUtils.SimpleJSONResponse("Note saved successfully");
  }

  public JSONObject deleteSampleNote(HttpSession session, JSONObject json) {
    Long sampleId = json.getLong("sampleId");
    Long noteId = json.getLong("noteId");

    try {
      Sample sample = sampleService.get(sampleId);
      sampleService.deleteNote(sample, noteId);
      return JSONUtils.SimpleJSONResponse("OK");
    } catch (IOException e) {
      log.error("cannot remove note", e);
      return JSONUtils.SimpleJSONError("Cannot remove note: " + e.getMessage());
    }
  }

  public JSONObject getSampleByBarcode(HttpSession session, JSONObject json) {
    JSONObject response = new JSONObject();
    String barcode = json.getString("barcode");

    try {
      Sample sample = sampleService.getByBarcode(barcode);
      // Base64-encoded string, most likely a barcode image beeped in. decode and search
      if (sample == null) {
        sample = sampleService.getByBarcode(new String(Base64.decodeBase64(barcode)));
      }
      if (sample.getReceivedDate() == null) {
        response.put("name", sample.getName());
        response.put("desc", sample.getDescription());
        response.put("id", sample.getId());
        response.put("type", sample.getSampleType());
        response.put("project", sample.getProject().getName());
        return response;
      } else {
        return JSONUtils.SimpleJSONError("Sample " + sample.getName() + " has already been received");
      }
    } catch (Exception e) {
      log.error("sample not in database", e);
      return JSONUtils.SimpleJSONError(e.getMessage() + ": This sample doesn't seem to be in the database.");
    }
  }

  public JSONObject setSampleReceivedDateByBarcode(HttpSession session, JSONObject json) {
    JSONObject response = new JSONObject();
    try {
      User user = securityManager.getUserByLoginName(SecurityContextHolder.getContext().getAuthentication().getName());
      JSONArray sampleIds = JSONArray.fromObject(json.getString("samples"));
      for (int index = 0; index < sampleIds.size(); index++) {
        Sample sample = sampleService.get(sampleIds.getLong(index));
        sample.setReceivedDate(new Date());
        sample.setLastModifier(user);
        sampleService.update(sample);
      }
      response.put("result", "Samples received date saved");
      return response;
    } catch (IOException e) {
      log.error("cannot set receipt date for sample", e);
      return JSONUtils.SimpleJSONError(e.getMessage() + ": Cannot set receipt date for sample");
    }
  }

  public JSONObject getSampleBarcode(HttpSession session, JSONObject json) {
    Long sampleId = json.getLong("sampleId");
    File temploc = getBarcodeFileLocation(session);
    try {
      Sample sample = sampleService.get(sampleId);
      BarcodeFactory barcodeFactory = new BarcodeFactory();
      barcodeFactory.setPointPixels(1.5f);
      barcodeFactory.setBitmapResolution(600);
      RenderedImage bi = null;

      if (json.has("barcodeGenerator")) {
        BarcodeDimension dim = new BarcodeDimension(100, 100);
        if (json.has("dimensionWidth") && json.has("dimensionHeight")) {
          dim = new BarcodeDimension(json.getDouble("dimensionWidth"), json.getDouble("dimensionHeight"));
        }
        BarcodeGenerator bg = BarcodeFactory.lookupGenerator(json.getString("barcodeGenerator"));
        if (bg != null) {
          bi = barcodeFactory.generateBarcode(sample, bg, dim);
        } else {
          return JSONUtils.SimpleJSONError("'" + json.getString("barcodeGenerator") + "' is not a valid barcode generator type");
        }
      } else {
        bi = barcodeFactory.generateSquareDataMatrix(sample, 400);
      }

      if (bi != null) {
        File tempimage = misoFileManager.generateTemporaryFile("barcode-", ".png", temploc);
        if (ImageIO.write(bi, "png", tempimage)) {
          return JSONUtils.JSONObjectResponse("img", tempimage.getName());
        }
        return JSONUtils.SimpleJSONError("Writing temp image file failed.");
      } else {
        return JSONUtils.SimpleJSONError("Sample has no parseable barcode");
      }
    } catch (IOException e) {
      log.error("cannot access: " + temploc.getAbsolutePath(), e);
      return JSONUtils.SimpleJSONError(e.getMessage() + ": Cannot seem to access " + temploc.getAbsolutePath());
    }
  }

  public JSONObject printSampleBarcodes(HttpSession session, JSONObject json) {
    return ControllerHelperServiceUtils.printBarcodes(printerService, json, new SampleBarcodeAssister(requestManager, sampleService));
  }

  public JSONObject changeSampleLocation(HttpSession session, JSONObject json) {
    Long sampleId = json.getLong("sampleId");
    String locationBarcode = json.getString("locationBarcode");

    try {
      String newLocation = LimsUtils.lookupLocation(locationBarcode);
      if (newLocation != null) {
        User user = securityManager.getUserByLoginName(SecurityContextHolder.getContext().getAuthentication().getName());
        Sample sample = sampleService.get(sampleId);
        String oldLocation = sample.getLocationBarcode();
        sample.setLocationBarcode(newLocation);

        Note note = new Note();
        note.setInternalOnly(true);
        note.setText("Location changed from " + oldLocation + " to " + newLocation + " by " + user.getLoginName() + " on " + new Date());
        sampleService.addNote(sample, note);
        sampleService.update(sample);
      } else {
        return JSONUtils.SimpleJSONError("New location barcode not recognised");
      }
    } catch (IOException e) {
      log.error("change sample location", e);
      return JSONUtils.SimpleJSONError(e.getMessage());
    }

    return JSONUtils.SimpleJSONResponse("Note saved successfully");
  }

  public JSONObject changeSampleIdBarcode(HttpSession session, JSONObject json) {
    Long sampleId = json.getLong("sampleId");
    String idBarcode = json.getString("identificationBarcode");

    try {
      if (isStringEmptyOrNull(idBarcode)) {
        // if the user accidentally deletes a barcode, the changelogs will have a record of the original barcode
        idBarcode = null;
      } else {
        List<Boxable> previouslyBarcodedItems = new ArrayList<>(requestManager.getBoxablesFromBarcodeList(Arrays.asList(idBarcode)));
        if (!previouslyBarcodedItems.isEmpty()
            && !(previouslyBarcodedItems.size() == 1 && previouslyBarcodedItems.get(0).getId() == sampleId)) {
          Boxable previouslyBarcodedItem = previouslyBarcodedItems.get(0);
          String error = String.format(
              "Could not change sample identification barcode to '%s'. This barcode is already in use by an item with the name '%s' and the alias '%s'.",
              idBarcode, previouslyBarcodedItem.getName(), previouslyBarcodedItem.getAlias());
          log.debug(error);
          return JSONUtils.SimpleJSONError(error);
        }
      }
      User user = securityManager.getUserByLoginName(SecurityContextHolder.getContext().getAuthentication().getName());
      Sample sample = sampleService.get(sampleId);
      sample.setIdentificationBarcode(idBarcode);
      sample.setLastModifier(user);
      sampleService.update(sample);
    } catch (IOException e) {
      log.debug("Could not change Sample identificationBarcode: " + e.getMessage());
      return JSONUtils.SimpleJSONError(e.getMessage());
    }

    return JSONUtils.SimpleJSONResponse("New Identification Barcode successfully assigned.");
  }

  public JSONObject lookupNCBIScientificName(HttpSession session, JSONObject json) {
    String taxon = TaxonomyUtils.checkScientificNameAtNCBI(json.getString("scientificName"));
    if (taxon != null) {
      return JSONUtils.SimpleJSONResponse("NCBI taxon is valid");
    } else {
      return JSONUtils.SimpleJSONError(
          "This scientific name is not of a known taxonomy. You may have problems when trying to submit this data to public repositories.");
    }
  }

  public JSONObject deleteSample(HttpSession session, JSONObject json) {
    User user;
    try {
      user = securityManager.getUserByLoginName(SecurityContextHolder.getContext().getAuthentication().getName());
    } catch (IOException e) {
      log.error("delete sample", e);
      return JSONUtils.SimpleJSONError("Error getting currently logged in user.");
    }

    if (user != null && user.isAdmin()) {
      if (json.has("sampleId")) {
        Long sampleId = json.getLong("sampleId");
        try {
          sampleService.delete(sampleId);
          return JSONUtils.SimpleJSONResponse("Sample deleted");
        } catch (IOException e) {
          log.error("delete sample", e);
          return JSONUtils.SimpleJSONError("Cannot delete sample: " + e.getMessage());
        }
      } else {
        return JSONUtils.SimpleJSONError("No sample specified to delete.");
      }
    } else {
      return JSONUtils.SimpleJSONError("Only logged-in admins can delete objects.");
    }
  }

  public JSONObject removeSampleFromOverview(HttpSession session, JSONObject json) {
    User user;
    try {
      user = securityManager.getUserByLoginName(SecurityContextHolder.getContext().getAuthentication().getName());
    } catch (IOException e) {
      log.error("remove sample from group", e);
      return JSONUtils.SimpleJSONError("Error getting currently logged in user.");
    }

    if (user != null) {
      if (json.has("sampleId") && json.has("overviewId")) {
        Long sampleId = json.getLong("sampleId");
        Long overviewId = json.getLong("overviewId");
        try {
          ProjectOverview overview = requestManager.getProjectOverviewById(overviewId);
          Sample s = sampleService.get(sampleId);
          if (overview.getSamples().contains(s)) {
            if (overview.getSamples().remove(s)) {
              requestManager.saveProjectOverview(overview);

              return JSONUtils.SimpleJSONResponse("Sample removed from group");
            } else {
              return JSONUtils.SimpleJSONError("Error removing sample from sample group.");
            }
          } else {
            return JSONUtils.SimpleJSONResponse("Sample not in this sample group!");
          }
        } catch (IOException e) {
          log.error("remove sample from group", e);
          return JSONUtils.SimpleJSONError("Cannot remove sample from group: " + e.getMessage());
        }
      } else {
        return JSONUtils.SimpleJSONError("No sample or sample group specified to remove.");
      }
    } else {
      return JSONUtils.SimpleJSONError("Only logged-in users can remove objects.");
    }
  }

  public JSONObject getSampleLastQCRequest(HttpSession session, JSONObject json) {
    Long sampleId = json.getLong("sampleId");
    return JSONUtils.SimpleJSONResponse(getSampleLastQC(sampleId));
  }

  public String getSampleLastQC(Long sampleId) {
    try {
      String sampleQCValue = "NA";
      Collection<SampleQC> sampleQCs = requestManager.listAllSampleQCsBySampleId(sampleId);
      if (sampleQCs.size() > 0) {
        List<SampleQC> list = new ArrayList<>(sampleQCs);
        Collections.sort(list, new Comparator<SampleQC>() {
          @Override
          public int compare(SampleQC sqc1, SampleQC sqc2) {
            return (int) sqc1.getId() - (int) sqc2.getId();
          }
        });
        SampleQC sampleQC = list.get(list.size() - 1);
        sampleQCValue = sampleQC.getResults().toString();
      }
      return sampleQCValue;
    } catch (IOException e) {
      log.debug("Failed", e);
      return "Failed: " + e.getMessage();
    }
  }

  public void setSecurityManager(SecurityManager securityManager) {
    this.securityManager = securityManager;
  }

  public void setRequestManager(RequestManager requestManager) {
    this.requestManager = requestManager;
  }

  public void setMisoFileManager(MisoFilesManager misoFileManager) {
    this.misoFileManager = misoFileManager;
  }

  public void setPrinterService(PrinterService printerService) {
    this.printerService = printerService;
  }

  public void setSampleNamingScheme(NamingScheme namingScheme) {
    this.namingScheme = namingScheme;
  }
}
