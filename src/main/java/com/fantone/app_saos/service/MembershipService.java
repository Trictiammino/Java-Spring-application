package com.fantone.app_saos.service;

import com.fantone.app_saos.dto.response.MembershipResponseDto;
import com.fantone.app_saos.exception.ResourceConflictException;
import com.fantone.app_saos.model.Card;
import com.fantone.app_saos.model.GymPlan;
import com.fantone.app_saos.model.Membership;
import com.fantone.app_saos.model.User;
import com.fantone.app_saos.repository.CardRepository;
import com.fantone.app_saos.repository.GymPlanRepository;
import com.fantone.app_saos.repository.MembershipRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MembershipService {

    private final MembershipRepository membershipRepository;
    private final GymPlanRepository gymPlanRepository;
    private final CardRepository cardRepository;
    private final UserService userService;

    /**
     * Recupera l'abbonamento attivo dell'utente e lo mappa nel DTO per il frontend.
     * Se non viene trovato alcun abbonamento attivo, restituisce null (gestito dal Controller con un 404).
     */
    public MembershipResponseDto getActiveSubscriptionByUserId(Long userId) {
        return membershipRepository.findActiveMembershipByUserId(userId)
                .map(m -> new MembershipResponseDto(
                        m.getId(),
                        m.getGymPlan().getName(),
                        m.getStatus(),
                        m.getExpiresAt()
                ))
                .orElse(null);
    }

    /**
     * Processa l'acquisto di un abbonamento in tempo reale.
     * Sfrutta @Transactional per garantire che se una qualsiasi operazione fallisce (es: saldo insufficiente),
     * il database effettui il rollback completo (nessun addebito sulla tessera).
     */
    @Transactional
    public void purchasePlan(Long userId, Long planId) {
        // 1. Recupera l'utente e il piano d'abbonamento richiesto
        User user = userService.findById(userId);
        GymPlan plan = gymPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceConflictException("Abbonamento palestra non trovato con id: " + planId));

        // 2. Recupera la tessera attiva dell'utente
        Card card = cardRepository.findByUserIdAndExpiresAtAfter(userId, LocalDateTime.now())
                .orElseThrow(() -> new ResourceConflictException("Nessuna tessera attiva trovata. Genera una tessera prima di acquistare un abbonamento."));

        // 3. Controllo del saldo disponibile sulla tessera
        if (card.getBalance().compareTo(plan.getPrice()) < 0) {
            throw new ResourceConflictException("Saldo insufficiente sulla tua carta. Per favore, ricarica.");
        }

        // 4. Scala l'importo dal saldo della tessera e salva
        card.setBalance(card.getBalance().subtract(plan.getPrice()));
        cardRepository.save(card);

        // 5. Se l'utente ha già una membership attiva, la portiamo a 'expired' per evitare sovrapposizioni
        membershipRepository.findActiveMembershipByUserId(userId)
                .ifPresent(oldMembership -> {
                    oldMembership.setStatus("expired");
                    membershipRepository.save(oldMembership);
                });

        // 6. Crea, configura e memorizza la nuova Membership
        Membership membership = new Membership();
        membership.setUser(user);
        membership.setGymPlan(plan);
        membership.setStartAt(LocalDateTime.now());
        membership.setExpiresAt(LocalDateTime.now().plusDays(plan.getDurationDays()));
        membership.setStatus("active");

        membershipRepository.save(membership);
    }
}