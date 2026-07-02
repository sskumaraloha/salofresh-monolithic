package com.salofresh.security;

import java.util.Map;

public class GoogleOAuth2UserInfo {

    private final Map<String, Object> attributes;

    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public String getId() {
        return String.valueOf(attributes.get("sub"));
    }

    public String getEmail() {
        return (String) attributes.get("email");
    }

    public String getFirstName() {
        Object givenName = attributes.get("given_name");
        return givenName != null ? givenName.toString() : "User";
    }

    public String getLastName() {
        Object familyName = attributes.get("family_name");
        return familyName != null ? familyName.toString() : null;
    }

    public String getImageUrl() {
        return (String) attributes.get("picture");
    }

    public boolean isEmailVerified() {
        Object verified = attributes.get("email_verified");
        return verified != null && Boolean.parseBoolean(verified.toString());
    }
}
