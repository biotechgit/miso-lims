CREATE INDEX Alert_userId ON Alert(userId);
CREATE INDEX BoxChangeLog_boxId_changeTime ON BoxChangeLog(BoxId, changeTime);
CREATE INDEX ExperimentChangeLog_experimentId_changeTime ON ExperimentChangeLog(experimentId, changeTime);
CREATE INDEX KitDescriptorChangeLog_kitDescriptorId_changeTime ON KitDescriptorChangeLog(kitDescriptorId, changeTime);
CREATE INDEX LibraryChangeLog_libraryId_changeTime ON LibraryChangeLog(libraryId, changeTime);
CREATE INDEX LibraryDilution_libraryId_dilutionId ON LibraryDilution(library_libraryId, dilutionId);
CREATE INDEX Library_boxPositionId ON Library(boxPositionId);
CREATE INDEX Library_sampleId_libraryId ON Library(sample_sampleId,libraryId);
CREATE INDEX PlateChangeLog_plateId_changeTime ON PlateChangeLog(plateId, changeTime);
CREATE INDEX PoolChangeLog_poolId_changeTime ON PoolChangeLog(poolId, changeTime);
CREATE INDEX PoolElements_pool_poolId_elementId ON Pool_Elements(pool_poolId, elementId);
CREATE INDEX Pool_boxPositionId ON Pool(boxPositionId);
CREATE INDEX RunChangeLog_runId_changeTime ON RunChangeLog(runId, changeTime);
CREATE INDEX SampleChangeLog_sampleId_changeTime ON SampleChangeLog(sampleId, changeTime);
CREATE INDEX SampleQC_sampleId ON SampleQC(sample_sampleId);
CREATE INDEX Sample_boxPositionId ON Sample(boxPositionId);
CREATE INDEX Sample_project_projectId_sampleId ON Sample(project_projectId, sampleId);
CREATE INDEX SequencerPartitionContainerChangeLog_containerId_changeTime ON SequencerPartitionContainerChangeLog(containerId, changeTime);
CREATE INDEX StudyChangeLog_studyId_changeTime ON StudyChangeLog(studyId, changeTime);
CREATE INDEX User_loginName ON `User`(loginName);
CREATE INDEX emPCRDilution_emPCR_pcrId_dilutionId ON emPCRDilution(emPCR_pcrId,dilutionId);
CREATE INDEX emPCR_LibraryDilution_dilutionId_pcrId ON emPCR(dilution_dilutionId, pcrId);
