package com.examly.springapp.service;

import com.examly.springapp.model.FaqEntity;
import com.examly.springapp.repository.FaqRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Order(1)
public class FaqService implements CommandLineRunner {
  private static final Logger log = LoggerFactory.getLogger(FaqService.class);
  private static final Set<String> STOP_WORDS = Set.of(
      "a", "an", "and", "are", "be", "can", "do", "does", "for", "how", "i", "in", "is",
      "my", "of", "on", "or", "the", "to", "what", "when", "where", "why", "with", "you", "your",
      "it", "that", "this", "those", "them", "about", "also", "please", "tell", "me");
  private final FaqRepo repo;
  private final GeminiService gemini;
  private final ObjectMapper json;
  private final double semanticThreshold;
  private final double lexicalThreshold;
  private volatile List<FaqEntity> faqs = List.of();

  public FaqService(FaqRepo repo, GeminiService gemini, ObjectMapper json,
      @Value("${chatbot.similarity.threshold:0.65}") double semanticThreshold,
      @Value("${chatbot.lexical.threshold:0.08}") double lexicalThreshold) {
    this.repo = repo;
    this.gemini = gemini;
    this.json = json;
    this.semanticThreshold = semanticThreshold;
    this.lexicalThreshold = lexicalThreshold;
  }

  @Override
  public void run(String... args) throws Exception {
    if (repo.count() == 0) {
      try (InputStream input = new ClassPathResource("faqs.json").getInputStream()) {
        FaqSeed[] seeds = json.readValue(input, FaqSeed[].class);
        List<String> documents = Arrays.stream(seeds)
            .map(seed -> seed.question() + " " + seed.answer()).toList();
        List<float[]> embeddings = gemini.embedAll(documents);
        List<FaqEntity> entries = new ArrayList<>();
        for (int index = 0; index < seeds.length; index++) {
          FaqSeed seed = seeds[index];
          FaqEntity faq = new FaqEntity();
          faq.setId(seed.id());
          faq.setCategory(seed.category());
          faq.setQuestion(seed.question());
          faq.setAnswer(seed.answer());
          if (embeddings.size() == seeds.length)
            faq.setEmbedding(embeddings.get(index));
          entries.add(faq);
        }
        repo.saveAll(entries);
      }
    }
    faqs = repo.findAll().stream().sorted(Comparator.comparing(FaqEntity::getId)).toList();
    log.info("Loaded {} GemVault FAQs (semantic search {})", faqs.size(),
        faqs.stream().anyMatch(faq -> faq.getEmbedding() != null) ? "ready" : "using lexical fallback");
  }

  public List<FaqEntity> list() {
    return faqs;
  }

  public Match findBestMatch(String query, float[] queryEmbedding) {
    Match semantic = null;
    if (queryEmbedding != null) {
      for (FaqEntity faq : faqs) {
        float[] vector = faq.getEmbedding();
        if (vector == null || vector.length != queryEmbedding.length)
          continue;
        double score = cosine(queryEmbedding, vector);
        if (semantic == null || score > semantic.confidence())
          semantic = new Match(faq, score, "semantic");
      }
      if (semantic != null && semantic.confidence() >= semanticThreshold)
        return semantic;
    }
    Set<String> words = tokens(query);
    Match lexical = null;
    for (FaqEntity faq : faqs) {
      double score = Math.max(jaccard(words, tokens(faq.getQuestion())),
          jaccard(words, tokens(faq.getAnswer())) * 0.7);
      if (lexical == null || score > lexical.confidence())
        lexical = new Match(faq, score, "lexical");
    }
    return lexical != null && lexical.confidence() >= lexicalThreshold ? lexical : null;
  }

  private Set<String> tokens(String text) {
    return Arrays.stream(text.toLowerCase().split("[^\\p{L}\\p{N}]+"))
        .filter(word -> word.length() > 1 && !STOP_WORDS.contains(word))
        .map(word -> {
          if (word.startsWith("cancell") || word.startsWith("cancel"))
            return "cancel";
          return word.length() > 4 && word.endsWith("s") ? word.substring(0, word.length() - 1) : word;
        })
        .collect(Collectors.toCollection(HashSet::new));
  }

  private double jaccard(Set<String> left, Set<String> right) {
    if (left.isEmpty() || right.isEmpty())
      return 0;
    Set<String> intersection = new HashSet<>(left);
    intersection.retainAll(right);
    Set<String> union = new HashSet<>(left);
    union.addAll(right);
    return (double) intersection.size() / union.size();
  }

  private double cosine(float[] left, float[] right) {
    double dot = 0, leftLength = 0, rightLength = 0;
    for (int index = 0; index < left.length; index++) {
      dot += left[index] * right[index];
      leftLength += left[index] * left[index];
      rightLength += right[index] * right[index];
    }
    return leftLength == 0 || rightLength == 0 ? 0 : dot / Math.sqrt(leftLength * rightLength);
  }

  private record FaqSeed(Long id, String category, String question, String answer) {
  }

  public record Match(FaqEntity faq, double confidence, String source) {
  }
}
