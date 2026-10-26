package com.examly.springapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "faqs")
public class FaqEntity {
  @Id
  private Long id;
  @Column(nullable = false, length = 80)
  private String category;
  @Column(nullable = false, length = 500)
  private String question;
  @Lob
  @Column(nullable = false, columnDefinition = "LONGTEXT")
  private String answer;
  @Lob
  @Column(columnDefinition = "LONGTEXT")
  @Convert(converter = EmbeddingConverter.class)
  private float[] embedding;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getQuestion() {
    return question;
  }

  public void setQuestion(String question) {
    this.question = question;
  }

  public String getAnswer() {
    return answer;
  }

  public void setAnswer(String answer) {
    this.answer = answer;
  }

  public float[] getEmbedding() {
    return embedding;
  }

  public void setEmbedding(float[] embedding) {
    this.embedding = embedding;
  }
}
