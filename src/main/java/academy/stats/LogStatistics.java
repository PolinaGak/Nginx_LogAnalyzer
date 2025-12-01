package academy.stats;

import java.time.LocalDate;
import java.util.List;

public class LogStatistics {

  private List<String> files;
  private int totalRequestsCount;
  private ResponseSize responseSizeInBytes;
  private List<ResourceCount> resources;
  private List<ResponseCodeCount> responseCodes;
  private LocalDate startDate;
  private LocalDate endDate;

  private List<RequestsPerDate> requestsPerDate;
  private List<String> uniqueProtocols;

  public List<String> getFiles() { return files; }

  public void setFiles(List<String> files) { this.files = files; }

  public int getTotalRequestsCount() { return totalRequestsCount; }

  public void setTotalRequestsCount(int totalRequestsCount) {
    this.totalRequestsCount = totalRequestsCount;
  }

  public void setStartDate(LocalDate fromDate) { this.startDate = fromDate; }

  public LocalDate getStartDate() { return this.startDate; }

  public void setEndDate(LocalDate toDate) { this.endDate = toDate; }

  public LocalDate getEndDate() { return this.endDate; }

  public ResponseSize getResponseSizeInBytes() { return responseSizeInBytes; }

  public void setResponseSizeInBytes(ResponseSize responseSizeInBytes) {
    this.responseSizeInBytes = responseSizeInBytes;
  }

  public List<ResourceCount> getResources() { return resources; }

  public void setResources(List<ResourceCount> resources) {
    this.resources = resources;
  }

  public List<ResponseCodeCount> getResponseCodes() { return responseCodes; }

  public void setResponseCodes(List<ResponseCodeCount> responseCodes) {
    this.responseCodes = responseCodes;
  }

  public List<RequestsPerDate> getRequestsPerDate() { return requestsPerDate; }

  public void setRequestsPerDate(List<RequestsPerDate> requestsPerDate) {
    this.requestsPerDate = requestsPerDate;
  }

  public List<String> getUniqueProtocols() { return uniqueProtocols; }

  public void setUniqueProtocols(List<String> uniqueProtocols) {
    this.uniqueProtocols = uniqueProtocols;
  }

  public static class ResponseSize {
    private double average;
    private double max;
    private double p95;

    public double getAverage() { return average; }
    public void setAverage(double average) { this.average = average; }
    public double getMax() { return max; }
    public void setMax(double max) { this.max = max; }
    public double getP95() { return p95; }
    public void setP95(double p95) { this.p95 = p95; }
  }

  public static class ResourceCount {
    private String resource;
    private int totalRequestsCount;

    public String getResource() { return resource; }
    public void setResource(String resource) { this.resource = resource; }
    public int getTotalRequestsCount() { return totalRequestsCount; }
    public void setTotalRequestsCount(int totalRequestsCount) {
      this.totalRequestsCount = totalRequestsCount;
    }
  }

  public static class ResponseCodeCount {
    private int code;
    private int totalResponsesCount;

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public int getTotalResponsesCount() { return totalResponsesCount; }
    public void setTotalResponsesCount(int totalResponsesCount) {
      this.totalResponsesCount = totalResponsesCount;
    }
  }

  public static class RequestsPerDate {
    private String date;
    private String weekday;
    private int totalRequestsCount;
    private double totalRequestsPercentage;

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getWeekday() { return weekday; }
    public void setWeekday(String weekday) { this.weekday = weekday; }
    public int getTotalRequestsCount() { return totalRequestsCount; }
    public void setTotalRequestsCount(int totalRequestsCount) {
      this.totalRequestsCount = totalRequestsCount;
    }
    public double getTotalRequestsPercentage() {
      return totalRequestsPercentage;
    }
    public void setTotalRequestsPercentage(double totalRequestsPercentage) {
      this.totalRequestsPercentage = totalRequestsPercentage;
    }
  }
}