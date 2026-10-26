package com.examly.springapp.repository;

import com.examly.springapp.model.Feedback;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeedbackRepo extends JpaRepository<Feedback, Long> {
  List<Feedback> findByUserUserId(Integer userId);

  List<Feedback> findByItemItemId(Long itemId);

  @EntityGraph(attributePaths = "user")
  List<Feedback> findByItemItemIdOrderByDateDescFeedbackIdDesc(Long itemId);

  boolean existsByUserUserIdAndItemItemId(Integer userId, Long itemId);

  @Query("select avg(f.rating), count(f) from Feedback f where f.item.itemId = :itemId and f.rating is not null")
  List<Object[]> ratingForItem(@Param("itemId") Long itemId);

  @Query("select f.item.itemId, avg(f.rating), count(f) from Feedback f where f.item.itemId in :itemIds and f.rating is not null group by f.item.itemId")
  List<Object[]> ratingsForItems(@Param("itemIds") Collection<Long> itemIds);
}
