package io.pivotal.security.model;


public class StringGeneratorRequest {

  private StringSecretParameters parameters;
  private String type;

  public StringSecretParameters getParameters() {
    return parameters;
  }

  public void setParameters(StringSecretParameters parameters) {
    this.parameters = parameters;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }
}