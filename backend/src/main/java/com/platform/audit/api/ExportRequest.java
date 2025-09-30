package com.platform.audit.api;

import java.time.LocalDate;
import java.util.List;

public class ExportRequest {
    private String format;
    private ExportFilter filter;

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public ExportFilter getFilter() {
        return filter;
    }

    public void setFilter(ExportFilter filter) {
        this.filter = filter;
    }

    public static class ExportFilter {
        private LocalDate dateFrom;
        private LocalDate dateTo;
        private String search;
        private List<String> actionTypes;
        private List<String> resourceTypes;

        public LocalDate getDateFrom() {
            return dateFrom;
        }

        public void setDateFrom(LocalDate dateFrom) {
            this.dateFrom = dateFrom;
        }

        public LocalDate getDateTo() {
            return dateTo;
        }

        public void setDateTo(LocalDate dateTo) {
            this.dateTo = dateTo;
        }

        public String getSearch() {
            return search;
        }

        public void setSearch(String search) {
            this.search = search;
        }

        public List<String> getActionTypes() {
            return actionTypes;
        }

        public void setActionTypes(List<String> actionTypes) {
            this.actionTypes = actionTypes;
        }

        public List<String> getResourceTypes() {
            return resourceTypes;
        }

        public void setResourceTypes(List<String> resourceTypes) {
            this.resourceTypes = resourceTypes;
        }
    }
}