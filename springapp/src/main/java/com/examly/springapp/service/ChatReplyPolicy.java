package com.examly.springapp.service;

import java.util.regex.Pattern;

final class ChatReplyPolicy {
  static final String OUT_OF_SCOPE = "I can help with GemVault jewellery, orders, your account and shopping questions. What would you like to know?";
  private static final Pattern UNRELATED_REQUEST = Pattern.compile(
      "(?i)\\b(?:write|generate|create|give|show|make)\\b.{0,90}\\b(?:code|program|script|hello\\s*world|poem|story|recipe)\\b"
          + "|\\b(?:java|python|javascript|typescript|c\\+\\+|sql)\\s+(?:code|program|script|example)\\b"
          + "|\\b(?:weather forecast|solve (?:this |an? )?equation)\\b");
  private static final Pattern CODE_OUTPUT = Pattern.compile(
      "(?is)```|\\bpublic\\s+class\\s+\\w+|\\bSystem\\.out\\.print|\\bdef\\s+\\w+\\s*\\(|\\bconsole\\.log\\s*\\(");

  private ChatReplyPolicy() {
  }

  static boolean isClearlyOutOfScope(String question) {
    return question != null && UNRELATED_REQUEST.matcher(question).find();
  }

  static String plainText(String answer) {
    if (answer == null)
      return "";
    String text = answer.replace("\r\n", "\n").replace('\r', '\n').trim();
    text = text.replaceAll("(?m)^[ \\t]*#{1,6}[ \\t]+", "");
    text = text.replaceAll("(?m)^[ \\t]*[-*][ \\t]+", "");
    text = text.replaceAll("\\*\\*([^*\\n]+)\\*\\*", "$1");
    text = text.replaceAll("(?<!\\*)\\*([^*\\n]+)\\*(?!\\*)", "$1");
    text = text.replaceAll("(?m)^[ \\t]*`{3}[^\\n]*\\n?", "");
    text = text.replace("`", "");
    text = text.replaceAll("\\n{3,}", "\n\n");
    return text.trim();
  }

  static String safeAnswer(String answer) {
    return CODE_OUTPUT.matcher(answer == null ? "" : answer).find() ? OUT_OF_SCOPE : plainText(answer);
  }
}
