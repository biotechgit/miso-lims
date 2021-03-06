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

package uk.ac.bbsrc.tgac.miso.core.data.impl.pacbio;

import uk.ac.bbsrc.tgac.miso.core.data.impl.StatusImpl;
import uk.ac.bbsrc.tgac.miso.core.data.type.HealthType;

/**
 * uk.ac.bbsrc.tgac.miso.core.data.impl.pacbio
 * <p/>
 * 
 * @author Rob Davey
 * @since 0.1.6
 */
public class PacBioStatus extends StatusImpl {

  private static final long serialVersionUID = 1L;
  String metadata = null;

  public PacBioStatus() {
    setHealth(HealthType.Unknown);
  }

  public PacBioStatus(String metadata) {
    this.metadata = metadata;
  }

  public void parseMetaData(String metadata) {
  }

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(super.toString());
    if (metadata != null) {
      sb.append(" : ");
      sb.append(metadata);
    }
    return sb.toString();
  }
}
