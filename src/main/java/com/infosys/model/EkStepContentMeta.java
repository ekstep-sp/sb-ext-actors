/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EkStepContentMeta {

    // General
    private String identifier;
    private String name;
    private String code;
    private String description;
    private String[] keywords;
    private String ageGroup;
    private String loadingMessage;
    private String appIcon;
    private String grayScaleAppIcon;
    private String thumbnail;
    private String mediaType;
    private String minSupportedVersion;
    private String filter;
    private String theme;
    private String genre;
    private String objects;
    private String contentType;
    private String visibility;
    private String posterImage;
    private String[] language;
    private String resourceTye;

    // Pedagogy
    private String feedback;
    private String scaffolding;
    private String feedbackType;
    private String teachingMode;
    private String skills;
    private String learningObjective;
    private String pareRequisites;
    private String curriculum;
    private String interactivityLevel;
    private String[] gradeLevel;

    // Technical
    private String activity_class;
    private long duration;
    private long size;
    private String resources;
    private String idealScreenSize;
    private String idealScreenDensity;
    private String mimeType;
    private String minGenieVersion;
    private String minOsVersion;
    private String[] os;
    private String checksum;
    private String downloadUrl;
    private String pkgVersion;
    private String launchUrl;
    private String osId;
    private String artifactUrl;

    // Ownership
    private String developer;
    private String creator;
    private String source;
    private String license;
    private String[] attributions;
    private String[] copyright;
    private String portalOwner;

    // Analytics
    private long rating;
    private double rating_a;
    private long downloads;

    // Authoring
    private String body;
    private String text;
    private String words;
    private String publisher;
    private String owner;
    private String collaborators;
    private String voiceCredits;
    private String soundCredits;
    private String imageCredits;
    private boolean forkable;
    private boolean translatable;
    private String templateType;
    private String domain;

    // Lifecycle
    private String versionCreatedBy;
    private String versionDate;
    private String versionKey; // As per response versionKey
    private String createdBy;
    private String createdOn;
    private String lastUpdatedBy;
    private String lastUpdatedOn;
    private String lastSubmitterBy;
    private String lastSubmittedOn;
    private String status;
    private String releaseNotes;

    // Relations
    private ObjectMeta[] concepts;
    private ObjectMeta[] collection;
    private ObjectMeta[] children;

    public static EkStepContentMeta fromMap(Map<String, Object> map) {
        return new ObjectMapper().convertValue(map, EkStepContentMeta.class);
    }

    public static EkStepContentMeta fromJson(String json) throws IOException {
        return new ObjectMapper().readValue(json, EkStepContentMeta.class);
    }

    public Map<String, Object> toMap() {
        return new ObjectMapper().convertValue(this, Map.class);
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String[] getKeywords() {
        return keywords;
    }

    public void setKeywords(String[] keywords) {
        this.keywords = keywords;
    }

    public String getAgeGroup() {
        return ageGroup;
    }

    public void setAgeGroup(String ageGroup) {
        this.ageGroup = ageGroup;
    }

    public String getLoadingMessage() {
        return loadingMessage;
    }

    public void setLoadingMessage(String loadingMessage) {
        this.loadingMessage = loadingMessage;
    }

    public String getAppIcon() {
        return appIcon;
    }

    public void setAppIcon(String appIcon) {
        this.appIcon = appIcon;
    }

    public String getGrayScaleAppIcon() {
        return grayScaleAppIcon;
    }

    public void setGrayScaleAppIcon(String grayScaleAppIcon) {
        this.grayScaleAppIcon = grayScaleAppIcon;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getMinSupportedVersion() {
        return minSupportedVersion;
    }

    public void setMinSupportedVersion(String minSupportedVersion) {
        this.minSupportedVersion = minSupportedVersion;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getObjects() {
        return objects;
    }

    public void setObjects(String objects) {
        this.objects = objects;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getPosterImage() {
        return posterImage;
    }

    public void setPosterImage(String posterImage) {
        this.posterImage = posterImage;
    }

    public String[] getLanguage() {
        return language;
    }

    public void setLanguage(String[] language) {
        this.language = language;
    }

    public String getResourceTye() {
        return resourceTye;
    }

    public void setResourceTye(String resourceTye) {
        this.resourceTye = resourceTye;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public String getScaffolding() {
        return scaffolding;
    }

    public void setScaffolding(String scaffolding) {
        this.scaffolding = scaffolding;
    }

    public String getFeedbackType() {
        return feedbackType;
    }

    public void setFeedbackType(String feedbackType) {
        this.feedbackType = feedbackType;
    }

    public String getTeachingMode() {
        return teachingMode;
    }

    public void setTeachingMode(String teachingMode) {
        this.teachingMode = teachingMode;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getLearningObjective() {
        return learningObjective;
    }

    public void setLearningObjective(String learningObjective) {
        this.learningObjective = learningObjective;
    }

    public String getPareRequisites() {
        return pareRequisites;
    }

    public void setPareRequisites(String pareRequisites) {
        this.pareRequisites = pareRequisites;
    }

    public String getCurriculum() {
        return curriculum;
    }

    public void setCurriculum(String curriculum) {
        this.curriculum = curriculum;
    }

    public String getInteractivityLevel() {
        return interactivityLevel;
    }

    public void setInteractivityLevel(String interactivityLevel) {
        this.interactivityLevel = interactivityLevel;
    }

    public String[] getGradeLevel() {
        return gradeLevel;
    }

    public void setGradeLevel(String[] gradeLevel) {
        this.gradeLevel = gradeLevel;
    }

    public String getActivity_class() {
        return activity_class;
    }

    public void setActivity_class(String activity_class) {
        this.activity_class = activity_class;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getResources() {
        return resources;
    }

    public void setResources(String resources) {
        this.resources = resources;
    }

    public String getIdealScreenSize() {
        return idealScreenSize;
    }

    public void setIdealScreenSize(String idealScreenSize) {
        this.idealScreenSize = idealScreenSize;
    }

    public String getIdealScreenDensity() {
        return idealScreenDensity;
    }

    public void setIdealScreenDensity(String idealScreenDensity) {
        this.idealScreenDensity = idealScreenDensity;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getMinGenieVersion() {
        return minGenieVersion;
    }

    public void setMinGenieVersion(String minGenieVersion) {
        this.minGenieVersion = minGenieVersion;
    }

    public String getMinOsVersion() {
        return minOsVersion;
    }

    public void setMinOsVersion(String minOsVersion) {
        this.minOsVersion = minOsVersion;
    }

    public String[] getOs() {
        return os;
    }

    public void setOs(String[] os) {
        this.os = os;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getPkgVersion() {
        return pkgVersion;
    }

    public void setPkgVersion(String pkgVersion) {
        this.pkgVersion = pkgVersion;
    }

    public String getLaunchUrl() {
        return launchUrl;
    }

    public void setLaunchUrl(String launchUrl) {
        this.launchUrl = launchUrl;
    }

    public String getOsId() {
        return osId;
    }

    public void setOsId(String osId) {
        this.osId = osId;
    }

    public String getArtifactUrl() {
        return artifactUrl;
    }

    public void setArtifactUrl(String artifactUrl) {
        this.artifactUrl = artifactUrl;
    }

    public String getDeveloper() {
        return developer;
    }

    public void setDeveloper(String developer) {
        this.developer = developer;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public String[] getAttributions() {
        return attributions;
    }

    public void setAttributions(String[] attributions) {
        this.attributions = attributions;
    }

    public String[] getCopyright() {
        return copyright;
    }

    public void setCopyright(String[] copyright) {
        this.copyright = copyright;
    }

    public String getPortalOwner() {
        return portalOwner;
    }

    public void setPortalOwner(String portalOwner) {
        this.portalOwner = portalOwner;
    }

    public long getRating() {
        return rating;
    }

    public void setRating(long rating) {
        this.rating = rating;
    }

    public double getRating_a() {
        return rating_a;
    }

    public void setRating_a(double rating_a) {
        this.rating_a = rating_a;
    }

    public long getDownloads() {
        return downloads;
    }

    public void setDownloads(long downloads) {
        this.downloads = downloads;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getWords() {
        return words;
    }

    public void setWords(String words) {
        this.words = words;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getCollaborators() {
        return collaborators;
    }

    public void setCollaborators(String collaborators) {
        this.collaborators = collaborators;
    }

    public String getVoiceCredits() {
        return voiceCredits;
    }

    public void setVoiceCredits(String voiceCredits) {
        this.voiceCredits = voiceCredits;
    }

    public String getSoundCredits() {
        return soundCredits;
    }

    public void setSoundCredits(String soundCredits) {
        this.soundCredits = soundCredits;
    }

    public String getImageCredits() {
        return imageCredits;
    }

    public void setImageCredits(String imageCredits) {
        this.imageCredits = imageCredits;
    }

    public boolean isForkable() {
        return forkable;
    }

    public void setForkable(boolean forkable) {
        this.forkable = forkable;
    }

    public boolean isTranslatable() {
        return translatable;
    }

    public void setTranslatable(boolean translatable) {
        this.translatable = translatable;
    }

    public String getTemplateType() {
        return templateType;
    }

    public void setTemplateType(String templateType) {
        this.templateType = templateType;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getVersionCreatedBy() {
        return versionCreatedBy;
    }

    public void setVersionCreatedBy(String versionCreatedBy) {
        this.versionCreatedBy = versionCreatedBy;
    }

    public String getVersionDate() {
        return versionDate;
    }

    public void setVersionDate(String versionDate) {
        this.versionDate = versionDate;
    }

    public String getVersionKey() {
        return versionKey;
    }

    public void setVersionKey(String versionKey) {
        this.versionKey = versionKey;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(String createdOn) {
        this.createdOn = createdOn;
    }

    public String getLastUpdatedBy() {
        return lastUpdatedBy;
    }

    public void setLastUpdatedBy(String lastUpdatedBy) {
        this.lastUpdatedBy = lastUpdatedBy;
    }

    public String getLastUpdatedOn() {
        return lastUpdatedOn;
    }

    public void setLastUpdatedOn(String lastUpdatedOn) {
        this.lastUpdatedOn = lastUpdatedOn;
    }

    public String getLastSubmitterBy() {
        return lastSubmitterBy;
    }

    public void setLastSubmitterBy(String lastSubmitterBy) {
        this.lastSubmitterBy = lastSubmitterBy;
    }

    public String getLastSubmittedOn() {
        return lastSubmittedOn;
    }

    public void setLastSubmittedOn(String lastSubmittedOn) {
        this.lastSubmittedOn = lastSubmittedOn;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReleaseNotes() {
        return releaseNotes;
    }

    public void setReleaseNotes(String releaseNotes) {
        this.releaseNotes = releaseNotes;
    }

    public ObjectMeta[] getConcepts() {
        return concepts;
    }

    public void setConcepts(ObjectMeta[] concepts) {
        this.concepts = concepts;
    }

    public ObjectMeta[] getCollection() {
        return collection;
    }

    public void setCollection(ObjectMeta[] collection) {
        this.collection = collection;
    }

    public ObjectMeta[] getChildren() {
        return children;
    }

    public void setChildren(ObjectMeta[] children) {
        this.children = children;
    }
}
