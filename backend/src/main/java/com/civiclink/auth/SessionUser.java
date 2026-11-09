package com.civiclink.auth;

import java.io.Serializable;

public class SessionUser implements Serializable {
    public final long id;
    public final String username;
    public final String role;
    public final String organization;
    public final String department;
    public final String displayName;
    public SessionUser(long id, String username, String role, String organization, String department, String displayName) {
        this.id = id; this.username = username; this.role = role;
        this.organization = organization == null ? "" : organization;
        this.department = department == null ? "" : department;
        this.displayName = displayName == null ? "" : displayName;
    }
}
