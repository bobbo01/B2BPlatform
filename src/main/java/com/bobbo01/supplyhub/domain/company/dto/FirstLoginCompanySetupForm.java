package com.bobbo01.supplyhub.domain.company.dto;

public class FirstLoginCompanySetupForm {

    private String companyName;
    private String companyDomain;
    private String inviteCode;
    private String action = "register";

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanyDomain() {
        return companyDomain;
    }

    public void setCompanyDomain(String companyDomain) {
        this.companyDomain = companyDomain;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public boolean useInviteCode() {
        return "useInviteCode".equals(action);
    }
}
