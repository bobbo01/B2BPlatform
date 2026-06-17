package com.bobbo01.supplyhub.domain.company.dto;

public class CompanyProfileUpdateForm {

    private String companyName;
    private String companyDomain;

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
}
