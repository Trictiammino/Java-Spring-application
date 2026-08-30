package com.fantone.app_saos.repository;

import com.fantone.app_saos.model.PurchasedProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PurchasedProductRepository extends JpaRepository<PurchasedProduct, Long> {

    /**
     * Recupera la cronologia di tutti i prodotti acquistati da un determinato utente
     * ordinati dal più recente al più vecchio.
     */
    List<PurchasedProduct> findByUserIdOrderByPurchasedAtDesc(Long userId);
}