package uk.ac.bbsrc.tgac.miso.core.data.impl;

import static uk.ac.bbsrc.tgac.miso.core.util.LimsUtils.nullifyStringIfBlank;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import uk.ac.bbsrc.tgac.miso.core.data.DetailedQcStatus;
import uk.ac.bbsrc.tgac.miso.core.data.DetailedSample;
import uk.ac.bbsrc.tgac.miso.core.data.SampleClass;
import uk.ac.bbsrc.tgac.miso.core.data.Subproject;

@Entity
@Table(name = "DetailedSample")
@Inheritance(strategy = InheritanceType.JOINED)
public class DetailedSampleImpl extends SampleImpl implements DetailedSample {

  private static final long serialVersionUID = 1L;

  @ManyToOne(optional = true, targetEntity = DetailedSampleImpl.class, fetch = FetchType.LAZY)
  @JoinColumn(name = "parentId", nullable = true)
  private DetailedSample parent;

  @OneToMany(targetEntity = DetailedSampleImpl.class, mappedBy = "parent")
  private Set<DetailedSample> children = new HashSet<>();

  @OneToOne(targetEntity = SampleClassImpl.class)
  @JoinColumn(name = "sampleClassId", nullable = false)
  private SampleClass sampleClass;

  @OneToOne(targetEntity = DetailedQcStatusImpl.class)
  @JoinColumn(name = "detailedQcStatusId")
  private DetailedQcStatus detailedQcStatus;
  private String detailedQcStatusNote;

  @OneToOne(targetEntity = SubprojectImpl.class)
  @JoinColumn(name = "subprojectId")
  private Subproject subproject;

  @Column(nullable = false)
  private Boolean archived = Boolean.FALSE;

  private Integer siblingNumber;

  private String groupId;
  private String groupDescription;
  private boolean isSynthetic = false;
  private Double concentration;

  @Column(nullable = false)
  private boolean nonStandardAlias = false;
  
  private Long preMigrationId;

  @Transient
  private Long identityId;

  @Override
  public DetailedSample getParent() {
    return parent;
  }

  @Override
  public void setParent(DetailedSample parent) {
    this.parent = parent;
  }

  @Override
  public Set<DetailedSample> getChildren() {
    return children;
  }

  @Override
  public void setChildren(Set<DetailedSample> children) {
    this.children = children;
  }

  @Override
  public SampleClass getSampleClass() {
    return sampleClass;
  }

  @Override
  public void setSampleClass(SampleClass sampleClass) {
    this.sampleClass = sampleClass;
  }

  @Override
  public Subproject getSubproject() {
    return subproject;
  }

  @Override
  public void setSubproject(Subproject subproject) {
    this.subproject = subproject;
  }

  @Override
  public Boolean getArchived() {
    return archived;
  }

  @Override
  public void setArchived(Boolean archived) {
    this.archived = archived;
  }

  @Override
  public DetailedQcStatus getDetailedQcStatus() {
    return detailedQcStatus;
  }

  @Override
  public void setDetailedQcStatus(DetailedQcStatus detailedQcStatus) {
    this.detailedQcStatus = detailedQcStatus;
  }

  @Override
  public Integer getSiblingNumber() {
    return siblingNumber;
  }

  @Override
  public void setSiblingNumber(Integer siblingNumber) {
    this.siblingNumber = siblingNumber;
  }

  @Override
  public String getGroupId() {
    return groupId;
  }

  @Override
  public void setGroupId(String groupId) {
    this.groupId = nullifyStringIfBlank(groupId);
  }

  @Override
  public String getGroupDescription() {
    return groupDescription;
  }

  @Override
  public void setGroupDescription(String groupDescription) {
    this.groupDescription = nullifyStringIfBlank(groupDescription);
  }

  @Override
  public Boolean isSynthetic() {
    return isSynthetic;
  }

  @Override
  public void setSynthetic(Boolean isSynthetic) {
    if (isSynthetic != null) {
      this.isSynthetic = isSynthetic;
    }
  }

  @Override
  public Double getConcentration() {
    return concentration;
  }

  @Override
  public void setConcentration(Double concentration) {
    this.concentration = concentration;
  }

  @Override
  public boolean hasNonStandardAlias() {
    return nonStandardAlias;
  }

  @Override
  public void setNonStandardAlias(boolean nonStandardAlias) {
    this.nonStandardAlias = nonStandardAlias;
  }

  @Override
  public String getDetailedQcStatusNote() {
    return detailedQcStatusNote;
  }

  @Override
  public void setDetailedQcStatusNote(String detailedQcStatusNote) {
    this.detailedQcStatusNote = detailedQcStatusNote;
  }
  
  @Override
  public Long getPreMigrationId() {
    return preMigrationId;
  }

  @Override
  public void setPreMigrationId(Long preMigrationId) {
    this.preMigrationId = preMigrationId;
  }

  @Override
  public Long getIdentityId() {
    return identityId;
  }

  @Override
  public void setIdentityId(Long identityId) {
    this.identityId = identityId;
  }

}
