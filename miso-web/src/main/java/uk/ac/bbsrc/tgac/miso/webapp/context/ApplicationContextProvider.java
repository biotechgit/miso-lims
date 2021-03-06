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

package uk.ac.bbsrc.tgac.miso.webapp.context;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * uk.ac.bbsrc.tgac.miso.webapp.context
 * <p/>
 * Info
 * 
 * @author Rob Davey
 * @date 05-Aug-2011
 * @since 0.0.3
 */

public class ApplicationContextProvider implements ApplicationContextAware {
  private static ApplicationContext ctx = null;

  private static String bugUrl;

  public static ApplicationContext getApplicationContext() {
    return ctx;
  }

  public static String getBugUrl() {
    return bugUrl;
  }

  private String baseUrl = "";

  public String getBaseUrl() {
    return baseUrl;
  }

  @Override
  public void setApplicationContext(ApplicationContext ctx) throws BeansException {
    ApplicationContextProvider.ctx = ctx;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  @Value("${miso.bugUrl}")
  private void setBugUrl(String bugUrl) {
    // This instance method writes to a static field because that's how Spring's injection system works.
    ApplicationContextProvider.bugUrl = bugUrl;
  }
}
