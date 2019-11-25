package csc450.airline.models;

import java.util.HashMap;

public class AnalyticResult<T> {
  public T target;
  public HashMap<String, String> results;

  public AnalyticResult(T target, HashMap<String, String> results) {
    this.target = target;
    this.results = results;
  }
}