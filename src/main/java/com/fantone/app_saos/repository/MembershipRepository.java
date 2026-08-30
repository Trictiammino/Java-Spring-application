package com.fantone.app_saos.repository;

import com.fantone.app_saos.model.Membership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, Long> {

    /**
     * Recupera l'abbonamento attivo di un utente specifico.
     * Cerca una riga dove lo stato è 'active' e la data di scadenza è successiva al momento attuale.
     */
    @Query("SELECT m FROM Membership m " +
            "WHERE m.user.id = :userId " +
            "AND m.status = 'active' " +
            "AND m.expiresAt > CURRENT_TIMESTAMP")
    Optional<Membership> findActiveMembershipByUserId(@Param("userId") Long userId);

    /**
     * Trova l'ultimo abbonamento acquistato dall'utente in ordine cronologico.
     * Utile se vuoi mostrare l'ultimo piano posseduto anche se è scaduto.
     */
    Optional<Membership> findFirstByUserIdOrderByStartAtDesc(Long userId);
}