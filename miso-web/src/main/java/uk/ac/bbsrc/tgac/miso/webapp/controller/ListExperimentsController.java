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

package uk.ac.bbsrc.tgac.miso.webapp.controller;

import java.io.IOException;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import uk.ac.bbsrc.tgac.miso.core.data.Experiment;
import uk.ac.bbsrc.tgac.miso.service.ExperimentService;

/**
 * com.eaglegenomics.miso.web
 * <p/>
 * TODO Info
 * 
 * @author Rob Davey
 * @since 0.0.2
 */
@Controller
public class ListExperimentsController {
  protected static final Logger log = LoggerFactory.getLogger(ListExperimentsController.class);

  @Autowired
  private ExperimentService experimentService;

  @ModelAttribute("title")
  public String title() {
    return "Experiments";
  }

  @RequestMapping(value = "/experiments/rest/", method = RequestMethod.GET)
  public @ResponseBody Collection<Experiment> jsonRest() throws IOException {
    return experimentService.listAll();
  }

  @RequestMapping("/experiments")
  public ModelAndView listExperiments() throws Exception {
    return new ModelAndView("/pages/listExperiments.jsp");
  }
}